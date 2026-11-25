package ml.docilealligator.infinityforreddit.post;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.account.Account;
import ml.docilealligator.infinityforreddit.activities.BaseActivity;
import ml.docilealligator.infinityforreddit.database.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.events.PostUpdateEventToPostDetailFragment;
import ml.docilealligator.infinityforreddit.events.PostVisibilityChangedEvent;
import ml.docilealligator.infinityforreddit.readpost.ReadPost;
import ml.docilealligator.infinityforreddit.readpost.ReadPostType;
import retrofit2.Retrofit;

public final class PostHideAction {
    private static final Set<String> pending = new HashSet<>();

    private PostHideAction() {}

    public static boolean isPending(String accountName, Post post) {
        return post != null && pending.contains(accountName + ":" + post.getFullName());
    }

    public static void bind(View row, Post post, BaseActivity activity, String accountName,
                            String accessToken, Retrofit oauthRetrofit,
                            RedditDataRoomDatabase database, Executor executor, int iconColor) {
        MaterialButton button = row.findViewById(R.id.tablet_hide_button);
        if (button == null || post == null) {
            return;
        }
        String key = accountName + ":" + post.getFullName();
        button.setTag(key);
        button.setIconTint(ColorStateList.valueOf(iconColor));
        button.setContentDescription(activity.getString(post.isHidden()
                ? R.string.action_unhide_post : R.string.action_hide_post));
        button.setEnabled(!pending.contains(key));
        button.setOnClickListener(view -> {
            if (!pending.add(key)) {
                return;
            }
            View parent = row;
            while (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
                if (parent instanceof RecyclerView) {
                    ((RecyclerView) parent).stopScroll();
                    break;
                }
            }
            button.setEnabled(false);
            boolean hidden = !post.isHidden();
            HidePost.HidePostListener listener = new HidePost.HidePostListener() {
                private void complete(boolean success) {
                    pending.remove(key);
                    if (success) {
                        post.setHidden(hidden);
                    }
                    if (key.equals(button.getTag())) {
                        button.setEnabled(true);
                        button.setContentDescription(activity.getString(post.isHidden()
                                ? R.string.action_unhide_post : R.string.action_hide_post));
                    }
                    if (success) {
                        EventBus.getDefault().post(new PostVisibilityChangedEvent(accountName, post));
                        EventBus.getDefault().post(new PostUpdateEventToPostDetailFragment(post));
                    } else {
                        Toast.makeText(activity, hidden ? R.string.post_hide_failed
                                : R.string.post_unhide_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void success() { complete(true); }

                @Override
                public void failed() { complete(false); }
            };
            if (Account.ANONYMOUS_ACCOUNT.equals(accountName)) {
                executor.execute(() -> {
                    try {
                        database.runInTransaction(() -> {
                            if (hidden) {
                                if (!database.accountDao().isAnonymousAccountInserted()) {
                                    database.accountDao().insert(Account.getAnonymousAccount());
                                }
                                database.readPostDao().insert(new ReadPost(accountName, post.getId(),
                                        ReadPostType.ANONYMOUS_HIDDEN_POSTS));
                            } else {
                                database.readPostDao().deleteReadPost(accountName, post.getId(),
                                        ReadPostType.ANONYMOUS_HIDDEN_POSTS);
                            }
                        });
                        new Handler(Looper.getMainLooper()).post(listener::success);
                    } catch (RuntimeException error) {
                        new Handler(Looper.getMainLooper()).post(listener::failed);
                    }
                });
            } else if (hidden) {
                HidePost.hidePost(oauthRetrofit, accessToken, post.getFullName(), listener);
            } else {
                HidePost.unhidePost(oauthRetrofit, accessToken, post.getFullName(), listener);
            }
        });
    }
}
