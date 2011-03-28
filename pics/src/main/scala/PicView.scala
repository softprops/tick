package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{Bundle, Environment=> Env, Handler}
import android.widget.{ImageView, TextView, Toast, LinearLayout}
import android.content.{BroadcastReceiver, Context,
                        ContentResolver, Intent, IntentFilter}
import android.graphics.{Typeface}
import android.provider.MediaStore
import MediaStore.Images
import android.view.{Gravity, View, Window, WindowManager}
import android.util.{Log, TypedValue, AttributeSet}
import android.graphics.{Bitmap, Canvas, Color, Paint,
                        PorterDuffXfermode, PorterDuff, RectF, Path}
import android.net.Uri
import java.util.Calendar
import java.text.SimpleDateFormat
import java.io.File

abstract class PicView(context: Context,  attrs: AttributeSet, defStyle: Int)
  extends ImageView(context, attrs, defStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  /** Array of 8 values, 4 pairs of [X,Y] radii */
  def radii: Array[Float]

  override def onDraw(canvas: Canvas) = {
    canvas.clipPath(new Path {
      addRoundRect(
       new RectF(0, 0, PicView.this.getWidth(), PicView.this.getHeight()),
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

  def radii = Array(10.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 10.0f, 10.0f)
}

class PicViewR(context: Context,  attrs: AttributeSet, defStyle: Int)
  extends PicView(context, attrs, defStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  def radii = Array(0.0f, 0.0f, 10.0f, 10.0f, 10.0f, 10.0f, 0.0f, 0.0f)
}
