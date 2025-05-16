package com.example.p40

import android.content.Context

/**
 * 포커 카드 생성을 담당하는 클래스
 * PokerCardManager에서 카드 생성 로직만 분리함
 */
class CardGenerationManager(
    private val context: Context?
) {
    // 기본 생성 설정
    private val baseCardCount = 5 // 기본 5장
    
    /**
     * 카드 패 생성
     * @param waveNumber 현재 웨이브 번호 (사용하지 않음)
     * @param totalCardCount 생성할 전체 카드 수
     * @return 생성된 카드 목록
     */
    fun dealCards(waveNumber: Int, totalCardCount: Int): MutableList<Card> {
        val cards = mutableListOf<Card>()
        
        // 모든 카드가 랜덤하게 나오도록 수정 - 좋은 패 확률 삭제
        cards.addAll(generateRandomHand())
        
        // 필요한 경우 추가 카드 생성
        if (totalCardCount > baseCardCount) {
            cards.addAll(addExtraCards(totalCardCount - baseCardCount, cards))
        }
        
        return cards
    }
    
    /**
     * 랜덤 카드 패 생성
     * @return 생성된 카드 목록
     */
    fun generateRandomHand(): List<Card> {
        val cards = mutableListOf<Card>()
        
        // 저장된 덱 불러오기
        val savedDeck = if (context != null) DeckBuilderFragment.loadDeckFromPrefs(context) else null
        
        // 중복 없는 카드 생성
        val usedCards = mutableSetOf<Pair<CardSuit, CardRank>>()
        
        // 저장된 덱이 있는 경우, 저장된 덱에서만 카드 선택
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            // 덱을 섞어서 무작위 카드 선택
            val shuffledDeck = savedDeck.toMutableList().shuffled()
            
            // 모든 카드가 동일한 확률로 선택되게 함
            for (i in 0 until minOf(baseCardCount, shuffledDeck.size)) {
                val card = shuffledDeck[i]
                cards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                usedCards.add(Pair(card.suit, card.rank))
            }
            
            // 부족한 카드가 있는 경우 덱에서 추가로 채우기
            if (cards.size < baseCardCount && shuffledDeck.size > baseCardCount) {
                val remainingCards = shuffledDeck.filter { 
                    Pair(it.suit, it.rank) !in usedCards 
                }.shuffled()
                
                val needMoreCards = baseCardCount - cards.size
                for (i in 0 until minOf(needMoreCards, remainingCards.size)) {
                    val card = remainingCards[i]
                    cards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                    usedCards.add(Pair(card.suit, card.rank))
                }
            }
            
            // 카드가 여전히 부족한 경우 사용한 카드를 중복해서 사용
            if (cards.size < baseCardCount) {
                // 이미 사용된 카드를 포함해 덱에서 다시 선택
                val allAvailableCards = savedDeck.shuffled()
                while (cards.size < baseCardCount && allAvailableCards.isNotEmpty()) {
                    val card = allAvailableCards[cards.size % allAvailableCards.size]
                    cards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                }
            }
        } else {
            // 저장된 덱이 없는 경우 기본 카드 생성
            val basicDeck = createBasicDeck()
            val shuffledBasicDeck = basicDeck.shuffled()
            
            for (i in 0 until minOf(baseCardCount, shuffledBasicDeck.size)) {
                val card = shuffledBasicDeck[i]
                cards.add(Card(card.suit, card.rank))
                usedCards.add(Pair(card.suit, card.rank))
            }
        }
        
        return cards
    }
    
    /**
     * 기본 덱 생성 (저장된 덱이 없을 경우 사용)
     */
    private fun createBasicDeck(): List<Card> {
        val deck = mutableListOf<Card>()
        
        // 기본 52장의 카드 생성 (조커 제외)
        for (suit in CardSuit.values().filter { it != CardSuit.JOKER }) {
            for (rank in CardRank.values().filter { it != CardRank.JOKER }) {
                deck.add(Card(suit, rank))
            }
        }
        
        // 조커 추가하지 않음 (52장으로 구성)
        
        return deck
    }
    
    /**
     * 추가 카드 생성 (모두 저장된 덱에서만 선택)
     */
    private fun addExtraCards(extraCardCount: Int, existingCards: List<Card>): List<Card> {
        val additionalCards = mutableListOf<Card>()
        
        if (extraCardCount <= 0) return additionalCards
        
        // 저장된 덱 불러오기
        val savedDeck = context?.let { DeckBuilderFragment.loadDeckFromPrefs(it) } ?: createBasicDeck()
        
        // 현재 사용 중인 카드 패턴 확인
        val usedCards = existingCards.map { Pair(it.suit, it.rank) }.toMutableSet()
        
        // 덱에서 사용하지 않은 카드만 필터링
        val availableCards = savedDeck.filter { 
            Pair(it.suit, it.rank) !in usedCards 
        }.shuffled()
        
        // 사용 가능한 카드가 있는 경우
        if (availableCards.isNotEmpty()) {
            // 필요한 만큼 카드 추가 (중복 없이)
            for (i in 0 until minOf(extraCardCount, availableCards.size)) {
                val card = availableCards[i]
                additionalCards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                usedCards.add(Pair(card.suit, card.rank))
            }
            
            // 카드가 여전히 부족한 경우 사용된 카드도 포함하여 추가
            if (additionalCards.size < extraCardCount) {
                val allDeckCards = savedDeck.shuffled()
                var index = 0
                
                while (additionalCards.size < extraCardCount) {
                    val card = allDeckCards[index % allDeckCards.size]
                    additionalCards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                    index++
                }
            }
        } else {
            // 모든 카드가 이미 사용된 경우, 덱에서 무작위로 선택
            val shuffledDeck = savedDeck.shuffled()
            var index = 0
            
            while (additionalCards.size < extraCardCount) {
                val card = shuffledDeck[index % shuffledDeck.size]
                additionalCards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
                index++
            }
        }
        
        return additionalCards
    }
    
    /**
     * 랜덤 카드 한 장 생성 (저장된 덱에서만 선택)
     */
    fun createRandomCard(usedCards: MutableSet<Pair<CardSuit, CardRank>>): Card {
        // 저장된 덱 불러오기
        val savedDeck = context?.let { DeckBuilderFragment.loadDeckFromPrefs(it) } ?: createBasicDeck()
        
        // 사용하지 않은 카드만 필터링
        val availableCards = savedDeck.filter { 
            Pair(it.suit, it.rank) !in usedCards 
        }
        
        // 사용 가능한 카드가 있는 경우
        if (availableCards.isNotEmpty()) {
            val card = availableCards.random()
            usedCards.add(Pair(card.suit, card.rank))
            return Card(card.suit, card.rank, isJoker = card.isJoker)
        }
        
        // 모든 카드가 이미 사용된 경우 덱에서 무작위 선택
        val randomCard = savedDeck.random()
        return Card(randomCard.suit, randomCard.rank, isJoker = randomCard.isJoker)
    }
} 