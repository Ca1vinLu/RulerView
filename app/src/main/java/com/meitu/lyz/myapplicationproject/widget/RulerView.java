package com.meitu.lyz.myapplicationproject.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
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


/**
 * 刻度尺View，可自定义水平或垂直，及尺的位置
 * 支持fling手势，滑动后可自动校准
 *
 * @author LYZ 2018-04-13
 */
public class RulerView extends View {
    private static final String TAG = "RulerView";

    //尺相对于整个View的位置，水平尺只能为TOP|BOTTOM，垂直尺只能为LEFT|RIGHT
    public static final int RULER_GRAVITY_TOP = 0;
    public static final int RULER_GRAVITY_BOTTOM = 1;
    public static final int RULER_GRAVITY_LEFT = 2;
    public static final int RULER_GRAVITY_RIGHT = 3;
    private int mRulerGravity = RULER_GRAVITY_TOP;

    private Context mContext;

    //尺的方向
    private boolean mIsHorizontal = true;
    //是否开启校准
    private boolean mOpenCorrection = true;

    //刻度单位
    private String mUnit = "kg";
    //当前刻度值
    private double mCurrentVale;
    //单位值
    private double mUnitValue;
    //长刻度的间隔
    private int mValueInterval;
    //刻度文字的间隔
    private int mTextInterval;

    //中间标识线的宽度
    private int mBoldWidth = 2;
    //刻度线的宽度
    private int mNormalWidth = 1;

    //中间标识线的长度
    private int mMaxLength = 24;
    //短刻度线的长度
    private int mNormalLength = 12;
    //长刻度线的长度
    private int mLongLength = 18;

    //View的宽高
    private int mMeasuredWidth;
    private int mMeasuredHeight;
    //中间线的位置
    private int mCenterXY;

    //屏幕的宽高
    private int mDisplayWidth;
    private int mDisplayHeight;

    //文字和刻度线的颜色
    private int mLineColor;
    private int mTextColor;
    //刻度线的间距
    private int mLineMargin = 8;
    //尺和刻度值文字的间距
    private int mTextValueMargin = 10;

    //中间刻度值文字的大小
    private int mLargeTextSize = 16;
    //两边刻度值文字的大小
    private int mSmallTextSize = 12;

    //可显示的最大刻度数
    private int mLineItems;
    //刻度最大最小值
    private double mMinValue;
    private double mMaxValue;
    //换算后的刻度最大最小单位值
    private int mMinUnitValue;
    private int mMaxUnitValue;


    //偏移值
    private double mScrollXY;
    //滑动中的当前坐标值及scroller计算的当前值
    private float mLastXY, mLastScrollerXY;


    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity, mMinimumVelocity;
    private DecimalFormat mDecimalFormat;

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

        //计算View的宽高
        if (mIsHorizontal) {
            if (widthMode != MeasureSpec.EXACTLY)
                mMeasuredWidth = mDisplayWidth;
            if (heightMode != MeasureSpec.EXACTLY) {
                mMeasuredHeight = mMaxLength + mTextValueMargin * 3 + mLargeTextSize;
            }

        } else {
            if (widthMode != MeasureSpec.EXACTLY) {
                mMeasuredWidth = mMaxLength + mTextValueMargin * 3;
                mPaint.setTextSize(mLargeTextSize);
                mMeasuredWidth += mPaint.measureText(mMaxValue + mUnit);
            }
            if (heightMode != MeasureSpec.EXACTLY) {
                mMeasuredHeight = mDisplayHeight;
            }
        }

        setMeasuredDimension(mMeasuredWidth, mMeasuredHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mIsHorizontal) {
            mLineItems = mMeasuredWidth / mLineMargin;
            mCenterXY = mMeasuredWidth / 2;
        } else {
            mLineItems = mMeasuredHeight / mLineMargin;
            mCenterXY = mMeasuredHeight / 2;
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCenterText(canvas);
        drawBaseLine(canvas);
        drawLines(canvas);
        drawSideText(canvas);
    }


    private void initAttr(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.RulerView);

