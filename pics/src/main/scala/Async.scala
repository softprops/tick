package me.lessis

trait Async {
  val handler: android.os.Handler
  def async(f: => Unit) = handler.post(new Runnable {
    def run = f
  })
}
