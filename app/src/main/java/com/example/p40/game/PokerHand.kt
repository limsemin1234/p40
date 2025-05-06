package com.example.p40.game

/**
 * 포커 족보 기본 클래스 (상속용)
 */
abstract class PokerHand(val cards: List<Card> = emptyList(), val handName: String) {
    // 족보 순위 (1이 가장 낮음)
    abstract val handRank: Int
    
    // 족보 효과 설명 반환
    abstract fun getDescription(): String
}

/**
 * 하이 카드 (가장 낮은 족보)
 */
class HighCard(cards: List<Card> = emptyList()) : PokerHand(cards, "족보 없음") {
    override val handRank = 0
    
    override fun getDescription(): String {
        return "족보 없음"
    }
}

/**
 * 원 페어 (같은 숫자 2장)
 */
class OnePair(cards: List<Card> = emptyList(), val pairRank: CardRank? = null) : PokerHand(cards, "원페어") {
    override val handRank = 1
    
    override fun getDescription(): String {
        return "원페어 (데미지 10% 증가)"
    }
    
    // 페어의 랭크(숫자) 반환
    fun getActualPairRank(): CardRank {
        return pairRank ?: CardRank.TWO // 기본값으로 2 반환
    }
}

/**
 * 투 페어 (두 쌍의 같은 숫자)
 */
class TwoPair(cards: List<Card> = emptyList()) : PokerHand(cards, "투 페어") {
    override val handRank = 2
    
    override fun getDescription(): String {
        return "투 페어 (데미지 20% 증가)"
    }
}

/**
 * 트리플 (같은 숫자 3장)
 */
class ThreeOfAKind(cards: List<Card> = emptyList()) : PokerHand(cards, "트리플") {
    override val handRank = 3
    
    override fun getDescription(): String {
        return "트리플 (데미지 30% 증가)"
    }
}

/**
 * 스트레이트 (연속된 숫자 5장)
 */
class Straight(cards: List<Card> = emptyList()) : PokerHand(cards, "스트레이트") {
    override val handRank = 4
    
    override fun getDescription(): String {
        return "스트레이트 (데미지 40% 증가)"
    }
}

/**
 * 플러시 (같은 무늬 5장)
 */
class Flush(cards: List<Card> = emptyList()) : PokerHand(cards, "플러시") {
    override val handRank = 5
    
    override fun getDescription(): String {
        val suit = if (cards.isNotEmpty()) cards[0].suit.name else "?"
        return "$suit 플러시 (문양 스킬 활성화)"
    }
}

/**
 * 풀하우스 (트리플 + 원페어)
 */
class FullHouse(cards: List<Card> = emptyList()) : PokerHand(cards, "풀 하우스") {
    override val handRank = 6
    
    override fun getDescription(): String {
        return "풀 하우스 (데미지 60% 증가)"
    }
}

/**
 * 포카드 (같은 숫자 4장)
 */
class FourOfAKind(cards: List<Card> = emptyList()) : PokerHand(cards, "포카드") {
    override val handRank = 7
    
    override fun getDescription(): String {
        return "포카드 (데미지 70% 증가)"
    }
}

/**
 * 스트레이트 플러시 (같은 무늬 연속된 숫자 5장)
 */
class StraightFlush(cards: List<Card> = emptyList()) : PokerHand(cards, "스트레이트 플러시") {
    override val handRank = 8
    
    override fun getDescription(): String {
        return "스트레이트 플러시 (데미지 80% 증가)"
    }
}

/**
 * 로얄 플러시 (A, K, Q, J, 10 + 같은 무늬)
 */
class RoyalFlush(cards: List<Card> = emptyList()) : PokerHand(cards, "로열 플러시") {
    override val handRank = 9
    
    override fun getDescription(): String {
        return "로열 플러시 (데미지 90% 증가)"
    }
}

/**
 * 포커 족보 평가 클래스
 */
object PokerHandEvaluator {
    /**
     * 카드 5장으로 포커 족보 평가
     */
    fun evaluate(cards: List<Card>): PokerHand {
        if (cards.size < 5) return HighCard(cards)
        
        // 플러시 체크
        val isFlush = isFlush(cards)
        
        // 스트레이트 체크
        val isStraight = isStraight(cards)
        
        // 스트레이트 플러시 체크
        if (isFlush && isStraight) {
            // 로열 스트레이트 플러시 체크 (A,K,Q,J,10)
            val sortedRanks = cards.map { it.rank.value }.sorted()
            if (sortedRanks == listOf(CardRank.TEN.value, CardRank.JACK.value, 
                                       CardRank.QUEEN.value, CardRank.KING.value, 
                                       CardRank.ACE.value)) {
                return RoyalFlush(cards)
            }
            return StraightFlush(cards)
        }
        
        // 카드 랭크별 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 같은 랭크 카드가 몇 장씩 있는지 확인
        val maxOfAKind = rankGroups.values.maxOf { it.size }
        
        return when (maxOfAKind) {
            4 -> FourOfAKind(cards)
            3 -> {
                // 풀하우스 체크 (트리플 + 페어)
                if (rankGroups.values.any { it.size == 2 }) {
                    FullHouse(cards)
                } else {
                    ThreeOfAKind(cards)
                }
            }
            2 -> {
                // 투페어 체크
                if (rankGroups.values.count { it.size == 2 } >= 2) {
                    TwoPair(cards)
                } else {
                    // 원페어: 페어의 랭크(숫자)를 찾아서 생성자에 전달
                    val pairRank = rankGroups.entries.first { it.value.size == 2 }.key
                    OnePair(cards, pairRank)
                }
            }
            else -> {
                if (isFlush) Flush(cards)
                else if (isStraight) Straight(cards)
                else HighCard(cards)
            }
        }
    }
    
    /**
     * 스트레이트 체크 (연속된 5장의 카드)
     */
    private fun isStraight(cards: List<Card>): Boolean {
        val sortedValues = cards.map { it.rank.value }.sorted()
        
        // 일반적인 스트레이트 체크
        if (isConsecutive(sortedValues)) return true
        
        // A-5 스트레이트 체크 (A,2,3,4,5)
        if (sortedValues.contains(CardRank.ACE.value)) {
            val tempValues = sortedValues.toMutableList()
            tempValues.remove(CardRank.ACE.value)
            tempValues.add(1) // A를 1로 취급
            return isConsecutive(tempValues.sorted())
        }
        
        return false
    }
    
    /**
     * 숫자가 연속되었는지 확인
     */
    private fun isConsecutive(values: List<Int>): Boolean {
        for (i in 0 until values.size - 1) {
            if (values[i + 1] - values[i] != 1) {
                return false
            }
        }
        return true
    }
    
    /**
     * 플러시 체크 (같은 무늬 5장의 카드)
     */
    private fun isFlush(cards: List<Card>): Boolean {
        val suits = cards.groupBy { it.suit }
        return suits.size == 1
    }
} 