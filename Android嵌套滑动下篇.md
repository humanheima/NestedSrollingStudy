

对于fling，NestedScrolling的处理并不友好。对于fling，要么child处理，要么parent处理。这就会导致惯性滑动的时候不是很流畅。
而通过NestedScrolling2可以实现fling类型的滚动，可以先由外层控件处理一部分，剩余的再交给内层控件处理。


NestedScrollingChild2继承了NestedScrollingChild接口并新增了几个方法。

```
public interface NestedScrollingChild2 extends NestedScrollingChild {

    boolean startNestedScroll(@ScrollAxis int axes, @NestedScrollType int type);

    void stopNestedScroll(@NestedScrollType int type);

    boolean hasNestedScrollingParent(@NestedScrollType int type);

    boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow,
            @NestedScrollType int type);

    boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
            @Nullable int[] offsetInWindow, @NestedScrollType int type);

}
```

新增的几个方法都有一个`@NestedScrollType int type`参数，是用来区分滚动类型的，有两个取值：

```
//触摸屏幕类型
public static final int TYPE_TOUCH = 0;

//惯性滑动类型   
public static final int TYPE_NON_TOUCH = 1;

```

对应的NestedScrollingParent2继承了NestedScrollingParent接口也新增了几个方法。

```
public interface NestedScrollingParent2 extends NestedScrollingParent {

    boolean onStartNestedScroll(@NonNull View child, @NonNull View target, @ScrollAxis int axes,
            @NestedScrollType int type);
    
    void onNestedScrollAccepted(@NonNull View child, @NonNull View target, @ScrollAxis int axes,
                @NestedScrollType int type);

    void onStopNestedScroll(@NonNull View target, @NestedScrollType int type);

    void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @NestedScrollType int type);
    
    void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                @NestedScrollType int type);

}
```

最新的嵌套滑动已经都出NestedScrollingChild3了，这个更新也是日新月异啊，哈哈。

```
public interface NestedScrollingChild3 extends NestedScrollingChild2 {

    void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
            @Nullable int[] offsetInWindow, @ViewCompat.NestedScrollType int type,
            @NonNull int[] consumed);
}
```

```
public interface NestedScrollingParent3 extends NestedScrollingParent2 {
    void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, @ViewCompat.NestedScrollType int type, @NonNull int[] consumed);
}
```


下面我们以RecyclerView为例开始分析，`androidx1.1.0`的源码。

```
public class RecyclerView extends ViewGroup implements ScrollingView,
        NestedScrollingChild2, NestedScrollingChild3 {
    //...
}
```

RecyclerView的onTouchEvent方法

```
@Override
public boolean onTouchEvent(MotionEvent e) {
    //...
    final MotionEvent vtev = MotionEvent.obtain(e);
    vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

    switch (action) {
        case MotionEvent.ACTION_DOWN: {
            //...
            int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
            if (canScrollHorizontally) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
            }
            if (canScrollVertically) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
            }
            //注释1处
            startNestedScroll(nestedScrollAxis, TYPE_TOUCH);
        } break;
        case MotionEvent.ACTION_MOVE: {
                
            final int x = (int) (e.getX(index) + 0.5f);
            final int y = (int) (e.getY(index) + 0.5f);
            int dx = mLastTouchX - x;
            int dy = mLastTouchY - y;

            if (mScrollState == SCROLL_STATE_DRAGGING) {
                mReusableIntPair[0] = 0;
                mReusableIntPair[1] = 0;
                //注释2处
                if (dispatchNestedPreScroll(
                        canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0,
                        mReusableIntPair, mScrollOffset, TYPE_TOUCH
                )) {
                    //注释3处
                    dx -= mReusableIntPair[0];
                    dy -= mReusableIntPair[1];
                    //...
                }

                mLastTouchX = x - mScrollOffset[0];
                mLastTouchY = y - mScrollOffset[1];
                //注释4处
                if (scrollByInternal(
                        canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0,
                        e)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } break;
        case MotionEvent.ACTION_UP: {
            mVelocityTracker.addMovement(vtev);
            eventAddedToVelocityTracker = true;
            //计算速度
            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
            final float xvel = canScrollHorizontally
                    ? -mVelocityTracker.getXVelocity(mScrollPointerId) : 0;
            final float yvel = canScrollVertically
                    ? -mVelocityTracker.getYVelocity(mScrollPointerId) : 0;
            //注释5处，调用fling方法
            if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                setScrollState(SCROLL_STATE_IDLE);
            }
            //注释6处
            resetScroll();
        } break;
    }
    //...
    return true;
}
```

