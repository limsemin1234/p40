package com.example.p40.game

import android.content.Context
import com.example.p40.DeckBuilderFragment
import kotlin.random.Random

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
     * @param waveNumber 현재 웨이브 번호
     * @param totalCardCount 생성할 전체 카드 수
     * @return 생성된 카드 목록
     */
    fun dealCards(waveNumber: Int, totalCardCount: Int): MutableList<Card> {
        val cards = mutableListOf<Card>()
        
        // 웨이브 번호에 따라 더 좋은 카드가 나올 확률 증가
        // 기본 확률 0.15에서 웨이브 번호에 따라 증가
        val goodHandProbability = minOf(0.15f + (waveNumber * 0.03f), 0.5f)
        
        // 좋은 패가 나올 확률 계산
        if (Random.nextFloat() < goodHandProbability) {
            // 좋은 패 생성 (스트레이트 이상)
            cards.addAll(generateGoodHand(waveNumber))
        } else {
            // 일반 랜덤 패 생성
            cards.addAll(generateRandomHand())
        }
        
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
        
        // 조커 카드가 나올 확률 (10%)
        val jokerProbability = 0.1f
        
        // 조커 카드 추가 여부 결정
        val includeJoker = Random.nextFloat() < jokerProbability
        
        // 카드 수 결정 (조커 카드가 포함되면 1장 줄임)
        val normalCards = if (includeJoker) baseCardCount - 1 else baseCardCount
        
        // 저장된 덱이 있는 경우, 저장된 덱에서 카드 선택
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            // 덱을 섞어서 무작위 카드 선택
            val shuffledDeck = savedDeck.shuffled()
            
            // 일반 카드 추가
            for (i in 0 until normalCards) {
                if (i < shuffledDeck.size) {
                    val card = shuffledDeck[i]
                    if (Pair(card.suit, card.rank) !in usedCards) {
                        cards.add(Card(card.suit, card.rank))
                        usedCards.add(Pair(card.suit, card.rank))
                    }
                }
            }
            
            // 조커 카드 추가 여부
            if (includeJoker) {
                cards.add(Card.createJoker())
            }
            
            // 부족한 카드가 있는 경우 기본 카드로 채우기
            if (cards.size < baseCardCount) {
                fillRemainingCards(cards, usedCards)
            }
        } else {
            // 저장된 덱이 없는 경우 기존 로직으로 카드 생성
            // 랜덤 카드 생성 (기본 카드 수만큼)
            val suits = CardSuit.values().filter { it != CardSuit.JOKER }
            val ranks = CardRank.values().filter { it != CardRank.JOKER }
            
            while (cards.size < baseCardCount) {
                // 조커 카드 추가 여부 결정
                if (includeJoker && !cards.any { CardUtils.isJokerCard(it) } && cards.size == normalCards) {
                    // 조커 카드 추가 (한 번만)
                    cards.add(Card.createJoker())
                } else {
                    // 일반 카드 추가
                    val suit = suits.random()
                    val rank = ranks.random()
                    val cardPair = Pair(suit, rank)
                    
                    if (cardPair !in usedCards) {
                        usedCards.add(cardPair)
                        cards.add(Card(suit, rank))
                    }
                }
            }
        }
        
        return cards
    }
    
    /**
     * 부족한 카드를 채우는 함수
     */
    private fun fillRemainingCards(cards: MutableList<Card>, usedCards: MutableSet<Pair<CardSuit, CardRank>>) {
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        while (cards.size < baseCardCount) {
            val suit = suits.random()
            val rank = ranks.random()
            val cardPair = Pair(suit, rank)
            
            if (cardPair !in usedCards) {
                usedCards.add(cardPair)
                cards.add(Card(suit, rank))
            }
        }
    }
    
    /**
     * 좋은 카드 패 생성 (스트레이트 이상)
     * @param waveNumber 현재 웨이브 번호
     * @return 생성된 카드 목록
     */
    fun generateGoodHand(waveNumber: Int): List<Card> {
        // 웨이브 번호에 따라 더 좋은 족보 가능성 증가
        val handType = when {
            waveNumber >= 8 && Random.nextFloat() < 0.2f -> "royal_flush"
            waveNumber >= 6 && Random.nextFloat() < 0.3f -> "straight_flush"
            waveNumber >= 5 && Random.nextFloat() < 0.4f -> "four_of_a_kind"
            waveNumber >= 4 && Random.nextFloat() < 0.5f -> "full_house"
            waveNumber >= 3 && Random.nextFloat() < 0.6f -> "flush"
            else -> "straight"
        }
        
        val cards = mutableListOf<Card>()
        
        // 저장된 덱 불러오기
        val savedDeck = if (context != null) DeckBuilderFragment.loadDeckFromPrefs(context) else null
        
        // 조커 카드 추가 여부 결정 (20% 확률)
        val includeJoker = Random.nextFloat() < 0.2f
        
        // 저장된 덱이 있고 충분한 카드가 있는 경우
        if (savedDeck != null && savedDeck.size >= 5) {
            generateGoodHandFromSavedDeck(cards, handType, savedDeck, includeJoker)
        } else {
            // 저장된 덱이 없거나 카드가 부족한 경우, 기존 로직으로 카드 생성
            val cardsToGenerate = if (includeJoker) baseCardCount - 1 else baseCardCount
            generateDefaultGoodHand(cards, handType, cardsToGenerate, includeJoker)
        }
        
        // 카드 순서 섞기
        cards.shuffle()
        
        return cards
    }
    
    /**
     * 저장된 덱에서 좋은 패 생성
     */
    private fun generateGoodHandFromSavedDeck(
        cards: MutableList<Card>, 
        handType: String, 
        savedDeck: List<Card>, 
        includeJoker: Boolean
    ) {
        // 덱을 무늬와 숫자별로 분류
        val cardsBySuit = savedDeck.groupBy { it.suit }
        val cardsByRank = savedDeck.groupBy { it.rank }
        
        // 무늬와 숫자별 카드가 충분한지 확인
        val suitWithMostCards = cardsBySuit.maxByOrNull { it.value.size }
        val rankWithMostCards = cardsByRank.maxByOrNull { it.value.size }
        
        // 가능한 족보 생성
        when (handType) {
            "royal_flush", "straight_flush", "flush" -> {
                // 같은 무늬 카드가 5장 이상 있는지 확인
                if (suitWithMostCards != null && suitWithMostCards.value.size >= 5) {
                    generateFlushLikeHand(cards, handType, suitWithMostCards.key, suitWithMostCards.value, includeJoker)
                } else {
                    // 없으면 대체 족보 생성
                    generateAlternateHand(cards, handType, savedDeck, includeJoker)
                }
            }
            "four_of_a_kind" -> {
                // 같은 숫자 카드가 4장 있는지 확인 또는 3장 + 조커로 구성 가능한지
                if (rankWithMostCards != null && (rankWithMostCards.value.size >= 4 || 
                    (rankWithMostCards.value.size >= 3 && includeJoker))) {
                    generateFourOfAKindHand(cards, rankWithMostCards.key, rankWithMostCards.value, savedDeck, includeJoker)
                } else {
                    // 없으면 대체 족보 생성
                    generateAlternateHand(cards, handType, savedDeck, includeJoker)
                }
            }
            "full_house" -> {
                // 트리플과 페어를 구성할 수 있는지 확인
                val ranksWithThreeOrMore = cardsByRank.filter { it.value.size >= 3 }
                val ranksWithTwoOrMore = cardsByRank.filter { it.value.size >= 2 }
                
                if (ranksWithThreeOrMore.isNotEmpty() && ranksWithTwoOrMore.size >= 2) {
                    generateFullHouseHand(cards, ranksWithThreeOrMore, ranksWithTwoOrMore, includeJoker)
                } else {
                    // 없으면 대체 족보 생성
                    generateAlternateHand(cards, handType, savedDeck, includeJoker)
                }
            }
            else -> { // "straight"
                // 스트레이트 구성 가능 여부 확인은 복잡하므로 간단하게 대체 족보 생성
                generateAlternateHand(cards, handType, savedDeck, includeJoker)
            }
        }
    }
    
    /**
     * 추가 카드 생성
     */
    private fun addExtraCards(extraCardCount: Int, existingCards: List<Card>): List<Card> {
        val additionalCards = mutableListOf<Card>()
        
        if (extraCardCount <= 0) return additionalCards
        
        // 저장된 덱 불러오기
        val savedDeck = context?.let { DeckBuilderFragment.loadDeckFromPrefs(it) }
        
        // 현재 사용 중인 카드 패턴 확인
        val usedCards = existingCards.map { Pair(it.suit, it.rank) }.toMutableSet()
        
        // 조커 카드가 이미 있는지 확인
        val hasJoker = existingCards.any { CardUtils.isJokerCard(it) }
        
        // 저장된 덱이 있는 경우
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            // 덱에서 사용하지 않은 카드만 필터링
            val availableCards = savedDeck.filter { 
                Pair(it.suit, it.rank) !in usedCards 
            }.shuffled()
            
            // 필요한 만큼 카드 추가
            var availableIndex = 0
            var cardsToAdd = extraCardCount
            
            while (additionalCards.size < cardsToAdd && availableIndex < availableCards.size) {
                // 조커 추가 확인 (10% 확률, 이미 조커가 있으면 추가 안함)
                if (Random.nextFloat() < 0.1f && !hasJoker && additionalCards.size < cardsToAdd - 1) {
                    additionalCards.add(Card.createJoker())
                } else if (availableIndex < availableCards.size) {
                    // 일반 카드 추가
                    val card = availableCards[availableIndex]
                    additionalCards.add(Card(card.suit, card.rank))
                    availableIndex++
                }
            }
            
            // 부족한 카드가 있으면 기본 방식으로 채우기
            if (additionalCards.size < extraCardCount) {
                additionalCards.addAll(
                    fillRemainingExtraCards(
                        extraCardCount - additionalCards.size, 
                        usedCards + additionalCards.map { Pair(it.suit, it.rank) },
                        hasJoker || additionalCards.any { CardUtils.isJokerCard(it) }
                    )
                )
            }
        } else {
            // 저장된 덱이 없는 경우 기본 방식으로 추가 카드 생성
            additionalCards.addAll(fillRemainingExtraCards(extraCardCount, usedCards, hasJoker))
        }
        
        return additionalCards
    }
    
    /**
     * 부족한 추가 카드 채우기
     */
    private fun fillRemainingExtraCards(
        count: Int, 
        usedCards: Set<Pair<CardSuit, CardRank>>, 
        hasJoker: Boolean
    ): List<Card> {
        val additionalCards = mutableListOf<Card>()
        val mutableUsedCards = usedCards.toMutableSet()
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        var jokerAdded = hasJoker
        
        // 필요한 만큼 추가 카드 생성
        while (additionalCards.size < count) {
            // 조커 카드 추가 여부 결정 (10%, 이미 조커가 있으면 추가하지 않음)
            if (Random.nextFloat() < 0.1f && !jokerAdded) {
                // 조커 카드 추가
                additionalCards.add(Card.createJoker())
                jokerAdded = true
            } else {
                // 일반 카드 추가
                var newCard: Card? = null
                
                // 사용하지 않은 카드 찾기
                for (attempt in 0 until 100) { // 무한 루프 방지
                    val suit = suits.random()
                    val rank = ranks.random()
                    val cardPair = Pair(suit, rank)
                    
                    if (cardPair !in mutableUsedCards) {
                        newCard = Card(suit, rank)
                        mutableUsedCards.add(cardPair)
                        break
                    }
                }
                
                // 모든 카드가 사용 중이면 중복 허용
                if (newCard == null) {
                    val suit = suits.random()
                    val rank = ranks.random()
                    newCard = Card(suit, rank)
                }
                
                // 카드 추가
                additionalCards.add(newCard)
            }
        }
        
        return additionalCards
    }
    
    // 아래는 복잡한 카드 조합 생성 메서드들입니다...
    
    /**
     * 플러시 계열 패 생성
     */
    private fun generateFlushLikeHand(
        cards: MutableList<Card>, 
        handType: String, 
        suit: CardSuit, 
        availableCards: List<Card>, 
        includeJoker: Boolean
    ) {
        when (handType) {
            "royal_flush" -> {
                // 로얄 플러시 (10, J, Q, K, A)
                val royalRanks = listOf(CardRank.TEN, CardRank.JACK, CardRank.QUEEN, CardRank.KING, CardRank.ACE)
                val royalCards = availableCards.filter { it.rank in royalRanks }
                
                if (royalCards.size >= 5 || (royalCards.size >= 4 && includeJoker)) {
                    // 로얄 플러시 구성 가능
                    for (rank in royalRanks) {
                        val card = royalCards.find { it.rank == rank }
                        if (card != null) {
                            cards.add(Card(card.suit, card.rank))
                            if (cards.size >= (if (includeJoker) 4 else 5)) break
                        }
                    }
                    
                    // 조커 추가
                    if (includeJoker) {
                        cards.add(Card.createJoker())
                    }
                } else {
                    // 일반 플러시로 대체
                    val shuffledCards = availableCards.shuffled()
                    for (i in 0 until (if (includeJoker) 4 else 5)) {
                        if (i < shuffledCards.size) {
                            cards.add(Card(shuffledCards[i].suit, shuffledCards[i].rank))
                        }
                    }
                    
                    if (includeJoker) {
                        cards.add(Card.createJoker())
                    }
                }
            }
            "straight_flush" -> {
                // 간단히 처리하기 위해 일반 플러시로 구현
                val shuffledCards = availableCards.shuffled()
                for (i in 0 until (if (includeJoker) 4 else 5)) {
                    if (i < shuffledCards.size) {
                        cards.add(Card(shuffledCards[i].suit, shuffledCards[i].rank))
                    }
                }
                
                if (includeJoker) {
                    cards.add(Card.createJoker())
                }
            }
            else -> { // "flush"
                // 일반 플러시
                val shuffledCards = availableCards.shuffled()
                for (i in 0 until (if (includeJoker) 4 else 5)) {
                    if (i < shuffledCards.size) {
                        cards.add(Card(shuffledCards[i].suit, shuffledCards[i].rank))
                    }
                }
                
                if (includeJoker) {
                    cards.add(Card.createJoker())
                }
            }
        }
    }
    
    /**
     * 포카드 패 생성
     */
    private fun generateFourOfAKindHand(
        cards: MutableList<Card>, 
        rank: CardRank, 
        sameRankCards: List<Card>, 
        allCards: List<Card>, 
        includeJoker: Boolean
    ) {
        // 같은 숫자 카드 추가
        val countToAdd = if (includeJoker) 3 else 4
        for (i in 0 until countToAdd) {
            if (i < sameRankCards.size) {
                cards.add(Card(sameRankCards[i].suit, sameRankCards[i].rank))
            }
        }
        
        // 조커 추가
        if (includeJoker) {
            cards.add(Card.createJoker())
        }
        
        // 킥커 카드 추가
        val otherCards = allCards.filter { it.rank != rank }.shuffled()
        if (otherCards.isNotEmpty()) {
            cards.add(Card(otherCards[0].suit, otherCards[0].rank))
        } else {
            // 다른 랭크의 카드가 없으면 임의 생성
            val suits = CardSuit.values().filter { it != CardSuit.JOKER }
            val ranks = CardRank.values().filter { it != CardRank.JOKER && it != rank }
            cards.add(Card(suits.random(), ranks.random()))
        }
    }
    
    /**
     * 풀하우스 패 생성
     */
    private fun generateFullHouseHand(
        cards: MutableList<Card>, 
        ranksWithThree: Map<CardRank, List<Card>>, 
        ranksWithTwo: Map<CardRank, List<Card>>, 
        includeJoker: Boolean
    ) {
        // 트리플용 랭크와 페어용 랭크 선택
        val tripleRank = ranksWithThree.keys.first()
        val pairRank = ranksWithTwo.keys.filter { it != tripleRank }.firstOrNull() ?: ranksWithTwo.keys.first()
        
        // 트리플 카드 추가
        val tripleCards = ranksWithThree[tripleRank]!!
        for (i in 0 until (if (includeJoker) 2 else 3)) {
            if (i < tripleCards.size) {
                cards.add(Card(tripleCards[i].suit, tripleCards[i].rank))
            }
        }
        
        // 페어 카드 추가
        val pairCards = ranksWithTwo[pairRank]!!
        for (i in 0 until 2) {
            if (i < pairCards.size && cards.size < (if (includeJoker) 4 else 5)) {
                cards.add(Card(pairCards[i].suit, pairCards[i].rank))
            }
        }
        
        // 조커 추가
        if (includeJoker) {
            cards.add(Card.createJoker())
        }
    }
    
    /**
     * 대체 족보 생성 (덱에서 무작위로 선택)
     */
    private fun generateAlternateHand(
        cards: MutableList<Card>, 
        originalHandType: String, 
        deck: List<Card>, 
        includeJoker: Boolean
    ) {
        // 덱을 섞음
        val shuffledDeck = deck.shuffled()
        
        // 카드 선택
        for (i in 0 until (if (includeJoker) baseCardCount - 1 else baseCardCount)) {
            if (i < shuffledDeck.size) {
                cards.add(Card(shuffledDeck[i].suit, shuffledDeck[i].rank))
            }
        }
        
        // 조커 추가
        if (includeJoker) {
            cards.add(Card.createJoker())
        }
        
        // 카드가 부족한 경우 채우기
        if (cards.size < baseCardCount) {
            val usedCards = cards.map { Pair(it.suit, it.rank) }.toMutableSet()
            fillRemainingCards(cards, usedCards)
        }
    }
    
    /**
     * 기존 좋은 패 생성 로직 (덱이 없는 경우 사용)
     */
    private fun generateDefaultGoodHand(
        cards: MutableList<Card>, 
        handType: String, 
        cardsToGenerate: Int, 
        includeJoker: Boolean
    ) {
        when (handType) {
            "royal_flush" -> {
                // 로얄 플러시 (스페이드 10, J, Q, K, A)
                val suit = CardSuit.SPADE
                cards.add(Card(suit, CardRank.TEN))
                cards.add(Card(suit, CardRank.JACK))
                cards.add(Card(suit, CardRank.QUEEN))
                cards.add(Card(suit, CardRank.KING))
                
                // 조커 추가 여부에 따라 A를 조커로 대체하거나 그대로 둠
                if (includeJoker) {
                    cards.add(Card.createJoker())
                } else {
                    cards.add(Card(suit, CardRank.ACE))
                }
            }
            "straight_flush" -> {
                // 스트레이트 플러시 (같은 무늬의 연속된 숫자)
                val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                
                for (i in 0 until cardsToGenerate) {
                    val rankValue = startRank + i
                    val rank = CardRank.values().find { it.value == rankValue }
                        ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                        ?: CardRank.ACE
                    
                    cards.add(Card(suit, rank))
                }
                
                // 조커 추가
                if (includeJoker) {
                    cards.add(Card.createJoker())
                }
            }
            "four_of_a_kind" -> {
                // 포카드 (같은 숫자 4장)
                val rank = CardRank.values().filter { it != CardRank.JOKER }.random()
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                
                // 조커가 포함된 경우 같은 숫자 3장 + 조커 + 다른 카드 1장
                if (includeJoker) {
                    // 같은 숫자 3장
                    for (i in 0 until 3) {
                        cards.add(Card(suits[i], rank))
                    }
                    
                    // 조커 추가
                    cards.add(Card.createJoker())
                    
                    // 다른 숫자 1장
                    var otherRank: CardRank
                    do {
                        otherRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    } while (otherRank == rank)
                    
                    cards.add(Card(suits.random(), otherRank))
                } else {
                    // 같은 숫자 4장
                    for (i in 0 until 4) {
                        cards.add(Card(suits[i], rank))
                    }
                    
                    // 다른 숫자 1장
                    var otherRank: CardRank
                    do {
                        otherRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                    } while (otherRank == rank)
                    
                    cards.add(Card(suits.random(), otherRank))
                }
            }
            "full_house" -> {
                // 풀하우스 (트리플 + 원페어)
                val tripleRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                var pairRank: CardRank
                do {
                    pairRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                } while (pairRank == tripleRank)
                
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                
                if (includeJoker) {
                    // 조커가 있는 경우: 같은 숫자 2장 + 다른 같은 숫자 2장 + 조커
                    // 같은 숫자 2장
                    for (i in 0 until 2) {
                        cards.add(Card(suits[i], tripleRank))
                    }
                    
                    // 다른 같은 숫자 2장
                    for (i in 0 until 2) {
                        cards.add(Card(suits[i], pairRank))
                    }
                    
                    // 조커 추가
                    cards.add(Card.createJoker())
                } else {
                    // 같은 숫자 3장
                    for (i in 0 until 3) {
                        cards.add(Card(suits[i], tripleRank))
                    }
                    
                    // 다른 같은 숫자 2장
                    for (i in 0 until 2) {
                        cards.add(Card(suits[i], pairRank))
                    }
                }
            }
            "flush" -> {
                // 플러시 (같은 무늬 5장)
                val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                val ranks = CardRank.values().filter { it != CardRank.JOKER }.shuffled().take(cardsToGenerate)
                
                for (rank in ranks) {
                    cards.add(Card(suit, rank))
                }
                
                // 조커 추가
                if (includeJoker) {
                    cards.add(Card.createJoker())
                }
            }
            else -> { // 스트레이트
                // 스트레이트 (연속된 숫자 5장)
                val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }
                
                for (i in 0 until cardsToGenerate) {
                    val rankValue = startRank + i
                    val rank = CardRank.values().find { it.value == rankValue }
                        ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                        ?: CardRank.ACE
                    
                    cards.add(Card(suits.random(), rank))
                }
                
                // 조커 추가
                if (includeJoker) {
                    cards.add(Card.createJoker())
                }
            }
        }
    }
    
    /**
     * 랜덤 카드 한 장 생성
     */
    fun createRandomCard(usedCards: MutableSet<Pair<CardSuit, CardRank>>): Card {
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        // 100번까지만 시도 (무한 루프 방지)
        for (i in 0 until 100) {
            val suit = suits.random()
            val rank = ranks.random()
            val cardPair = Pair(suit, rank)
            
            if (cardPair !in usedCards) {
                usedCards.add(cardPair)
                return Card(suit, rank)
            }
        }
        
        // 모든 시도가 실패하면 중복된 카드라도 생성
        val suit = suits.random()
        val rank = ranks.random()
        return Card(suit, rank)
    }
} 