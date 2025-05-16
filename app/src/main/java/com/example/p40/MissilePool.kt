package com.example.p40

import android.graphics.PointF
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Missile 객체 풀 클래스
 * 게임 성능 향상을 위해 Missile 객체를 재사용
 * 최적화: 자동 확장 기능 추가, 풀 사이즈 모니터링 추가
 */
class MissilePool {
    companion object {
        private const val INITIAL_POOL_SIZE = 200 // 초기 풀 사이즈 증가
        private const val MAX_POOL_SIZE = 500 // 최대 풀 사이즈 제한
        private const val AUTO_EXPAND_SIZE = 50 // 자동 확장 시 추가되는 객체 수
        private val instance = MissilePool()
        
        fun getInstance(): MissilePool = instance
    }
    
    // 객체 풀
    private val pool = ConcurrentLinkedQueue<Missile>()
    
    // 성능 모니터링을 위한 카운터
    private val poolMisses = AtomicInteger(0) // 풀에 객체가 없어서 새로 생성한 횟수
    private val poolHits = AtomicInteger(0) // 풀에서 객체를 재사용한 횟수
    private var currentPoolSize = 0 // 현재 풀 사이즈
    
    init {
        // 풀 초기화
        expandPool(INITIAL_POOL_SIZE)
    }
    
    /**
     * 풀에서 미사일 가져오기
     * @param position 시작 위치
     * @param angle 발사 각도
     * @param speed 이동 속도
     * @param size 크기
     * @param damage 데미지
     * @param target 목표 적
     * @return 설정된 Missile 객체
     */
    fun obtain(
        position: PointF,
        angle: Double,
        speed: Float = GameConfig.MISSILE_SPEED,
        size: Float = GameConfig.MISSILE_SIZE,
        damage: Int = GameConfig.MISSILE_DAMAGE,
        target: Enemy?
    ): Missile {
        // 풀에서 객체 가져오기
        val missile = pool.poll()
        
        if (missile != null) {
            // 풀 히트 증가
            poolHits.incrementAndGet()
            // 객체 재설정
            missile.reset(position, angle, speed, size, damage, target)
            currentPoolSize--
            return missile
        } else {
            // 풀 미스 증가
            poolMisses.incrementAndGet()
            
            // 풀이 특정 기준 이하로 줄어들면 자동 확장
            if (currentPoolSize < AUTO_EXPAND_SIZE && currentPoolSize + AUTO_EXPAND_SIZE <= MAX_POOL_SIZE) {
                expandPool(AUTO_EXPAND_SIZE)
                // 확장 후 다시 시도
                val newMissile = pool.poll()
                if (newMissile != null) {
                    poolHits.incrementAndGet()
                    newMissile.reset(position, angle, speed, size, damage, target)
                    currentPoolSize--
                    return newMissile
                }
            }
            
            // 새 객체 생성
            val newMissile = createMissile()
            newMissile.reset(position, angle, speed, size, damage, target)
            return newMissile
        }
    }
    
    /**
     * 사용 완료된 미사일을 풀에 반환
     */
    fun recycle(missile: Missile) {
        // 풀 사이즈가 최대치보다 작을 때만 추가
        if (currentPoolSize < MAX_POOL_SIZE) {
            pool.offer(missile)
            currentPoolSize++
        }
    }
    
    /**
     * 풀 크기 확장
     */
    fun expandPool(count: Int) {
        // 최대 크기 제한 적용
        val actualCount = minOf(count, MAX_POOL_SIZE - currentPoolSize)
        if (actualCount <= 0) return
        
        for (i in 0 until actualCount) {
            pool.add(createMissile())
            currentPoolSize++
        }
    }
    
    /**
     * 풀 사용 통계 정보 반환
     */
    fun getPoolStats(): String {
        val hits = poolHits.get()
        val misses = poolMisses.get()
        val total = hits + misses
        val hitRate = if (total > 0) hits * 100.0 / total else 0.0
        return "Pool Size: $currentPoolSize, Hits: $hits, Misses: $misses, Hit Rate: ${String.format("%.2f", hitRate)}%"
    }
    
    /**
     * 통계 리셋
     */
    fun resetStats() {
        poolHits.set(0)
        poolMisses.set(0)
    }
    
    /**
     * 새 Missile 객체 생성
     */
    private fun createMissile(): Missile {
        return Missile(
            position = PointF(0f, 0f),
            angle = 0.0,
            speed = 0f,
            size = 0f,
            damage = 0,
            target = null
        )
    }
} 