package com.example.p40.game

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.ConcurrentHashMap
import com.example.p40.R

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
    var value: Float = 0.0f, // 실제 효과값 (예: 데미지 증가율)
    var duration: Long = -1L,  // -1은 무제한
    val name: String,
    val description: String
) {
    // 버프 레벨 증가
    fun upgrade(): Boolean {
        level++
        return true
    }
    
    // 버프 값 증가
    fun addValue(additionalValue: Float) {
        value += additionalValue
    }
    
    // 버프 레벨 및 효과 정보 생성
    fun getDisplayText(): String {
        val effectText = when (type) {
            BuffType.MISSILE_DAMAGE -> "데미지 +${(value * 100).toInt()}%"
            BuffType.ATTACK_SPEED -> "공격속도 +${(value * 100).toInt()}%"
            BuffType.MISSILE_SPEED -> "미사일 속도 +${(value * 100).toInt()}%"
            BuffType.MULTI_DIRECTION -> "${level + 1}방향 발사"
            BuffType.MISSILE_PIERCE -> "관통 ${level}회"
            BuffType.ENEMY_SLOW -> "적 이동속도 -${(value * 100).toInt()}%"
            BuffType.DOT_DAMAGE -> "초당 ${(value * 100).toInt()}데미지"
            BuffType.MASS_DAMAGE -> "5초마다 ${(value * 100).toInt()}데미지"
            BuffType.RESOURCE_GAIN -> "자원 획득 +${(value * 100).toInt()}%"
            BuffType.HEART_FLUSH_SKILL -> "하트 플러시 스킬"
            BuffType.SPADE_FLUSH_SKILL -> "스페이드 플러시 스킬"
            BuffType.CLUB_FLUSH_SKILL -> "클로버 플러시 스킬"
            BuffType.DIAMOND_FLUSH_SKILL -> "다이아 플러시 스킬"
        }
        
        return "$name: $effectText"
    }
    
    // 간단한 표시용 텍스트
    fun getShortDisplayText(): String {
        val effectText = when (type) {
            BuffType.MISSILE_DAMAGE -> "데미지 +${(value * 100).toInt()}%"
            BuffType.ATTACK_SPEED -> "공격속도 +${(value * 100).toInt()}%"
            BuffType.MISSILE_SPEED -> "미사일 속도 +${(value * 100).toInt()}%"
            BuffType.MULTI_DIRECTION -> "${level + 1}방향 발사"
            BuffType.MISSILE_PIERCE -> "관통 ${level}회"
            BuffType.ENEMY_SLOW -> "이동속도 -${(value * 100).toInt()}%"
            BuffType.DOT_DAMAGE -> "DoT ${(value * 100).toInt()}/초"
            BuffType.MASS_DAMAGE -> "5초마다 ${(value * 100).toInt()}"
            BuffType.RESOURCE_GAIN -> "자원 +${(value * 100).toInt()}%"
            BuffType.HEART_FLUSH_SKILL -> "하트 플러시 스킬"
            BuffType.SPADE_FLUSH_SKILL -> "스페이드 플러시 스킬"
            BuffType.CLUB_FLUSH_SKILL -> "클로버 플러시 스킬"
            BuffType.DIAMOND_FLUSH_SKILL -> "다이아 플러시 스킬"
        }
        
        return effectText
    }
}

/**
 * 버프 관리 클래스
 */
class BuffManager(private val context: Context) {
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
            // 이미 존재하는 버프가 있는 경우
            existingBuff.addValue(buff.value) // 버프 값을 합산
            existingBuff.upgrade() // 레벨도 증가
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
        
        // 미사일 데미지 배율 계산 - 이제 level 대신 value 사용
        val damageBuff = buffs[BuffType.MISSILE_DAMAGE]
        cachedMissileDamageMultiplier = 1f + (damageBuff?.value ?: 0f)
        
        // 공격 속도 배율 계산
        val attackSpeedBuff = buffs[BuffType.ATTACK_SPEED]
        cachedAttackSpeedMultiplier = 1f - (attackSpeedBuff?.value ?: 0f)
        
        // 미사일 속도 배율 계산
        val missileSpeedBuff = buffs[BuffType.MISSILE_SPEED]
        cachedMissileSpeedMultiplier = 1f + (missileSpeedBuff?.value ?: 0f)
        
