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
    // 페인트 객체를 정적으로 공유하여 객체 생성 최소화
    companion object {
        private val NORMAL_PAINT = Paint().apply {
            color = GameConfig.ENEMY_COLOR
            style = Paint.Style.FILL
        }
        
        private val BOSS_PAINT = Paint().apply {
            color = GameConfig.BOSS_COLOR
            style = Paint.Style.FILL
        }
        
        private val BOSS_BORDER_PAINT = Paint().apply {
            color = GameConfig.BOSS_BORDER_COLOR
            style = Paint.Style.STROKE
            strokeWidth = GameConfig.BOSS_BORDER_WIDTH
        }
        
        private val HEALTH_PAINT = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
    }
    
    private var isDead = false
    private val paint = if (isBoss) BOSS_PAINT else NORMAL_PAINT
    
    // 방향 벡터 - 계산을 한 번만 하도록 최적화
    private var directionX: Float
    private var directionY: Float
    
    // 캐싱된 데미지 값
    private val damage: Int
    
    // 이동 방향 계산
    init {
        // 이동 방향 계산 - 한 번만 수행
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
        
        // 데미지 미리 계산하여 캐싱
        val baseDamage = if (isBoss) GameConfig.BOSS_DAMAGE else GameConfig.NORMAL_ENEMY_DAMAGE
        damage = GameConfig.getScaledEnemyDamage(baseDamage, wave, isBoss)
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
     * 적 공격력 반환 - 미리 계산된 값 사용
     */
    fun getDamage(): Int = damage
    
    // 현재 웨이브 반환
    fun getWave(): Int = wave
    
    // 스피드 배율을 적용할 수 있는 업데이트 메서드
    fun update(speedMultiplier: Float = 1.0f) {
        // 목표 방향으로 이동 (버프로 인한 속도 배율 적용)
        position.x += directionX * speed * speedMultiplier
        position.y += directionY * speed * speedMultiplier
    }
    
    fun draw(canvas: Canvas) {
        // 화면 밖 객체는 그리지 않음 (최적화)
        val screenWidth = canvas.width
        val screenHeight = canvas.height
        
        if (position.x < -size || position.x > screenWidth + size || 
            position.y < -size || position.y > screenHeight + size) {
            return
        }
        
        canvas.drawCircle(position.x, position.y, size, paint)
        
        // 보스는 테두리 추가
        if (isBoss) {
            canvas.drawCircle(position.x, position.y, size, BOSS_BORDER_PAINT)
            
            // 디버그 빌드나 특정 설정이 활성화된 경우에만 체력 표시
            if (GameConfig.DEBUG_MODE) {
                canvas.drawText("$health", position.x, position.y - size - 10, HEALTH_PAINT)
            }
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