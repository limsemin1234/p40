package com.example.p40.game

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 미사일 관리 클래스
 * 미사일 생성, 처리, 업데이트 등 미사일 관련 모든 기능을 관리
 */
class MissileManager(
    private val gameStats: GameStats,
    private val gameConfig: GameConfig
) {
    // 미사일 관리
    private val missiles = CopyOnWriteArrayList<Missile>()
    private val missilePool = MissilePool.getInstance()
    
    // 동기화를 위한 락 객체
    private val missilesLock = Any()
    
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    /**
     * 초기화
     */
    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        missiles.clear()
    }
    
    /**
     * 미사일 업데이트 처리
     */
    fun updateMissiles(screenRect: ScreenRect): List<Missile> {
        // 현재 미사일 상태의 스냅샷을 만들어 작업
        val deadMissiles = mutableListOf<Missile>()
        val currentMissiles: List<Missile>
        
        // 미사일 목록의 스냅샷 생성 (스레드 안전하게)
        synchronized(missilesLock) {
            currentMissiles = ArrayList(missiles) // 완전히 새로운 리스트로 복사
        }
        
        val farOffScreenMargin = gameConfig.FAR_OFFSCREEN_MARGIN
        
        for (missile in currentMissiles) {
            val missilePos = missile.getPosition()
            
            // 이미 제거된 미사일은 건너뛰기
            if (missile.isDead()) {
                deadMissiles.add(missile)
                continue
            }
            
            // 화면 밖으로 완전히 벗어난 미사일은 즉시 제거 표시
            if (missilePos.x < -farOffScreenMargin || missilePos.x > screenWidth + farOffScreenMargin ||
                missilePos.y < -farOffScreenMargin || missilePos.y > screenHeight + farOffScreenMargin) {
                missile.setOutOfBounds()
                deadMissiles.add(missile)
                continue
            }
            
            // 화면 내부 또는 가까운 범위의 미사일만 업데이트
            if (screenRect.contains(missilePos.x, missilePos.y)) {
                // 항상 원래의 속도로 이동 (웨이브에 관계없이 동일한 속도)
                missile.update(1.0f)
            }
            
            // 죽은 미사일 확인
            if (missile.isDead()) {
                deadMissiles.add(missile)
            }
        }
        
        return deadMissiles
    }
    
    /**
     * 미사일 충돌 체크
     */
    fun checkMissileCollisions(enemies: List<Enemy>, defenseUnit: DefenseUnit): List<Missile> {
        val deadMissiles = mutableListOf<Missile>()
        
        // 현재 미사일의 스냅샷을 사용
        val currentMissiles = synchronized(missilesLock) { ArrayList(missiles) }
        
        for (missile in currentMissiles) {
            // 이미 죽은 미사일은 스킵
            if (missile.isDead()) {
                deadMissiles.add(missile)
                continue
            }
            
            var hasCollided = false
            
            // 적들과 충돌 체크
            for (enemy in enemies) {
                // 죽은 적과는 충돌 체크하지 않음
                if (enemy.isDead()) continue
                
                if (missile.checkCollision(enemy)) {
                    // 하트 문양 효과: 적 공격 시 GameConfig에 정의된 양만큼 체력 회복
                    if (defenseUnit.isHealOnDamage()) {
                        gameStats.healUnit(GameConfig.HEART_HEAL_ON_DAMAGE)
                    }
                    
                    // 충돌 발생
                    hasCollided = true
                    deadMissiles.add(missile)
                    break
                }
            }
        }
        
        return deadMissiles
    }
    
    /**
     * 미사일 제거 처리
     */
    fun removeMissiles(deadMissiles: List<Missile>) {
        if (deadMissiles.isEmpty()) return
        
        // 제거할 미사일이 있는 경우에만 처리
        val uniqueDeadMissiles = deadMissiles.distinctBy { it.getId() }.toSet()
        
        // 스레드 안전하게 미사일 제거
        synchronized(missilesLock) {
            missiles.removeAll(uniqueDeadMissiles)
        }
        
        // 미사일 객체 풀에 반환
        uniqueDeadMissiles.forEach { missilePool.recycle(it) }
    }
    
    /**
     * 새 미사일 추가
     */
    fun addMissile(missile: Missile): Boolean {
        // 로직 중에 방어 유닛이 미사일을 발사할 경우 이 메서드로 추가
        if (missile.isDead()) return false
        
        var added = false
        
        // 스레드 안전하게 미사일 추가
        synchronized(missilesLock) {
            // 추가하기 전에 기존 미사일이 있는지 확인
            val beforeCount = missiles.size
            
            // 새 미사일 추가
            missiles.add(missile)
            
            // 정말 추가되었는지 확인
            val afterCount = missiles.size
            if (afterCount > beforeCount) {
                added = true
            }
        }
        
        // 추가 실패 시 미사일 반환
        if (!added) {
            missilePool.recycle(missile)
        }
        
        return added
    }
    
    /**
     * 현재 미사일 목록 반환
     */
    fun getMissiles(): CopyOnWriteArrayList<Missile> = missiles
    
    /**
     * 게임 재시작 시 리셋
     */
    fun reset() {
        // 미사일 초기화 및 풀에 반환
        synchronized(missilesLock) {
            missiles.forEach { missilePool.recycle(it) }
            missiles.clear()
        }
    }
} 