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
    // 페인트 객체를 정적으로 공유하여 객체 생성 최소화
    companion object {
        private val MISSILE_PAINT = Paint().apply {
            color = GameConfig.MISSILE_COLOR
            style = Paint.Style.FILL
        }
    }
    
    private var isDead = false
    
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
        
        // 충돌 범위 제곱값 재계산
        collisionRadiusSquared = newSize * newSize
    }
    
    /**
     * 미사일 업데이트
     */
    fun update(speedMultiplier: Float = 1.0f) {
        // 이미 죽은 미사일이면 업데이트 하지 않음
        if (isDead) return
        
        // 각도 기반 직선 이동
        val adjustedSpeed = speed * speedMultiplier
        position.x += cos(angle).toFloat() * adjustedSpeed
        position.y += sin(angle).toFloat() * adjustedSpeed
        
        // 화면 밖으로 나가면 제거
        if (position.x < -50 || position.x > GameConfig.MISSILE_MAX_DISTANCE || 
            position.y < -50 || position.y > GameConfig.MISSILE_MAX_DISTANCE) {
            isDead = true
        }
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
    }
} 