package com.example.p40

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/**
 * 미사일 클래스
 * 성능 최적화: 공유 페인트 객체 사용, 충돌 감지 최적화, 제곱근 연산 회피
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
        
        // 미사일 크기별 페인트 캐싱 (다양한 크기의 미사일 지원)
        private val sizePaintMap = HashMap<Float, Paint>()
        
        /**
         * 크기에 맞는 페인트 객체 반환 (캐싱)
         */
        fun getPaintForSize(size: Float): Paint {
            return sizePaintMap.getOrPut(size) {
                Paint().apply {
                    color = GameConfig.MISSILE_COLOR
                    style = Paint.Style.FILL
                }
            }
        }
    }
    
    private var isDead = false
    private var deathReason: String? = null // 디버깅용 사망 이유
    
    // 충돌 범위 제곱값을 미리 계산 (제곱근 연산 최소화)
    private var collisionRadiusSquared = size * size
    
    // 충돌 감지용 위치 캐싱
    private var lastPositionX = 0f
    private var lastPositionY = 0f
    private var lastProcessedEnemy: Int = 0 // 마지막 처리한 적의 ID
    
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
        
        // 위치 캐싱 초기화
        lastPositionX = newPosition.x
        lastPositionY = newPosition.y
        lastProcessedEnemy = 0
        
        // 충돌 범위 제곱값 재계산
        collisionRadiusSquared = newSize * newSize
    }
    
    /**
     * 미사일 업데이트
     */
    fun update(speedMultiplier: Float) {
        // 이미 죽은 미사일이면 업데이트 하지 않음
        if (isDead) return
        
        // 각도 기반 직선 이동 (타겟과 상관없이 초기 방향으로 계속 이동)
        val adjustedSpeed = speed * speedMultiplier
        position.x += cos(angle).toFloat() * adjustedSpeed
        position.y += sin(angle).toFloat() * adjustedSpeed
        
        // 위치 캐싱 업데이트
        lastPositionX = position.x
        lastPositionY = position.y
    }
    
    /**
     * 미사일 그리기
     */
    fun draw(canvas: Canvas) {
        if (!isDead) {
            // 크기에 맞는 캐싱된 페인트 객체 사용
            val paint = getPaintForSize(size)
            canvas.drawCircle(position.x, position.y, size, paint)
        }
    }
    
    /**
     * 적과의 충돌 체크 - 최적화된 버전
     */
    fun checkCollision(enemy: Enemy): Boolean {
        if (isDead || enemy.isDead()) return false
        
        // 같은 적과의 중복 충돌 체크 방지
        val enemyId = enemy.hashCode()
        if (enemyId == lastProcessedEnemy) return false
        lastProcessedEnemy = enemyId
        
        val enemyPos = enemy.getPosition()
        val enemySize = enemy.getSize()
        
        // 거리 계산 최적화 (제곱근 연산 회피)
        val dx = lastPositionX - enemyPos.x
        val dy = lastPositionY - enemyPos.y
        val distanceSquared = dx * dx + dy * dy
        
        // 총 충돌 반경 제곱 (미사일 반경 + 적 반경)^2
        val totalRadius = size + enemySize
        val totalRadiusSquared = totalRadius * totalRadius
        
        if (distanceSquared < totalRadiusSquared) {
            enemy.takeDamage(damage)
            isDead = true
            deathReason = "적과 충돌 (ID: $enemyId)"
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
     * 미사일 크기 반환
     */
    fun getSize(): Float = size
    
    /**
     * 화면 밖으로 나간 미사일 처리
     */
    fun setOutOfBounds() {
        isDead = true
        deathReason = "화면 밖으로 나감"
    }
    
    /**
     * 미사일의 고유 ID 반환
     */
    fun getId(): Int = id
    
    /**
     * 디버깅 정보 반환
     */
    override fun toString(): String {
        return "Missile(id=$id, isDead=$isDead, reason=$deathReason, pos=$position, angle=$angle, target=${target?.hashCode()})"
    }
} 