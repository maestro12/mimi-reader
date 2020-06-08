package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;
import android.text.TextUtils;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.mimireader.chanlib.interfaces.PostConverter;
import com.mimireader.chanlib.models.ChanPost;

@Table(name = "thread_posts")
public class PostModel extends BaseModel implements PostConverter {
    public static final String TABLE_NAME = "thread_posts";

    public static final String KEY_THREAD_ID = "thread_id";
    public static final String KEY_POST_ID = "post_id";
    public static final String KEY_CLOSED = "closed";
    public static final String KEY_STICKY = "sticky";
    public static final String KEY_READABLE_TIME = "readable_time";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_OLD_FILENAME = "old_filename";
    public static final String KEY_NEW_FILENAME = "new_filename";
    public static final String KEY_FILE_SIZE = "file_size";
    public static final String KEY_FILE_EXT = "file_ext";
    public static final String KEY_FILE_WIDTH = "file_width";
    public static final String KEY_FILE_HEIGHT = "file_height";
    public static final String KEY_THUMB_WIDTH = "thumb_width";
    public static final String KEY_THUMB_HEIGHT = "thumb_height";
    public static final String KEY_EPOCH = "epoch";
    public static final String KEY_MD5 = "md5";
    public static final String KEY_RESTO = "resto";
    public static final String KEY_BUMP_LIMIT = "bump_limit";
    public static final String KEY_IMAGE_LIMIT = "image_limit";
    public static final String KEY_SEMANTIC_URL = "semantic_url";
    public static final String KEY_REPLY_COUNT = "reply_count";
    public static final String KEY_IMAGE_COUNT = "image_count";
    public static final String KEY_OMITTED_POSTS = "omitted_posts";
    public static final String KEY_OMITTED_IMAGE = "omitted_image";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_TRIPCODE = "tripcode";
    public static final String KEY_AUTHOR_ID = "author_id";
    public static final String KEY_CAPCODE = "capcode";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_COUNTRY_NAME = "country_name";
    public static final String KEY_TROLL_COUNTRY = "troll_country";
    public static final String KEY_SPOILER = "spoiler";
    public static final String KEY_CUSTOM_SPOILER = "custom_spoiler";

    @Column(name = KEY_THREAD_ID)           private long threadId;
    @Column(name = KEY_POST_ID, onUniqueConflicts = Column.ConflictAction.REPLACE, onUniqueConflict = Column.ConflictAction.REPLACE, unique = true)             private long postId;
    @Column(name = KEY_CLOSED)              private int closed;
    @Column(name = KEY_STICKY)              private int sticky;
    @Column(name = KEY_READABLE_TIME)       private String readableTime;
    @Column(name = KEY_AUTHOR)              private String author;
    @Column(name = KEY_COMMENT)             private String comment;
    @Column(name = KEY_SUBJECT)             private String subject;
    @Column(name = KEY_OLD_FILENAME)        private String oldFilename;
    @Column(name = KEY_NEW_FILENAME)        private String newFilename;
    @Column(name = KEY_FILE_EXT)            private String fileExt;
    @Column(name = KEY_FILE_WIDTH)          private int fileWidth;
    @Column(name = KEY_FILE_HEIGHT)         private int fileHeight;
    @Column(name = KEY_THUMB_WIDTH)         private int thumbnailWidth;
    @Column(name = KEY_THUMB_HEIGHT)        private int thumbnailHeight;
    @Column(name = KEY_EPOCH)               private long epoch;
    @Column(name = KEY_MD5)                 private String md5;
    @Column(name = KEY_FILE_SIZE)           private int fileSize;
    @Column(name = KEY_RESTO)               private int resto;
    @Column(name = KEY_BUMP_LIMIT)          private int bumplimit;
    @Column(name = KEY_IMAGE_LIMIT)         private int imagelimit;
    @Column(name = KEY_SEMANTIC_URL)        private String semanticUrl;
    @Column(name = KEY_REPLY_COUNT)         private int replyCount;
    @Column(name = KEY_IMAGE_COUNT)         private int imageCount;
    @Column(name = KEY_OMITTED_POSTS)       private int omittedPosts;
    @Column(name = KEY_OMITTED_IMAGE)       private int omittedImages;
    @Column(name = KEY_EMAIL)               private String email;
    @Column(name = KEY_TRIPCODE)            private String tripcode;
    @Column(name = KEY_AUTHOR_ID)           private String authorId;
    @Column(name = KEY_CAPCODE)             private String capcode;
    @Column(name = KEY_COUNTRY)             private String country;
    @Column(name = KEY_COUNTRY_NAME)        private String countryName;
    @Column(name = KEY_TROLL_COUNTRY)       private String trollCountry;
    @Column(name = KEY_SPOILER)             private int spoiler;
    @Column(name = KEY_CUSTOM_SPOILER)      private int customSpoiler;

