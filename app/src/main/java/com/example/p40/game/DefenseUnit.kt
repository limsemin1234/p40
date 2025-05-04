package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DefenseUnit(
    private val position: PointF,
    var attackRange: Float,
    private val attackCooldown: Long
) {
    private var lastAttackTime: Long = 0
    
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
            
            val dx = target.getPosition().x - position.x
            val dy = target.getPosition().y - position.y
            val angle = atan2(dy.toDouble(), dx.toDouble()) + angleOffset
            
            val missileSpeed = GameConfig.MISSILE_SPEED
            val damage = (GameConfig.MISSILE_DAMAGE * damageMultiplier).toInt()
            val missileSize = GameConfig.MISSILE_SIZE
            
            val startX = position.x + cos(angle).toFloat() * 20
            val startY = position.y + sin(angle).toFloat() * 20
            
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
    
    // 가장 가까운 적 찾기
    private fun findNearestEnemy(enemies: List<Enemy>): Enemy? {
        var nearest: Enemy? = null
        var minDistance = Float.MAX_VALUE
        
        for (enemy in enemies) {
            val dx = enemy.getPosition().x - position.x
            val dy = enemy.getPosition().y - position.y
            val distance = dx * dx + dy * dy
            
            if (distance < attackRange * attackRange && distance < minDistance) {
                minDistance = distance
                nearest = enemy
            }
        }
        
        return nearest
    }
    
    // 적들까지의 거리 계산 (디버깅용)
    fun getEnemyDistances(enemies: List<Enemy>): List<Float> {
        return enemies.map { enemy ->
            val dx = enemy.getPosition().x - position.x
            val dy = enemy.getPosition().y - position.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    
    fun getPosition(): PointF = position
} 