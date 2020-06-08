package com.mimireader.chanlib.models;

import java.util.ArrayList;
import java.util.List;

public class ChanArchive {

    private Integer uid;
    private String name;
    private String domain;
    private Boolean http;
    private Boolean https;
    private String software;
    private List<String> boards;
    private List<String> files;
    private Boolean reports;

    public Integer getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public Boolean getHttp() {
        return http;
    }

    public Boolean getHttps() {
        return https;
    }

    public String getSoftware() {
        return software;
    }

    public List<String> getBoards() {
        return boards;
    }

    public List<String> getFiles() {
        return files;
    }

    public Boolean getReports() {
        return reports;
    }

    public static class Builder {

        private Integer uid;
        private String name;
        private String domain;
        private Boolean http;
        private Boolean https;
        private String software;
        private List<String> boards = new ArrayList<>();
        private List<String> files = new ArrayList<>();
        private Boolean reports;

        public Builder() {
        }

        Builder(Integer uid, String name, String domain, Boolean http, Boolean https, String software, List<String> boards, List<String> files, Boolean reports) {
            this.uid = uid;
            this.name = name;
            this.domain = domain;
            this.http = http;
            this.https = https;
            this.software = software;
            this.boards = boards;
            this.files = files;
            this.reports = reports;
        }

        public Builder uid(Integer uid) {
            this.uid = uid;
            return Builder.this;
        }

        public Builder name(String name) {
            this.name = name;
            return Builder.this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return Builder.this;
        }

        public Builder http(Boolean http) {
            this.http = http;
            return Builder.this;
        }

        public Builder https(Boolean https) {
            this.https = https;
            return Builder.this;
        }

        public Builder software(String software) {
            this.software = software;
            return Builder.this;
        }

        public Builder boards(List<String> boards) {
            this.boards = boards;
            return Builder.this;
        }

        public Builder addBoards(String boards) {
            this.boards.add(boards);
            return Builder.this;
        }

        public Builder files(List<String> files) {
            this.files = files;
            return Builder.this;
        }

        public Builder addFiles(String files) {
            this.files.add(files);
            return Builder.this;
        }

        public Builder reports(Boolean reports) {
            this.reports = reports;
            return Builder.this;
        }

        public ChanArchive build() {
            if (this.name == null) {
                throw new NullPointerException("The property \"name\" is null. "
                        + "Please set the value by \"name()\". "
                        + "The properties \"name\", \"domain\" and \"boards\" are required.");
            }
            if (this.domain == null) {
                throw new NullPointerException("The property \"domain\" is null. "
                        + "Please set the value by \"domain()\". "
                        + "The properties \"name\", \"domain\" and \"boards\" are required.");
            }
            if (this.boards == null) {
                throw new NullPointerException("The property \"boards\" is null. "
                        + "Please set the value by \"boards()\". "
                        + "The properties \"name\", \"domain\" and \"boards\" are required.");
            }

            return new ChanArchive(this);
        }
    }

    private ChanArchive(Builder builder) {
        this.uid = builder.uid;
        this.name = builder.name;
        this.domain = builder.domain;
        this.http = builder.http;
        this.https = builder.https;
        this.software = builder.software;
        this.boards = builder.boards;
        this.files = builder.files;
        this.reports = builder.reports;
    }
}
