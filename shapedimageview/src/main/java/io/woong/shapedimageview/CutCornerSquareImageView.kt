package io.woong.shapedimageview

import android.content.Context
import android.util.AttributeSet

/**
 * The shaped imageview that draws image in corner-cut square shape.
 *
 * The difference between [CutCornerImageView] and this view is that
 * it has same with and height size.
 *
 * If width or height size is smaller than another,
 * this view's size will be set to the smaller one.
 *
 * Aspect ratio setting is not working for this view
 * because this view always has same width and height.
 *
 * To use this imageview in xml, you can add this view simply like below code.
 *
 * ```
 *      <io.woong.shapedimageview.CutCornerSquareImageView
 *          android:layout_width="200dp"
 *          android:layout_height="200dp"
 *          android:src="@drawable/sample" />
 * ```
 *
 * In [CutCornerSquareImageView], there are some attributes to configure this imageview.
 * The most important attribute is `radius`.
 *
 * ```
 *      <!-- To apply all corner radius in one line. -->
 *      <io.woong.shapedimageview.CutCornerSquareImageView
 *          android:layout_width="200dp"
 *          android:layout_height="200dp"
 *          android:src="@drawable/sample"
 *          app:radius="32dp" />
 *
 *      <!-- To apply each corner radius. -->
 *      <io.woong.shapedimageview.CutCornerSquareImageView
 *          android:layout_width="200dp"
 *          android:layout_height="200dp"
 *          android:src="@drawable/sample"
 *          app:top_left_radius="8dp"
 *          app:top_right_radius="12dp"
 *          app:bottom_right_radius="16dp"
 *          app:bottom_left_radius="24dp" />
 * ```
 */
class CutCornerSquareImageView : CutCornerImageView {
    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        applyAttributes(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int): super(context, attrs, defStyle) {
        applyAttributes(attrs, defStyle)
    }

    init {
        isRegularShape = true
    }
}
