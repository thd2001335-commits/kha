package com.example.treasurehunter.bot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * Service Tiếp Cận (AccessibilityService) kéo Cần Gạt Ảo (Virtual Joystick)
 * để di chuyển nhân vật đến các ô mục tiêu trong game Treasure Hunter - MMORPG.
 * Hoạt động mượt mà trên Redmi Note 11 (MIUI / HyperOS, không cần Root).
 */
class TreasureHunterAccessibilityService : AccessibilityService() {

    companion object {
        var instance: TreasureHunterAccessibilityService? = null
            private set
    }

    // Tọa độ tâm Cần Gạt Ảo trên màn hình Redmi Note 11 (1080 x 2400)
    // Tương ứng: X = 50% (540px), Y = 71.5% (1716px)
    private val joystickCenterX = 540f
    private val joystickCenterY = 1716f
    private val dragRadius = 110f // Bán kính kéo cần gạt

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("TreasureBot", "AccessibilityService đã kết nối! Sẵn sàng kéo cần gạt ảo.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w("TreasureBot", "AccessibilityService bị ngắt quãng")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Kéo Cần Gạt Ảo ở góc dưới màn hình theo hướng yêu cầu
     * @param direction Hướng cần đi (UP, DOWN, LEFT, RIGHT)
     * @param durationMs Thời gian giữ cần gạt để nhân vật bước (mặc định 220ms)
     * @param onComplete Callback khi kéo hoàn tất
     */
    fun dragJoystick(
        direction: Direction,
        durationMs: Long = 220L,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        var endX = joystickCenterX
        var endY = joystickCenterY

        when (direction) {
            Direction.UP -> endY = joystickCenterY - dragRadius
            Direction.DOWN -> endY = joystickCenterY + dragRadius
            Direction.LEFT -> endX = joystickCenterX - dragRadius
            Direction.RIGHT -> endX = joystickCenterX + dragRadius
        }

        // Tạo đường vuốt từ tâm cần gạt đến hướng chỉ định
        val swipePath = Path().apply {
            moveTo(joystickCenterX, joystickCenterY)
            val jitterX = (Random.nextFloat() - 0.5f) * 6f
            val jitterY = (Random.nextFloat() - 0.5f) * 6f
            val midX = (joystickCenterX + endX) / 2f + jitterX
            val midY = (joystickCenterY + endY) / 2f + jitterY
            quadTo(midX, midY, endX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(
            swipePath,
            0L,
            durationMs + Random.nextLong(-10L, 20L)
        )

        val gestureBuilder = GestureDescription.Builder().addStroke(stroke)

        // Phát lệnh kéo cử chỉ lên hệ điều hành Android
        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d("TreasureBot", "Đã kéo cần gạt hướng $direction thành công!")
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.e("TreasureBot", "Cử chỉ kéo cần gạt bị hủy")
                    onComplete?.invoke(false)
                }
            },
            null
        )
    }
}