        // 적 이동속도 배율 계산
        val enemySlowBuff = buffs[BuffType.ENEMY_SLOW]
        cachedEnemySpeedMultiplier = 1f - (enemySlowBuff?.value ?: 0f)
        
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
                // 하이카드는 버프 없음 (아무 작업도 하지 않음)
                // 메시지는 PokerHand 클래스에서 "족보 없음"으로 표시됨
            }
            
            is OnePair -> {
                // 원페어 (데미지 10% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.1f,
                    name = "원페어",
                    description = "데미지 10% 증가"
                ))
            }
            
            is TwoPair -> {
                // 투 페어 (데미지 20% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.2f,
                    name = "투 페어",
                    description = "데미지 20% 증가"
                ))
            }
            
            is ThreeOfAKind -> {
                // 트리플 (데미지 30% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.3f,
                    name = "트리플",
                    description = "데미지 30% 증가"
                ))
            }
            
            is Straight -> {
                // 스트레이트 (데미지 40% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.4f,
                    name = "스트레이트",
                    description = "데미지 40% 증가"
                ))
            }
            
            is Flush -> {
                // 플러시 - 문양에 따른 특수 스킬만 활성화 (데미지 증가 없음)
                val cards = CardSelectionManager.instance.getSelectedCards()
                if (cards.size >= 5) {
                    // 모든 카드가 같은 무늬인지 확인 (조커는 변환된 문양으로 취급)
                    val suit = cards[0].getEffectiveSuit() // 조커의 경우 변환된 문양 반환
                    val isFlush = cards.all { it.getEffectiveSuit() == suit }
                    
                    if (isFlush) {
                        // 문양에 맞는 스킬 버프 추가
                        when (suit) {
                            CardSuit.HEART -> {
                                addBuff(Buff(
                                    type = BuffType.HEART_FLUSH_SKILL,
                                    level = 1,
                                    value = 1.0f,
                                    name = "하트 플러시 스킬",
                                    description = "체력 전체 회복 (1회용)"
                                ))
                            }
                            
                            CardSuit.SPADE -> {
                                addBuff(Buff(
                                    type = BuffType.SPADE_FLUSH_SKILL,
                                    level = 1,
                                    value = 1.0f,
                                    name = "스페이드 플러시 스킬",
                                    description = "화면 내 모든 적 제거 (보스 제외, 1회용)"
                                ))
                            }
                            
                            CardSuit.CLUB -> {
                                addBuff(Buff(
                                    type = BuffType.CLUB_FLUSH_SKILL,
                                    level = 1,
                                    value = 0.5f,
                                    name = "클로버 플러시 스킬",
                                    description = "적 이동속도 50% 감소 (1회용)"
                                ))
                            }
                            
                            CardSuit.DIAMOND -> {
                                addBuff(Buff(
                                    type = BuffType.DIAMOND_FLUSH_SKILL,
                                    level = 1,
                                    value = 100f,
                                    name = "다이아 플러시 스킬",
                                    description = "자원 100 획득 (1회용)"
                                ))
                            }

                            CardSuit.JOKER -> {
                                // 조커 자체는 문양이 아니므로 스킬 발동하지 않음
                                // (getEffectiveSuit()에서 이미 변환된 문양을 반환하므로 이 케이스는 실행되지 않음)
                            }
                        }
                    }
                }
            }
            
            is FullHouse -> {
                // 풀하우스 (데미지 60% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.6f,
                    name = "풀하우스",
                    description = "데미지 60% 증가"
                ))
            }
            
            is FourOfAKind -> {
                // 포카드 (데미지 70% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.7f,
                    name = "포카드",
                    description = "데미지 70% 증가"
                ))
            }
            
            is StraightFlush -> {
                // 스트레이트 플러시 (데미지 80% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.8f,
                    name = "스트레이트 플러시",
                    description = "데미지 80% 증가"
                ))
            }
            
            is RoyalFlush -> {
                // 로열 플러시 (데미지 90% 증가)
                addBuff(Buff(
                    type = BuffType.MISSILE_DAMAGE,
                    level = 1,
                    value = 0.9f,
                    name = "로열 플러시",
                    description = "데미지 90% 증가"
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
    
    // 자원 획득량 배율 계산
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
    
    // 특정 타입의 버프 제거
    fun removeBuff(type: BuffType) {
        buffs.remove(type)
        invalidateCache()
    }

    // 버프 아이템 뷰 생성
    fun createBuffView(buff: Buff): TextView {
        return TextView(context).apply {
            // 버프 텍스트 설정
            text = when (buff.type) {
                BuffType.MISSILE_DAMAGE -> "데미지${(buff.value * 100).toInt()}%"
                BuffType.HEART_FLUSH_SKILL -> "♥회복"
                BuffType.SPADE_FLUSH_SKILL -> "♠제거"
                BuffType.CLUB_FLUSH_SKILL -> "♣감속"
                BuffType.DIAMOND_FLUSH_SKILL -> "♦자원"
                else -> buff.name
            }
            
            // 스타일 설정
            setBackgroundResource(R.drawable.buff_item_bg)
            textSize = 12f
            setTextColor(Color.WHITE)
            
            // 마진 설정
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 4
                marginEnd = 4
            }
            layoutParams = params
        }
    }
} 