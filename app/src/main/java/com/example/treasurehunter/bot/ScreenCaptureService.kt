package com.example.treasurehunter.bot

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/**
 * Service chụp màn hình nền (MediaProjection) và quét lưới 5x5 / 7x7
 * xung quanh nhân vật cố định ở tâm màn hình MMORPG.
 */
class ScreenCaptureService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    private var screenWidth = 1080
    private var screenHeight = 2400
    private var isBotActive = false

    // Vị trí nhân vật cố định ở tâm (Redmi Note 11)
    private val playerScreenX = 524f
    private val playerScreenY = 1235f
    private val cellSizePx = 74f // Bước gạch quy đổi trên màn hình 1080x2400

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("DATA", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("DATA")
        }

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            startForeground(1001, createNotification())
            setupVirtualDisplay()
            startScanningLoop()
            isRunning = true
        }

        return START_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "TreasureHunterCapture",
            screenWidth,
            screenHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun startScanningLoop() {
        isBotActive = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isBotActive) return
                scanAndMoveCharacter()
                handler.postDelayed(this, 800) // Chu kỳ quét 800ms
            }
        })
    }

    private fun scanAndMoveCharacter() {
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val adjacentDirections = listOf(
            Direction.UP to Pair(0, -1),
            Direction.DOWN to Pair(0, 1),
            Direction.LEFT to Pair(-1, 0),
            Direction.RIGHT to Pair(1, 0)
        )

        for ((dir, offset) in adjacentDirections) {
            val checkX = (playerScreenX + offset.first * cellSizePx).toInt()
            val checkY = (playerScreenY + offset.second * cellSizePx).toInt()

            if (checkX in 0 until screenWidth && checkY in 0 until screenHeight) {
                val pixel = bitmap.getPixel(checkX, checkY)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val isWhiteStone = (r > 160 && g > 160 && b > 160 && abs(r - g) < 25)
                val isBrownSoil = (r in 100..180 && g in 55..130 && b in 35..100 && r > g + 15)
                val isPatternedGreen = (r in 120..185 && g in 145..205 && b in 90..155 && g > r + 10)

                if (isWhiteStone || isBrownSoil || isPatternedGreen) {
                    Log.i("TreasureBot", "Phát hiện ô mục tiêu ở hướng $dir. Thực hiện kéo cần gạt!")
                    TreasureHunterAccessibilityService.instance?.dragJoystick(dir, 220L)
                    break
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "treasure_bot_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Treasure Hunter Bot", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Treasure Hunter MMORPG Auto-Bot")
            .setContentText("Đang quét khối và kéo cần gạt ảo...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isBotActive = false
        isRunning = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
