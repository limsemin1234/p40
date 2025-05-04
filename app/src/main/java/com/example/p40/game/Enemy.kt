package com.example.p40.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Enemy(
    private var position: PointF,
    private val target: PointF,
    private val speed: Float = GameConfig.ENEMY_BASE_SIZE,
    private val size: Float = GameConfig.ENEMY_BASE_SIZE,
    var health: Int = GameConfig.ENEMY_BASE_HEALTH,
    val isBoss: Boolean = false
) {
    private val paint = Paint().apply {
        color = if (isBoss) GameConfig.BOSS_COLOR else GameConfig.ENEMY_COLOR
        style = Paint.Style.FILL
    }
    
    private var isDead = false
    
    // 방향 벡터 선언
    private var directionX: Float
    private var directionY: Float
    
    // 이동 방향 계산
    init {
        val angle = atan2(target.y - position.y, target.x - position.x)
        val dirX = cos(angle)
        val dirY = sin(angle)
        
        // 방향 벡터 정규화
        val length = kotlin.math.sqrt(dirX * dirX + dirY * dirY)
        val normalizedDirX = dirX / length
        val normalizedDirY = dirY / length
        
        this.directionX = normalizedDirX
        this.directionY = normalizedDirY
        
        // 보스는 이미 GameView에서 체력이 5배로 설정되어 있으므로 여기서는 중복 적용하지 않음
        // size는 생성자에서 더 큰 값으로 전달
    }
    
    fun update() {
        // 목표 방향으로 이동
        position.x += directionX * speed
        position.y += directionY * speed
    }
    
    fun draw(canvas: Canvas) {
        canvas.drawCircle(position.x, position.y, size, paint)
        
        // 보스는 테두리 추가
        if (isBoss) {
            val bossBorderPaint = Paint().apply {
                color = GameConfig.BOSS_BORDER_COLOR
                style = Paint.Style.STROKE
                strokeWidth = GameConfig.BOSS_BORDER_WIDTH
            }
            canvas.drawCircle(position.x, position.y, size, bossBorderPaint)
            
            // 보스 체력 표시 (디버깅용)
            val healthPaint = Paint().apply {
                color = Color.WHITE
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("$health", position.x, position.y - size - 10, healthPaint)
        }
    }
    
    fun takeDamage(damage: Int) {
        health -= damage
        if (health <= 0) {
            isDead = true
        }
    }
    
    fun isDead(): Boolean = isDead
    
    fun getPosition(): PointF = position
    
    fun getSize(): Float = size
} 