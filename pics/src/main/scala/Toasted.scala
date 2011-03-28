package me.lessis

trait Toasted { self: android.content.Context =>
  import android.widget.Toast

  protected def quickToast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

  protected def toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
