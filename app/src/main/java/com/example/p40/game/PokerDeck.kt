package com.example.p40.game

/**
 * 포커 덱 관리 및 족보 판단 클래스
 */
class PokerDeck {
    private val cards = mutableListOf<Card>()
    private var hasJoker = false
    
    // 현재 플레이어가 가진 5장의 카드
    var playerHand = mutableListOf<Card>()
    
    // 덱 초기화
    fun initializeDeck() {
        cards.clear()
        
        // 52장의 카드 생성
        for (suit in CardSuit.values()) {
            if (suit == CardSuit.JOKER) continue
            
            for (rank in CardRank.values()) {
                if (rank == CardRank.JOKER) continue
                
                cards.add(Card(suit, rank))
            }
        }
        
        // 덱 섞기
        cards.shuffle()
    }
    
    // 조커 카드 추가
    fun addJoker() {
        if (!hasJoker) {
            cards.add(Card.createJoker())
            hasJoker = true
            // 덱 다시 섞기
            cards.shuffle()
        }
    }
    
    // 5장의 카드 뽑기
    fun draw5Cards(): List<Card> {
        val drawnCards = mutableListOf<Card>()
        
        // 덱에 카드가 5장 미만이면 덱 초기화
        if (cards.size < 5) {
            initializeDeck()
            if (hasJoker) {
                addJoker()
            }
        }
        
        // 5장 뽑기
        for (i in 0 until 5) {
            if (cards.isNotEmpty()) {
                drawnCards.add(cards.removeAt(0))
            }
        }
        
        playerHand = drawnCards.toMutableList()
        return drawnCards
    }
    
    // 선택된 카드 교체
    fun replaceSelectedCards(): List<Card> {
        val indexesToReplace = mutableListOf<Int>()
        
        // 교체할 카드 인덱스 찾기
        for (i in playerHand.indices) {
            if (playerHand[i].isSelected) {
                indexesToReplace.add(i)
            }
        }
        
        // 덱에 카드가 부족하면 덱 리셋 (교체할 카드 수보다 덱에 카드가 적을 경우)
        if (cards.size < indexesToReplace.size) {
            val remainingCards = cards.toList() // 현재 남은 카드 저장
            initializeDeck()
            if (hasJoker) {
                addJoker()
            }
            // 사용했던 카드와 플레이어 핸드의 카드는 제외
            cards.removeAll(remainingCards)
            cards.removeAll(playerHand)
            cards.shuffle()
        }
        
        // 선택된 카드 교체
        for (index in indexesToReplace) {
            if (cards.isNotEmpty()) {
                playerHand[index] = cards.removeAt(0)
            }
        }
        
        // 모든 카드 선택 상태 초기화
        for (card in playerHand) {
            card.isSelected = false
        }
        
        return playerHand
    }
    
    // 현재 족보 확인
    fun evaluateHand(): PokerHand {
        val tempHand = playerHand.toMutableList()
        
        // 조커가 있으면 가장 유리한 카드로 대체하여 평가
        val jokerIndex = tempHand.indexOfFirst { it.suit == CardSuit.JOKER }
        if (jokerIndex != -1) {
            // 여기서는 간단히 구현 - 조커는 평가 시 가장 유리한 카드로 대체됨
            // 실제 구현에서는 모든 가능한 조합을 시도하여 최적의 족보 찾아야 함
            tempHand.removeAt(jokerIndex) // 일단 조커 제거
            
            // 임시 구현: 로얄 플러시 가능성 확인
            if (tempHand.size == 4) {
                // 간단한 대체 로직
                tempHand.add(Card(CardSuit.SPADE, CardRank.ACE))
            }
        }
        
        // 로얄 플러시 체크
        if (isRoyalFlush(tempHand)) return PokerHand.ROYAL_FLUSH
        
        // 스트레이트 플러시 체크
        if (isStraightFlush(tempHand)) return PokerHand.STRAIGHT_FLUSH
        
        // 포카드 체크
        if (isFourOfAKind(tempHand)) return PokerHand.FOUR_OF_A_KIND
        
        // 풀하우스 체크
        if (isFullHouse(tempHand)) return PokerHand.FULL_HOUSE
        
        // 플러시 체크
        if (isFlush(tempHand)) return PokerHand.FLUSH
        
        // 스트레이트 체크
        if (isStraight(tempHand)) return PokerHand.STRAIGHT
        
        // 트리플 체크
        if (isThreeOfAKind(tempHand)) return PokerHand.THREE_OF_A_KIND
        
        // 투페어 체크
        if (isTwoPair(tempHand)) return PokerHand.TWO_PAIR
        
        // 원페어 체크
        if (isOnePair(tempHand)) return PokerHand.ONE_PAIR
        
        // 하이카드
        return PokerHand.HIGH_CARD
    }
    
    // 족보 판정 메서드들
    private fun isRoyalFlush(hand: List<Card>): Boolean {
        if (!isFlush(hand)) return false
        
        val ranks = hand.map { it.rank }
        return ranks.contains(CardRank.TEN) &&
                ranks.contains(CardRank.JACK) &&
                ranks.contains(CardRank.QUEEN) &&
                ranks.contains(CardRank.KING) &&
                ranks.contains(CardRank.ACE)
    }
    
    private fun isStraightFlush(hand: List<Card>): Boolean {
        return isFlush(hand) && isStraight(hand)
    }
    
    private fun isFourOfAKind(hand: List<Card>): Boolean {
        val rankGroups = hand.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 4 }
    }
    
    private fun isFullHouse(hand: List<Card>): Boolean {
        val rankGroups = hand.groupBy { it.rank }
        return rankGroups.size == 2 && rankGroups.any { it.value.size == 3 }
    }
    
    private fun isFlush(hand: List<Card>): Boolean {
        val suits = hand.map { it.suit }.toSet()
        return suits.size == 1
    }
    
    private fun isStraight(hand: List<Card>): Boolean {
        val sortedValues = hand.map { it.rank.value }.sorted()
        
        // A,2,3,4,5 스트레이트 특수 처리
        if (sortedValues == listOf(1, 2, 3, 4, 13)) return true
        
        // 일반적인 스트레이트 확인
        for (i in 1 until sortedValues.size) {
            if (sortedValues[i] != sortedValues[i-1] + 1) {
                return false
            }
        }
        return true
    }
    
    private fun isThreeOfAKind(hand: List<Card>): Boolean {
        val rankGroups = hand.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 3 }
    }
    
    private fun isTwoPair(hand: List<Card>): Boolean {
        val rankGroups = hand.groupBy { it.rank }
        val pairs = rankGroups.filter { it.value.size >= 2 }
        return pairs.size >= 2
    }
    
    private fun isOnePair(hand: List<Card>): Boolean {
        val rankGroups = hand.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 2 }
    }
} 