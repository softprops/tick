package me.lessis

import android.app.Activity
import android.hardware.{Sensor, SensorEvent, SensorEventListener,
                       SensorManager}

trait Shaking extends Activity {

 /** called when a device is shook */
 def onShake: Unit

 /** called when a devices lack of support is detected */
 def onShakeUnsupported: Unit = { }

 lazy val sensorManager = getSystemService("sensor"/*Activity.SENSOR_SERVICE*/).
                              asInstanceOf[SensorManager]

 val sense = new SensorEventListener {
    val prev = Array(0f,0f,0f)
    var before = -1l
    def onAccuracyChanged(sensor: Sensor, accuracy:Int) {  }
    /** uses algoithm described by http://www.clingmarks.com/?p=25
     * could possibly be improved */
    def onSensorChanged(evt: SensorEvent) {
      val now = System.currentTimeMillis
      val diff = now - before
      if(diff > 100) {
        before = now
        val Array(x, y, z) = evt.values
        val Array(px, py, pz) = prev
        val speed = math.abs(x + y + z - px - py - pz) / diff * 10000
        if(speed > 800) onShake
        prev.update(0, x)
        prev.update(1, y)
        prev.update(2, z)
      }
    }
  }

  protected override def onPause() {
    super.onPause()
    sensorManager.unregisterListener(sense)
  }

  protected override def onResume() {
    super.onResume()
    val supported = sensorManager.registerListener(
       sense,
       sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
       SensorManager.SENSOR_DELAY_UI
    )
    if(!supported) onShakeUnsupported
  }
}
