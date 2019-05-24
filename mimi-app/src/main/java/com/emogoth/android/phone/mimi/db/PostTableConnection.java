package com.emogoth.android.phone.mimi.db;


import com.emogoth.android.phone.mimi.db.model.PostModel;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

public class PostTableConnection {
    public static final String LOG_TAG = PostTableConnection.class.getSimpleName();

    public static Single<List<PostModel>> fetchThread(long threadId) {
        return DatabaseUtils.fetchTable(PostModel.class, PostModel.TABLE_NAME, "post_id", PostModel.KEY_THREAD_ID + "=?", String.valueOf(threadId)).single(Collections.emptyList());
    }

    public static Flowable<List<PostModel>> watchThread(long threadId) {
        return DatabaseUtils.observeTable(PostModel.class, PostModel.TABLE_NAME, "post_id", PostModel.KEY_THREAD_ID + "=?", String.valueOf(threadId));
    }

    public static Function<List<PostModel>, ChanThread> mapDbPostsToChanThread(final String boardName, final long threadId) {
        return postModels -> convertDbPostsToChanThread(boardName, threadId, postModels);
    }

    public static ChanThread convertDbPostsToChanThread(String boardName, long threadId, List<PostModel> postModels) {
        if (postModels == null || postModels.size() == 0) {
            return ChanThread.empty();
        }

        List<ChanPost> posts = new ArrayList<>(postModels.size());
        for (PostModel dbPost : postModels) {
            posts.add(dbPost.toPost());
        }

        return new ChanThread(boardName, threadId, posts);
    }

    public static Flowable<Boolean> putThread(final ChanThread thread) {
        if (thread == null || thread.getPosts() == null || thread.getPosts().size() == 0) {
            return Flowable.just(false);
        }

        List<PostModel> posts = new ArrayList<>(thread.getPosts().size());
        for (int i = 0; i < thread.getPosts().size(); i++) {
            posts.add(new PostModel(thread.getThreadId(), thread.getPosts().get(i)));
        }

        return DatabaseUtils.insert(posts);
    }

    public static Flowable<Boolean> removeThread(final long threadId) {
        DatabaseUtils.WhereArg arg = new DatabaseUtils.WhereArg(PostModel.KEY_THREAD_ID + "=?", threadId);
        return DatabaseUtils.remove(new PostModel(), false, arg);
    }
}
