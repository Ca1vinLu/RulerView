package com.meitu.lyz.myapplicationproject.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;

import com.meitu.lyz.myapplicationproject.util.ConvertUtils;

import java.text.DecimalFormat;


public class RulerView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private static final String TAG = "RulerView";
    private Context mContext;
    private GestureDetectorCompat gestureDetector;

    private String mUnit = "kg";
    private double mCurrentVale = 5.1;
    private double mUnitValue = 0.1;
    private int mUnitCount = 5;

    private int mLineColor;
    private int mLineMargin = 8;

    private int mMaxWidth = 2;
    private int mNormalWidth = 1;

    private int mMaxLength = 24;
    private int mNormalLength = 12;
    private int mLongLength = 18;

    private int mMeasuredWidth;
    private int mMeasuredHeight;

    private int mDisplayWidth;
    private int mDisplayHeight;

    private int mTextValueMargin = 10;
    private int mLargeTextSize = 16;
    private int mSmallTextSize = 12;

    private int mLineItems;
    private int mMinValue = 0;
    private int mMaxValue = Integer.MAX_VALUE;
    private int mMinUnitValue = (int) (mMinValue / mUnitValue);
    private int mMaxUnitValue = (int) (mMaxValue / mUnitValue);


    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] lines;

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initGestureDetector();
        initAttr();
        initLines();
        
        mScroller=new Scroller(mContext);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        mMeasuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        mMeasuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY)
            mMeasuredWidth = mDisplayWidth;
        if (heightMode != MeasureSpec.EXACTLY)
            mMeasuredHeight = mDisplayHeight;

        setMeasuredDimension(mMeasuredWidth, mMeasuredHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mLineItems = mMeasuredWidth / mLineMargin;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


//        canvas.save();
//        int offSet = getOffSet();
//
//        paint.setStrokeWidth(mNormalWidth);
//
//        canvas.translate(center - mLineMargin * offSet, 0);
//        canvas.drawLines(lines, paint);
//        canvas.translate(mLineMargin * (10 - offSet), 0);
//
//        int size = (int) (mMeasuredWidth * 0.5f / (mLineMargin * 10) + 0.5);
//
//        for (int i = 0; i < size; i++) {
//            canvas.drawLines(lines, paint);
//            canvas.translate(mLineMargin * 10, 0);
//        }
//        canvas.restore();
//
//        canvas.save();
//        canvas.translate(center - mLineMargin * (offSet + 10), 0);
//        for (int i = 0; i < size; i++) {
//            canvas.drawLines(lines, paint);
//            canvas.translate(-mLineMargin * 10, 0);
//        }
//        canvas.restore();

        drawLines(canvas);
        drawCenterText(canvas);
        drawBaseLine(canvas);
    }

    private void initAttr() {
        mMaxWidth = ConvertUtils.dp2px(mMaxWidth, mContext);
        mNormalWidth = ConvertUtils.dp2px(mNormalWidth, mContext);
        mMaxLength = ConvertUtils.dp2px(mMaxLength, mContext);
        mNormalLength = ConvertUtils.dp2px(mNormalLength, mContext);
        mLongLength = ConvertUtils.dp2px(mLongLength, mContext);
        mLineMargin = ConvertUtils.dp2px(mLineMargin, mContext);
        mLargeTextSize = ConvertUtils.sp2px(mLargeTextSize, mContext);
        mSmallTextSize = ConvertUtils.sp2px(mSmallTextSize, mContext);

        mTextValueMargin = ConvertUtils.dp2px(mTextValueMargin, mContext);

        mLineColor = Color.parseColor("#294383");


        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;
    }

    private void initLines() {
        lines = new float[40];
        for (int i = 0, startX = 0; i < 40; i += 4) {
            lines[i] = startX;
            lines[i + 2] = startX;

            startX += mLineMargin;
        }

        for (int i = 1; i < 40; i += 4) {
            lines[i] = 0;
            lines[i + 2] = mNormalLength;

        }

        lines[3] = mLongLength;
        lines[23] = mLongLength;

    }

    private void initGestureDetector() {
        setClickable(true);
        setFocusable(true);
        setLongClickable(true);
        setOnTouchListener(this);
        gestureDetector = new GestureDetectorCompat(mContext, this);

    }

    private void drawCenterText(Canvas canvas) {
        String s = mCurrentVale + mUnit;

        float center = mMeasuredWidth / 2f;
        paint.setTextSize(mLargeTextSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s, center, mMaxLength + mTextValueMargin + mLargeTextSize, paint);
    }

    private void drawText(Canvas canvas, double value, int startX) {
        String s = value + mUnit;

        paint.setTextSize(mSmallTextSize);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s, startX, mMaxLength + mTextValueMargin + mSmallTextSize, paint);
    }

    private void drawBaseLine(Canvas canvas) {
        int center = mMeasuredWidth / 2;

        paint.setStrokeWidth(mMaxWidth);
        paint.setColor(mLineColor);

        canvas.drawLine(center, 0, center, mMaxLength, paint);
    }

    private void drawLines(Canvas canvas) {
        paint.setStrokeWidth(mNormalWidth);
        paint.setColor(mLineColor);

        int center = mMeasuredWidth / 2;

        int halfNum = center / mLineMargin;
        int currentUnitValue = (int) (mCurrentVale / mUnitValue);
        int startUnitValue = currentUnitValue - halfNum;

        int startX = center - halfNum * mLineMargin;
        for (int i = 0; i < mLineItems; i++, startUnitValue++, startX += mLineMargin) {
            if (startUnitValue >= mMinUnitValue && startUnitValue <= mMaxUnitValue)
                if (startUnitValue % mUnitCount == 0) {
                    canvas.drawLine(startX, 0, startX, mLongLength, paint);
                    if (startUnitValue % 10 == 0 && !(currentUnitValue - startUnitValue < 10 && currentUnitValue - startUnitValue >= 0)) {
                        drawText(canvas, startUnitValue * mUnitValue, startX);
                    }
                } else {
                    canvas.drawLine(startX, 0, startX, mNormalLength, paint);
                }


        }

    }


    @Override
    public boolean onDown(MotionEvent e) {
        Log.d(TAG, "onDown: ");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d(TAG, "onShowPress: ");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp: ");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "onScroll: ");
        if (mCurrentVale == mMinValue && distanceX <= 0)
            return true;
        mCurrentVale += (distanceX / mLineMargin / 4);
        DecimalFormat format = new DecimalFormat(String.valueOf(mUnitValue));
        mCurrentVale = Double.parseDouble(format.format(mCurrentVale));

        if (mCurrentVale <= mMinValue) {
            mCurrentVale = mMinValue;
        }
        invalidate();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "onLongPress: ");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "onFling: ");
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch: ");
        return gestureDetector.onTouchEvent(event);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
