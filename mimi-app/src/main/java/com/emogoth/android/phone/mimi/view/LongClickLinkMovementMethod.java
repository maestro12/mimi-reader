
package com.emogoth.android.phone.mimi.view;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.span.LongClickableSpan;

public class LongClickLinkMovementMethod extends LinkMovementMethod {

    private Long lastClickTime = 0l;
    private int lastRawX = 0;
    private int lastRawY = 0;

    private LongClickableSpan thisLink;
    private TextView lastWidget;

    private java.lang.Thread longClickSensor;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
                                MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            lastRawX = (int) event.getRawX();
            lastRawY = (int) event.getRawY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            LongClickableSpan[] link = buffer.getSpans(off, off, LongClickableSpan.class);
//            SpoilerSpan[] check = buffer.getSpans(off, off, SpoilerSpan.class);

            if (link.length != 0) {
                // If the user is pressing down and there is no thread, make one and start it
                if (event.getAction() == MotionEvent.ACTION_DOWN && longClickSensor == null) {
                    thisLink = link[0];
                    lastWidget = widget;
                    longClickSensor = new java.lang.Thread(new MyDelayedAction());
                    longClickSensor.start();
                }
                // If the user has let go and there was a thread, stop it and forget about the thread
                if (event.getAction() == MotionEvent.ACTION_UP && longClickSensor != null) {
                    longClickSensor.interrupt();
                    longClickSensor = null;
                }
            }

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - lastClickTime < 800) {
                        link[0].onClick(widget);
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    // Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                    lastClickTime = System.currentTimeMillis();
                }
                return true;
            }
        } else {
            int deltaX = Math.abs((int) event.getRawX() - lastRawX);
            int deltaY = Math.abs((int) event.getRawY() - lastRawY);
            // must hold finger still
            if ((longClickSensor != null) && ((deltaX > 10) || (deltaY > 10))) {
                longClickSensor.interrupt();
                longClickSensor = null;
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    private class MyDelayedAction implements Runnable {
        private final long delayMs = ViewConfiguration.getLongPressTimeout();

        public void run() {
            try {
                java.lang.Thread.sleep(delayMs); // Sleep for a while
                thisLink.onLongClick(lastWidget);    // If the thread is still around after the sleep, do the work
            } catch (InterruptedException e) {
                return;
            }
        }
    }


    public static MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new LongClickLinkMovementMethod();

        return sInstance;
    }

    private static LongClickLinkMovementMethod sInstance;
}
