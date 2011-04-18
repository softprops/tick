package me.lessis

trait IO {
  def using[C <: { def close() }, T](c: C)(f: C => T): T =
    try f(c)
    finally c.close()
}
