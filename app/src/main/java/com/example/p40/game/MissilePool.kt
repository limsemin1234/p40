package com.example.p40.game

import android.graphics.PointF
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Missile 객체 풀 클래스
 * 게임 성능 향상을 위해 Missile 객체를 재사용
 */
class MissilePool {
    companion object {
        private const val INITIAL_POOL_SIZE = 100
        private val instance = MissilePool()
        
        fun getInstance(): MissilePool = instance
    }
    
    // 객체 풀
    private val pool = ConcurrentLinkedQueue<Missile>()
    
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
        
        // 풀이 비었으면 자동으로 확장 (10개씩 추가)
        if (missile == null) {
            expandPool(10)
            return obtain(position, angle, speed, size, damage, target) // 재귀 호출
        }
        
        // 객체 재설정
        missile.reset(position, angle, speed, size, damage, target)
        
        return missile
    }
    
    /**
     * 사용 완료된 미사일을 풀에 반환
     */
    fun recycle(missile: Missile) {
        pool.offer(missile)
    }
    
    /**
     * 풀 크기 확장
     */
    fun expandPool(count: Int) {
        for (i in 0 until count) {
            pool.add(createMissile())
        }
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