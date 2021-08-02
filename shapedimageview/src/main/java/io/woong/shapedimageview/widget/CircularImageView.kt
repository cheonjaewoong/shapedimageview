package io.woong.shapedimageview.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import io.woong.shapedimageview.ShapedImageView

/**
 * An image view that display image in circle shape.
 *
 * Scale type is always [center crop][android.widget.ImageView.ScaleType.CENTER_CROP].
 * And also it's width and height size is same.
 */
class CircularImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ShapedImageView(context, attrs, defStyle) {

    /** X position of image center. */
    private var imageCenterX: Float = 0f
    /** Y position of image center. */
    private var imageCenterY: Float = 0f
    /** Radius size of image. */
    private var imageRadius: Float = 0f

    /** Radius size of border. */
    private var borderRadius: Float = 0f

    init {
        applyCommonAttributes(attrs, defStyle)
    }

    /**
     * Update sizes and values like image size, shadow, border and other values.
     */
    override fun remeasure() {
        val shadowAdjustment = if (shadowEnabled && shadowAdjustEnabled) shadowSize * 2 else 0f
        val borderAdjustment = if (borderEnabled) borderSize else 0f

        imageCenterX = (paddingLeft + (viewSize - paddingRight)) / 2f
        imageCenterY = (paddingTop + (viewSize - paddingBottom)) / 2f

        imageRadius = imageSize / 2f - shadowAdjustment - borderAdjustment
        borderRadius = imageRadius + borderAdjustment
    }

    /**
     * This method is invoked after [onDraw].
     *
     * @param canvas Canvas to draw image view.
     */
    override fun postOnDraw(canvas: Canvas) {
        if (shadowEnabled) {
            canvas.drawCircle(imageCenterX, imageCenterY, borderRadius, shadowPaint)
        }

        if (borderEnabled) {
            canvas.drawCircle(imageCenterX, imageCenterY, borderRadius, borderPaint)
        }

        canvas.drawCircle(imageCenterX, imageCenterY, imageRadius, imagePaint)
    }
}
