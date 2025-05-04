package com.example.p40.game

import com.example.p40.R

/**
 * 카드 무늬 정의
 */
enum class CardSuit {
    HEART, DIAMOND, CLUB, SPADE, JOKER; // 조커는 특별한 슈트로 정의

    fun getName(): String {
        return when (this) {
            HEART -> "하트"
            DIAMOND -> "다이아"
            CLUB -> "클로버"
            SPADE -> "스페이드"
            JOKER -> "조커"
        }
    }
}

/**
 * 카드 숫자/랭크 정의
 */
enum class CardRank(val value: Int) {
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
    JOKER(0); // 조커는 특별한 랭크로 정의

    fun getName(): String {
        return when (this) {
            ACE -> "A"
            TWO -> "2"
            THREE -> "3"
            FOUR -> "4"
            FIVE -> "5"
            SIX -> "6"
            SEVEN -> "7"
            EIGHT -> "8"
            NINE -> "9"
            TEN -> "10"
            JACK -> "J"
            QUEEN -> "Q"
            KING -> "K"
            JOKER -> "조커"
        }
    }
}

/**
 * 포커 족보 정의
 */
enum class PokerHand(val value: Int, val handName: String) {
    HIGH_CARD(1, "하이카드"),
    ONE_PAIR(2, "원페어"),
    TWO_PAIR(3, "투페어"),
    THREE_OF_A_KIND(4, "트리플"),
    STRAIGHT(5, "스트레이트"),
    FLUSH(6, "플러시"),
    FULL_HOUSE(7, "풀하우스"),
    FOUR_OF_A_KIND(8, "포카드"),
    STRAIGHT_FLUSH(9, "스트레이트 플러시"),
    ROYAL_FLUSH(10, "로얄 플러시");

    fun getDescription(): String {
        return when (this) {
            HIGH_CARD -> "가장 높은 카드 기준 - 미사일 데미지 10% 증가"
            ONE_PAIR -> "같은 숫자 2장 - 미사일 데미지 20% 증가"
            TWO_PAIR -> "같은 숫자 2장이 2쌍 - 적 이동 속도 20% 감소"
            THREE_OF_A_KIND -> "같은 숫자 3장 - 미사일 발사 속도 30% 증가"
            STRAIGHT -> "연속된 숫자 5장 - 미사일 속도와 사거리 50% 증가"
            FLUSH -> "같은 무늬 5장 - 미사일이 적을 관통하여 2마리까지 공격 가능"
            FULL_HOUSE -> "같은 숫자 3장 + 같은 숫자 2장 - 모든 적에게 지속적 데미지"
            FOUR_OF_A_KIND -> "같은 숫자 4장 - 미사일이 4방향으로 발사됨"
            STRAIGHT_FLUSH -> "같은 무늬의 연속된 숫자 5장 - 모든 적의 이동 속도 50% 감소 및 데미지 2배"
            ROYAL_FLUSH -> "같은 무늬의 10,J,Q,K,A - 일정 시간마다 화면의 모든 적에게 강력한 데미지"
        }
    }
}

/**
 * 카드 클래스
 */
data class Card(
    val suit: CardSuit,
    val rank: CardRank,
    var isSelected: Boolean = false // 카드 교체 선택 여부
) {
    fun getDisplayName(): String {
        return if (suit == CardSuit.JOKER) {
            "조커"
        } else {
            "${suit.getName()} ${rank.getName()}"
        }
    }
    
    fun getColorRes(): Int {
        return when(suit) {
            CardSuit.HEART, CardSuit.DIAMOND -> android.graphics.Color.RED
            CardSuit.CLUB, CardSuit.SPADE -> android.graphics.Color.BLACK
            CardSuit.JOKER -> android.graphics.Color.MAGENTA
        }
    }
    
    // 조커 카드 생성 팩토리 메서드
    companion object {
        fun createJoker(): Card {
            return Card(CardSuit.JOKER, CardRank.JOKER)
        }
    }
} 