package com.meitu.lyz.myapplicationproject.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import com.meitu.lyz.myapplicationproject.R;
import com.meitu.lyz.myapplicationproject.util.ConvertUtils;

import java.text.DecimalFormat;


public class RulerView extends View {
    private static final String TAG = "RulerView";

    public static final int RULER_GRAVITY_TOP = 0;
    public static final int RULER_GRAVITY_BOTTOM = 1;
    public static final int RULER_GRAVITY_LEFT = 2;
    public static final int RULER_GRAVITY_RIGHT = 3;

    private Context mContext;
    private boolean isHorizontal = true;
    private int mRulerGravity = RULER_GRAVITY_TOP;

    private String mUnit = "kg";
    private double mCurrentVale = 12.3;
    private double mUnitValue = 0.1;
    private int mUnitCount = 5;

    private int mLineColor;
    private int mLineMargin = 8;

    private int mBoldWidth = 2;
    private int mNormalWidth = 1;

    private int mMaxLength = 24;
    private int mNormalLength = 12;
    private int mLongLength = 18;

    private int mMeasuredWidth;
    private int mMeasuredHeight;
    private int mCenterXY;

    private int mDisplayWidth;
    private int mDisplayHeight;

    private int mTextColor;
    private int mTextValueMargin = 10;
    private int mLargeTextSize = 16;
    private int mSmallTextSize = 12;

    private int mLineItems;
    private double mMinValue = 0;
    private double mMaxValue = 100;
    private int mMinUnitValue = (int) (mMinValue / mUnitValue);
    private int mMaxUnitValue = (int) (mMaxValue / mUnitValue);
    private double mScrollXY = 0;

    private boolean openCorrection = true;

    private int mMaximumVelocity, mMinimumVelocity;


    private float mCurrentXY, mLastXY, mLastScrollerXY;


    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        initAttr(attrs);
        initScroller();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        mMeasuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        mMeasuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (isHorizontal) {
            if (widthMode != MeasureSpec.EXACTLY)
                mMeasuredWidth = mDisplayWidth;
            if (heightMode != MeasureSpec.EXACTLY) {
                mMeasuredHeight = mMaxLength + mTextValueMargin * 3 + mLargeTextSize;
            }

            mCenterXY = mMeasuredWidth / 2;
        } else {
            if (widthMode != MeasureSpec.EXACTLY) {
                mMeasuredWidth = mMaxLength + mTextValueMargin * 3;
                mPaint.setTextSize(mLargeTextSize);
                mMeasuredWidth += mPaint.measureText(mMaxValue + mUnit);
            }
            if (heightMode != MeasureSpec.EXACTLY) {
                mMeasuredHeight = mDisplayHeight;
            }
            mCenterXY = mMeasuredHeight / 2;
        }

        setMeasuredDimension(mMeasuredWidth, mMeasuredHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (isHorizontal)
            mLineItems = mMeasuredWidth / mLineMargin;
        else
            mLineItems = mMeasuredHeight / mLineMargin;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawCenterText(canvas);
        drawBaseLine(canvas);
        drawLines(canvas);
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.RulerView);

        isHorizontal = typedArray.getInt(R.styleable.RulerView_orientation, 0) == 0;
        mUnit = typedArray.getString(R.styleable.RulerView_unit);
        if (TextUtils.isEmpty(mUnit))
            mUnit = "kg";
        mUnitValue = typedArray.getInt(R.styleable.RulerView_unitValue, -1);
        mUnitValue = Math.pow(10, mUnitValue);
        mUnitCount = typedArray.getInt(R.styleable.RulerView_unitCount, mUnitCount);
        mMaxValue = typedArray.getFloat(R.styleable.RulerView_maxValue, 100f);
        mMinValue = typedArray.getFloat(R.styleable.RulerView_minValue, 0f);
        mCurrentVale = typedArray.getFloat(R.styleable.RulerView_value, (float) ((mMaxValue + mMinValue) / 2));

        DecimalFormat format = new DecimalFormat(String.valueOf(mUnitValue));
        mCurrentVale = Double.parseDouble(format.format(mCurrentVale));

