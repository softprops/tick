package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{Bundle, Handler}
import android.widget.{ImageView, TextView, Toast, LinearLayout}
import android.content.{BroadcastReceiver, Context,
                        ContentResolver, Intent, IntentFilter}
import android.graphics.{Typeface}
import android.provider.MediaStore.Images
import android.view.{Gravity,Window,WindowManager}
import android.util.{Log,TypedValue}
import java.util.Calendar
import java.text.SimpleDateFormat

class MainActivity extends Activity {

  val SelectedImage = 1

  val mHandler = new Handler()
  val rcvr = new BroadcastReceiver() {
    def onReceive(cxt: Context, intent: Intent) = intent.getAction() match {
      case Intent.ACTION_TIME_TICK | Intent.ACTION_TIME_CHANGED |
      Intent.ACTION_TIMEZONE_CHANGED =>
        mHandler.post(new Runnable() {
          def run = tick()
        })
    }
  }
  lazy val hTens: TextView = textView
  lazy val hOnes: TextView = textView
  lazy val mTens: TextView = textView
  lazy val mOnes: TextView = textView
  lazy val sep = new TextView(this) {
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30f)
    setText("_")
  }

  def tick() {
    val t = Calendar.getInstance().getTime().getTime
    new SimpleDateFormat("HH").format(t).split("") match {
      case Array(_, tens, ones) =>
        hTens.setText(tens)
      hOnes.setText(ones)
    }
    new SimpleDateFormat("mm").format(t).split("") match {
      case Array(_, tens, ones) =>
        mTens.setText(tens)
      mOnes.setText(ones)
    }
  }

  def textView = {
    val t = new TextView(this)
    t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 180f)
    t.setTypeface(Typeface.SERIF)
    t
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)

    setContentView(new LinearLayout(this) {
      setHorizontalGravity(Gravity.CENTER)
      setOrientation(LinearLayout.HORIZONTAL)
      addView(hTens)
      addView(hOnes)
      addView(sep)
      addView(mTens)
      addView(mOnes)
      requestLayout()
    })

    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
      WindowManager.LayoutParams.FLAG_BLUR_BEHIND
    )

    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN
    )

    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )

    registerReceiver(rcvr, new IntentFilter() {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    })

    tick()
  }

  def selectPic =
    startActivityForResult(
       new Intent(
        Intent.ACTION_PICK,
        Images.Media.EXTERNAL_CONTENT_URI /*INTERNAL_CONTENT_URI*/
       ), SelectedImage
    )

  protected override def onActivityResult(
      reqCode: Int, resCode: Int, data: Intent
    ) =
    reqCode match {
      case SelectedImage =>
        resCode match {
          case Activity.RESULT_OK =>
            val uri = data.getData
            toast("okay! %s" format uri)
            try{
              val bm = Images.Media.getBitmap(getContentResolver(), uri)
              toast("bm %s" format bm)
              val iv = new ImageView(this)
              iv.setImageBitmap(bm)
              setContentView(iv)
            } catch { case e => toast("err %s" format e) }
          case _ => toast("unexpected resCode")
        }
      case _ => toast("unexpected reqCode")
    }

  def toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
