package com.mimireader.chanlib.models;

public class ErrorChanThread extends ChanThread {
    private final Throwable throwable;

    public ErrorChanThread(ChanThread chanThread, Throwable throwable) {
        this.boardName = chanThread.boardName;
        this.threadId = chanThread.threadId;
        this.posts.addAll(chanThread.posts);
        this.throwable = throwable;
    }

    public Throwable getError() {
        return throwable;
    }
}
