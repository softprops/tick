package me.lessis

import java.net.{HttpURLConnection, URL}
import android.graphics.{Bitmap, BitmapFactory}

trait RemoteFiles extends IO {
  def remoteBitmap[T](from: URL)(f: Bitmap => T) = {
    val con = from.openConnection().asInstanceOf[HttpURLConnection]
    con.setDoInput(true)
    con.connect()
    using(con.getInputStream) { in =>
      f(BitmapFactory.decodeStream(in))
    }
  }
}
