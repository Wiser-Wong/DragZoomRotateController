package com.wiser.dragzoomrotatelayout

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.FrameLayout
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * @author Wiser
 *
 * 用途:  拖拽 放大缩小 旋转控件 可自己实现这些功能，也可添加子View 让子View实现这些功能
 *      (但请注意：子View实现这些功能，只针对只有一个子View的时候）
 */
open class DragZoomRotateController @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context!!, attrs, defStyleAttr) {

    /**
     * 最小缩小比例
     */
    private var minScale = 0.5f

    /**
     * 中间放大比例
     */
    private var midScale = 1.5f

    /**
     * 最大放大比例
     */
    private var maxScale = 2.0f

    /**
     * 记录移动X
     */
    private var recordTranslationX = 0f

    /**
     * 记录移动Y
     */
    private var recordTranslationY = 0f

    /**
     * 记录伸缩比例
     */
    private var recordScale = 1f

    /**
     * 记录旋转角度
     */
    private var recordRotation = 0f

    /**
     * 移动过程中临时变量
     */
    private var actionX = 0f
    private var actionY = 0f
    private var downX = 0f
    private var downY = 0f
    private var spacing = 0f
    private var degree = 0f

    /**
     * 0=未选择，1=拖动，2=缩放
     */
    private var moveType = 0

    /**
     * 拖拽监听
     */
    private var onDragListener: OnDragListener? = null

    /**
     * 旋转监听
     */
    private var onRotateListener: OnRotateListener? = null

    /**
     * 放大缩小监听
     */
    private var onScaleListener: OnScaleListener? = null

    /**
     * 抬起监听
     */
    private var onTouchUpListener: OnTouchUpListener? = null

    /**
     * 宽度 如果没有子View 该宽度就是指该View的父控件宽度，如果有子View 且只有一个，那么该宽度指改View的宽度
     */
    private var mWidth = 0

    /**
     * 高度 如果没有子View 该高度就是指该View的父控件高度，如果有子View 且只有一个，那么该高度指改View的高度
     */
    private var mHeight = 0

    /**
     * 是否子View 超出可移动区域回弹
     */
    private var isLimitSpringAnim = true

    /**
     * 是否可以拖动
     */
    private var isDrag = true

    /**
     * 是否可以放大缩小
     */
    private var isScale = true

    /**
     * 是否可以旋转
     */
    private var isRotate = true

    /**
     * 是否双击放大缩小
     */
    private var isDoubleScale = true

    /**
     * 是否拖拽到边界不能继续拖动
     */
    private var isDragLimit = true

    /**
     * 插值器
     */
    private var interpolator: Interpolator = SpringInterpolator(0.8f)

    /**
     * 放大动画执行时间
     */
    private var durationScale: Long = 800

    /**
     * 回弹动画执行时间
     */
    private var durationSpring:Long = 500

    /**
     * 手势
     */
    private var detector: GestureDetector? = null

