本片文章学习分析一下Android中的嵌套滑动。涉及到的类有

* NestedScrollingChild
* NestedScrollingParent
* ViewParent
* RecyclerView

下一篇会分析NestedScrollingChild2相关的内容。

为了方便叙述，我们把在内部的滑动View称为内层控件。外部滑动的控件称为外层控件。

内层控件需要实现NestedScrollingChild接口。比如RecyclerView是实现了NestedScrollingChild接口的。

在`Build.VERSION.SDK_INT`小于21版本以下，外层控件必须实现NestedScrollingParent接口。在21版本及以上版本，外层控件不再需要实现NestedScrollingParent接口，因为ViewParent接口中已经定义了嵌套滑动外层控件相关的方法，ViewGroup实现了ViewParent接口。

注意：我们的是`android-25`版本里面的RecyclerView源码，因为高版本的RecyclerView是实现了`NestedScrollingChild2`接口，下一篇文章会介绍。

嵌套滑动的整个流程是从内层控件发起的。先看一下流程图。

![NestedScrolling流程.jpg](https://upload-images.jianshu.io/upload_images/3611193-b4c7fbb29ee2db8d.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


我们以RecyclerView为例进行分析。

```java
public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild {
    //...
}
```

RecyclerView的onTouchEvent方法精简版

```java
@Override
public boolean onTouchEvent(MotionEvent e) {
    //...
    final MotionEvent vtev = MotionEvent.obtain(e);
    final int action = MotionEventCompat.getActionMasked(e);
    final int actionIndex = MotionEventCompat.getActionIndex(e);

    if (action == MotionEvent.ACTION_DOWN) {
        mNestedOffsets[0] = mNestedOffsets[1] = 0;
    }
    vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

    switch (action) {
        case MotionEvent.ACTION_DOWN: {
            //...
            //注释1处
            startNestedScroll(nestedScrollAxis);
        } break;
        case MotionEvent.ACTION_MOVE: {
            final int index = e.findPointerIndex(mScrollPointerId);

            final int x = (int) (e.getX(index) + 0.5f);
            final int y = (int) (e.getY(index) + 0.5f);
            int dx = mLastTouchX - x;
            int dy = mLastTouchY - y;
            //注释2处
            if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                //注释3处
                dx -= mScrollConsumed[0];
                dy -= mScrollConsumed[1];
                vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                // Updated the nested offsets
                mNestedOffsets[0] += mScrollOffset[0];
                mNestedOffsets[1] += mScrollOffset[1];
            }
            //...
            if (mScrollState == SCROLL_STATE_DRAGGING) {
                mLastTouchX = x - mScrollOffset[0];
                mLastTouchY = y - mScrollOffset[1];
                //注释4处
                if (scrollByInternal(
                        canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0,
                        vtev)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } break;
        case MotionEvent.ACTION_UP: {
            mVelocityTracker.addMovement(vtev);
            eventAddedToVelocityTracker = true;

            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
            //计算fling速度
            final float xvel = canScrollHorizontally ?
                    -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
            final float yvel = canScrollVertically ?
                    -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
            //注释5处
            if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                setScrollState(SCROLL_STATE_IDLE);
            }
            //注释6处
            resetTouch();
        } break;
    }
    //...
    return true;
}
```

ACTION_DOWN的注释1处

```java
//注释1处
startNestedScroll(nestedScrollAxis);
```

这里开始嵌套滑动的整个流程。

```java
@Override
public boolean startNestedScroll(int axes) {
    return getScrollingChildHelper().startNestedScroll(axes);
}
```

调用NestedScrollingChildHelper的startNestedScroll方法。

```java
/**
 *
 * @param axes 支持嵌套滚动的方向。
 *
 * @return true 如果找到了协作的嵌套滑动的父控件并成功启动嵌套滑动则返回true。
 */
public boolean startNestedScroll(int axes) {
    if (hasNestedScrollingParent()) {
        // 已经在嵌套滑动中了，直接返回true
        return true;
    }
    //启用了嵌套滑动
    if (isNestedScrollingEnabled()) {
        //获取父控件
        ViewParent p = mView.getParent();
        View child = mView;
        //注释1处，向上遍历View层级，找到处理嵌套滑动的ViewParent
        while (p != null) {
            //注释2处
            if (ViewParentCompat.onStartNestedScroll(p, child, mView, axes)) {
                //当前正在处理嵌套滑动的父控件
                mNestedScrollingParent = p;
                ViewParentCompat.onNestedScrollAccepted(p, child, mView, axes);
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

注释1处，向上遍历View层级，找到处理嵌套滑动的ViewParent。

注释2处，调用`ViewParentCompat.onStartNestedScroll`来询问是否某个父级控件想处理嵌套滑动，如果`ViewParentCompat.onStartNestedScroll`返回true，说明父控件想要处理处理嵌套滑动。

ViewParentCompat的onStartNestedScroll方法。

```java
public static boolean onStartNestedScroll(ViewParent parent, View child, View target,
            int nestedScrollAxes) {
    //找到对应的ViewParentCompatImpl来处理
    return IMPL.onStartNestedScroll(parent, child, target, nestedScrollAxes);
}
```

```java
static final ViewParentCompatImpl IMPL;
static {
    final int version = Build.VERSION.SDK_INT;
    if (version >= 21) {
        IMPL = new ViewParentCompatLollipopImpl();
    } else if (version >= 19) {
        IMPL = new ViewParentCompatKitKatImpl();
    } else if (version >= 14) {
        IMPL = new ViewParentCompatICSImpl();
    } else {
        IMPL = new ViewParentCompatStubImpl();
    }
}
```
ViewParentCompatImpl在不同的版本中有不同的实现。在21版本以下，外层控件必须实现`NestedScrollingParent`接口。在21版本及以上版本，外层控件不再需要实现`NestedScrollingParent`接口，因为ViewParent接口中已经定义了嵌套滑动外层控件相关的方法，ViewGroup实现了ViewParent接口。

这里我们就看21以上的版本。

ViewParentCompatLollipopImpl的onStartNestedScroll方法。

```java
@Override
public boolean onStartNestedScroll(ViewParent parent, View child, View target,
                int nestedScrollAxes) {
    return ViewParentCompatLollipop.onStartNestedScroll(parent, child, target,
                nestedScrollAxes);
}
```

ViewParentCompatLollipop的onStartNestedScroll方法。

```java
public static boolean onStartNestedScroll(ViewParent parent, View child, View target,
            int nestedScrollAxes) {
    try {
        //注释1处
        return parent.onStartNestedScroll(child, target, nestedScrollAxes);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                    "method onStartNestedScroll", e);
        return false;
    }
}
```

注释1处，最终调用ViewParent的onStartNestedScroll方法。如果返回true，紧接着会调用ViewParent的onNestedScrollAccepted方法。

```java
public static void onNestedScrollAccepted(ViewParent parent, View child, View target,
            int nestedScrollAxes) {
    try {
        //调用ViewParent的onNestedScrollAccepted方法。
        parent.onNestedScrollAccepted(child, target, nestedScrollAxes);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                    "method onNestedScrollAccepted", e);
    }
}
```


我们回到RecyclerView的onTouchEvent方法的ACTION_MOVE处

```java
//注释2处
if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
    //注释3处
    dx -= mScrollConsumed[0];
    dy -= mScrollConsumed[1];
    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
    // Updated the nested offsets
    mNestedOffsets[0] += mScrollOffset[0];
    mNestedOffsets[1] += mScrollOffset[1];
 }
