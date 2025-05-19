package com.example.p40

import android.content.Context
import android.graphics.PointF
import com.example.p40.UserManager
import java.util.concurrent.CopyOnWriteArrayList

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
    private val levelClearListener: LevelClearListener? = null,
    private val context: Context
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
    
    // 디펜스 유닛 문양 변경 리스너
    private var symbolChangeListener: DefenseUnitSymbolChangeListener? = null
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // 타이밍 관련
    private var gameStartTime = 0L
    
    // 매니저 클래스 인스턴스
    private lateinit var enemyManager: EnemyManager
    private lateinit var missileManager: MissileManager
    
    // GameView 참조
    private lateinit var gameView: GameView
    
    /**
     * GameView 참조 설정
     */
    fun setGameView(gameView: GameView) {
        this.gameView = gameView
    }
    
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
        
        // gameView 초기화 여부 확인 (안전장치 추가)
        if (!::gameView.isInitialized) {
            // 로그 출력 또는 예외 처리 대신 조용히 리턴
            // 나중에 setGameView가 호출되면 필요한 설정을 할 수 있도록 게임 상태는 유지
            return
        }
        
        // 사용자가 설정한 유닛 문양 적용 (첫 번째 적용된 유닛 기준)
        val userManager = UserManager.getInstance(context)
        val appliedUnits = userManager.getAppliedDefenseUnits()
        if (appliedUnits.isNotEmpty()) {
            val symbolTypeOrdinal = appliedUnits[0] // 첫 번째 적용된 유닛의 문양 가져오기
            val symbolType = CardSymbolType.values()[symbolTypeOrdinal]
            defenseUnit.setSymbolType(symbolType) // 디펜스 유닛에 문양 설정
        }
        
        gameStartTime = System.currentTimeMillis()
        isRunning = true
        isGameOver = false
        isPaused = false
        
        // 매니저 초기화
        enemyManager = EnemyManager(context, gameStats, gameConfig, gameView, gameOverListener, bossKillListener)
        missileManager = MissileManager(gameStats, gameConfig)
        
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
        
        // enemyManager 초기화 여부 확인
        if (!::enemyManager.isInitialized) {
            return
        }
        
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
        
        // enemyManager 초기화 여부 확인
        if (!::enemyManager.isInitialized) {
            return
        }
        
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
        // enemyManager 초기화 여부 확인
        if (!::enemyManager.isInitialized) {
            return
        }
        
        enemyManager.setInvincible(invincible)
    }
    
    /**
     * 게임 로직 업데이트 - 성능 최적화 적용
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
        
        // 화면 영역 객체 - 이제 공간 분할 그리드 기능 포함
        val screenRect = ScreenRect(minX, minY, maxX, maxY)
        
        // 1. 적 업데이트
        val deadEnemies = enemyManager.updateEnemies(screenRect, centerX, centerY, defenseUnit, isGameOver)
        
        // 2. 디펜스 유닛 공격 로직 (미사일 발사)
        updateDefenseUnitAttack(screenRect, currentTime)
        
        // 3. 미사일 업데이트 (새 공간 분할 그리드 사용)
        val deadMissiles = missileManager.updateMissiles(screenRect)
        
        // 4. 미사일 충돌 체크 (그리드 기반 충돌 감지)
        val collisionDeadMissiles = missileManager.checkMissileCollisions(
            enemyManager.getEnemies(),
            defenseUnit
        )
        
        // 5. 미사일 제거
        if (deadMissiles.isNotEmpty() || collisionDeadMissiles.isNotEmpty()) {
            missileManager.removeMissiles(deadMissiles + collisionDeadMissiles)
        }
        
        // 6. 죽은 적 처리
        if (deadEnemies.isNotEmpty()) {
            enemyManager.processDeadEnemies(deadEnemies) {
                // 다음 웨이브로 이동 콜백
                startNextWave()
            }
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
            
            // 사용자가 설정한 유닛 문양 적용 (첫 번째 적용된 유닛 기준)
            val userManager = UserManager.getInstance(context)
            val appliedUnits = userManager.getAppliedDefenseUnits()
            if (appliedUnits.isNotEmpty()) {
                val symbolTypeOrdinal = appliedUnits[0] // 첫 번째 적용된 유닛의 문양 가져오기
                val symbolType = CardSymbolType.values()[symbolTypeOrdinal]
                defenseUnit.setSymbolType(symbolType) // 디펜스 유닛에 문양 설정
            }
        }
    }
    
    /**
     * 화면 내 모든 적 제거 (보스 제외) - 플러시 스킬용
     * @return 제거된 적의 수
     */
    fun removeAllEnemiesExceptBoss(): Int {
        if (!::enemyManager.isInitialized) {
            return 0 // enemyManager가 초기화되지 않은 경우 0 반환
        }
        
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
    fun getEnemies(): CopyOnWriteArrayList<Enemy> = 
        if (::enemyManager.isInitialized) enemyManager.getEnemies() else CopyOnWriteArrayList()
    fun getMissiles(): CopyOnWriteArrayList<Missile> = 
        if (::missileManager.isInitialized) missileManager.getMissiles() else CopyOnWriteArrayList()
    
    // 화면 정보 접근자 메서드들
    fun getScreenWidth(): Float = screenWidth
    fun getScreenHeight(): Float = screenHeight
    
    /**
     * 현재 보스 체력 반환
     */
    fun getCurrentBossHealth(): Int {
        return if (::enemyManager.isInitialized) {
            enemyManager.getCurrentBossHealth()
        } else {
            0 // enemyManager가 초기화되지 않은 경우 기본값 0 반환
        }
    }

    /**
     * 게임 렌더링 - Canvas에 게임 요소 그리기
     */
    fun render(canvas: android.graphics.Canvas) {
        // 화면 범위
        val screenRect = ScreenRect(0f, 0f, screenWidth, screenHeight)
        
        // 배경 지우기 (검은색)
        canvas.drawColor(android.graphics.Color.BLACK)
        
        // 디펜스 유닛 공격 범위 표시 (명확한 색상과 두께로 항상 표시)
        val rangePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(100, 100, 180, 255) // 반투명 파란색
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(screenWidth / 2, screenHeight / 2, defenseUnit.attackRange, rangePaint)
        
        // 1. 적 그리기
        val visibleEnemies = enemyManager.getEnemies().filter { 
            val pos = it.getPosition()
            // 화면 안쪽이나 약간 밖에 있는 적만 그리기 (성능 최적화)
            pos.x >= -gameConfig.ENEMY_RENDER_MARGIN_X && 
            pos.x <= screenWidth + gameConfig.ENEMY_RENDER_MARGIN_X &&
            pos.y >= -gameConfig.ENEMY_RENDER_MARGIN_Y && 
            pos.y <= screenHeight + gameConfig.ENEMY_RENDER_MARGIN_Y &&
            !it.isDead()
        }
        for (enemy in visibleEnemies) {
            enemy.draw(canvas)
        }
        
        // 2. 미사일 그리기
        val visibleMissiles = missileManager.getMissiles().filter {
            val pos = it.getPosition()
            // 화면 안쪽이나 약간 밖에 있는 미사일만 그리기 (성능 최적화)
            pos.x >= -gameConfig.MISSILE_RENDER_MARGIN_X && 
            pos.x <= screenWidth + gameConfig.MISSILE_RENDER_MARGIN_X &&
            pos.y >= -gameConfig.MISSILE_RENDER_MARGIN_Y && 
            pos.y <= screenHeight + gameConfig.MISSILE_RENDER_MARGIN_Y &&
            !it.isDead()
        }
        for (missile in visibleMissiles) {
            missile.draw(canvas)
        }
        
        // 3. 디펜스 유닛 그리기
        defenseUnit.draw(canvas)
        
        // 4. UI 그리기
        drawGameUI(canvas)
    }

    /**
     * 게임 UI 그리기 (점수, 상태 등)
     */
    private fun drawGameUI(canvas: android.graphics.Canvas) {
        // 화면 중앙 메시지만 표시 (상단 wave, HP, resource 정보는 제거)
        
        // 웨이브 시작 메시지
        if (showWaveMessage) {
            val wavePaint = android.graphics.Paint().apply {
                color = gameConfig.WAVE_TEXT_COLOR
                textSize = gameConfig.TEXT_SIZE_WAVE
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val waveStartText = "Wave ${gameStats.getWaveCount()} Start!"
            canvas.drawText(waveStartText, screenWidth / 2, screenHeight / 2, wavePaint)
        }
        
        // 일시정지 화면
        if (isPaused) {
            val pausePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = gameConfig.TEXT_SIZE_PAUSE
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2, pausePaint)
        }
        
        // 게임 오버 화면
        if (isGameOver) {
            val gameOverPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                textSize = gameConfig.TEXT_SIZE_PAUSE
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("GAME OVER", screenWidth / 2, screenHeight / 2, gameOverPaint)
        }
    }

    /**
     * 디펜스 유닛 문양 변경 - 클릭 시
     * UserManager에서 적용 설정된 유닛들 사이에서 순환합니다.
     */
    fun changeDefenseUnitSymbol() {
        val userManager = UserManager.getInstance(context)
        val appliedUnits = userManager.getAppliedDefenseUnits()
        
        if (appliedUnits.isEmpty()) {
            // 기본 스페이드 문양만 사용 가능한 경우
            return
        }
        
        // 현재 문양 가져오기
        val currentSymbolType = defenseUnit.getSymbolType()
        val currentOrdinal = currentSymbolType.ordinal
        
        // 현재 문양이 적용 목록에 없으면 첫 번째 적용 유닛으로 설정
        if (!appliedUnits.contains(currentOrdinal)) {
            val nextOrdinal = appliedUnits[0]
            val nextSymbol = CardSymbolType.values()[nextOrdinal]
            defenseUnit.setSymbolType(nextSymbol)
            symbolChangeListener?.onSymbolChanged(nextSymbol)
            return
        }
        
        // 현재 문양의 인덱스 찾기
        val currentIndex = appliedUnits.indexOf(currentOrdinal)
        
        // 다음 문양 계산 (순환)
        val nextIndex = (currentIndex + 1) % appliedUnits.size
        val nextOrdinal = appliedUnits[nextIndex]
        
        // 유닛 문양 변경
        val nextSymbol = CardSymbolType.values()[nextOrdinal]
        defenseUnit.setSymbolType(nextSymbol)
        
        // 변경 리스너 호출
        symbolChangeListener?.onSymbolChanged(nextSymbol)
    }

    /**
     * 문양 변경 리스너 설정
     */
    fun setSymbolChangeListener(listener: DefenseUnitSymbolChangeListener?) {
        this.symbolChangeListener = listener
    }

    // 테스트용: 강제로 다음 웨이브로 이동
    fun forceNextWave() {
        // 현재 웨이브의 모든 적 제거 (보스 포함)
        enemyManager.removeAllEnemies()
        
        // 다음 웨이브 시작
        startNextWave()
    }
} 