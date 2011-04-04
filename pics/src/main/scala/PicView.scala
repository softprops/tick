package me.lessis

import android.widget.ImageView
import android.content.Context
import android.util.{AttributeSet,Log}
import android.graphics.{Canvas, RectF, Path}
import android.widget.Toast


abstract class PicView(context: Context,  attrs: AttributeSet, defStyle: Int)
  extends ImageView(context, attrs, defStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  /** Array of 8 values, 4 pairs of [X,Y] radii */
  val radii: Array[Float]

  override def onDraw(canvas: Canvas) = {
    val (x,y) = (0, 0)
    canvas.clipPath(new Path {
      addRoundRect(
       new RectF(x, y, PicView.this.getWidth(), PicView.this.getHeight()),
       radii,
       Path.Direction.CW
      )
    })
    super.onDraw(canvas)
  }
}

class PicViewL(context: Context,  attrs: AttributeSet, defStyle: Int)
  extends PicView(context, attrs, defStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  val radii = Array(10.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 10.0f, 10.0f)
}

class PicViewR(context: Context,  attrs: AttributeSet, defStyle: Int)
  extends PicView(context, attrs, defStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  val radii = Array(0.0f, 0.0f, 10.0f, 10.0f, 10.0f, 10.0f, 0.0f, 0.0f)
}
