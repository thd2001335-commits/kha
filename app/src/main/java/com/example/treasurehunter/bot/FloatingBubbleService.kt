package com.example.treasurehunter.bot

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot

/**
 * Nút Ảo Tròn Nổi (Floating Bubble) đè lên game Treasure Hunter MMORPG:
 * - Hiển thị nút tròn có thể kéo di chuyển tự do trên màn hình
 * - Khi chạm vào nút tròn -> Mở bảng điều khiển Tool có nút "BẮT ĐẦU"
 * - Nút thu nhỏ lại thành nút tròn khi đóng
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var panelView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private var isBotRunning = false
    private var isPanelOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // ==========================================
        // 1. TẠO NÚT TRÒN ẢO NỔI (CIRCULAR BUBBLE)
        // ==========================================
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EE0F172A"))
                setStroke(6, Color.parseColor("#F59E0B"))
            }
            background = shape
        }

        val bubbleText = TextView(this).apply {
            text = "⛏️\nTOOL"
            textSize = 11f
            setTextColor(Color.parseColor("#FBBF24"))
            gravity = Gravity.CENTER
        }
        bubbleLayout.addView(bubbleText)
        bubbleView = bubbleLayout

        bubbleParams = WindowManager.LayoutParams(
            150, // 54dp quy đổi
            150,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 880 // Góc phải màn hình
            y = 450
        }

        var startTouchX = 0f
        var startTouchY = 0f
        var startParamX = 0
        var startParamY = 0

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    startParamX = bubbleParams.x
                    startParamY = bubbleParams.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    bubbleParams.x = startParamX + dx
                    bubbleParams.y = startParamY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dist = hypot(event.rawX - startTouchX, event.rawY - startTouchY)
                    if (dist < 14) {
                        // CHẠM VÀO NÚT TRÒN -> HIỂN THỊ GIAO DIỆN TOOL
                        toggleToolPanel()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, bubbleParams)

        // ==========================================
        // 2. TẠO GIAO DIỆN TOOL CÓ NÚT BẮT ĐẦU
        // ==========================================
        createToolPanel(layoutType)
    }

    private fun createToolPanel(layoutType: Int) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F50F172A"))
                cornerRadius = 36f
                setStroke(4, Color.parseColor("#334155"))
            }
            background = bg
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "⛏️ Treasure Hunter Bot"
            setTextColor(Color.WHITE)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = Button(this).apply {
            text = "✕"
            setTextColor(Color.LTGRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { toggleToolPanel() }
        }
        header.addView(title)
        header.addView(closeBtn)
        panel.addView(header)

        val statusText = TextView(this).apply {
            text = "Trạng thái: Sẵn sàng quét ô"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            setPadding(0, 16, 0, 20)
        }
        panel.addView(statusText)

        // NÚT BẮT ĐẦU AUTO-DRAG CHÍNH
        val startBtn = Button(this).apply {
            text = "BẮT ĐẦU AUTO-DRAG"
            setTextColor(Color.parseColor("#020617"))
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 24f
            }
            background = btnBg
            setPadding(24, 20, 24, 20)

            setOnClickListener {
                isBotRunning = !isBotRunning
                if (isBotRunning) {
                    text = "TẠM DỪNG BOT"
                    btnBg.setColor(Color.parseColor("#EF4444"))
                    setTextColor(Color.WHITE)
                    statusText.text = "Trạng thái: Đang tự động kéo cần gạt..."
                    statusText.setTextColor(Color.parseColor("#34D399"))
                } else {
                    text = "BẮT ĐẦU AUTO-DRAG"
                    btnBg.setColor(Color.parseColor("#10B981"))
                    setTextColor(Color.parseColor("#020617"))
                    statusText.text = "Trạng thái: Đang tạm dừng"
                    statusText.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
        }
        panel.addView(startBtn)

        val stepBtn = Button(this).apply {
            text = "Kéo Cần Gạt 1 Bước"
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 20f
                setStroke(2, Color.parseColor("#475569"))
            }
            background = btnBg
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            layoutParams = params
            setOnClickListener {
                TreasureHunterAccessibilityService.instance?.dragJoystick(Direction.DOWN, 220L)
            }
        }
        panel.addView(stepBtn)

        panelView = panel

        panelParams = WindowManager.LayoutParams(
            780, // Chiều rộng panel
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun toggleToolPanel() {
        if (isPanelOpen) {
            windowManager.removeView(panelView)
            isPanelOpen = false
        } else {
            windowManager.addView(panelView, panelParams)
            isPanelOpen = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        if (isPanelOpen && ::panelView.isInitialized) windowManager.removeView(panelView)
    }
}
