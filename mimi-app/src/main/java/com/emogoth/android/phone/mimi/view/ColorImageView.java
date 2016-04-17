
package com.emogoth.android.phone.mimi.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.emogoth.android.phone.mimi.R;

/**
 * Created by ferranribell on 19/08/15.
 */
public class ColorImageView extends ImageView {

    private int mColor;
    private int mBorderColor;
    private int mBorderColorSelected;

    public ColorImageView(Context context) {
        super(context);
        init();
    }

    public ColorImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ColorPickerImageView,
                0, 0);
        try {
            setBorderColor(a.getInteger(R.styleable.ColorPickerImageView_imageBorderColor, R.color.border));
            setBorderColorSelected(a.getInteger(R.styleable.ColorPickerImageView_imageBorderColorSelected, R.color.border_selected));
            setBackgroundColor(a.getInteger(R.styleable.ColorPickerImageView_imageBackgroundColor, R.color.border_selected));
        } finally {
            a.recycle();
        }
    }

    private void init() {
        mBorderColor = getResources().getColor(R.color.border);
        mBorderColorSelected = getResources().getColor(R.color.border_selected);
        mColor = getResources().getColor(R.color.circle);
        setBackground(getResources().getDrawable(R.drawable.circle_border));
        setImageDrawable(getResources().getDrawable(R.drawable.circle));
    }

    public void setBackgroundColor(int color) {
        mColor = color;
    }

    public int getBackgroundColor() {
        return mColor;
    }

    public void setBorderColor(int color) {
        mBorderColor = color;
    }

    public void setBorderColorSelected(int color) {
        mBorderColorSelected = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable drawableBackground = getResources().getDrawable(R.drawable.circle);
        drawableBackground = DrawableCompat.wrap(drawableBackground);
        drawableBackground.mutate().setColorFilter(mColor, PorterDuff.Mode.SRC_IN);

        setImageDrawable(drawableBackground);

        Drawable drawableBorder = getResources().getDrawable(R.drawable.circle_border);
        drawableBorder = DrawableCompat.wrap(drawableBorder);
        int borderColor = mBorderColor;
        if(isSelected()){
            borderColor = mBorderColorSelected;
        }
        drawableBorder.mutate().setColorFilter(borderColor, PorterDuff.Mode.SRC_IN);
        setBackground(drawableBorder);
    }
}
