package com.example.p40

import android.graphics.PointF
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enemy 객체 풀 클래스
 * 게임 성능 향상을 위해 Enemy 객체를 재사용
 * 최적화: 자동 확장 기능 추가, 풀 사이즈 모니터링 추가
 */
class EnemyPool {
    companion object {
        private const val INITIAL_POOL_SIZE = 100 // 기본 풀 사이즈 증가
        private const val MAX_POOL_SIZE = 300 // 최대 풀 사이즈 제한
        private const val AUTO_EXPAND_SIZE = 20 // 자동 확장 시 추가되는 객체 수
        private val instance = EnemyPool()
        
        fun getInstance(): EnemyPool = instance
    }
    
    // 객체 풀
    private val pool = ConcurrentLinkedQueue<Enemy>()
    
    // 성능 모니터링을 위한 카운터
    private val poolMisses = AtomicInteger(0) // 풀에 객체가 없어서 새로 생성한 횟수
    private val poolHits = AtomicInteger(0) // 풀에서 객체를 재사용한 횟수
    private var currentPoolSize = 0 // 현재 풀 사이즈
    
    init {
        // 풀 초기화
        expandPool(INITIAL_POOL_SIZE)
    }
    
    /**
     * 풀에서 적 가져오기
     * @param position 시작 위치
     * @param target 목표 위치
     * @param speed 이동 속도
     * @param size 크기
     * @param health 체력
     * @param isBoss 보스 여부
     * @param wave 웨이브 번호
     * @return 설정된 Enemy 객체
     */
    fun obtain(
        position: PointF,
        target: PointF,
        speed: Float,
        size: Float = GameConfig.ENEMY_BASE_SIZE,
        health: Int = GameConfig.ENEMY_BASE_HEALTH,
        isBoss: Boolean = false,
        wave: Int = 1
    ): Enemy {
        // 풀에서 객체 가져오기 또는 새로 생성
        val enemy = pool.poll()
        
        if (enemy != null) {
            // 풀 히트 증가
            poolHits.incrementAndGet()
            // 객체 재설정
            enemy.reset(position, target, speed, size, health, isBoss, wave)
            currentPoolSize--
            return enemy
        } else {
            // 풀 미스 증가
            poolMisses.incrementAndGet()
            
            // 풀이 특정 기준 이하로 줄어들면 자동 확장
            if (currentPoolSize < AUTO_EXPAND_SIZE && currentPoolSize + AUTO_EXPAND_SIZE <= MAX_POOL_SIZE) {
                expandPool(AUTO_EXPAND_SIZE)
            }
            
            // 새 객체 생성
            val newEnemy = createEnemy()
            newEnemy.reset(position, target, speed, size, health, isBoss, wave)
            return newEnemy
        }
    }
    
    /**
     * 사용 완료된 적을 풀에 반환
     */
    fun recycle(enemy: Enemy) {
        // 풀 사이즈가 최대치보다 작을 때만 추가
        if (currentPoolSize < MAX_POOL_SIZE) {
            pool.offer(enemy)
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
            pool.add(createEnemy())
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
     * 새 Enemy 객체 생성
     */
    private fun createEnemy(): Enemy {
        return Enemy(
            position = PointF(0f, 0f),
            target = PointF(0f, 0f),
            speed = 0f,
            size = 0f,
            health = 0,
            isBoss = false,
            wave = 1
        )
    }
} 