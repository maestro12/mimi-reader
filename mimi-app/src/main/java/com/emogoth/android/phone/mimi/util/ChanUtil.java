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

package com.emogoth.android.phone.mimi.util;


import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.span.CodeSpan;
import com.emogoth.android.phone.mimi.span.LinkSpan;
import com.emogoth.android.phone.mimi.span.ReplySpan;
import com.emogoth.android.phone.mimi.span.SpoilerSpan;
import com.emogoth.android.phone.mimi.span.YoutubeLinkSpan;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChanUtil {
    private static final String LOG_TAG = ChanUtil.class.getSimpleName();

    private static Pattern YOUTUBE_PATTERN = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");

    public static Spannable getUserName(final Context context, final String userName, final String capcode) {
        String name = userName;

        if (name == null) {
            name = "";
        }

        int color = 0;
        if (!TextUtils.isEmpty(capcode)) {
            if (capcode.toLowerCase().equals("mod")) {
                name = name + " ## Mod";
                color = context.getResources().getColor(R.color.mod_username_color);
            } else if (capcode.toLowerCase().equals("admin")) {
                name = name + " ## Admin";
                color = context.getResources().getColor(R.color.admin_username_color);
            }
        }

        final Spannable nameSpan = new SpannableString(name);

        if (color != 0) {
            final ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
            nameSpan.setSpan(colorSpan, 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return nameSpan;

    }

//    public static Spannable getSpannableFromComment(final Context context, final CharSequence comment, final String boardName, final int threadId, List<String> replies, List<Integer> userPostIds, List<Integer> highlightedPosts) {
//        //SpannableStringBuilder formattedPost = new SpannableStringBuilder();
//
//        final String opTag = " (OP)";
//        final String youTag = " (You)";
//        String rawPost = comment == null ? "" : comment.toString().replaceAll("<br>", "br2nl");
//        Document document = Jsoup.parse(rawPost);
//        document.outputSettings(new Document.OutputSettings().prettyPrint(true));
//        String postWithoutHtml = document.text().replaceAll("br2nl", "\n");
//
//        rawPost = rawPost.replaceAll("br2nl", "\n");
//        rawPost = rawPost.replaceAll("&quot;", "\"");
//        rawPost = rawPost.replaceAll("&#039;", "\'");
//        rawPost = rawPost.replaceAll("&amp;", "&");
//        rawPost = rawPost.replaceAll("&gt;", ">");
//        rawPost = rawPost.replaceAll("&lt;", "<");
//
////        String postWithoutHtml = "";
////        if (comment != null) {
////            postWithoutHtml = String.valueOf(Html.fromHtml(comment.toString()));
////        }
//
//        postWithoutHtml = postWithoutHtml.replace(">>" + threadId, ">>" + threadId + opTag);
//
//        if (userPostIds != null) {
//            for (Integer userPostId : userPostIds) {
//                final String id = userPostId.toString();
//                if (postWithoutHtml.contains(id)) {
//                    Log.d(LOG_TAG, "Found user post: id=" + id);
//
//                    if (postWithoutHtml.contains(">>" + id)) {
//                        Log.d(LOG_TAG, "Found >>" + id);
//                    }
//                }
//                postWithoutHtml = postWithoutHtml.replace(">>" + id, ">>" + id + youTag);
//            }
//        }
//
//        SpannableStringBuilder span = new SpannableStringBuilder(postWithoutHtml);
//
//        // These can be exact matches
//        String replyToken = "class=\"quotelink\">";
//        String quoteToken = "<span class=\"quote\">>";
//        String codeToken = "<pre class=\"prettyprint\">";
//        String spoilerToken = "<s>";
//        String replyEnd = "</a>"; // replies end with anchorEnd
//        String quoteEnd = "</span>"; // quotes end with spanEnd
//        String codeEnd = "<br></pre>";
//        String codeEnd2 = "</pre>";
//        String spoilerEnd = "</s>";
//
//        // Use this string to search in the spannable variable and set the span to whatever is appropriate
//        String stringToSpan = null;
//
//        // These are compared to determine what the cursor is currently on
//        int quotePos;
//        int replyPos;
//        int codePos;
//        int spoilerPos;
//
//        int start;
//        int end;
//        int strlen;
//
//        int quoteCursor = 0;
//        int replyCursor = 0;
//        int codeCursor = 0;
//        int spoilerCursor = 0;
//
//        final ArrayList<String> urlList = new ArrayList<>();
//        final String[] parts = postWithoutHtml.split("\\s+");
//
//        // Attempt to convert each item into an URL.
//        for (String item : parts)
//            try {
//
//                if (item != null) {
//                    if (item.contains("http://") || item.contains("https://")) {
//                        if (item.startsWith(">")) {
//                            item = item.replace(">", "");
//                        }
//
//                        final URL url = new URL(item);
//                        urlList.add(item);
//
//                        final int urlStart = postWithoutHtml.indexOf(item);
//                        final int urlEnd;
//                        final int endPos = urlStart + item.length();
//
//                        if (endPos > span.length()) {
//                            urlEnd = span.length() - 1;
//                        } else {
//                            urlEnd = endPos;
//                        }
//
//                        if (item.contains("youtube.com") || item.contains("youtu.be")) {
//                            final String youtubeId = getYouTubeIdFromUrl(item);
//                            if (!TextUtils.isEmpty(youtubeId)) {
//                                span.setSpan(new YoutubeLinkSpan(context, youtubeId), urlStart, urlEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            } else {
//                                span.setSpan(new LinkSpan(context, item), urlStart, urlEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            }
//                        } else {
//                            span.setSpan(new LinkSpan(context, item), urlStart, urlEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                        }
//                    } else if (item.startsWith(">>>")) {
//                        if (replies != null && item.length() > 3) {
//                            final String reply = item.substring(3);
//                            if (replies.indexOf(reply) < 0) {
//                                replies.add(reply);
//                            }
//                        }
//                    } else if (item.startsWith(">>")) {
//                        if (replies != null && item.length() > 2) {
//                            final String reply = item.substring(2);
//                            if (replies.indexOf(reply) < 0) {
//                                replies.add(reply);
//                            }
//                        }
//                    }
//                }
//
//            } catch (MalformedURLException e) {
//                // If there was an URL that was not it!...
//            }
//
//        rawPost = rawPost.replace(">>" + threadId, ">>" + threadId + opTag);
//
//        if (userPostIds != null) {
//            for (final Integer userPostId : userPostIds) {
//                rawPost = rawPost.replace(">>" + userPostId, ">>" + userPostId + youTag);
//            }
//        }
//
//        boolean done = false;
//        while (!done) {
//
//            start = -1;
//
//            quotePos = rawPost.indexOf(quoteToken, quoteCursor);
//            replyPos = rawPost.indexOf(replyToken, replyCursor);
//            codePos = rawPost.indexOf(codeToken, codeCursor);
//            spoilerPos = rawPost.indexOf(spoilerToken, spoilerCursor);
//
//            if (quotePos >= 0) {
//                quoteCursor = quotePos + quoteToken.length();
//
//                // move the starting position up one place so we don't match with quoteEnd again
//                start = quoteCursor - 1;
//
//                // set end to be the start of quoteEnd
//                end = rawPost.indexOf(quoteEnd, start);
//
//                // reset the cursor for the next loop
//                quoteCursor = end;
//
//                // this needs to be set because start and end aren't the same in the spannable
//                strlen = end - start;
//
//                // get the string we want to set as a custom spannable
//                stringToSpan = rawPost.substring(start, end); // lower case because postWithoutHtml is lower case
//
//                // reuse the start variable to find the start of stringToSpan
//                start = postWithoutHtml.indexOf(stringToSpan);
//                while (start >= 0) {
//
//                    // reuse end to be the end of stringToSpan within the text already in the spannable
//                    end = start + strlen;
//                    if (end > span.length()) {
//                        end = span.length();
//                    }
//
//                    // The magic happens here!
//                    span.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.quote)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                    start = postWithoutHtml.indexOf(stringToSpan, start + stringToSpan.length());
//
//                }
//
//            }
//
//            if (replyPos >= 0) {
//                replyCursor = replyPos + replyToken.length();
//
//                start = replyCursor;
//
//                // set end to be the start of quoteEnd
//                end = rawPost.indexOf(replyEnd, start);
//
//                // reset the cursor for the next loop
//                replyCursor = end;
//
//                // this needs to be set because start and end aren't the same in the spannable
//                strlen = end - start;
//
//                // get the string we want to set as a custom spannable
//                stringToSpan = rawPost.substring(start, end);
//
//                // reuse the start variable to find the start of stringToSpan
//                start = postWithoutHtml.indexOf(stringToSpan);
//
//                while (start >= 0) {
//                    // reuse end to be the end of stringToSpan within the text already in the spannable
//                    end = start + strlen;
//                    if (end > span.length()) {
//                        end = span.length();
//                    }
//
//                    // The magic happens here!
//                    if (replies != null && replies.size() > 0) {
//
//                        final String s = stringToSpan.substring(2).split(" ")[0];
//                        if (StringUtils.isNumericSpace(s)) {
//                            final int id = Integer.valueOf(s);
//                            final boolean highlight = (userPostIds != null && userPostIds.contains(id))
//                                    || (highlightedPosts != null && highlightedPosts.contains(id) && replies.size() > 1);
//
//                            span.setSpan(new ReplySpan(context, boardName, threadId, replies, highlight), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                        } else {
//                            span.setSpan(new ReplySpan(context, boardName, threadId, replies, false), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                        }
//
//                    } else {
//                        span.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.reply)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    }
//
//                    start = postWithoutHtml.indexOf(stringToSpan, start + stringToSpan.length());
//
//                }
//            }
//
//            if (codePos >= 0) {
//                codeCursor = codePos + codeToken.length();
//
//                start = codeCursor;
//                end = rawPost.indexOf(codeEnd, start);
//                if (end < 0) {
//                    end = rawPost.indexOf(codeEnd2, start);
//                }
//
//                codeCursor = end;
//
//                strlen = end - start;
//                stringToSpan = rawPost.substring(start, end);
//                start = postWithoutHtml.indexOf(stringToSpan.replace("<br>", "\n").trim());
//
//                while (start >= 0) {
//                    end = start + strlen;
//                    if (end > span.length()) {
//                        end = span.length();
//                    }
//
//                    span.setSpan(new CodeSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    start = postWithoutHtml.indexOf(stringToSpan, start + stringToSpan.length());
//                }
//
//            }
//
//            if (spoilerPos >= 0) {
//                spoilerCursor = spoilerPos + spoilerToken.length();
//
//                start = spoilerCursor;
//                end = rawPost.indexOf(spoilerEnd, start);
//
//                codeCursor = end;
//                strlen = end - start;
//                stringToSpan = rawPost.substring(start, end);
//                start = postWithoutHtml.indexOf(stringToSpan);
//
//                while (start >= 0) {
//                    end = start + strlen;
//                    if(end > span.length()) {
//                        end = span.length();
//                    }
//
//                    span.setSpan(new SpoilerSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    start = postWithoutHtml.indexOf(stringToSpan, start + stringToSpan.length());
//                }
//            }
//
//            if ((quotePos < 0) && (replyPos < 0) && (codePos < 0) && (spoilerPos < 0)) {
//                done = true;
//            }
//
//        }
//
//        return span;
//    }
//
//    public static String getYouTubeIdFromUrl(String url) {
//        String vId = null;
//        Matcher matcher = YOUTUBE_PATTERN.matcher(url);
//        if (matcher.matches()) {
//            vId = matcher.group(1);
//        }
//        return vId;
//    }

//    public static class CommentBuilder {
//        // final CharSequence comment, final String boardName, final int threadId, List<String> replies, List<Integer> userPostIds, List<Integer> highlightedPosts
//        final CharSequence comment;
//        final String boardName;
//        final int threadId;
//        final List<String> replies;
//        final List<Integer> userPostIds;
//        final List<Integer> highlithtedPosts;
//        final boolean spoilers;
//        final boolean code;
//    }
}
