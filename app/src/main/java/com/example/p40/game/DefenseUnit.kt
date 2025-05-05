package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 디펜스 유닛 클래스 - 벡터 계산 최적화 적용
 */
class DefenseUnit(
    private val position: PointF,
    attackRange: Float,
    private var attackCooldown: Long
) {
    private var lastAttackTime: Long = 0
    
    // 내부적으로 사용할 private 필드로 변경
    private var _attackRange: Float = attackRange
    
    // 타겟팅 최적화를 위한 캐시 설정
    private var attackRangeSquared: Float = _attackRange * _attackRange
    
    // 공격 범위에 대한 getter 프로퍼티
    val attackRange: Float
        get() = _attackRange
    
    // 벡터 계산 재사용을 위한 캐싱
    private val tempDx = mutableMapOf<Enemy, Float>()
    private val tempDy = mutableMapOf<Enemy, Float>()
    private val tempDistanceSquared = mutableMapOf<Enemy, Float>()
    
    // 미사일 위치 계산 최적화를 위한 캐싱
    private var lastCalculatedAngle: Double = 0.0
    private var lastCosValue: Float = 0f
    private var lastSinValue: Float = 0f
    
    // attackRange가 변경될 때 마다 제곱값 업데이트
    init {
        updateAttackRangeSquared()
    }
    
    // attackRange 제곱값 업데이트
    private fun updateAttackRangeSquared() {
        attackRangeSquared = _attackRange * _attackRange
    }
    
    // 공격 범위 설정 시 제곱값도 함께 업데이트
    fun setAttackRange(newRange: Float) {
        _attackRange = newRange
        updateAttackRangeSquared()
        
        // 범위가 변경되면 캐시 초기화
        clearVectorCache()
    }
    
    /**
     * 벡터 계산 캐시 초기화
     */
    private fun clearVectorCache() {
        tempDx.clear()
        tempDy.clear()
        tempDistanceSquared.clear()
    }
    
    // 공격 쿨다운 설정 메서드 추가
    fun setAttackCooldown(newCooldown: Long) {
        attackCooldown = newCooldown
    }
    
    /**
     * 각도 계산 결과 캐싱
     */
    private fun cacheAngleCalculation(angle: Double) {
        if (angle != lastCalculatedAngle) {
            lastCalculatedAngle = angle
            lastCosValue = cos(angle).toFloat()
            lastSinValue = sin(angle).toFloat()
        }
    }
    
    // 가장 가까운 적을 찾아 공격
    fun attack(
        enemies: List<Enemy>, 
        currentTime: Long,
        adjustedCooldown: Long = attackCooldown,
        damageMultiplier: Float = 1.0f,
        angleOffset: Double = 0.0
    ): Missile? {
        // 쿨다운 체크
        if (currentTime - lastAttackTime < adjustedCooldown) {
            return null
        }
        
        lastAttackTime = currentTime
        
        // 가장 가까운 적 찾기
        val target = if (enemies.isNotEmpty()) findNearestEnemy(enemies) else null
        
        // 타겟 좌표 계산 (타겟이 없으면 기본 방향으로)
        val angle = if (target != null) {
            val targetPos = target.getPosition()
            // 벡터 계산 캐싱 활용
            val dx = tempDx[target] ?: (targetPos.x - position.x).also { tempDx[target] = it }
            val dy = tempDy[target] ?: (targetPos.y - position.y).also { tempDy[target] = it }
            atan2(dy.toDouble(), dx.toDouble()) + angleOffset
        } else {
            // 타겟이 없으면 기본 방향 (오른쪽)으로 발사
            0.0 + angleOffset
        }
        
        // 각도 관련 삼각함수 계산 결과 캐싱
        cacheAngleCalculation(angle)
        
        val missileSpeed = GameConfig.MISSILE_SPEED
        val damage = (GameConfig.MISSILE_DAMAGE * damageMultiplier).toInt()
        val missileSize = GameConfig.MISSILE_SIZE
        
        // 미사일 시작 위치 계산 (캐싱된 결과 활용)
        val startX = position.x + lastCosValue * 20
        val startY = position.y + lastSinValue * 20
        
        // 객체 풀에서 미사일 가져오기
        return MissilePool.getInstance().obtain(
            position = PointF(startX, startY),
            angle = angle,
            speed = missileSpeed,
            size = missileSize,
            damage = damage,
            target = target
        )
    }
    
    // 가장 가까운 적 찾기 (최적화 버전)
    private fun findNearestEnemy(enemies: List<Enemy>): Enemy? {
        var nearest: Enemy? = null
        var minDistance = Float.MAX_VALUE
        
        // 죽은 적 제거 및 캐시 정리
        tempDx.entries.removeAll { it.key.isDead() }
        tempDy.entries.removeAll { it.key.isDead() }
        tempDistanceSquared.entries.removeAll { it.key.isDead() }
        
        // 반복문 한 번으로 제곱근 연산 없이 최적의 타겟 찾기
        for (enemy in enemies) {
            if (enemy.isDead()) continue // 죽은 적은 건너뜀
            
            // 이미 계산된 거리가 있으면 재사용
            var distanceSquared = tempDistanceSquared[enemy]
            
            if (distanceSquared == null) {
                val enemyPos = enemy.getPosition()
                val dx = enemyPos.x - position.x
                val dy = enemyPos.y - position.y
                
                // 계산 결과 캐싱
                tempDx[enemy] = dx
                tempDy[enemy] = dy
                
                // 거리 제곱 계산 (제곱근 연산 회피하여 성능 향상)
                distanceSquared = dx * dx + dy * dy
                tempDistanceSquared[enemy] = distanceSquared
            }
            
            // 공격 범위 제한 임시 제거 - 항상 가장 가까운 적을 타겟팅
            if (distanceSquared < minDistance) {
                minDistance = distanceSquared
                nearest = enemy
            }
        }
        
        return nearest
    }
    
    fun getPosition(): PointF = position
    
    /**
     * 적의 위치가 변경될 때 캐시 업데이트
     */
    fun updateEnemyPosition(enemy: Enemy) {
        if (!enemy.isDead()) {
            // 이 적에 대한 캐시된 계산 결과 제거
            tempDx.remove(enemy)
            tempDy.remove(enemy)
            tempDistanceSquared.remove(enemy)
        }
    }
} 