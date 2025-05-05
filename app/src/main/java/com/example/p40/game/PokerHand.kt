package com.example.p40.game

/**
 * 포커 족보 기본 클래스 (상속용)
 */
abstract class PokerHand {
    abstract val handName: String
    
    // 족보 효과 설명 반환
    abstract fun getDescription(): String
    
    // 족보 표시 문자열 반환
    override fun toString(): String {
        return "$handName: ${getDescription()}"
    }
}

/**
 * 하이 카드 (가장 낮은 족보)
 */
class HighCard : PokerHand() {
    override val handName = "하이 카드"
    
    override fun getDescription(): String {
        return "가장 높은 카드로 승부 (데미지 +15%)"
    }
}

/**
 * 원 페어 (같은 숫자 2장)
 */
class OnePair : PokerHand() {
    override val handName = "원 페어"
    
    override fun getDescription(): String {
        return "같은 숫자 2장 (데미지 +30%)"
    }
}

/**
 * 투 페어 (두 쌍의 같은 숫자)
 */
class TwoPair : PokerHand() {
    override val handName = "투 페어"
    
    override fun getDescription(): String {
        return "두 쌍의 같은 숫자 (데미지 +15%, 공격속도 +12%)"
    }
}

/**
 * 트리플 (같은 숫자 3장)
 */
class ThreeOfAKind : PokerHand() {
    override val handName = "트리플"
    
    override fun getDescription(): String {
        return "같은 숫자 3장 (데미지 +30%, 적 이동속도 -15%)"
    }
}

/**
 * 스트레이트 (연속된 숫자 5장)
 */
class Straight : PokerHand() {
    override val handName = "스트레이트"
    
    override fun getDescription(): String {
        return "연속된 숫자 5장 (3방향 발사)"
    }
}

/**
 * 플러시 (같은 무늬 5장)
 */
class Flush : PokerHand() {
    override val handName = "플러시"
    
    override fun getDescription(): String {
        return "같은 무늬 5장 (문양별 특수 스킬 획득)"
    }
}

/**
 * 풀하우스 (트리플 + 원페어)
 */
class FullHouse : PokerHand() {
    override val handName = "풀 하우스"
    
    override fun getDescription(): String {
        return "트리플 + 원페어 (지속 데미지 6/초, 공격속도 +24%)"
    }
}

/**
 * 포카드 (같은 숫자 4장)
 */
class FourOfAKind : PokerHand() {
    override val handName = "포카드"
    
    override fun getDescription(): String {
        return "같은 숫자 4장 (데미지 +45%, 관통 2회, 2방향 발사)"
    }
}

/**
 * 스트레이트 플러시 (같은 무늬 연속된 숫자 5장)
 */
class StraightFlush : PokerHand() {
    override val handName = "스트레이트 플러시"
    
    override fun getDescription(): String {
        return "같은 무늬 연속된 숫자 5장 (데미지 +30%, 공격속도 +24%, 3방향 발사, 적 이동속도 -30%)"
    }
}

/**
 * 로얄 플러시 (스페이드 10,J,Q,K,A)
 */
class RoyalFlush : PokerHand() {
    override val handName = "로얄 플러시"
    
    override fun getDescription(): String {
        return "최고의 패 (데미지 +45%, 공격속도 +36%, 4방향 발사, 5초마다 300데미지, 관통 2회)"
    }
}

/**
 * 포커 패 평가기
 */
object PokerHandEvaluator {
    /**
     * 포커 패의 족보 평가
     * @param cards 5장의 카드 리스트
     * @return 포커 족보
     */
    fun evaluate(cards: List<Card>): PokerHand {
        // 이미 합계가 계산된 족보 텍스트를 사용
        return when (evaluateRank(cards)) {
            10 -> RoyalFlush()
            9 -> StraightFlush()
            8 -> FourOfAKind()
            7 -> FullHouse()
            6 -> Flush()
            5 -> Straight()
            4 -> ThreeOfAKind()
            3 -> TwoPair()
            2 -> OnePair()
            else -> HighCard()
        }
    }
    
    /**
     * 족보 순위 계산
     */
    private fun evaluateRank(cards: List<Card>): Int {
        // 카드 5장 확인
        if (cards.size != 5) return 1
        
        // 효율성을 위해 필요한 카드 정보를 한 번만 계산
        val suits = cards.groupBy { it.suit }
        val ranks = cards.groupBy { it.rank }
        val rankValues = cards.map { it.rank.value }.sorted()
        val isAllSameSuit = suits.size == 1
        val isStraight = checkStraight(rankValues)
        
        // 로얄 플러시 체크
        if (isAllSameSuit && 
            rankValues.containsAll(listOf(1, 10, 11, 12, 13))) {
            return 10
        }
        
        // 스트레이트 플러시 체크
        if (isAllSameSuit && isStraight) {
            return 9
        }
        
        // 포카드 체크
        if (ranks.any { it.value.size >= 4 }) {
            return 8
        }
        
        // 풀하우스 체크
        if (ranks.size == 2 && ranks.any { it.value.size == 3 }) {
            return 7
        }
        
        // 플러시 체크
        if (isAllSameSuit) {
            return 6
        }
        
        // 스트레이트 체크
        if (isStraight) {
            return 5
        }
        
        // 트리플 체크
        if (ranks.any { it.value.size >= 3 }) {
            return 4
        }
        
        // 투페어 체크
        val pairs = ranks.filter { it.value.size >= 2 }
        if (pairs.size >= 2) {
            return 3
        }
        
        // 원페어 체크
        if (pairs.size == 1) {
            return 2
        }
        
        // 하이카드
        return 1
    }
    
    /**
     * 스트레이트 체크 (연속된 5장의 카드)
     */
    private fun checkStraight(sortedValues: List<Int>): Boolean {
        // A,2,3,4,5 스트레이트 특수 처리
        if (sortedValues == listOf(1, 2, 3, 4, 5)) return true
        
        // 일반적인 스트레이트 확인
        for (i in 1 until sortedValues.size) {
            if (sortedValues[i] != sortedValues[i-1] + 1) {
                return false
            }
        }
        return true
    }
} 