package com.example.p40.game

import java.util.concurrent.ConcurrentHashMap

/**
 * 버프 종류 정의
 */
enum class BuffType {
    MISSILE_DAMAGE, // 미사일 데미지 증가
    ENEMY_SLOW,     // 적 이동 속도 감소
    ATTACK_SPEED,   // 공격 속도 증가
    MISSILE_SPEED,  // 미사일 속도 증가
    MISSILE_RANGE,  // 미사일 사거리 증가
    MISSILE_PIERCE, // 미사일 관통
    DOT_DAMAGE,     // 지속적 데미지
    MULTI_DIRECTION,// 다방향 발사
    MASS_DAMAGE     // 주기적 대량 데미지
}

/**
 * 버프 카테고리 정의
 */
enum class BuffCategory {
    DEFENSE_BUFF, // 디펜스 유닛 강화 버프
    ENEMY_NERF    // 적 약화 너프
}

/**
 * 버프 정보
 */
data class Buff(
    val type: BuffType,
    var level: Int = 1,
    val displayName: String,
    val shortName: String, // 축약된 이름
    val category: BuffCategory, // 버프 카테고리
    val increasePerLevel: Float // 레벨당 효과 증가량
) {
    fun getEffectValue(): Float {
        return increasePerLevel * level
    }
    
    fun getDisplayText(): String {
        return when (type) {
            BuffType.MISSILE_DAMAGE -> "$displayName Lv.$level (${(increasePerLevel * level * 100).toInt()}%)"
            BuffType.ENEMY_SLOW -> "$displayName Lv.$level (${(increasePerLevel * level * 100).toInt()}%)"
            BuffType.ATTACK_SPEED -> "$displayName Lv.$level (${(increasePerLevel * level * 100).toInt()}%)"
            BuffType.MISSILE_SPEED -> "$displayName Lv.$level (${(increasePerLevel * level * 100).toInt()}%)"
            BuffType.MISSILE_RANGE -> "$displayName Lv.$level (${(increasePerLevel * level * 100).toInt()}%)"
            BuffType.MISSILE_PIERCE -> "$displayName Lv.$level (${level + 1}개 관통)"
            BuffType.DOT_DAMAGE -> "$displayName Lv.$level (초당 ${level}데미지)"
            BuffType.MULTI_DIRECTION -> "$displayName Lv.$level (${level + 1}방향)"
            BuffType.MASS_DAMAGE -> "$displayName Lv.$level (${level * 100}데미지)"
        }
    }
    
    // 축약된 버프 표시 텍스트 (예: 공격+20%)
    fun getShortDisplayText(): String {
        return when (type) {
            BuffType.MISSILE_DAMAGE -> "${shortName}+${(increasePerLevel * level * 100).toInt()}%"
            BuffType.ENEMY_SLOW -> "${shortName}-${(increasePerLevel * level * 100).toInt()}%"
            BuffType.ATTACK_SPEED -> "${shortName}+${(increasePerLevel * level * 100).toInt()}%"
            BuffType.MISSILE_SPEED -> "${shortName}+${(increasePerLevel * level * 100).toInt()}%"
            BuffType.MISSILE_RANGE -> "${shortName}+${(increasePerLevel * level * 100).toInt()}%"
            BuffType.MISSILE_PIERCE -> "${shortName}×${level + 1}"
            BuffType.DOT_DAMAGE -> "${shortName}${level}/초"
            BuffType.MULTI_DIRECTION -> "${shortName}×${level + 1}"
            BuffType.MASS_DAMAGE -> "${shortName}${level * 100}"
        }
    }
}

/**
 * 버프 관리 클래스
 */
class BuffManager {
    private val buffs = ConcurrentHashMap<BuffType, Buff>()
    
