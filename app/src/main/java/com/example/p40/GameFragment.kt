package com.example.p40

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.game.Buff
import com.example.p40.game.GameConfig
import com.example.p40.game.GameOverListener
import com.example.p40.game.GameView
import com.example.p40.game.PokerHand
import com.example.p40.game.BossKillListener
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.PokerHandEvaluator
import com.example.p40.game.PokerDeck
import com.example.p40.game.CardUtils
import com.example.p40.game.JokerSelectionDialog
import com.example.p40.game.PokerCardManager
import com.example.p40.game.BuffType
import com.example.p40.game.MessageManager
import kotlin.random.Random

class GameFragment : Fragment(R.layout.fragment_game), GameOverListener, PokerCardManager.PokerCardListener {

    private lateinit var gameView: GameView
    private var isPaused = false
    
    // 현재 열려있는 패널 추적
    private var currentOpenPanel: LinearLayout? = null
    
    // 현재 웨이브 정보
    private var currentWave = 1
    
    // 코인 정보
    private var coins = 0
    
    // 버프 정보 UI
    private lateinit var tvBuffList: TextView
    
    // 메시지 관리자 추가
    private lateinit var messageManager: MessageManager
    
    // UI 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateGameInfoUI()
            updateBuffUI()
            updateUnitStatsUI()
            updateEnemyStatsUI()
            updateBossStatsUI()
            updateCoinUI()
            handler.postDelayed(this, 500) // 500ms마다 업데이트
        }
    }

    // 포커 카드 관련 변수 추가
    private lateinit var pokerCardManager: PokerCardManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 메시지 관리자 초기화는 onStart로 이동
        messageManager = MessageManager.getInstance()
        
        // 게임 레벨 정보 가져오기
        arguments?.let { args ->
            val levelId = args.getInt("levelId", 1)
            val totalWaves = args.getInt("totalWaves", 10)
            
            // 웨이브 수만 설정 (난이도 설정은 제거됨)
            GameConfig.setTotalWaves(totalWaves)
        }
        
        // 게임 뷰 초기화
        gameView = view.findViewById(R.id.gameView)
        
        // 게임 오버 리스너 설정
        gameView.setGameOverListener(this)
        
        // 보스 처치 리스너 설정
        gameView.setBossKillListener(object : BossKillListener {
            override fun onBossKilled() {
                // UI 스레드에서 실행하기 위해 Handler 사용
                Handler(Looper.getMainLooper()).post {
                    // 보스 처치 시 100코인 획득
                    coins += 100
                    updateCoinUI()
                    // Toast 대신 메시지 매니저 사용
                    messageManager.showSuccess("보스 처치! +100 코인")
                }
            }
        })
        
        // 버프 UI 초기화
        initBuffUI(view)
        
        // 탭 버튼 초기화
        setupStatTabs(view)
        
        // 게임 메뉴 초기화
        setupGameMenu(view)
        
        // 업그레이드 버튼 설정
        setupAttackUpgradeButtons(view)
        setupDefenseUpgradeButtons(view)
        
        // 카드 버튼 설정 - 패널에서 직접 포커 카드 기능 처리
        setupCardButtons(view)
        
        // 웨이브 완료 리스너 설정
        setupWaveCompletionListener()
        
        // 패널 초기화
        val attackUpgradePanel = view.findViewById<LinearLayout>(R.id.attackUpgradePanel)
        val defenseUpgradePanel = view.findViewById<LinearLayout>(R.id.defenseUpgradePanel)
        val cardPanel = view.findViewById<LinearLayout>(R.id.cardPanel)
        
        // 공격업 버튼
        val attackUpButton = view.findViewById<Button>(R.id.attackUpButton)
        attackUpButton.setOnClickListener {
            togglePanel(attackUpgradePanel)
        }
        
        // 방어업 버튼
        val defenseUpButton = view.findViewById<Button>(R.id.defenseUpButton)
        defenseUpButton.setOnClickListener {
            togglePanel(defenseUpgradePanel)
        }
        
        // 카드 버튼
        val cardButton = view.findViewById<Button>(R.id.cardButton)
        cardButton.setOnClickListener {
            togglePanel(cardPanel)
        }
        
        // UI 업데이트 시작
        startUiUpdates()
        
        // 업그레이드 버튼 텍스트 초기화 (UI 업데이트 후에 실행)
        handler.post {
            updateUpgradeButtonsText()
        }

        // 저장된 코인 불러오기
        coins = MainMenuFragment.loadCoins(requireContext())
    }
    
    override fun onStart() {
        super.onStart()
        
        // 메시지 관리자 초기화
        // 액티비티가 완전히 준비된 후 초기화
        view?.let { messageManager.init(requireActivity().findViewById(android.R.id.content)) }
        
        // 저장된 덱 확인
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(requireContext())
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            messageManager.showInfo("저장된 덱이 게임에 적용되었습니다 (${savedDeck.size}장)")
        }
    }
    
    // 버프 UI 초기화
    private fun initBuffUI(view: View) {
        tvBuffList = view.findViewById(R.id.tvBuffList)
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
    
    // 게임 정보 UI 업데이트
    public override fun updateGameInfoUI() {
        if (!isAdded) return
        
        val resource = gameView.getResource()
        val wave = gameView.getWaveCount()
        val enemiesKilled = gameView.getKillCount()
        val totalEnemies = gameView.getTotalEnemiesInWave()
        
        // 현재 웨이브 정보 업데이트
        currentWave = wave
        
        // 웨이브 정보 업데이트
        view?.findViewById<TextView>(R.id.tvWaveInfo)?.text = "웨이브: $wave/${GameConfig.getTotalWaves()}"
        
        // 자원 정보 업데이트
        view?.findViewById<TextView>(R.id.tvResourceInfo)?.text = "자원: $resource"
        
        // 적 처치 정보 업데이트
        view?.findViewById<TextView>(R.id.tvKillInfo)?.text = "처치: $enemiesKilled/$totalEnemies"
    }
    
    // 버프 UI 업데이트
    private fun updateBuffUI() {
        if (!isAdded) return
        
        val activeBuffs = gameView.getActiveBuffs()
        val buffContainer = view?.findViewById<LinearLayout>(R.id.buffContainer)
        val tvBuffList = view?.findViewById<TextView>(R.id.tvBuffList)
        
        if (buffContainer == null || tvBuffList == null) return
        
        // 이전 버프 표시 제거
        buffContainer.removeAllViews()
        
        if (activeBuffs.isEmpty()) {
            // 버프가 없을 경우
            tvBuffList.text = "버프 없음"
            tvBuffList.visibility = View.VISIBLE
            return
        } else {
            tvBuffList.visibility = View.GONE
        }
        
        // 각 버프별 표시
        for (buff in activeBuffs) {
            // 버프 UI 요소 생성
            val buffView = createBuffView(buff)
            buffContainer.addView(buffView)
        }
    }
    
    // 버프 UI 요소 생성
    private fun createBuffView(buff: Buff): View {
        // 버프 타입에 따라 카테고리 결정
        val isDefenseBuff = when (buff.type) {
            BuffType.MISSILE_DAMAGE, BuffType.ATTACK_SPEED, 
            BuffType.MISSILE_SPEED, BuffType.MULTI_DIRECTION,
            BuffType.MISSILE_PIERCE, BuffType.RESOURCE_GAIN -> true
            
            BuffType.ENEMY_SLOW, BuffType.DOT_DAMAGE,
            BuffType.MASS_DAMAGE -> false
        }
        
        // 버프 뷰 생성
        val buffView = TextView(requireContext())
        buffView.text = buff.getShortDisplayText()
        buffView.textSize = 12f
        buffView.setTextColor(Color.WHITE)
        buffView.setPadding(10, 5, 10, 5)
        
        // 마진 설정
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = 8
        buffView.layoutParams = layoutParams
        
        // 배경 설정
        val drawable = GradientDrawable()
        drawable.cornerRadius = 8f
        
        if (isDefenseBuff) {
            drawable.setColor(GameConfig.BUFF_DEFENSE_COLOR)
            drawable.setStroke(1, GameConfig.BUFF_DEFENSE_STROKE_COLOR)
        } else {
            drawable.setColor(GameConfig.BUFF_ENEMY_NERF_COLOR)
            drawable.setStroke(1, GameConfig.BUFF_ENEMY_NERF_STROKE_COLOR)
        }
        
        buffView.background = drawable
        
        return buffView
    }
    
    // 내 유닛 스탯 UI 업데이트
    private fun updateUnitStatsUI() {
        if (!isAdded) return
        
        val health = gameView.getUnitHealth()
        val maxHealth = gameView.getUnitMaxHealth()
        val attack = gameView.getUnitAttack()
        val attackSpeed = gameView.getUnitAttackSpeed()
        val attackRange = gameView.getUnitAttackRange()
        
        // 체력 정보 업데이트
        view?.findViewById<TextView>(R.id.unitHealthText)?.text = "체력: $health/$maxHealth"
        
        // 공격력 정보 업데이트
        view?.findViewById<TextView>(R.id.unitAttackText)?.text = "공격력: $attack"
        
        // 공격속도 정보 업데이트
        val attacksPerSecond = 1000.0 / attackSpeed
        val formattedAttackSpeed = String.format("%.2f", attacksPerSecond)
        view?.findViewById<TextView>(R.id.unitAttackSpeedText)?.text = "공격속도: ${formattedAttackSpeed}/초"
        
        // 사거리 정보 업데이트
        view?.findViewById<TextView>(R.id.unitRangeText)?.text = "사거리: ${attackRange.toInt()}"
    }
    
    // 적 유닛 스탯 UI 업데이트
    private fun updateEnemyStatsUI() {
        if (!isAdded) return
        
        val wave = gameView.getWaveCount()
        
        // GameConfig를 통해 웨이브별 적 스탯 정보 계산
        val health = GameConfig.getEnemyHealthForWave(wave)
        val damage = GameConfig.getEnemyDamageForWave(wave, false)
        val speed = GameConfig.getEnemySpeedForWave(wave)
        
        // 체력 정보 업데이트
        view?.findViewById<TextView>(R.id.enemyHealthText)?.text = "체력: $health"
        
        // 공격력 정보 업데이트
        view?.findViewById<TextView>(R.id.enemyAttackText)?.text = "공격력: $damage"
        
        // 이동속도 정보 업데이트
        val formattedSpeed = String.format("%.2f", speed)
        view?.findViewById<TextView>(R.id.enemySpeedText)?.text = "이동속도: $formattedSpeed"
    }
    
    // 보스 유닛 스탯 UI 업데이트
    private fun updateBossStatsUI() {
        if (!isAdded) return
        
        val wave = gameView.getWaveCount()
        
        // GameConfig를 통해 웨이브별 보스 스탯 정보 계산
        val maxHealth = GameConfig.getEnemyHealthForWave(wave, true)
        val damage = GameConfig.getEnemyDamageForWave(wave, true)
        val speed = GameConfig.getEnemySpeedForWave(wave, true)
        
        // 현재 보스 체력 정보 가져오기
        val currentBossHealth = gameView.getCurrentBossHealth()
        
        // 체력 정보 업데이트 - 현재/최대 체력 표시 형식으로 변경
        val healthText = if (currentBossHealth > 0) {
            "체력: $currentBossHealth/$maxHealth"
        } else {
            "체력: $maxHealth"
        }
        view?.findViewById<TextView>(R.id.bossHealthText)?.text = healthText
        
        // 공격력 정보 업데이트
        view?.findViewById<TextView>(R.id.bossAttackText)?.text = "공격력: $damage"
        
        // 이동속도 정보 업데이트
        val formattedSpeed = String.format("%.2f", speed)
        view?.findViewById<TextView>(R.id.bossSpeedText)?.text = "이동속도: $formattedSpeed"
    }
    
    // 웨이브 완료 리스너 설정
    private fun setupWaveCompletionListener() {
        // 테스트용 코드 제거
        // 실제 구현 시에는 GameView 클래스에 웨이브 완료 콜백 추가 필요
        // gameView.setOnWaveCompletedListener { waveNumber -> 
        //     onWaveCompleted(waveNumber)
        // }
    }
    
    // 웨이브 완료 시 처리
    private fun onWaveCompleted(waveNumber: Int) {
        // 게임 일시 정지
        gameView.pause()
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 포커 카드 다이얼로그 표시
        showPokerCardsDialog(waveNumber)
    }
    
    // 포커 카드 다이얼로그 표시
    private fun showPokerCardsDialog(waveNumber: Int) {
        // 이전: 게임 일시 정지 제거
        // gameView.pause()
        // handler.removeCallbacks(uiUpdateRunnable)
        
        val dialog = PokerCardsDialog(requireContext(), waveNumber) { pokerHand ->
            // 선택된 포커 족보 적용
            applyPokerHandEffect(pokerHand)
            
            // 게임 재개 코드 제거(게임이 계속 진행되므로)
            // gameView.resume()
            // handler.post(uiUpdateRunnable)
            
            // 버프 정보 업데이트
            updateBuffUI()
            
            // 메시지 표시
            messageManager.showSuccess("적용된 효과: ${pokerHand.handName}")
        }
        
        dialog.show()
    }
    
    // 포커 족보 효과 적용
    override fun applyPokerHandEffect(pokerHand: PokerHand) {
        // GameView에 포커 족보 효과 적용
        gameView.applyPokerHandEffect(pokerHand)
        
        // 버프 정보 업데이트
        updateBuffUI()
        
        // 메시지 표시
        messageManager.showSuccess("적용된 효과: ${pokerHand.handName}")
    }
    
    private fun togglePanel(panel: LinearLayout) {
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
    
    private fun openPanel(panel: LinearLayout) {
        panel.visibility = View.VISIBLE
        panel.translationY = panel.height.toFloat()
        
        val animator = ObjectAnimator.ofFloat(panel, "translationY", 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        currentOpenPanel = panel
    }
    
    private fun closePanel(panel: LinearLayout) {
        val animator = ObjectAnimator.ofFloat(panel, "translationY", panel.height.toFloat())
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
    
    private fun setupAttackUpgradeButtons(view: View) {
        val btnUpgradeDamage = view.findViewById<Button>(R.id.btnUpgradeDamage)
        val btnUpgradeAttackSpeed = view.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
        val btnUpgradeAttackRange = view.findViewById<Button>(R.id.btnUpgradeAttackRange)
        
        // 데미지 업그레이드 버튼
        btnUpgradeDamage.setOnClickListener {
            val cost = gameView.getDamageCost()
            if (gameView.upgradeDamage()) {
                // 업그레이드 성공
                updateGameInfoUI() // 자원 정보 갱신
                updateUnitStatsUI() // 스탯 정보 갱신
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
                // Toast 대신 메시지 매니저 사용
                messageManager.showSuccess("데미지 +1 향상! (비용: $cost)")
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
                updateGameInfoUI() // 자원 정보 갱신
                updateUnitStatsUI() // 스탯 정보 갱신
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
                messageManager.showSuccess("공격속도 +1% 향상! (비용: $cost)")
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
                updateGameInfoUI() // 자원 정보 갱신
                updateUnitStatsUI() // 스탯 정보 갱신
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
                messageManager.showSuccess("공격범위 +5 향상! (비용: $cost)")
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
    }
    
    private fun setupDefenseUpgradeButtons(view: View) {
        val defenseUpgrade1 = view.findViewById<Button>(R.id.defenseUpgrade1)
        val defenseUpgrade2 = view.findViewById<Button>(R.id.defenseUpgrade2)
        val defenseUpgrade3 = view.findViewById<Button>(R.id.defenseUpgrade3)
        
        defenseUpgrade1.setOnClickListener {
            val cost = gameView.getDefenseCost()
            if (gameView.upgradeDefense()) {
                // 업그레이드 성공
                updateGameInfoUI() // 자원 정보 갱신
                updateUnitStatsUI() // 스탯 정보 갱신
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
                messageManager.showSuccess("방어력 +20 향상! (비용: $cost)")
                // 패널을 닫지 않도록 수정됨
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
        
        // 다른 버튼들은 아직 구현하지 않음
        defenseUpgrade2.setOnClickListener {
            messageManager.showInfo("준비 중인 기능입니다")
        }
        
        defenseUpgrade3.setOnClickListener {
            messageManager.showInfo("준비 중인 기능입니다")
        }
    }
    
    // 카드 버튼 설정 - 패널에서 직접 포커 카드 기능 처리
    private fun setupCardButtons(view: View) {
        // 포커 카드 매니저 초기화
        pokerCardManager = PokerCardManager(requireContext(), view, this)
        
        // 카드 버튼 - 이제 카드 패널 직접 열기
        val cardButton = view?.findViewById<Button>(R.id.cardButton)
        cardButton?.setOnClickListener {
            // 패널 토글 기능 유지
            togglePanel(view.findViewById(R.id.cardPanel))
        }
    }
    
    // PokerCardListener 인터페이스 구현
    override fun getResource(): Int {
        return gameView.getResource()
    }
    
    override fun useResource(amount: Int): Boolean {
        return gameView.useResource(amount)
    }
    
    // 모든 업그레이드 버튼 텍스트 업데이트
    private fun updateUpgradeButtonsText() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 데미지 업그레이드 버튼
        val btnUpgradeDamage = view?.findViewById<Button>(R.id.btnUpgradeDamage)
        if (btnUpgradeDamage != null) {
            val damageLevel = gameView.getDamageLevel()
            if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
                btnUpgradeDamage.text = "데미지\n최대 레벨"
                btnUpgradeDamage.isEnabled = false
            } else {
                btnUpgradeDamage.text = "데미지 +${GameConfig.DAMAGE_UPGRADE_VALUE}\n💰 ${gameView.getDamageCost()} 자원"
                btnUpgradeDamage.isEnabled = true
            }
        }
        
        // 공격속도 업그레이드 버튼
        val btnUpgradeAttackSpeed = view?.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
        if (btnUpgradeAttackSpeed != null) {
            val attackSpeedLevel = gameView.getAttackSpeedLevel()
            if (attackSpeedLevel >= GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
                btnUpgradeAttackSpeed.text = "공격속도\n최대 레벨"
                btnUpgradeAttackSpeed.isEnabled = false
            } else {
                btnUpgradeAttackSpeed.text = "공격속도 +${(GameConfig.ATTACK_SPEED_UPGRADE_PERCENT * 100).toInt()}%\n💰 ${gameView.getAttackSpeedCost()} 자원"
                btnUpgradeAttackSpeed.isEnabled = true
            }
        }
        
        // 공격범위 업그레이드 버튼
        val btnUpgradeAttackRange = view?.findViewById<Button>(R.id.btnUpgradeAttackRange)
        if (btnUpgradeAttackRange != null) {
            val attackRangeLevel = gameView.getAttackRangeLevel()
            if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
                btnUpgradeAttackRange.text = "공격범위\n최대 레벨"
                btnUpgradeAttackRange.isEnabled = false
            } else {
                btnUpgradeAttackRange.text = "공격범위 +${GameConfig.ATTACK_RANGE_UPGRADE_VALUE.toInt()}\n💰 ${gameView.getAttackRangeCost()} 자원"
                btnUpgradeAttackRange.isEnabled = true
            }
        }
        
        // 방어력 업그레이드 버튼
        val defenseUpgrade1 = view?.findViewById<Button>(R.id.defenseUpgrade1)
        if (defenseUpgrade1 != null) {
            val defenseLevel = gameView.getDefenseLevel()
            if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
                defenseUpgrade1.text = "체력\n최대 레벨"
                defenseUpgrade1.isEnabled = false
            } else {
                defenseUpgrade1.text = "체력 +${GameConfig.DEFENSE_UPGRADE_VALUE}\n💰 ${gameView.getDefenseCost()} 자원"
                defenseUpgrade1.isEnabled = true
            }
        }
    }
    
    // 게임 오버 다이얼로그 표시
    override fun onGameOver(resource: Int, waveCount: Int) {
        // 코인 저장
        saveCoins()
        
        // 게임 오버 다이얼로그 생성
        val dialog = Dialog(requireContext())
        
        // 게임 오버 다이얼로그 레이아웃 설정
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_game_over, null)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 다이얼로그 내용 설정
        val tvGameOverScore = dialogView.findViewById<TextView>(R.id.tvGameOverScore)
        val tvGameOverWave = dialogView.findViewById<TextView>(R.id.tvGameOverWave)
        val tvGameOverCoins = dialogView.findViewById<TextView>(R.id.tvGameOverCoins)
        
        tvGameOverScore.text = "최종 자원: $resource"
        tvGameOverWave.text = "도달한 웨이브: $waveCount"
        tvGameOverCoins?.text = "보유 코인: $coins"
        
        // 메인 메뉴 버튼 설정
        val btnMainMenu = dialogView.findViewById<Button>(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_gameFragment_to_mainMenuFragment)
        }
        
        // 게임 종료 버튼 설정
        val btnExit = dialogView.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            dialog.dismiss()
            requireActivity().finish()  // 앱 종료
        }
        
        // 다이얼로그 표시
        dialog.show()
    }
    
    override fun onPause() {
        super.onPause()
        gameView.pause()
        handler.removeCallbacks(uiUpdateRunnable)
    }
    
    override fun onResume() {
        super.onResume()
        if (!isPaused) {
            gameView.resume()
            handler.post(uiUpdateRunnable)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(uiUpdateRunnable)
        
        // 코인 저장
        saveCoins()
    }

    // setupGameMenu 함수 추가 (pauseButton과 exitButton 설정 코드를 여기로 이동)
    private fun setupGameMenu(view: View) {
        // 종료(일시정지) 버튼
        val exitButton = view.findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            // 게임 일시정지
            isPaused = true
            gameView.pause()
            handler.removeCallbacks(uiUpdateRunnable)
            
            // 일시정지 메뉴 보여주기
            showPauseDialog()
        }
    }

    // 일시정지 다이얼로그 표시
    private fun showPauseDialog() {
        // 다이얼로그 생성
        val dialog = Dialog(requireContext())
        
        // 레이아웃 설정
        dialog.setContentView(R.layout.dialog_pause_menu)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 버튼 설정
        // 1. 게임 계속하기 버튼
        val btnResume = dialog.findViewById<Button>(R.id.btnResume)
        btnResume.setOnClickListener {
            dialog.dismiss()
            
            // 게임 재개
            isPaused = false
            gameView.resume()
            handler.post(uiUpdateRunnable)
        }
        
        // 2. 메인화면으로 버튼
        val btnMainMenu = dialog.findViewById<Button>(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            dialog.dismiss()
            
            // 메인화면으로 이동
            findNavController().navigate(R.id.action_gameFragment_to_mainMenuFragment)
        }
        
        // 3. 게임 종료 버튼
        val btnExitGame = dialog.findViewById<Button>(R.id.btnExitGame)
        btnExitGame.setOnClickListener {
            dialog.dismiss()
            
            // 앱 종료
            requireActivity().finish()
        }
        
        // 다이얼로그 표시
        dialog.show()
    }

    // UI 업데이트 시작하는 함수
    private fun startUiUpdates() {
        handler.post(uiUpdateRunnable)
    }

    // 코인 UI 업데이트
    private fun updateCoinUI() {
        if (!isAdded) return
        view?.findViewById<TextView>(R.id.tvCoinInfo)?.text = "코인: $coins"
    }

    // 코인 획득
    private fun addCoins(amount: Int) {
        coins += amount
        updateCoinUI()
        saveCoins() // 코인이 변경될 때마다 저장
    }

    // 코인 사용
    private fun useCoins(amount: Int): Boolean {
        return if (coins >= amount) {
            coins -= amount
            updateCoinUI()
            saveCoins() // 코인이 변경될 때마다 저장
            true
        } else {
            messageManager.showWarning("코인이 부족합니다!")
            false
        }
    }

    // 코인 저장
    private fun saveCoins() {
        MainMenuFragment.saveCoins(requireContext(), coins)
    }
}