    init {
        val ta: TypedArray? =
            context?.obtainStyledAttributes(attrs, R.styleable.DragZoomRotateController)
        this.isLimitSpringAnim =
            ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isLimitSpringAnim, true) == true
        this.isDrag = ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isDrag, true) == true
        this.isScale =
            ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isScale, true) == true
        this.isRotate =
            ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isRotate, true) == true
        this.isDoubleScale =
            ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isDoubleScale, true) == true
        this.isDragLimit =
            ta?.getBoolean(R.styleable.DragZoomRotateController_dzr_isDragLimit, false) == true
        this.minScale =
            ta?.getFloat(R.styleable.DragZoomRotateController_dzr_minScale, minScale) ?: minScale
        this.midScale =
            ta?.getFloat(R.styleable.DragZoomRotateController_dzr_midScale, midScale) ?: midScale
        this.maxScale =
            ta?.getFloat(R.styleable.DragZoomRotateController_dzr_maxScale, maxScale) ?: maxScale
        ta?.recycle()

        if (maxScale < 1f) {
            maxScale = 2f
        }

        if (midScale > maxScale) {
            midScale = 1f + (maxScale - 1f) / 2
        }

        if (minScale > 1f) {
            minScale = 0.5f
        }

        detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                var currentValue = 0f
                var limitValue = 0f
                if (getViewScaleX() < 1f) {
                    currentValue = getViewScaleX()
                    limitValue = 1f
                } else if (getViewScaleX() >= 1f && getViewScaleX() < midScale) {
                    currentValue = getViewScaleX()
                    limitValue = midScale
                } else if (getViewScaleX() >= midScale && getViewScaleX() < maxScale) {
                    currentValue = getViewScaleX()
                    limitValue = maxScale
                } else if (getViewScaleX() == maxScale) {
                    currentValue = maxScale
                    limitValue = 1f
                }
                setScaleAnimator(currentValue, limitValue)
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
                return false
            }
        })
    }

    fun setScaleAnimator(currentValue: Float, limitValue: Float) {
        val valueAnimator = ValueAnimator.ofFloat(currentValue, limitValue)
        valueAnimator.duration = durationScale
        valueAnimator.interpolator = interpolator
        valueAnimator.addUpdateListener {
            val value: Float? = it.animatedValue as? Float
            if (value != null) {
                setViewScaleX(value)
                setViewScaleY(value)
                recordScale = value
            }
        }
        valueAnimator.start()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                moveType = 1
                actionX = event.rawX
                actionY = event.rawY
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                moveType = 2
                spacing = getSpacing(event)
                degree = getDegree(event)
            }
            MotionEvent.ACTION_MOVE -> if (moveType == 1) { // 单指
                // 拖动
                if (isDrag) {
                    if (onDragListener?.isCustomDrag() == true) {
                        onDragListener?.onDrag(event)
                    } else {
                        recordTranslationX = recordTranslationX + event.rawX - actionX
                        recordTranslationY = recordTranslationY + event.rawY - actionY
                        // 超出边界不能继续拖动，只能停留在边界处
                        if (isDragLimit) {
                            // 左边界
                            val left = -getView().left + (getView().width * (getView().scaleX - 1)) / 2
                            // 右边界
                            val right =
                                (mWidth - getView().left) - (getView().width + (getView().width * (getView().scaleX - 1)) / 2)
                            // 上边界
                            val top = -getView().top + (getView().height * (getView().scaleY - 1)) / 2
                            // 下边界
                            val bottom =
                                (mHeight - getView().top) - (getView().height + (getView().height * (getView().scaleY - 1)) / 2)
                            if (recordTranslationX < left) {
                                recordTranslationX = left
                            }
                            if (recordTranslationX > right) {
                                recordTranslationX = right
                            }
                            if (recordTranslationY < top) {
                                recordTranslationY = top
                            }
                            if (recordTranslationY > bottom) {
                                recordTranslationY = bottom
                            }
                        }
                        setViewTranslationX(recordTranslationX)
                        setViewTranslationY(recordTranslationY)
                        actionX = event.rawX
                        actionY = event.rawY
                    }
                }
            } else if (moveType == 2) { // 双指
                // 放大缩小
                if (isScale) {
                    if (onScaleListener?.isCustomScale() == true) {
                        onScaleListener?.onScale(event, recordScale, recordScale)
                    } else {
                        recordScale = recordScale * getSpacing(event) / spacing
                        if (recordScale > maxScale) {
                            recordScale = maxScale
                        }
                        if (recordScale < minScale) {
                            recordScale = minScale
                        }
                        setViewScaleX(recordScale)
                        setViewScaleY(recordScale)
                    }
                }
                // 旋转
                if (isRotate) {
                    if (onRotateListener?.isCustomRotate() == true) {
                        onRotateListener?.onRotate(event, recordRotation)
                    } else {
                        recordRotation = (recordRotation + getDegree(event) - degree)
                        if (recordRotation > 360) {
                            recordRotation -= 360
                        }
                        if (recordRotation < -360) {
                            recordRotation += 360
                        }
                        setViewRotation(recordRotation)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                moveType = 0
                if (onTouchUpListener?.isCustomTouchUp() == true) {
                    onTouchUpListener?.onTouchUp(event)
                } else {
                    // 如果设置了限制回弹动画则执行
                    if (isLimitSpringAnim) {
                        limit()
                    }
                    recordTranslationX = getViewTranslationX()
                    recordTranslationY = getViewTranslationY()
                }
            }
        }

        // 是否双击放大缩小
        if (isDoubleScale)
            detector?.onTouchEvent(event)

        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isChildOneCount()) {
            this.mWidth = width
            this.mHeight = height
        } else {
            val viewGroup: ViewGroup? = parent as? ViewGroup
            this.mWidth = viewGroup?.width ?: 0
            this.mHeight = viewGroup?.height ?: 0
        }
    }

    /**
     * 限制区域 回弹
     */
    private fun limit() {
        if (mWidth == 0 || mHeight == 0) return
        // 左边界
        val left = -getView().left + (getView().width * (getView().scaleX - 1)) / 2
        // 右边界
        val right =
            (mWidth - getView().left) - (getView().width + (getView().width * (getView().scaleX - 1)) / 2)
        // 上边界
        val top = -getView().top + (getView().height * (getView().scaleY - 1)) / 2
        // 下边界
        val bottom =
            (mHeight - getView().top) - (getView().height + (getView().height * (getView().scaleY - 1)) / 2)

        // 限制左边滚动距离
        val limitLeft = getView().left - (getView().width * (getView().scaleX - 1)) / 2
        // 限制右边滚动距离
        val limitRight =
            (mWidth - getView().left) - (getView().width + (getView().width * (getView().scaleX - 1)) / 2)
        // 限制上边滚动距离
        val limitTop = getView().top - (getView().height * (getView().scaleY - 1)) / 2
        // 限制下边滚动距离
        val limitBottom =
            (mHeight - getView().top) - (getView().height + (getView().height * (getView().scaleY - 1)) / 2)
        // 如果放大的宽度超过了总宽度，则回弹到中心位置
        if (getView().width * getView().scaleX > mWidth) {
            scrollSpringAnimLimit(getRecordTranslationX(), (limitRight - limitLeft) / 2, true)
        } else {
            // 滑动超过左边界 回弹
            if (getRecordTranslationX() + limitLeft <= 0) {
                scrollSpringAnimLimit(getRecordTranslationX(), left, true)
            }
            // 滑动超过右边界 回弹
            if (getRecordTranslationX() - limitRight >= 0) {
                scrollSpringAnimLimit(getRecordTranslationX(), right, true)
            }
        }
        // 如果放大的高度超过了总高度，则回弹到中心位置
        if (getView().height * getView().scaleY > mHeight) {
            scrollSpringAnimLimit(getRecordTranslationY(), (limitBottom - limitTop) / 2, false)
        } else {
            // 滑动超过上边界 回弹
            if (getRecordTranslationY() + limitTop <= 0) {
                scrollSpringAnimLimit(getRecordTranslationY(), top, false)
            }
            // 滑动超过下边界 回弹
            if (getRecordTranslationY() - limitBottom >= 0) {
                scrollSpringAnimLimit(getRecordTranslationY(), bottom, false)
            }
        }
    }

    /**
     * 获取触碰两点间距离
     */
    private fun getSpacing(event: MotionEvent): Float {
        //通过三角函数得到两点间的距离
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        val scale = if (isChildOneCount()) getChildAt(0).scaleX else 1f
        return sqrt(x * x + y * y.toDouble()).toFloat() / scale
    }

    /**
     * 获取旋转角度
     */
    private fun getDegree(event: MotionEvent): Float {
        //得到两个手指间的旋转角度
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        val rotation = if (isChildOneCount()) getChildAt(0).rotation else 0f
        return Math.toDegrees(radians).toFloat() - rotation
    }

    /**
     * 设置最小缩小参数
     */
    fun setMinScale(minScale: Float) {
        this.minScale = minScale
    }

    /**
     * 设置最大放大参数
     */
    fun setMaxScale(maxScale: Float) {
        this.maxScale = maxScale
    }

    /**
     * 设置双击放大缩小时，中间参数
     */
    fun setMidScale(midScale: Float) {
        this.midScale = midScale
    }

    fun getMinScale(): Float = minScale

    fun getMaxScale(): Float = maxScale

    fun getMidScale(): Float = midScale

    /**
     * 设置是否可以拖拽
     */
    fun setIsDrag(isDrag: Boolean) {
        this.isDrag = isDrag
    }

    /**
     * 设置是否可以放大缩小
     */
    fun setIsScale(isScale: Boolean) {
        this.isScale = isScale;
    }

    /**
     * 设置是否可以旋转
     */
    fun setIsRotate(isRotate: Boolean) {
        this.isRotate = isRotate
    }

    /**
     * 设置是否滑动限制时拖拽到屏幕外自动回弹到边界处动画
     */
    fun setIsLimitSprintAnim(isLimitSpringAnim: Boolean) {
        this.isLimitSpringAnim = isLimitSpringAnim
    }

    /**
     * 设置是否拖拽到边界不可继续拖拽，停留在边界处
     */
    fun setIsDragLimit(isDragLimit: Boolean) {
        this.isDragLimit = isDragLimit
    }

    fun isDrag(): Boolean = isDrag

    fun isScale(): Boolean = isScale

    fun isRotate(): Boolean = isRotate

    fun isLimitSpringAnim(): Boolean = isLimitSpringAnim

    fun isDragLimit(): Boolean = isDragLimit

    fun setOnDragListener(onDragListener: OnDragListener?) {
        this.onDragListener = onDragListener
    }

    fun setOnRotateListener(onRotateListener: OnRotateListener?) {
        this.onRotateListener = onRotateListener
    }

    fun setOnScaleListener(onScaleListener: OnScaleListener?) {
        this.onScaleListener = onScaleListener
    }

    fun setOnTouchUpListener(onTouchUpListener: OnTouchUpListener?) {
        this.onTouchUpListener = onTouchUpListener
    }

    interface OnDragListener {
        fun onDrag(event: MotionEvent)
        fun isCustomDrag(): Boolean
    }

    interface OnRotateListener {
        fun onRotate(event: MotionEvent, rotation: Float)
        fun isCustomRotate(): Boolean
    }

    interface OnScaleListener {
        fun onScale(event: MotionEvent, scaleX: Float, scaleY: Float)
        fun isCustomScale(): Boolean
    }

    interface OnTouchUpListener {
        fun onTouchUp(event: MotionEvent)
        fun isCustomTouchUp(): Boolean
    }

    /**
     * 超出限制回弹动画
     */
    fun scrollSpringAnimLimit(currentValue: Float, limitValue: Float, isLandscape: Boolean) {
        val valueAnimator = ValueAnimator.ofFloat(currentValue, limitValue)
        valueAnimator.duration = durationSpring
        valueAnimator.interpolator = interpolator
        valueAnimator.addUpdateListener {
            val value: Float? = it.animatedValue as? Float
            if (isLandscape) {
                setViewTranslationX(value ?: 0f)
                setRecordTranslationX(value ?: 0f)
            } else {
                setViewTranslationY(value ?: 0f)
                setRecordTranslationY(value ?: 0f)
            }
        }
        valueAnimator.start()
    }

    /**
     * 设置动画插值器
     */
    fun setInterpolator(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    /**
     * 设置放大缩小动画执行时间
     */
    fun setDurationScale(duration: Long) {
        this.durationScale = duration
    }

    /**
     * 设置回弹动画执行时间
     */
    fun setDurationSpring(duration: Long) {
        this.durationSpring = duration
    }

    /**
     * 是否只有一个子View
     */
    fun isChildOneCount(): Boolean {
        return childCount == 1
    }

    /**
     * 获取子View
     * 如果子View数量为0或者大于1个，那么返回该View就是针对该View 父控件的子View
     * 如果子View数量是1 则返回该子View
     */
    private fun getView(): View {
        return if (childCount == 0 || childCount > 1) {
            this
        } else {
            getChildAt(0)
        }
    }

    fun setRecordTranslationX(x: Float) {
        if (x.isNaN()) return
        this.recordTranslationX = x
    }

    fun setRecordTranslationY(y: Float) {
        if (y.isNaN()) return
        this.recordTranslationY = y
    }

    fun getRecordTranslationX() = recordTranslationX

    fun getRecordTranslationY() = recordTranslationY

    fun setViewTranslationX(x: Float) {
        if (x.isNaN()) return
        if (childCount == 0 || childCount > 1) {
            translationX = x
        } else {
            getChildAt(0).translationX = x
        }
    }

    fun setViewTranslationY(y: Float) {
        if (y.isNaN()) return
        if (childCount == 0 || childCount > 1) {
            translationY = y
        } else {
            getChildAt(0).translationY = y
        }
    }

    fun getViewTranslationX(): Float {
        return if (childCount == 0 || childCount > 1) {
            translationX
        } else {
            getChildAt(0).translationX
        }
    }

    fun getViewTranslationY(): Float {
        return if (childCount == 0 || childCount > 1) {
            translationY
        } else {
            getChildAt(0).translationY
        }
    }

    fun setViewScaleX(x: Float) {
        if (x.isNaN()) return
        if (childCount == 0 || childCount > 1) {
            scaleX = x
        } else {
            getChildAt(0).scaleX = x
        }
    }

    fun setViewScaleY(y: Float) {
        if (y.isNaN()) return
        if (childCount == 0 || childCount > 1) {
            scaleY = y
        } else {
            getChildAt(0).scaleY = y
        }
    }

    fun getViewScaleX(): Float {
        return if (childCount == 0 || childCount > 1) {
            scaleX
        } else {
            getChildAt(0).scaleX
        }
    }

    fun getViewScaleY(): Float {
        return if (childCount == 0 || childCount > 1) {
            scaleY
        } else {
            getChildAt(0).scaleY
        }
    }

    fun setViewRotation(x: Float) {
        if (x.isNaN()) return
        if (childCount == 0 || childCount > 1) {
            rotation = x
        } else {
            getChildAt(0).rotation = x
        }
    }

    fun getViewRotation(): Float {
        return if (childCount == 0 || childCount > 1) {
            rotation
        } else {
            getChildAt(0).rotation
        }
    }

    fun setActionX(x: Float) {
        if (x.isNaN()) return
        this.actionX = x
    }

    fun getActionX() = actionX

    fun setActionY(y: Float) {
        if (y.isNaN()) return
        this.actionY = y
    }

    fun getDownX() = downX

    fun getDownY() = downY

    fun getActionY() = actionY
}