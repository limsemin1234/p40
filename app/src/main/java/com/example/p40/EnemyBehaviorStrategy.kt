package com.example.p40

import android.graphics.Canvas

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
        // 체력 감소 (일반 적 데미지 감소율 적용)
        val actualDamage = (damage * EnemyConfig.NORMAL_ENEMY_DAMAGE_REDUCTION).toInt()
        val health = enemy.getHealth() - actualDamage
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
        // 보스는 일반 적보다 약간 느리게 설정 (GameConfig 값 사용)
        val speed = enemy.getSpeed() * speedMultiplier * EnemyConfig.BOSS_SPEED_MULTIPLIER
        
        // 목표 방향으로 이동 (지그재그 패턴)
        val dx = target.x - position.x
        val dy = target.y - position.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        if (distance > 0) {
            // 지그재그 움직임 구현 (시간에 따른 사인파) (GameConfig 값 사용)
            val time = System.currentTimeMillis() / EnemyConfig.BOSS_ZIGZAG_PERIOD // 지그재그 주기 단위로 변화
            val offsetX = Math.sin(time) * EnemyConfig.BOSS_ZIGZAG_AMPLITUDE // 좌우 진폭
            
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
        // 보스는 데미지 저항이 있음 (데미지 감소율은 GameConfig에서 설정)
        val actualDamage = (damage * EnemyConfig.BOSS_DAMAGE_REDUCTION).toInt()
        val health = enemy.getHealth() - actualDamage
        enemy.setHealth(health)
        
        // 특수 효과: 체력이 설정된 비율 이하로 떨어지면 분노 모드 (빨간색으로 변경)
        if (health <= enemy.getMaxHealth() * EnemyConfig.BOSS_ENRAGE_HEALTH_RATIO) {
            enemy.setEnraged(true)
        }
        
        // 체력이 0 이하이면 사망 처리 추가
        if (health <= 0) {
            enemy.setDead(true)
        }
        
        return health <= 0
    }
    
    override fun onReachCenter(enemy: Enemy) {
        // 보스가 중앙에 도달하면 이벤트만 발생시키고 죽지 않음
        // enemy.setDead(true) 호출 제거
        
        // 이벤트 발생
        GameEventManager.getInstance().dispatchEvent(
            GameEventType.ENEMY_REACHED_CENTER,
            mapOf("enemy" to enemy, "isBoss" to true)
        )
    }
} 