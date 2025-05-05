package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF

/**
 * 적 행동 전략 인터페이스 (전략 패턴)
 */
interface EnemyBehaviorStrategy {
    fun move(enemy: Enemy, speedMultiplier: Float)
    fun draw(enemy: Enemy, canvas: Canvas)
    fun onDamage(enemy: Enemy, damage: Int): Boolean
    fun onReachCenter(enemy: Enemy)
}

/**
 * 기본 적 행동 전략 - 일반적인 적의 행동 패턴
 */
class BasicEnemyBehavior : EnemyBehaviorStrategy {
    override fun move(enemy: Enemy, speedMultiplier: Float) {
        val position = enemy.getPosition()
        val target = enemy.getTarget()
        val speed = enemy.getSpeed() * speedMultiplier
        
        // 목표 방향으로 이동
        val dx = target.x - position.x
        val dy = target.y - position.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        if (distance > 0) {
            position.x += dx / distance * speed
            position.y += dy / distance * speed
        }
    }
    
    override fun draw(enemy: Enemy, canvas: Canvas) {
        val position = enemy.getPosition()
        val size = enemy.getSize()
        val paint = enemy.getPaint()
        
        canvas.drawCircle(position.x, position.y, size, paint)
    }
    
    override fun onDamage(enemy: Enemy, damage: Int): Boolean {
        // 체력 감소
        val health = enemy.getHealth() - damage
        enemy.setHealth(health)
        
        // 죽었는지 확인
        if (health <= 0) {
            enemy.setDead(true)
            return true
        }
        
        return false
    }
    
    override fun onReachCenter(enemy: Enemy) {
        // 기본 적이 중앙에 도달하면 그냥 사라짐
        enemy.setDead(true)
    }
}

/**
 * 보스 적 행동 전략 - 보스만의 특수한 행동 패턴
 */
class BossEnemyBehavior : EnemyBehaviorStrategy {
    override fun move(enemy: Enemy, speedMultiplier: Float) {
        val position = enemy.getPosition()
        val target = enemy.getTarget()
        // 보스는 일반 적보다 약간 느리게 설정
        val speed = enemy.getSpeed() * speedMultiplier * 0.8f
        
        // 목표 방향으로 이동 (지그재그 패턴)
        val dx = target.x - position.x
        val dy = target.y - position.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        if (distance > 0) {
            // 지그재그 움직임 구현 (시간에 따른 사인파)
            val time = System.currentTimeMillis() / 500.0 // 반 초 단위로 변화
            val offsetX = Math.sin(time) * 2.0 // 좌우 진폭
            
            position.x += (dx / distance * speed) + offsetX.toFloat()
            position.y += dy / distance * speed
        }
    }
    
    override fun draw(enemy: Enemy, canvas: Canvas) {
        val position = enemy.getPosition()
        val size = enemy.getSize()
        val paint = enemy.getPaint()
        
        // 보스는 일반 적보다 더 복잡한 그래픽으로 그림
        canvas.drawCircle(position.x, position.y, size, paint)
        
        // 보스 특수 효과 (외곽선)
        val strokePaint = enemy.getStrokePaint()
        canvas.drawCircle(position.x, position.y, size, strokePaint)
    }
    
    override fun onDamage(enemy: Enemy, damage: Int): Boolean {
        // 보스는 데미지 저항이 있음 (데미지 25% 감소)
        val actualDamage = (damage * 0.75f).toInt()
        val health = enemy.getHealth() - actualDamage
        enemy.setHealth(health)
        
        // 특수 효과: 체력이 50% 이하로 떨어지면 분노 모드 (빨간색으로 변경)
        if (health <= enemy.getMaxHealth() / 2) {
            enemy.setEnraged(true)
        }
        
        return health <= 0
    }
    
    override fun onReachCenter(enemy: Enemy) {
        // 보스가 중앙에 도달하면 더 큰 대미지를 입힘
        enemy.setDead(true)
        
        // 이벤트 발생
        GameEventManager.getInstance().dispatchEvent(
            GameEventType.ENEMY_REACHED_CENTER,
            mapOf("enemy" to enemy, "isBoss" to true)
        )
    }
}

/**
 * 공중 적 행동 전략 - 위에서 떠다니는 적
 */
class FlyingEnemyBehavior : EnemyBehaviorStrategy {
    override fun move(enemy: Enemy, speedMultiplier: Float) {
        val position = enemy.getPosition()
        val target = enemy.getTarget()
        // 날아다니는 적은 좀 더 빠르게 설정
        val speed = enemy.getSpeed() * speedMultiplier * 1.2f
        
        // 호버링 효과 (상하로 움직임 추가)
        val time = System.currentTimeMillis() / 300.0 // 0.3초 단위로 변화
        val offsetY = Math.sin(time) * 3.0 // 상하 진폭
        
        val dx = target.x - position.x
        val dy = target.y - position.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        if (distance > 0) {
            position.x += dx / distance * speed
            position.y += (dy / distance * speed) + offsetY.toFloat()
        }
    }
    
    override fun draw(enemy: Enemy, canvas: Canvas) {
        val position = enemy.getPosition()
        val size = enemy.getSize()
        val paint = enemy.getPaint()
        
        // 날아다니는 적은 삼각형으로 그림
        val halfSize = size / 2
        
        // 삼각형 꼭지점 계산
        val xLeft = position.x - size
        val xRight = position.x + size
        val xCenter = position.x
        val yTop = position.y - size
        val yBottom = position.y + size
        
        // 삼각형 그리기
        canvas.drawLine(xLeft, yBottom, xRight, yBottom, paint)
        canvas.drawLine(xRight, yBottom, xCenter, yTop, paint)
        canvas.drawLine(xCenter, yTop, xLeft, yBottom, paint)
    }
    
    override fun onDamage(enemy: Enemy, damage: Int): Boolean {
        // 날아다니는 적은 데미지를 더 많이 받음 (20% 증가)
        val actualDamage = (damage * 1.2f).toInt()
        val health = enemy.getHealth() - actualDamage
        enemy.setHealth(health)
        
        return health <= 0
    }
    
    override fun onReachCenter(enemy: Enemy) {
        // 공중 적이 중앙에 도달
        enemy.setDead(true)
        
        // 이벤트 발생
        GameEventManager.getInstance().dispatchEvent(
            GameEventType.ENEMY_REACHED_CENTER, 
            mapOf("enemy" to enemy, "isFlying" to true)
        )
    }
} 