package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

@Table(name = "archived_posts", id = "id")
public class ArchivedPost extends BaseModel {
    public static final String TABLE_NAME = "archived_posts";

    public static final String POST_ID = "post_id";
    public static final String THREAD_ID = "thread_id";
    public static final String BOARD_NAME = "board_name";

    public static final String MEDIA_LINK = "media_link";
    public static final String THUMB_LINK = "thumb_link";
    public static final String ARCHIVE_NAME = "archive_name";
    public static final String ARCHIVE_DOMAIN = "archive_domain";

    @Column(name = POST_ID)
    private long postId;

    @Column(name = THREAD_ID)
    private long threadId;

    @Column(name = BOARD_NAME)
    private String board;

    @Column(name = MEDIA_LINK)
    private String mediaLink;

    @Column(name = THUMB_LINK)
    private String thumbLink;

    @Column(name = ARCHIVE_NAME)
    private String archiveName;

    @Column(name = ARCHIVE_DOMAIN)
    private String archiveDomain;

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink) {
        this.mediaLink = mediaLink;
    }

    public String getThumbLink() {
        return thumbLink;
    }

    public void setThumbLink(String thumbLink) {
        this.thumbLink = thumbLink;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getArchiveDomain() {
        return archiveDomain;
    }

    public void setArchiveDomain(String archiveDomain) {
        this.archiveDomain = archiveDomain;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(POST_ID, postId);
        values.put(THREAD_ID, threadId);
        values.put(BOARD_NAME, board);
        values.put(MEDIA_LINK, mediaLink);
        values.put(THUMB_LINK, thumbLink);
        values.put(ARCHIVE_NAME, archiveName);
        values.put(ARCHIVE_DOMAIN, archiveDomain);
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[2];
        args[0] = new DatabaseUtils.WhereArg(BOARD_NAME + "=?", board);
        args[1] = new DatabaseUtils.WhereArg(POST_ID + "=?", postId);
        return args;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof ArchivedPost) {
            this.postId = ((ArchivedPost) model).postId;
            this.threadId = ((ArchivedPost) model).threadId;
            this.board = ((ArchivedPost) model).board;
            this.mediaLink = ((ArchivedPost) model).mediaLink;
            this.thumbLink = ((ArchivedPost) model).thumbLink;
            this.archiveName = ((ArchivedPost) model).archiveName;
            this.archiveDomain = ((ArchivedPost) model).archiveDomain;
        }
    }

    @Override
    public boolean isEmpty() {
        return postId == -1;
    }
}