        mIsHorizontal = typedArray.getInt(R.styleable.RulerView_orientation, 0) == 0;
        mUnit = typedArray.getString(R.styleable.RulerView_unit);
        if (TextUtils.isEmpty(mUnit))
            mUnit = "kg";
        mUnitValue = typedArray.getInt(R.styleable.RulerView_unitValue, -1);
        mUnitValue = Math.pow(10, mUnitValue);
        mValueInterval = typedArray.getInt(R.styleable.RulerView_valueInterval, 5);
        mTextInterval = typedArray.getInt(R.styleable.RulerView_textInterval, 10);
        mMaxValue = typedArray.getFloat(R.styleable.RulerView_maxValue, 100f);
        mMinValue = typedArray.getFloat(R.styleable.RulerView_minValue, 0f);
        mCurrentVale = typedArray.getFloat(R.styleable.RulerView_value, (float) ((mMaxValue + mMinValue) / 2));

        mDecimalFormat = new DecimalFormat(String.valueOf(mUnitValue));
        mCurrentVale = Double.parseDouble(mDecimalFormat.format(mCurrentVale));
        mMaxValue = Double.parseDouble(mDecimalFormat.format(mMaxValue));
        mMinValue = Double.parseDouble(mDecimalFormat.format(mMinValue));
        mMinUnitValue = (int) (mMinValue / mUnitValue);
        mMaxUnitValue = (int) (mMaxValue / mUnitValue);

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

        mOpenCorrection = typedArray.getBoolean(R.styleable.RulerView_openCorrection, true);
        mRulerGravity = typedArray.getInt(R.styleable.RulerView_rulerGravity, mIsHorizontal ? RULER_GRAVITY_TOP : RULER_GRAVITY_LEFT);

        typedArray.recycle();


        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;

