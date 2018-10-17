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


/**
 * Author: snail
 * Data: 2018/10/11.
 * Des:
 * version:
 */
public class MaxPullUpScrollView extends FrameLayout {

    private final static int FACTOR = 4;
    private int mContentHeight;
    private float mDownY;
    private float mLastY;
    private boolean hasTouched;
    private ValueAnimator mValueAnimator;
    private boolean isFling;
    private int mMaxHeight;
    private int mMinHeight;
    /**
     * 是否需要支持自动对齐
     */
    private boolean mAutoAlign = true;
    /**
     * 是否需要里面的部分内容消费滚动，而不是整体滚动
     */
    private View mInnerScrollView;

    public void setScrollContentView(@IdRes int id) {
        mInnerScrollView = findViewById(id);
    }

    private OnShrinkListener mOnShrinkListener;

    public void setOnShrinkListener(OnShrinkListener onShrinkListener) {
        mOnShrinkListener = onShrinkListener;
    }

    public void setAutoAlign(boolean autoAlign) {
        mAutoAlign = autoAlign;
    }

    public interface OnShrinkListener {
        void onShrink();

        void onExpand();
    }

    private float mLastScrollY1;
    private float mLastScrollY2;
    private long mLastScrollTimeY1;
    private long mLastScrollTimeY2;
    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mLastScrollY2 != mLastScrollY1 && mLastScrollTimeY1 != mLastScrollTimeY2) {
                float speed = (mLastScrollY2 - mLastScrollY1) * 1000 / (mLastScrollTimeY2 - mLastScrollTimeY1);
                isFling = true;
                fling(speed);
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    });

    private void fling(final float velocityY) {
        float flingSpeed = velocityY / FACTOR;
        float maxScrollDistance;
        final int maxHeight = Math.min(mContentHeight, mMaxHeight);
        int scrollY = mInnerScrollView == null ? getScrollY() : mInnerScrollView.getScrollY();
        if (flingSpeed >= 0) {
            //  向下滚动
            if (flingSpeed > scrollY && mAutoAlign) {
                maxScrollDistance = Math.max(scrollY, 0) + getMeasuredHeight() - mMinHeight;
            } else {
                maxScrollDistance = Math.min(Math.max(scrollY, 0) + getMeasuredHeight() - mMinHeight, flingSpeed);
            }
        } else {
            if (-flingSpeed < maxHeight - getMeasuredHeight() && mAutoAlign) {
                flingSpeed = getMeasuredHeight() - maxHeight;
            }
            // 向上滚动
            maxScrollDistance = Math.max(-(Math.max(mContentHeight - scrollY - maxHeight, 0) + maxHeight - getMeasuredHeight()), flingSpeed);
        }

        if (maxScrollDistance < 0) {
            if (mOnShrinkListener != null) {
                mOnShrinkListener.onExpand();
            }
        } else {
            if (mOnShrinkListener != null) {
                mOnShrinkListener.onShrink();
            }
        }
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
                int scrollY = mInnerScrollView == null ? getScrollY() : mInnerScrollView.getScrollY();
                if (scrollY == 0) {
                    if ((getMeasuredHeight() + distance) <= mMinHeight) {
                        getLayoutParams().height = mMinHeight;
                    } else if ((getMeasuredHeight() + distance) >= maxHeight) {
                        getLayoutParams().height = maxHeight;
                        if (mInnerScrollView != null) {
                            mInnerScrollView.scrollBy(0, (int) -(getMeasuredHeight() + distance - maxHeight));
                        } else {
                            scrollBy(0, (int) -(getMeasuredHeight() + distance - maxHeight));
                        }
                    } else {
                        getLayoutParams().height = (int) (getMeasuredHeight() + distance);
                    }
                    requestLayout();
                } else {
                    if (scrollY + distance <= 0) {
                        if (mInnerScrollView != null) {
                            mInnerScrollView.scrollTo(0, 0);
                        } else {
                            scrollTo(0, 0);
                        }

                        getLayoutParams().height += scrollY + distance;
                        requestLayout();
                    } else if (scrollY + distance > mContentHeight - maxHeight) {
                        getLayoutParams().height = maxHeight;
                        requestLayout();
                    } else {
                        if (mInnerScrollView != null) {
                            mInnerScrollView.scrollBy(0, (int) distance);
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
                mLastY = ev.getRawY();
                mLastScrollY1 = 0;
                mLastScrollY2 = 0;
                isFling = false;
                break;
            case MotionEvent.ACTION_MOVE:
                drag(ev);
                markSpeedPoint(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isFling && mAutoAlign) {
                    //   注意点击事件有限极高，可能还没重新布局测量
                    if (getLayoutParams().height < maxHeight && getLayoutParams().height > 0) {
                        if (ev.getRawY() - mDownY < 0) {
                            reset((maxHeight - getLayoutParams().height));
                        } else {
                            reset(mMinHeight - getLayoutParams().height);
                        }
                    }
                }
                break;
        }
        mLastY = ev.getRawY();
        super.dispatchTouchEvent(ev);
        return true;
    }

    private void markSpeedPoint(MotionEvent ev) {
        mLastScrollY1 = mLastScrollY2;
        mLastScrollTimeY1 = mLastScrollTimeY2;
        mLastScrollTimeY2 = ev.getEventTime();
        mLastScrollY2 = ev.getRawY();
    }


    private void drag(MotionEvent ev) {
        int maxHeight = Math.min(mContentHeight, mMaxHeight);
        float distance = ev.getRawY() - mLastY;
        int scrollY = mInnerScrollView == null ? getScrollY() : mInnerScrollView.getScrollY();
        if (getMeasuredHeight() < maxHeight || (distance > 0 && scrollY <= 0)) {
            getLayoutParams().height = Math.min(Math.max(mMinHeight, Math.min((int) (getMeasuredHeight() - distance), maxHeight)), maxHeight);
            requestLayout();
            if (mInnerScrollView != null) {
                mInnerScrollView.scrollTo(0, 0);
            } else {
                scrollTo(0, 0);
            }
        } else {
            if (mContentHeight > maxHeight) {
                if (scrollY - distance < mContentHeight - maxHeight) {
                    if (mInnerScrollView != null) {
                        mInnerScrollView.scrollBy(0, (int) -distance);
                    } else {
                        scrollBy(0, (int) -distance);
                    }
                } else {
                    if (mInnerScrollView != null) {
                        mInnerScrollView.scrollTo(0, mContentHeight - maxHeight);
                    } else {
                        scrollTo(0, mContentHeight - maxHeight);
                    }
                }
            }
        }
    }

    /**
     * 自动对齐
     */
    private void reset(int distance) {
        if (distance > 0) {
            if (mOnShrinkListener != null) {
                mOnShrinkListener.onExpand();
            }
        } else {
            if (mOnShrinkListener != null) {
                mOnShrinkListener.onShrink();
            }
        }

        mValueAnimator = ValueAnimator.ofInt(0, distance);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int mLastY;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                getLayoutParams().height = getMeasuredHeight() + value - mLastY;
                requestLayout();
                mLastY = value;
            }
        });
        mValueAnimator.start();
    }
}