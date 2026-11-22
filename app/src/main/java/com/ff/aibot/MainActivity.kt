package com.ff.aibot

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var voiceEnabled = true
    private var running = false
    private var startedAt = 0L
    private var elapsedBefore = 0L
    private val maxSessionMs = TimeUnit.HOURS.toMillis(8)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var avatar: TextView
    private lateinit var logBox: LinearLayout
    private lateinit var startButton: Button
    private lateinit var voiceButton: Button
    private lateinit var chatInput: EditText

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = elapsedBefore + (System.currentTimeMillis() - startedAt)
            val remain = (maxSessionMs - elapsed).coerceAtLeast(0)
            timerText.text = "Thời gian: ${clock(elapsed)} / 08:00:00  •  Còn ${clock(remain)}"
            if (remain == 0L) {
                stopSession()
                addMessage("Hệ thống", "Đã đủ 8 giờ. Phiên hỗ trợ đã tự kết thúc.")
                return
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        render()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
                addMessage("Hệ thống", "Máy chưa có giọng tiếng Việt. Hãy cài dữ liệu chuyển văn bản thành giọng nói trong cài đặt Android.")
            } else addMessage("MC AI", "Xin chào mọi người! Chào mừng bạn đến với buổi trò chuyện hôm nay.")
        } else addMessage("Hệ thống", "Không khởi tạo được Text-to-Speech trên thiết bị.")
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            setBackgroundColor(0xFFF3F5FA.toInt())
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        body.addView(TextView(this).apply {
            text = "AI TIKTOK LIVE"
            textSize = 25f
            setTextColor(0xFF18243A.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }, params(-1, dp(42)))
        body.addView(TextView(this).apply {
            text = "AVATAR MC • CHAT • PHIÊN 8 GIỜ • ƯU TIÊN OFFLINE"
            textSize = 10f; setTextColor(0xFF607086.toInt()); gravity = Gravity.CENTER
        }, params(-1, dp(26)))

        val avatarCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        avatar = TextView(this).apply {
            text = "🤖"
            textSize = 76f
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFE9EFFF.toInt())
            setPadding(dp(28), dp(12), dp(28), dp(12))
        }
        avatarCard.addView(avatar, params(-2, dp(132)))
        avatarCard.addView(TextView(this).apply {
            text = "MC ẢO • TRỢ LÝ TRÒ CHUYỆN"
            textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF283B59.toInt()); gravity = Gravity.CENTER
        }, params(-1, dp(32)))
        statusText = TextView(this).apply {
            text = "● Chưa bắt đầu phiên"
            textSize = 12f; setTextColor(0xFF64748B.toInt()); gravity = Gravity.CENTER
        }
        avatarCard.addView(statusText, params(-1, dp(26)))
        body.addView(avatarCard, params(-1, -2))

        timerText = TextView(this).apply {
            text = "Thời gian: 00:00:00 / 08:00:00"
            textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF176B52.toInt()); gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        }
        body.addView(timerText, params(-1, dp(42)))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startButton = Button(this).apply {
            text = "▶ Bắt đầu 8 giờ"; isAllCaps = false
            setOnClickListener { if (running) stopSession() else startSession() }
        }
        voiceButton = Button(this).apply {
            text = "🔊 Giọng: Bật"; isAllCaps = false
            setOnClickListener {
                voiceEnabled = !voiceEnabled
                text = if (voiceEnabled) "🔊 Giọng: Bật" else "🔇 Giọng: Tắt"
                if (!voiceEnabled && ttsReady) tts.stop()
            }
        }
        controls.addView(startButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        controls.addView(voiceButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        body.addView(controls, params(-1, dp(58)))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Mở TikTok"; isAllCaps = false
            setOnClickListener { runCatching { startActivity(packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/live"))) } }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(Button(this).apply {
            text = "Chào phòng LIVE"; isAllCaps = false
            setOnClickListener { say("Xin chào cả nhà! Cảm ơn mọi người đã ghé vào phòng LIVE. Mọi người hôm nay thế nào?") }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        body.addView(actions, params(-1, dp(52)))

        body.addView(TextView(this).apply {
            text = "KỊCH BẢN TRÒ CHUYỆN NHANH"
            textSize = 15f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF18243A.toInt()); gravity = Gravity.START
            setPadding(0, dp(12), 0, dp(6))
        }, params(-1, dp(36)))
        val quick = listOf(
            "Chào mọi người" to "Xin chào mọi người! Rất vui vì bạn đang ở đây. Hãy để lại lời chào nhé!",
            "Hỏi thăm người xem" to "Mọi người đang xem từ tỉnh thành nào vậy? Hãy bình luận cho MC biết nhé!",
            "Gợi ý chủ đề" to "Mọi người muốn trò chuyện về âm nhạc, phim ảnh hay chuyện đời thường hôm nay?",
            "Cảm ơn tương tác" to "Cảm ơn bạn đã bình luận và đồng hành cùng buổi LIVE. Chúc bạn một ngày vui vẻ!",
            "Nhắc theo dõi" to "Nếu thấy nội dung thú vị, bạn có thể theo dõi kênh để gặp lại MC trong những buổi sau nhé!"
        )
        quick.forEach { item ->
            body.addView(Button(this).apply {
                text = item.first; isAllCaps = false
                setOnClickListener { say(item.second) }
            }, params(-1, dp(46)))
        }

        body.addView(TextView(this).apply {
            text = "NHẬP BÌNH LUẬN ĐỂ MC GỢI Ý TRẢ LỜI"
            textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF18243A.toInt()); gravity = Gravity.START
            setPadding(0, dp(12), 0, dp(5))
        }, params(-1, dp(36)))
        chatInput = EditText(this).apply { hint = "Nhập bình luận bạn nhìn thấy trên LIVE…"; minLines = 2; maxLines = 3 }
        body.addView(chatInput, params(-1, -2))
        body.addView(Button(this).apply {
            text = "✨ AI gợi ý và đọc câu trả lời"; isAllCaps = false
            setOnClickListener {
                val q = chatInput.text.toString().trim()
                if (q.isNotEmpty()) {
                    val answer = makeReply(q)
                    addMessage("Người xem (nhập tay)", q)
                    say(answer)
                    chatInput.setText("")
                }
            }
        }, params(-1, dp(50)))

        body.addView(TextView(this).apply {
            text = "NHẬT KÝ MC"
            textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF18243A.toInt()); gravity = Gravity.START
            setPadding(0, dp(12), 0, dp(5))
        }, params(-1, dp(34)))
        logBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        body.addView(logBox, params(-1, -2))
        addMessage("Hệ thống", "MC hoạt động offline bằng kịch bản và giọng đọc Android. Bình luận TikTok cần bạn nhập tay.")
        body.addView(TextView(this).apply {
            text = "GIỚI HẠN: Đây là avatar minh họa và trợ lý MC trên điện thoại, chưa phải nhân vật 3D/AI tạo sinh. Ứng dụng không tự đọc bình luận TikTok, không tự phát LIVE và không giữ máy chạy nền 8 giờ nếu Android đóng ứng dụng. Hãy dùng tính năng LIVE chính thức và tuân thủ điều kiện của TikTok."
            textSize = 11f; setTextColor(0xFF8A4B16.toInt())
            setPadding(dp(10), dp(12), dp(10), dp(12))
            setBackgroundColor(0xFFFFF1D6.toInt())
        }, params(-1, -2))
    }

    private fun startSession() {
        if (elapsedBefore >= maxSessionMs) elapsedBefore = 0
        startedAt = System.currentTimeMillis()
        running = true
        startButton.text = "■ Kết thúc phiên"
        statusText.text = "● Đang hỗ trợ phiên trò chuyện"
        statusText.setTextColor(0xFF16835D.toInt())
        val pulse = AlphaAnimation(0.72f, 1f).apply { duration = 900; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE }
        avatar.startAnimation(pulse)
        addMessage("Hệ thống", "Đã bắt đầu bộ đếm phiên tối đa 8 giờ.")
        handler.post(tick)
        say("MC đã sẵn sàng. Xin chào mọi người!")
    }

    private fun stopSession() {
        if (running) elapsedBefore += System.currentTimeMillis() - startedAt
        running = false
        handler.removeCallbacks(tick)
        avatar.clearAnimation()
        startButton.text = "▶ Tiếp tục phiên"
        statusText.text = "● Đã tạm dừng phiên"
        statusText.setTextColor(0xFF64748B.toInt())
        if (::timerText.isInitialized) timerText.text = "Thời gian: ${clock(elapsedBefore)} / 08:00:00"
        if (ttsReady) tts.stop()
    }

    private fun say(text: String) {
        addMessage("MC AI", text)
        if (voiceEnabled && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "live-${System.currentTimeMillis()}")
    }

    private fun makeReply(message: String): String {
        val q = message.lowercase(Locale("vi", "VN"))
        return when {
            listOf("xin chào", "hello", "chào").any { it in q } -> "Xin chào bạn! Cảm ơn bạn đã ghé phòng LIVE. Chúc bạn có khoảng thời gian thật vui nhé!"
            listOf("bao nhiêu tuổi", "tuổi").any { it in q } -> "MC là nhân vật ảo nên không có tuổi thật. Còn bạn bao nhiêu tuổi rồi?"
            listOf("ở đâu", "đến từ đâu", "ở tỉnh").any { it in q } -> "MC đang đồng hành cùng mọi người qua buổi LIVE trực tuyến. Bạn đang xem từ đâu vậy?"
            listOf("buồn", "mệt", "stress").any { it in q } -> "Mong bạn sẽ thấy vui hơn nhé. Nếu muốn, hãy kể thêm một chút, MC đang lắng nghe đây."
            listOf("hát", "nhạc", "bài hát").any { it in q } -> "Chủ đề âm nhạc thú vị đấy! Bạn thích thể loại nhạc nào: pop, ballad hay remix?"
            listOf("cảm ơn", "thanks").any { it in q } -> "MC cảm ơn bạn nhiều nhé! Chúc bạn luôn vui vẻ và gặp nhiều điều may mắn."
            listOf("follow", "theo dõi").any { it in q } -> "Nếu yêu thích buổi trò chuyện, bạn có thể theo dõi kênh để gặp lại mọi người ở LIVE tiếp theo nhé."
            else -> "Cảm ơn bạn đã chia sẻ! MC đã ghi nhận bình luận. Bạn kể thêm một chút để cả phòng cùng trò chuyện nhé?"
        }
    }

    private fun addMessage(sender: String, message: String) {
        if (!::logBox.isInitialized) return
        logBox.addView(TextView(this).apply {
            text = "$sender: $message"
            textSize = 12f
            setTextColor(if (sender == "MC AI") 0xFF176B52.toInt() else 0xFF334155.toInt())
            setPadding(0, dp(4), 0, dp(4))
        })
        while (logBox.childCount > 24) logBox.removeViewAt(0)
    }

    private fun clock(ms: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }

    private fun params(width: Int, height: Int) = LinearLayout.LayoutParams(width, height).apply { bottomMargin = dp(5) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        stopSession()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
