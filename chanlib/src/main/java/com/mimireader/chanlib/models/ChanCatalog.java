/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.mimireader.chanlib.models;


import java.util.ArrayList;
import java.util.List;

public class ChanCatalog {

    private String boardName;
    private String boardTitle;
    private List<ChanPost> posts;

    public ChanCatalog() {
        posts = new ArrayList<>();
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public void setBoardTitle(String boardTitle) {
        this.boardTitle = boardTitle;
    }

    public List<ChanPost> getPosts() {
        return posts;
    }

    public void setPosts(List<ChanPost> posts) {
        this.posts = new ArrayList<>(posts);
    }

    public void addPosts(List<ChanPost> posts) {
        this.posts.addAll(posts);
    }
}
