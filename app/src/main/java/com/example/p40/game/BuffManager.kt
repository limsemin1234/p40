package com.example.p40.game

import android.graphics.Color
import java.util.concurrent.ConcurrentHashMap

/**
 * 버프 종류 정의
 */
enum class BuffType {
    MISSILE_DAMAGE,   // 미사일 데미지 증가
    ATTACK_SPEED,     // 공격 속도 증가
    MISSILE_SPEED,    // 미사일 속도 증가
    MULTI_DIRECTION,  // 다방향 발사
    MISSILE_PIERCE,   // 미사일 관통
    ENEMY_SLOW,       // 적 이동속도 감소
    DOT_DAMAGE,       // 지속 데미지
    MASS_DAMAGE,      // 주기적 대량 데미지
    RESOURCE_GAIN     // 자원 획득량 증가
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
    val maxLevel: Int = GameConfig.BUFF_MAX_LEVEL,
    var duration: Long = -1L,  // -1은 무제한
    val name: String,
    val description: String
) {
    // 버프 레벨 증가
    fun upgrade(): Boolean {
        if (level < maxLevel) {
            level++
            return true
        }
        return false
    }
    
    // 버프 레벨 및 효과 정보 생성
    fun getDisplayText(): String {
        val effectText = when (type) {
            BuffType.MISSILE_DAMAGE -> "데미지 +${level * 15}%"
            BuffType.ATTACK_SPEED -> "공격속도 +${level * 12}%"
            BuffType.MISSILE_SPEED -> "미사일 속도 +${level * 20}%"
            BuffType.MULTI_DIRECTION -> "${level + 1}방향 발사"
            BuffType.MISSILE_PIERCE -> "관통 ${level}회"
            BuffType.ENEMY_SLOW -> "적 이동속도 -${level * 15}%"
            BuffType.DOT_DAMAGE -> "초당 ${level * 2}데미지"
            BuffType.MASS_DAMAGE -> "5초마다 ${level * 100}데미지"
            BuffType.RESOURCE_GAIN -> "자원 획득 +${level * 15}%"
        }
        
        return "$name Lv.$level: $effectText"
    }
    
    // 간단한 표시용 텍스트
    fun getShortDisplayText(): String {
        val effectText = when (type) {
            BuffType.MISSILE_DAMAGE -> "데미지 +${level * 15}%"
            BuffType.ATTACK_SPEED -> "공격속도 +${level * 12}%"
            BuffType.MISSILE_SPEED -> "미사일 속도 +${level * 20}%"
            BuffType.MULTI_DIRECTION -> "${level + 1}방향 발사"
            BuffType.MISSILE_PIERCE -> "관통 ${level}회"
            BuffType.ENEMY_SLOW -> "이동속도 -${level * 15}%"
            BuffType.DOT_DAMAGE -> "DoT ${level * 2}/초"
            BuffType.MASS_DAMAGE -> "5초마다 ${level * 100}"
            BuffType.RESOURCE_GAIN -> "자원 +${level * 15}%"
        }
        
        return effectText
    }
}

/**
 * 버프 관리 클래스
 */
class BuffManager {
    private val buffs = ConcurrentHashMap<BuffType, Buff>()
    
    // 효과 계산 결과 캐싱을 위한 변수들
    private var cachedMissileDamageMultiplier: Float = 1.0f
    private var cachedAttackSpeedMultiplier: Float = 1.0f
    private var cachedMissileSpeedMultiplier: Float = 1.0f
    private var cachedEnemySpeedMultiplier: Float = 1.0f
    private var cachedMultiDirectionCount: Int = 1
    private var cachedMissilePierceCount: Int = 0
    
    // 캐시 유효성 플래그
    private var isBuffCacheValid: Boolean = false
    
    init {
        // 버프 초기화
        // (실제 게임에서는 아무 버프도 없는 상태로 시작)
    }
    
    // 버프 추가
    fun addBuff(buff: Buff) {
        val existingBuff = buffs[buff.type]
        if (existingBuff != null) {
            // 이미 존재하는 버프면 레벨 업
            existingBuff.upgrade()
        } else {
            // 새 버프 추가
            buffs[buff.type] = buff
        }
        
        // 캐시 무효화
        invalidateCache()
    }
    
    // 캐시 무효화
    private fun invalidateCache() {
        isBuffCacheValid = false
    }
    
