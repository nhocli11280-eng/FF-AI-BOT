package com.ff.aibot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CaptureOverlayService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "ff_ai_capture"
        private const val NOTIFICATION_ID = 41
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var panel: LinearLayout? = null
    private var status: TextView? = null
    private var markerView: MarkerView? = null
    private lateinit var windowManager: WindowManager
    private var detector: ObjectDetector? = null
    private var worker: HandlerThread? = null
    private var workerHandler: Handler? = null
    private val busy = AtomicBoolean(false)
    private var lastFrameAt = 0L
    private var screenWidth = 1
    private var screenHeight = 1

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        worker = HandlerThread("ff-ai-inference").also { it.start() }
        workerHandler = Handler(worker!!.looper)
        try {
            val model = File(filesDir, "efficientdet_lite0.tflite")
            assets.open("efficientdet_lite0.tflite").use { input -> model.outputStream().use(input::copyTo) }
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(model.absolutePath)
                .build()
            val detectorOptions = ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.35f)
                .setMaxResults(12)
                .build()
            detector = ObjectDetector.createFromOptions(this, detectorOptions)
        } catch (_: Exception) {
            detector = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        startScreenCapture()
        showOverlay()
        return START_NOT_STICKY
    }

    private fun startScreenCapture() {
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels.coerceAtLeast(1)
        screenHeight = metrics.heightPixels.coerceAtLeast(1)
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val now = System.currentTimeMillis()
            if (now - lastFrameAt < 450 || !busy.compareAndSet(false, true)) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastFrameAt = now
            workerHandler?.post {
                try { analyze(image) } catch (_: Exception) {
                    status?.post { status?.text = "Không phân tích được khung hình" }
                } finally { image.close(); busy.set(false) }
            } ?: run { image.close(); busy.set(false) }
        }, null)
        virtualDisplay = projection?.createVirtualDisplay(
            "FF-AI-BOT-Capture", screenWidth, screenHeight, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null
        )
    }

    private fun analyze(image: Image) {
        val activeDetector = detector
        if (activeDetector == null) {
            status?.post { status?.text = "Chưa tải được mô hình AI. Kiểm tra kết nối rồi cài lại ứng dụng." }
            return
        }
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = (rowStride - pixelStride * image.width) / pixelStride
        val padded = Bitmap.createBitmap(image.width + rowPadding, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val bitmap = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        val result: ObjectDetectorResult = activeDetector.detect(BitmapImageBuilder(bitmap).build())
        val boxes = result.detections().mapNotNull { detection ->
            val category = detection.categories().firstOrNull()
            if (category?.categoryName()?.equals("person", true) == true) {
                val r = detection.boundingBox()
                Box(r.left, r.top, r.right, r.bottom, category.score())
            } else null
        }
        bitmap.recycle()
        markerView?.post { markerView?.boxes = boxes; markerView?.invalidate() }
        status?.post {
            status?.text = if (boxes.isEmpty()) "Đang quét màn hình… chưa thấy người rõ ràng"
            else "Phát hiện ${boxes.size} hình người khả nghi (không xác định phe)"
        }
    }

    private fun showOverlay() {
        markerView = MarkerView(this)
        val markerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(markerView, markerParams)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(0xEE202A38.toInt())
        }
        status = TextView(this).apply {
            text = if (detector == null) "FF AI BOT\nKhông tải được mô hình" else "FF AI BOT\nAI đang khởi động…"
            setTextColor(Color.WHITE); textSize = 13f
        }
        card.addView(status)
        card.addView(Button(this).apply { text = "Đóng"; setOnClickListener { stopSelf() } })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 16; y = 120 }
        windowManager.addView(card, params)
        panel = card
    }

    private fun overlayType() = if (Build.VERSION.SDK_INT >= 26)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float, val score: Float)

    private inner class MarkerView(context: Context) : View(context) {
        var boxes: List<Box> = emptyList()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 5f
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED; textSize = 30f; style = Paint.Style.FILL
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val sx = width.toFloat() / screenWidth
            val sy = height.toFloat() / screenHeight
            boxes.forEach { b ->
                canvas.drawRect(RectF(b.left * sx, b.top * sy, b.right * sx, b.bottom * sy), paint)
                canvas.drawText("NGƯỜI ${(b.score * 100).toInt()}%", b.left * sx, (b.top * sy - 8).coerceAtLeast(28f), labelPaint)
            }
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setContentTitle("FF AI BOT đang chạy")
            .setContentText("Đang phân tích hình ảnh màn hình. Nhấn Đóng để dừng.")
            .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "FF AI BOT screen capture", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        markerView?.let { runCatching { windowManager.removeView(it) } }
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        detector?.close()
        worker?.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}