        if (mIsHorizontal)
            mPaint.setTextAlign(Paint.Align.CENTER);
        else if (mRulerGravity == RULER_GRAVITY_LEFT)
            mPaint.setTextAlign(Paint.Align.LEFT);
        else if (mRulerGravity == RULER_GRAVITY_RIGHT)
            mPaint.setTextAlign(Paint.Align.RIGHT);
    }

    /**
     * 初始化scroller 使用加速度递减插值器
     * 初始化fling速度值
     */
    private void initScroller() {

        mMaximumVelocity = ViewConfiguration.get(mContext)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(mContext)
                .getScaledMinimumFlingVelocity();
        mScroller = new OverScroller(mContext, new LinearOutSlowInInterpolator());
    }

    /**
     * 绘制中间的当前数值
     */
    private void drawCenterText(Canvas canvas) {
        String s = mCurrentVale + mUnit;

        mPaint.setTextSize(mLargeTextSize);
        mPaint.setColor(mTextColor);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setAlpha(255);


        if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawText(s, mCenterXY, mMaxLength + mTextValueMargin + mLargeTextSize, mPaint);
        else if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawText(s, mCenterXY, mMeasuredHeight - (mMaxLength + mTextValueMargin), mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawText(s, mMaxLength + mTextValueMargin + mSmallTextSize, mCenterXY + mLargeTextSize / 2f, mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawText(s, mMeasuredWidth - (mMaxLength + mTextValueMargin), mCenterXY + mLargeTextSize / 2f, mPaint);
    }

    /**
     * 绘制坐标文字
     *
     * @param alpha   透明度 [0-255]
     * @param startXY 偏移值
     * @param value   刻度值
     */
    private void drawText(Canvas canvas, double value, int startXY, int alpha) {

        String s = value + mUnit;

        float textWidth = mPaint.measureText(s);
        float centerTextWidth = mPaint.measureText(mCurrentVale + mUnit);

        if (mIsHorizontal && Math.abs(startXY - mCenterXY) <= ((textWidth + centerTextWidth) / 2 + mSmallTextSize + mLargeTextSize))
            return;
        else if (!mIsHorizontal && Math.abs(startXY - mCenterXY) <= ((mSmallTextSize + mLargeTextSize) / 2 + mSmallTextSize))
            return;

        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mSmallTextSize);
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.setAlpha(alpha);

        if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawText(s, startXY, mMaxLength + mTextValueMargin + mSmallTextSize, mPaint);
        else if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawText(s, startXY, mMeasuredHeight - (mMaxLength + mTextValueMargin), mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawText(s, mMaxLength + mTextValueMargin + mSmallTextSize, startXY + mSmallTextSize / 2f, mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawText(s, mMeasuredWidth - (mMaxLength + mTextValueMargin), startXY + mSmallTextSize / 2f, mPaint);

    }

    /**
     * 绘制中间的基准线
     */
    private void drawBaseLine(Canvas canvas) {

        mPaint.setAlpha(255);
        mPaint.setStrokeWidth(mBoldWidth);
        mPaint.setColor(mLineColor);

        if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
            canvas.drawLine(mCenterXY, 0, mCenterXY, mMaxLength, mPaint);
        else if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
            canvas.drawLine(mCenterXY, mMeasuredHeight, mCenterXY, mMeasuredHeight - mMaxLength, mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
            canvas.drawLine(0, mCenterXY, mMaxLength, mCenterXY, mPaint);
        else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
            canvas.drawLine(mMeasuredWidth, mCenterXY, mMeasuredWidth - mMaxLength, mCenterXY, mPaint);
    }

    /**
     * 绘制刻度线
     */
    private void drawLines(Canvas canvas) {

        mPaint.setStrokeWidth(mNormalWidth);


        //计算中间基准线前半部分最多能绘制的刻度线数，及开始的数值
        int halfNum = mCenterXY / mLineMargin;
        int currentUnitValue = (int) Math.ceil(mCurrentVale / mUnitValue);
        int startUnitValue = currentUnitValue - halfNum;

        //计算开始绘制的第一条刻度线的坐标值
        int startXY = (int) (mCenterXY - halfNum * mLineMargin - mScrollXY);

        for (int i = 0; i < mLineItems; i++, startUnitValue++, startXY += mLineMargin) {
            //若当前单位值在数值范围内则进行绘制
            if (startUnitValue >= mMinUnitValue && startUnitValue <= mMaxUnitValue) {

                //计算透明度，透明度根据距离中间基准线的位置线性计算
                int alpha = 255 - 255 * Math.abs(startXY - mCenterXY) / mCenterXY;
                alpha = alpha >= 0 ? alpha : 0;
                mPaint.setColor(mLineColor);
                mPaint.setAlpha(alpha);

                if (startUnitValue % mValueInterval == 0) {
                    //绘制长刻度线
                    if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
                        canvas.drawLine(startXY, 0, startXY, mLongLength, mPaint);
                    else if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
                        canvas.drawLine(startXY, mMeasuredHeight, startXY, mMeasuredHeight - mLongLength, mPaint);
                    else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
                        canvas.drawLine(0, startXY, mLongLength, startXY, mPaint);
                    else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
                        canvas.drawLine(mMeasuredWidth, startXY, mMeasuredWidth - mLongLength, startXY, mPaint);

//                    //绘制刻度值
//                    if (startUnitValue % mTextInterval == 0) {
//                        drawText(canvas, startUnitValue * mUnitValue, startXY, alpha);
//                    }
                } else {
                    //绘制短刻度线
                    if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_TOP)
                        canvas.drawLine(startXY, 0, startXY, mNormalLength, mPaint);
                    else if (mIsHorizontal && mRulerGravity == RULER_GRAVITY_BOTTOM)
                        canvas.drawLine(startXY, mMeasuredHeight, startXY, mMeasuredHeight - mNormalLength, mPaint);
                    else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_LEFT)
                        canvas.drawLine(0, startXY, mNormalLength, startXY, mPaint);
                    else if (!mIsHorizontal && mRulerGravity == RULER_GRAVITY_RIGHT)
                        canvas.drawLine(mMeasuredWidth, startXY, mMeasuredWidth - mNormalLength, startXY, mPaint);

                }
            }


        }


    }

    /**
     * 绘制两侧的刻度值
     */
    private void drawSideText(Canvas canvas) {
        int halfNum = mCenterXY / mLineMargin;
        int currentUnitValue = (int) Math.ceil(mCurrentVale / mUnitValue);

        int interval = (int) Math.ceil(halfNum * 1f / mTextInterval - 1);
        if (interval <= 0)
            return;
        interval *= mTextInterval;

        int textStartXY = mCenterXY - mLineMargin * interval;
        int value = currentUnitValue - interval;
        if (value >= mMinUnitValue) {
            double textValue = value * mUnitValue;
            textValue = Double.parseDouble(mDecimalFormat.format(textValue));
            drawText(canvas, textValue, textStartXY, 255 / 3);
        }

        textStartXY = mCenterXY + mLineMargin * interval;
        value = currentUnitValue + interval;
        if (value <= mMaxUnitValue) {
            double textValue = value * mUnitValue;
            textValue = Double.parseDouble(mDecimalFormat.format(textValue));
            drawText(canvas, textValue, textStartXY, 255 / 3);
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
                //按下屏幕时，若scroller还未停止计算，则立即终止计算
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastXY = mIsHorizontal ? event.getX() : event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                //滑动手势
                float distance = mIsHorizontal ? mLastXY - event.getX() : mLastXY - event.getY();
                mLastXY = mIsHorizontal ? event.getX() : event.getY();
                scroll(distance);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //当手指离开屏幕时，计算滑动速度，满足fling手势则进行相应处理，否则直接判断是否需要校准
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = mIsHorizontal ? (int) mVelocityTracker.getXVelocity() : (int) mVelocityTracker.getYVelocity();
                if (Math.abs(velocityX) > mMinimumVelocity) {
                    fling(-velocityX);
                } else if (mOpenCorrection && mScrollXY != 0) {
                    mScrollXY %= mLineMargin;
                    if (mScrollXY != 0)
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

            //根据scroller计算的值进行偏移，并记录当前scroller的偏移值
            float distance = mIsHorizontal ? mScroller.getCurrX() - mLastScrollerXY : mScroller.getCurrY() - mLastScrollerXY;
            scroll(distance);
            mLastScrollerXY = mIsHorizontal ? mScroller.getCurrX() : mScroller.getCurrY();

            //若开启了校准且scroller计算已结束且最终的偏移值不为0，则进行校准
            if (mOpenCorrection && mScroller.isFinished() && mScrollXY != 0) {
                mScrollXY %= mLineMargin;
                if (mScrollXY != 0)
                    scrollBackToCorrectPos();
            }
        }

    }

    /**
     * 计算偏移后的数值并重新计算偏移值
     * 若当前值已为最大值或最小值则不进行偏移
     */
    private void scroll(float distanceXY) {
        Log.d(TAG, "scroll: " + distanceXY);
        mScrollXY += distanceXY;
        //若当前值已为最大值或最小值则不进行偏移，并将偏移值归零
        if ((mCurrentVale == mMinValue && distanceXY <= 0) ||
                (mCurrentVale == mMaxValue && distanceXY >= 0)) {
            mScrollXY = 0;
        } else {

            //计算偏移后的数值并校准格式
            if (mScrollXY > 0) {
                mScrollXY = Math.round(mScrollXY);
                mCurrentVale += Math.floor(mScrollXY / mLineMargin) * mUnitValue;
            } else {
                mScrollXY = -Math.round(-mScrollXY);
                mCurrentVale -= Math.floor(-mScrollXY / mLineMargin) * mUnitValue;
            }

            mCurrentVale = Double.parseDouble(mDecimalFormat.format(mCurrentVale));

            //若偏移后的值超出范围，则进行校准
            if (mCurrentVale <= mMinValue) {
                mCurrentVale = mMinValue;
            } else if (mCurrentVale >= mMaxValue)
                mCurrentVale = mMaxValue;

            //重新计算偏移值
            mScrollXY %= mLineMargin;

            //若偏移后的值为最大值或最小值则不进行偏移，并将偏移值归零
            if ((mCurrentVale == mMinValue && distanceXY <= 0) ||
                    (mCurrentVale == mMaxValue && distanceXY >= 0)) {
                mScrollXY = 0;
            }

            invalidate();
        }


    }

    /**
     * 调用scroller的fling方法来处理fling手势的移动计算
     */
    private void fling(int velocity) {
        mLastScrollerXY = 0;
        if (mIsHorizontal)
            mScroller.fling(0, 0, velocity, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin, 0, 0);
        else
            mScroller.fling(0, 0, 0, velocity, 0, 0, -mMaxUnitValue * mLineMargin, mMaxUnitValue * mLineMargin);
        invalidate();

    }

    /**
     * 根据mScrollXY的值及四舍五入原则来计算校准偏移的位置
     */
    private void scrollBackToCorrectPos() {
        double scroll;
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
        if (mIsHorizontal)
            mScroller.startScroll(0, 0, (int) scroll, 0);
        else
            mScroller.startScroll(0, 0, 0, (int) scroll);
        invalidate();
    }

    public boolean isHorizontal() {
        return mIsHorizontal;
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
        return mOpenCorrection;
    }

    public void setHorizontal(boolean horizontal) {
        mIsHorizontal = horizontal;
        requestLayout();
    }

    public void setUnit(String mUnit) {
        this.mUnit = mUnit;
    }

    public void setCurrentVale(double mCurrentVale) {
        this.mCurrentVale = mCurrentVale;
        this.mCurrentVale = Double.parseDouble(mDecimalFormat.format(mCurrentVale));
    }

    public void setUnitValue(int mUnitValue) {
        this.mUnitValue = Math.pow(10, mUnitValue);
        mDecimalFormat = new DecimalFormat(String.valueOf(mUnitValue));
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
        requestLayout();
    }

    public void setNormalLength(int mNormalLength) {
        this.mNormalLength = mNormalLength;
        requestLayout();
    }

    public void setLongLength(int mLongLength) {
        this.mLongLength = mLongLength;
        requestLayout();
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
    }

    public void setTextValueMargin(int mTextValueMargin) {
        this.mTextValueMargin = mTextValueMargin;
        requestLayout();
    }

    public void setLargeTextSize(int mLargeTextSize) {
        this.mLargeTextSize = mLargeTextSize;
        requestLayout();
    }

    public void setSmallTextSize(int mSmallTextSize) {
        this.mSmallTextSize = mSmallTextSize;
        requestLayout();
    }

    public void setMinValue(double mMinValue) {
        this.mMinValue = mMinValue;
        this.mMinValue = Double.parseDouble(mDecimalFormat.format(mMinValue));
        mMinUnitValue = (int) (mMinValue / mUnitValue);
    }

    public void setMaxValue(double mMaxValue) {
        this.mMaxValue = mMaxValue;
        this.mMaxValue = Double.parseDouble(mDecimalFormat.format(mMaxValue));
        mMaxUnitValue = (int) (mMaxValue / mUnitValue);
    }

    public void setOpenCorrection(boolean openCorrection) {
        this.mOpenCorrection = openCorrection;
    }

    public int getRulerGravity() {
        return mRulerGravity;
    }

    public void setRulerGravity(int mRulerGravity) {
        this.mRulerGravity = mRulerGravity;
        requestLayout();
    }


    public void setValueInterval(int mValueInterval) {
        this.mValueInterval = mValueInterval;
    }


    public void setTextInterval(int mTextInterval) {
        this.mTextInterval = mTextInterval;
    }

    /**
     * 状态保存 {@link SavedState}
     */
    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superInstanceState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superInstanceState);
        ss.setCurrentVale(mCurrentVale);
        return ss;
    }

    /**
     * 状态恢复 {@link SavedState}
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mCurrentVale = ss.getCurrentVale();
    }


    /**
     * SavedState 保存{@link #mCurrentVale}
     */
    static class SavedState extends BaseSavedState {

        //当前刻度值
        private double mCurrentVale;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        protected SavedState(Parcel in) {
            super(in);
            mCurrentVale = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(mCurrentVale);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public double getCurrentVale() {
            return mCurrentVale;
        }

        public void setCurrentVale(double currentVale) {
            mCurrentVale = currentVale;
        }
    }
}