注释1处，调用startNestedScroll方法

```
@Override
public boolean startNestedScroll(int axes, int type) {
    //调用NestedScrollingChildHelper的startNestedScroll方法
    return getScrollingChildHelper().startNestedScroll(axes, type);
}
```

这里提一下，使用NestedScrollingChildHelper和NestedScrollingParentHelper是为了对`Android 5.0 Lollipop (API 21)`以下的版本做兼容。

NestedScrollingChildHelper的startNestedScroll方法

```
public boolean startNestedScroll(@ScrollAxis int axes, @NestedScrollType int type) {
    //根据嵌套滑动的类型来获取NestedScrollingParent
    if (hasNestedScrollingParent(type)) {
        // 嵌套滑动已经在处理过程中，直接返回true
        return true;
    }
    if (isNestedScrollingEnabled()) {
        ViewParent p = mView.getParent();
        View child = mView;
        //遍历父级控件
        while (p != null) {
            //注释1处
            if (ViewParentCompat.onStartNestedScroll(p, child, mView, axes, type)) {
                //注释2处
                setNestedScrollingParentForType(type, p);
                //注释3处
                ViewParentCompat.onNestedScrollAccepted(p, child, mView, axes, type);
                return true;
            }
            if (p instanceof View) {
                child = (View) p;
            }
            p = p.getParent();
        }
    }
    return false;
}

```

如果开启了嵌套滑动就遍历父控件，询问是否有父控件想要处理。

注释1处，ViewParentCompat的onStartNestedScroll方法

```
public static boolean onStartNestedScroll(ViewParent parent, View child, View target,
            int nestedScrollAxes, int type) {
        if (parent instanceof NestedScrollingParent2) {
            //注释1处，首先尝试调用NestedScrollingParent2的API
            return ((NestedScrollingParent2) parent).onStartNestedScroll(child, target,
                    nestedScrollAxes, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {注释2处，NestedScrollingParent只处理正常的触摸类型

            if (Build.VERSION.SDK_INT >= 21) {//大于21版本直接调用ViewParent的方法即可。
                try {
                    return parent.onStartNestedScroll(child, target, nestedScrollAxes);
                } catch (AbstractMethodError e) {
                    Log.e(TAG, "ViewParent " + parent + " does not implement interface "
                            + "method onStartNestedScroll", e);
                }
            } else if (parent instanceof NestedScrollingParent) {
                //NestedScrollingParent处理
                return ((NestedScrollingParent) parent).onStartNestedScroll(child, target,
                        nestedScrollAxes);
            }
        }
        return false;
    }
```

注释1处，首先尝试调用NestedScrollingParent2的`onStartNestedScroll(child, target,nestedScrollAxes, type)`。
注释2处，NestedScrollingParent只处理正常的触摸类型，丢弃掉type参数。`onStartNestedScroll(child, target,nestedScrollAxes)`。
大于21版本直接调用ViewParent的方法即可。否则调用NestedScrollingParent处理。

后面的分析中，我们就只看NestedScrollingParent2相关的内容。

回到NestedScrollingChildHelper的startNestedScroll方法的注释2处。

```
private void setNestedScrollingParentForType(@NestedScrollType int type, ViewParent p) {
    switch (type) {
        case TYPE_TOUCH:
            mNestedScrollingParentTouch = p;
            break;
        case TYPE_NON_TOUCH:
            mNestedScrollingParentNonTouch = p;
            break;
    }
}
```

根据滑动的类型将ViewParent赋值给不同的变量保存。


注释3处
```
//注释3处
ViewParentCompat.onNestedScrollAccepted(p, child, mView, axes, type);
```

回到RecyclerView的onTouchEvent方法的注释2处

```
//注释2处


最终会调用NestedScrollingParent2的onNestedScrollAccepted方法。
if (dispatchNestedPreScroll(
        canScrollHorizontally ? dx : 0,
        canScrollVertically ? dy : 0,
        mReusableIntPair, mScrollOffset, TYPE_TOUCH
)) {
    //注释3处
    dx -= mReusableIntPair[0];
    dy -= mReusableIntPair[1];
    //...
}
```

注释2处，RecyclerView的dispatchNestedPreScroll方法
```
@Override
public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow,
        int type) {
    return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow,
            type);
}
```

最终会调用NestedScrollingParent2的onNestedPreScroll方法。

注释3处，如果NestedScrollingParent2消耗了一些滑动距离，减去消耗的距离。


