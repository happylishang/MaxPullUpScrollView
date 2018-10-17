package com.snail.labaffinity.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.snail.labaffinity.R;
import com.snail.labaffinity.utils.LogUtils;


public class MaxPullUpScrollView2 extends FrameLayout {


    private int mMaxHeight;
    private int mMinHeight;
    private final static int FACTOR = 4;
    private int mContentHeight;
    private float mFlingSpeed;
    private float mDownY;
    private boolean hasTouched;
    private ValueAnimator mValueAnimator;

    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float speed = (e2.getRawY() - e1.getRawY()) * 1000 / (e2.getEventTime() - e1.getEventTime());
            fling(speed);
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    });

    private void fling(final float velocityY) {
        mFlingSpeed = velocityY / FACTOR;
        float maxScrollDistance;
        final int maxHeight = Math.min(mContentHeight, mMaxHeight);
        if (mFlingSpeed >= 0) {
            //  向下滚动
            maxScrollDistance = Math.min(Math.max(getScrollY(), 0) + getMeasuredHeight() - mMinHeight, mFlingSpeed);
        } else {
            // 向上滚动
            maxScrollDistance = Math.max(-(Math.max(mContentHeight - getScrollY() - maxHeight, 0) + maxHeight - getMeasuredHeight()), mFlingSpeed);
        }
        LogUtils.v("mFlingSpeed " + mFlingSpeed + " maxScrollDistance " + maxScrollDistance + " mContentHeight " + mContentHeight + " getMeasuredHeight " + getMeasuredHeight() + " getScrollY " + getScrollY());
        mValueAnimator = ValueAnimator.ofFloat(0, maxScrollDistance);
        mValueAnimator.setInterpolator(new DecelerateInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private float mLastY;
            private float totalFloat;
            private int totalInt;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                float distance = mLastY - value;
                //  累加校准
                totalFloat += distance;
                totalInt += (int) distance;
                if (Math.abs(totalFloat - totalInt) > 1) {
                    distance += (int) (totalFloat - totalInt);
                    totalFloat -= (int) (totalFloat - totalInt);
                }
                if (getScrollY() == 0) {
                    if ((getMeasuredHeight() + distance) <= mMinHeight) {
                        getLayoutParams().height = mMinHeight;
                    } else if ((getMeasuredHeight() + distance) >= maxHeight) {
                        getLayoutParams().height = maxHeight;
                        scrollBy(0, (int) (getMeasuredHeight() + distance - maxHeight));
                    } else {
                        getLayoutParams().height = (int) (getMeasuredHeight() + distance);
                    }
                    requestLayout();
                } else {
                    if (getScrollY() + distance <= 0) {
                        scrollTo(0, 0);
                        getLayoutParams().height += getScrollY() + distance;
                        requestLayout();
                    } else if (getScrollY() + distance > mContentHeight - maxHeight) {
                        getLayoutParams().height = maxHeight;
                        requestLayout();
                    } else {
                        scrollBy(0, (int) distance);
                    }

                }
                mLastY = value;
            }
        });
        mValueAnimator.start();
    }

    public MaxPullUpScrollView2(@NonNull Context context) {
        this(context, null);
    }

    public MaxPullUpScrollView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxPullUpScrollView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MaxPullUpScrollView, defStyleAttr, 0);
            mMaxHeight = array.getDimensionPixelOffset(R.styleable.MaxPullUpScrollView_maxHeight, 200);
            mMinHeight = array.getDimensionPixelOffset(R.styleable.MaxPullUpScrollView_minHeight, 100);
            array.recycle();
        }
    }

    public void setMaxHeight(int height) {

        mMaxHeight = height;
    }

    public void setMinHeight(int height) {
        mMinHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        super.onMeasure(widthMeasureSpec, measureSpec);
        if (!hasTouched) {
            mContentHeight = getMeasuredHeight();
            setMeasuredDimension(getMeasuredWidth(), mMinHeight);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(getMeasuredWidth(), Math.max(mMinHeight, getMeasuredHeight()));
            measureChildren(widthMeasureSpec, measureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
        }
        mGestureDetector.onTouchEvent(ev);
        hasTouched = true;
        int maxHeight = Math.min(mContentHeight, mMaxHeight);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = ev.getRawY() - mDownY;
                if (getMeasuredHeight() < maxHeight || (distance > 0 && getScrollY() <= 0)) {
                    getLayoutParams().height = Math.min(Math.max(mMinHeight, Math.min((int) (getMeasuredHeight() - distance), maxHeight)), maxHeight);
                    requestLayout();
                    scrollTo(0, 0);
                } else {
                    if (mContentHeight > maxHeight) {
                        if (getScrollY() - distance < mContentHeight - maxHeight) {
                            scrollBy(0, (int) -distance);
                        } else {
                            scrollTo(0, mContentHeight - maxHeight);
                        }
                    }
                }
                break;
        }
        mDownY = ev.getRawY();
        super.dispatchTouchEvent(ev);
        return true;
    }

    private View mScrollView;

    public void setScrollContentView(@IdRes int id) {
        mScrollView = findViewById(id);
    }
}