package com.example.p40

import android.animation.ObjectAnimator
import android.app.Dialog
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

class GameFragment : Fragment(R.layout.fragment_game), GameOverListener {

    private lateinit var gameView: GameView
    private var isPaused = false
    private var cardCooldown = false
    private val cardCooldownTime = GameConfig.CARD_COOLDOWN
    
    // 현재 열려있는 패널 추적
    private var currentOpenPanel: LinearLayout? = null
    
    // 현재 웨이브 정보
    private var currentWave = 1
    
    // 버프 정보 UI
    private lateinit var tvBuffList: TextView
    
    // UI 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateGameInfoUI()
            updateBuffUI()
            updateUnitStatsUI()
            handler.postDelayed(this, 500) // 500ms마다 업데이트
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 게임 뷰 초기화
        gameView = view.findViewById(R.id.gameView)
        
        // 게임 오버 리스너 설정
        gameView.setGameOverListener(this)
        
        // 버프 UI 초기화
        initBuffUI(view)
        
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
        
        // 일시정지 버튼
        val pauseButton = view.findViewById<Button>(R.id.pauseButton)
        pauseButton.setOnClickListener {
            isPaused = !isPaused
            if (isPaused) {
                pauseButton.text = "재개"
                gameView.pause()
                handler.removeCallbacks(uiUpdateRunnable)
            } else {
                pauseButton.text = "일시정지"
                gameView.resume()
                handler.post(uiUpdateRunnable)
            }
        }
        
        // 종료 버튼
        val exitButton = view.findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            // 게임 종료 및 메인 메뉴로 돌아가기
            gameView.pause() // 먼저 게임 일시정지
            handler.removeCallbacks(uiUpdateRunnable)
            findNavController().navigate(R.id.action_gameFragment_to_mainMenuFragment)
        }
        
        // 공격 업그레이드 패널 버튼 설정
        setupAttackUpgradeButtons(view)
        
        // 방어 업그레이드 패널 버튼 설정
        setupDefenseUpgradeButtons(view)
        
        // 카드 패널 버튼 설정
        setupCardButtons(view)
        
        // UI 업데이트 시작
        handler.post(uiUpdateRunnable)
        
        // 업그레이드 버튼 텍스트 초기화 (UI 업데이트 후에 실행)
        handler.post {
            updateUpgradeButtonsText()
        }
    }
    
    // 버프 UI 초기화
    private fun initBuffUI(view: View) {
        tvBuffList = view.findViewById(R.id.tvBuffList)
    }
    
    // 게임 정보 UI 업데이트
    private fun updateGameInfoUI() {
        if (!::gameView.isInitialized || !isAdded) return
        
        // 게임 상태 정보 가져오기
        val resource = gameView.getResource()
        val waveCount = gameView.getWaveCount()
        val killCount = gameView.getKillCount()
        val totalEnemies = gameView.getTotalEnemiesInWave()
        
        // 현재 웨이브의 적 데미지 계산
        val normalEnemyDamage = GameConfig.getEnemyDamageForWave(waveCount, false)
        val bossDamage = GameConfig.getEnemyDamageForWave(waveCount, true)
        
        // 게임 정보 업데이트
        view?.findViewById<TextView>(R.id.tvGameInfo)?.text = 
            "자원: $resource  웨이브: $waveCount  처치: $killCount/$totalEnemies  적 데미지: $normalEnemyDamage/보스: $bossDamage"
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
        
        // 유닛 스탯 정보 업데이트
        view?.findViewById<TextView>(R.id.tvUnitStats)?.text = 
            "체력: $health/$maxHealth  |  공격력: $attack  |  공격속도: ${String.format("%.2f", attacksPerSecond)}/초  |  범위: ${attackRange.toInt()}"
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
        
        // 디펜스 유닛 버프 먼저 추가
        for (buff in defenseBuffs) {
            addBuffItem(buffContainer, buff, true)
        }
        
        // 적 너프 추가
        for (buff in enemyNerfs) {
            addBuffItem(buffContainer, buff, false)
        }
        
        // 버프가 없으면 "버프 없음" 텍스트뷰 표시
        if (defenseBuffs.isEmpty() && enemyNerfs.isEmpty()) {
            val noBuff = TextView(context)
            noBuff.text = "버프 없음"
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
        
        tvGameOverScore.text = "최종 자원: $resource"
        tvGameOverWave.text = "도달한 웨이브: $waveCount"
        
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
    }
}
