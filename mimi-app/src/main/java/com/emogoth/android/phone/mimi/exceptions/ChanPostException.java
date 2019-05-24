package com.emogoth.android.phone.mimi.exceptions;

public class ChanPostException extends Exception {
    final String html;

    public ChanPostException(String html) {
        this.html = html;
    }

    public ChanPostException(String message, String html) {
        super(message);
        this.html = html;
    }

    public ChanPostException(String message, Throwable cause, String html) {
        super(message, cause);
        this.html = html;
    }

    public ChanPostException(Throwable cause, String html) {
        super(cause);
        this.html = html;
    }

    public String getHtml() {
        return html;
    }
}
