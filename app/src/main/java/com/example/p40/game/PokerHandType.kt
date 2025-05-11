package com.example.p40.game

/**
 * 포커 핸드 타입을 정의하는 열거형 클래스
 * 확장성과 가독성을 위해 각 족보를 명확히 분류
 */
enum class PokerHandType(val rank: Int, val displayName: String, val description: String) {
    HIGH_CARD(0, "족보 없음", "족보 없음"),
    ONE_PAIR(1, "원페어", "원페어 (데미지 10% 증가)"),
    TWO_PAIR(2, "투 페어", "투 페어 (데미지 20% 증가)"),
    THREE_OF_A_KIND(3, "트리플", "트리플 (데미지 30% 증가)"),
    STRAIGHT(4, "스트레이트", "스트레이트 (데미지 40% 증가)"),
    FLUSH(5, "플러시", "플러시 (문양 스킬 활성화)"),
    FULL_HOUSE(6, "풀 하우스", "풀 하우스 (데미지 60% 증가)"),
    FOUR_OF_A_KIND(7, "포카드", "포카드 (데미지 70% 증가)"),
    STRAIGHT_FLUSH(8, "스트레이트 플러시", "스트레이트 플러시 (데미지 80% 증가)"),
    ROYAL_FLUSH(9, "로열 플러시", "로열 플러시 (데미지 90% 증가)");
    
    /**
     * 데미지 증가율을 반환
     */
    fun getDamageBonus(): Float {
        return when (this) {
            HIGH_CARD -> 0.0f
            ONE_PAIR -> 0.1f
            TWO_PAIR -> 0.2f
            THREE_OF_A_KIND -> 0.3f
            STRAIGHT -> 0.4f
            FLUSH -> 0.5f // 플러시도 데미지 보너스 추가
            FULL_HOUSE -> 0.6f
            FOUR_OF_A_KIND -> 0.7f
            STRAIGHT_FLUSH -> 0.8f
            ROYAL_FLUSH -> 0.9f
        }
    }
    
    /**
     * 족보 설명 반환 (특수 케이스 처리 포함)
     */
    fun getDescription(suit: CardSuit? = null): String {
        // 플러시인 경우 문양에 따른 설명 추가
        if (this == FLUSH && suit != null) {
            return "${suit.getName()} 플러시 (문양 스킬 활성화)"
        }
        
        return description
    }
    
    companion object {
        /**
         * 족보 순위로 PokerHandType 찾기
         */
        fun fromRank(rank: Int): PokerHandType {
            return values().find { it.rank == rank } ?: HIGH_CARD
        }
    }
} 