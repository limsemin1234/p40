package com.example.p40

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF

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
    // 고유 ID 생성
    private val id = nextId()
    
    private var dead = false
    private var maxHealth = health
    private var enraged = false
    private var deathReason: String? = null // 디버깅용 사망 이유
    
    // 페인트 객체 (색상)
    private val paint = Paint().apply {
        color = if (isBoss) GameConfig.BOSS_COLOR else GameConfig.ENEMY_COLOR
        style = Paint.Style.FILL
    }
    
    // 외곽선용 페인트 객체
    private val strokePaint = Paint().apply {
        color = if (isBoss) GameConfig.BOSS_BORDER_COLOR else Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = if (isBoss) GameConfig.BOSS_BORDER_WIDTH else 2f
    }
    
    // 행동 전략 설정 (전략 패턴)
    private var behaviorStrategy: EnemyBehaviorStrategy = when {
        isBoss -> BossEnemyBehavior()
        wave >= GameConfig.FLYING_ENEMY_WAVE_THRESHOLD && Math.random() < GameConfig.FLYING_ENEMY_SPAWN_CHANCE -> FlyingEnemyBehavior() // GameConfig에서 설정한 웨이브와 확률로 공중 적 생성
        else -> BasicEnemyBehavior()
    }
    
    companion object {
        // 적 ID 생성기
        private var idCounter = 0
        private fun nextId(): Int {
            return ++idCounter
        }
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
        isBoss = newIsBoss
        wave = newWave
        dead = false
        enraged = false
        deathReason = null
        
        // 행동 전략 재설정
        behaviorStrategy = when {
            isBoss -> BossEnemyBehavior()
            newWave >= GameConfig.FLYING_ENEMY_WAVE_THRESHOLD && Math.random() < GameConfig.FLYING_ENEMY_SPAWN_CHANCE -> FlyingEnemyBehavior()
            else -> BasicEnemyBehavior()
        }
        
        // 적 타입에 따른 체력 설정
        val isFlying = behaviorStrategy is FlyingEnemyBehavior
        health = if (isBoss) {
            GameConfig.getEnemyHealthForWave(wave, true)
        } else if (isFlying) {
            GameConfig.getEnemyHealthForWave(wave, false, true)
        } else {
            newHealth // 일반 적은 기존 파라미터 사용
        }
        maxHealth = health
        
        // 페인트 색상 재설정
        paint.color = if (isBoss) GameConfig.BOSS_COLOR else GameConfig.ENEMY_COLOR
        
        // 외곽선 페인트 재설정
        strokePaint.color = if (isBoss) GameConfig.BOSS_BORDER_COLOR else Color.WHITE
        strokePaint.strokeWidth = if (isBoss) GameConfig.BOSS_BORDER_WIDTH else 2f
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
        val isDead = behaviorStrategy.onDamage(this, damage)
        
        if (isDead) {
            deathReason = "미사일로 인한 체력 손실"
        }
        
        return isDead
    }
    
    /**
     * 적 공격력 반환
     * GameLogic에서 호출되는 메서드로, 적이 중앙에 도달했을 때 입히는 데미지
     */
    fun getDamage(): Int {
        return if (isBoss) {
            // 보스 공격력: 기본 보스 공격력 + (웨이브 - 1) * 웨이브당 보스 공격력 증가량
            GameConfig.BOSS_DAMAGE + ((wave - 1) * GameConfig.BOSS_DAMAGE_INCREASE_PER_WAVE)
        } else {
            // 공중적인지 확인
            val isFlying = behaviorStrategy is FlyingEnemyBehavior
            
            if (isFlying) {
                // 공중적 공격력: 기본 공중적 공격력 + (웨이브에서 첫 등장 웨이브를 뺀 값) * 웨이브당 공중적 공격력 증가량
                // 웨이브 6부터 등장하므로, 웨이브 6에서는 증가량이 0이 되어 기본 공격력만 적용됨
                val flyingWave = wave - GameConfig.FLYING_ENEMY_WAVE_THRESHOLD + 1
                val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
                GameConfig.FLYING_ENEMY_DAMAGE + (waveIncrease * GameConfig.FLYING_ENEMY_DAMAGE_INCREASE_PER_WAVE)
            } else {
                // 일반 적 공격력: 기본 적 공격력 + (웨이브 - 1) * 웨이브당 적 공격력 증가량
                GameConfig.NORMAL_ENEMY_DAMAGE + ((wave - 1) * GameConfig.ENEMY_DAMAGE_PER_WAVE)
            }
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
            newWave >= GameConfig.FLYING_ENEMY_WAVE_THRESHOLD && Math.random() < GameConfig.FLYING_ENEMY_SPAWN_CHANCE -> FlyingEnemyBehavior()
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
    fun getId(): Int = id
    fun getWave(): Int = wave
    
    /**
     * 현재 적이 공중적인지 확인하는 메서드
     * @return 공중적 여부
     */
    fun isFlying(): Boolean = behaviorStrategy is FlyingEnemyBehavior
    
    // 설정자 메서드들
    fun setDead(value: Boolean) { 
        dead = value 
        if (value) {
            deathReason = deathReason ?: "명시적 setDead() 호출"
        }
    }
    
    fun setHealth(value: Int) { health = value }
    fun setEnraged(value: Boolean) {
        enraged = value
        if (enraged) {
            paint.color = Color.MAGENTA // 분노 상태일 때 색상 변경
        }
    }
    
    /**
     * 적의 위치 설정
     * @param x X 좌표
     * @param y Y 좌표
     */
    fun setPosition(x: Float, y: Float) {
        position.x = x
        position.y = y
    }
    
    /**
     * 행동 전략 설정
     */
    fun setBehaviorStrategy(strategy: EnemyBehaviorStrategy) {
        behaviorStrategy = strategy
    }
    
    /**
     * 디버깅 정보 반환
     */
    override fun toString(): String {
        return "Enemy(id=$id, wave=$wave, boss=$isBoss, isDead=$dead, reason=$deathReason, health=$health/$maxHealth, pos=$position)"
    }
} 