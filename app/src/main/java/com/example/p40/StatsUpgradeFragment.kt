package com.example.p40

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.GameConfig
import com.example.p40.MessageManager

class StatsUpgradeFragment : Fragment(R.layout.fragment_stats_upgrade) {

    private lateinit var userManager: UserManager
    private lateinit var statsManager: StatsManager
    private lateinit var messageManager: MessageManager
    private lateinit var tvCoinAmount: TextView
    
    // 현재 스탯 표시 텍스트뷰
    private lateinit var tvCurrentHealth: TextView
    private lateinit var tvCurrentAttack: TextView
    private lateinit var tvCurrentAttackSpeed: TextView
    private lateinit var tvCurrentRange: TextView
    
    // 스탯 강화 정보 텍스트뷰
    private lateinit var tvHealthUpgradeInfo: TextView
    private lateinit var tvAttackUpgradeInfo: TextView
    private lateinit var tvAttackSpeedUpgradeInfo: TextView
    private lateinit var tvRangeUpgradeInfo: TextView
    
    // 스탯 강화 비용 텍스트뷰
    private lateinit var tvHealthUpgradeCost: TextView
    private lateinit var tvAttackUpgradeCost: TextView
    private lateinit var tvAttackSpeedUpgradeCost: TextView
    private lateinit var tvRangeUpgradeCost: TextView
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager와 StatsManager 초기화
        userManager = UserManager.getInstance(requireContext())
        statsManager = StatsManager.getInstance(requireContext())
        messageManager = MessageManager.getInstance()
        messageManager.init(view.findViewById(android.R.id.content) ?: view as ViewGroup)
        
        // UI 요소 초기화
        initViews(view)
        
        // 현재 스탯 정보 표시
        updateStatsUI()
        
        // 코인 정보 업데이트
        updateCoinUI()
        
        // 뒤로가기 버튼 설정
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
        
