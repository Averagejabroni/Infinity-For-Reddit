package ml.docilealligator.infinityforreddit.events;

import ml.docilealligator.infinityforreddit.post.Post;

public class PostVisibilityChangedEvent {
    public final String accountName;
    public final Post post;

    public PostVisibilityChangedEvent(String accountName, Post post) {
        this.accountName = accountName;
        this.post = post;
    }
}
