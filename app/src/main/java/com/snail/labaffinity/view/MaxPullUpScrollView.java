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


public class MaxPullUpScrollView extends FrameLayout {


    private int mMaxHeight;
    private int mMinHeight;
    private final static int FACTOR = 1;
    private int mContentHeight;
    private float mDownY;
    private float mLastY;
    private boolean hasTouched;
    private ValueAnimator mValueAnimator;
    private float mLastScrollY1;
    private float mLastScrollY2;
    private long mLastScrollTimeY1;
    private long mLastScrollTimeY2;
    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mLastScrollY2 != mLastScrollY1 && mLastScrollTimeY1 != mLastScrollTimeY2) {
                float speed = (mLastScrollY2 - mLastScrollY1) * 1000 / (mLastScrollTimeY2 - mLastScrollTimeY1);
                fling(speed);
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mLastScrollY1 = mLastScrollY2;
            mLastScrollTimeY1 = mLastScrollTimeY2;
            mLastScrollTimeY2 = e2.getEventTime();
            mLastScrollY2 = e2.getRawY();
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    });

    private void fling(final float velocityY) {
        float flingSpeed = velocityY / FACTOR;
        float maxScrollDistance;
        final int maxHeight = Math.min(mContentHeight, mMaxHeight);
        int scrollY = mScrollView == null ? getScrollY() : mScrollView.getScrollY();
        if (flingSpeed >= 0) {
            //  向下滚动
            maxScrollDistance = Math.min(Math.max(scrollY, 0) + getMeasuredHeight() - mMinHeight, flingSpeed);
        } else {
            // 向上滚动
            maxScrollDistance = Math.max(-(Math.max(mContentHeight - scrollY - maxHeight, 0) + maxHeight - getMeasuredHeight()), flingSpeed);
        }
        LogUtils.v("mFlingSpeed " + flingSpeed + " maxScrollDistance " + maxScrollDistance + " mContentHeight " + mContentHeight + " getMeasuredHeight " + getMeasuredHeight() + " getScrollY " + getScrollY());
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
                int scrollY = mScrollView == null ? getScrollY() : mScrollView.getScrollY();
                if (scrollY == 0) {
                    if ((getMeasuredHeight() + distance) <= mMinHeight) {
                        getLayoutParams().height = mMinHeight;
                    } else if ((getMeasuredHeight() + distance) >= maxHeight) {
                        getLayoutParams().height = maxHeight;

                        if (mScrollView != null) {
                            mScrollView.scrollBy(0, (int) (getMeasuredHeight() + distance - maxHeight));
                        } else {
                            scrollBy(0, (int) (getMeasuredHeight() + distance - maxHeight));
                        }

                    } else {
                        getLayoutParams().height = (int) (getMeasuredHeight() + distance);
                    }
                    requestLayout();
                } else {
                    if (scrollY + distance <= 0) {
                        if (mScrollView != null) {
                            mScrollView.scrollTo(0, 0);
                        } else {
                            scrollTo(0, 0);
                        }


                        getLayoutParams().height += scrollY + distance;
                        requestLayout();
                    } else if (scrollY + distance > mContentHeight - maxHeight) {
                        getLayoutParams().height = maxHeight;
                        requestLayout();
                    } else {

                        if (mScrollView != null) {
                            mScrollView.scrollBy(0, (int) distance);
                        } else {
                            scrollBy(0, (int) distance);
                        }
                    }

                }
                mLastY = value;
            }
        });
        mValueAnimator.start();
    }

    public MaxPullUpScrollView(@NonNull Context context) {
        this(context, null);
    }

    public MaxPullUpScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxPullUpScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = ev.getRawY() - mLastY;
                int scrollY = mScrollView == null ? getScrollY() : mScrollView.getScrollY();

                if (getMeasuredHeight() < maxHeight || (distance > 0 && scrollY <= 0)) {
                    getLayoutParams().height = Math.min(Math.max(mMinHeight, Math.min((int) (getMeasuredHeight() - distance), maxHeight)), maxHeight);
                    requestLayout();
                    if (mScrollView != null) {
                        mScrollView.scrollTo(0, 0);
                    } else {
                        scrollTo(0, 0);
                    }

                } else {
                    if (mContentHeight > maxHeight) {
                        if (scrollY - distance < mContentHeight - maxHeight) {
                            if (mScrollView != null) {
                                mScrollView.scrollBy(0, (int) -distance);
                            } else {
                                scrollBy(0, (int) -distance);
                            }

                        } else {
                            if (mScrollView != null) {
                                mScrollView.scrollTo(0, mContentHeight - maxHeight);
                            } else {
                                scrollTo(0, mContentHeight - maxHeight);
                            }

                        }
                    }
                }
                LogUtils.v("mLastY " + mLastY);
                mLastY = ev.getRawY();
                break;
        }
        super.dispatchTouchEvent(ev);
        return true;
    }

    private View mScrollView;

    public void setScrollContentView(@IdRes int id) {
        mScrollView = findViewById(id);
    }
}