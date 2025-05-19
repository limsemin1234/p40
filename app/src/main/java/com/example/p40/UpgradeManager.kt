package com.example.p40

import android.content.Context
import android.view.View
import android.widget.Button
import com.example.p40.CardSymbolType
import com.example.p40.GameConfig
import com.example.p40.GameView
import com.example.p40.MessageManager

/**
 * 업그레이드 관련 기능을 처리하는 매니저 클래스
 * GameFragment에서 업그레이드 관련 로직을 분리했습니다.
 */
class UpgradeManager(
    private val context: Context,
    private val gameView: GameView,
    private val messageManager: MessageManager,
    private val rootView: View
) {
    /**
     * 공격 업그레이드 버튼 설정
     */
    fun setupAttackUpgradeButtons() {
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
    fun setupDefenseUpgradeButtons() {
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
        
        // 가시 데미지 업그레이드 버튼
        defenseUpgrade2.setOnClickListener {
            val cost = gameView.getThornDamageCost()
            if (gameView.upgradeThornDamage()) {
                // 업그레이드 성공
                messageManager.showSuccess("가시 데미지가 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족 또는 최대 레벨
                if (gameView.getThornDamageLevel() >= GameConfig.THORN_DAMAGE_UPGRADE_MAX_LEVEL) {
                    messageManager.showInfo("이미 최대 레벨입니다.")
                } else {
                    messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
                }
            }
        }
        
        // 밀치기 업그레이드 버튼
        defenseUpgrade3.setOnClickListener {
            val cost = gameView.getPushDistanceCost()
            if (gameView.upgradePushDistance()) {
                // 업그레이드 성공
                messageManager.showSuccess("밀치기 거리가 강화되었습니다!")
                updateUpgradeButtonsText() // 모든 버튼 텍스트 갱신
            } else {
                // 자원 부족 또는 최대 레벨
                if (gameView.getPushDistanceLevel() >= GameConfig.PUSH_DISTANCE_UPGRADE_MAX_LEVEL) {
                    messageManager.showInfo("이미 최대 레벨입니다.")
                } else {
                    messageManager.showWarning("자원이 부족합니다! (필요: $cost)")
                }
            }
        }
    }
    
    /**
     * 모든 업그레이드 버튼 텍스트 업데이트
     */
    fun updateUpgradeButtonsText() {
        // 업그레이드 버튼 활성화 상태를 위한 설정 제거 (모든 문양에서 업그레이드 가능)
        // val isSpadeSymbol = gameView.getDefenseUnit()?.getSymbolType() == CardSymbolType.SPADE
        
        // 데미지 업그레이드 버튼
        val btnUpgradeDamage = rootView.findViewById<Button>(R.id.btnUpgradeDamage)
        val damageLevel = gameView.getDamageLevel()
        if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
            btnUpgradeDamage.text = "데미지\n최대 레벨\n(Lv.${damageLevel}/${GameConfig.DAMAGE_UPGRADE_MAX_LEVEL})"
            btnUpgradeDamage.isEnabled = false
        } else {
            btnUpgradeDamage.text = "데미지 +${GameConfig.DAMAGE_UPGRADE_VALUE}\n💰 ${gameView.getDamageCost()} 자원\n(Lv.${damageLevel}/${GameConfig.DAMAGE_UPGRADE_MAX_LEVEL})"
            btnUpgradeDamage.isEnabled = true // 모든 문양에서 업그레이드 가능
        }
        
        // 공격속도 업그레이드 버튼
        val btnUpgradeAttackSpeed = rootView.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
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
            btnUpgradeAttackSpeed.isEnabled = true // 모든 문양에서 업그레이드 가능
        }
        
        // 공격범위 업그레이드 버튼
        val btnUpgradeAttackRange = rootView.findViewById<Button>(R.id.btnUpgradeAttackRange)
        val attackRangeLevel = gameView.getAttackRangeLevel()
        if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
            btnUpgradeAttackRange.text = "공격범위\n최대 레벨\n(Lv.${attackRangeLevel}/${GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL})"
            btnUpgradeAttackRange.isEnabled = false
        } else {
            btnUpgradeAttackRange.text = "공격범위 +${GameConfig.ATTACK_RANGE_UPGRADE_VALUE.toInt()}\n💰 ${gameView.getAttackRangeCost()} 자원\n(Lv.${attackRangeLevel}/${GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL})"
            btnUpgradeAttackRange.isEnabled = true // 모든 문양에서 업그레이드 가능
        }
        
        // 방어력 업그레이드 버튼
        val defenseUpgrade1 = rootView.findViewById<Button>(R.id.defenseUpgrade1)
        val defenseLevel = gameView.getDefenseLevel()
        if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
            defenseUpgrade1.text = "체력\n최대 레벨\n(Lv.${defenseLevel}/${GameConfig.DEFENSE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade1.isEnabled = false
        } else {
            defenseUpgrade1.text = "체력 +${GameConfig.DEFENSE_UPGRADE_VALUE}\n💰 ${gameView.getDefenseCost()} 자원\n(Lv.${defenseLevel}/${GameConfig.DEFENSE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade1.isEnabled = true // 모든 문양에서 업그레이드 가능
        }
        
        // 준비 중인 기능 버튼 텍스트 업데이트
        val defenseUpgrade2 = rootView.findViewById<Button>(R.id.defenseUpgrade2)
        val thornDamageLevel = gameView.getThornDamageLevel()
        if (thornDamageLevel >= GameConfig.THORN_DAMAGE_UPGRADE_MAX_LEVEL) {
            defenseUpgrade2.text = "가시데미지\n최대 레벨\n(Lv.${thornDamageLevel}/${GameConfig.THORN_DAMAGE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade2.isEnabled = false
        } else {
            defenseUpgrade2.text = "가시데미지 +${GameConfig.THORN_DAMAGE_UPGRADE_VALUE}\n💰 ${gameView.getThornDamageCost()} 자원\n(Lv.${thornDamageLevel}/${GameConfig.THORN_DAMAGE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade2.isEnabled = true
        }
        
        val defenseUpgrade3 = rootView.findViewById<Button>(R.id.defenseUpgrade3)
        val pushDistanceLevel = gameView.getPushDistanceLevel()
        if (pushDistanceLevel >= GameConfig.PUSH_DISTANCE_UPGRADE_MAX_LEVEL) {
            defenseUpgrade3.text = "밀치기\n최대 레벨\n(Lv.${pushDistanceLevel}/${GameConfig.PUSH_DISTANCE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade3.isEnabled = false
        } else {
            defenseUpgrade3.text = "밀치기 +${GameConfig.PUSH_DISTANCE_UPGRADE_VALUE}\n💰 ${gameView.getPushDistanceCost()} 자원\n(Lv.${pushDistanceLevel}/${GameConfig.PUSH_DISTANCE_UPGRADE_MAX_LEVEL})"
            defenseUpgrade3.isEnabled = true
        }
    }
    
    /**
     * 메모리 누수 방지를 위한 참조 정리
     * Fragment의 onDestroyView에서 호출해야 함
     */
    fun clearReferences() {
        // 버튼 클릭 리스너 제거
        try {
            val btnUpgradeDamage = rootView.findViewById<Button>(R.id.btnUpgradeDamage)
            val btnUpgradeAttackSpeed = rootView.findViewById<Button>(R.id.btnUpgradeAttackSpeed)
            val btnUpgradeAttackRange = rootView.findViewById<Button>(R.id.btnUpgradeAttackRange)
            val defenseUpgrade1 = rootView.findViewById<Button>(R.id.defenseUpgrade1)
            val defenseUpgrade2 = rootView.findViewById<Button>(R.id.defenseUpgrade2)
            val defenseUpgrade3 = rootView.findViewById<Button>(R.id.defenseUpgrade3)
            
            // 모든 버튼의 클릭 리스너 제거
            btnUpgradeDamage?.setOnClickListener(null)
            btnUpgradeAttackSpeed?.setOnClickListener(null)
            btnUpgradeAttackRange?.setOnClickListener(null)
            defenseUpgrade1?.setOnClickListener(null)
            defenseUpgrade2?.setOnClickListener(null)
            defenseUpgrade3?.setOnClickListener(null)
        } catch (e: Exception) {
            // 예외 처리 - 이미 버튼이 제거되었을 수 있음
            e.printStackTrace()
        }
    }
} 