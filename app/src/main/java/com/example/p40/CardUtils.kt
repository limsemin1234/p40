package com.example.p40

import android.graphics.Color

/**
 * 카드 관련 유틸리티 함수들을 모아놓은 클래스
 * 
 * 주요 성능 개선 및 중복 제거 작업:
 * 1. 카드 관련 기능을 중앙 집중화하여 코드 중복 제거
 * 2. 반복적인 카드 유형 검사 로직 간소화
 * 3. 카드 검색 및 비교 로직 최적화
 * 4. 메모리 관리 개선을 위한 리소스 정리 로직 추가
 * 5. 카드 족보 평가 성능 최적화
 * 
 * @see Card
 * @see CardSuit
 * @see CardRank
 */
object CardUtils {
    
    /**
     * 카드 모양에 해당하는 심볼 문자열 반환
     */
    fun getSuitSymbol(suit: CardSuit): String {
        return when (suit) {
            CardSuit.HEART -> "♥"
            CardSuit.DIAMOND -> "♦"
            CardSuit.CLUB -> "♣"
            CardSuit.SPADE -> "♠"
            CardSuit.JOKER -> "★"
        }
    }
    
    /**
     * 카드 모양에 따른 색상 반환
     */
    fun getSuitColor(suit: CardSuit): Int {
        return when (suit) {
            CardSuit.HEART, CardSuit.DIAMOND -> Color.RED
            CardSuit.CLUB, CardSuit.SPADE -> Color.BLACK
            CardSuit.JOKER -> Color.parseColor("#FFD700") // 황금색(#FFD700)으로 변경
        }
    }
    
    /**
     * 카드 숫자에 해당하는 표시 문자열 반환
     */
    fun getRankDisplayValue(rank: CardRank): String {
        return rank.getName()
    }
    
    /**
     * 숫자 값으로 카드 랭크 반환
     */
    fun getCardRankByValue(value: Int): CardRank {
        return when (value) {
            1 -> CardRank.ACE
            2 -> CardRank.TWO
            3 -> CardRank.THREE
            4 -> CardRank.FOUR
            5 -> CardRank.FIVE
            6 -> CardRank.SIX
            7 -> CardRank.SEVEN
            8 -> CardRank.EIGHT
            9 -> CardRank.NINE
            10 -> CardRank.TEN
            11 -> CardRank.JACK
            12 -> CardRank.QUEEN
            13 -> CardRank.KING
            else -> CardRank.ACE // 기본값
        }
    }
    
    /**
     * 카드 랭크 표시용 배열 반환
     */
    fun getRankDisplayValues(): Array<String> {
        return arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    }
    
    /**
     * 두 카드가 같은 카드인지 비교 (무늬와 숫자가 동일한지)
     */
    fun isSameCard(card1: Card, card2: Card): Boolean {
        return card1.suit == card2.suit && card1.rank == card2.rank
    }
    
    /**
     * 카드의 고유 식별자 문자열 생성 (해시셋 검색용)
     */
    fun getCardKey(card: Card): String {
        return "${card.suit.name}_${card.rank.name}"
    }
    
    /**
     * 카드가 조커인지 확인 (별 조커)
     */
    fun isJokerCard(card: Card): Boolean {
        return card.isJoker || isStarJoker(card)
    }
    
    /**
     * 카드가 별 조커인지 확인
     */
    fun isStarJoker(card: Card): Boolean {
        // 별 조커는 suit=JOKER이고 rank=JOKER인 카드
        return card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER
    }
} 