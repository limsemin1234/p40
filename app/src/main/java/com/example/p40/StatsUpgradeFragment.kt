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

class StatsUpgradeFragment : BaseFragment(R.layout.fragment_stats_upgrade) {

    private lateinit var statsManager: StatsManager
    private lateinit var messageManager: MessageManager
    
    // 현재 스탯 표시 텍스트뷰
    private lateinit var tvCurrentHealth: TextView
    private lateinit var tvCurrentAttack: TextView
    private lateinit var tvCurrentAttackSpeed: TextView
    private lateinit var tvCurrentRange: TextView
    private lateinit var tvCurrentThornDamage: TextView
    private lateinit var tvCurrentPushDistance: TextView
    
    // 스탯 강화 정보 텍스트뷰
    private lateinit var tvHealthUpgradeInfo: TextView
    private lateinit var tvAttackUpgradeInfo: TextView
    private lateinit var tvAttackSpeedUpgradeInfo: TextView
    private lateinit var tvRangeUpgradeInfo: TextView
    private lateinit var tvThornDamageUpgradeInfo: TextView
    private lateinit var tvPushDistanceUpgradeInfo: TextView
    
    // 스탯 강화 비용 텍스트뷰
    private lateinit var tvHealthUpgradeCost: TextView
    private lateinit var tvAttackUpgradeCost: TextView
    private lateinit var tvAttackSpeedUpgradeCost: TextView
    private lateinit var tvRangeUpgradeCost: TextView
    private lateinit var tvThornDamageUpgradeCost: TextView
    private lateinit var tvPushDistanceUpgradeCost: TextView
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // StatsManager 초기화
        statsManager = StatsManager.getInstance(requireContext())
        messageManager = MessageManager.getInstance()
        messageManager.init(view.findViewById(android.R.id.content) ?: view as ViewGroup)
        
        // UI 요소 초기화
        initViews(view)
        
        // 현재 스탯 정보 표시
        updateStatsUI()
        
