package com.example.p40.game

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavController
import com.example.p40.R
import com.example.p40.UserManager
import com.example.p40.game.PokerHand

/**
 * GameFragment의 UI 컨트롤 로직을 담당하는 컨트롤러 클래스
 * 버튼 설정, 패널 토글, 다이얼로그 표시 등의 역할을 담당합니다.
 */
class GameUIController(
    private val context: Context,
    private val gameViewModel: GameViewModel,
    private val messageManager: MessageManager,
    private val gameView: GameView,
    private val rootView: View
) {
    // 현재 열려있는 패널 추적
    private var currentOpenPanel: LinearLayout? = null
    
    // 플러시 스킬 매니저
    private lateinit var flushSkillManager: FlushSkillManager
    
    /**
     * UI 초기화
     */
    fun initialize() {
        // 기본 UI 설정
        setupStatTabs()
        setupGameMenu()
        setupUpgradeButtons()
        setupCardButtons()
        
        // 플러시 스킬 매니저 초기화
        initFlushSkillManager()
    }
    
    /**
     * 패널 토글 (열기/닫기)
     */
    fun togglePanel(panel: LinearLayout) {
        // 이미 열려있는 다른 패널이 있으면 닫기
        if (currentOpenPanel != null && currentOpenPanel != panel) {
            closePanel(currentOpenPanel!!)
        }
        
        // 선택한 패널 토글
        if (panel.visibility == View.VISIBLE) {
            closePanel(panel)
        } else {
            // 패널을 열기 전에 최신 비용으로 업그레이드 버튼 텍스트 업데이트
            updateUpgradeButtonsText()
            openPanel(panel)
        }
    }
    
    /**
     * 패널 열기
     */
    private fun openPanel(panel: LinearLayout) {
        panel.visibility = View.VISIBLE
        
        // 화면 높이의 일정 부분을 시작점으로 사용
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val startPosition = screenHeight * 0.05f  // 화면 높이의 5%만큼 아래에서 시작
        
        panel.translationY = startPosition
        
        val animator = ObjectAnimator.ofFloat(panel, "translationY", 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        currentOpenPanel = panel
    }
    
    /**
     * 패널 닫기
     */
    private fun closePanel(panel: LinearLayout) {
        // 화면 높이의 일정 부분을 종료점으로 사용
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val endPosition = screenHeight * 0.05f  // 화면 높이의 5%만큼 아래로 이동
        
        val animator = ObjectAnimator.ofFloat(panel, "translationY", endPosition)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        // 애니메이션 종료 후 패널 숨기기
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                panel.visibility = View.GONE
            }
        })
        
        if (currentOpenPanel == panel) {
            currentOpenPanel = null
        }
    }
    
    /**
     * 스탯 탭 설정
     */
    private fun setupStatTabs() {
        val myUnitTabButton = rootView.findViewById<TextView>(R.id.myUnitTabButton)
        val enemyUnitTabButton = rootView.findViewById<TextView>(R.id.enemyUnitTabButton)
        val bosUnitTabButton = rootView.findViewById<TextView>(R.id.bosUnitTabButton)
        val myUnitStatsContainer = rootView.findViewById<LinearLayout>(R.id.myUnitStatsContainer)
        val enemyStatsContainer = rootView.findViewById<LinearLayout>(R.id.enemyStatsContainer)
        val bossStatsContainer = rootView.findViewById<LinearLayout>(R.id.bossStatsContainer)
        
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
    }
    
    /**
     * 탭 상태 업데이트
     */
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
        myUnitTabButton.setTextColor(Color.parseColor("#808080"))
        enemyUnitTabButton.setTextColor(Color.parseColor("#808080"))
        bossUnitTabButton.setTextColor(Color.parseColor("#808080"))
        
        // 모든 컨테이너 숨기기
        myUnitStatsContainer.visibility = View.GONE
        enemyStatsContainer.visibility = View.GONE
        bossStatsContainer.visibility = View.GONE
        
        // 선택된 탭에 따라 활성화
        when (selectedTab) {
            0 -> { // 내 유닛 정보
                myUnitTabButton.setTextColor(Color.WHITE)
                myUnitStatsContainer.visibility = View.VISIBLE
            }
            1 -> { // 적 유닛 정보
                enemyUnitTabButton.setTextColor(Color.WHITE)
                enemyStatsContainer.visibility = View.VISIBLE
            }
            2 -> { // 보스 유닛 정보
                bossUnitTabButton.setTextColor(Color.WHITE)
                bossStatsContainer.visibility = View.VISIBLE
            }
        }
    }
    
    /**
     * 게임 메뉴 설정
     */
    private fun setupGameMenu() {
        // 종료(일시정지) 버튼
        val exitButton = rootView.findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            // 게임 일시정지
            gameViewModel.setPaused(true)
            gameView.pause()
            
            // 일시정지 메뉴 보여주기
            showPauseDialog()
        }
    }
    
    /**
     * 업그레이드 버튼 설정
     */
    private fun setupUpgradeButtons() {
        // 패널 초기화
        val attackUpgradePanel = rootView.findViewById<LinearLayout>(R.id.attackUpgradePanel)
        val defenseUpgradePanel = rootView.findViewById<LinearLayout>(R.id.defenseUpgradePanel)
        val cardPanel = rootView.findViewById<LinearLayout>(R.id.cardPanel)
        
        // 공격업 버튼
        val attackUpButton = rootView.findViewById<Button>(R.id.attackUpButton)
        attackUpButton.setOnClickListener {
            togglePanel(attackUpgradePanel)
        }
        
        // 방어업 버튼
        val defenseUpButton = rootView.findViewById<Button>(R.id.defenseUpButton)
        defenseUpButton.setOnClickListener {
            togglePanel(defenseUpgradePanel)
        }
        
        // 카드 버튼
        val cardButton = rootView.findViewById<Button>(R.id.cardButton)
        cardButton.setOnClickListener {
            togglePanel(cardPanel)
        }
        
        setupAttackUpgradeButtons()
        setupDefenseUpgradeButtons()
    }
    
    /**
     * 공격 업그레이드 버튼 설정
     */
    private fun setupAttackUpgradeButtons() {
        val btnUpgradeDamage = rootView.findViewById<Button>(R.id.btnUpgradeDamage)
        val btnUpgradeAttackSpeed = rootView.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
        val btnUpgradeAttackRange = rootView.findViewById<Button>(R.id.btnUpgradeAttackRange)
        
        // 데미지 업그레이드 버튼
        btnUpgradeDamage.setOnClickListener {
            val cost = gameView.getDamageCost()
            if (gameView.upgradeDamage()) {
                // 업그레이드 성공
                messageManager.showSuccess("데미지가 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
        
        // 공격속도 업그레이드 버튼
        btnUpgradeAttackSpeed.setOnClickListener {
            val cost = gameView.getAttackSpeedCost()
            if (gameView.upgradeAttackSpeed()) {
                // 업그레이드 성공
                messageManager.showSuccess("공격 속도가 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
        
        // 공격범위 업그레이드 버튼
        btnUpgradeAttackRange.setOnClickListener {
            val cost = gameView.getAttackRangeCost()
            if (gameView.upgradeAttackRange()) {
                // 업그레이드 성공
                messageManager.showSuccess("공격 범위가 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
    }
    
    /**
     * 방어 업그레이드 버튼 설정
     */
    private fun setupDefenseUpgradeButtons() {
        val defenseUpgrade1 = rootView.findViewById<Button>(R.id.defenseUpgrade1)
        val defenseUpgrade2 = rootView.findViewById<Button>(R.id.defenseUpgrade2)
        val defenseUpgrade3 = rootView.findViewById<Button>(R.id.defenseUpgrade3)
        
        defenseUpgrade1.setOnClickListener {
            val cost = gameView.getDefenseCost()
            if (gameView.upgradeDefense()) {
                // 업그레이드 성공
                messageManager.showSuccess("체력이 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
        
        // 다른 버튼들은 아직 구현하지 않음
        defenseUpgrade2.setOnClickListener {
            // 준비 중인 기능
            messageManager.showInfo("준비 중인 기능입니다.")
        }
        
        defenseUpgrade3.setOnClickListener {
            // 준비 중인 기능
            messageManager.showInfo("준비 중인 기능입니다.")
        }
    }
    
    /**
     * 카드 버튼 설정
     */
    private fun setupCardButtons() {
        // 카드 버튼 - 이제 카드 패널 직접 열기
        val cardButton = rootView.findViewById<Button>(R.id.cardButton)
        cardButton.setOnClickListener {
            // 패널 토글 기능 유지
            togglePanel(rootView.findViewById(R.id.cardPanel))
        }
    }
    
    /**
     * 플러시 스킬 매니저 초기화
     */
    private fun initFlushSkillManager() {
        val flushSkillButtonContainer = rootView.findViewById<LinearLayout>(R.id.flushSkillButtonContainer)
        flushSkillManager = FlushSkillManager(
            context,
            gameView,
            flushSkillButtonContainer,
            messageManager
        )
        
        // 플러시 스킬 버튼 컨테이너 초기 설정
        flushSkillButtonContainer.visibility = View.GONE
    }
    
    /**
     * 테스트를 위해 모든 스킬 활성화
     */
    fun activateAllSkillsForTesting() {
        // 먼저 기존 스킬 모두 비활성화
        flushSkillManager.deactivateAllSkills()
        
        // 모든 스킬 활성화
        flushSkillManager.activateFlushSkill(CardSuit.HEART)
        flushSkillManager.activateFlushSkill(CardSuit.SPADE)
        flushSkillManager.activateFlushSkill(CardSuit.CLUB)
        flushSkillManager.activateFlushSkill(CardSuit.DIAMOND)
    }
    
    /**
     * 업그레이드 버튼 텍스트 업데이트
     */
    fun updateUpgradeButtonsText() {
        // 데미지 업그레이드 버튼
        val btnUpgradeDamage = rootView.findViewById<Button>(R.id.btnUpgradeDamage)
        if (btnUpgradeDamage != null) {
            val damageLevel = gameView.getDamageLevel()
            if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
                btnUpgradeDamage.text = "데미지\n최대 레벨\n(Lv.${damageLevel}/${GameConfig.DAMAGE_UPGRADE_MAX_LEVEL})"
                btnUpgradeDamage.isEnabled = false
            } else {
                btnUpgradeDamage.text = "데미지 +${GameConfig.DAMAGE_UPGRADE_VALUE}\n💰 ${gameView.getDamageCost()} 자원\n(Lv.${damageLevel}/${GameConfig.DAMAGE_UPGRADE_MAX_LEVEL})"
                btnUpgradeDamage.isEnabled = true
            }
        }
        
        // 공격속도 업그레이드 버튼
        val btnUpgradeAttackSpeed = rootView.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
        if (btnUpgradeAttackSpeed != null) {
            val attackSpeedLevel = gameView.getAttackSpeedLevel()
            if (attackSpeedLevel >= GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
                btnUpgradeAttackSpeed.text = "공격속도\n최대 레벨\n(Lv.${attackSpeedLevel}/${GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL})"
                btnUpgradeAttackSpeed.isEnabled = false
            } else {
                // 현재 공격속도에 따라 다른 감소량 표시
                val currentAttackSpeed = gameView.getUnitAttackSpeed().toLong()
                val decreaseAmount = when {
                    currentAttackSpeed > GameConfig.ATTACK_SPEED_TIER1_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER1
                    currentAttackSpeed > GameConfig.ATTACK_SPEED_TIER2_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER2
                    currentAttackSpeed > GameConfig.ATTACK_SPEED_TIER3_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER3
                    else -> 0L
                }
                
                btnUpgradeAttackSpeed.text = "공격속도 -${decreaseAmount}ms\n💰 ${gameView.getAttackSpeedCost()} 자원\n(Lv.${attackSpeedLevel}/${GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL})"
                btnUpgradeAttackSpeed.isEnabled = true
            }
        }
        
        // 공격범위 업그레이드 버튼
        val btnUpgradeAttackRange = rootView.findViewById<Button>(R.id.btnUpgradeAttackRange)
        if (btnUpgradeAttackRange != null) {
            val attackRangeLevel = gameView.getAttackRangeLevel()
            if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
                btnUpgradeAttackRange.text = "공격범위\n최대 레벨\n(Lv.${attackRangeLevel}/${GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL})"
                btnUpgradeAttackRange.isEnabled = false
            } else {
                btnUpgradeAttackRange.text = "공격범위 +${GameConfig.ATTACK_RANGE_UPGRADE_VALUE.toInt()}\n💰 ${gameView.getAttackRangeCost()} 자원\n(Lv.${attackRangeLevel}/${GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL})"
                btnUpgradeAttackRange.isEnabled = true
            }
        }
        
        // 방어력 업그레이드 버튼
        val defenseUpgrade1 = rootView.findViewById<Button>(R.id.defenseUpgrade1)
        if (defenseUpgrade1 != null) {
            val defenseLevel = gameView.getDefenseLevel()
            if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
                defenseUpgrade1.text = "체력\n최대 레벨\n(Lv.${defenseLevel}/${GameConfig.DEFENSE_UPGRADE_MAX_LEVEL})"
                defenseUpgrade1.isEnabled = false
            } else {
                defenseUpgrade1.text = "체력 +${GameConfig.DEFENSE_UPGRADE_VALUE}\n💰 ${gameView.getDefenseCost()} 자원\n(Lv.${defenseLevel}/${GameConfig.DEFENSE_UPGRADE_MAX_LEVEL})"
                defenseUpgrade1.isEnabled = true
            }
        }
    }
    
    /**
     * 일시정지 다이얼로그 표시
     */
    fun showPauseDialog() {
        // 다이얼로그 생성
        val dialog = Dialog(context)
        
        // 레이아웃 설정
        dialog.setContentView(R.layout.dialog_pause_menu)
        dialog.setCancelable(false)
        
        // 버튼 설정
        // 1. 게임 계속하기 버튼
        val btnResume = dialog.findViewById<Button>(R.id.btnResume)
        btnResume.setOnClickListener {
            dialog.dismiss()
            
            // 게임 재개
            gameViewModel.setPaused(false)
            gameView.resume()
        }
        
        // 2. 끝내기 버튼 (게임 오버로 처리)
        val btnExitGame = dialog.findViewById<Button>(R.id.btnExitGame)
        btnExitGame.setOnClickListener {
            dialog.dismiss()
            
            // 현재 웨이브와 자원 정보를 활용하여 게임 오버 처리
            val currentResource = gameView.getResource()
            val currentWave = gameView.getWaveCount()
            onGameOver(currentResource, currentWave)
        }
        
        // 다이얼로그 표시
        dialog.show()
    }
    
    /**
     * 게임 오버 다이얼로그 표시
     */
    fun onGameOver(resource: Int, waveCount: Int) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_game_over)
        
        // 점수와 웨이브 표시
        val tvGameOverScore = dialog.findViewById<TextView>(R.id.tvGameOverScore)
        val tvGameOverWave = dialog.findViewById<TextView>(R.id.tvGameOverWave)
        
        tvGameOverScore.text = "최종 자원: $resource"
        tvGameOverWave.text = "도달한 웨이브: $waveCount"
        
        // 코인 관련 텍스트뷰 - 획득한 코인 표시
        val tvGameOverCoins = dialog.findViewById<TextView>(R.id.tvGameOverCoins)
        tvGameOverCoins.text = "획득한 코인: ${gameViewModel.earnedCoins.value}"
        
        // 종료 버튼 - 앱 종료
        val btnExit = dialog.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            dialog.dismiss()
            (context as? androidx.activity.ComponentActivity)?.finish()
        }
        
        // 메인 메뉴 버튼
        val btnMainMenu = dialog.findViewById<Button>(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            dialog.dismiss()
            // 게임 프래그먼트에서 네비게이션 처리를 하도록 콜백 필요
            onMainMenuRequested?.invoke()
        }
        
        dialog.setCancelable(false)
        dialog.show()
    }
    
    /**
     * 레벨 클리어 다이얼로그 표시
     */
    fun showLevelClearDialog(wave: Int, score: Int) {
        // 레벨 클리어 보상 (1단계 난이도 클리어 시 500 코인)
        val levelClearReward = 500
        
        // 코인 보상 지급
        gameViewModel.addCoins(levelClearReward)
        
        // 레벨 클리어 다이얼로그 표시
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_level_clear)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 텍스트 설정
        val tvLevelTitle = dialog.findViewById<TextView>(R.id.tvLevelTitle)
        val tvClearedWaves = dialog.findViewById<TextView>(R.id.tvClearedWaves)
        val tvRewardCoins = dialog.findViewById<TextView>(R.id.tvRewardCoins)
        
        tvLevelTitle.text = "1단계 난이도"
        tvClearedWaves.text = "$wave 웨이브 클리어!"
        tvRewardCoins.text = "$levelClearReward 코인"
        
        // 메인화면으로 버튼
        val btnToMainMenu = dialog.findViewById<Button>(R.id.btnToMainMenu)
        btnToMainMenu.setOnClickListener {
            dialog.dismiss()
            onMainMenuRequested?.invoke()
        }
        
        // 다시 도전 버튼
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnPlayAgain)
        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            // 게임 리셋 및 재시작
            onRestartGameRequested?.invoke()
        }
        
        dialog.setCancelable(false)
        dialog.show()
        
        // 통계 업데이트 - 게임 클리어 횟수 증가
        gameViewModel.incrementGamesCompleted()
    }
    
    /**
     * 포커 카드 다이얼로그 표시
     */
    fun showPokerCardsDialog(waveNumber: Int, onPokerHandSelected: (PokerHand) -> Unit) {
        val dialog = PokerCardsDialog(context, waveNumber, onPokerHandSelected)
        dialog.show()
    }
    
    // 콜백 정의
    var onMainMenuRequested: (() -> Unit)? = null
    var onRestartGameRequested: (() -> Unit)? = null
    
    /**
     * 게임 리소스 정리
     */
    fun cleanup() {
        // 플러시 스킬 매니저 정리
        if (::flushSkillManager.isInitialized) {
            flushSkillManager.resetAllSkills()
        }
    }
} 