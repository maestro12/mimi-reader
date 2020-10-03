package com.emogoth.android.phone.mimi.db;

import android.util.Log;

import com.emogoth.android.phone.mimi.db.model.ArchivedPost;
import com.mimireader.chanlib.models.ArchivedChanPost;
import com.mimireader.chanlib.models.ArchivedChanThread;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

public class ArchivedPostTableConnection {
    public static Single<List<ArchivedPost>> getPosts(String boardName, long threadId) {
        return DatabaseUtils.fetchTable(ArchivedPost.class, ArchivedPost.TABLE_NAME, null, ArchivedPost.BOARD_NAME + "=? AND " + ArchivedPost.THREAD_ID + "=?", boardName, threadId);
    }

    public static Flowable<List<ArchivedPost>> watchPosts(String boardName, long threadId) {
        return DatabaseUtils.observeTable(ArchivedPost.class, ArchivedPost.TABLE_NAME, null, ArchivedPost.BOARD_NAME + "=? AND " + ArchivedPost.THREAD_ID + "=?", boardName, threadId);
    }

    public static Single<Boolean> putThreadSingle(final ArchivedChanThread thread) {
        return Single.defer((Callable<SingleSource<ArchivedChanThread>>) () -> Single.just(thread))
                .map(ArchivedPostTableConnection::convertToArchivedPosts)
                .flatMap((Function<List<ArchivedPost>, SingleSource<Boolean>>) archivedPosts -> DatabaseUtils.insert(archivedPosts).first(false));
    }

    public static void putThread(final ArchivedChanThread thread) throws Exception {
        final List<ArchivedPost> posts = convertToArchivedPosts(thread);
        Log.d("ArchivedPostsTable", "Put an archived thread into the database: saved " + posts.size() + " posts");
        DatabaseUtils.insertModels(posts);
    }

    public static Single<Boolean> removeThread(final String boardName, final long threadId) {
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[2];
        args[0] = new DatabaseUtils.WhereArg(ArchivedPost.BOARD_NAME + "=?", boardName);
        args[1] = new DatabaseUtils.WhereArg(ArchivedPost.POST_ID + "=?", threadId);

        return DatabaseUtils.remove(new ArchivedPost(), true, args).single(false);
    }

    private static List<ArchivedPost> convertToArchivedPosts(ArchivedChanThread archivedChanThread) throws Exception {
        final List<ArchivedPost> posts = new ArrayList<>(archivedChanThread.getPosts().size());
        for (ChanPost post : archivedChanThread.getPosts()) {
            if (post instanceof ArchivedChanPost) {
                ArchivedPost dbPost = new ArchivedPost();
                dbPost.setArchiveDomain(archivedChanThread.getDomain());
                dbPost.setArchiveName(archivedChanThread.getName());
                dbPost.setBoard(archivedChanThread.getBoardName());
                dbPost.setThreadId(archivedChanThread.getThreadId());
                dbPost.setPostId(post.getNo());
                dbPost.setMediaLink(((ArchivedChanPost) post).mediaLink);
                dbPost.setThumbLink(((ArchivedChanPost) post).thumbLink);

                posts.add(dbPost);
            }
        }
        if (posts.size() != archivedChanThread.getPosts().size()) {
            throw new Exception("Not all posts in ArchivedChanThread are of type ArchivedChanPost");
        }

        return posts;
    }
}
