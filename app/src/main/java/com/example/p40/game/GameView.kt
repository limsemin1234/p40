package com.example.p40.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 게임 오버 콜백 인터페이스
 */
interface GameOverListener {
    fun onGameOver(resource: Int, waveCount: Int)
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
    private val gameStats = GameStats(gameConfig)
    private lateinit var gameRenderer: GameRenderer
    private lateinit var gameLogic: GameLogic
    
    // 게임 스레드
    private var gameThread: GameThread? = null
    private var isRunning = false
    private var paused = false
    
    // 콜백 리스너
    private var gameOverListener: GameOverListener? = null
    private var bossKillListener: BossKillListener? = null
    
    init {
        holder.addCallback(this)
        gameRenderer = GameRenderer(gameStats, gameConfig)
    }
    
    /**
     * 게임 로직 초기화 메서드
     * 필요할 때마다 호출하여 게임 로직이 초기화되도록 함
     */
    private fun initGameLogic(width: Float, height: Float) {
        gameLogic = GameLogic(gameStats, gameConfig, gameOverListener, bossKillListener)
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
     * 게임 스레드 클래스
     */
    inner class GameThread : Thread() {
        // 프레임 제한을 위한 설정
        private val targetFPS = gameConfig.FRAME_LIMIT
        private val targetFrameTime = 1000 / targetFPS
        
        override fun run() {
            var lastFrameTime = System.currentTimeMillis()
            
            while (isRunning) {
                val startTime = System.currentTimeMillis()
                
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        // 게임 로직 업데이트
                        gameLogic.update()
                        
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
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
                
                // FPS 제한 및 CPU 사용량 최적화
                val frameTime = System.currentTimeMillis() - startTime
                if (frameTime < targetFrameTime) {
                    try {
                        sleep(targetFrameTime - frameTime)
                    } catch (e: InterruptedException) {
                        // 무시
                    }
                }
                
                // 디버그 모드에서 FPS 계산
                val currentTime = System.currentTimeMillis()
                val elapsedFrameTime = currentTime - lastFrameTime
                lastFrameTime = currentTime
                
                // 디버그 모드에서 FPS 저장
                if (gameConfig.DEBUG_MODE && elapsedFrameTime > 0) {
                    val currentFPS = 1000 / elapsedFrameTime
                    gameRenderer.updateFPS(currentFPS)
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
    fun getUnitAttack(): Int = gameStats.getEffectiveAttackPower()
    fun getUnitAttackSpeed(): Float = gameStats.getEffectiveAttackSpeed()
    fun getActiveBuffs(): List<Buff> = gameStats.getActiveBuffs()
    fun getDefenseBuffs(): List<Buff> = gameStats.getDefenseBuffs()
    fun getEnemyNerfs(): List<Buff> = gameStats.getEnemyNerfs()
    fun getResource(): Int = gameStats.getResource()
    fun getWaveCount(): Int = gameStats.getWaveCount()
    fun getKillCount(): Int = gameStats.getKillCount()
    fun getTotalEnemiesInWave(): Int = gameStats.getTotalEnemiesInWave()
    fun getUnitAttackRange(): Float = gameStats.getUnitAttackRange()
    
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
    fun setGameOverListener(listener: GameOverListener) {
        this.gameOverListener = listener
        if (initializeIfNeeded()) {
            this.gameLogic = GameLogic(gameStats, gameConfig, listener, bossKillListener)
            gameLogic.initGame(width.toFloat(), height.toFloat())
        }
    }
    
    fun setBossKillListener(listener: BossKillListener) {
        this.bossKillListener = listener
        if (initializeIfNeeded()) {
            this.gameLogic = GameLogic(gameStats, gameConfig, gameOverListener, listener)
            gameLogic.initGame(width.toFloat(), height.toFloat())
        }
    }
    
    // 하트 플러시 스킬: 체력 전체 회복
    fun restoreFullHealth() {
        gameStats.restoreFullHealth()
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
    
    fun freezeAllEnemies(freeze: Boolean) {
        timeFrozen = freeze
    }
    
    // 다이아 플러시 스킬: 무적
    private var isInvincible = false
    
    fun setInvincible(invincible: Boolean) {
        isInvincible = invincible
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
} 