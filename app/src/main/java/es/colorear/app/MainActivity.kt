package es.colorear.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
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
        val message = TextView(this).apply {
            text = "COLOREAR es una versión para Android del juego hecho por Jorge y Javi prieto para MS-DOS en el año 1996\n\nMás información y código fuente en https://github.com/Ganso/Colorear-Android"
            setPadding(50, 50, 50, 50)
            textSize = 16f
            autoLinkMask = Linkify.WEB_URLS
        }
        AlertDialog.Builder(this)
            .setTitle("Información")
            .setView(message)
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                hideSystemUi()
            }
            .setCancelable(false)
            .show()
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
