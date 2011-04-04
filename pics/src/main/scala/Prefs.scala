package me.lessis

import android.app.Activity

trait Prefs { self: Activity =>
  import android.content.SharedPreferences
  import android.content.Context

  def edit[T](kind: String)(f: SharedPreferences.Editor => T) = {
    val editor = prefs(kind).edit()
    val res = f(editor)
    editor.commit()
    res
  }

  def prefs(kind: String) = getSharedPreferences(kind, Context.MODE_PRIVATE)
}