```

注释2处，在内层控件开始滑动之前先询问外层控件是否要先处理滑动，如果dispatchNestedPreScroll返回true，表明外层控件消耗了某些滑动距离，所以我们在注释3处减去外层控件消耗的滑动距离。

```java
@Override
public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
    return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
}
```

```java
/**
 * Dispatch one step of a nested pre-scrolling operation to the current nested scrolling parent.
 *
 * @return true 如果外层控件消耗了滑动距离，返回true。
 */
public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
    //启用了嵌套滑动并且有父控件处理嵌套滑动
    if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
        if (dx != 0 || dy != 0) {
            int startX = 0;
            int startY = 0;
            if (offsetInWindow != null) {
                mView.getLocationInWindow(offsetInWindow);
                startX = offsetInWindow[0];
                startY = offsetInWindow[1];
            }

            if (consumed == null) {
                if (mTempNestedScrollConsumed == null) {
                    mTempNestedScrollConsumed = new int[2];
                }
                consumed = mTempNestedScrollConsumed;
            }
            consumed[0] = 0;
            consumed[1] = 0;
            //注释1处
            ViewParentCompat.onNestedPreScroll(mNestedScrollingParent, mView, dx, dy, consumed);

            if (offsetInWindow != null) {
                mView.getLocationInWindow(offsetInWindow);
                offsetInWindow[0] -= startX;
                offsetInWindow[1] -= startY;
            }
            return consumed[0] != 0 || consumed[1] != 0;
        } else if (offsetInWindow != null) {
            offsetInWindow[0] = 0;
            offsetInWindow[1] = 0;
        }
    }
    return false;
}
```

注释一处，调用外层控件的onNestedPreScroll方法。

```java
public static void onNestedPreScroll(ViewParent parent, View target, int dx, int dy,
            int[] consumed) {
    try {
        parent.onNestedPreScroll(target, dx, dy, consumed);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                "method onNestedPreScroll", e);
    }
}
```

我们回到RecyclerView的onTouchEvent方法的ACTION_MOVE处：

```java
//注释4处
if (scrollByInternal(
        canScrollHorizontally ? dx : 0,
        canScrollVertically ? dy : 0,
        vtev)) {
    getParent().requestDisallowInterceptTouchEvent(true);
}
```

```java
boolean scrollByInternal(int x, int y, MotionEvent ev) {
    //...
    if (mAdapter != null) {
        if (x != 0) {
            //注释1处
            consumedX = mLayout.scrollHorizontallyBy(x, mRecycler, mState);
            unconsumedX = x - consumedX;
        }
        if (y != 0) {
            //注释2处
            consumedY = mLayout.scrollVerticallyBy(y, mRecycler, mState);
            unconsumedY = y - consumedY;
        }
    }
    //注释3处
    if (dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset)) {
    //...
    }
}
```

注释1处，注释2处，RecyclerView先自己滑动，计算出消耗的滑动距离consumedX、consumedY，没有消耗的滑动距离 unconsumedX、unconsumedY。

注释3处，自身滑动之后通知外层控件。最终会调用外层控件的onNestedScroll方法。

```java
public static void onNestedScroll(ViewParent parent, View target, int dxConsumed,
            int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    try {
        //调用外层控件的onNestedScroll方法。
        parent.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                "method onNestedScroll", e);
    }
}
```

我们回到RecyclerView的onTouchEvent方法的ACTION_UP处：

```java
//注释5处
if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
    setScrollState(SCROLL_STATE_IDLE);
}
//注释6处
resetTouch();
```

注释5处，开始惯性滑动。

```java
public boolean fling(int velocityX, int velocityY) {
  //...
  //注释1处
  if (!dispatchNestedPreFling(velocityX, velocityY)) {
     //内层控件自身是否可以滑动
     final boolean canScroll = canScrollHorizontal || canScrollVertical;
     //注释2处
     dispatchNestedFling(velocityX, velocityY, canScroll);

     if (mOnFlingListener != null && mOnFlingListener.onFling(velocityX, velocityY)) {
         return true;
     }

     if (canScroll) {
         velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
         velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
         //注释3处
         mViewFlinger.fling(velocityX, velocityY);
         return true;
     }
  }
  return false;
}
```

注释1处，先询问外层控件是否要处理惯性滑动，如果要处理，直接返回false，自身不滑动。

```java
public static boolean onNestedPreFling(ViewParent parent, View target, float velocityX,
            float velocityY) {
    try {
        //调用外层控件的onNestedPreFling方法。
        return parent.onNestedPreFling(target, velocityX, velocityY);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                "method onNestedPreFling", e);
        return false;
    }
}
```

注释2处，最终会调用外层控件的onNestedFling方法。
```java
public static boolean onNestedFling(ViewParent parent, View target, float velocityX,
            float velocityY, boolean consumed) {
    try {
        return parent.onNestedFling(target, velocityX, velocityY, consumed);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                "method onNestedFling", e);
        return false;
    }
}
```

注释3处，内层控件自身滑动。

```java
 mViewFlinger.fling(velocityX, velocityY);
