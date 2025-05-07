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
import androidx.appcompat.app.AlertDialog
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
import com.example.p40.game.FlushSkillManager
import kotlin.random.Random

class GameFragment : Fragment(R.layout.fragment_game), GameOverListener, PokerCardManager.PokerCardListener {

    private lateinit var gameView: GameView
    private var isPaused = false
    
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
    
    // 플러시 스킬 매니저 추가
    private lateinit var flushSkillManager: FlushSkillManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
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
                    userManager.addCoin(100)
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

        // 플러시 스킬 매니저 초기화
        val flushSkillButtonContainer = view.findViewById<LinearLayout>(R.id.flushSkillButtonContainer)
        flushSkillManager = FlushSkillManager(
            requireContext(),
            gameView,
            flushSkillButtonContainer,
            messageManager
        )
        
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
    
    // 버프 UI 초기화
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
        
        // 현재 적용된 모든 버프 목록 가져오기
        val buffs = gameView.getBuffManager().getAllBuffs()
        
        // 플러시 스킬 감지 및 활성화
        checkAndActivateFlushSkills(buffs)
        
        // 일반 버프 표시 처리
        val displayBuffs = buffs.filter { buff ->
            // 플러시 스킬 버프는 목록에서 제외
            buff.type !in listOf(
                BuffType.HEART_FLUSH_SKILL,
                BuffType.SPADE_FLUSH_SKILL, 
                BuffType.CLUB_FLUSH_SKILL,
                BuffType.DIAMOND_FLUSH_SKILL
            )
        }
        
        // 버프 컨테이너 찾기
        val buffContainer = view?.findViewById<LinearLayout>(R.id.buffContainer)
        buffContainer?.removeAllViews()
        
        // 버프 없음 메시지 참조
        val tvBuffList = view?.findViewById<TextView>(R.id.tvBuffList)
        
