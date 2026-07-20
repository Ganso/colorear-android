package es.colorear.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var gameView: ColoringGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        gameView = ColoringGameView(this) { finish() }
        setContentView(gameView)
        showInitialNotice()
    }

    private fun showInitialNotice() {
        val density = resources.displayMetrics.density
        val corner = 14f * density
        val stroke = 4f * density

        val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xF5001018.toInt())
            setStroke(stroke.toInt(), 0xFF00FFFF.toInt())
            cornerRadius = corner
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            background = backgroundDrawable
        }

        val message = TextView(this).apply {
            text = "COLOREAR es una versión para Android del juego hecho por Jorge y Javi prieto para MS-DOS en el año 1996\n\nInfinitas gracias a Migue McLeod por conservar el código tras años creyéndolo perdido, y por hacer el port a Android.\n\nMás información y código fuente en https://github.com/Ganso/Colorear-Android"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setLinkTextColor(0xFFFFFF00.toInt())
            autoLinkMask = Linkify.WEB_URLS
            movementMethod = LinkMovementMethod.getInstance()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }

        val button = TextView(this).apply {
            text = "ACEPTAR"
            textSize = 22f
            setTextColor(0xFFFFFF00.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }

        container.addView(message)
        container.addView(button)

        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        button.setOnClickListener {
            dialog.dismiss()
            hideSystemUi()
        }

        dialog.show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @Deprecated("Android calls this method on the supported minimum SDK levels")
    override fun onBackPressed() {
        if (!gameView.handleBack()) super.onBackPressed()
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