        // 강화 버튼 이벤트 설정
        setupUpgradeButtons(view)
    }
    
    // UI 요소 초기화
    private fun initViews(view: View) {
        // 코인 정보 텍스트뷰
        tvCoinAmount = view.findViewById(R.id.tvCoinAmount)
        
        // 현재 스탯 텍스트뷰
        tvCurrentHealth = view.findViewById(R.id.tvCurrentHealth)
        tvCurrentAttack = view.findViewById(R.id.tvCurrentAttack)
        tvCurrentAttackSpeed = view.findViewById(R.id.tvCurrentAttackSpeed)
        tvCurrentRange = view.findViewById(R.id.tvCurrentRange)
        
        // 스탯 강화 정보 텍스트뷰
        tvHealthUpgradeInfo = view.findViewById(R.id.tvHealthUpgradeInfo)
        tvAttackUpgradeInfo = view.findViewById(R.id.tvAttackUpgradeInfo)
        tvAttackSpeedUpgradeInfo = view.findViewById(R.id.tvAttackSpeedUpgradeInfo)
        tvRangeUpgradeInfo = view.findViewById(R.id.tvRangeUpgradeInfo)
        
        // 스탯 강화 비용 텍스트뷰
        tvHealthUpgradeCost = view.findViewById(R.id.tvHealthUpgradeCost)
        tvAttackUpgradeCost = view.findViewById(R.id.tvAttackUpgradeCost)
        tvAttackSpeedUpgradeCost = view.findViewById(R.id.tvAttackSpeedUpgradeCost)
        tvRangeUpgradeCost = view.findViewById(R.id.tvRangeUpgradeCost)
    }
    
    // 강화 버튼 설정
    private fun setupUpgradeButtons(view: View) {
        // 체력 강화 버튼
        view.findViewById<Button>(R.id.btnUpgradeHealth).setOnClickListener {
            upgradeHealth()
        }
        
        // 공격력 강화 버튼
        view.findViewById<Button>(R.id.btnUpgradeAttack).setOnClickListener {
            upgradeAttack()
        }
        
        // 공격 속도 강화 버튼
        view.findViewById<Button>(R.id.btnUpgradeAttackSpeed).setOnClickListener {
            upgradeAttackSpeed()
        }
        
        // 사거리 강화 버튼
        view.findViewById<Button>(R.id.btnUpgradeRange).setOnClickListener {
            upgradeRange()
        }
    }
    
    // 현재 스탯 정보 업데이트
    private fun updateStatsUI() {
        // 현재 스탯 표시
        tvCurrentHealth.text = statsManager.getHealth().toString()
        tvCurrentAttack.text = statsManager.getAttack().toString()
        
        // 공격 속도를 초당 횟수가 아닌 공격 간격(ms)으로 표시
        val attackSpeedInMs = (1000 / statsManager.getAttackSpeed()).toInt()
        tvCurrentAttackSpeed.text = "${attackSpeedInMs}ms"
        
        tvCurrentRange.text = statsManager.getRange().toString()
        
        // 현재 레벨 정보
        val healthLevel = statsManager.getHealthLevel()
        val attackLevel = statsManager.getAttackLevel()
        val attackSpeedLevel = statsManager.getAttackSpeedLevel()
        val rangeLevel = statsManager.getRangeLevel()
        
        // 강화 정보 표시 (현재 레벨/최대 레벨 표시 추가)
        tvHealthUpgradeInfo.text = "체력 +${GameConfig.STATS_HEALTH_UPGRADE_AMOUNT} (Lv.${healthLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvAttackUpgradeInfo.text = "공격력 +${GameConfig.STATS_ATTACK_UPGRADE_AMOUNT} (Lv.${attackLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvAttackSpeedUpgradeInfo.text = "공격 속도 -${GameConfig.STATS_ATTACK_SPEED_UPGRADE_AMOUNT}ms (Lv.${attackSpeedLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvRangeUpgradeInfo.text = "사거리 +${GameConfig.STATS_RANGE_UPGRADE_AMOUNT} (Lv.${rangeLevel}/${GameConfig.STATS_MAX_LEVEL})"
        
        // 강화 비용 표시
        val healthCost = statsManager.getHealthUpgradeCost()
        val attackCost = statsManager.getAttackUpgradeCost()
        val attackSpeedCost = statsManager.getAttackSpeedUpgradeCost()
        val rangeCost = statsManager.getRangeUpgradeCost()
        
        tvHealthUpgradeCost.text = "코인: $healthCost"
        tvAttackUpgradeCost.text = "코인: $attackCost"
        tvAttackSpeedUpgradeCost.text = "코인: $attackSpeedCost"
        tvRangeUpgradeCost.text = "코인: $rangeCost"
    }
    
    // 코인 정보 업데이트
    private fun updateCoinUI() {
        tvCoinAmount.text = "코인: ${userManager.getCoin()}"
    }
    
    // 체력 강화
    private fun upgradeHealth() {
        val cost = statsManager.getHealthUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getHealthLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 체력 증가
            val success = statsManager.upgradeHealth()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            if (success) {
                messageManager.showSuccess("체력이 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    // 공격력 강화
    private fun upgradeAttack() {
        val cost = statsManager.getAttackUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getAttackLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 공격력 증가
            val success = statsManager.upgradeAttack()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            if (success) {
                messageManager.showSuccess("공격력이 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    // 공격 속도 강화
    private fun upgradeAttackSpeed() {
        val cost = statsManager.getAttackSpeedUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getAttackSpeedLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 공격 속도 증가
            val success = statsManager.upgradeAttackSpeed()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            if (success) {
                messageManager.showSuccess("공격 속도가 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    // 사거리 강화
    private fun upgradeRange() {
        val cost = statsManager.getRangeUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getRangeLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 사거리 증가
            val success = statsManager.upgradeRange()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            if (success) {
                messageManager.showSuccess("사거리가 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 코인 정보 갱신
        updateCoinUI()
    }
} 