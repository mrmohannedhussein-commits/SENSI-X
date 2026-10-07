package com.mohanned.sensix

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : Activity() {
    private val GOLD = 0xFFD4AF37.toInt()
    private val BG = 0xFF0B0B0B.toInt()
    private val CARD = 0xFF16130A.toInt()
    private val LINE = 0xFF4A3C12.toInt()
    private val TEXT = 0xFFF5F0DC.toInt()
    private val MUTED = 0xFFA89A6A.toInt()
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val scopesEn = listOf(
        "No scope (TPP)", "No scope (FPP)", "Red dot / Holo",
        "2x", "3x", "4x", "6x", "8x"
    )
    private val scopesAr = listOf(
        "بدون منظار (TPP)", "بدون منظار (FPP)", "ريد دوت / هولو",
        "2x", "3x", "4x", "6x", "8x"
    )
    private val camBase = listOf(100, 95, 85, 55, 40, 32, 22, 16)
    private val adsBase = listOf(85, 80, 70, 45, 32, 26, 18, 13)
    private val gyroBase = listOf(300, 290, 270, 200, 160, 130, 95, 70)

    private lateinit var root: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("sensix", Context.MODE_PRIVATE) }
    private var ar = true
    private var redraw: () -> Unit = {}
    private var style = 0
    private var fingers = 4
    private var tab = 0
    private var hasGyro = false
    private var inches = 6.5
    private var deviceName = ""
    private var ramGb = 0
    private var wpx = 0
    private var hpx = 0
    private var hz = 60
    private var dpi = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ar = prefs.getBoolean("ar", true)
        root = FrameLayout(this)
        root.setBackgroundColor(BG)
        setContentView(root)
        readDevice()
        showSplash()
    }

    private fun s(a: String, e: String): String = if (ar) a else e

    @Suppress("DEPRECATION")
    private fun readDevice() {
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        deviceName = if (Build.MODEL.startsWith(Build.MANUFACTURER, true)) {
            Build.MODEL
        } else {
            "$brand ${Build.MODEL}"
        }
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hasGyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        ramGb = ceil(mi.totalMem / 1073741824.0).toInt()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        wpx = dm.widthPixels
        hpx = dm.heightPixels
        dpi = resources.displayMetrics.densityDpi
        hz = windowManager.defaultDisplay.refreshRate.roundToInt()
        val w = dm.widthPixels / dm.xdpi
        val h = dm.heightPixels / dm.ydpi
        val d = sqrt((w * w + h * h).toDouble())
        inches = if (d in 4.0..13.0) d else 6.5
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun shape(color: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(radius).toFloat()
        if (stroke != 0) d.setStroke(dp(1), stroke)
        return d
    }

    private fun LinearLayout.put(v: View, top: Int = 0) {
        val p = LinearLayout.LayoutParams(MATCH, WRAP)
        p.topMargin = dp(top)
        addView(v, p)
    }

    private fun weightLp(): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(0, WRAP, 1f)
        p.setMargins(dp(4), 0, dp(4), 0)
        return p
    }

    private fun screen(): LinearLayout {
        handler.removeCallbacksAndMessages(null)
        root.removeAllViews()
        val dir = if (ar) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        root.layoutDirection = dir
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.layoutDirection = dir
        l.setPadding(dp(20), dp(24), dp(20), dp(20))
        root.addView(l, FrameLayout.LayoutParams(MATCH, MATCH))
        langButton(l)
        return l
    }

    private fun langButton(l: LinearLayout) {
        val b = text(s("English", "عربي"), 14f, GOLD, true)
        b.background = shape(Color.TRANSPARENT, 16, GOLD)
        b.setPadding(dp(14), dp(6), dp(14), dp(6))
        b.setOnClickListener {
            ar = !ar
            prefs.edit().putBoolean("ar", ar).apply()
            redraw()
        }
        val p = LinearLayout.LayoutParams(WRAP, WRAP)
        p.gravity = Gravity.END
        l.addView(b, p)
    }

    private fun button(s: String, filled: Boolean, onClick: () -> Unit): TextView {
        val b = text(s, 16f, if (filled) BG else GOLD, true)
        b.gravity = Gravity.CENTER
        b.background = shape(if (filled) GOLD else Color.TRANSPARENT, 28, GOLD)
        b.setPadding(dp(24), dp(14), dp(24), dp(14))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun chip(s: String, selected: Boolean, onClick: () -> Unit): TextView {
        val c = text(s, 15f, if (selected) BG else GOLD, true)
        c.gravity = Gravity.CENTER
        c.background = shape(if (selected) GOLD else Color.TRANSPARENT, 20, GOLD)
        c.setPadding(dp(8), dp(12), dp(8), dp(12))
        c.setOnClickListener { onClick() }
        return c
    }

    private fun row(
        label: String,
        value: String,
        valueColor: Int = TEXT,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(10), 0, dp(10))
        val a = text(label, 15f, MUTED)
        val b = text(value, 15f, valueColor, true)
        b.gravity = Gravity.END
        r.addView(a, LinearLayout.LayoutParams(0, WRAP, 1f))
        r.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
        if (onClick != null) r.setOnClickListener { onClick() }
        return r
    }

    private fun copy(v: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("sensix", v))
        Toast.makeText(this, s("تم النسخ", "Copied"), Toast.LENGTH_SHORT).show()
    }

    private fun mult(): Double {
        var m = if (style == 0) 1.12 else 0.92
        m *= (6.5 / inches).coerceIn(0.9, 1.1)
        m *= when (fingers) {
            2 -> 1.1
            3 -> 1.05
            4 -> 1.0
            5 -> 0.97
            else -> 0.95
        }
        return m
    }

    private fun scaled(base: Int): Int = (base * mult()).roundToInt().coerceIn(1, 300)

    private fun showSplash() {
        redraw = { showSplash() }
        val l = screen()
        l.gravity = Gravity.CENTER_HORIZONTAL
        l.addView(View(this), LinearLayout.LayoutParams(MATCH, 0, 1f))
        val logo = ImageView(this)
        logo.setImageResource(R.drawable.logo_crosshair)
        l.addView(logo, LinearLayout.LayoutParams(dp(180), dp(180)))
        val title = text("SENSI-X", 34f, GOLD, true)
        title.gravity = Gravity.CENTER
        title.letterSpacing = 0.15f
        l.put(title, 16)
        val sub = text(
            s("الحساسية والتحكم المناسبين لجوالك", "Sensitivity & controls for your phone"),
            14f, MUTED
        )
        sub.gravity = Gravity.CENTER
        l.put(sub, 6)
        l.put(button(s("ابدأ", "START"), true) { showScan() }, 36)
        l.addView(View(this), LinearLayout.LayoutParams(MATCH, 0, 1f))
        val dev = text(s("تطوير: MOHANNED", "Developed by MOHANNED"), 13f, MUTED)
        dev.gravity = Gravity.CENTER
        l.put(dev)
        val dis = text(
            s("تطبيق غير رسمي. النتائج مجرد اقتراحات.", "Unofficial app. Results are suggestions."),
            11f, 0xFF7A6D40.toInt()
        )
        dis.gravity = Gravity.CENTER
        l.put(dis, 4)
    }

    private fun showScan() {
        redraw = { showScan() }
        val l = screen()
        l.put(text(s("جاري فحص جهازك...", "Scanning your device..."), 22f, GOLD, true), 8)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = shape(CARD, 14, LINE)
        card.setPadding(dp(16), dp(12), dp(16), dp(12))
        l.put(card, 20)
        val cont = button(s("متابعة", "Continue"), true) { showSetup() }
        cont.visibility = View.GONE
        l.put(cont, 24)
        val rows = listOf(
            s("الجهاز", "Device") to deviceName,
            "Android" to Build.VERSION.RELEASE,
            "RAM" to "$ramGb GB",
            s("الشاشة", "Screen") to "$wpx x $hpx",
            s("معدل التحديث", "Refresh rate") to "$hz Hz",
            s("الكثافة", "Density") to "$dpi DPI",
            s("الجيروسكوب", "Gyroscope") to
                if (hasGyro) s("مدعوم", "Supported") else s("غير متوفر", "Not available")
        )
        rows.forEachIndexed { i, r ->
            handler.postDelayed({
                card.addView(row(r.first, r.second))
                if (i == rows.size - 1) {
                    cont.visibility = View.VISIBLE
                }
            }, 700L * (i + 1))
        }
    }

    private fun showSetup() {
        redraw = { showSetup() }
        val l = screen()
        l.put(text(s("أسلوب لعبك", "Your play style"), 22f, GOLD, true), 8)
        val styleRow = LinearLayout(this)
        styleRow.orientation = LinearLayout.HORIZONTAL
        listOf(s("هجومي", "Aggressive"), s("دفاعي", "Defensive")).forEachIndexed { i, n ->
            styleRow.addView(chip(n, style == i) {
                style = i
                showSetup()
            }, weightLp())
        }
        l.put(styleRow, 14)
        l.put(text(s("عدد الأصابع", "Number of fingers"), 22f, GOLD, true), 28)
        val fRow = LinearLayout(this)
        fRow.orientation = LinearLayout.HORIZONTAL
        listOf(2, 3, 4, 5, 6).forEach { n ->
            fRow.addView(chip("$n", fingers == n) {
                fingers = n
                showSetup()
            }, weightLp())
        }
        l.put(fRow, 14)
        l.put(button(s("اعرض الحساسية", "Show my sensitivity"), true) {
            tab = 0
            showResults()
        }, 36)
    }

    private fun showResults() {
        redraw = { showResults() }
        val l = screen()
        l.put(text(deviceName, 20f, GOLD, true), 8)
        val ids = if (hasGyro) listOf(0, 1, 2, 3) else listOf(0, 1, 3)
        val tabs = LinearLayout(this)
        tabs.orientation = LinearLayout.HORIZONTAL
        ids.forEachIndexed { i, id ->
            val n = when (id) {
                0 -> s("الكاميرا", "Camera")
                1 -> s("التصويب", "ADS")
                2 -> s("الجيروسكوب", "Gyro")
                else -> s("التحكم", "Controls")
            }
            tabs.addView(chip(n, tab == i) {
                tab = i
                showResults()
            }, weightLp())
        }
        l.put(tabs, 14)

        val sv = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        sv.addView(box)
        val lp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        lp.topMargin = dp(10)
        l.addView(sv, lp)

        val kind = ids[tab.coerceIn(0, ids.size - 1)]
        val lines = ArrayList<String>()
        if (kind == 3) {
            val fire = when (fingers) {
                2 -> 110
                3 -> 100
                4 -> 95
                5 -> 90
                else -> 85
            } + (if (style == 0) 5 else 0) + (if (inches < 6.0) 5 else 0)
            val factor = if (style == 0) 0.95 else 1.0
            val sdpi = ((dpi * factor) / 10).roundToInt() * 10
            val styleName = if (style == 0) s("هجومي", "Aggressive") else s("دفاعي", "Defensive")
            val items = listOf(
                s("حجم زر الإطلاق", "Fire button size") to "$fire%",
                s("DPI المقترح (اختياري)", "Suggested DPI (optional)") to "$sdpi",
                s("عدد الأصابع", "Fingers") to "$fingers",
                s("أسلوب اللعب", "Play style") to styleName
            )
            items.forEach { (a, b) ->
                lines.add("$a: $b")
                box.addView(row(a, b, GOLD) { copy(b) })
            }
        } else {
            val base = when (kind) {
                0 -> camBase
                1 -> adsBase
                else -> gyroBase
            }
            val names = if (ar) scopesAr else scopesEn
            names.forEachIndexed { i, n ->
                val v = scaled(base[i])
                lines.add("$n: $v")
                box.addView(row(n, "$v", GOLD) { copy("$v") })
            }
        }
        l.put(button(s("نسخ الكل", "Copy all"), false) { copy(lines.joinToString("\n")) }, 8)
        val note = text(
            s("اقتراحات فقط. عدّل بنفسك في وضع التدريب.", "Suggestions only. Fine-tune in Training mode."),
            11f, MUTED
        )
        note.gravity = Gravity.CENTER
        l.put(note, 8)
        l.put(button(s("رجوع", "Back"), false) { showSetup() }, 8)
    }
}
