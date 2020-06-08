package com.emogoth.android.phone.mimi.fourchan.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FourChanArchive {
    @SerializedName("uid")
    @Expose
    private Integer uid;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("domain")
    @Expose
    private String domain;
    @SerializedName("http")
    @Expose
    private Boolean http;
    @SerializedName("https")
    @Expose
    private Boolean https;
    @SerializedName("software")
    @Expose
    private String software;
    @SerializedName("boards")
    @Expose
    private List<String> boards = null;
    @SerializedName("files")
    @Expose
    private List<String> files = null;
    @SerializedName("reports")
    @Expose
    private Boolean reports;

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getHttp() {
        return http;
    }

    public void setHttp(Boolean http) {
        this.http = http;
    }

    public Boolean getHttps() {
        return https;
    }

    public void setHttps(Boolean https) {
        this.https = https;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public List<String> getBoards() {
        return boards;
    }

    public void setBoards(List<String> boards) {
        this.boards = boards;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public Boolean getReports() {
        return reports;
    }

    public void setReports(Boolean reports) {
        this.reports = reports;
    }
}