        // 강화 버튼 이벤트 설정
        setupUpgradeButtons(view)
    }
    
    // 메모리 누수 방지를 위한 onDestroy 추가
    override fun onDestroy() {
        super.onDestroy()
        // MessageManager 정리
        messageManager.clear()
    }
    
    // UI 요소 초기화
    private fun initViews(view: View) {
        // 현재 스탯 텍스트뷰
        tvCurrentHealth = view.findViewById(R.id.tvCurrentHealth)
        tvCurrentAttack = view.findViewById(R.id.tvCurrentAttack)
        tvCurrentAttackSpeed = view.findViewById(R.id.tvCurrentAttackSpeed)
        tvCurrentRange = view.findViewById(R.id.tvCurrentRange)
        tvCurrentThornDamage = view.findViewById(R.id.tvCurrentThornDamage)
        tvCurrentPushDistance = view.findViewById(R.id.tvCurrentPushDistance)
        
        // 스탯 강화 정보 텍스트뷰
        tvHealthUpgradeInfo = view.findViewById(R.id.tvHealthUpgradeInfo)
        tvAttackUpgradeInfo = view.findViewById(R.id.tvAttackUpgradeInfo)
        tvAttackSpeedUpgradeInfo = view.findViewById(R.id.tvAttackSpeedUpgradeInfo)
        tvRangeUpgradeInfo = view.findViewById(R.id.tvRangeUpgradeInfo)
        tvThornDamageUpgradeInfo = view.findViewById(R.id.tvThornDamageUpgradeInfo)
        tvPushDistanceUpgradeInfo = view.findViewById(R.id.tvPushDistanceUpgradeInfo)
        
        // 스탯 강화 비용 텍스트뷰
        tvHealthUpgradeCost = view.findViewById(R.id.tvHealthUpgradeCost)
        tvAttackUpgradeCost = view.findViewById(R.id.tvAttackUpgradeCost)
        tvAttackSpeedUpgradeCost = view.findViewById(R.id.tvAttackSpeedUpgradeCost)
        tvRangeUpgradeCost = view.findViewById(R.id.tvRangeUpgradeCost)
        tvThornDamageUpgradeCost = view.findViewById(R.id.tvThornDamageUpgradeCost)
        tvPushDistanceUpgradeCost = view.findViewById(R.id.tvPushDistanceUpgradeCost)
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
        
        // 가시데미지 강화 버튼 추가
        view.findViewById<Button>(R.id.btnUpgradeThornDamage).setOnClickListener {
            upgradeThornDamage()
        }
        
        // 밀치기 강화 버튼 추가
        view.findViewById<Button>(R.id.btnUpgradePushDistance).setOnClickListener {
            upgradePushDistance()
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
        tvCurrentThornDamage.text = statsManager.getThornDamage().toString()
        tvCurrentPushDistance.text = statsManager.getPushDistance().toString()
        
        // 현재 레벨 정보
        val healthLevel = statsManager.getHealthLevel()
        val attackLevel = statsManager.getAttackLevel()
        val attackSpeedLevel = statsManager.getAttackSpeedLevel()
        val rangeLevel = statsManager.getRangeLevel()
        val thornDamageLevel = statsManager.getThornDamageLevel()
        val pushDistanceLevel = statsManager.getPushDistanceLevel()
        
        // 강화 정보 표시 (현재 레벨/최대 레벨 표시 추가)
        tvHealthUpgradeInfo.text = "체력 +${GameConfig.STATS_HEALTH_UPGRADE_AMOUNT} (Lv.${healthLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvAttackUpgradeInfo.text = "공격력 +${GameConfig.STATS_ATTACK_UPGRADE_AMOUNT} (Lv.${attackLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvAttackSpeedUpgradeInfo.text = "공격 속도 -${GameConfig.STATS_ATTACK_SPEED_UPGRADE_AMOUNT}ms (Lv.${attackSpeedLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvRangeUpgradeInfo.text = "사거리 +${GameConfig.STATS_RANGE_UPGRADE_AMOUNT} (Lv.${rangeLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvThornDamageUpgradeInfo.text = "가시데미지 +1 (Lv.${thornDamageLevel}/${GameConfig.STATS_MAX_LEVEL})"
        tvPushDistanceUpgradeInfo.text = "밀치기 +0.1 (Lv.${pushDistanceLevel}/${GameConfig.STATS_MAX_LEVEL})"
        
        // 강화 비용 표시
        val healthCost = statsManager.getHealthUpgradeCost()
        val attackCost = statsManager.getAttackUpgradeCost()
        val attackSpeedCost = statsManager.getAttackSpeedUpgradeCost()
        val rangeCost = statsManager.getRangeUpgradeCost()
        val thornDamageCost = statsManager.getThornDamageUpgradeCost()
        val pushDistanceCost = statsManager.getPushDistanceUpgradeCost()
        
        tvHealthUpgradeCost.text = "코인: $healthCost"
        tvAttackUpgradeCost.text = "코인: $attackCost"
        tvAttackSpeedUpgradeCost.text = "코인: $attackSpeedCost"
        tvRangeUpgradeCost.text = "코인: $rangeCost"
        tvThornDamageUpgradeCost.text = "코인: $thornDamageCost"
        tvPushDistanceUpgradeCost.text = "코인: $pushDistanceCost"
        
        // 코인 정보 업데이트 (BaseFragment의 메서드 활용)
        updateCoinInfo(requireView())
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
            
            if (success) {
                messageManager.showSuccess("사거리가 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    // 가시데미지 강화
    private fun upgradeThornDamage() {
        val cost = statsManager.getThornDamageUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getThornDamageLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 가시데미지 증가
            val success = statsManager.upgradeThornDamage()
            
            // UI 업데이트
            updateStatsUI()
            
            if (success) {
                messageManager.showSuccess("가시데미지가 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    // 밀치기 강화
    private fun upgradePushDistance() {
        val cost = statsManager.getPushDistanceUpgradeCost()
        
        // 최대 레벨 체크
        if (statsManager.getPushDistanceLevel() >= GameConfig.STATS_MAX_LEVEL) {
            messageManager.showWarning("이미 최대 레벨에 도달했습니다!")
            return
        }
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 밀치기 증가
            val success = statsManager.upgradePushDistance()
            
            // UI 업데이트
            updateStatsUI()
            
            if (success) {
                messageManager.showSuccess("밀치기가 강화되었습니다!")
            } else {
                messageManager.showWarning("최대 레벨에 도달했습니다!")
            }
        } else {
            messageManager.showWarning("코인이 부족합니다!")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 스탯 정보 갱신
        updateStatsUI()
    }
} 