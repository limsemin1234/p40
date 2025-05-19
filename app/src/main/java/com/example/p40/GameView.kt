package com.example.p40

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 게임 오버 콜백 인터페이스
 */
interface GameOverListener {
    fun onGameOver(resource: Int, waveCount: Int)
}

/**
 * 디펜스 유닛 문양 변경 콜백 인터페이스
 */
interface DefenseUnitSymbolChangeListener {
    fun onSymbolChanged(symbolType: CardSymbolType)
}

/**
 * 게임 컴포넌트 인터페이스
 * 컴포넌트 간 의존성 감소를 위한 인터페이스 정의
 */
interface GameComponent {
    fun initialize()
    fun update()
    fun destroy()
}

/**
 * 게임 뷰 클래스 - 리팩토링 버전
 * 이전 버전에서 크게 3가지 책임으로 나누어 분리했습니다:
 * 1. GameRenderer: 렌더링(그리기) 관련 로직
 * 2. GameLogic: 게임 논리(적 생성, 충돌 등) 처리
 * 3. GameStats: 게임 상태 및 통계 관리
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    // 게임 설정
    private val gameConfig = GameConfig
    
    // 분리된 책임들
    private val gameStats by lazy {
        // GameStats를 싱글톤으로 초기화
        GameStats.initialize(gameConfig, context)
        GameStats.getInstance()
    }
    
    private lateinit var gameRenderer: GameRenderer
    private lateinit var gameLogic: GameLogic
    
    // 게임 스레드
    private var gameThread: GameThread? = null
    private var isRunning = false
    private var paused = false
    
    // 콜백 리스너
    private var gameOverListener: GameOverListener? = null
    private var bossKillListener: BossKillListener? = null
    private var symbolChangeListener: DefenseUnitSymbolChangeListener? = null
    private var levelClearListener: LevelClearListener? = null
    
    // 가시 데미지 업그레이드 레벨 및 비용
    private var thornDamageLevel = 0
    private var thornDamageCost = GameConfig.THORN_DAMAGE_UPGRADE_INITIAL_COST
    
    // 밀치기 업그레이드 레벨 및 비용
    private var pushDistanceLevel = 0
    private var pushDistanceCost = GameConfig.PUSH_DISTANCE_UPGRADE_INITIAL_COST
    
    init {
        holder.addCallback(this)
        gameRenderer = GameRenderer(gameStats, gameConfig)
    }
    
    /**
     * 게임 로직 초기화 메서드
     * 필요할 때마다 호출하여 게임 로직이 초기화되도록 함
     */
    private fun initGameLogic(width: Float, height: Float) {
        gameLogic = GameLogic(gameStats, gameConfig, gameOverListener, bossKillListener, levelClearListener, context)
        gameLogic.setGameView(this)
        gameLogic.initGame(width, height)
    }
    
    /**
     * 필요한 경우 게임 로직 초기화를 수행하는 메서드
     * @return 초기화 되었는지 여부
     */
    private fun initializeIfNeeded(): Boolean {
        if (!::gameLogic.isInitialized) {
            if (width > 0 && height > 0) {
                initGameLogic(width.toFloat(), height.toFloat())
                return true
            }
            return false
        }
        return true
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()
        
        // 게임 로직 초기화
        initGameLogic(screenWidth, screenHeight)
        
        // 게임 시작
        isRunning = true
        gameThread = GameThread()
        gameThread?.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 화면 크기가 변경되면 필요한 작업 수행
        if (::gameLogic.isInitialized) {
            // 필요 시 게임 로직에 새 크기 알림
        }
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // 스레드가 종료될 때까지 대기
            }
        }
    }
    
    /**
     * 게임 스레드 클래스 - 성능 최적화 적용
     */
    inner class GameThread : Thread() {
        private var lastFrameTime = System.currentTimeMillis()
        private var deltaTime = 0L
        
        // 성능 모니터링 변수
        private var frameCount = 0
        private var totalFrameTime = 0L
        private var lastFpsUpdate = System.currentTimeMillis()
        private var currentFps = 0
        
        // 프레임 제한 변수
        private val targetFps = gameConfig.FRAME_LIMIT
        private val targetFrameTime = 1000 / targetFps
        
        // 메모리 모니터링
        private var lastMemoryCheck = System.currentTimeMillis()
        private var memoryUsage = 0L
        
        override fun run() {
            while (isRunning) {
                val startTime = System.currentTimeMillis()
                
                // 로직 업데이트 및 렌더링
                updateAndRender()
                
                // 프레임 시간 계산
                val currentTime = System.currentTimeMillis()
                deltaTime = currentTime - lastFrameTime
                lastFrameTime = currentTime
                
                // 프레임 제한 (CPU 사용량 감소)
                val frameTime = System.currentTimeMillis() - startTime
                if (frameTime < targetFrameTime) {
                    try {
                        sleep(targetFrameTime - frameTime)
                    } catch (e: InterruptedException) {
                        // 인터럽트 예외 무시
                    }
                }
                
                // 성능 모니터링
                frameCount++
                totalFrameTime += frameTime
                
                // 1초마다 FPS 업데이트
                if (currentTime - lastFpsUpdate >= 1000) {
                    currentFps = frameCount
                    frameCount = 0
                    lastFpsUpdate = currentTime
                    totalFrameTime = 0
                    
                    // 메모리 사용량 업데이트 (5초마다)
                    if (currentTime - lastMemoryCheck >= 5000) {
                        val runtime = Runtime.getRuntime()
                        memoryUsage = runtime.totalMemory() - runtime.freeMemory()
                        lastMemoryCheck = currentTime
                        
                        if (gameConfig.DEBUG_MODE) {
                            // 객체 풀 통계 로깅
                            val enemyPoolStats = EnemyPool.getInstance().getPoolStats()
                            val missilePoolStats = MissilePool.getInstance().getPoolStats()
                            Log.d("GameView", "Enemy Pool: $enemyPoolStats")
                            Log.d("GameView", "Missile Pool: $missilePoolStats")
                            Log.d("GameView", "Memory Usage: ${memoryUsage / 1024 / 1024}MB")
                        }
                    }
                }
            }
        }
        
        private fun updateAndRender() {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        // 게임 로직 업데이트
                        if (::gameLogic.isInitialized) {
                            gameLogic.update()
                        }
                        
                        // 화면 그리기
                        canvas.drawColor(gameConfig.BACKGROUND_COLOR)
                        if (::gameLogic.isInitialized) {
                            // GameLogic에 render 메소드가 있으면 호출, 없으면 게임 렌더러 직접 호출
                            if (gameLogic.javaClass.declaredMethods.any { it.name == "render" }) {
                                gameLogic.render(canvas)
                            } else if (::gameRenderer.isInitialized) {
                                // 게임 화면 렌더링
                                gameRenderer.renderGame(
                                    canvas,
                                    gameLogic.getDefenseUnit(),
                                    gameLogic.getEnemies(),
                                    gameLogic.getMissiles(),
                                    gameLogic.getScreenWidth(),
                                    gameLogic.getScreenHeight(),
                                    gameLogic.isGamePaused(),
                                    gameLogic.isGameOver(),
                                    gameLogic.isShowingWaveMessage()
                                )
                            }
                        }
                        
                        // FPS 표시 (디버그 모드일 때만)
                        if (gameConfig.DEBUG_MODE) {
                            val debugPaint = Paint().apply {
                                color = Color.YELLOW
                                textSize = 30f
                            }
                            canvas.drawText("FPS: $currentFps", 20f, 40f, debugPaint)
                            canvas.drawText("Memory: ${memoryUsage / 1024 / 1024}MB", 20f, 80f, debugPaint)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // 다음 메서드들은 GameFragment에서 호출되는 공개 인터페이스입니다.
    
    /**
     * 포커 족보 효과 적용
     */
    fun applyPokerHandEffect(pokerHand: PokerHand) {
        gameStats.applyPokerHandEffect(pokerHand)
    }
    
    /**
     * 일시정지 처리
     */
    fun pause() {
        paused = true
        if (initializeIfNeeded()) {
            gameLogic.pause()
        }
    }
    
    /**
     * 게임 재개
     */
    fun resume() {
        paused = false
        if (initializeIfNeeded()) {
            gameLogic.resume()
        }
    }
    
    /**
     * 게임 재개 (별칭 메서드)
     * resume 메서드와 동일한 기능을 수행합니다.
     */
    fun resumeGame() {
        resume()
    }
    
    /**
     * 게임 리소스 완전 정리
     * 게임 종료 또는 Fragment가 파괴될 때 호출
     */
    fun cleanup() {
        // 게임 중지
        isRunning = false
        paused = true
        
        // 게임 스레드가 실행 중이면 종료 대기
        gameThread?.let { thread ->
            var retry = true
            while (retry) {
                try {
                    thread.join(1000) // 1초 대기 후 강제 종료
                    retry = false
                } catch (e: InterruptedException) {
                    // 무시
                }
            }
        }
        gameThread = null
        
        // 게임 렌더러 정리
        if (::gameRenderer.isInitialized) {
            gameRenderer.clearResources()
        }
        
        // 게임 로직이 초기화된 경우 정리
        if (::gameLogic.isInitialized) {
            // 현재 게임 상태를 UserManager에 저장하는 코드가 있다면 여기서 호출
        }
        
        // 콜백 참조 제거
        gameOverListener = null
        bossKillListener = null
    }
    
    /**
     * 게임 재시작
     */
    fun restartGame() {
        if (initializeIfNeeded()) {
            gameLogic.restartGame()
        }
    }
    
    /**
     * 자원 소모 메서드
     */
    fun useResource(amount: Int): Boolean {
        return gameStats.useResource(amount)
    }
    
    // 업그레이드 메서드들
    fun upgradeDamage(): Boolean = gameStats.upgradeDamage()
    fun upgradeAttackSpeed(): Boolean = gameStats.upgradeAttackSpeed()
    fun upgradeAttackRange(): Boolean {
        val result = gameStats.upgradeAttackRange()
        if (result && initializeIfNeeded()) {
            // 디펜스 유닛 공격 범위 업데이트
            gameLogic.getDefenseUnit().setAttackRange(gameStats.getUnitAttackRange())
        }
        return result
    }
    fun upgradeDefense(): Boolean = gameStats.upgradeDefense()
    
    // 게임 상태 접근자 메서드들 - 데이터 클래스를 활용한 통합 접근자
    
    /**
     * 유닛 스탯 정보를 한번에 반환
     */
    fun getUnitStats(): UnitStats = gameStats.getUnitStats()
    
    /**
     * 게임 진행 정보를 한번에 반환
     */
    fun getGameProgress(): GameProgress = gameStats.getGameProgress()
    
    /**
     * 데미지 업그레이드 정보 반환
     */
    fun getDamageUpgradeInfo(): UpgradeInfo = gameStats.getDamageUpgradeInfo()
    
    /**
     * 현재 보스 체력 반환
     */
    fun getCurrentBossHealth(): Int {
        return if (::gameLogic.isInitialized) {
            gameLogic.getCurrentBossHealth()
        } else {
            0
        }
    }
    
    /**
     * 공격 속도 업그레이드 정보 반환
     */
    fun getAttackSpeedUpgradeInfo(): UpgradeInfo = gameStats.getAttackSpeedUpgradeInfo()
    
    /**
     * 공격 범위 업그레이드 정보 반환
     */
    fun getAttackRangeUpgradeInfo(): UpgradeInfo = gameStats.getAttackRangeUpgradeInfo()
    
    /**
     * 방어력 업그레이드 정보 반환
     */
    fun getDefenseUpgradeInfo(): UpgradeInfo = gameStats.getDefenseUpgradeInfo()
    
    // 기존 개별 접근자들도 일단 유지 (호환성)
    fun getActivePokerHandInfo(): String = gameStats.getActivePokerHandInfo()
    fun getUnitHealth(): Int = gameStats.getUnitHealth()
    fun getUnitMaxHealth(): Int = gameStats.getUnitMaxHealth()
    
    // 디펜스 유닛의 문양 효과가 반영된 공격력 반환
    fun getUnitAttack(): Int {
        if (!::gameLogic.isInitialized) {
            return gameStats.getEffectiveAttackPower()
        }
        val defenseUnit = gameLogic.getDefenseUnit()
        // 기본 업그레이드된 공격력에 문양 효과 배율 적용
        return defenseUnit.applyDamageMultiplier(gameStats.getEffectiveAttackPower())
    }
    
    // 디펜스 유닛의 문양 효과가 반영된 공격속도 반환
    fun getUnitAttackSpeed(): Float {
        if (!::gameLogic.isInitialized) {
            return gameStats.getUnitAttackSpeed().toFloat()
        }
        val defenseUnit = gameLogic.getDefenseUnit()
        // 기본 업그레이드된 공격속도에 문양 효과 배율 적용
        return defenseUnit.applySpeedMultiplier(gameStats.getUnitAttackSpeed()).toFloat()
    }
    
    // 디펜스 유닛의 문양 효과가 반영된 공격범위 반환
    fun getUnitAttackRange(): Float {
        if (!::gameLogic.isInitialized) {
            return gameStats.getUnitAttackRange()
        }
        val defenseUnit = gameLogic.getDefenseUnit()
        // 기본 업그레이드된 공격범위에 문양 효과 배율 적용
        return defenseUnit.applyRangeMultiplier(gameStats.getUnitAttackRange())
    }
    
    fun getActiveBuffs(): List<Buff> = gameStats.getActiveBuffs()
    fun getDefenseBuffs(): List<Buff> = gameStats.getDefenseBuffs()
    fun getEnemyNerfs(): List<Buff> = gameStats.getEnemyNerfs()
    fun getResource(): Int = gameStats.getResource()
    fun getWaveCount(): Int = gameStats.getWaveCount()
    fun getKillCount(): Int = gameStats.getKillCount()
    fun getTotalEnemiesInWave(): Int = gameStats.getTotalEnemiesInWave()
    
    // 현재 업그레이드 비용 정보 반환
    fun getDamageCost(): Int = gameStats.getDamageCost()
    fun getAttackSpeedCost(): Int = gameStats.getAttackSpeedCost()
    fun getAttackRangeCost(): Int = gameStats.getAttackRangeCost()
    fun getDefenseCost(): Int = gameStats.getDefenseCost()
    
    // 현재 업그레이드 레벨 정보 반환
    fun getDamageLevel(): Int = gameStats.getDamageLevel()
    fun getAttackSpeedLevel(): Int = gameStats.getAttackSpeedLevel()
    fun getAttackRangeLevel(): Int = gameStats.getAttackRangeLevel()
    fun getDefenseLevel(): Int = gameStats.getDefenseLevel()
    
    // 리스너 설정
    fun setGameOverListener(listener: GameOverListener?) {
        this.gameOverListener = listener
        if (initializeIfNeeded()) {
            this.gameLogic = GameLogic(gameStats, gameConfig, listener, bossKillListener, levelClearListener, context)
            gameLogic.initGame(width.toFloat(), height.toFloat())
        }
    }
    
    fun setBossKillListener(listener: BossKillListener?) {
        this.bossKillListener = listener
        if (initializeIfNeeded()) {
            this.gameLogic = GameLogic(gameStats, gameConfig, gameOverListener, listener, levelClearListener, context)
            gameLogic.initGame(width.toFloat(), height.toFloat())
        }
    }
    
    /**
     * 레벨 클리어 리스너 설정
     */
    fun setLevelClearListener(listener: LevelClearListener?) {
        this.levelClearListener = listener
        if (initializeIfNeeded()) {
            this.gameLogic = GameLogic(gameStats, gameConfig, gameOverListener, bossKillListener, listener, context)
            gameLogic.initGame(width.toFloat(), height.toFloat())
        }
    }
    
    // 하트 플러시 스킬: 체력 전체 회복
    fun restoreFullHealth() {
        gameStats.restoreFullHealth()
    }
    
    // 하트 플러시 스킬: 특정 수치만큼 체력 회복
    fun healUnit(amount: Int) {
        gameStats.healUnit(amount)
    }
    
    // 스페이드 플러시 스킬: 화면 내 모든 적 제거 (보스 제외)
    fun removeAllEnemiesExceptBoss(): Int {
        return if (initializeIfNeeded()) {
            gameLogic.removeAllEnemiesExceptBoss()
        } else {
            0
        }
    }
    
    // 클로버 플러시 스킬: 시간 멈춤 (모든 적 멈춤)
    private var timeFrozen = false
    private var rangeBasedTimeFrozen = false
    
    fun freezeAllEnemies(freeze: Boolean) {
        timeFrozen = freeze
        
        // GameLogic에 시간 정지 상태 전달 - 초기화 여부 확인
        if (::gameLogic.isInitialized) {
            try {
                gameLogic.setTimeFrozen(freeze)
            } catch (e: Exception) {
                // 예외 발생 시 로그만 출력하고 계속 진행
                e.printStackTrace()
            }
        }
    }
    
    // 클로버 플러시 스킬: 공격 범위 내 적 시간 멈춤
    fun freezeEnemiesInRange(freeze: Boolean) {
        rangeBasedTimeFrozen = freeze
        
        // GameLogic에 범위 기반 시간 정지 상태 전달 - 초기화 여부 확인
        if (::gameLogic.isInitialized) {
            try {
                gameLogic.setRangeBasedTimeFrozen(freeze)
            } catch (e: Exception) {
                // 예외 발생 시 로그만 출력하고 계속 진행
                e.printStackTrace()
            }
        }
    }
    
    // 다이아 플러시 스킬: 무적
    private var isInvincible = false
    
    fun setInvincible(invincible: Boolean) {
        isInvincible = invincible
        
        // GameLogic에도 무적 상태 전달 - 초기화 여부 확인
        if (::gameLogic.isInitialized) {
            try {
                gameLogic.setInvincible(invincible)
            } catch (e: Exception) {
                // 예외 발생 시 로그만 출력하고 계속 진행
                e.printStackTrace()
            }
        }
    }
    
    // 데미지 메서드 수정 (무적 상태 체크 추가)
    fun takeDamage(damage: Int): Boolean {
        // 무적 상태일 경우 데미지를 받지 않음
        if (isInvincible) return false
        
        // gameStats를 통해 데미지 적용
        val gameOver = gameStats.applyDamageToUnit(damage)
        
        // 체력이 0 이하면 게임 오버
        if (gameOver) {
            gameOverListener?.onGameOver(gameStats.getResource(), gameStats.getWaveCount())
            return true
        }
        return false
    }
    
    // 업데이트 메서드 수정 (시간 멈춤 상태 처리 추가)
    fun update() {
        // 게임이 일시정지 상태이면 업데이트 안함
        if (paused) return
        
        // 시간 멈춤 상태 정보 전달 (GameLogic에서 처리하도록 변경)
        if (::gameLogic.isInitialized) {
            gameLogic.setTimeFrozen(timeFrozen)
            gameLogic.update()
        }
    }
    
    // 버프 매니저 접근자
    fun getBuffManager(): BuffManager {
        return gameStats.getBuffManager()
    }
    
    /**
     * DefenseUnit을 반환합니다.
     * 디펜스 유닛의 문양 정보와 같은 상태를 조회하기 위해 사용됩니다.
     */
    fun getDefenseUnit(): DefenseUnit? {
        return if (::gameLogic.isInitialized) {
            gameLogic.getDefenseUnit()
        } else {
            null
        }
    }
    
    /**
     * StatsManager에서 가져온 유닛 스탯을 설정하는 메서드
     * @param health 체력
     * @param attack 공격력
     * @param attackSpeed 공격 속도 (밀리초 단위의 쿨다운)
     * @param range 공격 범위
     */
    fun setUnitStats(health: Int, attack: Int, attackSpeed: Long, range: Float) {
        gameStats.setUnitHealth(health)
        gameStats.setUnitMaxHealth(health)
        gameStats.setUnitAttackPower(attack)
        gameStats.setUnitAttackSpeed(attackSpeed)
        gameStats.setUnitAttackRange(range)
        
        // 이미 초기화된 경우 디펜스 유닛에도 적용
        if (::gameLogic.isInitialized) {
            gameLogic.getDefenseUnit().setAttackRange(range)
            gameLogic.getDefenseUnit().setAttackCooldown(attackSpeed)
        }
    }
    
    /**
     * 게임을 초기 상태로 리셋합니다.
     * @param config 초기화에 사용할 GameConfig 객체
     */
    fun resetGame(config: GameConfig) {
        // 게임 현재 상태 초기화
        paused = false
        isInvincible = false
        timeFrozen = false
        rangeBasedTimeFrozen = false
        
        // GameStats 초기화
        gameStats.reset()
        
        // GameLogic 새로 생성 및 초기화
        gameLogic = GameLogic(gameStats, config, gameOverListener, bossKillListener, levelClearListener, context)
        
        // GameLogic에 GameView 참조 설정 (중요: 순서 변경)
        gameLogic.setGameView(this)
        
        // 이제 GameView 참조가 설정된 후 initGame 호출
        gameLogic.initGame(width.toFloat(), height.toFloat())
    }
    
    /**
     * 문양 변경 리스너 설정
     */
    fun setSymbolChangeListener(listener: DefenseUnitSymbolChangeListener?) {
        this.symbolChangeListener = listener
        if (::gameLogic.isInitialized) {
            gameLogic.setSymbolChangeListener(listener)
        }
    }
    
    /**
     * 터치 이벤트 처리
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && initializeIfNeeded()) {
            val touchX = event.x
            val touchY = event.y
            
            // 디펜스 유닛의 위치와 크기
            val defenseUnit = gameLogic.getDefenseUnit()
            val unitPos = defenseUnit.getPosition()
            val unitSize = gameConfig.DEFENSE_UNIT_SIZE
            
            // 터치 지점과 디펜스 유닛 중심 사이의 거리 계산
            val distance = sqrt((touchX - unitPos.x).pow(2) + (touchY - unitPos.y).pow(2))
            
            // 디펜스 유닛 영역을 터치했는지 확인 (크기의 1.5배 영역까지 인식)
            if (distance <= unitSize * 1.5f) {
                // 새로운 방식으로 문양 변경 - 유저가 적용한 유닛 간 순환
                gameLogic.changeDefenseUnitSymbol()
                
                // 이벤트 소비
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * 카드 터치 확인 및 처리
     * @return 카드 영역 터치 여부
     */
    private fun checkAndHandleCardTouch(touchX: Float, touchY: Float): Boolean {
        // 카드를 표시하는 UI가 있다면 여기서 처리
        // 현재는 기본 구현으로 false 반환
        return false
    }

    // 테스트용: 강제로 다음 웨이브로 이동
    fun forceNextWave() {
        if (::gameLogic.isInitialized) {
            gameLogic.forceNextWave()
        }
    }

    /**
     * 가시 데미지 레벨 가져오기
     */
    fun getThornDamageLevel(): Int = thornDamageLevel
    
    /**
     * 밀치기 레벨 가져오기
     */
    fun getPushDistanceLevel(): Int = pushDistanceLevel
    
    /**
     * 가시 데미지 비용 가져오기
     */
    fun getThornDamageCost(): Int = thornDamageCost
    
    /**
     * 밀치기 비용 가져오기
     */
    fun getPushDistanceCost(): Int = pushDistanceCost
    
    /**
     * 현재 가시 데미지 계산
     */
    fun getCurrentThornDamage(): Int {
        return GameConfig.DEFENSE_UNIT_THORN_DAMAGE + thornDamageLevel * GameConfig.THORN_DAMAGE_UPGRADE_VALUE
    }
    
    /**
     * 현재 밀치기 거리 계산 (유닛 크기 배수로)
     */
    fun getCurrentPushDistance(): Float {
        return GameConfig.DEFENSE_UNIT_PUSH_DISTANCE + pushDistanceLevel * GameConfig.PUSH_DISTANCE_UPGRADE_VALUE
    }
    
    /**
     * 가시 데미지 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradeThornDamage(): Boolean {
        // 최대 레벨 체크
        if (thornDamageLevel >= GameConfig.THORN_DAMAGE_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        // 자원 부족 체크
        if (gameStats.getResource() < thornDamageCost) {
            return false
        }
        
        // 자원 차감
        gameStats.useResource(thornDamageCost)
        
        // 레벨 증가
        thornDamageLevel++
        
        // 비용 증가
        thornDamageCost = GameConfig.THORN_DAMAGE_UPGRADE_INITIAL_COST + 
                          thornDamageLevel * GameConfig.THORN_DAMAGE_UPGRADE_COST_INCREASE
        
        return true
    }
    
    /**
     * 밀치기 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradePushDistance(): Boolean {
        // 최대 레벨 체크
        if (pushDistanceLevel >= GameConfig.PUSH_DISTANCE_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        // 자원 부족 체크
        if (gameStats.getResource() < pushDistanceCost) {
            return false
        }
        
        // 자원 차감
        gameStats.useResource(pushDistanceCost)
        
        // 레벨 증가
        pushDistanceLevel++
        
        // 비용 증가
        pushDistanceCost = GameConfig.PUSH_DISTANCE_UPGRADE_INITIAL_COST + 
                           pushDistanceLevel * GameConfig.PUSH_DISTANCE_UPGRADE_COST_INCREASE
        
        return true
    }
} 