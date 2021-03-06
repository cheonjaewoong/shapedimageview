package io.woong.shapedimageview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import io.woong.shapedimageview.util.*
import kotlin.math.min

/**
 * The parent class of all shaped image view.
 *
 * This class cannot be used alone cause it is abstract class.
 * To use shaped imageview, use classes that inherit from this class.
 *
 * To inherit from this class, you should call [applyAttributes] on init
 * for obtaining common attributes from context.
 *
 * ```
 *      class CustomImageView @JvmOverloads constructor(
 *      context: Context,
 *      attrs: AttributeSet? = null,
 *      defStyle: Int = 0
 *      ) : ShapedImageView(context, attrs, defStyle) {
 *
 *          init {
 *              applyAttributes(attrs, defStyle)
 *          }
 *
 *      ...
 *      }
 * ```
 *
 * If your imageview has custom attributes, you can override [applyAttributes] method.
 * In this case, you should call super method.
 *
 * ```
 *      init {
 *          applyAttributes(attrs, defStyle)
 *      }
 *
 *      override fun applyAttributes(attrs: AttributeSet?, defStyle: Int) {
 *          super.applyAttributes(attrs, defStyle)
 *          val a = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyle, 0)
 *          try {
 *              // Obtaining custom attributes.
 *          } finally {
 *              a.recycle()
 *          }
 *      }
 * ```
 *
 * To draw shape, override [onDraw] method.
 * When override [onDraw], you should call super method for updating bitmap shader to image paint
 * and adding shadow layer to shadow paint.
 *
 * It is recommended that draw shadow first, border next and image at last.
 *
 * ```
 *      override fun onDraw(canvas: Canvas) {
 *          super.onDraw(canvas)
 *
 *          if (shadowEnabled) {
 *              canvas.drawOval(shadowRect, shadowPaint)
 *          }
 *
 *          if (borderEnabled) {
 *              canvas.drawOval(borderRect, borderPaint)
 *          }
 *
 *          canvas.drawOval(imageRect, imagePaint)
 *      }
 * ```
 *
 * @see io.woong.shapedimageview.OvalImageView
 * @see io.woong.shapedimageview.CircleImageView
 * @see io.woong.shapedimageview.RoundImageView
 * @see io.woong.shapedimageview.RoundSquareImageView
 * @see io.woong.shapedimageview.CutCornerImageView
 * @see io.woong.shapedimageview.CutCornerSquareImageView
 * @see io.woong.shapedimageview.FormulableImageView
 */
abstract class ShapedImageView : AppCompatImageView {
    protected val imagePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val imageRect: RectF = RectF()
    /** The bitmap image to draw in this imageview. */
    private var image: Bitmap? = null
    /** The drawable image to check this imageview needs to update [image] property. */
    private var imageCache: Drawable? = null

    protected val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val borderRect: RectF = RectF()
    @ColorInt
    private var borderColor: Int = DEFAULT_BORDER_COLOR
    var borderSize: Float = DEFAULT_BORDER_SIZE
        set(value) {
            field = if (value > 0) value else DEFAULT_BORDER_SIZE
            measureBounds()
            invalidate()
        }

    var borderEnabled: Boolean = DEFAULT_BORDER_ENABLED
        set(value) {
            field = value
            measureBounds()
            invalidate()
        }

    protected val shadowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val shadowRect: RectF = RectF()
    @ColorInt
    private var shadowColor: Int = DEFAULT_SHADOW_COLOR
    var shadowSize: Float = DEFAULT_SHADOW_SIZE
        set(value) {
            field = if (value > 0) value else DEFAULT_SHADOW_SIZE
            measureBounds()
            invalidate()
        }

    var shadowEnabled: Boolean = DEFAULT_SHADOW_ENABLED
        set(value) {
            field = value
            measureBounds()
            invalidate()
        }

    /**
     * The width and height size ratio of this imageview.
     * If it is [NOT_FIXED_ASPECT_RATIO], the view never set fixed with and height size.
     */
    private var aspectRatio: Double = NOT_FIXED_ASPECT_RATIO

