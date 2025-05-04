package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF
import kotlin.math.sqrt

class DefenseUnit(
    private val position: PointF,
    private val attackRange: Float = 500f, // 공격범위
    private val attackCooldown: Long = 1000 // 공격속도 1초에 1번
) {
    private var lastAttackTime = 0L
    
    // 가장 가까운 적을 찾아 공격
    fun attack(enemies: List<Enemy>, currentTime: Long): Missile? {
        if (enemies.isEmpty()) return null
        
        // 쿨다운 체크
        if (currentTime - lastAttackTime < attackCooldown) {
            return null
        }
        
        // 가장 가까운 적 찾기
        var closestEnemy: Enemy? = null
        var closestDistance = Float.MAX_VALUE
        
        for (enemy in enemies) {
            if (enemy.isDead()) continue
            
            val enemyPos = enemy.getPosition()
            val dx = enemyPos.x - position.x
            val dy = enemyPos.y - position.y
            val distance = sqrt(dx * dx + dy * dy)
            
            if (distance < attackRange && distance < closestDistance) {
                closestDistance = distance
                closestEnemy = enemy
            }
        }
        
        // 공격 가능한 적이 있으면 미사일 발사
        return if (closestEnemy != null) {
            lastAttackTime = currentTime
            Missile(
                position = PointF(position.x, position.y),
                target = closestEnemy
            )
        } else {
            null
        }
    }
    
    fun getPosition(): PointF = position
} 