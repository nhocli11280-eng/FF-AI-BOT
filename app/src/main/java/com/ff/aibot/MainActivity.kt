package com.ff.aibot

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private data class Task(val id: Long, val title: String, val source: String, var reward: Long, val note: String, var completed: Boolean = false)
    private val tasks = mutableListOf<Task>()
    private val prefs by lazy { getSharedPreferences("game_worker", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private val sources = listOf(
        Pair("Game mobile có thưởng", "legitimate mobile games reward tasks Vietnam"),
        Pair("Ứng dụng Reward", "legitimate reward apps tasks cash out Vietnam"),
        Pair("Game trên Web", "legitimate browser games reward programs"),
        Pair("Khảo sát trực tuyến", "paid survey platforms Vietnam official rewards")
    )

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); loadTasks(); render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(24)); setBackgroundColor(0xFFF3F6FA.toInt()) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content); root.addView(scroll); setContentView(root)
        addText("AI GAME WORKER", 27f, 0xFF14243A.toInt(), true)
        addText("ANDROID • PHIÊN BẢN 1.0.0", 11f, 0xFF52657A.toInt(), true, 8)
        addText("Tìm cơ hội • Phân tích điều kiện • Theo dõi phần thưởng", 14f, 0xFF34465C.toInt(), false, 12)
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard("NHIỆM VỤ", tasks.size.toString()), LinearLayout.LayoutParams(0, dp(82), 1f))
        stats.addView(statCard("HOÀN THÀNH", tasks.count { it.completed }.toString()), LinearLayout.LayoutParams(0, dp(82), 1f).apply { marginStart = dp(7) })
        stats.addView(statCard("ĐÃ NHẬN", money(tasks.filter { it.completed }.sumOf { it.reward })), LinearLayout.LayoutParams(0, dp(82), 1f).apply { marginStart = dp(7) })
        content.addView(stats)
        addText("KHÁM PHÁ NGUỒN NHIỆM VỤ", 16f, 0xFF14243A.toInt(), true, 20)
        addText("Mở tìm kiếm để xem chương trình hiện có. Tự kiểm tra điều khoản, quốc gia hỗ trợ, ngưỡng rút và đánh giá độc lập trước khi tham gia.", 13f, 0xFF52657A.toInt(), false, 6)
        sources.forEach { pair ->
            content.addView(Button(this).apply { text = "TÌM: " + pair.first; isAllCaps = false; setOnClickListener { openSearch(pair.second) } }, matchWrap().apply { bottomMargin = dp(5) })
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "SỔ NHIỆM VỤ CỦA BẠN"; textSize = 16f; setTextColor(0xFF14243A.toInt()); setTypeface(null, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(Button(this).apply { text = "+ Thêm"; isAllCaps = false; setOnClickListener { showAddTaskDialog() } })
        content.addView(header)
        if (tasks.isEmpty()) addText("Chưa có nhiệm vụ. Tìm nguồn ở trên rồi thêm nhiệm vụ bạn đã xác minh.", 13f, 0xFF64748B.toInt(), false)
        tasks.sortedBy { it.completed }.forEach { task -> content.addView(taskCard(task), matchWrap().apply { bottomMargin = dp(8) }) }
        val note = TextView(this).apply { text = "AN TOÀN: Bản 1.0 mở tìm kiếm web và lưu sổ nhiệm vụ trên điện thoại; chưa tự xác minh đối tác, chưa tự chơi game hoặc rút tiền. Không nhập mật khẩu, OTP, seed phrase; không nạp tiền để mở khóa thưởng."; textSize = 12f; setTextColor(0xFF8A4B16.toInt()); setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xFFFFF1D6.toInt()) }
        content.addView(note, matchWrap().apply { topMargin = dp(14) })
    }

    private fun statCard(label: String, value: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(4), dp(7), dp(4), dp(7)); setBackgroundColor(0xFFFFFFFF.toInt()); addText(label, 9f, 0xFF64748B.toInt(), true); addText(value, 15f, 0xFF176B52.toInt(), true, 4) }
    private fun taskCard(task: Task) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); setBackgroundColor(0xFFFFFFFF.toInt())
        addText(task.title, 15f, 0xFF172A42.toInt(), true)
        addText(task.source + " • " + if (task.completed) "Đã hoàn thành" else "Đang theo dõi", 11f, 0xFF64748B.toInt(), false, 3)
        if (task.note.isNotBlank()) addText(task.note, 12f, 0xFF52657A.toInt(), false, 4)
        addText("Tiền thực nhận: " + money(if (task.completed) task.reward else 0), 12f, 0xFF176B52.toInt(), true, 4)
        val actions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this@MainActivity).apply { text = if (task.completed) "Chưa hoàn thành" else "Đánh dấu hoàn thành"; isAllCaps = false; setOnClickListener { if (task.completed) { task.completed = false; saveTasks(); render() } else confirmComplete(task) } }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(Button(this@MainActivity).apply { text = "Xóa"; isAllCaps = false; setOnClickListener { AlertDialog.Builder(this@MainActivity).setMessage("Xóa nhiệm vụ này?").setNegativeButton("Hủy", null).setPositiveButton("Xóa") { _, _ -> tasks.removeAll { it.id == task.id }; saveTasks(); render() }.show() } })
        addView(actions)
    }

    private fun showAddTaskDialog() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val title = EditText(this).apply { hint = "Tên nhiệm vụ" }; form.addView(title)
        form.addView(TextView(this).apply { text = "Nguồn nhiệm vụ"; textSize = 12f })
        val source = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Game", "Reward", "Web", "Survey")) }; form.addView(source)
        val reward = EditText(this).apply { hint = "Tiền thực nhận (VNĐ), có thể để 0"; inputType = InputType.TYPE_CLASS_NUMBER }; form.addView(reward)
        val note = EditText(this).apply { hint = "Ghi chú / điều kiện rút tiền" }; form.addView(note)
        AlertDialog.Builder(this).setTitle("Thêm nhiệm vụ đã kiểm tra").setView(form).setNegativeButton("Hủy", null).setPositiveButton("Lưu") { _, _ ->
            val name = title.text.toString().trim(); if (name.isNotEmpty()) { tasks.add(Task(System.currentTimeMillis(), name, source.selectedItem.toString(), reward.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 0, note.text.toString().trim())); saveTasks(); render() }
        }.show()
    }

    private fun confirmComplete(task: Task) {
        val amount = EditText(this).apply { hint = "Số tiền thực tế đã nhận (VNĐ)"; inputType = InputType.TYPE_CLASS_NUMBER }
        AlertDialog.Builder(this).setTitle("Xác nhận nhiệm vụ").setMessage("Chỉ xác nhận sau khi nền tảng đã trả thưởng. Nhập số tiền thực nhận; nhập 0 nếu chưa rút được.").setView(amount).setNegativeButton("Hủy", null).setPositiveButton("Xác nhận") { _, _ -> task.reward = amount.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 0; task.completed = true; saveTasks(); render() }.show()
    }

    private fun addText(value: String, size: Float, color: Int, bold: Boolean, top: Int = 0) { content.addView(TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, android.graphics.Typeface.BOLD); if (top > 0) setPadding(0, dp(top), 0, 0) }, matchWrap()) }
    private fun openSearch(query: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))) }.onFailure { AlertDialog.Builder(this).setMessage("Không mở được trình duyệt. Hãy kiểm tra Wi-Fi.").setPositiveButton("OK", null).show() } }
    private fun loadTasks() { tasks.clear(); runCatching { val a = JSONArray(prefs.getString("tasks", "[]")); for (i in 0 until a.length()) { val o = a.getJSONObject(i); tasks.add(Task(o.optLong("id"), o.optString("title"), o.optString("source"), o.optLong("reward"), o.optString("note"), o.optBoolean("completed"))) } } }
    private fun saveTasks() { val a = JSONArray(); tasks.forEach { t -> a.put(JSONObject().apply { put("id", t.id); put("title", t.title); put("source", t.source); put("reward", t.reward); put("note", t.note); put("completed", t.completed) }) }; prefs.edit().putString("tasks", a.toString()).apply() }
    private fun money(value: Long) = String.format(Locale("vi", "VN"), "%,d đ", value)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
}