RecyclerView的onTouchEvent方法的注释4处，调用scrollByInternal方法

```
    boolean scrollByInternal(int x, int y, MotionEvent ev) {
        int unconsumedX = 0;
        int unconsumedY = 0;
        int consumedX = 0;
        int consumedY = 0;

        if (mAdapter != null) {
            mReusableIntPair[0] = 0;
            mReusableIntPair[1] = 0;
            //内部自身滑动
            scrollStep(x, y, mReusableIntPair);
            consumedX = mReusableIntPair[0];
            consumedY = mReusableIntPair[1];
            //剩余的滑动距离
            unconsumedX = x - consumedX;
            unconsumedY = y - consumedY;
        }
        if (!mItemDecorations.isEmpty()) {
            invalidate();
        }

        mReusableIntPair[0] = 0;
        mReusableIntPair[1] = 0;
        //将剩余的滑动距离再次分发给处理嵌套滑动的父View
        dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset,
                TYPE_TOUCH, mReusableIntPair);
        unconsumedX -= mReusableIntPair[0];
        unconsumedY -= mReusableIntPair[1];
        boolean consumedNestedScroll = mReusableIntPair[0] != 0 || mReusableIntPair[1] != 0;

        //...

       
        return consumedNestedScroll || consumedX != 0 || consumedY != 0;
    }

```

方法内部首先调用scrollStep方法滑动，然后计算出剩余的滑动距离。然后将剩余的滑动距离再次分发给处理嵌套滑动的父View。

```
@Override
public final void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
        int dyUnconsumed, int[] offsetInWindow, int type, @NonNull int[] consumed) {
    getScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed);
}
```

最终会调用NestedScrollingParent2的onNestedScroll方法。

到目前为止，NestedScrolling2和NestedScrolling并没有差别。

回到RecyclerView的onTouchEvent方法的注释5处，调用fling方法。从这里开始NestedScrolling2和NestedScrolling的处理逻辑产生了差异。

```
public boolean fling(int velocityX, int velocityY) {
    //...
    //注释1处
    if (!dispatchNestedPreFling(velocityX, velocityY)) {
        final boolean canScroll = canScrollHorizontal || canScrollVertical;
        //注释2处
        dispatchNestedFling(velocityX, velocityY, canScroll);

        //...

        if (canScroll) {
            int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
            if (canScrollHorizontal) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
            }
            if (canScrollVertical) {
                nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
            }
            //注释3处
            startNestedScroll(nestedScrollAxis, TYPE_NON_TOUCH);

            velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
            velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
            //注释4处
            mViewFlinger.fling(velocityX, velocityY);
            return true;
        }
    }
    return false;
}
```

注释1处，先询问外层控件是否要处理惯性滑动，如果要处理，直接返回false，自身不滑动。

```
@Override
public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
    return getScrollingChildHelper().dispatchNestedPreFling(velocityX, velocityY);
}
```

会调用外层控件的onNestedPreFling方法。代码就不贴了。

如果外层控件的onNestedPreFling方法反返回了true，内层控件自身不滑动。

如果`!dispatchNestedPreFling(velocityX, velocityY)`为true，说明外层控件没有处理惯性滑动。注释2处：

```
@Override
public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
    return getScrollingChildHelper().dispatchNestedFling(velocityX, velocityY, consumed);
}
```

会调用外层控件的onNestedFling方法。代码就不贴了。


注释3处，这里和NestedScrolling有差异。

```
@Override
public boolean startNestedScroll(int axes, int type) {
    return getScrollingChildHelper().startNestedScroll(axes, type);
}
```

注释3处，再次调用了startNestedScroll方法，但是这时候滑动类型是`TYPE_NON_TOUCH`。

NestedScrollingChildHelper的startNestedScroll方法

```
public boolean startNestedScroll(@ScrollAxis int axes, @NestedScrollType int type) {
    //根据嵌套滑动的类型来获取NestedScrollingParent
    if (hasNestedScrollingParent(type)) {
        // 嵌套滑动已经在处理过程中，直接返回true
        return true;
    }
    if (isNestedScrollingEnabled()) {
        ViewParent p = mView.getParent();
        View child = mView;
        //遍历父级控件
        while (p != null) {
            //注释1处
            if (ViewParentCompat.onStartNestedScroll(p, child, mView, axes, type)) {
                //注释2处
                setNestedScrollingParentForType(type, p);
                //注释3处
                ViewParentCompat.onNestedScrollAccepted(p, child, mView, axes, type);
                return true;
            }
            if (p instanceof View) {
                child = (View) p;
            }
            p = p.getParent();
        }
    }
    return false;
}

```

