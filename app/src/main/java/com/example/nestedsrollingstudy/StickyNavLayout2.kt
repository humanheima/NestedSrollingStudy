package com.example.nestedsrollingstudy

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat

/**
 * Created by dumingwei on 2020-02-15.
 * Desc:
 */
class StickyNavLayout2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), NestedScrollingParent2 {

    private val TAG: String = "StickyNavLayout2"

    private lateinit var mTop: View
    private var mNav: View? = null
    private lateinit var mNestedChild: View

    private var mTopViewHeight: Int = 0

    /**
     * 用来兼容Android 5.0 Lollipop (API 21)或更老的版本
     */
    private val mNestedScrollingParentHelper: NestedScrollingParentHelper =
        NestedScrollingParentHelper(this)

    override fun onFinishInflate() {
        super.onFinishInflate()
        mTop = findViewById(R.id.nestedScrollTopView)
        mNav = findViewById(R.id.nestedScrollIndicator)
        mNestedChild = findViewById(R.id.nestedScrollContent)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /**
         * 先测量一遍，获取自身的高度和子控件的高度
         */
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        Log.e(
            TAG,
            "onMeasure1: measuredHeight = $measuredHeight , mNav.measuredHeight = ${mNav?.measuredHeight} , mNestedChild.measuredHeight = ${mNestedChild.measuredHeight}"
        )

        /**
         * 这里为什么要将 mNestedChild高度设置为mNestedChild原本的高度加上mNav的高度呢？因为mTop滑出去以后，mNav和mNestedChild应该占据
         * StickyNavLayout的整个高度，不然StickyNavLayout底部会有空白。
         */
        val params = mNestedChild.layoutParams
        params?.height = measuredHeight - (mNav?.measuredHeight ?: 0)

        /**
         * 子控件mNestedChild的高度发生了变化，重新测量一遍，最终的结果是该控件本身高度没有发生变化，但是子控件mNestedChild的变高了
         */
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.e(
            TAG,
            "onMeasure2: measuredHeight = $measuredHeight , mNav.measuredHeight = ${mNav?.measuredHeight} , mNestedChild.measuredHeight = ${mNestedChild.measuredHeight}"
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTopViewHeight = mTop.measuredHeight
        Log.e(TAG, "onSizeChanged: mTopViewHeight =$mTopViewHeight")
    }


    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return (nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // 向上滑动（手指从下向上滑）, dy>0
        val hiddenTop = dy > 0 && scrollY < mTopViewHeight


        // dy<0 向下滑动（手指从上向下滑）
        val canScrollVertically = target.canScrollVertically(-1)

        Log.e(
            TAG,
            "ViewGroup onNestedPreScroll: dy= $dy canScrollVertically = $canScrollVertically "
        )

        val showTop = dy < 0 && scrollY >= 0 && !canScrollVertically


        if (hiddenTop || showTop) {
            //滑动y距离
            scrollBy(0, dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        //do nothing
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return false
    }

    override fun onStopNestedScroll(child: View) {
        mNestedScrollingParentHelper.onStopNestedScroll(child)
    }

    override fun scrollTo(x: Int, y: Int) {
        val distanceY = if (y < 0) {
            0
        } else {
            if (y > mTopViewHeight) {
                mTopViewHeight
            } else {
                y
            }
        }

        if (distanceY != scrollY) {
            super.scrollTo(x, distanceY)
        }
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        Log.e(TAG, "onStartNestedScroll: ")
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        Log.e(TAG, "NestedScrollingParent2 onNestedScrollAccepted: ")
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        // 向上滑动（手指从下向上滑）, dy>0
        val hiddenTop = dy > 0 && scrollY < mTopViewHeight

        // dy<0 向下滑动（手指从上向下滑）

        val canScrollVertically = target.canScrollVertically(-1)

        Log.e(
            TAG,
            "NestedScrollingParent2 onNestedPreScroll: dy= $dy canScrollVertically = $canScrollVertically"
        )

        val showTop = dy < 0 && scrollY >= 0 && !canScrollVertically
        if (hiddenTop || showTop) {
            //滑动y距离
            scrollBy(0, dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        Log.e(TAG, "NestedScrollingParent2 onNestedScroll: ")
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        Log.e(TAG, "NestedScrollingParent2 onStopNestedScroll: ")
        mNestedScrollingParentHelper.onStopNestedScroll(target, type)
    }

}