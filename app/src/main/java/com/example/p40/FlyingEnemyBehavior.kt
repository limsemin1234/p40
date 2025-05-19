package com.example.p40

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Color

/**
 * 공중 적 행동 전략 - 위에서 떠다니는 적
 */
class FlyingEnemyBehavior : EnemyBehaviorStrategy {
    
    // 공중 적 전용 페인트 객체
    private val flyingPaint = Paint().apply {
        color = EnemyConfig.FLYING_ENEMY_COLOR
        style = Paint.Style.FILL
    }
    
    override fun move(enemy: Enemy, speedMultiplier: Float) {
        val position = enemy.getPosition()
        val target = enemy.getTarget()
        val wave = enemy.getWave()
        
        // GameConfig에서 웨이브에 따른 공중적 속도 가져오기
        val baseSpeed = EnemyConfig.getEnemySpeedForWave(wave, false, true)
        val speed = baseSpeed * speedMultiplier
        
        // 호버링 효과 (상하로 움직임 추가) (GameConfig 값 사용)
        val time = System.currentTimeMillis() / EnemyConfig.FLYING_ENEMY_HOVER_PERIOD // 호버링 주기 단위로 변화
        val offsetY = Math.sin(time) * EnemyConfig.FLYING_ENEMY_HOVER_AMPLITUDE // 상하 진폭
        
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
        // 공중 적 크기 설정
        val size = EnemyConfig.FLYING_ENEMY_SIZE
        
        // 날아다니는 적은 삼각형으로 그림
        val halfSize = size / 2
        
        // 삼각형 꼭지점 계산
        val xLeft = position.x - size
        val xRight = position.x + size
        val xCenter = position.x
        val yTop = position.y - size
        val yBottom = position.y + size
        
        // 삼각형 그리기 (공중 적 전용 색상 사용)
        canvas.drawLine(xLeft, yBottom, xRight, yBottom, flyingPaint)
        canvas.drawLine(xRight, yBottom, xCenter, yTop, flyingPaint)
        canvas.drawLine(xCenter, yTop, xLeft, yBottom, flyingPaint)
    }
    
    override fun onDamage(enemy: Enemy, damage: Int): Boolean {
        // 날아다니는 적은 데미지를 더 많이 받음 (GameConfig 값 사용)
        val actualDamage = (damage * EnemyConfig.FLYING_ENEMY_DAMAGE_MULTIPLIER).toInt()
        val health = enemy.getHealth() - actualDamage
        enemy.setHealth(health)
        
        // 체력이 0 이하이면 죽은 것으로 처리
        if (health <= 0) {
            enemy.setDead(true)
            return true
        }
        
        return false
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