    // 캐시 재계산
    private fun recalculateCache() {
        if (isBuffCacheValid) return
        
        // 미사일 데미지 배율 계산
        val missileDamageLevel = getBuffLevel(BuffType.MISSILE_DAMAGE)
        cachedMissileDamageMultiplier = 1f + (missileDamageLevel * 0.15f)
        
        // 공격 속도 배율 계산
        val attackSpeedLevel = getBuffLevel(BuffType.ATTACK_SPEED)
        cachedAttackSpeedMultiplier = 1f - (attackSpeedLevel * 0.12f)
        
        // 미사일 속도 배율 계산
        val missileSpeedLevel = getBuffLevel(BuffType.MISSILE_SPEED)
        cachedMissileSpeedMultiplier = 1f + (missileSpeedLevel * 0.2f)
        
        // 적 이동속도 배율 계산
        val enemySlowLevel = getBuffLevel(BuffType.ENEMY_SLOW)
        cachedEnemySpeedMultiplier = 1f - (enemySlowLevel * 0.15f)
        
        // 다방향 발사 계산
        val multiDirectionLevel = getBuffLevel(BuffType.MULTI_DIRECTION)
        cachedMultiDirectionCount = 1 + multiDirectionLevel
        
        // 미사일 관통 횟수 계산
        val missilePierceLevel = getBuffLevel(BuffType.MISSILE_PIERCE)
        cachedMissilePierceCount = missilePierceLevel
        
        // 캐시 유효성 표시
        isBuffCacheValid = true
    }
    
    // 포커 족보에 따른 버프 추가
    fun addPokerHandBuff(pokerHand: PokerHand) {
        when (pokerHand) {
            is HighCard -> {
                // 하이 카드는 약한 버프 제공 (데미지 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    name = "하이 카드",
                    description = "약한 데미지 증가"
                ))
            }
            
