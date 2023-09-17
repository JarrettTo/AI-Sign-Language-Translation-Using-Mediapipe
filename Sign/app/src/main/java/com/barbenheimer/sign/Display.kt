package com.barbenheimer.sign

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class Display(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    var srcRect = Rect(0, 0, 480, 640)
    var disRect: Rect? = null
    var b: Bitmap? = null
    fun getBitmap(bitmap: Bitmap?) {
        b = bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        invalidate()
        disRect = Rect(0, 0, right, bottom)
        if (b != null) {
            canvas.drawBitmap(b!!, srcRect, disRect!!, null)
        }
    }
}