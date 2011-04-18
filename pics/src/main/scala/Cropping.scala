package me.lessis

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.{Environment=>Env}
import android.provider.MediaStore
import android.util.Log

import java.io.File

trait Cropping extends Activity with Toasted with Prefs {
  import MainActivity._ // factor out crop specifics

  protected def afterCrop: Unit

  protected def cropCanceled: Unit

  protected def croppedUri(n: Int) =
    Uri.fromFile(croppedFile(n))

  protected def croppedFile(n: Int) =
    new File(
      Env.getExternalStorageDirectory(), "tickpic_%s.jpg" format n
    )

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
            Log.i("picsee", "request made to crop uri %s" format uri)
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
                    toast("crop it")
                    startActivityForResult(crop, reqCode | (1<<4))
                  case state => toast(
                    "got unexpected ext media storage state (%s)" format state
                  )
                }
            }
          case canceled => cropCanceled
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
              afterCrop
            } else toast(
              "failed to retrieve preferred image %s" format croppedUri(dig)
            )
          case canceled => cropCanceled
        }
      case code =>
        toast("unhandled req code %s" format reqCode)
        super.onActivityResult(reqCode, resCode, data)
    }

}
