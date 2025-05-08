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
    
    // 캐싱된 값들
    private var cachedMissileDamageMultiplier: Float = 1.0f
    
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
    fun invalidateCache() {
        isBuffCacheValid = false
    }
    
    /**
     * 버프 캐시 업데이트 - 계산 최적화를 위함
     */
    private fun updateBuffCache() {
        if (isBuffCacheValid) return
        
        // 데미지 배율 계산
        val damageBuff = buffs[BuffType.MISSILE_DAMAGE]
        cachedMissileDamageMultiplier = 1f + (damageBuff?.value ?: 0f)
        
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
                                    value = 1.0f,
                                    name = "클로버 플러시 스킬",
                                    description = "시간 멈춤 (5초, 1회용)"
                                ))
                            }
                            
                            CardSuit.DIAMOND -> {
                                addBuff(Buff(
                                    type = BuffType.DIAMOND_FLUSH_SKILL,
                                    level = 1,
                                    value = 1.0f,
                                    name = "다이아 플러시 스킬",
                                    description = "무적 (5초, 1회용)"
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
    
    // 데미지 배율 반환
    fun getMissileDamageMultiplier(): Float {
        updateBuffCache()
        return cachedMissileDamageMultiplier
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