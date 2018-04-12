package com.meitu.lyz.myapplicationproject.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.meitu.lyz.myapplicationproject.util.ConvertUtils;

import java.text.DecimalFormat;


public class RulerView extends View implements GestureDetector.OnGestureListener {
    private static final String TAG = "RulerView";

    private Context mContext;
    private boolean isHorizontal = true;
    private GestureDetectorCompat gestureDetector;

    private String mUnit = "kg";
    private double mCurrentVale = 12.3;
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
    private int mMaxValue = 10000;
    private int mMinUnitValue = (int) (mMinValue / mUnitValue);
    private int mMaxUnitValue = (int) (mMaxValue / mUnitValue);
    private double mScrollX = 0;

    private int mMaximumVelocity, mMinimumVelocity;


    private float mCurrentX, mLastX;

    private boolean isScrolling = false;
    private boolean isFlinging = false;
    private boolean isScrollingBack = false;


    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OverScroller mScroller;
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

        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();
        mScroller = new OverScroller(mContext, new LinearOutSlowInInterpolator());
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
        if (heightMode != MeasureSpec.EXACTLY) {
            mMeasuredHeight = mMaxLength + mTextValueMargin * 3 + mSmallTextSize + mLargeTextSize;
        }

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


    private void initGestureDetector() {
        setClickable(true);
        setFocusable(true);
        setLongClickable(true);
        gestureDetector = new GestureDetectorCompat(mContext, this);


    }

    private void drawCenterText(Canvas canvas) {
        String s = mCurrentVale + mUnit;

        float center = mMeasuredWidth / 2f;
        paint.setTextSize(mLargeTextSize);
        paint.setAlpha(255);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s, center, mMaxLength + mTextValueMargin * 2 + mSmallTextSize + mLargeTextSize, paint);
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

        paint.setAlpha(255);
        paint.setStrokeWidth(mMaxWidth);
        paint.setColor(mLineColor);

        canvas.drawLine(center, 0, center, mMaxLength, paint);
    }

    private void drawLines(Canvas canvas) {
//        canvas.save();
//
//        if (Math.floor(Math.abs(mScrollX) / mLineMargin) == 0)
//            canvas.translate((float) -mScrollX, 0);
        paint.setStrokeWidth(mNormalWidth);
        paint.setColor(mLineColor);

        int center = mMeasuredWidth / 2;

        int halfNum = center / mLineMargin;
        int currentUnitValue = (int) Math.ceil(mCurrentVale / mUnitValue);
        int startUnitValue = currentUnitValue - halfNum;

        int startX = (int) (center - halfNum * mLineMargin - mScrollX);
        for (int i = 0; i < mLineItems; i++, startUnitValue++, startX += mLineMargin) {
            if (startUnitValue >= mMinUnitValue && startUnitValue <= mMaxUnitValue) {
                int alpha = 255 - 255 * Math.abs(startX - center) / center;
                if (alpha < 0)
                    alpha = 0;
                paint.setAlpha(alpha);
                if (startUnitValue % mUnitCount == 0) {
                    canvas.drawLine(startX, 0, startX, mLongLength, paint);
//                    if (startUnitValue % 10 == 0 && !(currentUnitValue - startUnitValue < 10 && currentUnitValue - startUnitValue >= 0)) {
                    if (startUnitValue % 10 == 0) {
                        drawText(canvas, startUnitValue * mUnitValue, startX);
                    }
                } else {
                    canvas.drawLine(startX, 0, startX, mNormalLength, paint);
                }
            }


        }

//        canvas.restore();

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
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "onScroll: ");
        scroll(distanceX);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "onLongPress: ");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "onFling: ");
        fling((int) -velocityX);
        isScrolling = true;
        return true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: ");

//        if (mVelocityTracker == null)
//            mVelocityTracker = VelocityTracker.obtain();
//        mVelocityTracker.addMovement(event);
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                mLastX = event.getX();
//                break;
//            case MotionEvent.ACTION_MOVE:
//                float distance = event.getX() - mLastX;
//                mLastX = event.getX();
//                scroll(distance);
//                break;
//            case MotionEvent.ACTION_UP:
//                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
//                int velocityX = (int) mVelocityTracker.getXVelocity();
//                if (Math.abs(velocityX) > mMinimumVelocity) {
//                    fling(-velocityX);
//                } else {
//                    scrollBackToCorrectPos();
//                }
//
//                mVelocityTracker.clear();
//                break;
//        }

//
        gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!isFlinging && mScrollX != 0)
                scrollBackToCorrectPos();
        }
        return true;
    }

    @Override
    public void computeScroll() {
        Log.d(TAG, "computeScroll: ");
        if (mScroller.computeScrollOffset()) {
            if (isScrollingBack) {
                scrollBack(mScroller.getCurrX() - mScroller.getStartX());
                if (mScroller.isFinished())
                    isScrollingBack = false;
            } else {
                scroll(mScroller.getCurrX() - mScroller.getStartX());
                if (mScroller.isFinished()) {
                    if (isFlinging)
                        isFlinging = false;
                    if (mScrollX != 0)
                        scrollBackToCorrectPos();
                }
            }
            invalidate();
        }

    }

    private void scroll(float distanceX) {
        mScrollX += distanceX;
        if ((mCurrentVale == mMinValue && distanceX <= 0) ||
                (mCurrentVale == mMaxValue && distanceX >= 0)) {
            mScrollX = 0;
        } else {

            if (mScrollX >= 0)
                mCurrentVale += Math.round(mScrollX / mLineMargin) * mUnitValue;
            else
                mCurrentVale -= Math.round(-mScrollX / mLineMargin) * mUnitValue;

            DecimalFormat format = new DecimalFormat(String.valueOf(mUnitValue));
            mCurrentVale = Double.parseDouble(format.format(mCurrentVale));

            if (mCurrentVale <= mMinValue) {
                mCurrentVale = mMinValue;
            } else if (mCurrentVale >= mMaxValue)
                mCurrentVale = mMaxValue;

            if (mScrollX > 0)
                mScrollX %= mLineMargin;
            else
                mScrollX %= -mLineMargin;
            invalidate();
        }


    }

    private void scrollBack(float distanceX) {
//        if ((mScrollX >= 0 && distanceX >= 0) || (mScrollX <= 0 && distanceX <= 0))
//            mScrollX -= distanceX;
//        else mScrollX += distanceX;

        mScrollX += distanceX;
        invalidate();

    }

    private void fling(int velocity) {
        isFlinging = true;
        if (!mScroller.isFinished())
            if (isHorizontal)
                mScroller.fling(0, 0, velocity, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin, 0, 0);
            else
                mScroller.fling(0, 0, 0, velocity, 0, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin);
        invalidate();

    }

    private void scrollBackToCorrectPos() {
        double scroll;
        if (mScrollX > 0) {
            mCurrentVale += Math.round(mScrollX / mLineMargin) * mUnitValue;
            if (mScrollX < mLineMargin / 2)
                scroll = -mScrollX;
            else scroll = mLineMargin - mScrollX;
        } else {
            mCurrentVale -= Math.round(-mScrollX / mLineMargin) * mUnitValue;
            if (-mScrollX < mLineMargin / 2)
                scroll = -mScrollX;
            else scroll = -(mLineMargin + mScrollX);
        }

        DecimalFormat format = new DecimalFormat(String.valueOf(mUnitValue));
        mCurrentVale = Double.parseDouble(format.format(mCurrentVale));

        isScrollingBack = true;
        mScroller.startScroll(0, 0, (int) scroll, 0);
        invalidate();
    }

}
