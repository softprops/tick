package me.lessis

import android.app.{Activity, NotificationManager}
import android.os.{AsyncTask, Bundle, Environment=> Env, Handler}
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

import org.json.JSONObject
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.protocol.BasicHttpContext

object MainActivity {
  val DigitWidth = 125
  val DigitHeight = 220
  val DigitsPref = "digits"
  val SourcePref = "via"
  val SourceOption = "src"
  val DeviceValue = "device"
  val InstagramValue = "instagram"
  val Words = Map(
    0 -> "zero", 1 -> "one", 2 -> "two", 3 -> "three",
    4 -> "four", 5 -> "five", 6 -> "six", 7 -> "seven",
    8 -> "eight", 9 -> "nine"
  )
  private def wordFor(n: Int) = Words(n)
}

class MainActivity extends Activity
  with Cropping with Toasted with Shaking
  with Prefs with Async with HttpClientProvider {

  import scala.collection.JavaConversions._
  import MainActivity._

  val handler = new Handler()

  val rcvr = new BroadcastReceiver() {
    def onReceive(cxt: Context, intent: Intent) =
      intent.getAction() match {
        case Intent.ACTION_TIME_TICK | Intent.ACTION_TIME_CHANGED |
        Intent.ACTION_TIMEZONE_CHANGED =>
          async {
            tick()
          }
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
    toast("This device does not support ~~shaking~~")

  def afterCrop = tick()

  def cropCanceled = ()

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

    lazy val dialog =
      new JumpDialog(MainActivity.this,
        new OnJumpListener {
          def onJump(c: CharSequence) = select(c.toString.toInt)
        }
      ).numbers.inRowsOf(5)

    (hTens :: hOnes :: mTens :: mOnes :: Nil) foreach {
      _.setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = dialog.show()
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

    tick()
  }

  override def onPause() {
    super.onPause()
    unregisterReceiver(rcvr)
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater().inflate(R.menu.clock_menu, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem) =
    item.getItemId match {
      case R.id.use_device =>
        edit(SourcePref) { _.putString(SourceOption, DeviceValue) }
        true
      case R.id.use_instagram =>
        edit(SourcePref) { _.putString(SourceOption, InstagramValue) }
        prefs("oauth").getString("instagram-token", null) match {
          case null => oauthInstagram
          case _ => ()
        }
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
      case InstagramValue =>
        prefs("oauth").getString("instagram-token", null) match {
          case null => oauthInstagram
          case token =>
            async {
              Log.i("picsee", "fetching urls for %s" format wordFor(which))
              try {
                val url = Instagram.taggedMedia(wordFor(which), token)
                val resp = lenientHttp.execute(new HttpGet(url), new BasicHttpContext())
                Log.i("picsee", "status %s" format resp.getStatusLine().getStatusCode())
                val body = io.Source.fromInputStream(
                  resp.getEntity().getContent()
                ).getLines().mkString("")
                val json = new JSONObject(body)
                val data = json.getJSONArray("data")
                val urls = for(i <- 0 until data.length()) yield {
                  data.getJSONObject(i)
                  .getJSONObject("images")
                  .getJSONObject("low_resolution")
                  .getString("url")
                }
                val intent = new Intent(this, classOf[RemotePicsActivity])
                intent.putExtra("urls", (new java.util.ArrayList[String](urls.size) /: urls){ (a,e) =>
                  a.add(e);a
                })
                intent.putExtra("dig", which)
                startActivity(intent)
              } catch { case e =>
                toast("error %s" format e)
                e.printStackTrace
              }
           }
        }
    }

  private def oauthInstagram =
    startActivity(new Intent(Intent.ACTION_VIEW) {
      setData(Uri.parse(Instagram.authorizeUrl))
    })

  override def onResume() = {
    super.onResume()
    registerReceiver(rcvr, new IntentFilter() {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    })

    getIntent().getData() match {
      case null => ()
        case uri: Uri =>
          prefs(SourcePref).getString(SourceOption, DeviceValue) match {
            case DeviceValue => ()
            case InstagramValue =>
              uri.getFragment() match {
                case null => toast("instagram failed to provide a valid response")
                case fragment =>
                  val AccessParam = """access_token=(.*)""".r
                  fragment match {
                    case AccessParam(token) =>
                      edit("oauth") { _.putString("instagram-token", token) }
                    case _ =>
                      toast("failed to get ig token")
                }
              }
          }
    }
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

  private def uri(path: String) =
    Uri.fromFile(new File(path))

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
