package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DefenseUnit(
    private val position: PointF,
    attackRange: Float,
    private val attackCooldown: Long
) {
    private var lastAttackTime: Long = 0
    
    // 내부적으로 사용할 private 필드로 변경
    private var _attackRange: Float = attackRange
    
    // 타겟팅 최적화를 위한 캐시 설정
    private var attackRangeSquared: Float = _attackRange * _attackRange
    
    // 공격 범위에 대한 getter 프로퍼티
    val attackRange: Float
        get() = _attackRange
    
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
    }
    
    // 가장 가까운 적을 찾아 공격
    fun attack(
        enemies: List<Enemy>, 
        currentTime: Long,
        adjustedCooldown: Long = attackCooldown,
        damageMultiplier: Float = 1.0f,
        angleOffset: Double = 0.0
    ): Missile? {
        if (enemies.isEmpty()) return null
        
        // 쿨다운 체크
        if (currentTime - lastAttackTime < adjustedCooldown) {
            return null
        }
        
        // 가장 가까운 적 찾기
        val target = findNearestEnemy(enemies)
        if (target != null) {
            lastAttackTime = currentTime
            
            val targetPos = target.getPosition()
            val dx = targetPos.x - position.x
            val dy = targetPos.y - position.y
            val angle = atan2(dy.toDouble(), dx.toDouble()) + angleOffset
            
            val missileSpeed = GameConfig.MISSILE_SPEED
            val damage = (GameConfig.MISSILE_DAMAGE * damageMultiplier).toInt()
            val missileSize = GameConfig.MISSILE_SIZE
            
            // 미사일 시작 위치 계산 (객체 재사용)
            val startX = position.x + cos(angle).toFloat() * 20
            val startY = position.y + sin(angle).toFloat() * 20
            
            // 미사일 생성 (미리 계산된 값 사용)
            return Missile(
                position = PointF(startX, startY),
                angle = angle,
                speed = missileSpeed,
                size = missileSize,
                damage = damage,
                target = target
            )
        }
        
        return null
    }
    
    // 가장 가까운 적 찾기 (최적화 버전)
    private fun findNearestEnemy(enemies: List<Enemy>): Enemy? {
        var nearest: Enemy? = null
        var minDistance = Float.MAX_VALUE
        
        // 반복문 한 번으로 제곱근 연산 없이 최적의 타겟 찾기
        for (enemy in enemies) {
            if (enemy.isDead()) continue // 죽은 적은 건너뜀
            
            val enemyPos = enemy.getPosition()
            val dx = enemyPos.x - position.x
            val dy = enemyPos.y - position.y
            
            // 거리 제곱 계산 (제곱근 연산 회피하여 성능 향상)
            val distanceSquared = dx * dx + dy * dy
            
            // attackRange 제곱값을 미리 계산해두어 성능 향상
            if (distanceSquared < attackRangeSquared && distanceSquared < minDistance) {
                minDistance = distanceSquared
                nearest = enemy
            }
        }
        
        return nearest
    }
    
    // 적들까지의 거리 계산 (디버깅용, 실제 게임에서는 호출 최소화)
    fun getEnemyDistances(enemies: List<Enemy>): List<Float> {
        return enemies.map { enemy ->
            val dx = enemy.getPosition().x - position.x
            val dy = enemy.getPosition().y - position.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    
    fun getPosition(): PointF = position
} 