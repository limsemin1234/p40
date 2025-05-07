package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.game.GameConfig

class StatsUpgradeFragment : Fragment(R.layout.fragment_stats_upgrade) {

    private lateinit var userManager: UserManager
    private lateinit var statsManager: StatsManager
    private lateinit var tvCurrency: TextView
    
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
        tvCurrency = view.findViewById(R.id.tvCurrency)
        
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
        tvCurrentAttackSpeed.text = String.format("%.1f", statsManager.getAttackSpeed())
        tvCurrentRange.text = statsManager.getRange().toString()
        
        // 강화 정보 표시
        tvHealthUpgradeInfo.text = "체력 +${GameConfig.STATS_HEALTH_UPGRADE_AMOUNT}"
        tvAttackUpgradeInfo.text = "공격력 +${GameConfig.STATS_ATTACK_UPGRADE_AMOUNT}"
        tvAttackSpeedUpgradeInfo.text = "공격 속도 +${GameConfig.STATS_ATTACK_SPEED_UPGRADE_AMOUNT}"
        tvRangeUpgradeInfo.text = "사거리 +${GameConfig.STATS_RANGE_UPGRADE_AMOUNT}"
        
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
        tvCurrency.text = "코인: ${userManager.getCoin()}"
    }
    
    // 체력 강화
    private fun upgradeHealth() {
        val cost = statsManager.getHealthUpgradeCost()
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 체력 증가
            statsManager.upgradeHealth()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            Toast.makeText(requireContext(), "체력이 강화되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "코인이 부족합니다!", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 공격력 강화
    private fun upgradeAttack() {
        val cost = statsManager.getAttackUpgradeCost()
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 공격력 증가
            statsManager.upgradeAttack()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            Toast.makeText(requireContext(), "공격력이 강화되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "코인이 부족합니다!", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 공격 속도 강화
    private fun upgradeAttackSpeed() {
        val cost = statsManager.getAttackSpeedUpgradeCost()
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 공격 속도 증가
            statsManager.upgradeAttackSpeed()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            Toast.makeText(requireContext(), "공격 속도가 강화되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "코인이 부족합니다!", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 사거리 강화
    private fun upgradeRange() {
        val cost = statsManager.getRangeUpgradeCost()
        
        if (userManager.getCoin() >= cost) {
            // 코인 차감
            userManager.decreaseCoin(cost)
            
            // 사거리 증가
            statsManager.upgradeRange()
            
            // UI 업데이트
            updateStatsUI()
            updateCoinUI()
            
            Toast.makeText(requireContext(), "사거리가 강화되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "코인이 부족합니다!", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 코인 정보 갱신
        updateCoinUI()
    }
} 