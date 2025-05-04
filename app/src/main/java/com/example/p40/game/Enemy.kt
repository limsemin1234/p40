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
    val isBoss: Boolean = false,
    private var wave: Int = 1
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
        // 이동 방향 계산
        val dx = target.x - position.x
        val dy = target.y - position.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        directionX = dx / distance
        directionY = dy / distance
        
        // 보스일 경우 체력 보정
        if (isBoss) {
            health = GameConfig.getScaledEnemyHealth(health * 5, wave, isBoss)
        } else {
            health = GameConfig.getScaledEnemyHealth(health, wave, isBoss)
        }
    }
    
    /**
     * 현재 웨이브 설정
     */
    fun setWave(waveNumber: Int) {
        this.wave = waveNumber
        // 웨이브 변경 시 체력 재계산
        if (isBoss) {
            health = GameConfig.getScaledEnemyHealth(GameConfig.ENEMY_BASE_HEALTH * 5, wave, isBoss)
        } else {
            health = GameConfig.getScaledEnemyHealth(GameConfig.ENEMY_BASE_HEALTH, wave, isBoss)
        }
    }

    /**
     * 적 공격력 반환
     */
    fun getDamage(): Int {
        val baseDamage = if (isBoss) GameConfig.BOSS_DAMAGE else GameConfig.NORMAL_ENEMY_DAMAGE
        return GameConfig.getScaledEnemyDamage(baseDamage, wave, isBoss)
    }
    
    // 현재 웨이브 반환
    fun getWave(): Int = wave
    
    // 스피드 배율을 적용할 수 있는 업데이트 메서드
    fun update(speedMultiplier: Float = 1.0f) {
        // 목표 방향으로 이동 (버프로 인한 속도 배율 적용)
        position.x += directionX * speed * speedMultiplier
        position.y += directionY * speed * speedMultiplier
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