            is OnePair -> {
                // 원 페어 (데미지 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 2,
                    name = "원 페어",
                    description = "데미지 증가"
                ))
            }
            
            is TwoPair -> {
                // 투 페어 (데미지 & 공격 속도 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    name = "투 페어 - 데미지",
                    description = "데미지 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.ATTACK_SPEED,
                    level = 1,
                    name = "투 페어 - 공격속도",
                    description = "공격 속도 증가"
                ))
            }
            
            is ThreeOfAKind -> {
                // 트리플 (데미지 & 적 이동속도 감소)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 2,
                    name = "트리플 - 데미지",
                    description = "데미지 대폭 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.ENEMY_SLOW,
                    level = 1,
                    name = "트리플 - 슬로우",
                    description = "적 이동속도 감소"
                ))
            }
            
            is Straight -> {
                // 스트레이트 (다방향 발사)
                addBuff(Buff(
                    type = BuffType.MULTI_DIRECTION,
                    level = 2,
                    name = "스트레이트",
                    description = "3방향 발사"
                ))
            }
            
            is Flush -> {
                // 플러시 (미사일 속도 & 관통)
                addBuff(Buff(
                    type = BuffType.MISSILE_SPEED,
                    level = 2,
                    name = "플러시 - 속도",
                    description = "미사일 속도 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.MISSILE_PIERCE,
                    level = 1,
                    name = "플러시 - 관통",
                    description = "미사일 관통"
                ))
            }
            
            is FullHouse -> {
                // 풀 하우스 (지속 데미지 & 공격속도)
                addBuff(Buff(
                    type = BuffType.DOT_DAMAGE,
                    level = 3,
                    name = "풀 하우스 - DoT",
                    description = "적에게 지속 데미지"
                ))
                
                addBuff(Buff(
                    type = BuffType.ATTACK_SPEED,
                    level = 2,
                    name = "풀 하우스 - 공격속도",
                    description = "공격 속도 대폭 증가"
                ))
            }
            
            is FourOfAKind -> {
                // 포카드 (데미지 & 관통 & 다방향)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 3,
                    name = "포카드 - 데미지",
                    description = "데미지 대폭 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.MISSILE_PIERCE,
                    level = 2,
                    name = "포카드 - 관통",
                    description = "미사일 관통 강화"
                ))
                
                addBuff(Buff(
                    type = BuffType.MULTI_DIRECTION,
                    level = 1,
                    name = "포카드 - 다방향",
                    description = "2방향 발사"
                ))
            }
            
            is StraightFlush -> {
                // 스트레이트 플러시 (모든 능력치 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 2,
                    name = "스트레이트 플러시 - 데미지",
                    description = "데미지 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.ATTACK_SPEED,
                    level = 2,
                    name = "스트레이트 플러시 - 속도",
                    description = "공격 속도 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.MULTI_DIRECTION,
                    level = 2,
                    name = "스트레이트 플러시 - 다방향",
                    description = "3방향 발사"
                ))
                
                addBuff(Buff(
                    type = BuffType.ENEMY_SLOW,
                    level = 2,
                    name = "스트레이트 플러시 - 슬로우",
                    description = "적 이동속도 크게 감소"
                ))
            }
            
            is RoyalFlush -> {
                // 로얄 플러시 (초강력 효과)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 3,
                    name = "로얄 플러시 - 데미지",
                    description = "데미지 대폭 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.ATTACK_SPEED,
                    level = 3,
                    name = "로얄 플러시 - 속도",
                    description = "공격 속도 대폭 증가"
                ))
                
                addBuff(Buff(
                    type = BuffType.MULTI_DIRECTION,
                    level = 3,
                    name = "로얄 플러시 - 다방향",
                    description = "4방향 발사"
                ))
                
                addBuff(Buff(
                    type = BuffType.MASS_DAMAGE,
                    level = 3,
                    name = "로얄 플러시 - 대량 데미지",
                    description = "정기적인 대량 데미지"
                ))
                
                addBuff(Buff(
                    type = BuffType.MISSILE_PIERCE,
                    level = 2,
                    name = "로얄 플러시 - 관통",
                    description = "미사일 관통 강화"
                ))
            }
        }
        
        // 버프 추가 후 캐시 무효화
        invalidateCache()
    }
    
    // 특정 버프 타입의 레벨 가져오기
    fun getBuffLevel(type: BuffType): Int {
        return buffs[type]?.level ?: 0
    }
    
    // 모든 버프 가져오기
    fun getAllBuffs(): List<Buff> {
        return buffs.values.toList()
    }
    
    // 디펜스 유닛에 적용되는 버프만 가져오기
    fun getDefenseBuffs(): List<Buff> {
        return buffs.values.filter { buff ->
            buff.type in listOf(
                BuffType.MISSILE_DAMAGE,
                BuffType.ATTACK_SPEED,
                BuffType.MISSILE_SPEED,
                BuffType.MULTI_DIRECTION,
                BuffType.MISSILE_PIERCE,
                BuffType.RESOURCE_GAIN
            )
        }
    }
    
    // 적에게 적용되는 너프만 가져오기
    fun getEnemyNerfs(): List<Buff> {
        return buffs.values.filter { buff ->
            buff.type in listOf(
                BuffType.ENEMY_SLOW,
                BuffType.DOT_DAMAGE,
                BuffType.MASS_DAMAGE
            )
        }
    }
    
    // 미사일 데미지 배율 계산
    fun getMissileDamageMultiplier(): Float {
        if (!isBuffCacheValid) recalculateCache()
        return cachedMissileDamageMultiplier
    }
    
    // 공격 속도 배율 계산
    fun getAttackSpeedMultiplier(): Float {
        if (!isBuffCacheValid) recalculateCache()
        return cachedAttackSpeedMultiplier
    }
    
    // 미사일 속도 배율 계산
    fun getMissileSpeedMultiplier(): Float {
        if (!isBuffCacheValid) recalculateCache()
        return cachedMissileSpeedMultiplier
    }
    
    // 적 이동속도 배율 계산
    fun getEnemySpeedMultiplier(): Float {
        if (!isBuffCacheValid) recalculateCache()
        return cachedEnemySpeedMultiplier
    }
    
    // 다방향 발사 수 계산
    fun getMultiDirectionCount(): Int {
        if (!isBuffCacheValid) recalculateCache()
        return cachedMultiDirectionCount
    }
    
    // 미사일 관통 횟수 계산
    fun getMissilePierceCount(): Int {
        if (!isBuffCacheValid) recalculateCache()
        return cachedMissilePierceCount
    }
    
    // 자원 획득량 배율 계산 (새 메서드)
    fun getResourceGainMultiplier(): Float {
        if (!isBuffCacheValid) recalculateCache()
        val level = getBuffLevel(BuffType.RESOURCE_GAIN)
        return 1f + (level * 0.15f)
    }
    
    // 모든 버프 제거
    fun clearAllBuffs() {
        buffs.clear()
        invalidateCache()
    }
} 