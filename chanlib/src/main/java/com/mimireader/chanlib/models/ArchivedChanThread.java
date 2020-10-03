package com.mimireader.chanlib.models;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ArchivedChanThread extends ChanThread {
    @Expose
    private final String name;

    @Expose
    private final String domain;

    public ArchivedChanThread(@NonNull String boardName, long threadId, @NonNull List<ArchivedChanPost> posts, @NonNull String name, @NonNull String domain) {
        super(boardName, threadId, new ArrayList<ChanPost>(posts.size()));

        this.name = name;
        this.domain = domain;
        this.posts.addAll(posts);
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    @NotNull
    @Override
    public String toString() {
        return "ArchivedChanThread{" +
                "board='" + boardName + '\'' +
                ", title='" + boardTitle + '\'' +
                ", thread id='" + threadId + '\'' +
                ", post count='" + posts.size() + '\'' +
                ", archive name='" + name + '\'' +
                ", archive domain='" + domain + '\'' +
                '}';
    }
}
