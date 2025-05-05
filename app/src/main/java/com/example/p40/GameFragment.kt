package com.example.p40

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
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
import kotlin.random.Random

class GameFragment : Fragment(R.layout.fragment_game), GameOverListener {

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
    
    // UI 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateGameInfoUI()
            updateBuffUI()
            updateUnitStatsUI()
            updateEnemyStatsUI()
            updateCoinUI()
            handler.postDelayed(this, 500) // 500ms마다 업데이트
        }
    }

    // 포커 카드 관련 변수 추가
    private lateinit var pokerCardPanel: PokerCardPanel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 게임 레벨 정보 가져오기
        arguments?.let { args ->
            val levelId = args.getInt("levelId", 1)
            val totalWaves = args.getInt("totalWaves", 10)
            val difficulty = args.getFloat("difficulty", 1.0f)
            
            // GameConfig에 게임 레벨 설정 적용
            GameConfig.setGameLevel(difficulty, totalWaves)
        }
        
        // 게임 뷰 초기화
        gameView = view.findViewById(R.id.gameView)
        
        // 게임 오버 리스너 설정
        gameView.setGameOverListener(this)
        
        // 보스 처치 리스너 설정
        gameView.setBossKillListener(object : BossKillListener {
            override fun onBossKilled() {
                // 보스 처치 시 100코인 획득
                coins += 100
                updateCoinUI()
                Toast.makeText(context, "보스 처치! +100 코인", Toast.LENGTH_SHORT).show()
            }
        })
        
        // 버프 UI 초기화
        initBuffUI(view)
        
        // 탭 버튼 초기화
        setupStatTabs(view)
        
        // 저장된 덱 확인
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(requireContext())
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            Toast.makeText(
                requireContext(),
                "저장된 덱이 게임에 적용되었습니다 (${savedDeck.size}장)",
                Toast.LENGTH_SHORT
            ).show()
        }
        
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
    
    // 버프 UI 초기화
    private fun initBuffUI(view: View) {
        tvBuffList = view.findViewById(R.id.tvBuffList)
    }
    
    // 스탯 탭 설정
    private fun setupStatTabs(view: View) {
        val myUnitTabButton = view.findViewById<TextView>(R.id.myUnitTabButton)
        val enemyUnitTabButton = view.findViewById<TextView>(R.id.enemyUnitTabButton)
        val myUnitStatsContainer = view.findViewById<LinearLayout>(R.id.myUnitStatsContainer)
        val enemyStatsContainer = view.findViewById<LinearLayout>(R.id.enemyStatsContainer)
        
        // 초기 상태 설정 (내 유닛 정보 탭이 활성화)
        updateTabState(true, myUnitTabButton, enemyUnitTabButton, myUnitStatsContainer, enemyStatsContainer)
        
        // 내 유닛 정보 탭 클릭 시
        myUnitTabButton.setOnClickListener {
            updateTabState(true, myUnitTabButton, enemyUnitTabButton, myUnitStatsContainer, enemyStatsContainer)
        }
        
        // 적 유닛 정보 탭 클릭 시
        enemyUnitTabButton.setOnClickListener {
            updateTabState(false, myUnitTabButton, enemyUnitTabButton, myUnitStatsContainer, enemyStatsContainer)
        }
    }
    
    // 탭 상태 업데이트 (코드 중복 제거 및 일관성 유지)
    private fun updateTabState(
        isMyUnitTab: Boolean, 
        myUnitTabButton: TextView, 
        enemyUnitTabButton: TextView,
        myUnitStatsContainer: LinearLayout,
        enemyStatsContainer: LinearLayout
    ) {
        if (isMyUnitTab) {
            // 내 유닛 탭 활성화
            myUnitTabButton.setTextColor(resources.getColor(android.R.color.white, null))
            myUnitTabButton.setBackgroundResource(R.drawable.tab_selected_background)
            enemyUnitTabButton.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            enemyUnitTabButton.setBackgroundResource(R.drawable.tab_unselected_background)
            
            // 내 유닛 정보 표시, 적 정보 숨김
            myUnitStatsContainer.visibility = View.VISIBLE
            enemyStatsContainer.visibility = View.GONE
            
            // 최신 유닛 스탯 업데이트
            updateUnitStatsUI()
        } else {
            // 적 유닛 탭 활성화
            myUnitTabButton.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            myUnitTabButton.setBackgroundResource(R.drawable.tab_unselected_background)
            enemyUnitTabButton.setTextColor(resources.getColor(android.R.color.white, null))
            enemyUnitTabButton.setBackgroundResource(R.drawable.tab_selected_background)
            
            // 적 정보 표시, 내 유닛 정보 숨김
            myUnitStatsContainer.visibility = View.GONE
            enemyStatsContainer.visibility = View.VISIBLE
            
            // 최신 적 스탯 업데이트
            updateEnemyStatsUI()
        }
    }
    
    // 게임 정보 UI 업데이트
    private fun updateGameInfoUI() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 게임 상태 정보 가져오기
        val resource = gameView.getResource()
        val waveCount = gameView.getWaveCount()
        val killCount = gameView.getKillCount()
        val totalEnemies = gameView.getTotalEnemiesInWave()
        val totalWaves = GameConfig.getTotalWaves()
        val difficulty = GameConfig.getDifficulty()
        
        // 각 정보를 개별 TextView에 업데이트
        view?.apply {
            findViewById<TextView>(R.id.tvResourceInfo)?.text = "자원: $resource"
            findViewById<TextView>(R.id.tvWaveInfo)?.text = "웨이브: $waveCount/$totalWaves"
            findViewById<TextView>(R.id.tvKillInfo)?.text = "처치: $killCount/$totalEnemies"
        }
    }
    
    // 유닛 스탯 UI 업데이트
    private fun updateUnitStatsUI() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 유닛 스탯 정보 가져오기
        val health = gameView.getUnitHealth()
        val maxHealth = gameView.getUnitMaxHealth()
        val attack = gameView.getUnitAttack()
        val attackSpeed = gameView.getUnitAttackSpeed()
        val attacksPerSecond = 1000f / attackSpeed
        val attackRange = gameView.getUnitAttackRange()
        
        // UI 요소 찾기
        view?.findViewById<TextView>(R.id.unitHealthText)?.text = "체력: $health/$maxHealth"
        view?.findViewById<TextView>(R.id.unitAttackText)?.text = "공격력: $attack"
        view?.findViewById<TextView>(R.id.unitAttackSpeedText)?.text = "공격속도: ${String.format("%.2f", attacksPerSecond)}/초"
        view?.findViewById<TextView>(R.id.unitRangeText)?.text = "사거리: ${attackRange.toInt()}"
        
        // 이전 코드 유지 (이전 레이아웃과의 호환성을 위해)
        view?.findViewById<TextView>(R.id.tvUnitStats)?.text = 
            "체력: $health/$maxHealth  |  공격력: $attack  |  공격속도: ${String.format("%.2f", attacksPerSecond)}/초  |  범위: ${attackRange.toInt()}"
    }
    
    // 적 스탯 UI 업데이트 (새 메서드)
    private fun updateEnemyStatsUI() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 현재 웨이브 정보 가져오기
        val waveCount = gameView.getWaveCount()
        
        // 현재 웨이브의 적 스탯 계산
        val normalEnemyHealth = GameConfig.getEnemyHealthForWave(waveCount, false)
        val normalEnemyDamage = GameConfig.getEnemyDamageForWave(waveCount, false)
        val normalEnemySpeed = GameConfig.getEnemySpeedForWave(waveCount, false)
        
        // UI 요소 찾기
        view?.findViewById<TextView>(R.id.enemyHealthText)?.text = "체력: $normalEnemyHealth"
        view?.findViewById<TextView>(R.id.enemyAttackText)?.text = "공격력: $normalEnemyDamage"
        view?.findViewById<TextView>(R.id.enemySpeedText)?.text = "이동속도: ${String.format("%.1f", normalEnemySpeed)}"
    }
    
    // 버프 정보 업데이트
    private fun updateBuffUI() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 버프 컨테이너 가져오기
        val buffContainer = view?.findViewById<LinearLayout>(R.id.buffContainer) ?: return
        
        // 기존 버프 항목 모두 제거
        buffContainer.removeAllViews()
        
        // 버프 정보 가져오기
        val defenseBuffs = gameView.getDefenseBuffs()
        val enemyNerfs = gameView.getEnemyNerfs()
        
        // 버프가 있는지 확인
        val hasBuffs = defenseBuffs.isNotEmpty() || enemyNerfs.isNotEmpty()
        
        if (hasBuffs) {
            // 디펜스 유닛 버프 먼저 추가
            for (buff in defenseBuffs) {
                addBuffItem(buffContainer, buff, true)
            }
            
            // 적 너프 추가
            for (buff in enemyNerfs) {
                addBuffItem(buffContainer, buff, false)
            }
        } else {
            // 버프가 없으면 "없음" 텍스트뷰 표시
            val noBuff = TextView(context)
            noBuff.text = "없음"
            noBuff.textSize = 14f
            noBuff.setTextColor(resources.getColor(android.R.color.white, null))
            buffContainer.addView(noBuff)
        }
    }
    
    // 버프 항목 추가
    private fun addBuffItem(container: LinearLayout, buff: Buff, isDefenseBuff: Boolean) {
        // 레이아웃 인플레이터로 버프 항목 생성
        val inflater = LayoutInflater.from(context)
        val buffView = inflater.inflate(R.layout.item_buff, container, false) as TextView
        
        // 버프 텍스트 설정
        buffView.text = buff.getShortDisplayText()
        
        // 배경 설정
        val drawable = if (isDefenseBuff) {
            GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.buff_corner_radius)
                setColor(GameConfig.BUFF_DEFENSE_COLOR)
                setStroke(1, GameConfig.BUFF_DEFENSE_STROKE_COLOR)
            }
        } else {
            GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.buff_corner_radius)
                setColor(GameConfig.BUFF_ENEMY_NERF_COLOR)
                setStroke(1, GameConfig.BUFF_ENEMY_NERF_STROKE_COLOR)
            }
        }
        
        buffView.background = drawable
        
        // 컨테이너에 추가
        container.addView(buffView)
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
            
            // 토스트 메시지 표시
            Toast.makeText(
                context,
                "적용된 효과: ${pokerHand.handName}",
                Toast.LENGTH_LONG
            ).show()
        }
        
        dialog.show()
    }
    
    // 포커 족보 효과 적용
    private fun applyPokerHandEffect(pokerHand: PokerHand) {
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
                Toast.makeText(context, "데미지 +1 향상! (비용: $cost)", Toast.LENGTH_SHORT).show()
            } else {
                // 자원 부족
                Toast.makeText(context, "자원이 부족합니다! (필요: $cost)", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "공격속도 +1% 향상! (비용: $cost)", Toast.LENGTH_SHORT).show()
            } else {
                // 자원 부족
                Toast.makeText(context, "자원이 부족합니다! (필요: $cost)", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "공격범위 +5 향상! (비용: $cost)", Toast.LENGTH_SHORT).show()
            } else {
                // 자원 부족
                Toast.makeText(context, "자원이 부족합니다! (필요: $cost)", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "방어력 +20 향상! (비용: $cost)", Toast.LENGTH_SHORT).show()
                closePanel(view.findViewById(R.id.defenseUpgradePanel))
            } else {
                // 자원 부족
                Toast.makeText(context, "자원이 부족합니다! (필요: $cost)", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 다른 버튼들은 아직 구현하지 않음
        defenseUpgrade2.setOnClickListener {
            Toast.makeText(context, "준비 중인 기능입니다", Toast.LENGTH_SHORT).show()
        }
        
        defenseUpgrade3.setOnClickListener {
            Toast.makeText(context, "준비 중인 기능입니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 카드 버튼 설정 - 패널에서 직접 포커 카드 기능 처리
    private fun setupCardButtons(view: View) {
        // 포커 카드 패널 초기화
        pokerCardPanel = PokerCardPanel(view)
        
        // 카드 버튼 - 이제 카드 패널 직접 열기
        val cardButton = view?.findViewById<Button>(R.id.cardButton)
        cardButton?.setOnClickListener {
            // 패널 토글 기능 유지
            togglePanel(view.findViewById(R.id.cardPanel))
        }
        
        // 포커 카드 뽑기 버튼 리스너
        val btnDrawPokerCards = view.findViewById<Button>(R.id.btnDrawPokerCards)
        btnDrawPokerCards?.setOnClickListener {
            // 자원 소모 비용 설정
            val cardDrawCost = 50 // 기본 비용 50 자원
            
            // 현재 자원 확인
            val currentResource = gameView.getResource()
            
            if (currentResource >= cardDrawCost) {
                // 자원 차감
                if (gameView.useResource(cardDrawCost)) {
                    // 자원 차감 성공 시 패널에서 카드 처리 시작
                    pokerCardPanel.startPokerCards(currentWave)
                }
            } else {
                // 자원 부족 메시지
                Toast.makeText(context, "자원이 부족합니다! (필요: $cardDrawCost)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 포커 카드 패널 클래스 (카드 패널 내에서 포커 카드 기능 처리)
    inner class PokerCardPanel(private val rootView: View) {
        // 카드 관련 UI 요소들
        private val cardInfoLayout: LinearLayout = rootView.findViewById(R.id.cardInfoLayout)
        private val btnDrawPokerCards: Button = rootView.findViewById(R.id.btnDrawPokerCards)
        private val replaceButton: Button = rootView.findViewById(R.id.btnReplaceCards)
        private val confirmButton: Button = rootView.findViewById(R.id.btnConfirmHand)
        private val replaceCountText: TextView = rootView.findViewById(R.id.tvReplaceCount)
        private val currentHandText: TextView = rootView.findViewById(R.id.tvCurrentHand)
        private val handDescriptionText: TextView = rootView.findViewById(R.id.tvHandDescription)
        
        private val cardViews: List<androidx.cardview.widget.CardView> = listOf(
            rootView.findViewById(R.id.cardView1),
            rootView.findViewById(R.id.cardView2),
            rootView.findViewById(R.id.cardView3),
            rootView.findViewById(R.id.cardView4),
            rootView.findViewById(R.id.cardView5)
        )
        
        private val cards = mutableListOf<Card>()
        private var replacesLeft = 2 // 교체 가능한 횟수
        private val selectedCardIndexes = mutableSetOf<Int>() // 선택된 카드의 인덱스
        
        init {
            // 카드 선택 이벤트 설정
            cardViews.forEachIndexed { index, cardView ->
                cardView.setOnClickListener {
                    toggleCardSelection(index)
                }
            }
            
            // 교체 버튼 이벤트
            replaceButton.setOnClickListener {
                replaceSelectedCards()
            }
            
            // 확인 버튼 이벤트
            confirmButton.setOnClickListener {
                confirmSelection()
            }
        }
        
        // 포커 카드 시작
        fun startPokerCards(waveNumber: Int) {
            // 상태 초기화
            cards.clear()
            selectedCardIndexes.clear()
            replacesLeft = 2
            
            // UI 초기화
            cardInfoLayout.visibility = View.VISIBLE
            btnDrawPokerCards.visibility = View.GONE
            
            // 카드 생성
            dealCards(waveNumber)
            
            // UI 업데이트
            updateUI()
        }
        
        // 카드 생성
        private fun dealCards(waveNumber: Int) {
            // 웨이브 번호에 따라 더 좋은 카드가 나올 확률 증가
            // 기본 확률 0.15에서 웨이브 번호에 따라 증가
            val goodHandProbability = minOf(0.15f + (waveNumber * 0.03f), 0.5f)
            
            // 좋은 패가 나올 확률 계산
            if (Random.nextFloat() < goodHandProbability) {
                // 좋은 패 생성 (스트레이트 이상)
                generateGoodHand(waveNumber)
            } else {
                // 일반 랜덤 패 생성
                generateRandomHand()
            }
        }
        
        // 랜덤 패 생성
        private fun generateRandomHand() {
            cards.clear()
            
            // 랜덤 카드 5장 생성
            val suits = CardSuit.values().filter { it != CardSuit.JOKER }
            val ranks = CardRank.values().filter { it != CardRank.JOKER }
            
            // 중복 없는 카드 5장 생성
            val usedCards = mutableSetOf<Pair<CardSuit, CardRank>>()
            
            while (cards.size < 5) {
                val suit = suits.random()
                val rank = ranks.random()
                val cardPair = Pair(suit, rank)
                
                if (cardPair !in usedCards) {
                    usedCards.add(cardPair)
                    cards.add(Card(suit, rank))
                }
            }
        }
        
        // 좋은 패 생성
        private fun generateGoodHand(waveNumber: Int) {
            // 웨이브 번호에 따라 더 좋은 족보 가능성 증가
            val handType = when {
                waveNumber >= 8 && Random.nextFloat() < 0.2f -> "royal_flush"
                waveNumber >= 6 && Random.nextFloat() < 0.3f -> "straight_flush"
                waveNumber >= 5 && Random.nextFloat() < 0.4f -> "four_of_a_kind"
                waveNumber >= 4 && Random.nextFloat() < 0.5f -> "full_house"
                waveNumber >= 3 && Random.nextFloat() < 0.6f -> "flush"
                else -> "straight"
            }
            
            cards.clear()
            
            when (handType) {
                "royal_flush" -> {
                    // 로얄 플러시 (스페이드 10, J, Q, K, A)
                    val suit = CardSuit.SPADE
                    cards.add(Card(suit, CardRank.TEN))
                    cards.add(Card(suit, CardRank.JACK))
                    cards.add(Card(suit, CardRank.QUEEN))
                    cards.add(Card(suit, CardRank.KING))
                    cards.add(Card(suit, CardRank.ACE))
                }
                "straight_flush" -> {
                    // 스트레이트 플러시 (같은 무늬의 연속된 숫자)
                    val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                    val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                    
                    for (i in 0 until 5) {
                        val rankValue = startRank + i
                        val rank = CardRank.values().find { it.value == rankValue }
                            ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                            ?: CardRank.ACE
                        
                        cards.add(Card(suit, rank))
                    }
                }
                "four_of_a_kind" -> {
                    // 포카드 (같은 숫자 4장)
                    val rank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                    
                    // 같은 숫자 4장
                    for (i in 0 until 4) {
                        cards.add(Card(suits[i], rank))
                    }
                    
                    // 다른 숫자 1장
                    var otherRank: CardRank
                    do {
                        otherRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    } while (otherRank == rank)
                    
                    cards.add(Card(suits.random(), otherRank))
                }
                "full_house" -> {
                    // 풀하우스 (트리플 + 원페어)
                    val tripleRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    var pairRank: CardRank
                    do {
                        pairRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    } while (pairRank == tripleRank)
                    
                    val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                    
                    // 같은 숫자 3장
                    for (i in 0 until 3) {
                        cards.add(Card(suits[i], tripleRank))
                    }
                    
                    // 다른 같은 숫자 2장
                    for (i in 0 until 2) {
                        cards.add(Card(suits[i], pairRank))
                    }
                }
                "flush" -> {
                    // 플러시 (같은 무늬 5장)
                    val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                    val ranks = CardRank.values().filter { it != CardRank.JOKER }.shuffled().take(5)
                    
                    for (rank in ranks) {
                        cards.add(Card(suit, rank))
                    }
                }
                else -> { // 스트레이트
                    // 스트레이트 (연속된 숫자 5장)
                    val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                    val suits = CardSuit.values().filter { it != CardSuit.JOKER }
                    
                    for (i in 0 until 5) {
                        val rankValue = startRank + i
                        val rank = CardRank.values().find { it.value == rankValue }
                            ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                            ?: CardRank.ACE
                        
                        cards.add(Card(suits.random(), rank))
                    }
                }
            }
            
            // 카드 순서 섞기
            cards.shuffle()
        }
        
        // UI 업데이트
        private fun updateUI() {
            // 카드 이미지 업데이트
            cards.forEachIndexed { index, card ->
                val cardView = cardViews[index]
                
                // 카드 정보 표시
                val suitTextView = cardView.findViewById<TextView>(
                    when (index) {
                        0 -> R.id.tvCardSuit1
                        1 -> R.id.tvCardSuit2
                        2 -> R.id.tvCardSuit3
                        3 -> R.id.tvCardSuit4
                        else -> R.id.tvCardSuit5
                    }
                )
                
                val rankTextView = cardView.findViewById<TextView>(
                    when (index) {
                        0 -> R.id.tvCardRank1
                        1 -> R.id.tvCardRank2
                        2 -> R.id.tvCardRank3
                        3 -> R.id.tvCardRank4
                        else -> R.id.tvCardRank5
                    }
                )
                
                // 카드 무늬와 숫자 설정
                suitTextView.text = card.suit.getSymbol()
                suitTextView.setTextColor(card.suit.getColor())
                
                rankTextView.text = card.rank.getName()
                rankTextView.setTextColor(card.suit.getColor())
                
                // 선택 상태 표시
                cardView.setCardBackgroundColor(
                    if (index in selectedCardIndexes) Color.YELLOW else Color.WHITE
                )
                
                // 카드 선택 가능 여부 설정
                cardView.isEnabled = replacesLeft > 0
            }
            
            // 교체 버튼 활성화/비활성화
            replaceButton.isEnabled = replacesLeft > 0 && selectedCardIndexes.isNotEmpty()
            
            // 교체 횟수 텍스트 업데이트
            replaceCountText.text = "교체 가능 횟수: $replacesLeft"
            
            // 현재 패 평가 및 표시
            val currentHand = PokerHandEvaluator.evaluate(cards)
            currentHandText.text = "현재 족보: ${currentHand.handName}"
            
            // 족보 설명 업데이트
            handDescriptionText.text = "효과: ${currentHand.getDescription()}"
        }
        
        // 카드 선택 토글
        private fun toggleCardSelection(index: Int) {
            // 교체 횟수가 남아있는 경우에만 선택 가능
            if (replacesLeft <= 0) return
            
            if (index in selectedCardIndexes) {
                selectedCardIndexes.remove(index)
            } else {
                selectedCardIndexes.add(index)
            }
            
            updateUI()
        }
        
        // 선택된 카드 교체
        private fun replaceSelectedCards() {
            if (selectedCardIndexes.isEmpty() || replacesLeft <= 0) return
            
            // 선택된 카드 교체
            val suits = CardSuit.values().filter { it != CardSuit.JOKER }
            val ranks = CardRank.values().filter { it != CardRank.JOKER }
            
            // 현재 사용 중인 카드 확인 (중복 방지)
            val usedCards = cards
                .filterIndexed { index, _ -> index !in selectedCardIndexes }
                .map { Pair(it.suit, it.rank) }
                .toMutableSet()
            
            // 선택된 카드 교체
            for (index in selectedCardIndexes) {
                var newCard: Card
                do {
                    val suit = suits.random()
                    val rank = ranks.random()
                    val cardPair = Pair(suit, rank)
                    
                    if (cardPair !in usedCards) {
                        newCard = Card(suit, rank)
                        usedCards.add(cardPair)
                        break
                    }
                } while (true)
                
                cards[index] = newCard
            }
            
            // 교체 횟수 감소
            replacesLeft--
            
            // 선택 초기화
            selectedCardIndexes.clear()
            
            // UI 업데이트
            updateUI()
        }
        
        // 카드 선택 확정
        private fun confirmSelection() {
            // 현재 패 평가 결과 전달
            val pokerHand = PokerHandEvaluator.evaluate(cards)
            applyPokerHandEffect(pokerHand)
            
            // 토스트 메시지 표시
            Toast.makeText(
                context,
                "적용된 효과: ${pokerHand.handName}",
                Toast.LENGTH_LONG
            ).show()
            
            // 패널 초기 상태로 복귀
            resetPanel()
        }
        
        // 패널 초기 상태로 복귀
        private fun resetPanel() {
            cardInfoLayout.visibility = View.GONE
            btnDrawPokerCards.visibility = View.VISIBLE
            
            // 추가로 패널 닫기를 원한다면 아래 코드 활성화
            // closePanel(rootView.findViewById(R.id.cardPanel))
        }
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
            Toast.makeText(context, "코인이 부족합니다!", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // 코인 저장
    private fun saveCoins() {
        MainMenuFragment.saveCoins(requireContext(), coins)
    }
}

