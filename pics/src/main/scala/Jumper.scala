package me.lessis

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.{Gravity, View, ViewGroup}
import android.view.WindowManager.LayoutParams
import android.widget.{TableLayout, TableRow, TextView}

trait OnJumpListener {
  def onJump(c: CharSequence): Unit
}

object JumpDialog {
  private val ALPHA = Array(
	  "A", "B", "C", "D", "E",
    "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
    "T", "U", "V", "W", "X", "Y", "Z", "#"
  )
  private val NUMER = Array(
	  "0", "1", "2", "3", "4",
    "5", "6", "7", "8", "9", "A"
  )
}
class JumpDialog(ctx: Context, listener: OnJumpListener,
                 cols: Int, subset: Seq[String]) extends Dialog(ctx) {
  import JumpDialog._

  def this(ctx: Context, listener: OnJumpListener) =
    this(ctx, listener, 4, List.empty[String])

  val lp = new LayoutParams()
  lp.x = 0
  lp.y = 0
  lp.width = ViewGroup.LayoutParams.FILL_PARENT
  lp.height = ViewGroup.LayoutParams.FILL_PARENT
  lp.flags = lp.flags | LayoutParams.FLAG_FULLSCREEN |
             LayoutParams.FLAG_BLUR_BEHIND

  val grid = new TableLayout(ctx)
  grid.setStretchAllColumns(true)
  grid.setPadding(0, 0, 0, 0)
  grid.setGravity(Gravity.CENTER)

  setContentView(grid, lp)
  getWindow().setBackgroundDrawable(new ColorDrawable(0))

  def inRowsOf(n: Int) = new JumpDialog(ctx, listener, n, subset)

  def only(subset: Seq[String]) = new JumpDialog(ctx, listener, cols, subset)

  def noflip = this // todo disable flipping here

  override final def onAttachedToWindow() {
    withView(NUMER)
    grid.requestLayout()
  }

  def numbers = flip(true)

  def alpha = flip(false)

  private def flip(fromAlpha: Boolean): JumpDialog = withView(
   if(fromAlpha) NUMER else ALPHA
  )

  private def withView(opts: Array[String]) = {
	  grid.removeAllViews()
    var row = new TableRow(getContext())
    opts.zipWithIndex.view.foreach({ case (o, i) =>
      if (i % cols == 0 || i == opts.length - 1) grid.addView(row)
      if (i % cols == 0) row = new TableRow(getContext())

      val c = new TextView(getContext()) {
        setText(o)
        setGravity(Gravity.CENTER)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60)
        setClickable(true)
        setTextColor(
    		  if(subset.isEmpty || subset.contains(o)) Color.WHITE
          else Color.GRAY
        )
      }

      if (i < opts.size - 1) {
        val selectable = subset.isEmpty || subset.contains(o)
        c.setTextColor(if(selectable) Color.WHITE else Color.GRAY)
        if (selectable) {
          c.setOnClickListener(new View.OnClickListener() {
            def onClick(v: View) {
              listener.onJump(c.getText())
              JumpDialog.this.dismiss()
            }
          })
        }
      } else {
        c.setTextColor(Color.WHITE);
        c.setOnClickListener(new View.OnClickListener() {
          def onClick(v: View) {
            flip(ALPHA(ALPHA.size - 1).equals(c.getText()))
          }
        })
      }
      val clp = new ViewGroup.LayoutParams(20, 20)
      clp.width = ViewGroup.LayoutParams.FILL_PARENT
      clp.height = ViewGroup.LayoutParams.FILL_PARENT
      row.addView(c)
    })
    this
  }
}
