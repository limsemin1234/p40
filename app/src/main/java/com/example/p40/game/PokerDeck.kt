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
        hasJoker = false
        
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
    
    /**
     * 사용자 정의 카드로 덱 초기화
     */
    fun initializeWithCards(userCards: List<Card>) {
        cards.clear()
        hasJoker = false
        
        // 사용자 정의 카드 추가
        userCards.forEach { card ->
            // 조커 카드 확인
            if (card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER) {
                hasJoker = true
            }
            
            // 복사본 생성하여 추가 (원본 카드의 선택 상태 초기화)
            cards.add(Card(card.suit, card.rank, isSelected = false))
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
        return drawCards(5)
    }
    
    // 지정된 수의 카드 뽑기
    fun drawCards(count: Int): List<Card> {
        val drawnCards = mutableListOf<Card>()
        
        // 덱에 카드가 부족하면 덱 초기화
        if (cards.size < count) {
            initializeDeck()
            if (hasJoker) {
                addJoker()
            }
        }
        
        // 지정된 수만큼 카드 뽑기
        for (i in 0 until count) {
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
            val card = playerHand[i]
            // 조커 카드는 교체에서 제외 (별 조커 또는 문양 조커)
            if (card.isSelected && !CardUtils.isJokerCard(card)) {
                indexesToReplace.add(i)
            } else if (CardUtils.isJokerCard(card)) {
                // 조커 카드의 선택 상태는 항상 false로 리셋
                card.isSelected = false
            }
        }
        
        // 교체할 카드가 없으면 현재 핸드 반환
        if (indexesToReplace.isEmpty()) {
            return playerHand
        }
        
        // 덱에 카드가 부족하면 덱 리셋 (교체할 카드 수보다 덱에 카드가 적을 경우)
        if (cards.size < indexesToReplace.size) {
            val remainingCards = cards.toHashSet() // 현재 남은 카드 저장 (HashSet으로 변환)
            
            // 현재 핸드의 카드를 HashSet으로 변환 (빠른 검색을 위해)
            val handCardKeys = playerHand.map { CardUtils.getCardKey(it) }.toHashSet()
            
            // 덱 초기화
            initializeDeck()
            if (hasJoker) {
                addJoker()
            }
            
            // 사용했던 카드 제거 (HashSet 검색으로 최적화)
            cards.removeAll { card -> 
                CardUtils.getCardKey(card) in handCardKeys || 
                remainingCards.any { CardUtils.isSameCard(it, card) }
            }
            
            // 덱 다시 섞기
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
        
        // 일반 조커 카드가 있는지 확인 (별 모양 조커)
        val jokerIndex = tempHand.indexOfFirst { CardUtils.isStarJoker(it) }
        
        // 일반 조커가 있고, 아직 변환되지 않은 경우에만 자동 변환 로직 적용
        if (jokerIndex != -1) {
            // 조커를 임시로 제거하고 가장 유리한 카드로 대체
            tempHand.removeAt(jokerIndex)
            
            // 남은 4장의 카드로 가능한 최적의 카드 찾기
            if (tempHand.size == 4) {
                val suits = tempHand.map { it.suit }.distinct()
                val ranks = tempHand.map { it.rank }.sortedBy { it.value }
                
                // 가능한 추가 전략:
                // 1. 같은 숫자가 3장 있으면 -> 포카드를 위해 같은 숫자 추가
                val rankGroups = tempHand.groupBy { it.rank }
                val maxRankGroup = rankGroups.maxByOrNull { it.value.size }
                
                if (maxRankGroup != null && maxRankGroup.value.size >= 3) {
                    // 같은 숫자 3장 이상 -> 포카드 만들기
                    tempHand.add(Card(CardSuit.SPADE, maxRankGroup.key))
                } else if (suits.size == 1) {
                    // 같은 무늬 4장 -> 플러시 만들기
                    tempHand.add(Card(suits.first(), CardRank.ACE))
                } else if (isNearStraight(tempHand)) {
                    // 연속된 숫자에 가까움 -> 스트레이트 만들기
                    val missingRank = findMissingRankForStraight(tempHand)
                    if (missingRank != null) {
                        tempHand.add(Card(CardSuit.SPADE, missingRank))
                    } else {
                        // 미싱 랭크를 찾지 못했을 경우 기본 전략
                        tempHand.add(Card(CardSuit.SPADE, CardRank.ACE))
                    }
                } else {
                    // 기본 전략: 최고 족보인 로얄 플러시를 위해 스페이드 에이스 추가
                    tempHand.add(Card(CardSuit.SPADE, CardRank.ACE))
                }
            }
        }
        
        // 효율성을 위해 필요한 카드 정보를 한 번만 계산
        val suits = tempHand.groupBy { it.suit }
        val ranks = tempHand.groupBy { it.rank }
        val rankValues = tempHand.map { it.rank.value }.sorted()
        val isAllSameSuit = suits.size == 1
        
        // 로얄 플러시 체크
        if (isAllSameSuit && 
            ranks.containsKey(CardRank.TEN) && 
            ranks.containsKey(CardRank.JACK) && 
            ranks.containsKey(CardRank.QUEEN) && 
            ranks.containsKey(CardRank.KING) && 
            ranks.containsKey(CardRank.ACE)) {
            return RoyalFlush()
        }
        
        // 스트레이트 플러시 체크
        val isStraight = checkStraight(rankValues)
        if (isAllSameSuit && isStraight) {
            return StraightFlush()
        }
        
        // 포카드 체크
        if (ranks.any { it.value.size >= 4 }) {
            return FourOfAKind()
        }
        
        // 풀하우스 체크
        if (ranks.size == 2 && ranks.any { it.value.size == 3 }) {
            return FullHouse()
        }
        
        // 플러시 체크
        if (isAllSameSuit) {
            return Flush()
        }
        
        // 스트레이트 체크
        if (isStraight) {
            return Straight()
        }
        
        // 트리플 체크
        if (ranks.any { it.value.size >= 3 }) {
            return ThreeOfAKind()
        }
        
        // 투페어 체크
        val pairs = ranks.filter { it.value.size >= 2 }
        if (pairs.size >= 2) {
            return TwoPair()
        }
        
        // 원페어 체크
        if (pairs.size == 1) {
            return OnePair()
        }
        
        // 하이카드
        return HighCard()
    }
    
    // 스트레이트 확인 (최적화된 버전)
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
    
    // 스트레이트에 가까운지 확인 (연속된 4장의 카드가 있는지)
    private fun isNearStraight(hand: List<Card>): Boolean {
        val sortedValues = hand.map { it.rank.value }.sorted()
        val uniqueValues = sortedValues.distinct()
        
        if (uniqueValues.size < 4) return false
        
        // 연속된 숫자 4개 확인
        for (i in 0 until uniqueValues.size - 3) {
            if (uniqueValues[i + 3] - uniqueValues[i] == 3) {
                return true
            }
        }
        
        // A, 2, 3, 4 or 10, J, Q, K 특수 케이스 확인
        if (uniqueValues.contains(1) && uniqueValues.contains(2) && 
            uniqueValues.contains(3) && uniqueValues.contains(4)) {
            return true
        }
        
        if (uniqueValues.contains(10) && uniqueValues.contains(11) && 
            uniqueValues.contains(12) && uniqueValues.contains(13)) {
            return true
        }
        
        return false
    }
    
    // 스트레이트를 만들기 위한 누락된 랭크 찾기
    private fun findMissingRankForStraight(hand: List<Card>): CardRank? {
        val values = hand.map { it.rank.value }.sorted().distinct()
        
        // 이미 4장이 연속된 경우, 마지막 카드를 찾음
        for (i in 0 until values.size - 3) {
            if (values[i + 3] - values[i] == 3 && values[i] >= 1) {
                // 앞쪽에 카드가 누락된 경우
                if (i == 0 && values[i] > 1) {
                    return getCardRankByValue(values[i] - 1)
                }
                // 뒤쪽에 카드가 누락된 경우
                else if (values[i + 3] < 13) {
                    return getCardRankByValue(values[i + 3] + 1)
                }
                // 중간에 카드가 누락된 경우
                else {
                    for (j in i until i + 3) {
                        if (values[j + 1] - values[j] > 1) {
                            return getCardRankByValue(values[j] + 1)
                        }
                    }
                }
            }
        }
        
        // A, 2, 3, 4를 가지고 있다면 5를 추가
        if (values.containsAll(listOf(1, 2, 3, 4))) {
            return CardRank.FIVE
        }
        
        // 10, J, Q, K를 가지고 있다면 A를 추가
        if (values.containsAll(listOf(10, 11, 12, 13))) {
            return CardRank.ACE
        }
        
        return null
    }
    
    // 값으로 CardRank 반환
    private fun getCardRankByValue(value: Int): CardRank? {
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
            else -> null
        }
    }
} 