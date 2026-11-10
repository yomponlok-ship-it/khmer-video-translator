package com.khmer.livetranslator

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class FloatingTranslatorService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var bubbleBtn: TextView
    private lateinit var subtitleBox: TextView
    private var tts: TextToSpeech? = null
    private var isTranslating = false
    private val handler = Handler(Looper.getMainLooper())

    private val khmerPhrases = listOf(
        "សូមស្វាគមន៍! កំពុងបកប្រែវីដេអូផ្ទាល់មកជាភាសាខ្មែរ...",
        "ប្រព័ន្ធកំពុងចាប់សំឡេងវីដេអូ និងអានជាសំឡេងភាសាខ្មែរ។",
        "អ្នកអាចអូសប៊ូតុងនេះទៅគ្រប់កន្លែងនៅលើអេក្រង់ទូរស័ព្ទ។"
    )
    private var index = 0

    private val translateLoop = object : Runnable {
        override fun run() {
            if (!isTranslating) return
            val phrase = khmerPhrases[index % khmerPhrases.size]
            subtitleBox.text = phrase
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "KH_TTS")
            index++
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        bubbleBtn = TextView(this).apply {
            text = "បកប្រែ"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2563EB"))
                setStroke(4, Color.WHITE)
            }
        }

        subtitleBox = TextView(this).apply {
            text = "កំពុងចាប់សំឡេងវីដេអូ..."
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(28, 20, 28, 20)
            setBackgroundColor(Color.parseColor("#DD0F172A"))
            visibility = View.GONE
        }

        rootLayout.addView(bubbleBtn)
        rootLayout.addView(subtitleBox)

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            flag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        bubbleBtn.setOnTouchListener(object : View.OnTouchListener {
            var initX = 0
            var initY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = params.x
                        initY = params.y
                        touchX = e.rawX
                        touchY = e.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initX + (e.rawX - touchX).toInt()
                        params.y = initY + (e.rawY - touchY).toInt()
                        windowManager.updateViewLayout(rootLayout, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (Math.abs(e.rawX - touchX) < 15 && Math.abs(e.rawY - touchY) < 15) {
                            toggleTranslation()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(rootLayout, params)
    }

    private fun toggleTranslation() {
        isTranslating = !isTranslating
        val bg = bubbleBtn.background as GradientDrawable
        if (isTranslating) {
            bg.setColor(Color.parseColor("#DC2626"))
            bubbleBtn.text = "ឈប់"
            subtitleBox.visibility = View.VISIBLE
            handler.post(translateLoop)
        } else {
            bg.setColor(Color.parseColor("#2563EB"))
            bubbleBtn.text = "បកប្រែ"
            subtitleBox.visibility = View.GONE
            handler.removeCallbacks(translateLoop)
            tts?.stop()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("km", "KH")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(translateLoop)
        tts?.shutdown()
        if (::rootLayout.isInitialized) windowManager.removeView(rootLayout)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
