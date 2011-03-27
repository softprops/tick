package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{Bundle, Environment, Handler}
import android.widget.{ImageView, TextView, Toast, LinearLayout}
import android.content.{BroadcastReceiver, Context,
                        ContentResolver, Intent, IntentFilter}
import android.graphics.{Typeface}
import android.provider.MediaStore
import MediaStore.Images
import android.view.{Gravity, View, Window, WindowManager}
import android.util.{Log, TypedValue}
import android.graphics.Bitmap
import android.net.Uri
import java.util.Calendar
import java.text.SimpleDateFormat
import java.io.File

class MainActivity extends Activity {

  val Ht = 1
  val Ho = 2
  val Mt = 3
  val Mo = 4
  val CroppedImage = 5

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

  lazy val hTens = findViewById(R.id.h_tens).asInstanceOf[ImageView]
  lazy val hOnes = findViewById(R.id.h_ones).asInstanceOf[ImageView]
  lazy val sep = findViewById(R.id.sep).asInstanceOf[TextView]
  lazy val mTens = findViewById(R.id.m_tens).asInstanceOf[ImageView]
  lazy val mOnes = findViewById(R.id.m_ones).asInstanceOf[ImageView]
  lazy val meridiem: TextView = findViewById(R.id.meridiem).asInstanceOf[TextView]/* new TextView(this) {
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
    setTypeface(Typeface.SERIF, Typeface.ITALIC)
    setBackgroundResource(R.drawable.roundcorner)
  }*/

  def tick() {
    val t = Calendar.getInstance().getTime().getTime
    new SimpleDateFormat("hh").format(t).split("") match {
      case Array(_, tens, ones) =>
        hTens.setImageResource(num(tens))
      hOnes.setImageResource(num(ones))
    }
    new SimpleDateFormat("mm").format(t).split("") match {
      case Array(_, tens, ones) =>
        mTens.setImageResource(num(tens))
        mOnes.setImageResource(num(ones))
    }
    meridiem.setText(new SimpleDateFormat("aa").format(t).toLowerCase)
  }

  def digit =
    new ImageView(this) {
      setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = {
          toast("clicked %s" format v)
          selectPic(Ht)
        }
      })
    }

  def textDigit =
    new TextView(this) {
      setTextSize(TypedValue.COMPLEX_UNIT_DIP, 180f)
      setTypeface(Typeface.SERIF)
    }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.clock)
    (hTens :: hOnes :: mTens :: mOnes :: Nil).view.zipWithIndex foreach {
      case (view, i) => view.setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = selectPic(i + 1)
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

  private def selectPic(which: Int) =
    startActivityForResult(
      new Intent(
        Intent.ACTION_PICK,
        Images.Media.INTERNAL_CONTENT_URI
      ) {
        setType("image/*")
      }, which
    )

  protected val cropFile =
    Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "TEMP_TICK_IMG" + ".jpg"))

  protected override def onActivityResult(
    reqCode: Int, resCode: Int, data: Intent
  ) =
    reqCode match {
      case CroppedImage =>
         toast("wee cropped data %s" format(data.getData) )
         /*val extras = data.getExtras()
         if(extras == null) toast("no extras")
         else {
              val parcelable = extras.getParcelable("data")
              if(parcelable == null) toast("no parsable data returned")
              else {
                 toast("got parsable data %s" format parcelable)
              }
         }*/
         Images.Media.getBitmap(getContentResolver(), cropFile) match {
           case null => toast("failed to retrieve cropFile %s" format cropFile)
           case bm =>
             toast("win %s" format bm)
             hTens.setImageBitmap(bm)
         }
      case Ht | Ho | Mt | Mo =>
        resCode match {
          case Activity.RESULT_OK =>
            val uri = data.getData
            toast("got uri %s" format uri)
            val crop = new Intent("com.android.camera.action.CROP") {
              //setClassName("com.android.camera", "com.android.camera.CropImage")
              setType("image/*")
            }
            getPackageManager().queryIntentActivities(crop,0) match {
               case null => toast("could not find a cropping intent for uri %s :(" format uri)
               case ia =>
                 toast("intent activities %s" format ia)
                 Environment.getExternalStorageState() match {
                   case Environment.MEDIA_MOUNTED =>
                     toast("going to save data in uri %s" format cropFile)
                     crop.setData(uri)
                     crop.putExtra("scale", false)
                     crop.putExtra("outputX", 100)
                     crop.putExtra("outputY", 240)
                     crop.putExtra("aspectX", 1)
                     crop.putExtra("aspectY", 1)
                     crop.putExtra(MediaStore.EXTRA_OUTPUT, cropFile)
                     //crop.putExtra("return-data", true) // write to a tmp file (cropFile), return-data is broke
                     startActivityForResult(crop, CroppedImage)
                   case s => toast("got unexpected ext media storage state %s" format s)
                 }
            }
          case er => toast("unexpected result code %s" format er)
        }
        case c => toast("unexpected response Code %s" format c)
      }

  private def view(id: Int) = findViewById(id)

  private def toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

  private def num(n: String) =
    n match {
      case "0" => R.drawable.n_0
      case "1" => R.drawable.n_1
      case "2" => R.drawable.n_2
      case "3" => R.drawable.n_3
      case "4" => R.drawable.n_4
      case "5" => R.drawable.n_5
      case "6" => R.drawable.n_6
      case "7" => R.drawable.n_7
      case "8" => R.drawable.n_8
      case "9" => R.drawable.n_9
    }
}
