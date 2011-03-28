 package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{Bundle, Environment=> Env, Handler}
import android.widget.{ImageView, TextView, LinearLayout}
import android.content.{BroadcastReceiver, Context,
                        ContentResolver, Intent, IntentFilter}
import android.graphics.{Typeface}
import android.provider.MediaStore
import MediaStore.Images
import android.view.{Gravity, View, Window, WindowManager}
import android.util.{Log, TypedValue}
import android.graphics.{Bitmap, Canvas, Color, Paint,
                        PorterDuffXfermode, PorterDuff, RectF}
import android.net.Uri
import android.hardware.{Sensor, SensorEvent, SensorEventListener,
                       SensorManager}

import java.util.Calendar
import java.text.SimpleDateFormat
import java.io.File

object MainActivity {
  val DigitWidth = 200
  val DigitHeight = 360
}


class MainActivity extends Activity with Toasted {
  import MainActivity._

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

  val sense = new SensorEventListener {
    def onAccuracyChanged(sensor: Sensor, accuracy:Int) { toast("accuracy changed") }
    def onSensorChanged(evt: SensorEvent) { /*toast("sensor changed") */}
    //private def deleteFiles = (0 to 10).foreach(toast)
  }

  lazy val hTens = view[PicView](R.id.h_tens)
  lazy val hOnes = view[PicView](R.id.h_ones)
  lazy val mTens = view[PicView](R.id.m_tens)
  lazy val mOnes = view[PicView](R.id.m_ones)
  lazy val meridiem = view[TextView](R.id.meridiem)
  lazy val sensorManager = getSystemService("sensor"/*Activity.SENSOR_SERVICE*/).
                              asInstanceOf[SensorManager]

  def tick() {
    val t = Calendar.getInstance().getTime().getTime
    new SimpleDateFormat("hh").format(t).split("") match {
      case Array(_, tens, ones) =>
        applyNum(hTens, tens.toInt)
        applyNum(hOnes, ones.toInt)
    }
    new SimpleDateFormat("mm").format(t).split("") match {
      case Array(_, tens, ones) =>
        applyNum(mTens, tens.toInt)
        applyNum(mOnes, ones.toInt)
    }
    meridiem.setText(
      new SimpleDateFormat("aa").format(t).toLowerCase
    )
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.clock)

    (hTens :: hOnes :: mTens :: mOnes :: Nil) foreach {
      _.setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = {
          new JumpDialog(MainActivity.this, new OnJumpListener {
            def onJump(c: CharSequence) = selectPic(c.toString.toInt)
          }).numbers.inRowsOf(5).show()
          quickToast("Select a digit")
        }
      })
    }

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

  /** @param which of 0-9 */
  private def selectPic(which: Int) =
    startActivityForResult(
      new Intent(
        Intent.ACTION_PICK,
        Images.Media.INTERNAL_CONTENT_URI
      ) {
        setType("image/*")
      }, which
    )

  /** @param reqCode 0-9 indicates an image was selected, a masked 4th bit
   *                 indicates a cropping result of 0-9's image */
  protected override def onActivityResult(
    reqCode: Int, resCode: Int, data: Intent
  ) =
    reqCode match {
      case n if((n & (1<<4)) > 0) =>
         resCode match {
           case Activity.RESULT_OK =>
             val dig = n & ~(1<<4)
             Images.Media.getBitmap(getContentResolver(), croppedFile(dig)) match {
               case null => toast(
                 "failed to retrieve cropFile %s" format croppedFile(dig)
               )
               case bm => // todo inplace update current digits, if number
             }
           case _ => toast("unexpected result code %s" format resCode)
         }
      case 0|1|2|3|4|5|6|7|8|9 =>
        resCode match {
          case Activity.RESULT_OK =>
            val uri = data.getData
            val crop = new Intent("com.android.camera.action.CROP") {
              setType("image/*")
            }
            getPackageManager().queryIntentActivities(crop, 0) match {
               case null => toast(
                 "could not find a cropping intent for uri %s :(" format uri
               )
               case ia =>
                 Env.getExternalStorageState() match {
                   case Env.MEDIA_MOUNTED =>
                     crop.setData(uri)
                     crop.putExtra("scale", false)
                     crop.putExtra("outputX", DigitWidth)
                     crop.putExtra("outputY", DigitHeight)
                     crop.putExtra("aspectX", DigitWidth)
                     crop.putExtra("aspectY", DigitHeight)
                     crop.putExtra(
                       MediaStore.EXTRA_OUTPUT, croppedFile(reqCode)
                     )
                     startActivityForResult(crop, reqCode | (1<<4))
                   case s => toast(
                     "got unexpected ext media storage state %s" format s
                   )
                 }
            }
          case er => toast("unexpected result code %s" format er)
        }
        case c => toast("unexpected response Code %s" format c)
      }

  protected override def onPause() {
    super.onPause()
    sensorManager.unregisterListener(sense)
  }

  protected override def onResume() {
    super.onResume()
    /*sensorManager.registerListener(
       sense,
       sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
       SensorManager.SENSOR_DELAY_UI
    )*/
  }

  private def croppedFile(n: Int) =
    Uri.fromFile(new File(
      Env.getExternalStorageDirectory(), "tickpic_%s.jpg" format n
    ))

  private def view[T <: View](id: Int): T = findViewById(id).asInstanceOf[T]

  private def applyNum(pv: PicView, n: Int) =
    try {
      Images.Media.getBitmap(getContentResolver(), croppedFile(n)) match {
        case null => pv.setImageResource(default(n))
        case bm => pv.setImageBitmap(bm)
      }
    } catch { case _ =>
      pv.setImageResource(default(n))
    }

  private def default(n: Int) =
    n match {
      case 0 => R.drawable.n_0
      case 1 => R.drawable.n_1
      case 2 => R.drawable.n_2
      case 3 => R.drawable.n_3
      case 4 => R.drawable.n_4
      case 5 => R.drawable.n_5
      case 6 => R.drawable.n_6
      case 7 => R.drawable.n_7
      case 8 => R.drawable.n_8
      case 9 => R.drawable.n_9
    }
}
