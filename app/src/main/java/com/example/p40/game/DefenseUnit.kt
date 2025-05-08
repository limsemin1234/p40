package com.example.p40.game

import android.graphics.Canvas
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 카드 문양 타입을 정의하는 enum 클래스
 */
enum class CardSymbolType {
    SPADE, HEART, DIAMOND, CLUB;
    
    // 다음 문양 타입으로 순환
    fun next(): CardSymbolType {
        return when (this) {
            SPADE -> HEART
            HEART -> DIAMOND
            DIAMOND -> CLUB
            CLUB -> SPADE
        }
    }
}

/**
 * 디펜스 유닛 클래스 - 벡터 계산 최적화 적용
 */
class DefenseUnit(
    private val position: PointF,
    attackRange: Float,
    private var attackCooldown: Long
) {
    // 현재 카드 문양 타입
    private var symbolType: CardSymbolType = CardSymbolType.SPADE
    
    // 문양 효과에 대한 배율들
    private var damageMultiplier: Float = 1.0f  // 공격력 배율
    private var speedMultiplier: Float = 1.0f   // 공격속도 배율
    private var rangeMultiplier: Float = 1.0f   // 공격범위 배율
    
    // 적 처치 시 체력 회복 활성화 여부
    private var healOnDamage: Boolean = false
    
    private var lastAttackTime: Long = 0
    
    // 내부적으로 사용할 private 필드로 변경
    private var _attackRange: Float = attackRange
    private var originalAttackRange: Float = attackRange // 원래 공격 범위 저장
    
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
    
    /**
     * 카드 문양 타입 반환
     */
    fun getSymbolType(): CardSymbolType = symbolType
    
    /**
     * 카드 문양 변경
     */
    fun changeSymbolType() {
        symbolType = symbolType.next()
        applySymbolEffects()
    }
    
    /**
     * 현재 문양에 따른 효과 적용
     */
    private fun applySymbolEffects() {
        // 모든 효과 초기화
        resetSymbolEffects()
        
        when (symbolType) {
            CardSymbolType.SPADE -> {
                // 기본 상태 - 아무 효과 없음
            }
            CardSymbolType.HEART -> {
                // 하트: 적에게 데미지를 줄 때마다 체력 1 회복, 공격력 50% 감소
                damageMultiplier = 0.5f
                healOnDamage = true
            }
            CardSymbolType.DIAMOND -> {
                // 다이아몬드: 공격속도 2배 증가, 공격범위 50% 감소
                speedMultiplier = 2.0f
                rangeMultiplier = 0.5f
                _attackRange = originalAttackRange * rangeMultiplier
                updateAttackRangeSquared()
            }
            CardSymbolType.CLUB -> {
                // 클로버: 공격범위 50% 증가, 체력 감소는 GameView에서 처리
                rangeMultiplier = 1.5f
                _attackRange = originalAttackRange * rangeMultiplier
                updateAttackRangeSquared()
            }
        }
        
        // 문양 효과 적용 시 벡터 캐시 초기화
        // 새 범위/효과로 적 타겟팅이 바로 적용되도록 함
        clearVectorCache()
    }
    
    /**
     * 문양 효과 초기화
     */
    private fun resetSymbolEffects() {
        damageMultiplier = 1.0f
        speedMultiplier = 1.0f
        rangeMultiplier = 1.0f
        healOnDamage = false
        
        // 원래 공격 범위로 복원
        _attackRange = originalAttackRange
        updateAttackRangeSquared()
    }
    
    /**
     * 카드 문양 설정
     */
    fun setSymbolType(type: CardSymbolType) {
        symbolType = type
        applySymbolEffects()
    }
    
    /**
     * 공격력 배율 반환
     */
    fun getDamageMultiplier(): Float = damageMultiplier
    
    /**
     * 공격속도 배율 반환
     */
    fun getSpeedMultiplier(): Float = speedMultiplier
    
    /**
     * 공격범위 배율 반환
     */
    fun getRangeMultiplier(): Float = rangeMultiplier
    
    /**
     * 데미지 시 회복 여부 반환
     */
    fun isHealOnDamage(): Boolean = healOnDamage
    
    // attackRange 제곱값 업데이트
    private fun updateAttackRangeSquared() {
        attackRangeSquared = _attackRange * _attackRange
    }
    
    // 공격 범위 설정 시 제곱값도 함께 업데이트
    fun setAttackRange(newRange: Float) {
        originalAttackRange = newRange
        _attackRange = originalAttackRange * rangeMultiplier
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
        angleOffset: Double = 0.0,
        baseDamage: Int = GameConfig.MISSILE_DAMAGE  // 기본 데미지 값 추가 (업그레이드된 값이 전달됨)
    ): Missile? {
        // 쿨다운 체크 (speedMultiplier 적용)
        val effectiveCooldown = (adjustedCooldown / speedMultiplier).toLong()
        if (currentTime - lastAttackTime < effectiveCooldown) {
            return null
        }
        
        // 가장 가까운 적 찾기
        val target = if (enemies.isNotEmpty()) findNearestEnemy(enemies) else null
        
        // 타겟이 없으면 미사일을 발사하지 않음
        if (target == null) {
            // 적은 있지만 타겟이 없는 경우 디버깅용
            if (enemies.isNotEmpty() && enemies.any { !it.isDead() }) {
                // 여기에 로그를 추가하거나 디버깅용 변수를 설정할 수 있음
                // 실제 앱에서는 android.util.Log를 사용할 수 있음
            }
            return null
        }
        
        lastAttackTime = currentTime
        
        // 타겟 좌표 계산
        val targetPos = target.getPosition()
        // 벡터 계산 캐싱 활용
        val dx = tempDx[target] ?: (targetPos.x - position.x).also { tempDx[target] = it }
        val dy = tempDy[target] ?: (targetPos.y - position.y).also { tempDy[target] = it }
        val angle = atan2(dy.toDouble(), dx.toDouble()) + angleOffset
        
        // 각도 관련 삼각함수 계산 결과 캐싱
        cacheAngleCalculation(angle)
        
        val missileSpeed = GameConfig.MISSILE_SPEED
        // 공격력에 현재 문양의 데미지 배율 적용 - 기본 데미지로 GameConfig.MISSILE_DAMAGE 대신 baseDamage 사용
        val damage = (baseDamage * damageMultiplier * this.damageMultiplier).toInt()
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
        
        // 적이 범위 내로 이동했을 때 감지하기 위해 모든 적에 대한 거리 재계산
        // (최적화에는 좋지 않지만 적이 범위 내로 들어왔을 때 감지하기 위함)
        for (enemy in enemies) {
            if (!enemy.isDead()) {
                tempDx.remove(enemy)
                tempDy.remove(enemy)
                tempDistanceSquared.remove(enemy)
            }
        }
        
        // 디버깅: 적의 수 확인
        val enemyCount = enemies.filter { !it.isDead() }.size
        if (enemyCount == 0) {
            return null // 적이 없으면 바로 종료
        }
        
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
            
            // 공격 범위 내에 있는 적만 타겟팅 (제한 완화 - 범위를 2배로 설정)
            if (distanceSquared <= attackRangeSquared && distanceSquared < minDistance) {
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
    
    /**
     * 디펜스 유닛 공격 범위 그리기
     */
    fun drawAttackRange(canvas: Canvas) {
        // 공격 범위 원 그리기
        val rangePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(80, 100, 180, 255) // 반투명 파란색
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // 실제 공격 범위 원
        canvas.drawCircle(position.x, position.y, _attackRange, rangePaint)
    }
} 