    /**
     * This property determines that this imageview should have same width and height size.
     * If this view is regular shape, it's sizes will be set to same value in [onMeasure].
     *
     * If it is `true`, this view will have same width and height.
     * If `false`, this view can have different width and height.
     */
    protected open var isRegularShape: Boolean = false

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle)

    init {
        // Set default scale type
        this.scaleType = ScaleType.CENTER_CROP
    }

    /**
     * Obtain XML attributes from context and apply them to this imageview.
     * This method have to be called at initializing.
     */
    @CallSuper
    protected open fun applyAttributes(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ShapedImageView, defStyle, 0)

        try {
            if (a.hasValue(R.styleable.ShapedImageView_android_scaleType)) {
                val type = a.getInt(R.styleable.ShapedImageView_android_scaleType, ScaleType.CENTER_CROP.ordinal)
                this.scaleType = when (type) {
                    ScaleType.MATRIX.ordinal -> ScaleType.MATRIX
                    ScaleType.FIT_XY.ordinal -> ScaleType.FIT_XY
                    ScaleType.FIT_START.ordinal -> ScaleType.FIT_START
                    ScaleType.FIT_CENTER.ordinal -> ScaleType.FIT_CENTER
                    ScaleType.FIT_END.ordinal -> ScaleType.FIT_END
                    ScaleType.CENTER.ordinal -> ScaleType.CENTER
                    ScaleType.CENTER_CROP.ordinal -> ScaleType.CENTER_CROP
                    ScaleType.CENTER_INSIDE.ordinal -> ScaleType.CENTER_INSIDE
                    else -> throw IllegalArgumentException("$type is not a scale type.")
                }
            }

            if (!isRegularShape) {
                if (a.hasValue(R.styleable.ShapedImageView_aspect_ratio)) {
                    val ratioString = a.getString(R.styleable.ShapedImageView_aspect_ratio)
                    val ratioPair = parseRatioString(ratioString)
                    this.aspectRatio = if (ratioPair.first == 0.0 || ratioPair.second == 0.0) {
                        0.0
                    } else {
                        ratioPair.first / ratioPair.second
                    }
                }
            }

            if (a.hasValue(R.styleable.ShapedImageView_border_size)) {
                this.borderSize = a.getDimension(R.styleable.ShapedImageView_border_size, DEFAULT_BORDER_SIZE)
            }

            if (a.hasValue(R.styleable.ShapedImageView_border_color)) {
                this.borderColor = a.getColor(R.styleable.ShapedImageView_border_color, DEFAULT_BORDER_COLOR)
            }

            if (a.hasValue(R.styleable.ShapedImageView_border_enabled)) {
                this.borderEnabled = a.getBoolean(R.styleable.ShapedImageView_border_enabled, DEFAULT_BORDER_ENABLED)
            }

            if (a.hasValue(R.styleable.ShapedImageView_shadow_size)) {
                this.shadowSize = a.getDimension(R.styleable.ShapedImageView_shadow_size, DEFAULT_SHADOW_SIZE)
            }

            if (a.hasValue(R.styleable.ShapedImageView_shadow_color)) {
                this.shadowColor = a.getColor(R.styleable.ShapedImageView_shadow_color, DEFAULT_SHADOW_COLOR)
            }

            if (a.hasValue(R.styleable.ShapedImageView_shadow_enabled)) {
                this.shadowEnabled = a.getBoolean(R.styleable.ShapedImageView_shadow_enabled, DEFAULT_SHADOW_ENABLED)
            }
        } finally {
            a.recycle()
        }
    }

    /**
     * Parses aspect ratio string to pair of number.
     *
     * @param ratioString A string to parse to 2 numbers.
     *
     * @return Pair of 2 numbers to calculate aspect ratio,
     * Pair(0, 0) when [ratioString] is null or blank or illegal format.
     *
     * @throws IllegalArgumentException When given string is illegal format.
     */
    private fun parseRatioString(ratioString: String?): Pair<Double, Double> {
        if (ratioString.isNullOrBlank()) {
            return 0.0 to 0.0
        } else {
            val rstr = ratioString.trim()
            if (!rstr.contains(":") || rstr.startsWith(":") || rstr.endsWith(":")) {
                throw IllegalArgumentException("'$ratioString' is illegal format. aspect_ratio should be 'number:number' format.")
            }

            val split = rstr.split(":")
            val num1 = split[0].toDoubleOrNull()
            val num2 = split[1].toDoubleOrNull()

            if (num1 == null) {
                throw IllegalArgumentException("'${split[0]}' is not a number. aspect_ratio should consist of numbers.")
            } else if (num2 == null) {
                throw IllegalArgumentException("'${split[1]}' is not a number. aspect_ratio should consist of numbers.")
            }

            return num1 to num2
        }
    }

    @ColorInt
    fun getBorderColor(): Int = borderColor

    fun setBorderColorResource(@ColorRes resId: Int) {
        val color = ResourcesCompat.getColor(resources, resId, context.theme)
        setBorderColor(color)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setBorderColor(@ColorInt color: Int) {
        borderColor = color
        measureBounds()
        invalidate()
    }

    @ColorInt
    fun getShadowColor(): Int = shadowColor

    fun setShadowColorResource(@ColorRes resId: Int) {
        val color = ResourcesCompat.getColor(resources, resId, context.theme)
        setShadowColor(color)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setShadowColor(@ColorInt color: Int) {
        shadowColor = color
        measureBounds()
        invalidate()
    }

    /**
     * Sets aspect ratio of this imageview.
     *
     * The ratio should be width / height.
     * For instance, if width is 1000px and height is 500px,
     * its ratio is 2 (1000 / 500).
     *
     * To set this imageview to not use fixed aspect ratio,
     * pass [NOT_FIXED_ASPECT_RATIO] or negative value.
     *
     * @param ratio A calculated aspect ratio.
     */
    fun setAspectRatio(ratio: Double) {
        this.aspectRatio = if (ratio < 0) NOT_FIXED_ASPECT_RATIO else ratio
        invalidate()
        requestLayout()
    }

    /**
     * Sets aspect ratio of this imageview.
     *
     * The ratio will be width / height.
     * For instance, if width is 100 and height is 50,
     * and real width size is 1000px, the real height size will be 500px.
     *
     * To set this imageview to not use fixed aspect ratio,
     * pass [NOT_FIXED_ASPECT_RATIO] to [width] or [height].
     *
     * @param width A width size to calculate ratio.
     * @param height a Height size to calculate ratio.
     */
    fun setAspectRatio(width: Double, height: Double) {
        if (width <= 0 || height <= 0) {
            setAspectRatio(NOT_FIXED_ASPECT_RATIO)
        } else {
            setAspectRatio(width / height)
        }
    }

    /**
     * A lifecycle method for measuring this view's size.
     *
     * In this method, it measures the views width and height and call [measureBounds].
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)

        if (isRegularShape) {
            val s = min(w, h)
            setMeasuredDimension(s, s)

            val sf = s.toFloat()
            measureBounds(sf, sf)
        } else {
            val width: Int
            val height: Int

            if (aspectRatio == NOT_FIXED_ASPECT_RATIO) {
                width = w
                height = h
                setMeasuredDimension(w, h)
            } else {
                when {
                    w == 0 -> {
                        width = (h * aspectRatio).toInt()
                        height = h
                    }
                    h == 0 -> {
                        width = w
                        height = (w / aspectRatio).toInt()
                    }
                    else -> {
                        val wm = MeasureSpec.getMode(widthMeasureSpec)
                        val hm = MeasureSpec.getMode(heightMeasureSpec)

                        when {
                            wm == MeasureSpec.EXACTLY -> {
                                width = w
                                height = (w / aspectRatio).toInt()
                            }
                            hm == MeasureSpec.EXACTLY -> {
                                width = (h * aspectRatio).toInt()
                                height = h
                            }
                            else -> {
                                width = w
                                height = (w / aspectRatio).toInt()
                            }
                        }
                    }
                }
                setMeasuredDimension(width, height)
            }

            measureBounds(width.toFloat(), height.toFloat())
        }
    }

    /**
     * Measure drawing bounds of this imageview's image, border and shadow.
     *
     * @param w The width size of this imageview. Default value is current width size.
     * @param h The height size of this imageview. Default value is current height size.
     */
    protected fun measureBounds(w: Float = this.width.toFloat(), h: Float = this.height.toFloat()) {
        val shadowAdjust = if (shadowEnabled) shadowSize * 1.5f else 0f
        val borderAdjust = if (borderEnabled) borderSize else 0f
        val adjustSum = shadowAdjust + borderAdjust

        if (shadowEnabled) {
            this.shadowRect.set(
                this.paddingLeft.toFloat() + shadowAdjust,
                this.paddingTop.toFloat() + shadowAdjust,
                w - this.paddingRight - shadowAdjust,
                h - this.paddingBottom - shadowAdjust
            )
        }

        if (borderEnabled) {
            this.borderRect.set(
                this.paddingLeft.toFloat() + shadowAdjust,
                this.paddingTop.toFloat() + shadowAdjust,
                w - this.paddingRight - shadowAdjust,
                h - this.paddingBottom - shadowAdjust
            )
        }

        this.imageRect.set(
            this.paddingLeft.toFloat() + adjustSum,
            this.paddingTop.toFloat() + adjustSum,
            w - this.paddingRight - adjustSum,
            h - this.paddingBottom - adjustSum
        )
    }

    /**
     * A lifecycle method for drawing image to this imageview.
     *
     * To override this method,
     * you should call super before drawing for update [imagePaint]'s bitmap shader
     * and shadow layer of [shadowPaint].
     */
    @CallSuper
    override fun onDraw(canvas: Canvas) {
        updateShader()
        updateBorderColor()
        updateShadowLayer()

        if (background != null) {
            background.apply {
                setBounds(0, 0, width, height)
                draw(canvas)
            }
        }
    }

    private fun updateShadowLayer() {
        this.shadowPaint.setShadowLayer(shadowSize, 0f, shadowSize / 2, shadowColor)
    }

    private fun updateBorderColor() {
        this.borderPaint.color = borderColor
    }

    private fun updateShader() {
        val needToUpdateBitmap = this.drawable != null && this.drawable != this.imageCache

        val bounds = Bounds.from(this)

        if (needToUpdateBitmap) {
            this.imageCache = this.drawable
            this.image = this.drawable.toBitmap()
        }

        this.image?.let {
            val shader = if (Build.VERSION.SDK_INT >= 31) {
                BitmapShader(it, Shader.TileMode.DECAL, Shader.TileMode.DECAL)
            } else {
                BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }

            shader.setLocalMatrix(
                if (Build.VERSION.SDK_INT >= 31) {
                    when (this.scaleType) {
                        ScaleType.MATRIX -> this.imageMatrix
                        ScaleType.FIT_XY -> createFitXYMatrix(it, bounds)
                        ScaleType.FIT_START -> createFitStartMatrix(it, bounds)
                        ScaleType.FIT_CENTER -> createFitCenterMatrix(it, bounds)
                        ScaleType.FIT_END -> createFitEndMatrix(it, bounds)
                        ScaleType.CENTER -> createCenterMatrix(it, bounds)
                        ScaleType.CENTER_CROP -> createCenterCropMatrix(it, bounds)
                        ScaleType.CENTER_INSIDE -> createCenterInsideMatrix(it, bounds)
                        null -> throw IllegalArgumentException("Cannot apply null matrix to BitmapShader.")
                    }
                } else {
                    when (this.scaleType) {
                        ScaleType.MATRIX -> this.imageMatrix
                        ScaleType.FIT_XY -> createFitXYMatrix(it, bounds)
                        ScaleType.CENTER_CROP -> createCenterCropMatrix(it, bounds)
                        else -> Matrix()
                    }
                }
            )

            imagePaint.shader = shader
        }
    }

    /**
     * Controls how the image should be resized or moved to match the size of this ImageView.
     *
     * Android 31 and later versions support all scale types.
     * But previous versions support
     * [matrix][android.widget.ImageView.ScaleType.MATRIX],
     * [fit-xy][android.widget.ImageView.ScaleType.FIT_XY] and
     * [center-crop][android.widget.ImageView.ScaleType.CENTER_CROP],
     *
     * @param scaleType [android.widget.ImageView.ScaleType.CENTER_CROP]
     *
     * @throws IllegalArgumentException When given scale type is not supported.
     */
    override fun setScaleType(scaleType: ScaleType) {
        if (Build.VERSION.SDK_INT >= 31) {
            super.setScaleType(scaleType)
        } else {
            when (scaleType) {
                ScaleType.MATRIX,
                ScaleType.FIT_XY,
                ScaleType.CENTER_CROP -> super.setScaleType(scaleType)
                else -> throw IllegalArgumentException(
                    "ShapedImageView does not support ${scaleType.name}.\u0020"
                    + "You should use Android API 31 or later to use ${scaleType.name}."
                )
            }
        }
    }

    companion object {
        /** The constants means this view doesn't use fixed aspect ratio. */
        const val NOT_FIXED_ASPECT_RATIO: Double = 0.0

        /** The default value of [ShapedImageView]'s border size. */
        const val DEFAULT_BORDER_SIZE: Float = 0f

        /** The default value of [ShapedImageView]'s border color. */
        @JvmStatic
        @ColorInt
        val DEFAULT_BORDER_COLOR: Int = Color.parseColor("#444444")

        /** The default value of [ShapedImageView]'s border enabled status. */
        const val DEFAULT_BORDER_ENABLED: Boolean = true

        /** The default value of [ShapedImageView]'s shadow size. */
        const val DEFAULT_SHADOW_SIZE: Float = 0f

        /** The default value of [ShapedImageView]'s shadow color. */
        @JvmStatic
        @ColorInt
        val DEFAULT_SHADOW_COLOR: Int = Color.parseColor("#888888")

        /** The default value of [ShapedImageView]'s shadow enabled status. */
        const val DEFAULT_SHADOW_ENABLED: Boolean = true
    }
}