        if (displayBuffs.isNotEmpty()) {
            // 새로운 방식: BuffManager를 통해 생성된 버프 뷰 추가
            displayBuffs.forEach { buff ->
                val buffView = gameView.getBuffManager().createBuffView(buff)
                buffContainer?.addView(buffView)
            }
            
            // 기존 텍스트 뷰 숨기기
            tvBuffList?.visibility = View.GONE
            buffContainer?.visibility = View.VISIBLE
        } else {
            // 버프가 없을 경우
            tvBuffList?.text = "버프 없음"
            tvBuffList?.visibility = View.VISIBLE
            buffContainer?.visibility = View.GONE
        }
    }
    
    // 플러시 스킬 감지 및 활성화
    private fun checkAndActivateFlushSkills(buffs: List<Buff>) {
        // 각 문양별 플러시 스킬 버프가 있는지 확인
        // 버프가 있으면 해당 스킬 활성화
        
        // 하트 플러시 스킬
        if (buffs.any { it.type == BuffType.HEART_FLUSH_SKILL }) {
            flushSkillManager.activateFlushSkill(CardSuit.HEART)
            // 버프 제거 (1회성)
            gameView.getBuffManager().removeBuff(BuffType.HEART_FLUSH_SKILL)
        }
        
        // 스페이드 플러시 스킬
        if (buffs.any { it.type == BuffType.SPADE_FLUSH_SKILL }) {
            flushSkillManager.activateFlushSkill(CardSuit.SPADE)
            // 버프 제거 (1회성)
            gameView.getBuffManager().removeBuff(BuffType.SPADE_FLUSH_SKILL)
        }
        
        // 클로버 플러시 스킬
        if (buffs.any { it.type == BuffType.CLUB_FLUSH_SKILL }) {
            flushSkillManager.activateFlushSkill(CardSuit.CLUB)
            // 버프 제거 (1회성)
            gameView.getBuffManager().removeBuff(BuffType.CLUB_FLUSH_SKILL)
        }
        
        // 다이아 플러시 스킬
        if (buffs.any { it.type == BuffType.DIAMOND_FLUSH_SKILL }) {
            flushSkillManager.activateFlushSkill(CardSuit.DIAMOND)
            // 버프 제거 (1회성)
            gameView.getBuffManager().removeBuff(BuffType.DIAMOND_FLUSH_SKILL)
        }
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
        val formattedAttackSpeed = String.format("%.2f", attacksPerSecond) // 소수점 두자리로 변경
        view?.findViewById<TextView>(R.id.unitAttackSpeedText)?.text = "공속: ${formattedAttackSpeed}/초"
        
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
        val dialog = PokerCardsDialog(requireContext(), waveNumber) { pokerHand ->
            // 선택된 포커 족보 적용
            applyPokerHandEffect(pokerHand)
            
            // 버프 정보 업데이트
            updateBuffUI()
        }
        
        dialog.show()
    }
    
    // 포커 족보 효과 적용
    override fun applyPokerHandEffect(pokerHand: PokerHand) {
        // GameView에 포커 족보 효과 적용
        gameView.applyPokerHandEffect(pokerHand)
        
        // 버프 정보 업데이트
        updateBuffUI()
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
        
        // 화면 높이의 일정 부분을 시작점으로 사용 (5%로 변경하여 더 위에서 시작)
        val displayMetrics = requireContext().resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val startPosition = screenHeight * 0.05f  // 화면 높이의 5%만큼 아래에서 시작
        
        panel.translationY = startPosition
        
        val animator = ObjectAnimator.ofFloat(panel, "translationY", 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        currentOpenPanel = panel
    }
    
    private fun closePanel(panel: LinearLayout) {
        // 화면 높이의 일정 부분을 종료점으로 사용 (5%로 변경)
        val displayMetrics = requireContext().resources.displayMetrics
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
            } else {
                // 자원 부족
                messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
            }
        }
        
        // 다른 버튼들은 아직 구현하지 않음
        defenseUpgrade2.setOnClickListener {
            // 준비 중인 기능
        }
        
        defenseUpgrade3.setOnClickListener {
            // 준비 중인 기능
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
            showPauseDialog()
        }
    }
    
    // 게임 오버 처리
    override fun onGameOver(resource: Int, waveCount: Int) {
        if (!isAdded) return
        
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_game_over)
        
        // 점수와 웨이브 표시
        val tvGameOverScore = dialog.findViewById<TextView>(R.id.tvGameOverScore)
        val tvGameOverWave = dialog.findViewById<TextView>(R.id.tvGameOverWave)
        
        tvGameOverScore.text = "최종 자원: $resource"
        tvGameOverWave.text = "도달한 웨이브: $waveCount"
        
        // 획득한 코인 계산 (웨이브 * 10)
        val earnedCoins = waveCount * 10
        val tvGameOverCoins = dialog.findViewById<TextView>(R.id.tvGameOverCoins)
        tvGameOverCoins.text = "획득한 코인: $earnedCoins"
        
        // 코인 저장 (기존 코인 + 획득한 코인)
        userManager.addCoin(earnedCoins)
        
        // 종료 버튼 - 앱 종료
        val btnExit = dialog.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            requireActivity().finish()
        }
        
        // 메인 메뉴 버튼
        val btnMainMenu = dialog.findViewById<Button>(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            findNavController().navigate(R.id.action_gameFragment_to_mainMenuFragment)
            dialog.dismiss()
        }
        
        dialog.setCancelable(false)
        dialog.show()
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
            // 경고 대화상자 표시
            showExitConfirmationDialog(dialog)
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

    // 나가기 확인 대화상자
    private fun showExitConfirmationDialog(pauseDialog: Dialog) {
        val builder = AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
        
        // 커스텀 타이틀 뷰 생성
        val titleView = TextView(requireContext()).apply {
            text = "게임 종료"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#e74c3c")) // 빨간색 배경
            gravity = android.view.Gravity.CENTER
        }
        
        builder.setCustomTitle(titleView)
            .setMessage("메인화면으로 나가면 현재 진행중인 게임내용은 저장되지 않습니다.\n정말 나가시겠습니까?")
            .setPositiveButton("나가기") { _, _ ->
                // 일시정지 다이얼로그 닫기
                pauseDialog.dismiss()
                
                // 게임 리소스 정리
                cleanupGameResources()
                
                // 메인화면으로 이동
                findNavController().navigate(R.id.action_gameFragment_to_mainMenuFragment)
            }
            .setNegativeButton("취소") { dialog, _ ->
                // 경고창만 닫고 일시정지 상태 유지
                dialog.dismiss()
            }
            .setCancelable(false)
        
        // 대화상자 표시
        val dialog = builder.create()
        
        // 대화상자가 표시된 후 버튼 색상 변경
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#e74c3c"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#3498db"))
        }
        
        dialog.show()
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
        
        // 메시지 매니저 정리
        if (::messageManager.isInitialized) {
            messageManager.clear()
        }
        
        // GameView 내부적으로 추가 정리 (GameView 클래스에 해당 메서드 구현 필요)
        // gameView.cleanup()
    }

    // UI 업데이트 시작하는 함수
    private fun startUiUpdates() {
        handler.post(uiUpdateRunnable)
    }

    // 코인 UI 업데이트
    private fun updateCoinUI() {
        view?.findViewById<TextView>(R.id.tvCoinInfo)?.text = "코인: ${userManager.getCoin()}"
    }

    // 코인 획득
    private fun addCoins(amount: Int) {
        userManager.addCoin(amount)
        updateCoinUI()
    }

    // 코인 사용
    private fun useCoins(amount: Int): Boolean {
        return if (userManager.getCoin() >= amount) {
            userManager.addCoin(-amount)
            updateCoinUI()
            true
        } else {
            messageManager.showWarning("코인이 부족합니다!")
            false
        }
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
            showPauseDialog()
        }
    }
}

