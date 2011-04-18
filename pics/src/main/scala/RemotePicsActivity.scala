package me.lessis

import android.app.{ListActivity,Activity}
import android.content.{Context, Intent}
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.{Bundle, Environment => Env, Handler}
import android.util.Log
import android.widget.{AbsListView, Adapter, AdapterView, BaseAdapter, ImageView}
import android.view.{View, ViewGroup, LayoutInflater}
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.provider.MediaStore

import java.lang.ref.SoftReference
import java.util.HashMap

import java.io.File

abstract class AbstractListAdapter(context: Context) extends BaseAdapter {
  def setScrollStatus(scroll: Boolean): Unit
  def scrollIdle(view: AbsListView): Unit
}

case class ViewHolder(imageView: ImageView)

class PicListAdapter(context: Context, urls: Seq[String])
  extends AbstractListAdapter(context) {

  val loader = new ImageLoader(this, urls)
  val inflater = LayoutInflater.from(context)
  loader.loadImages(0, math.min(10, urls.size))

    def getCount() = urls.size
    def getItem(pos: Int) = pos.asInstanceOf[AnyRef]
    def getItemId(pos: Int) = pos.toLong
    def getView(pos: Int, convertView: View, parent: ViewGroup) = {
      val (cv, holder): (View, ViewHolder) = convertView match {
        case null =>
          val view = inflater.inflate(R.layout.remote_images, null)
          val holder = ViewHolder(
            view.findViewById(R.id.remote_image).asInstanceOf[ImageView]
          )
          view.setTag(holder)
          (view, holder)
        case cv => (cv, cv.getTag().asInstanceOf[ViewHolder])
      }
      holder.imageView.setImageDrawable(loader.getDrawable(pos))
      cv
    }

    def scrollIdle(view: AbsListView) =
      loader.loadImages(view.getFirstVisiblePosition(),view.getLastVisiblePosition())

    def setScrollStatus(scroll: Boolean) = {}
  }

class ImageLoader(adapter: PicListAdapter, urls: Seq[String])
  extends RemoteFiles with Async {

  private val cache = new HashMap[Int, SoftReference[Drawable]]()

  val handler = new Handler()

  def getDrawable(pos: Int) =
    cache.get(pos) match {
      case null =>
        Log.i("picsee", "request for image at pos %s was empty" format pos)
        null
      case ref => ref.get()
    }

  def loadImages(first: Int, last: Int) =
    try {
      Log.i("picsee", "request to load images %s to %s" format(first, last))
      for(i <- first to last) {
        if(!cache.containsKey(i) || cache.get(i).get() == null) {
          async {
            fetch(i) match {
              case null => Log.i("picsee", "could not fetch item at pos %s" format i)
              case drawable =>
                cache.put(i, new SoftReference(drawable))
                adapter.notifyDataSetChanged()
            }
          }
        }
      }
    } catch {
      case e => e.printStackTrace
    }

  private def fetch(pos: Int): Drawable =
    if(urls.isDefinedAt(pos)) remoteBitmap(
      new java.net.URL(urls(pos))
    ) { bm => new BitmapDrawable(bm) }
    else null
}

class RemotePicsActivity extends ListActivity with Cropping with Toasted with IO {
  import scala.collection.JavaConversions._
  override def onCreate(instance: Bundle) {
    super.onCreate(instance)

    val (dig, urls): (Int, Seq[String]) = getIntent().getExtras() match {
      case null => (0, Nil)
      case extras => (extras.getInt("dig"), extras.getStringArrayList("urls").toList)
    }

    val adapter = new PicListAdapter(this, urls)
    setListAdapter(adapter)
    getListView().setOnScrollListener(new AbsListView.OnScrollListener {
      def onScroll(view: AbsListView, firstVis: Int, lastVis: Int, total: Int) {
        adapter.setScrollStatus(true)
      }
      def onScrollStateChanged(view: AbsListView, state: Int) =
        state match {
          case AbsListView.OnScrollListener.SCROLL_STATE_IDLE =>
            adapter.scrollIdle(view)
          case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL |
               AbsListView.OnScrollListener.SCROLL_STATE_FLING =>
            adapter.setScrollStatus(true)
        }
    })

    // consumer image data
    getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
       def onItemClick(par: AdapterView[_], view: View, pos: Int, id: Long): Unit = {
         val bm = view.findViewById(R.id.remote_image).asInstanceOf[ImageView].getDrawable.asInstanceOf[BitmapDrawable].getBitmap
         Env.getExternalStorageState() match {
           case Env.MEDIA_MOUNTED =>
             val uri = picUri(dig)
             using(getContentResolver().openOutputStream(uri)) { out =>
               bm.compress(Bitmap.CompressFormat.PNG, 90, out)
             }
             /*MediaStore.Images.Media.insertImage(
               getContentResolver(),
               file.getAbsolutePath(),
               file.getName(),
               file.getName()
             )*/
             Log.i("picsee", "should crop uri %s" format uri)
             val intent = new Intent() {
               setData(uri)
             }
             RemotePicsActivity.this.onActivityResult(dig, Activity.RESULT_OK, intent)
         }
       }
    })
  }

  def cropCanceled = ()

  def afterCrop = startActivity(new Intent(this, classOf[MainActivity]))

  protected def picUri(n: Int) =
    Uri.fromFile(picFile(n))

  protected def picFile(n: Int) =
    new File(
      Env.getExternalStorageDirectory(), "remotepic_%s.jpg" format n
    )
}