        mLineColor = typedArray.getColor(R.styleable.RulerView_lineColor, Color.parseColor("#294383"));
        mTextColor = typedArray.getColor(R.styleable.RulerView_textColor, Color.parseColor("#294383"));

        mLineMargin = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineMargin, ConvertUtils.dp2px(mLineMargin, mContext));
        mTextValueMargin = typedArray.getDimensionPixelOffset(R.styleable.RulerView_textValueMargin, ConvertUtils.dp2px(mTextValueMargin, mContext));
        mBoldWidth = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineBoldWidth, ConvertUtils.dp2px(mBoldWidth, mContext));
        mNormalWidth = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineNormalWidth, ConvertUtils.dp2px(mNormalWidth, mContext));

        mMaxLength = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineMaxLength, ConvertUtils.dp2px(mMaxLength, mContext));
        mNormalLength = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineNormalLength, ConvertUtils.dp2px(mNormalLength, mContext));
        mLongLength = typedArray.getDimensionPixelOffset(R.styleable.RulerView_lineLongLength, ConvertUtils.dp2px(mLongLength, mContext));


        mLargeTextSize = typedArray.getDimensionPixelOffset(R.styleable.RulerView_largeTextSize, ConvertUtils.sp2px(mLargeTextSize, mContext));
        mSmallTextSize = typedArray.getDimensionPixelOffset(R.styleable.RulerView_smallTextSize, ConvertUtils.sp2px(mSmallTextSize, mContext));

        openCorrection = typedArray.getBoolean(R.styleable.RulerView_openCorrection, true);
        mRulerGravity = typedArray.getInt(R.styleable.RulerView_rulerGravity, isHorizontal ? RULER_GRAVITY_TOP : RULER_GRAVITY_LEFT);

        typedArray.recycle();


        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;

        if (isHorizontal)
            mPaint.setTextAlign(Paint.Align.CENTER);
        else if (mRulerGravity == RULER_GRAVITY_LEFT)
            mPaint.setTextAlign(Paint.Align.LEFT);
        else if (mRulerGravity == RULER_GRAVITY_RIGHT)
            mPaint.setTextAlign(Paint.Align.RIGHT);
    }


    private void initScroller() {
        setClickable(true);
        setFocusable(true);
        setLongClickable(true);

        mMaximumVelocity = ViewConfiguration.get(mContext)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(mContext)
                .getScaledMinimumFlingVelocity();
        mScroller = new OverScroller(mContext, new LinearOutSlowInInterpolator());
    }

    private void drawCenterText(Canvas canvas) {
        String s = mCurrentVale + mUnit;

        mPaint.setTextSize(mLargeTextSize);
        mPaint.setColor(mTextColor);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setAlpha(255);


        if (isHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawText(s, mCenterXY, mMaxLength + mTextValueMargin + mLargeTextSize, mPaint);
        else if (isHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawText(s, mCenterXY, mMeasuredHeight - (mMaxLength + mTextValueMargin), mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawText(s, mMaxLength + mTextValueMargin + mSmallTextSize, mCenterXY, mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawText(s, mMeasuredWidth - (mMaxLength + mTextValueMargin), mCenterXY, mPaint);
    }

    private void drawText(Canvas canvas, double value, int startXY, int alpha) {

        String s = value + mUnit;

        float textWidth = mPaint.measureText(s);
        float centerTextWidth = mPaint.measureText(mCurrentVale + mUnit);

        if (isHorizontal && Math.abs(startXY - mCenterXY) <= ((textWidth + centerTextWidth) / 2 + mSmallTextSize + mLargeTextSize))
            return;
        else if (!isHorizontal && Math.abs(startXY - mCenterXY) <= ((mSmallTextSize + mLargeTextSize) / 2 + mSmallTextSize))
            return;

        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mSmallTextSize);
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.setAlpha(alpha);

        if (isHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawText(s, startXY, mMaxLength + mTextValueMargin + mSmallTextSize, mPaint);
        else if (isHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawText(s, startXY, mMeasuredHeight - (mMaxLength + mTextValueMargin), mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawText(s, mMaxLength + mTextValueMargin + mSmallTextSize, startXY, mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawText(s, mMeasuredWidth - (mMaxLength + mTextValueMargin), startXY, mPaint);

    }

    private void drawBaseLine(Canvas canvas) {

        mPaint.setAlpha(255);
        mPaint.setStrokeWidth(mBoldWidth);
        mPaint.setColor(mLineColor);

        if (isHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawLine(mCenterXY, 0, mCenterXY, mMaxLength, mPaint);
        else if (isHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawLine(mCenterXY, mMeasuredHeight, mCenterXY, mMeasuredHeight - mMaxLength, mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawLine(0, mCenterXY, mMaxLength, mCenterXY, mPaint);
        else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawLine(mMeasuredWidth, mCenterXY, mMeasuredWidth - mMaxLength, mCenterXY, mPaint);
    }

    private void drawLines(Canvas canvas) {

        mPaint.setStrokeWidth(mNormalWidth);


        int halfNum = mCenterXY / mLineMargin;
        int currentUnitValue = (int) Math.ceil(mCurrentVale / mUnitValue);
        int startUnitValue = currentUnitValue - halfNum;

        int startXY = (int) (mCenterXY - halfNum * mLineMargin - mScrollXY);
        for (int i = 0; i < mLineItems; i++, startUnitValue++, startXY += mLineMargin) {
            if (startUnitValue >= mMinUnitValue && startUnitValue <= mMaxUnitValue) {

                int alpha = 255 - 255 * Math.abs(startXY - mCenterXY) / mCenterXY;
                alpha = alpha >= 0 ? alpha : 0;
                mPaint.setColor(mLineColor);
                mPaint.setAlpha(alpha);

                if (startUnitValue % mUnitCount == 0) {

                    if (isHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
                        canvas.drawLine(startXY, 0, startXY, mLongLength, mPaint);
                    else if (isHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
                        canvas.drawLine(startXY, mMeasuredHeight, startXY, mMeasuredHeight - mLongLength, mPaint);
                    else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
                        canvas.drawLine(0, startXY, mLongLength, startXY, mPaint);
                    else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
                        canvas.drawLine(mMeasuredWidth, startXY, mMeasuredWidth - mLongLength, startXY, mPaint);


                    if (startUnitValue % 10 == 0) {
                        drawText(canvas, startUnitValue * mUnitValue, startXY, alpha);
                    }
                } else {

                    if (isHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
                        canvas.drawLine(startXY, 0, startXY, mNormalLength, mPaint);
                    else if (isHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
                        canvas.drawLine(startXY, mMeasuredHeight, startXY, mMeasuredHeight - mNormalLength, mPaint);
                    else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
                        canvas.drawLine(0, startXY, mNormalLength, startXY, mPaint);
                    else if (!isHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
                        canvas.drawLine(mMeasuredWidth, startXY, mMeasuredWidth - mNormalLength, startXY, mPaint);

                }
            }


        }


    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: ");

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                mLastXY = isHorizontal ? event.getX() : event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = isHorizontal ? mLastXY - event.getX() : mLastXY - event.getY();
                mLastXY = isHorizontal ? event.getX() : event.getY();
                scroll(distance);
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = isHorizontal ? (int) mVelocityTracker.getXVelocity() : (int) mVelocityTracker.getYVelocity();
                if (Math.abs(velocityX) > mMinimumVelocity) {
                    fling(-velocityX);
                } else if (openCorrection) {
                    scrollBackToCorrectPos();
                }

                mVelocityTracker.clear();
                break;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        Log.d(TAG, "computeScroll: ");
        if (mScroller.computeScrollOffset()) {
            Log.d(TAG, "computeScroll: Scroll");
            float distance = isHorizontal ? mScroller.getCurrX() - mLastScrollerXY : mScroller.getCurrY() - mLastScrollerXY;
            scroll(distance);
            mLastScrollerXY = isHorizontal ? mScroller.getCurrX() : mScroller.getCurrY();
            if (openCorrection && mScroller.isFinished() && mScrollXY != 0)
                scrollBackToCorrectPos();
        }

    }

    private void scroll(float distanceX) {
        Log.d(TAG, "scroll: " + distanceX);
        mScrollXY += distanceX;
        if ((mCurrentVale == mMinValue && distanceX <= 0) ||
                (mCurrentVale == mMaxValue && distanceX >= 0)) {
            mScrollXY = 0;
        } else {

            if (mScrollXY > 0) {
                mScrollXY = Math.round(mScrollXY);
                mCurrentVale += Math.floor(mScrollXY / mLineMargin) * mUnitValue;
            } else {
                mScrollXY = -Math.round(-mScrollXY);
                mCurrentVale -= Math.floor(-mScrollXY / mLineMargin) * mUnitValue;
            }

            DecimalFormat format = new DecimalFormat(String.valueOf(mUnitValue));
            mCurrentVale = Double.parseDouble(format.format(mCurrentVale));
            if (mCurrentVale <= mMinValue) {
                mCurrentVale = mMinValue;
            } else if (mCurrentVale >= mMaxValue)
                mCurrentVale = mMaxValue;

            mScrollXY %= mLineMargin;
            invalidate();
        }


    }


    private void fling(int velocity) {
        mLastScrollerXY = 0;
        if (isHorizontal)
            mScroller.fling(0, 0, velocity, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin, 0, 0);
        else
            mScroller.fling(0, 0, 0, velocity, 0, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin);
        invalidate();

    }

    private void scrollBackToCorrectPos() {
        double scroll;
        mScrollXY %= mLineMargin;
        if (mScrollXY > 0) {
            if (mScrollXY <= mLineMargin / 2)
                scroll = -mScrollXY;
            else {
                scroll = mLineMargin - mScrollXY;
            }
        } else {
            if (-mScrollXY <= mLineMargin / 2)
                scroll = -mScrollXY;
            else scroll = -(mLineMargin + mScrollXY);
        }


        Log.d(TAG, "scrollBackToCorrectPos: " + scroll + " mScrollXY " + mScrollXY);
        mLastScrollerXY = 0;
        if (isHorizontal)
            mScroller.startScroll(0, 0, (int) scroll, 0);
        else
            mScroller.startScroll(0, 0, 0, (int) scroll);
        invalidate();
    }

    public boolean isHorizontal() {
        return isHorizontal;
    }

    public double getCurrentVale() {
        return mCurrentVale;
    }

    public double getMinValue() {
        return mMinValue;
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public boolean isOpenCorrection() {
        return openCorrection;
    }

    public void setHorizontal(boolean horizontal) {
        isHorizontal = horizontal;
    }

    public void setUnit(String mUnit) {
        this.mUnit = mUnit;
    }

    public void setCurrentVale(double mCurrentVale) {
        this.mCurrentVale = mCurrentVale;
    }

    public void setUnitValue(double mUnitValue) {
        this.mUnitValue = mUnitValue;
    }

    public void setUnitCount(int mUnitCount) {
        this.mUnitCount = mUnitCount;
    }

    public void setLineColor(int mLineColor) {
        this.mLineColor = mLineColor;
    }

    public void setLineMargin(int mLineMargin) {
        this.mLineMargin = mLineMargin;
    }

    public void setBoldWidth(int mBoldWidth) {
        this.mBoldWidth = mBoldWidth;
    }

    public void setNormalWidth(int mNormalWidth) {
        this.mNormalWidth = mNormalWidth;
    }

    public void setMaxLength(int mMaxLength) {
        this.mMaxLength = mMaxLength;
    }

    public void setNormalLength(int mNormalLength) {
        this.mNormalLength = mNormalLength;
    }

    public void setLongLength(int mLongLength) {
        this.mLongLength = mLongLength;
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
    }

    public void setTextValueMargin(int mTextValueMargin) {
        this.mTextValueMargin = mTextValueMargin;
    }

    public void setLargeTextSize(int mLargeTextSize) {
        this.mLargeTextSize = mLargeTextSize;
    }

    public void setSmallTextSize(int mSmallTextSize) {
        this.mSmallTextSize = mSmallTextSize;
    }

    public void setMinValue(double mMinValue) {
        this.mMinValue = mMinValue;
    }

    public void setMaxValue(double mMaxValue) {
        this.mMaxValue = mMaxValue;
    }

    public void setOpenCorrection(boolean openCorrection) {
        this.openCorrection = openCorrection;
    }
}
