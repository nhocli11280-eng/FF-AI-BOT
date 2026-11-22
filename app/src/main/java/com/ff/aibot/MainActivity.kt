package com.ff.aibot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val requestCapture = 501
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
        }
        val title = TextView(this).apply {
            text = "FF AI BOT"
            textSize = 30f
            gravity = Gravity.CENTER
        }
        val info = TextView(this).apply {
            text = "Trợ lý nổi hỗ trợ xem màn hình. Bản thử nghiệm hiện mới thiết lập chia sẻ màn hình và cửa sổ nổi; chưa có AI nhận diện địch."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        val start = Button(this).apply {
            text = "BẮT ĐẦU CHIA SẺ MÀN HÌNH"
            setOnClickListener { beginCapture() }
        }
        val stop = Button(this).apply {
            text = "DỪNG TRỢ LÝ"
            setOnClickListener { stopService(Intent(this@MainActivity, CaptureOverlayService::class.java)) }
        }
        layout.addView(title)
        layout.addView(info)
        layout.addView(start)
        layout.addView(stop)
        setContentView(layout)
    }

    private fun beginCapture() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }
        startActivityForResult(projectionManager.createScreenCaptureIntent(), requestCapture)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureOverlayService::class.java).apply {
                putExtra(CaptureOverlayService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CaptureOverlayService.EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
        }
    }
}