注释1处，如果外层控件要处理`TYPE_NON_TOUCH`类型的滚动，外层控件的`onStartNestedScroll`返回true。
注释3处，外层控件调用onNestedScrollAccepted方法。


我们回到fling方法的注释4处

```
//注释4处
mViewFlinger.fling(velocityX, velocityY);
```

ViewFlinger的fling方法。

```
public void fling(int velocityX, int velocityY) {
    setScrollState(SCROLL_STATE_SETTLING);
    mLastFlingX = mLastFlingY = 0;
    //fling
    mOverScroller.fling(0, 0, velocityX, velocityY,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
    //请求重新绘制
    postOnAnimation();
}
```

Scroller怎么实现滚动的，这里就不展开了。

ViewFlinger实现了Runnable接口。

```
@Override
public void run() {
    //...           
    final OverScroller scroller = mOverScroller;
    //如果滚动还没结束
    if (scroller.computeScrollOffset()) {
        final int x = scroller.getCurrX();
        final int y = scroller.getCurrY();
        int unconsumedX = x - mLastFlingX;
        int unconsumedY = y - mLastFlingY;
        mLastFlingX = x;
        mLastFlingY = y;
        int consumedX = 0;
        int consumedY = 0;

        // Nested Pre Scroll
        mReusableIntPair[0] = 0;
        mReusableIntPair[1] = 0;
        //注释1处，每一帧的计算出来的滚动距离先分发到外层控件
        if (dispatchNestedPreScroll(unconsumedX, unconsumedY, mReusableIntPair, null,
                TYPE_NON_TOUCH)) {
            //注释2处，减去外层控件消耗的距离
            unconsumedX -= mReusableIntPair[0];
            unconsumedY -= mReusableIntPair[1];
        }

        // Local Scroll
        if (mAdapter != null) {
            mReusableIntPair[0] = 0;
            mReusableIntPair[1] = 0;
            //注释3处，自身滚动
            scrollStep(unconsumedX, unconsumedY, mReusableIntPair);
            consumedX = mReusableIntPair[0];
            consumedY = mReusableIntPair[1];
            //减去自身滚动的距离
            unconsumedX -= consumedX;
            unconsumedY -= consumedY;
        }

        // Nested Post Scroll
        mReusableIntPair[0] = 0;
        mReusableIntPair[1] = 0;
        //注释4处，将剩余的滑动距离分发给外层控件
        dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, null,
                TYPE_NON_TOUCH, mReusableIntPair);
        unconsumedX -= mReusableIntPair[0];
        unconsumedY -= mReusableIntPair[1];

        //...
        //继续请求重绘
        postOnAnimation();
    }
}
```

注释1处，每一帧的计算出来的滚动距离先分发到外层控件，外层控件调用onNestedPreScroll先滚动。

注释2处，减去外层控件消耗的距离。

注释3处，自身滚动。

注释4处，将剩余的滑动距离分发给外层控件。外层控件调用onNestedScroll来决定是否要进行处理。


这里总结一下

1. NestedScrolling2分发滚动事件的时候，区分了type，是正常的触摸滚动还是惯性滑动。

2. 内层控件调用dispatchNestedFling来处理惯性滑动。如果外层控件处理了惯性滑动，也就是外层控件的onNestedPreFling方法返回了true。那么
NestedScrolling2和NestedScrolling的惯性滑动效果没有什么差异。

3. 如果外层控件没有处理惯性滑动，也就是外层控件的onNestedPreFling方法返回了false。那么内层控件自身开始惯性滑动，但是在惯性滑动的每一帧，
通过Scroller计算出来的滚动距离都先分发给外层控件。外层控件可以先消耗部分滚动距离，然后内层控件再自身滚动。

4. 第3条是内外层控件流畅的滑动关键。


* [Android 8.0 NestedScrollingChild2与NestedScrollingParent2实现RecyclerView阻尼回弹效果](https://blog.csdn.net/qq_42944793/article/details/88417127)
* [Android嵌套滑动机制NestedScrolling](https://developer.aliyun.com/article/181671)
* [Andorid 嵌套滑动机制 NestedScrollingParent2和NestedScrollingChild2 详解](https://juejin.cn/post/6844903960432607246#heading-12)
* [Android NestedScrolling机制完全解析 带你玩转嵌套滑动](https://blog.csdn.net/lmj623565791/article/details/52204039)















