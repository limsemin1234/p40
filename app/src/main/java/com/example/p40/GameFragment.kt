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

class GameFragment : Fragment(R.layout.fragment_game), GameOverListener {

    private lateinit var gameView: GameView
    private var isPaused = false
    private var cardCooldown = false
    private val cardCooldownTime = GameConfig.CARD_COOLDOWN
    
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
        
        // 카드 패널 버튼 설정
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
        // 실제로는 GameView에 웨이브 완료 콜백이 필요하지만, 
        // 현재 구현에서는 임시로 버튼에 리스너 연결하여 테스트
        val cardButton = view?.findViewById<Button>(R.id.cardButton)
        cardButton?.setOnLongClickListener {
            // 테스트용: 카드 버튼 길게 누르면 웨이브 완료 처리 (실제 게임에서는 제거)
            onWaveCompleted(currentWave)
            currentWave++
            true
        }
        
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
            
            // 게임 재개
            gameView.resume()
            handler.post(uiUpdateRunnable)
            
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
    
    private fun setupCardButtons(view: View) {
        val card1 = view.findViewById<Button>(R.id.card1)
        val card2 = view.findViewById<Button>(R.id.card2)
        val card3 = view.findViewById<Button>(R.id.card3)
        
        card1.setOnClickListener {
            if (!cardCooldown) {
                gameView.useCard()
                setCardCooldown()
                Toast.makeText(context, "공격 카드 사용!", Toast.LENGTH_SHORT).show()
                closePanel(view.findViewById(R.id.cardPanel))
            } else {
                Toast.makeText(context, "카드가 아직 쿨다운 중입니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        card2.setOnClickListener {
            if (!cardCooldown) {
                // 방어 카드 기능은 아직 미구현
                Toast.makeText(context, "방어 카드 사용!", Toast.LENGTH_SHORT).show()
                setCardCooldown()
                closePanel(view.findViewById(R.id.cardPanel))
            } else {
                Toast.makeText(context, "카드가 아직 쿨다운 중입니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        card3.setOnClickListener {
            if (!cardCooldown) {
                // 특수 카드 기능은 아직 미구현
                Toast.makeText(context, "특수 카드 사용!", Toast.LENGTH_SHORT).show()
                setCardCooldown()
                closePanel(view.findViewById(R.id.cardPanel))
            } else {
                Toast.makeText(context, "카드가 아직 쿨다운 중입니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setCardCooldown() {
        cardCooldown = true
        view?.findViewById<Button>(R.id.cardButton)?.isEnabled = false
        view?.findViewById<Button>(R.id.cardButton)?.text = "쿨다운"
        
        view?.postDelayed({
            cardCooldown = false
            view?.findViewById<Button>(R.id.cardButton)?.isEnabled = true
            view?.findViewById<Button>(R.id.cardButton)?.text = "카드"
            if (isAdded) {
                Toast.makeText(context, "카드가 다시 사용 가능합니다!", Toast.LENGTH_SHORT).show()
            }
        }, cardCooldownTime)
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