```

我们回到RecyclerView的onTouchEvent方法的ACTION_UP处：

```java
//注释6处
resetTouch();
```

```java
private void resetTouch() {
    if (mVelocityTracker != null) {
        mVelocityTracker.clear();
    }
    //停止嵌套滑动
    stopNestedScroll();
    releaseGlows();
}
```

```java
public static void onStopNestedScroll(ViewParent parent, View target) {
    try {
        //注释1处
        parent.onStopNestedScroll(target);
    } catch (AbstractMethodError e) {
        Log.e(TAG, "ViewParent " + parent + " does not implement interface " +
                "method onStopNestedScroll", e);
    }
}
```

注释1处，最终调用外层控件的onStopNestedScroll方法。一次完整的嵌套滑动流程结束。

这里注意一下：外层控件的onStopNestedScroll方法被调用的时候，外层控件或者内层控件的惯性滑动可能并没有结束。

参考链接：

* [Android 8.0 NestedScrollingChild2与NestedScrollingParent2实现RecyclerView阻尼回弹效果](https://blog.csdn.net/qq_42944793/article/details/88417127)
* [Android嵌套滑动机制NestedScrolling](https://developer.aliyun.com/article/181671)
* [Andorid 嵌套滑动机制 NestedScrollingParent2和NestedScrollingChild2 详解](https://juejin.cn/post/6844903960432607246#heading-12)
* [Android NestedScrolling机制完全解析 带你玩转嵌套滑动](https://blog.csdn.net/lmj623565791/article/details/52204039)