    // 포커 족보에 따른 버프 추가/업그레이드
    fun addPokerHandBuff(pokerHand: PokerHand) {
        when (pokerHand) {
            PokerHand.HIGH_CARD -> {
                // 미사일 데미지 10% 증가
                addOrUpgradeBuff(BuffType.MISSILE_DAMAGE, "미사일 데미지", "공격", BuffCategory.DEFENSE_BUFF, 0.1f)
            }
            PokerHand.ONE_PAIR -> {
                // 미사일 데미지 20% 증가
                addOrUpgradeBuff(BuffType.MISSILE_DAMAGE, "미사일 데미지", "공격", BuffCategory.DEFENSE_BUFF, 0.2f)
            }
            PokerHand.TWO_PAIR -> {
                // 적 이동 속도 20% 감소
                addOrUpgradeBuff(BuffType.ENEMY_SLOW, "적 이동속도 감소", "적속도", BuffCategory.ENEMY_NERF, 0.2f)
            }
            PokerHand.THREE_OF_A_KIND -> {
                // 미사일 발사 속도 30% 증가
                addOrUpgradeBuff(BuffType.ATTACK_SPEED, "공격속도", "속도", BuffCategory.DEFENSE_BUFF, 0.3f)
            }
            PokerHand.STRAIGHT -> {
                // 미사일 속도와 사거리 50% 증가
                addOrUpgradeBuff(BuffType.MISSILE_SPEED, "미사일 속도", "투사체", BuffCategory.DEFENSE_BUFF, 0.5f)
                addOrUpgradeBuff(BuffType.MISSILE_RANGE, "미사일 사거리", "범위", BuffCategory.DEFENSE_BUFF, 0.5f)
            }
            PokerHand.FLUSH -> {
                // 미사일이 적을 관통하여 2마리까지 공격 가능
                addOrUpgradeBuff(BuffType.MISSILE_PIERCE, "미사일 관통", "관통", BuffCategory.DEFENSE_BUFF, 1f)
            }
            PokerHand.FULL_HOUSE -> {
                // 모든 적에게 지속적 데미지
                addOrUpgradeBuff(BuffType.DOT_DAMAGE, "지속 데미지", "DOT", BuffCategory.ENEMY_NERF, 1f)
            }
            PokerHand.FOUR_OF_A_KIND -> {
                // 미사일이 4방향으로 발사됨
                addOrUpgradeBuff(BuffType.MULTI_DIRECTION, "다방향 발사", "방향", BuffCategory.DEFENSE_BUFF, 1f)
            }
            PokerHand.STRAIGHT_FLUSH -> {
                // 모든 적의 이동 속도 50% 감소 및 데미지 2배
                addOrUpgradeBuff(BuffType.ENEMY_SLOW, "적 이동속도 감소", "적속도", BuffCategory.ENEMY_NERF, 0.5f)
                addOrUpgradeBuff(BuffType.MISSILE_DAMAGE, "미사일 데미지", "공격", BuffCategory.DEFENSE_BUFF, 1.0f)
            }
            PokerHand.ROYAL_FLUSH -> {
                // 일정 시간마다 화면의 모든 적에게 강력한 데미지
                addOrUpgradeBuff(BuffType.MASS_DAMAGE, "대량 데미지", "대폭발", BuffCategory.ENEMY_NERF, 1.0f)
            }
        }
    }
    
    // 버프 추가 또는 레벨 업
    private fun addOrUpgradeBuff(type: BuffType, displayName: String, shortName: String, category: BuffCategory, increasePerLevel: Float) {
        if (buffs.containsKey(type)) {
            // 이미 존재하는 버프면 레벨 업
            val existingBuff = buffs[type]!!
            existingBuff.level++
        } else {
            // 새 버프 추가
            buffs[type] = Buff(type, 1, displayName, shortName, category, increasePerLevel)
        }
    }
    
    // 특정 타입의 버프 레벨 반환
    fun getBuffLevel(type: BuffType): Int {
        return buffs[type]?.level ?: 0
    }
    
    // 특정 타입의 버프 효과 값 반환
    fun getBuffEffect(type: BuffType): Float {
        return buffs[type]?.getEffectValue() ?: 0f
    }
    
    // 모든 버프 목록 가져오기
    fun getAllBuffs(): List<Buff> {
        return buffs.values.toList()
    }
    
    // 디펜스 유닛 버프만 가져오기
    fun getDefenseBuffs(): List<Buff> {
        return buffs.values.filter { it.category == BuffCategory.DEFENSE_BUFF }
    }
    
    // 적 너프만 가져오기
    fun getEnemyNerfs(): List<Buff> {
        return buffs.values.filter { it.category == BuffCategory.ENEMY_NERF }
    }
    
    // 버프 효과를 계산하여 미사일 데미지 배율 반환
    fun getMissileDamageMultiplier(): Float {
        val baseDamage = 1.0f
        val additionalDamage = getBuffEffect(BuffType.MISSILE_DAMAGE)
        return baseDamage + additionalDamage
    }
    
    // 버프 효과를 계산하여 적 이동 속도 배율 반환
    fun getEnemySpeedMultiplier(): Float {
        val baseSpeed = 1.0f
        val slowAmount = getBuffEffect(BuffType.ENEMY_SLOW)
        return baseSpeed - slowAmount
    }
    
    // 버프 효과를 계산하여 공격 속도 배율 반환
    fun getAttackSpeedMultiplier(): Float {
        val baseSpeed = 1.0f
        val speedBoost = getBuffEffect(BuffType.ATTACK_SPEED)
        return baseSpeed - speedBoost // 값이 낮을수록 공격속도가 빨라짐
    }
    
    // 버프 효과를 계산하여 미사일 속도 배율 반환
    fun getMissileSpeedMultiplier(): Float {
        val baseSpeed = 1.0f
        val speedBoost = getBuffEffect(BuffType.MISSILE_SPEED)
        return baseSpeed + speedBoost
    }
    
    // 버프 효과를 계산하여 미사일 사거리 배율 반환
    fun getMissileRangeMultiplier(): Float {
        val baseRange = 1.0f
        val rangeBoost = getBuffEffect(BuffType.MISSILE_RANGE)
        return baseRange + rangeBoost
    }
    
    // 미사일 관통 횟수 반환
    fun getMissilePierceCount(): Int {
        return getBuffLevel(BuffType.MISSILE_PIERCE)
    }
    
    // 다방향 발사 개수 반환
    fun getMultiDirectionCount(): Int {
        val level = getBuffLevel(BuffType.MULTI_DIRECTION)
        return if (level > 0) level + 1 else 1
    }
} 