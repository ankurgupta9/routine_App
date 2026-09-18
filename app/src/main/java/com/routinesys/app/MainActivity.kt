package com.routinesys.app

import android.Manifest
import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private var blocks = mutableListOf<RoutineBlock>()

    private val bg = Color.rgb(15, 19, 25)
    private val surface = Color.rgb(23, 28, 37)
    private val surface2 = Color.rgb(31, 37, 48)
    private val border = Color.rgb(42, 49, 61)
    private val dim = Color.rgb(139, 147, 163)
    private val text = Color.rgb(230, 233, 239)
    private val teal = Color.rgb(53, 208, 186)
    private val red = Color.rgb(239, 91, 91)
    private val amber = Color.rgb(242, 169, 60)

    private val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val categories = linkedMapOf(
        "sleep" to "Sleep",
        "wake" to "Wake routine",
        "study" to "Study",
        "work" to "Work",
        "winddown" to "Wind-down"
    )

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        blocks = RoutineStore.load(this)
        build()
        requestNotifications()
        AlarmScheduler.scheduleAll(this)
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) {
            blocks = RoutineStore.load(this)
            render()
            AlarmScheduler.scheduleAll(this)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(s: String, size: Float, color: Int = text): TextView = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun rounded(color: Int, radius: Float = 12f, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun button(label: String, primary: Boolean = false): Button = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        minHeight = dp(44)
        minWidth = dp(44)
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(if (primary) Color.rgb(8, 14, 15) else dim)
        background = rounded(if (primary) teal else surface2, 8f, if (primary) teal else border)
    }

    private fun build() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(18), dp(16), dp(36))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        })
        render()
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(surface, 12f, border)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }
    }

    private fun render() {
        root.removeAllViews()
        val now = Calendar.getInstance()
        val today = blocks.filter { it.days.contains(now.get(Calendar.DAY_OF_WEEK) - 1) }.sortedBy { mins(it.start) }
        val nowM = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        renderHeader(now)
        renderOverview(today, nowM)
        renderTimeline(today, nowM)
        renderBlockLog(today, nowM)
        renderTracker()
        renderReminders()
        renderData()
    }

    private fun renderHeader(now: Calendar) {
        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(12)) }
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = tv("ROUTINE", 22f, text).apply { setTypeface(null, 1); letterSpacing = .04f }
        val sys = tv(".sys", 22f, dim).apply { setTypeface(null, 1) }
        val brandWrap = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        brandWrap.addView(brand); brandWrap.addView(sys)
        line.addView(brandWrap, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(tv("🔥 ${sleepStreak()}d", 12f, amber).apply {
            background = rounded(Color.rgb(38, 34, 27), 8f, Color.rgb(79, 64, 38))
            setPadding(dp(10), dp(5), dp(10), dp(5))
        })
        top.addView(line)
        top.addView(tv(SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(now.time).uppercase(), 11f, dim))
        root.addView(top)
    }

    private fun renderOverview(today: List<RoutineBlock>, nowM: Int) {
        val p = panel()
        val current = today.firstOrNull { active(it, nowM) }
        val next = today.firstOrNull { mins(it.start) > nowM }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv("TODAY", 11f, dim), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("${today.size} blocks", 11f, dim))
        p.addView(head)
        p.addView(tv(if (current != null) "● IN PROGRESS" else "● ON TRACK", 14f, teal).apply { setTypeface(null, 1) })
        p.addView(tv(if (current != null) current.label else "No routine is active right now", 18f, text).apply { setTypeface(null, 1) })
        p.addView(tv(if (next != null) "NEXT  ${next.label}  ·  ${pretty(next.start)}  ·  ${duration(mins(next.start) - nowM)}" else "NEXT  —", 11f, dim))
        root.addView(p)
    }

    private fun renderTimeline(today: List<RoutineBlock>, nowM: Int) {
        val p = panel()
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv("DAY TIMELINE", 11f, dim), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("12 AM    6 AM    12 PM    6 PM    12 AM", 8f, dim))
        p.addView(head)
        val track = FrameLayout(this).apply { background = rounded(surface2, 7f, border); minimumHeight = dp(44) }
        val width = resources.displayMetrics.widthPixels - dp(32) - dp(32)
        today.forEach { b ->
            val start = mins(b.start)
            val end = if (mins(b.end) <= start) 1440 else mins(b.end)
            val left = dp(2) + ((width - dp(4)) * (start / 1440f)).toInt()
            val w = max(dp(10), ((width - dp(4)) * ((end - start) / 1440f)).toInt())
            val v = TextView(this).apply {
                text = b.label; textSize = 8f; setTextColor(Color.rgb(8, 12, 14)); gravity = Gravity.CENTER_VERTICAL
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(4), 0, dp(4), 0)
                background = rounded(categoryColor(b.category), 5f)
            }
            track.addView(v, FrameLayout.LayoutParams(w, -1).apply { leftMargin = left })
        }
        val nowLine = View(this).apply { setBackgroundColor(Color.WHITE) }
        track.addView(nowLine, FrameLayout.LayoutParams(dp(2), -1).apply { leftMargin = dp(2) + ((width - dp(4)) * (nowM / 1440f)).toInt() })
        p.addView(track)
        p.addView(tv("NOW  ·  ${pretty(String.format(Locale.US, "%02d:%02d", nowM / 60, nowM % 60))}", 9f, text).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) })
        root.addView(p)
    }

    private fun renderBlockLog(today: List<RoutineBlock>, nowM: Int) {
        val p = panel()
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv("BLOCK LOG — TODAY", 11f, dim), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(button("Edit routine").apply { setOnClickListener { editDialog() } })
        p.addView(head)
        if (today.isEmpty()) p.addView(tv("No blocks scheduled for today. Add one via Edit routine.", 13f, dim))
        else today.forEachIndexed { index, b ->
            renderBlockRow(p, b, nowM)
            if (index != today.lastIndex) p.addView(View(this).apply { setBackgroundColor(border) }, LinearLayout.LayoutParams(-1, dp(1)))
        }
        root.addView(p)
    }

    private fun renderBlockRow(parent: LinearLayout, b: RoutineBlock, nowM: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(12)) }
        val main = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(categoryColor(b.category)) } }
        main.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)).apply { setMargins(0, 0, dp(10), 0) })
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(tv(b.label, 14f, text).apply { setTypeface(null, 1) })
        info.addView(tv("${pretty(b.start)} – ${pretty(b.end)}  ·  ${categoryLabel(b.category)}", 10f, dim))
        main.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        val s = status(b, nowM)
        main.addView(tv(s, 9f, statusColor(s)).apply { background = rounded(statusBg(s), 20f, statusColor(s)); setPadding(dp(8), dp(4), dp(8), dp(4)) })
        row.addView(main)
        if (s in listOf("IN PROGRESS", "LOG NOW", "FOLLOWED", "MISSED")) {
            val logged = loggedState(b.id)
            val actions = LinearLayout(this).apply { gravity = Gravity.END; setPadding(dp(19), dp(7), 0, 0) }
            val yes = actionButton("✓  FOLLOWED", logged == true, true)
            val no = actionButton("✕  MISSED", logged == false, false)
            yes.setOnClickListener { logBlock(b, true) }
            no.setOnClickListener { logBlock(b, false) }
            actions.addView(yes, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(6), 0) })
            actions.addView(no, LinearLayout.LayoutParams(0, dp(44), 1f))
            row.addView(actions)
            if (logged != null) row.addView(tv(if (logged) "✓ SAVED · counted toward consistency" else "✕ SAVED AS MISSED", 10f, if (logged) teal else red).apply { setPadding(dp(19), dp(5), 0, 0) })
        }
        parent.addView(row)
    }

    private fun actionButton(label: String, selected: Boolean, positive: Boolean): Button = Button(this).apply {
        text = if (selected) "✓  " + if (positive) "FOLLOWED" else "MISSED" else label
        textSize = 11f; isAllCaps = false; minHeight = dp(44); setPadding(dp(8), 0, dp(8), 0)
        val c = if (positive) teal else red
        setTextColor(if (selected) Color.rgb(8, 14, 15) else c)
        background = rounded(if (selected) c else Color.TRANSPARENT, 8f, c)
        setTypeface(null, if (selected) 1 else 0)
    }

    private fun renderTracker() {
        val p = panel()
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv("CONSISTENCY", 11f, dim), LinearLayout.LayoutParams(0, -2, 1f))
        val (logged, good) = trackerStats()
        head.addView(tv(if (logged == 0) "No logs yet" else "$good/$logged days ≥ 85%", 10f, dim))
        p.addView(head)
        val summary = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(8)) }
        summary.addView(tv("${sleepStreak()}", 26f, amber).apply { setTypeface(null, 1) })
        summary.addView(tv(" day sleep streak", 11f, dim).apply { setPadding(dp(7), dp(7), 0, 0) })
        p.addView(summary)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val end = Calendar.getInstance(); end.set(Calendar.HOUR_OF_DAY, 0); end.set(Calendar.MINUTE, 0); end.set(Calendar.SECOND, 0); end.set(Calendar.MILLISECOND, 0)
        end.add(Calendar.DAY_OF_YEAR, 6 - (end.get(Calendar.DAY_OF_WEEK) - 1))
        val start = end.clone() as Calendar; start.add(Calendar.DAY_OF_YEAR, -69)
        for (i in 0 until 10) {
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), 0, dp(2), 0) }
            for (d in 0 until 7) {
                val c = start.clone() as Calendar; c.add(Calendar.DAY_OF_YEAR, i * 7 + d)
                val ratio = if (c.after(Calendar.getInstance())) null else completionRatio(dateKey(c), c.get(Calendar.DAY_OF_WEEK) - 1)
                val cell = View(this).apply { background = rounded(heatColor(ratio), 3f) }
                col.addView(cell, LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 1, 0, 1) })
            }
            grid.addView(col)
        }
        p.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(grid) })
        p.addView(tv("LESS  ▪ ▪ ▪ ▪ ▪  MORE", 9f, dim).apply { gravity = Gravity.END; setPadding(0, dp(7), 0, 0) })
        root.addView(p)
    }

    private fun renderReminders() {
        val p = panel()
        p.addView(tv("REMINDERS", 11f, dim))
        p.addView(tv("Android schedules routine alerts even when ROUTINE.sys is closed.", 12f, text))
        val active = hasExactAlarm()
        val status = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        status.addView(View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (active) teal else amber) } }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(0, 0, dp(8), 0) })
        status.addView(tv(if (active) "Scheduled and ready" else "Exact alarm permission needed", 11f, if (active) teal else amber))
        p.addView(status)
        p.addView(button(if (active) "Refresh reminders" else "Enable reminders", true).apply {
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                else { requestNotifications(); AlarmScheduler.scheduleAll(this@MainActivity); Toast.makeText(this@MainActivity, "All routine reminders scheduled", Toast.LENGTH_SHORT).show(); render() }
            }
        })
        root.addView(p)
    }

    private fun renderData() {
        val p = panel(); p.addView(tv("DATA", 11f, dim)); p.addView(tv("Your routines and logs stay on this phone.", 12f, dim))
        val row = LinearLayout(this)
        row.addView(button("Export JSON").apply { setOnClickListener { share(RoutineStore.exportJson(this@MainActivity)) } }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, dp(8), dp(5), 0) })
        row.addView(button("Import JSON").apply { setOnClickListener { Toast.makeText(this@MainActivity, "Import picker coming in a future build", Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(5), dp(8), 0, 0) })
        p.addView(row); root.addView(p)
    }

    private fun logBlock(b: RoutineBlock, follow: Boolean) {
        getSharedPreferences("logs", 0).edit().putString("${dateKey()}_${b.id}", if (follow) "followed" else "missed").apply()
        Toast.makeText(this, if (follow) "✓ ${b.label} marked followed" else "✕ ${b.label} marked missed", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun loggedState(id: String): Boolean? = when (getSharedPreferences("logs", 0).getString("${dateKey()}_$id", null)) {
        "followed" -> true; "missed" -> false; else -> null
    }

    private fun completionRatio(key: String, day: Int): Float? {
        val dayBlocks = blocks.filter { it.days.contains(day) }; if (dayBlocks.isEmpty()) return null
        val prefs = getSharedPreferences("logs", 0); var logged = 0; var followed = 0
        dayBlocks.forEach { b -> when (prefs.getString("${key}_${b.id}", null)) { "followed" -> { logged++; followed++ }; "missed" -> logged++ } }
        return if (logged == 0) null else followed.toFloat() / dayBlocks.size.toFloat()
    }

    private fun trackerStats(): Pair<Int, Int> {
        var logged = 0; var good = 0
        val c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        repeat(70) { completionRatio(dateKey(c), c.get(Calendar.DAY_OF_WEEK) - 1)?.let { logged++; if (it >= .85f) good++ }; c.add(Calendar.DAY_OF_YEAR, -1) }
        return logged to good
    }

    private fun sleepStreak(): Int {
        var streak = 0; val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1)
        val sleep = blocks.firstOrNull { it.id == "b1" || it.label.equals("Sleep", true) } ?: return 0
        while (getSharedPreferences("logs", 0).getString("${dateKey(c)}_${sleep.id}", null) == "followed") { streak++; c.add(Calendar.DAY_OF_YEAR, -1) }
        return streak
    }

    private fun heatColor(r: Float?): Int = when { r == null -> surface2; r <= 0f -> Color.rgb(82,39,44); r < .5f -> Color.rgb(82,67,39); r < .85f -> Color.rgb(35,105,93); else -> Color.rgb(35,165,143) }
    private fun categoryColor(cat: String): Int = when (cat) { "sleep" -> Color.rgb(99,102,241); "wake" -> amber; "study" -> teal; "work" -> Color.rgb(91,141,239); "winddown" -> Color.rgb(167,139,250); else -> dim }
    private fun categoryLabel(cat: String) = categories[cat] ?: cat.replaceFirstChar { it.uppercase() }
    private fun statusColor(s: String) = when (s) { "IN PROGRESS", "FOLLOWED" -> teal; "LOG NOW", "MISSED" -> red; else -> dim }
    private fun statusBg(s: String) = when (s) { "IN PROGRESS", "FOLLOWED" -> Color.rgb(23,55,51); "LOG NOW", "MISSED" -> Color.rgb(61,34,38); else -> Color.rgb(38,43,52) }

    private fun status(b: RoutineBlock, n: Int): String {
        val logged = loggedState(b.id); if (logged == true) return "FOLLOWED"; if (logged == false) return "MISSED"
        return when { active(b, n) -> "IN PROGRESS"; n < mins(b.start) -> "UPCOMING"; else -> "LOG NOW" }
    }

    private fun active(b: RoutineBlock, n: Int): Boolean { val s = mins(b.start); val e = mins(b.end); return if (e <= s) n >= s || n < e else n >= s && n < e }
    private fun mins(s: String) = s.substringBefore(":").toInt() * 60 + s.substringAfter(":").toInt()

    private fun pretty(s: String): String {
        val m = mins(s); val h = m / 60
        return String.format(Locale.getDefault(), "%d:%02d %s", if (h % 12 == 0) 12 else h % 12, m % 60, if (h < 12) "AM" else "PM")
    }

    private fun duration(x: Int): String { val m = max(0, x); return if (m < 60) "${m}m" else "${m / 60}h ${m % 60}m" }
    private fun dateKey(c: Calendar = Calendar.getInstance()) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    private fun hasExactAlarm() = Build.VERSION.SDK_INT < 31 || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 0) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
    }

    private fun share(raw: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, raw) }, "Export routine data"))
    }

    private fun editDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        blocks.sortedBy { mins(it.start) }.forEach { b ->
            val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
            r.addView(View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(categoryColor(b.category)) } }, LinearLayout.LayoutParams(dp(9), dp(9)).apply { setMargins(0,0,dp(9),0) })
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(tv(b.label, 12f, text).apply { setTypeface(null,1) })
            info.addView(tv("${pretty(b.start)} – ${pretty(b.end)}  ·  ${categoryLabel(b.category)}  ·  ${daysText(b.days)}", 10f, dim))
            r.addView(info, LinearLayout.LayoutParams(0,-2,1f))
            r.addView(button("Edit").apply { setOnClickListener { editBlockDialog(b) } })
            r.addView(button("Delete").apply { setOnClickListener { deleteBlock(b); editDialog() } })
            list.addView(r)
            list.addView(View(this).apply { setBackgroundColor(border) }, LinearLayout.LayoutParams(-1,dp(1)))
        }
        box.addView(ScrollView(this).apply { addView(list); layoutParams = LinearLayout.LayoutParams(-1, dp(300)) })
        box.addView(button("+ Add routine", true).apply { setOnClickListener { addDialog() } })
        AlertDialog.Builder(this).setTitle("Edit routine").setView(box).setPositiveButton("Done") { _, _ -> render() }.show()
    }

    private fun deleteBlock(b: RoutineBlock) {
        AlarmScheduler.cancel(this, b); blocks.removeAll { it.id == b.id }; RoutineStore.save(this, blocks); AlarmScheduler.scheduleAll(this); Toast.makeText(this, "${b.label} deleted", Toast.LENGTH_SHORT).show()
    }

    private fun daysText(days: Set<Int>) = if (days.size == 7) "Daily" else days.sorted().joinToString(" ") { dayNames[it] }

    private fun editBlockDialog(b: RoutineBlock) {
        showBlockEditor("Edit routine", b) { label, category, start, end, days ->
            b.label = label; b.category = category; b.start = start; b.end = end; b.days = days
            RoutineStore.save(this, blocks); AlarmScheduler.cancel(this, b); AlarmScheduler.scheduleAll(this); Toast.makeText(this, "Routine updated", Toast.LENGTH_SHORT).show(); render()
        }
    }

    private fun addDialog() {
        showBlockEditor("Add routine", null) { label, category, start, end, days ->
            val b = RoutineBlock("b" + System.currentTimeMillis().toString(36), label, category, start, end, days)
            blocks.add(b); RoutineStore.save(this, blocks); AlarmScheduler.scheduleAll(this); Toast.makeText(this, "${b.label} added", Toast.LENGTH_SHORT).show(); render()
        }
    }

    private fun showBlockEditor(title: String, existing: RoutineBlock?, onSave: (String,String,String,String,MutableSet<Int>) -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), 0, dp(22), 0) }
        val label = EditText(this).apply { hint = "Routine name (e.g. Dinner)"; textSize = 16f; setSingleLine(true); setText(existing?.label ?: "") }
        box.addView(tv("NAME", 10f, dim)); box.addView(label)

        box.addView(tv("TYPE", 10f, dim).apply { setPadding(0,dp(12),0,dp(4)) })
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories.values.toList())
        spinner.adapter = adapter
        val selectedIndex = categories.keys.toList().indexOf(existing?.category ?: "study").coerceAtLeast(0)
        spinner.setSelection(selectedIndex)
        box.addView(spinner)

        box.addView(tv("TIME", 10f, dim).apply { setPadding(0,dp(12),0,dp(4)) })
        val times = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val startBtn = button("${existing?.let { pretty(it.start) } ?: "Start time"}")
        val endBtn = button("${existing?.let { pretty(it.end) } ?: "End time"}")
        var startValue = existing?.start ?: "20:00"
        var endValue = existing?.end ?: "20:30"
        startBtn.setOnClickListener { pickTime(startValue) { startValue = it; startBtn.text = pretty(it) } }
        endBtn.setOnClickListener { pickTime(endValue) { endValue = it; endBtn.text = pretty(it) } }
        times.addView(startBtn, LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(0,0,dp(5),0) })
        times.addView(endBtn, LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(5),0,0,0) })
        box.addView(times)

        box.addView(tv("DAYS", 10f, dim).apply { setPadding(0,dp(12),0,dp(4)) })
        val selectedDays = existing?.days?.toMutableSet() ?: (0..6).toMutableSet()
        val dayWrap = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val dayButtons = mutableMapOf<Int, Button>()
        fun refreshDays() { dayButtons.forEach { (i, btn) -> val selected = selectedDays.contains(i); btn.background = rounded(if(selected) teal else surface2, 7f, if(selected) teal else border); btn.setTextColor(if(selected) Color.rgb(8,14,15) else dim); btn.setTypeface(null, if(selected)1 else 0) } }
        dayNames.forEachIndexed { i, name ->
            val btn = Button(this).apply { text = name; textSize = 10f; isAllCaps = false; minHeight = dp(42); minWidth = dp(42); setPadding(0,0,0,0); setOnClickListener { if(selectedDays.contains(i)) selectedDays.remove(i) else selectedDays.add(i); refreshDays() } }
            dayButtons[i] = btn; dayWrap.addView(btn, LinearLayout.LayoutParams(0,dp(44),1f).apply { setMargins(if(i==0)0 else dp(2),0,if(i==6)0 else dp(2),0) })
        }
        refreshDays(); box.addView(dayWrap)
        box.addView(tv("Tap a day to turn it on/off. Time pickers use 12-hour AM/PM.", 10f, dim).apply { setPadding(0,dp(8),0,0) })

        val dialog = AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = label.text.toString().trim()
                if (name.isEmpty()) { label.error = "Enter a routine name"; return@setOnClickListener }
                if (selectedDays.isEmpty()) { Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (startValue == endValue) { Toast.makeText(this, "Start and end time cannot be the same", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                onSave(name, categories.keys.toList()[spinner.selectedItemPosition], startValue, endValue, selectedDays)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun pickTime(value: String, onPicked: (String) -> Unit) {
        val m = mins(value); val picker = TimePickerDialog(this, { _, hour, minute -> onPicked(String.format(Locale.US, "%02d:%02d", hour, minute)) }, m / 60, m % 60, false)
        picker.setTitle("Select time")
        picker.show()
    }
}
