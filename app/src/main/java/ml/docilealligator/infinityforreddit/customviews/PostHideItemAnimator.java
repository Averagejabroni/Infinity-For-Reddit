package ml.docilealligator.infinityforreddit.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PostHideItemAnimator extends DefaultItemAnimator {
    private final Set<RecyclerView.ViewHolder> removing = new HashSet<>();

    public PostHideItemAnimator() {
        setRemoveDuration(200);
        setMoveDuration(220);
        setSupportsChangeAnimations(false);
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        endAnimation(holder);
        removing.add(holder);
        View view = holder.itemView;
        dispatchRemoveStarting(holder);
        view.animate().translationX(view.getWidth()).alpha(0f).setDuration(getRemoveDuration())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate().setListener(null);
                        view.setTranslationX(0f);
                        view.setAlpha(1f);
                        if (removing.remove(holder)) {
                            dispatchRemoveFinished(holder);
                        }
                        if (!isRunning()) {
                            dispatchAnimationsFinished();
                        }
                    }
                }).start();
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder holder) {
        if (removing.contains(holder)) {
            holder.itemView.animate().cancel();
        }
        super.endAnimation(holder);
    }

    @Override
    public void endAnimations() {
        for (RecyclerView.ViewHolder holder : new ArrayList<>(removing)) {
            holder.itemView.animate().cancel();
        }
        super.endAnimations();
    }

    @Override
    public boolean isRunning() {
        return !removing.isEmpty() || super.isRunning();
    }
}