    public PostModel() { }

    public PostModel(PostModel other) {
        copyValuesFrom(other);
    }

    public PostModel(long threadId, ChanPost other) {
        this.threadId = threadId;
        this.postId = other.getNo();
        this.closed = other.isClosed() ? 1 : 0;
        this.sticky = other.isSticky() ? 1 : 0;
        this.readableTime = other.getNow();
        this.author = other.getName();
        this.comment = other.getCom();
        this.subject = other.getSub();
        this.oldFilename = other.getFilename();
        this.newFilename = other.getTim();
        this.fileExt = other.getExt();
        this.fileWidth = other.getWidth();
        this.fileHeight = other.getHeight();
        this.thumbnailWidth = other.getThumbnailWidth();
        this.thumbnailHeight = other.getThumbnailHeight();
        this.epoch = other.getTime();
        this.md5 = other.getMd5();
        this.fileSize = other.getFsize();
        this.resto = other.getResto();
        this.bumplimit = other.getBumplimit();
        this.imagelimit = other.getImagelimit();
        this.semanticUrl = other.getSemanticUrl();
        this.replyCount = other.getReplies();
        this.imageCount = other.getImages();
        this.omittedPosts = other.getOmittedPosts();
        this.omittedImages = other.getOmittedImages();
        this.email = other.getEmail();
        this.tripcode = other.getTrip();
        this.authorId = other.getId();
        this.capcode = other.getCapcode();
        this.country = other.getCountry();
        this.countryName = other.getCountryName();
        this.trollCountry = other.getTrollCountry();
        this.spoiler = other.getSpoiler();
        this.customSpoiler = other.getCustomSpoiler();
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(KEY_THREAD_ID, threadId);
        values.put(KEY_POST_ID, postId);
        values.put(KEY_CLOSED, closed);
        values.put(KEY_STICKY, sticky);
        values.put(KEY_READABLE_TIME, readableTime);
        values.put(KEY_AUTHOR, author);
        values.put(KEY_COMMENT, comment);
        values.put(KEY_SUBJECT, subject);
        values.put(KEY_OLD_FILENAME, oldFilename);
        values.put(KEY_NEW_FILENAME, newFilename);
        values.put(KEY_FILE_EXT, fileExt);
        values.put(KEY_FILE_WIDTH, fileWidth);
        values.put(KEY_FILE_HEIGHT, fileHeight);
        values.put(KEY_THUMB_WIDTH, thumbnailWidth);
        values.put(KEY_THUMB_HEIGHT, thumbnailHeight);
        values.put(KEY_EPOCH, epoch);
        values.put(KEY_MD5, md5);
        values.put(KEY_FILE_SIZE, fileSize);
        values.put(KEY_RESTO, resto);
        values.put(KEY_BUMP_LIMIT, bumplimit);
        values.put(KEY_IMAGE_LIMIT, imagelimit);
        values.put(KEY_SEMANTIC_URL, semanticUrl);
        values.put(KEY_REPLY_COUNT, replyCount);
        values.put(KEY_IMAGE_COUNT, imageCount);
        values.put(KEY_OMITTED_POSTS, omittedPosts);
        values.put(KEY_OMITTED_IMAGE, omittedImages);
        values.put(KEY_EMAIL, email);
        values.put(KEY_TRIPCODE, tripcode);
        values.put(KEY_AUTHOR_ID, authorId);
        values.put(KEY_CAPCODE, capcode);
        values.put(KEY_COUNTRY, country);
        values.put(KEY_COUNTRY_NAME, countryName);
        values.put(KEY_TROLL_COUNTRY, trollCountry);
        values.put(KEY_SPOILER, spoiler);
        values.put(KEY_CUSTOM_SPOILER, customSpoiler);
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] arg = new DatabaseUtils.WhereArg[1];
        arg[0] = new DatabaseUtils.WhereArg(KEY_POST_ID + "=?", String.valueOf(postId));
        return arg;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof PostModel) {
            PostModel other = (PostModel) model;

            this.threadId = other.threadId;
            this.postId = other.postId;
            this.closed = other.closed;
            this.sticky = other.sticky;
            this.readableTime = other.readableTime;
            this.author = other.author;
            this.comment = other.comment;
            this.subject = other.subject;
            this.oldFilename = other.oldFilename;
            this.newFilename = other.newFilename;
            this.fileExt = other.fileExt;
            this.fileWidth = other.fileWidth;
            this.fileHeight = other.fileHeight;
            this.thumbnailWidth = other.thumbnailWidth;
            this.thumbnailHeight = other.thumbnailHeight;
            this.epoch = other.epoch;
            this.md5 = other.md5;
            this.fileSize = other.fileSize;
            this.resto = other.resto;
            this.bumplimit = other.bumplimit;
            this.imagelimit = other.imagelimit;
            this.semanticUrl = other.semanticUrl;
            this.replyCount = other.replyCount;
            this.imageCount = other.imageCount;
            this.omittedPosts = other.omittedPosts;
            this.omittedImages = other.omittedImages;
            this.email = other.email;
            this.tripcode = other.tripcode;
            this.authorId = other.authorId;
            this.capcode = other.capcode;
            this.country = other.country;
            this.countryName = other.countryName;
            this.trollCountry = other.trollCountry;
            this.spoiler = other.spoiler;
            this.customSpoiler = other.customSpoiler;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(comment) && TextUtils.isEmpty(subject) && TextUtils.isEmpty(oldFilename) && TextUtils.isEmpty(newFilename) && TextUtils.isEmpty(fileExt);
    }

    @Override
    public ChanPost toPost() {
        ChanPost post = new ChanPost();

        post.setNo((int) postId);
        post.setClosed(closed == 1);
        post.setSticky(sticky == 1);
        post.setBumplimit(bumplimit);
        post.setCom(comment);
        post.setSub(subject);
        post.setName(author);
        post.setExt(fileExt);
        post.setFilename(oldFilename);
        post.setFsize(fileSize);
        post.setHeight(fileHeight);
        post.setWidth(fileWidth);
        post.setThumbnailHeight(thumbnailHeight);
        post.setThumbnailWidth(thumbnailWidth);
        post.setImagelimit(imagelimit);
        post.setImages(imageCount);
        post.setReplies(replyCount);
        post.setResto(resto);
        post.setOmittedImages(omittedImages);
        post.setOmittedPosts(omittedPosts);
        post.setSemanticUrl(semanticUrl);
        post.setMd5(md5);
        post.setTim(newFilename);
        post.setTime(epoch);
        post.setEmail(email);
        post.setTrip(tripcode);
        post.setId(authorId);
        post.setCapcode(capcode);
        post.setCountry(country);
        post.setCountryName(countryName);
        post.setTrollCountry(trollCountry);
        post.setSpoiler(spoiler);
        post.setCustomSpoiler(customSpoiler);

        return post;
    }
}
