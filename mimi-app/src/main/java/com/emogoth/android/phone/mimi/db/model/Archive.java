package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

@Table(name = "archives")
public class Archive extends BaseModel {
    public static final String TABLE_NAME = "archives";

    public static final String UID = "uid";
    public static final String NAME = "name";
    public static final String DOMAIN = "domain";
    public static final String HTTPS = "https";
    public static final String SOFTWARE = "software";
    public static final String BOARD = "board";
    public static final String REPORTS = "reports";

    @Column(name = UID)
    public int uid;

    @Column(name = NAME)
    public String name;

    @Column(name = DOMAIN)
    public String domain;

    @Column(name = HTTPS)
    public boolean https;

    @Column(name = SOFTWARE)
    public String software;

    @Column(name = BOARD)
    public String board;

    @Column(name = REPORTS)
    public boolean reports;

    public Archive(){
        this.uid = -1;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(UID, this.uid);
        values.put(NAME, this.name);
        values.put(DOMAIN, this.domain);
        values.put(HTTPS, this.https);
        values.put(SOFTWARE, this.software);
        values.put(BOARD, this.board);
        values.put(REPORTS, this.reports);
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[2];
        args[0] = new DatabaseUtils.WhereArg(DOMAIN + "=?", this.domain);
        args[1] = new DatabaseUtils.WhereArg(BOARD + "=?", this.board);
        return args;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof Archive) {
            Archive archiveModel = (Archive) model;
            this.uid = archiveModel.uid;
            this.name = archiveModel.name;
            this.domain = archiveModel.domain;
            this.https = archiveModel.https;
            this.software = archiveModel.software;
            this.board = archiveModel.board;
            this.reports = archiveModel.reports;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.uid == -1;
    }
}
