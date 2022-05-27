package com.luis.autopollviewpager;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * 自动轮播ViewPager
 *
 * @author luis
 * @date :2022/5/17
 */
public class AutoPollViewPager extends ViewPager implements Handler.Callback {

    private OnPagerChangeListener mOpageChangeListener;

    public void setTimeWait(long timeWait) {
        this.mTimeWait = timeWait;
    }

    public long getTimeWait() {
        return mTimeWait;
    }

    public long mTimeWait = 3000;  //每3秒轮播一次
    private boolean isPlaying = false;

    private OnPagerSelectedListener mOnPagerSelectedListener;

    private boolean lastStateIsPlaying = false; //停止轮播之前的状态
    //private   static  AutoBannerRunnable autoRunnable = null;

    private final Handler mTaskHandler;
    private static final int ACTION_PLAY = 1024 * 1024;

    public AutoPollViewPager(Context context) {
        this(context, null);
    }

    public AutoPollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setOffscreenPageLimit(5);
        if (mOpageChangeListener == null) {
            mOpageChangeListener = new OnPagerChangeListener(this);
        }
        mTaskHandler = new Handler(Looper.getMainLooper(), this);

    }

    public void setOnPagerSelectedListener(OnPagerSelectedListener mOnPagerSelectedListener) {
        this.mOnPagerSelectedListener = mOnPagerSelectedListener;
    }


    @Override
    public boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params) {
        return super.addViewInLayout(child, index, params);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params, boolean preventRequestLayout) {
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (null != getAdapter()) {
            removeOnPageChangeListener(mOpageChangeListener);
        }
        if (null == adapter) {
            super.setAdapter(null);
        } else {
            super.setAdapter(new InnerWrapperPagerAdapter(adapter, this));
            addOnPageChangeListener(mOpageChangeListener);
            if (adapter.getCount() > 1) {
                setCurrentItem(1, false);  //getCount>1 ,初始化时默认显示原始数据的第0位，也就是新的PagerAdapter的第二位
                startPlay();
            } else if (adapter.getCount() > 0) {
                mOpageChangeListener.onPageSelected(0);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        finishResetPosition();

        final ViewParent parent = this.getParent();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:

                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    //解决ViewPager嵌套ViewPager导致无法滑动的问题
                }
                if (isPlaying) {
                    stopPlay();
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                    //解决ViewPager嵌套ViewPager导致无法滑动的问题
                }
                if (lastStateIsPlaying) {
                    startPlay();
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    public PagerAdapter getRealAdapter() {
        final PagerAdapter adapter = getAdapter();
        if (adapter instanceof InnerWrapperPagerAdapter) {
            return ((InnerWrapperPagerAdapter) adapter).getTargetPagerAdapter();
        }
        return adapter;
    }


    public static class AdapterDataSetObserver extends DataSetObserver {
        private InnerWrapperPagerAdapter innerWrapperPagerAdapter;
        private AutoPollViewPager AutoPollViewPager;

        public AdapterDataSetObserver(InnerWrapperPagerAdapter innerWrapperPagerAdapter, AutoPollViewPager AutoPollViewPager) {
            this.innerWrapperPagerAdapter = innerWrapperPagerAdapter;
            this.AutoPollViewPager = AutoPollViewPager;
        }

        @Override
        public void onChanged() {
            super.onChanged();
            innerWrapperPagerAdapter.notifyDataSetChanged();
            if (innerWrapperPagerAdapter.getCount() > 1) {
                final int currentItem = this.AutoPollViewPager.getCurrentItem();
                if (currentItem == 0) {
                    this.AutoPollViewPager.setCurrentItem(1, false);
                }

                if (this.AutoPollViewPager.isPlaying || this.AutoPollViewPager.lastStateIsPlaying) {
                    this.AutoPollViewPager.startPlay();
                }
            }
            if (innerWrapperPagerAdapter.getCount() > 0 && this.AutoPollViewPager.mOpageChangeListener != null) {
                this.AutoPollViewPager.mOpageChangeListener.onPageSelected(this.AutoPollViewPager.getCurrentItem());
            }
        }

    }

    /**
     * 包装PagerAdapter，实现数据映射
     */
    private static class InnerWrapperPagerAdapter extends PagerAdapter {

        private final AdapterDataSetObserver adapterDataSetObserver;

        private PagerAdapter mPagerAdapter;

        public InnerWrapperPagerAdapter(PagerAdapter pagerAdapter, AutoPollViewPager AutoPollViewPager) {
            this.mPagerAdapter = pagerAdapter;
            this.adapterDataSetObserver = new AdapterDataSetObserver(this, AutoPollViewPager);
            if (this.mPagerAdapter != null) {
                this.mPagerAdapter.registerDataSetObserver(this.adapterDataSetObserver);
            }
        }

        public PagerAdapter getTargetPagerAdapter() {
            return mPagerAdapter;
        }

        @Override
        public int getCount() {
            if (mPagerAdapter != null) {  //如果数据大于1，说明可以轮播
                return mPagerAdapter.getCount() > 1 ? mPagerAdapter.getCount() + 2 : mPagerAdapter.getCount();
            }
            return 0;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            if (mPagerAdapter != null) {
                return mPagerAdapter.isViewFromObject(view, object);
            }
            return false;
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mPagerAdapter != null) {
                int realPosition = getRealPosition(position);
                return mPagerAdapter.instantiateItem(container, realPosition);
            }
            return super.instantiateItem(container, position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mPagerAdapter != null) {
                int realPosition = getRealPosition(position);
                mPagerAdapter.destroyItem(container, realPosition, object);
            } else {
                super.destroyItem(container, position, object);
            }
        }

        private int getRealPosition(int position) {
            int realPosition = position;
            if (InnerWrapperPagerAdapter.this.getCount() > 1) {
                if (position == 0) {
                    realPosition = (getCount() - 2) - 1;
                } else if (position == getCount() - 1) {
                    realPosition = 0;
                } else {
                    realPosition = position - 1;
                }
            }
            return realPosition;
        }

        @Override
        public int getItemPosition(Object object) {
            if (mPagerAdapter != null) {
                return mPagerAdapter.getItemPosition(object);
            }
            return super.getItemPosition(object);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            if (mPagerAdapter != null) {
                int realPosition = getRealPosition(position);
                return mPagerAdapter.getPageTitle(realPosition);
            } else {
                return super.getPageTitle(position);
            }
        }

        @Override
        public float getPageWidth(int position) {
            if (mPagerAdapter != null) {
                int realPosition = getRealPosition(position);
                return mPagerAdapter.getPageWidth(realPosition);
            }
            return super.getPageWidth(position);
        }


        @Override
        public void startUpdate(ViewGroup container) {
            if (mPagerAdapter != null) {
                mPagerAdapter.startUpdate(container);
            } else {
                super.startUpdate(container);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (mPagerAdapter != null) {
                int realPosition = getRealPosition(position);
                mPagerAdapter.setPrimaryItem(container, realPosition, object);
            } else {
                super.setPrimaryItem(container, position, object);
            }
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mPagerAdapter != null) {
                mPagerAdapter.finishUpdate(container);
            } else {
                super.finishUpdate(container);
            }
        }

        @Override
        public Parcelable saveState() {
            if (mPagerAdapter != null) {
                return mPagerAdapter.saveState();
            }
            return super.saveState();
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            if (mPagerAdapter != null) {
                mPagerAdapter.restoreState(state, loader);
            } else {
                super.restoreState(state, loader);
            }

        }

        public void setPagerAdapter(PagerAdapter pagerAdapter) {
            this.mPagerAdapter = pagerAdapter;
        }
    }


    public int getClientWidth() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    private static class OnPagerChangeListener implements OnPageChangeListener {

        private final AutoPollViewPager mAutoPollViewPager;

        public OnPagerChangeListener(AutoPollViewPager AutoPollViewPager) {
            this.mAutoPollViewPager = AutoPollViewPager;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (shouldCancelHandle()) return;
        }

        @Override
        public void onPageSelected(int position) {
            if (shouldCancelHandle()) return;
            notifyUpdateIndictor(position);
        }

        public void notifyUpdateIndictor(int position) {
            if (this.mAutoPollViewPager.mOnPagerSelectedListener != null) {
                final PagerAdapter adapter = this.mAutoPollViewPager.getAdapter();
                final int count = adapter.getCount();
                final int realPosition = getRealPosition(position);
                if (count > 1) {
                    if (position == 0 || position == count - 1) {
                        return;
                    }
                    this.mAutoPollViewPager.mOnPagerSelectedListener.onPagerItemSelected(realPosition);
                } else if (count == 1) {
                    this.mAutoPollViewPager.mOnPagerSelectedListener.onPagerItemSelected(1);
                } else {
                    this.mAutoPollViewPager.mOnPagerSelectedListener.onPagerItemSelected(0);
                }
            }
        }


        private int getRealPosition(int position) {
            int realPosition = position;
            if (this.mAutoPollViewPager.getAdapter().getCount() > 1) {
                PagerAdapter adapter = this.mAutoPollViewPager.getAdapter();
                if (adapter instanceof InnerWrapperPagerAdapter) {
                    adapter = ((InnerWrapperPagerAdapter) adapter).getTargetPagerAdapter();
                    if (position == 0) {
                        realPosition = adapter.getCount() - 1;
                    } else if (position == adapter.getCount() + 2 - 1) {
                        realPosition = 0;
                    } else {
                        realPosition = position - 1;
                    }
                }
            }
            return realPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            //在该方法中处理首尾切换，其他方法会产生动画中断问题
            if (shouldCancelHandle()) return;
            if (state == SCROLL_STATE_IDLE) { //当动画执行完，立即切换
                this.mAutoPollViewPager.finishResetPosition();
            }

        }

        private boolean shouldCancelHandle() {
            return this.mAutoPollViewPager == null || mAutoPollViewPager.getAdapter() == null;
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (lastStateIsPlaying) {  //如果之前是轮播的，那么继续轮播
            startPlay();
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isPlaying) {
            stopPlay();// 如果从window中移除掉，停止轮播
        }
        onInvisibleToUser();

    }

    private void onInvisibleToUser() {
        final int currentItem = getCurrentItem();
        PagerAdapter adapter = getAdapter();
        if (adapter instanceof InnerWrapperPagerAdapter) {
            adapter = ((InnerWrapperPagerAdapter) adapter).getTargetPagerAdapter();
        }
        if (adapter != null && adapter.getCount() > 1 && currentItem > 0) {
            setCurrentItem(currentItem - 1);
            setCurrentItem(currentItem);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            if (lastStateIsPlaying) {  //如果之前是轮播的，那么继续轮播
                startPlay();
            }
        } else if (visibility == INVISIBLE || visibility == GONE) {
            if (isPlaying) {
                stopPlay();// 如果从window中移除掉，停止轮播
            }
            onInvisibleToUser();
        }
    }


    public interface OnPagerSelectedListener {
        //用于监听真实的滑动位置，方便外部使用
        public void onPagerItemSelected(int position);
    }

    public void startPlay() {
        stopPlay();
        isPlaying = true;
        /* postDelayed(autoRunnable,TIME_WAIT);*/

        mTaskHandler.sendEmptyMessageDelayed(ACTION_PLAY, getTimeWait());
    }

    public void stopPlay() {

        lastStateIsPlaying = isPlaying == true;
        isPlaying = false;
        mTaskHandler.removeMessages(ACTION_PLAY);

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg == null) return false;
        int action = msg.what;

        switch (action) {
            case ACTION_PLAY:
                this.showNext();
                return true;

        }
        return false;
    }

    private long lastExecuteTimestramp = 0;

    private boolean finishResetPosition() {
        if (!(this.getAdapter() instanceof InnerWrapperPagerAdapter)) return false;
        int currentItem = this.getCurrentItem();
        if (this.getAdapter().getCount() > 1) {
            InnerWrapperPagerAdapter adapter = (InnerWrapperPagerAdapter) this.getAdapter();
            if (currentItem == 0) {
                //this.setCurrentItem(adapter.getCount()-2,false);
                scrollToItem(adapter.getCount() - 2, adapter.getCount());
                return true;
            } else if (currentItem == adapter.getCount() - 1) {
                scrollToItem(1, adapter.getCount());
                /*  this.setCurrentItem(1,false)*/
                ;
                return true;
            }
        }
        return false;
    }

    public void setRealCurrentItem(int position) {
        if (this.getAdapter() instanceof InnerWrapperPagerAdapter) {
            this.setCurrentItem(position + 1);
        } else {
            setCurrentItem(position);
        }
    }

    public void showNext() {
        if (Looper.myLooper() != Looper.getMainLooper()) return; //要求在主线程工作

        final long currentTimestramp = System.currentTimeMillis();
        final long diff = (currentTimestramp - lastExecuteTimestramp);
        lastExecuteTimestramp = currentTimestramp;
        PagerAdapter adapter = this.getAdapter();

        if (null == adapter || adapter.getCount() <= 1) return;
        if (!this.isPlaying) return; //停止执行
        int currentItem = this.getCurrentItem();
        if (currentItem > 0 && currentItem < (adapter.getCount() - 1)) {


            if (currentItem == 1) {
                scrollToItem(1, adapter.getCount());
            }

            this.setCurrentItem(currentItem + 1, true);
        }
        if (diff < getTimeWait()) {
            if (mTaskHandler != null) {
                mTaskHandler.removeMessages(ACTION_PLAY);
            }
        }
        if (mTaskHandler != null) {
            mTaskHandler.sendEmptyMessageDelayed(ACTION_PLAY, getTimeWait());
        }

    }

    protected void scrollToItem(int target, int childCount) {
        final int width = getClientWidth();
        int destX = (int) (width * Math.max(-1.0f,
                Math.min(target, childCount)));
        if (getScrollX() != destX) {
            scrollTo(0, 0);
            //setCurrentItem存在无法重置scrollX的可能性，最终导致滑动方向不一致，因此这里需要手动设置
        }
        this.setCurrentItem(target, false);
    }

}