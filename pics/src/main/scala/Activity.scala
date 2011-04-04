package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{Bundle, Environment=> Env, Handler}
import android.widget.{ImageView, TextView, LinearLayout}
import android.content.{BroadcastReceiver, Context,
                        ContentResolver, Intent, IntentFilter}
import android.provider.MediaStore
import android.util.Log
import MediaStore.Images
import android.view.{ContextMenu, Menu, MenuItem, View, Window, WindowManager}
import android.graphics.{Bitmap,BitmapFactory}
import android.net.Uri

import java.util.Calendar
import java.text.SimpleDateFormat
import java.io.File

import java.net.{URL, HttpURLConnection}

object MainActivity {
  val DigitWidth = 125
  val DigitHeight = 220
  val DigitsPref = "digits"
  val SourcePref = "via"
  val SourceOption = "src"
  val DeviceValue = "device"
  val PicplzValue = "picplz"
}

object PicPlz {
  val ClientId = "CbRLCGphEJ3CPANX98SfucGvwNQ7VzR4"
}

trait RemoteFiles {

  def get(from: URL): Bitmap = {
    val con = from.openConnection().asInstanceOf[HttpURLConnection]
    con.setDoInput(true)
    con.connect()
    using(con.getInputStream) { in =>
      BitmapFactory.decodeStream(in)
                             }
  }

  def using[C <: {  def close() }, T](c: C)(f: C => T): T =
    try f(c)
  finally c.close()
}


class MainActivity extends Activity
with Toasted with Shaking with Prefs {
  import MainActivity._

  val mHandler = new Handler()
  val rcvr = new BroadcastReceiver() {
    def onReceive(cxt: Context, intent: Intent) =
      intent.getAction() match {
        case Intent.ACTION_TIME_TICK | Intent.ACTION_TIME_CHANGED |
        Intent.ACTION_TIMEZONE_CHANGED =>
          mHandler.post(new Runnable() {
            def run = tick()
          })
      }
  }

  lazy val hTens = view[PicViewL](R.id.h_tens)
  lazy val hOnes = view[PicViewR](R.id.h_ones)

  lazy val mTens = view[PicViewL](R.id.m_tens)
  lazy val mOnes = view[PicViewR](R.id.m_ones)

  lazy val meridiem = view[TextView](R.id.meridiem)

  def onShake =
    try {
      (0 to 9).foreach({ n =>
        edit(DigitsPref) { _.remove("i_%s" format n) }
                      })
      tick()
    } catch { case e => toast("err %s" format e) }

  override def onShakeUnsupported =
    toast("This device does not support shaking")

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

    val selections = new JumpDialog(MainActivity.this, new OnJumpListener {
      def onJump(c: CharSequence) = select(c.toString.toInt)
    }).numbers.inRowsOf(5)

    (hTens :: hOnes :: mTens :: mOnes :: Nil) foreach {
      _.setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = selections.show()
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

  /** show options for selecting where source images come from */
  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater().inflate(R.menu.clock_menu, menu)
    true
  }

  /** handle option selections */
  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.via => true
    case R.id.use_device =>
      edit(SourcePref) { _.putString(SourceOption, DeviceValue) }
      true
    case R.id.use_picplz =>
      edit(SourcePref) { _.putString(SourceOption, PicplzValue) }
      oauthPicplz
      true
    case _ => super.onOptionsItemSelected(item)
  }

  /** @param which of 0-9 */
  private def select(which: Int) =
    prefs(SourcePref).getString(SourceOption, DeviceValue) match {
      case DeviceValue =>
        startActivityForResult(
          new Intent(
            Intent.ACTION_PICK,
            Images.Media.INTERNAL_CONTENT_URI
          ) {
            setType("image/*")
          }, which
        )
      case PicplzValue =>
        prefs("oauth").getString("picplz-token", null) match {
          case null => oauthPicplz
          case token => toast("not quite there yet with picplz, try via device")
        }
    }

    private def oauthPicplz = {
      val intent = new Intent(Intent.ACTION_VIEW)
      intent.setData(
        Uri.parse(
          "https://picplz.com/oauth2/authenticate?client_id=%s&response_type=code&redirect_uri=picsee://" format PicPlz.ClientId
        )
      )
      startActivity(intent)
    }

  override def onResume() = {
    super.onResume()
    getIntent().getData() match {
      case null => ()
      case uri: Uri =>
        uri.getQueryParameter("code") match {
          case null =>
            uri.getQueryParameter("error") match {
               case null => ()
               case err => toast("oauth error %s" format err)
            }
          case code =>
            edit("oauth") { _.putString("picplz-token", code) }
       }
    }
  }

  /** @param reqCode 0-9 indicates an image was selected, a masked 4th bit
   *                 indicates a cropping result of 0-9's image */
  protected override def onActivityResult(
    reqCode: Int, resCode: Int, data: Intent
  ) =
    reqCode match {
      // selected
      case 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 =>
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
                       MediaStore.EXTRA_OUTPUT, croppedUri(reqCode)
                     )
                     startActivityForResult(crop, reqCode | (1<<4))
                   case state => toast(
                     "got unexpected ext media storage state (%s)" format state
                   )
                 }
            }
            case canceled => ()
        }
      // cropped
      case n if((n & (1<<4)) > 0) =>
         resCode match {
           case Activity.RESULT_OK =>
             val dig = n & ~(1<<4)
             if(croppedFile(dig).exists) {
               edit(DigitsPref) {
                 _.putString(
                   "i_%s" format dig,
                   croppedFile(dig).getAbsolutePath
                 )
               }
               tick()
             } else toast(
               "failed to retrieve preferred image %s" format croppedUri(dig)
             )
           case canceled => ()
         }
      case code => toast("unexpected request Code %s" format code)
    }

  private def preferred(n: Int) =
    prefs(DigitsPref).getString("i_%s" format n, null) match {
      case null => None
      case url => stored(url)
    }

  private def stored(path: String) =
   Images.Media.getBitmap(getContentResolver(), uri(path)) match {
     case null => None
     case bm => Some(bm)
   }

  private def croppedUri(n: Int) =
    Uri.fromFile(croppedFile(n))

  private def uri(path: String) =
    Uri.fromFile(new File(path))

  private def croppedFile(n: Int) =
    new File(
       Env.getExternalStorageDirectory(), "tickpic_%s.jpg" format n
    )

  private def view[T <: View](id: Int): T = findViewById(id).asInstanceOf[T]

  private def applyNum(pv: PicView, n: Int) =
    try {
      preferred(n) match {
        case None =>
          pv.setImageResource(default(n))
        case Some(bm) =>
          pv.setImageBitmap(bm)
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
