package com.example.p40

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * 게임 화면을 담당하는 Fragment
 * 리팩토링: 책임 분리를 위해 여러 매니저 클래스로 기능 위임
 */
class GameFragment : Fragment(R.layout.fragment_game), GameOverListener, PokerCardManager.PokerCardListener,
    DefenseUnitSymbolChangeListener, LevelClearListener {

    private lateinit var gameView: GameView
    private var isPaused = false
    
    // GameConfig 추가
    private lateinit var gameConfig: GameConfig
    
    // 현재 열려있는 패널 추적
    private var currentOpenPanel: LinearLayout? = null
    
    // 현재 웨이브 정보
    private var currentWave = 1
    
    // UserManager 추가
    private lateinit var userManager: UserManager
    
    // 버프 정보 UI
    private lateinit var tvBuffList: TextView
    
    // 메시지 관리자 추가
    private lateinit var messageManager: MessageManager
    
    // 게임에서 획득한 코인 추적
    private var earnedCoins = 0
    
    // UI 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            // GameUIHelper의 메서드 호출
            gameUIHelper.updateGameInfoUI()
            gameUIHelper.updateBuffUI()
            gameUIHelper.updateUnitStatsUI()
            gameUIHelper.updateEnemyStatsUI()
            gameUIHelper.updateBossStatsUI()
            gameUIHelper.updateCoinUI()
            handler.postDelayed(this, 500) // 500ms마다 업데이트
        }
    }

    // 포커 카드 관련 변수 추가
    private lateinit var pokerCardManager: PokerCardManager
    
    // 플러시 스킬 매니저 추가
    private lateinit var flushSkillManager: FlushSkillManager

    // StatsManager 추가
    private lateinit var statsManager: StatsManager
    
    // 리팩토링: 새로운 매니저 클래스들 추가
    private lateinit var gameUIHelper: GameUIHelper
    private lateinit var upgradeManager: UpgradeManager
    private lateinit var gameDialogManager: GameDialogManager

    // 웨이브 변경을 감지하기 위한 Runnable
    private var waveCompletionRunnable: Runnable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // GameConfig 초기화
        gameConfig = GameConfig.getDefaultConfig()
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 메시지 관리자 초기화는 onStart로 이동
        messageManager = MessageManager.getInstance()
        
        // StatsManager 초기화
        statsManager = StatsManager.getInstance(requireContext())
        
        // 게임 시작 시 현재 코인 값 저장 (게임 중 획득한 코인 계산용)
        statsManager.setInitialGameCoins(userManager.getCoin())
        
        // 게임 시작 시 획득 코인 초기화
        earnedCoins = 0
        
        // 게임 레벨 정보 가져오기
        arguments?.let { args ->
            val levelId = args.getInt("levelId", 1)
            val totalWaves = args.getInt("totalWaves", 10)
            
            // 웨이브 수만 설정 (난이도 설정은 제거됨)
            GameConfig.setTotalWaves(totalWaves)
        }
        
        // 게임 뷰 초기화
        gameView = view.findViewById(R.id.gameView)
        
        // 게임을 항상 1웨이브부터 시작하도록 초기화
        gameView.resetGame(gameConfig)
        
        // 게임 오버 리스너 설정
        gameView.setGameOverListener(this)
        
        // 문양 변경 리스너 설정
        gameView.setSymbolChangeListener(this)
        
        // 보스 처치 리스너 설정
        gameView.setBossKillListener(object : BossKillListener {
            override fun onBossKilled(wave: Int) {
                // UI 스레드에서 실행하기 위해 Handler 사용
                Handler(Looper.getMainLooper()).post {
                    // 인자로 받은 웨이브에 따른 보스 처치 코인 보상 설정
                    val coinReward = EnemyConfig.getBossKillCoinReward(wave)
                    
                    // 웨이브에 맞는 코인 획득
                    userManager.addCoin(coinReward)
                    // 획득한 코인 누적
                    earnedCoins += coinReward
                    gameUIHelper.setEarnedCoins(earnedCoins)
                    
                    // 메시지 표시
                    messageManager.showSuccess("보스 처치! +${coinReward} 코인")
                }
            }
        })
        
        // 레벨 클리어 리스너 설정
        gameView.setLevelClearListener(this)
        
        // 파티클 효과를 위한 애니메이션 설정
        setupDefenseUnitAnimation(view)
        
        // 매니저 클래스 초기화
        initializeManagers(view)
        
        // 버프 UI 초기화 (기존 코드 유지하면서 새 클래스로 점진적 이전)
        initBuffUI(view)
        
        // 탭 버튼 초기화
        setupStatTabs(view)
        
        // 게임 메뉴 초기화
        setupGameMenu(view)
        
        // 업그레이드 버튼 설정
        upgradeManager.setupAttackUpgradeButtons()
        upgradeManager.setupDefenseUpgradeButtons()
        
        // 카드 버튼 설정 - 패널에서 직접 포커 카드 기능 처리
        setupCardButtons(view)
        
        // 웨이브 완료 리스너 설정
        setupWaveCompletionListener()
        
        // 패널 초기화
        setupPanels(view)

        // UI 업데이트 시작
        gameUIHelper.startUiUpdates()
        
        // 업그레이드 버튼 텍스트 초기화 (UI 업데이트 후에 실행)
        handler.post {
            upgradeManager.updateUpgradeButtonsText()
        }
        
        // 게임 뷰에 StatsManager의 스탯 적용
        applyStatsToGame()

        // 테스트 버튼 설정
        setupTestButton(view)
        
        // 게임 화면 버튼 스타일 적용
        applyGameButtonStyles(view)
    }

    /**
     * 매니저 클래스들 초기화
     */
    private fun initializeManagers(view: View) {
        // GameUIHelper 초기화
        gameUIHelper = GameUIHelper(
            requireContext(),
            gameConfig,
            userManager,
            statsManager,
            messageManager
        )
        
        // GameView 설정
        gameUIHelper.setGameView(gameView)
        
        // UpgradeManager 초기화
        upgradeManager = UpgradeManager(
            requireContext(),
            gameView,
            messageManager,
            view
        )
        
        // GameDialogManager 초기화
        gameDialogManager = GameDialogManager(
            requireContext(),
            gameView,
            messageManager,
            userManager,
            statsManager,
            viewLifecycleOwner,
            findNavController(),
            gameConfig
        )
        
        // 콜백 설정
        gameDialogManager.setCleanupResourcesCallback {
            cleanupGameResources()
        }
        
        gameDialogManager.setPokerHandAppliedCallback { pokerHand ->
            applyPokerHandEffect(pokerHand)
        }
        
        // 획득 코인 설정
        gameDialogManager.setEarnedCoins(earnedCoins)
        
        // UI 요소 초기화
        gameUIHelper.initUIElements(view)
    }
    
    /**
     * 패널 설정
     */
    private fun setupPanels(view: View) {
        val attackUpgradePanel = view.findViewById<LinearLayout>(R.id.attackUpgradePanel)
        val defenseUpgradePanel = view.findViewById<LinearLayout>(R.id.defenseUpgradePanel)
        val cardPanel = view.findViewById<LinearLayout>(R.id.cardPanel)
        
        // 공격업 버튼
        val attackUpButton = view.findViewById<Button>(R.id.attackUpButton)
        attackUpButton.setOnClickListener {
            gameUIHelper.togglePanel(attackUpgradePanel)
        }
        
        // 방어업 버튼
        val defenseUpButton = view.findViewById<Button>(R.id.defenseUpButton)
        defenseUpButton.setOnClickListener {
            gameUIHelper.togglePanel(defenseUpgradePanel)
        }
        
        // 카드 버튼
        val cardButton = view.findViewById<Button>(R.id.cardButton)
        cardButton.setOnClickListener {
            gameUIHelper.togglePanel(cardPanel)
        }
        
        // 플러시 스킬 매니저 초기화
        val flushSkillButtonContainer = view.findViewById<LinearLayout>(R.id.flushSkillButtonContainer)
        flushSkillManager = FlushSkillManager(
            requireContext(),
            gameView,
            flushSkillButtonContainer,
            messageManager
        )
        
        // GameUIHelper에 FlushSkillManager 설정
        gameUIHelper.setFlushSkillManager(flushSkillManager)
        
        // 플러시 스킬 버튼 컨테이너 초기 설정
        flushSkillButtonContainer.visibility = View.GONE
    }
    
    override fun onStart() {
        super.onStart()
        
        // 메시지 관리자 초기화
        // 액티비티가 완전히 준비된 후 초기화
        view?.let { messageManager.init(requireActivity().findViewById(android.R.id.content)) }
        
        // 저장된 덱 확인
        DeckBuilderFragment.loadDeckFromPrefs(requireContext())
    }
    
    override fun onStop() {
        super.onStop()
        
        // UI 업데이트 중지
        gameUIHelper.stopUiUpdates()
        
        // 게임 일시정지 (onPause에서 이미 처리되었을 수 있음)
        if (::gameView.isInitialized) {
            gameView.pause()
        }
        
        // UI 업데이트 중지 (핸들러 콜백 제거)
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 웨이브 완료 리스너 임시 중지
        waveCompletionRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 열려 있는 패널 정리
        currentOpenPanel = null
        
        // 게임 관련 리스너 정리 작업
        try {
            // 리스너 정리를 위한 순차적 정리 작업
            if (::gameView.isInitialized) {
                // 명시적으로 null 설정은 안 되지만 리스너 참조를 해제하기 위한 방법
                gameView.setGameOverListener(null)
                gameView.setBossKillListener(null)
                gameView.setLevelClearListener(null)
                gameView.setSymbolChangeListener(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 핸들러 콜백 제거
        handler.removeCallbacks(uiUpdateRunnable)
        waveCompletionRunnable?.let {
            handler.removeCallbacks(it)
        }
        
        // 매니저 클래스들의 뷰 참조 정리
        if (::gameUIHelper.isInitialized) {
            gameUIHelper.stopUiUpdates()
            gameUIHelper.clear()
        }
        
        if (::upgradeManager.isInitialized) {
            upgradeManager.clearReferences()
        }
        
        if (::pokerCardManager.isInitialized) {
            pokerCardManager.cancelPendingOperations()
            pokerCardManager.clearReferences()
        }
        
        if (::flushSkillManager.isInitialized) {
            flushSkillManager.resetAllSkills()
            flushSkillManager.clearReferences()
        }
    }
    
    // 버프 UI 초기화 (후에 GameUIHelper.initBuffUI 메서드로 점진적 이전)
    private fun initBuffUI(view: View) {
        // 버프 컨테이너 찾기
        val buffContainer = view.findViewById<LinearLayout>(R.id.buffContainer)
        // 버프 리스트 텍스트뷰 찾기
        tvBuffList = view.findViewById(R.id.tvBuffList)
        // 버프 리스트 텍스트뷰 초기 설정
        tvBuffList.visibility = View.VISIBLE
        tvBuffList.text = "활성화된 버프 없음"
    }
    
    // 스탯 탭 설정
    private fun setupStatTabs(view: View) {
        val myUnitTabButton = view.findViewById<TextView>(R.id.myUnitTabButton)
        val enemyUnitTabButton = view.findViewById<TextView>(R.id.enemyUnitTabButton)
        val bosUnitTabButton = view.findViewById<TextView>(R.id.bosUnitTabButton)
        val myUnitStatsContainer = view.findViewById<LinearLayout>(R.id.myUnitStatsContainer)
        val enemyStatsContainer = view.findViewById<LinearLayout>(R.id.enemyStatsContainer)
        val bossStatsContainer = view.findViewById<LinearLayout>(R.id.bossStatsContainer)
        
        // 초기 상태 설정 (내 유닛 정보 탭이 활성화)
        updateTabState(0, myUnitTabButton, enemyUnitTabButton, bosUnitTabButton, myUnitStatsContainer, enemyStatsContainer, bossStatsContainer)
        
        // 내 유닛 정보 탭 클릭 시
        myUnitTabButton.setOnClickListener {
            updateTabState(0, myUnitTabButton, enemyUnitTabButton, bosUnitTabButton, myUnitStatsContainer, enemyStatsContainer, bossStatsContainer)
        }
        
        // 적 유닛 정보 탭 클릭 시
        enemyUnitTabButton.setOnClickListener {
            updateTabState(1, myUnitTabButton, enemyUnitTabButton, bosUnitTabButton, myUnitStatsContainer, enemyStatsContainer, bossStatsContainer)
        }
        
        // 보스 유닛 정보 탭 클릭 시
        bosUnitTabButton.setOnClickListener {
            updateTabState(2, myUnitTabButton, enemyUnitTabButton, bosUnitTabButton, myUnitStatsContainer, enemyStatsContainer, bossStatsContainer)
        }
        
        // 탭 버튼에 클릭 애니메이션 적용
        ButtonAnimationUtils.applyButtonAnimationProperty(myUnitTabButton)
        ButtonAnimationUtils.applyButtonAnimationProperty(enemyUnitTabButton)
        ButtonAnimationUtils.applyButtonAnimationProperty(bosUnitTabButton)
    }
    
    // 탭 상태 업데이트 (3개 탭 지원)
    private fun updateTabState(
        selectedTab: Int, // 0: 내 유닛, 1: 적 유닛, 2: 보스 유닛
        myUnitTabButton: TextView,
        enemyUnitTabButton: TextView,
        bossUnitTabButton: TextView,
        myUnitStatsContainer: LinearLayout,
        enemyStatsContainer: LinearLayout,
        bossStatsContainer: LinearLayout
    ) {
        // 모든 탭 버튼 비활성화 스타일로 변경
        myUnitTabButton.setTextColor(Color.parseColor("#CCCCCC"))
        ButtonAnimationUtils.setButtonBackgroundDirectly(myUnitTabButton, "#252525")
        
        enemyUnitTabButton.setTextColor(Color.parseColor("#CCCCCC"))
        ButtonAnimationUtils.setButtonBackgroundDirectly(enemyUnitTabButton, "#252525")
        
        bossUnitTabButton.setTextColor(Color.parseColor("#CCCCCC"))
        ButtonAnimationUtils.setButtonBackgroundDirectly(bossUnitTabButton, "#252525")
        
        // 모든 컨테이너 숨기기
        myUnitStatsContainer.visibility = View.GONE
        enemyStatsContainer.visibility = View.GONE
        bossStatsContainer.visibility = View.GONE
        
        // 선택된 탭에 따라 활성화
        when (selectedTab) {
            0 -> { // 내 유닛 정보
                myUnitTabButton.setTextColor(Color.WHITE)
                ButtonAnimationUtils.setButtonBackgroundDirectly(myUnitTabButton, "#333333")
                myUnitStatsContainer.visibility = View.VISIBLE
            }
            1 -> { // 적 유닛 정보
                enemyUnitTabButton.setTextColor(Color.WHITE)
                ButtonAnimationUtils.setButtonBackgroundDirectly(enemyUnitTabButton, "#333333")
                enemyStatsContainer.visibility = View.VISIBLE
            }
            2 -> { // 보스 유닛 정보
                bossUnitTabButton.setTextColor(Color.WHITE)
                ButtonAnimationUtils.setButtonBackgroundDirectly(bossUnitTabButton, "#333333")
                bossStatsContainer.visibility = View.VISIBLE
            }
        }
    }
    
    // 웨이브 완료 리스너 설정
    private fun setupWaveCompletionListener() {
        // 이전 웨이브 정보 저장 변수
        var previousWave = 0
        
        // 기존 Runnable이 있으면 제거
        waveCompletionRunnable?.let {
            handler.removeCallbacks(it)
        }
        
        // 새로운 Runnable 생성
        waveCompletionRunnable = object : Runnable {
            override fun run() {
                if (!isAdded) return
                
                // 현재 웨이브 확인
                val currentWave = gameView.getWaveCount()
                
                // 웨이브가 변경되었고 테스트 모드가 활성화된 경우
                if (currentWave > previousWave && GameConfig.TEST_ENABLE_ALL_SKILLS) {
                    // 웨이브 시작 시 모든 스킬 활성화 (테스트용)
                    activateAllSkillsForTesting()
                    
                    // 메시지 표시
                    if (currentWave > 1) { // 게임 시작 시 첫 웨이브는 제외
                        messageManager.showInfo("웨이브 ${currentWave} 시작! 테스트 모드: 모든 스킬 활성화")
                    }
                }
                
                // 현재 웨이브 정보 저장
                previousWave = currentWave
                
                // 다음 확인 예약 (500ms 후)
                if (isAdded) {
                    handler.postDelayed(this, 500)
                }
            }
        }
        
        // 웨이브 체크 시작
        handler.post(waveCompletionRunnable!!)
    }
    
    // 테스트를 위해 모든 스킬 활성화
    private fun activateAllSkillsForTesting() {
        // 먼저 기존 스킬 모두 비활성화
        flushSkillManager.deactivateAllSkills()
        
        // 모든 스킬 활성화
        flushSkillManager.activateFlushSkill(CardSuit.HEART)
        flushSkillManager.activateFlushSkill(CardSuit.SPADE)
        flushSkillManager.activateFlushSkill(CardSuit.CLUB)
        flushSkillManager.activateFlushSkill(CardSuit.DIAMOND)
    }
    
    // 웨이브 완료 시 처리
    private fun onWaveCompleted(waveNumber: Int) {
        // 게임 일시 정지
        gameView.pause()
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 포커 카드 다이얼로그 표시
        gameDialogManager.showPokerCardsDialog(waveNumber)
    }
    
    // 포커 족보 효과 적용
    override fun applyPokerHandEffect(pokerHand: PokerHand) {
        // GameView에 포커 족보 효과 적용
        gameView.applyPokerHandEffect(pokerHand)
        
        // 버프 정보 업데이트
        gameUIHelper.updateBuffUI()
    }
    
    // 카드 버튼 설정 - 패널에서 직접 포커 카드 기능 처리
    private fun setupCardButtons(view: View) {
        // 포커 카드 매니저 초기화
        pokerCardManager = PokerCardManager(requireContext(), view, this)
        
        // 카드 버튼 - 이제 카드 패널 직접 열기
        val cardButton = view?.findViewById<Button>(R.id.cardButton)
        cardButton?.setOnClickListener {
            // 패널 토글 기능 유지
            gameUIHelper.togglePanel(view.findViewById(R.id.cardPanel))
        }
    }
    
    // PokerCardListener 인터페이스 구현
    override fun getResource(): Int {
        return gameView.getResource()
    }
    
    override fun useResource(amount: Int): Boolean {
        return gameView.useResource(amount)
    }
    
    // PokerCardListener 인터페이스 구현 (public 메서드로 유지)
    override fun updateGameInfoUI() {
        gameUIHelper.updateGameInfoUI()
    }
    
    // 게임 종료 처리
    fun exitGame() {
        // 게임 중지
        gameView.pause()
        isPaused = true
        
        // 코인 저장은 UserManager에서 자동 처리되므로 별도 호출 필요 없음
    }

    // 게임 일시정지 처리
    private fun pauseGame() {
        if (!isPaused) {
            isPaused = true
            gameView.pause()
            gameDialogManager.showPauseDialog(isPaused, uiUpdateRunnable, handler)
        }
    }
    
    // 게임 오버 처리
    override fun onGameOver(resource: Int, waveCount: Int) {
        if (!isAdded || requireActivity().isFinishing) return
        
        // 게임 중지
        gameView.pause()
        isPaused = true
        
        // UI 업데이트 중지
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 게임 오버 시 획득한 코인 정보 설정
        gameDialogManager.setEarnedCoins(earnedCoins)
        
        // 게임 오버 다이얼로그 표시
        gameDialogManager.onGameOver(resource, waveCount)
    }

    // setupGameMenu 함수 추가 (pauseButton과 exitButton 설정 코드)
    private fun setupGameMenu(view: View) {
        // 종료(일시정지) 버튼
        val exitButton = view.findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            // 게임 일시정지
            isPaused = true
            gameView.pause()
            handler.removeCallbacks(uiUpdateRunnable)
            
            // 일시정지 메뉴 보여주기
            gameDialogManager.showPauseDialog(isPaused, uiUpdateRunnable, handler)
        }
    }

    /**
     * StatsManager의 스탯을 게임에 적용하는 메서드
     */
    private fun applyStatsToGame() {
        // 디펜스 유닛 기본 스탯을 StatsManager의 값으로 설정
        val health = statsManager.getHealth()
        val attack = statsManager.getAttack()
        val attackSpeed = statsManager.getAttackSpeed()
        val range = statsManager.getRange()
        
        // 스탯 적용
        gameView.setUnitStats(
            health = health,
            attack = attack,
            attackSpeed = (1000 / attackSpeed).toLong(), // 공격속도는 쿨다운 값으로 변환 (초당 공격 횟수 -> 밀리초)
            range = range.toFloat()
        )
    }

    /**
     * 디펜스 유닛 애니메이션 설정
     */
    private fun setupDefenseUnitAnimation(view: View) {
        // 애니메이션은 GameRenderer 내부에서 처리되기 때문에
        // 여기서는 초기 애니메이션 설정만 수행합니다.
        
        // 나중에 필요한 경우 여기에 추가 애니메이션 초기화 코드를 추가할 수 있습니다.
        // 예: 회전 애니메이션 속도 조절, 특수 효과 설정 등
    }

    /**
     * DefenseUnitSymbolChangeListener 구현
     * 문양 변경 시 호출되는 콜백 메서드
     */
    override fun onSymbolChanged(symbolType: CardSymbolType) {
        // 문양 변경 후 UI 즉시 업데이트
        gameUIHelper.updateUnitStatsUI()
        
        // 업그레이드 버튼 상태 업데이트
        upgradeManager.updateUpgradeButtonsText()
        
        // 문양 타입에 따른 효과 메시지 표시
        when (symbolType) {
            CardSymbolType.SPADE -> {
                // GameConfig 기반 메시지 생성
                messageManager.showInfo("스페이드 문양: 기본 상태")
            }
            CardSymbolType.HEART -> {
                // GameConfig 기반 메시지 생성
                val damageEffect = (GameConfig.HEART_DAMAGE_MULTIPLIER * 100).toInt()
                messageManager.showInfo("하트 문양: 공격력 ${damageEffect}% 감소, 데미지 시 체력 ${GameConfig.HEART_HEAL_ON_DAMAGE} 회복")
            }
            CardSymbolType.DIAMOND -> {
                // GameConfig 기반 메시지 생성
                val speedEffect = (GameConfig.DIAMOND_SPEED_MULTIPLIER * 100).toInt()
                val rangeEffect = (GameConfig.DIAMOND_RANGE_MULTIPLIER * 100).toInt()
                messageManager.showInfo("다이아몬드 문양: 공격속도 ${speedEffect}%, 공격범위 ${rangeEffect}%")
            }
            CardSymbolType.CLUB -> {
                // GameConfig 기반 메시지 생성
                val speedEffect = (GameConfig.CLUB_SPEED_MULTIPLIER * 100).toInt()
                val rangeEffect = (GameConfig.CLUB_RANGE_MULTIPLIER * 100).toInt()
                messageManager.showInfo("클로버 문양: 공격범위 ${rangeEffect}%, 공격속도 ${speedEffect}%")
            }
        }
    }

    // 레벨 클리어 처리
    override fun onLevelCleared(wave: Int, score: Int) {
        if (!isAdded || requireActivity().isFinishing) return
        
        // UI 스레드에서 실행하기 위해 Handler 사용
        Handler(Looper.getMainLooper()).post {
            gameDialogManager.showLevelClearedDialog(wave, score)
        }
    }

    // 게임 리소스 정리 메서드
    private fun cleanupGameResources() {
        // 게임 일시정지
        gameView.pause()
        
        // UI 업데이트 중지
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 포커 카드 매니저 정리
        if (::pokerCardManager.isInitialized) {
            pokerCardManager.cancelPendingOperations()
        }
        
        // 플러시 스킬 매니저 정리
        if (::flushSkillManager.isInitialized) {
            flushSkillManager.resetAllSkills()
        }
        
        // GameUIHelper 정리
        if (::gameUIHelper.isInitialized) {
            gameUIHelper.stopUiUpdates()
            gameUIHelper.clear()
        }
        
        // 메시지 매니저 정리
        if (::messageManager.isInitialized) {
            messageManager.clear()
        }
        
        // 게임뷰 정리
        if (::gameView.isInitialized) {
            gameView.cleanup()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // 게임 재개
        if (::gameView.isInitialized) {
            gameView.resume()
        }
        
        // 웨이브 완료 리스너 재개
        if (::gameView.isInitialized && waveCompletionRunnable != null) {
            handler.post(waveCompletionRunnable!!)
        }
    }
    
    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 모든 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null)
        
        // 웨이브 완료 리스너 정리
        waveCompletionRunnable = null
        
        // 게임 리소스 정리
        cleanupGameResources()
    }

    private fun setupTestButton(view: View) {
        // 테스트 버튼 찾기
        val testButton = view.findViewById<Button>(R.id.testNextWaveButton)
        
        // 디버그 모드일 때만 버튼 표시
        if (GameConfig.DEBUG_MODE) {
            testButton.visibility = View.VISIBLE
            
            // 클릭 리스너 설정
            testButton.setOnClickListener {
                // 현재 웨이브의 모든 적 제거
                gameView.removeAllEnemiesExceptBoss()
                
                // 보스도 제거하고 다음 웨이브로 진행
                gameView.forceNextWave()
                
                // 메시지 표시
                messageManager.showInfo("테스트: 다음 웨이브로 이동")
            }
        }
    }

    /**
     * 게임 화면의 버튼들에 스타일 적용
     */
    private fun applyGameButtonStyles(view: View) {
        try {
            // 공격력 업그레이드 버튼 (파란색)
            view.findViewById<Button>(R.id.btnUpgradeDamage)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#1A237E", // 배경색 (짙은 파란색)
                    "#64B5F6"  // 테두리 (밝은 파란색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            view.findViewById<Button>(R.id.btnUpgradeAttackSpeed)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#1A237E", // 배경색 (짙은 파란색)
                    "#64B5F6"  // 테두리 (밝은 파란색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            view.findViewById<Button>(R.id.btnUpgradeAttackRange)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#1A237E", // 배경색 (짙은 파란색)
                    "#64B5F6"  // 테두리 (밝은 파란색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            // 방어력 업그레이드 버튼 (녹색)
            view.findViewById<Button>(R.id.defenseUpgrade1)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#103C10", // 배경색 (짙은 녹색)
                    "#4CAF50"  // 테두리 (밝은 녹색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            view.findViewById<Button>(R.id.defenseUpgrade2)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#103C10", // 배경색 (짙은 녹색)
                    "#4CAF50"  // 테두리 (밝은 녹색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            view.findViewById<Button>(R.id.defenseUpgrade3)?.let { button ->
                ButtonAnimationUtils.setButtonBackgroundDirectly(
                    button, 
                    "#103C10", // 배경색 (짙은 녹색)
                    "#4CAF50"  // 테두리 (밝은 녹색)
                )
                ButtonAnimationUtils.applyButtonAnimationProperty(button)
            }
            
            // 하단 메인 버튼들
            applyButtonStyle(view, R.id.attackUpButton, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.defenseUpButton, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.cardButton, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.exitButton, R.drawable.btn_game_danger)
            applyButtonStyle(view, R.id.testNextWaveButton, R.drawable.btn_game_secondary)
            
            // 포커 카드 패널 버튼들
            applyButtonStyle(view, R.id.btnDrawPokerCards, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.btnAddCard, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.btnReplaceCards, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.btnReplaceAllCards, R.drawable.btn_game_secondary)
            applyButtonStyle(view, R.id.btnConfirmHand, R.drawable.btn_game_secondary)
            
            // 카드 가이드 버튼 애니메이션
            view.findViewById<ImageButton>(R.id.btnPokerGuide)?.let {
                ButtonAnimationUtils.applyButtonAnimationProperty(it)
            }
            
            // 카드뷰들에 애니메이션 적용 (Property 애니메이터 사용)
            arrayOf(R.id.cardView1, R.id.cardView2, R.id.cardView3, 
                    R.id.cardView4, R.id.cardView5, R.id.cardView6, 
                    R.id.cardView7).forEach { cardId ->
                view.findViewById<View>(cardId)?.let {
                    ButtonAnimationUtils.applyButtonAnimationProperty(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 버튼에 스타일과 애니메이션을 적용하는 헬퍼 메서드
     */
    private fun applyButtonStyle(view: View, buttonId: Int, backgroundResId: Int) {
        view.findViewById<Button>(buttonId)?.let { button ->
            // 배경 색상 직접 설정 (테마 스타일 우회)
            when (backgroundResId) {
                R.drawable.btn_game_primary -> {
                    // 골드/황금색 버튼
                    ButtonAnimationUtils.setButtonBackgroundDirectly(
                        button, 
                        "#DAA520", // 배경색 (어두운 금색)
                        "#FFD700"  // 테두리 (밝은 금색)
                    )
                }
                R.drawable.btn_game_secondary -> {
                    // 파란색 버튼
                    ButtonAnimationUtils.setButtonBackgroundDirectly(
                        button, 
                        "#1976D2", // 배경색 (어두운 파란색)
                        "#64B5F6"  // 테두리 (밝은 파란색)
                    )
                }
                R.drawable.btn_game_danger -> {
                    // 빨간색 버튼
                    ButtonAnimationUtils.setButtonBackgroundDirectly(
                        button, 
                        "#D32F2F", // 배경색 (어두운 빨간색)  
                        "#EF5350"  // 테두리 (밝은 빨간색)
                    )
                }
                else -> {
                    // 기본 스타일 (보라색)
                    ButtonAnimationUtils.setButtonBackgroundDirectly(
                        button, 
                        "#3F51B5", // 배경색 (보라색) 
                        "#90CAF9"  // 테두리 (밝은 파란색)
                    )
                }
            }
            
            // 프로퍼티 애니메이션 적용 (setOnTouchListener를 사용하므로 기존 클릭은 전파됨)
            ButtonAnimationUtils.applyButtonAnimationProperty(button)
        }
    }
}

