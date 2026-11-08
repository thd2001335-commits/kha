package com.example.treasurehunter.bot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Màn hình cấp quyền cho Redmi Note 11:
 * 1. Quyền hiển thị trên ứng dụng khác (Nút tròn nổi)
 * 2. Quyền Trợ năng (Accessibility - kéo cần gạt)
 * 3. Quyền Chụp màn hình (Screen capture)
 */
class MainActivity : AppCompatActivity() {

    private val REQUEST_SCREEN_CAPTURE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
            setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "⛏️ Treasure Hunter MMORPG Auto-Bot"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(title)

        val desc = TextView(this).apply {
            text = "Tối ưu cho Redmi Note 11 (MIUI / HyperOS)\nVui lòng cấp 3 quyền dưới đây để kích hoạt nút ảo tròn trên màn hình game:"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setPadding(0, 16, 0, 32)
        }
        layout.addView(desc)

        // 1. Quyền Nút Nổi
        val btnOverlay = Button(this).apply {
            text = "1. Cấp Quyền Nút Nổi (Overlay)"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Đã có quyền Nút Nổi!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(btnOverlay)

        // 2. Quyền Trợ năng
        val btnAccess = Button(this).apply {
            text = "2. Cấp Quyền Trợ Năng (Accessibility)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(btnAccess)

        // 3. Khởi chạy nút tròn và bắt đầu bot
        val btnStart = Button(this).apply {
            text = "3. BẬT NÚT TRÒN NỔI & MỞ GAME"
            setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
            setTextColor(android.graphics.Color.BLACK)
            setOnClickListener {
                // Khởi động dịch vụ nút tròn nổi
                startService(Intent(this@MainActivity, FloatingBubbleService::class.java))
                // Yêu cầu quyền quay màn hình
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
            }
        }
        layout.addView(btnStart)

        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Đã kích hoạt Nút Tròn Nổi trên màn hình!", Toast.LENGTH_LONG).show()
            moveTaskToBack(true) // Thu nhỏ ra màn hình chính để vào game
        }
    }
}
