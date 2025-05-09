package com.example.p40.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/**
 * 미사일 클래스
 */
class Missile(
    private var position: PointF,
    private var angle: Double,
    private var speed: Float,
    private var size: Float,
    private var damage: Int,
    private var target: Enemy?
) {
    // 고유 ID 및 생성 시간
    private val id = nextId()
    private val creationTime = System.currentTimeMillis()
    
    // 페인트 객체를 정적으로 공유하여 객체 생성 최소화
    companion object {
        private val MISSILE_PAINT = Paint().apply {
            color = GameConfig.MISSILE_COLOR
            style = Paint.Style.FILL
        }
        
        // 미사일 ID 생성기
        private var idCounter = 0
        private fun nextId(): Int {
            return ++idCounter
        }
    }
    
    private var isDead = false
    private var deathReason: String? = null // 디버깅용 사망 이유
    
    // 충돌 범위 제곱값을 미리 계산 (제곱근 연산 최소화)
    private var collisionRadiusSquared = size * size
    
    /**
     * 객체 풀링을 위한 재설정 메서드
     */
    fun reset(
        newPosition: PointF,
        newAngle: Double,
        newSpeed: Float,
        newSize: Float,
        newDamage: Int,
        newTarget: Enemy?
    ) {
        position = newPosition
        angle = newAngle
        speed = newSpeed
        size = newSize
        damage = newDamage
        target = newTarget
        isDead = false
        deathReason = null
        
        // 충돌 범위 제곱값 재계산
        collisionRadiusSquared = newSize * newSize
    }
    
    /**
     * 미사일 업데이트
     */
    fun update(speedMultiplier: Float = 1.0f) {
        // 이미 죽은 미사일이면 업데이트 하지 않음
        if (isDead) return
        
        // 각도 기반 직선 이동 (타겟과 상관없이 초기 방향으로 계속 이동)
        val adjustedSpeed = speed * speedMultiplier
        position.x += cos(angle).toFloat() * adjustedSpeed
        position.y += sin(angle).toFloat() * adjustedSpeed
        
        // 화면 밖으로 나가는 것은 GameLogic에서 처리
    }
    
    /**
     * 미사일 그리기
     */
    fun draw(canvas: Canvas) {
        if (!isDead) {
            canvas.drawCircle(position.x, position.y, size, MISSILE_PAINT)
        }
    }
    
    /**
     * 적과의 충돌 체크 - 최적화된 버전
     */
    fun checkCollision(enemy: Enemy): Boolean {
        if (isDead || enemy.isDead()) return false
        
        val enemyPos = enemy.getPosition()
        val enemySize = enemy.getSize()
        
        // 거리 계산 최적화 (제곱근 연산 회피)
        val dx = position.x - enemyPos.x
        val dy = position.y - enemyPos.y
        val distanceSquared = dx * dx + dy * dy
        
        // 충돌 범위 계산 (사전 계산된 값이 아닌 실제 충돌 범위 계산)
        val totalRadius = size + enemySize
        val totalRadiusSquared = totalRadius * totalRadius
        
        if (distanceSquared < totalRadiusSquared) {
            enemy.takeDamage(damage)
            isDead = true
            deathReason = "적과 충돌 (ID: ${enemy.hashCode()})"
            return true
        }
        
        return false
    }
    
    /**
     * 미사일이 죽었는지 확인
     */
    fun isDead(): Boolean = isDead
    
    /**
     * 현재 위치 반환
     */
    fun getPosition(): PointF = position
    
    /**
     * 화면 밖으로 나간 미사일 처리
     */
    fun setOutOfBounds() {
        isDead = true
        deathReason = "화면 밖으로 나감"
    }
    
    /**
     * 타겟 정보 반환
     */
    fun getTarget(): Enemy? = target
    
    /**
     * 미사일 ID 반환
     */
    fun getId(): Int = id
    
    /**
     * 디버깅 정보 반환
     */
    override fun toString(): String {
        return "Missile(id=$id, isDead=$isDead, reason=$deathReason, pos=$position, angle=$angle, target=${target?.hashCode()})"
    }
} 