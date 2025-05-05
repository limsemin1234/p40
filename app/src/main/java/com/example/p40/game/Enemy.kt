package com.example.p40.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 적 클래스 - 전략 패턴 적용 버전
 */
class Enemy(
    private var position: PointF,
    private var target: PointF,
    private var speed: Float,
    private var size: Float,
    private var health: Int,
    private var isBoss: Boolean = false,
    private var wave: Int = 1
) {
    private var dead = false
    private var maxHealth = health
    private var enraged = false
    
    // 페인트 객체 (색상)
    private val paint = Paint().apply {
        color = if (isBoss) Color.RED else Color.YELLOW
        style = Paint.Style.FILL
    }
    
    // 외곽선용 페인트 객체
    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    // 행동 전략 설정 (전략 패턴)
    private var behaviorStrategy: EnemyBehaviorStrategy = when {
        isBoss -> BossEnemyBehavior()
        wave > 5 && Math.random() < 0.3 -> FlyingEnemyBehavior() // 5웨이브 이후 30% 확률로 날아다니는 적
        else -> BasicEnemyBehavior()
    }
    
    /**
     * 객체 풀링을 위한 재설정 메서드
     */
    fun reset(
        newPosition: PointF,
        newTarget: PointF,
        newSpeed: Float,
        newSize: Float,
        newHealth: Int,
        newIsBoss: Boolean = false,
        newWave: Int = 1
    ) {
        position = newPosition
        target = newTarget
        speed = newSpeed
        size = newSize
        health = newHealth
        maxHealth = newHealth
        isBoss = newIsBoss
        wave = newWave
        dead = false
        enraged = false
        
        // 페인트 색상 재설정
        paint.color = if (isBoss) Color.RED else Color.YELLOW
        
        // 행동 전략 재설정
        behaviorStrategy = when {
            isBoss -> BossEnemyBehavior()
            wave > 5 && Math.random() < 0.3 -> FlyingEnemyBehavior() 
            else -> BasicEnemyBehavior()
        }
    }
    
    /**
     * 적 이동 처리
     */
    fun update(speedMultiplier: Float = 1.0f) {
        if (dead) return
        
        // 전략 패턴: 이동 로직을 전략에 위임
        behaviorStrategy.move(this, speedMultiplier)
        
        // 중앙에 도달했는지 확인 (거리 계산 최적화)
        val dx = position.x - target.x
        val dy = position.y - target.y
        val distanceSquared = dx * dx + dy * dy
        
        // 중앙 도달 거리(제곱)
        val reachDistanceSquared = size * size
        
        if (distanceSquared <= reachDistanceSquared) {
            // 전략 패턴: 중앙 도달 로직을 전략에 위임
            behaviorStrategy.onReachCenter(this)
        }
    }
    
    /**
     * 적 그리기
     */
    fun draw(canvas: Canvas) {
        if (dead) return
        
        // 전략 패턴: 그리기 로직을 전략에 위임
        behaviorStrategy.draw(this, canvas)
    }
    
    /**
     * 데미지 받기
     */
    fun takeDamage(damage: Int): Boolean {
        if (dead) return true
        
        // 전략 패턴: 데미지 처리 로직을 전략에 위임
        return behaviorStrategy.onDamage(this, damage)
    }
    
    /**
     * 적 공격력 반환
     * GameLogic에서 호출되는 메서드로, 적이 중앙에 도달했을 때 입히는 데미지
     */
    fun getDamage(): Int {
        return if (isBoss) {
            // 보스는 더 강한 데미지
            GameConfig.BOSS_DAMAGE * (1 + 0.1f * wave).toInt()
        } else {
            // 일반 적 데미지 (웨이브에 따라 증가)
            GameConfig.NORMAL_ENEMY_DAMAGE * (1 + 0.05f * wave).toInt()
        }
    }
    
    /**
     * 웨이브 설정
     * 새 웨이브가 시작될 때 호출되어 적의 웨이브 번호를 업데이트
     */
    fun setWave(newWave: Int) {
        this.wave = newWave
        
        // 웨이브 변경에 따른 행동 전략 재평가
        behaviorStrategy = when {
            isBoss -> BossEnemyBehavior()
            newWave > 5 && Math.random() < 0.3 -> FlyingEnemyBehavior() 
            else -> BasicEnemyBehavior()
        }
    }
    
    // 접근자 메서드들
    fun isDead(): Boolean = dead
    fun getPosition(): PointF = position
    fun getTarget(): PointF = target
    fun getSize(): Float = size
    fun getSpeed(): Float = speed
    fun getHealth(): Int = health
    fun getMaxHealth(): Int = maxHealth
    fun isBoss(): Boolean = isBoss
    fun getPaint(): Paint = paint
    fun getStrokePaint(): Paint = strokePaint
    
    // 설정자 메서드들
    fun setDead(value: Boolean) { dead = value }
    fun setHealth(value: Int) { health = value }
    fun setEnraged(value: Boolean) {
        enraged = value
        if (enraged) {
            paint.color = Color.MAGENTA // 분노 상태일 때 색상 변경
        }
    }
    
    /**
     * 행동 전략 설정
     */
    fun setBehaviorStrategy(strategy: EnemyBehaviorStrategy) {
        behaviorStrategy = strategy
    }
} 