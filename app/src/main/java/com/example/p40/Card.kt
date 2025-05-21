package com.example.p40

import android.graphics.Color

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
    
    fun getSymbol(): String {
        return when (this) {
            HEART -> "♥"
            DIAMOND -> "♦"
            CLUB -> "♣"
            SPADE -> "♠"
            JOKER -> "★"
        }
    }
    
    fun getColor(): Int {
        return when (this) {
            HEART, DIAMOND -> Color.RED
            CLUB, SPADE -> Color.BLACK
            JOKER -> Color.parseColor("#FFD700") // 황금색(#FFD700)으로 변경
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
 * 카드 클래스
 */
class Card(
    val suit: CardSuit,
    val rank: CardRank,
    var isSelected: Boolean = false,
    val isJoker: Boolean = false
) {
    // 조커가 변환된 문양을 저장
    private var transformedSuit: CardSuit? = null
    
    // 카드의 실제 문양을 반환 (조커의 경우 변환된 문양 반환)
    fun getEffectiveSuit(): CardSuit {
        return if (isJoker && transformedSuit != null) {
            transformedSuit!!
        } else {
            suit
        }
    }
    
    // 조커 카드의 문양 변환
    fun transformJoker(newSuit: CardSuit) {
        if (isJoker) {
            transformedSuit = newSuit
        }
    }
    
    // 조커 카드의 문양 변환 초기화
    fun resetJokerTransform() {
        transformedSuit = null
    }
    
    // 카드 출력 문자열
    override fun toString(): String {
        return "${suit.getName()} ${rank.getName()}"
    }
    
    companion object {
        // 조커 카드 생성
        fun createJoker(): Card {
            return Card(CardSuit.JOKER, CardRank.JOKER, isJoker = true)
        }
    }
} 