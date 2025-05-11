package com.example.p40.game

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 게임 논리 처리 클래스
 * GameView에서 게임 논리(적 생성, 충돌, 업데이트 등) 관련 로직을 분리함
 * 리팩토링: 적 관리와 미사일 관리를 각각 EnemyManager와 MissileManager로 위임
 */
class GameLogic(
    private val gameStats: GameStats,
    private val gameConfig: GameConfig,
    private val gameOverListener: GameOverListener? = null,
    private val bossKillListener: BossKillListener? = null,
    private val levelClearListener: LevelClearListener? = null
) {
    // 게임 상태
    private var isRunning = false
    private var isPaused = false
    private var isGameOver = false
    private var showWaveMessage = false
    private var waveMessageStartTime = 0L
    private var waveMessageDuration = gameConfig.WAVE_MESSAGE_DURATION
    private var timeFrozen = false  // 시간 멈춤 상태 변수 추가
    private var rangeBasedTimeFrozen = false  // 범위 기반 시간 멈춤 상태 변수
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // 타이밍 관련
    private var gameStartTime = 0L
    
    // 매니저 클래스 인스턴스
    private val enemyManager = EnemyManager(gameStats, gameConfig, gameOverListener, bossKillListener)
    private val missileManager = MissileManager(gameStats, gameConfig)
    
    /**
     * 게임 초기화
     */
    fun initGame(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        
        // 중앙에 방어 유닛 배치
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        defenseUnit = DefenseUnit(
            position = PointF(centerX, centerY),
            attackRange = gameStats.getUnitAttackRange(),
            attackCooldown = gameStats.getUnitAttackSpeed()
        )
        
        gameStartTime = System.currentTimeMillis()
        isRunning = true
        isGameOver = false
        isPaused = false
        
        // 매니저 초기화
        enemyManager.init(width, height, gameStartTime)
        missileManager.init(width, height)
        
        // 게임 시작 시 1웨이브 메시지 표시
        showWaveMessage = true
        waveMessageStartTime = System.currentTimeMillis()
    }
    
    /**
     * 시간 멈춤 상태 설정 (플러시 스킬용)
     */
    fun setTimeFrozen(frozen: Boolean) {
        this.timeFrozen = frozen
        enemyManager.setTimeFrozen(frozen)
        
        // 범위 기반 시간 멈춤이 설정될 때 기존 전체 시간 멈춤은 비활성화
        if (!frozen) {
            this.rangeBasedTimeFrozen = false
        }
    }
    
    /**
     * 범위 기반 시간 멈춤 상태 설정 (클로버 플러시 스킬용)
     */
    fun setRangeBasedTimeFrozen(frozen: Boolean) {
        this.rangeBasedTimeFrozen = frozen
        enemyManager.setRangeBasedTimeFrozen(frozen)
        
        // 범위 기반 시간 멈춤이 켜지면 전체 시간 멈춤은 끄기
        if (frozen) {
            this.timeFrozen = false
        }
    }
    
    /**
     * 무적 상태 설정 (다이아몬드 플러시 스킬용)
     */
    fun setInvincible(invincible: Boolean) {
        enemyManager.setInvincible(invincible)
    }
    
    /**
     * 게임 업데이트 메인 로직
     */
    fun update() {
        if (isPaused || isGameOver) return
        
        val currentTime = System.currentTimeMillis()
        
        // 웨이브 메시지 표시 시간 체크
        if (showWaveMessage && currentTime - waveMessageStartTime > waveMessageDuration) {
            showWaveMessage = false
        }
        
        // 웨이브 내 적 생성 (웨이브 메시지와 상관없이 항상 적 생성)
        if (gameStats.getWaveCount() <= gameConfig.getTotalWaves()) {
            enemyManager.handleEnemySpawning(currentTime, isGameOver)
        }
        
        // 보스 소환 조건 체크
        enemyManager.checkBossSpawnCondition(currentTime)
        
        // 화면 범위 계산 (성능 최적화를 위한 값)
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 적 생성 거리보다 크게 visibleMargin 설정 (적 생성 거리 + 여유 공간)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * gameConfig.ENEMY_SPAWN_DISTANCE_FACTOR
        val visibleMargin = spawnDistance + gameConfig.ENEMY_UPDATE_MARGIN
        
        // 화면 범위 계산 (화면 밖 일정 거리까지 포함)
        val minX = -visibleMargin
        val minY = -visibleMargin
        val maxX = screenWidth + visibleMargin
        val maxY = screenHeight + visibleMargin
        
        // 화면 영역 객체
        val screenRect = ScreenRect(minX, minY, maxX, maxY)
        
        // 1. 적 업데이트
        val deadEnemies = enemyManager.updateEnemies(screenRect, centerX, centerY, defenseUnit, isGameOver)
        
        // 2. 디펜스 유닛 공격 로직 (미사일 발사)
        updateDefenseUnitAttack(screenRect, currentTime)
        
        // 3. 미사일 업데이트
        val deadMissiles = missileManager.updateMissiles(screenRect)
        
        // 4. 미사일 충돌 체크
        val collisionDeadMissiles = missileManager.checkMissileCollisions(
            enemyManager.getEnemies(),
            defenseUnit
        )
        
        // 5. 미사일 제거
        missileManager.removeMissiles(deadMissiles + collisionDeadMissiles)
        
        // 6. 죽은 적 처리
        enemyManager.processDeadEnemies(deadEnemies) {
            // 다음 웨이브로 이동 콜백
            startNextWave()
        }
    }
    
    /**
     * 디펜스 유닛 공격 처리
     */
    private fun updateDefenseUnitAttack(screenRect: ScreenRect, currentTime: Long) {
        // 공격 쿨다운 계산
        val attackCooldown = gameStats.getUnitAttackSpeed()
        
        // 모든 적을 대상으로 공격 처리 (죽지 않은 모든 적)
        val aliveEnemies = enemyManager.getEnemies().filter { !it.isDead() }
        
        // 적이 없으면 처리하지 않음
        if (aliveEnemies.isEmpty()) return
        
        // 기본 1방향 발사
        val missileDamageMultiplier = gameStats.getBuffManager().getMissileDamageMultiplier()
        
        // 업그레이드된 공격력 값 전달
        val baseDamage = gameStats.getUnitAttackPower()
        
        val newMissile = defenseUnit.attack(
            aliveEnemies, 
            currentTime, 
            attackCooldown,
            missileDamageMultiplier,
            0.0,  // 기본 각도 오프셋
            baseDamage  // 업그레이드된 공격력 전달
        )
        
        // 새 미사일 추가
        if (newMissile != null) {
            missileManager.addMissile(newMissile)
        }
    }
    
    /**
     * 다음 웨이브 시작
     */
    private fun startNextWave() {
        // 현재 웨이브가 총 웨이브 수와 같으면 게임 레벨 클리어
        if (gameStats.getWaveCount() >= gameConfig.getTotalWaves()) {
            // 레벨 클리어 리스너 호출
            levelClearListener?.onLevelCleared(gameStats.getWaveCount(), gameStats.getResource())
            return
        }
        
        // 다음 웨이브 시작
        gameStats.nextWave()
        
        // 웨이브 시작 메시지 표시
        showWaveMessage = true
        waveMessageStartTime = System.currentTimeMillis()
    }
    
    /**
     * 일시정지 처리
     */
    fun pause() {
        isPaused = true
    }
    
    /**
     * 게임 재개
     */
    fun resume() {
        isPaused = false
    }
    
    /**
     * 게임 재시작
     */
    fun restartGame() {
        gameStats.resetGame()
        
        // 매니저 클래스 리셋
        enemyManager.reset(System.currentTimeMillis())
        missileManager.reset()
        
        // 게임 상태 초기화
        isGameOver = false
        isPaused = false
        
        // 게임 시작 시간 재설정
        gameStartTime = System.currentTimeMillis()
        
        // 디펜스 유닛 업데이트
        if (this::defenseUnit.isInitialized) {
            defenseUnit.setAttackRange(gameStats.getUnitAttackRange())
            defenseUnit.setAttackCooldown(gameStats.getUnitAttackSpeed())
        }
    }
    
    /**
     * 화면 내 모든 적 제거 (보스 제외) - 플러시 스킬용
     * @return 제거된 적의 수
     */
    fun removeAllEnemiesExceptBoss(): Int {
        val centerX = defenseUnit.getPosition().x
        val centerY = defenseUnit.getPosition().y
        val attackRange = defenseUnit.attackRange
        
        return enemyManager.removeAllEnemiesExceptBoss(centerX, centerY, attackRange)
    }
    
    // 게임 상태 접근자 메서드들
    fun isGamePaused(): Boolean = isPaused
    fun isGameOver(): Boolean = isGameOver
    fun isShowingWaveMessage(): Boolean = showWaveMessage
    
    // 게임 요소 접근자 메서드들
    fun getDefenseUnit(): DefenseUnit = defenseUnit
    fun getEnemies(): CopyOnWriteArrayList<Enemy> = enemyManager.getEnemies()
    fun getMissiles(): CopyOnWriteArrayList<Missile> = missileManager.getMissiles()
    
    // 화면 정보 접근자 메서드들
    fun getScreenWidth(): Float = screenWidth
    fun getScreenHeight(): Float = screenHeight
    
    /**
     * 현재 보스 체력 반환
     */
    fun getCurrentBossHealth(): Int {
        return enemyManager.getCurrentBossHealth()
    }
} 