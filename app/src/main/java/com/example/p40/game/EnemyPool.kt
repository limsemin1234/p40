package com.example.p40.game

import android.graphics.PointF
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enemy 객체 풀 클래스
 * 게임 성능 향상을 위해 Enemy 객체를 재사용
 */
class EnemyPool {
    companion object {
        private const val INITIAL_POOL_SIZE = 50
        private val instance = EnemyPool()
        
        fun getInstance(): EnemyPool = instance
    }
    
    // 객체 풀
    private val pool = ConcurrentLinkedQueue<Enemy>()
    
    init {
        // 풀 초기화
        for (i in 0 until INITIAL_POOL_SIZE) {
            pool.add(createEnemy())
        }
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
        val enemy = pool.poll() ?: createEnemy()
        
        // 객체 재설정
        enemy.reset(position, target, speed, size, health, isBoss, wave)
        
        return enemy
    }
    
    /**
     * 사용 완료된 적을 풀에 반환
     */
    fun recycle(enemy: Enemy) {
        pool.offer(enemy)
    }
    
    /**
     * 풀 크기 확장
     */
    fun expandPool(count: Int) {
        for (i in 0 until count) {
            pool.add(createEnemy())
        }
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