package com.example.p40.game

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.p40.DeckBuilderFragment
import com.example.p40.R
import kotlin.random.Random

/**
 * 포커 카드 관리 클래스
 * - 카드 생성, 교체, UI 처리 등을 담당
 * - GameFragment에서 분리된 기능
 */
class PokerCardManager(
    private val context: Context,
    private val rootView: View,
    private val listener: PokerCardListener
) {
    // 카드 관련 UI 요소들
    private val cardInfoLayout: LinearLayout = rootView.findViewById(R.id.cardInfoLayout)
    private val cardButtonsLayout: LinearLayout = rootView.findViewById(R.id.cardButtonsLayout)
    private val btnDrawPokerCards: Button = rootView.findViewById(R.id.btnDrawPokerCards)
    private val btnAddCard: Button = rootView.findViewById(R.id.btnAddCard)
    private val replaceButton: Button = rootView.findViewById(R.id.btnReplaceCards)
    private val replaceAllButton: Button = rootView.findViewById(R.id.btnReplaceAllCards)
    private val confirmButton: Button = rootView.findViewById(R.id.btnConfirmHand)
    private val replaceCountText: TextView = rootView.findViewById(R.id.tvReplaceCount)
    private val currentHandText: TextView = rootView.findViewById(R.id.tvCurrentHand)
    private val handDescriptionText: TextView = rootView.findViewById(R.id.tvHandDescription)
    
    private val cardViews: List<CardView> = listOf(
        rootView.findViewById(R.id.cardView1),
        rootView.findViewById(R.id.cardView2),
        rootView.findViewById(R.id.cardView3),
        rootView.findViewById(R.id.cardView4),
        rootView.findViewById(R.id.cardView5),
        rootView.findViewById(R.id.cardView6),
        rootView.findViewById(R.id.cardView7)
    )
    
    // 기본 카드 수 및 최대 카드 수 설정
    private val baseCardCount = 5 // 기본 5장
    private val maxExtraCards = 2 // 최대 2장 추가 가능
    
    // 현재 사용 중인 카드 수 (기본 5장, 최대 7장까지 확장 가능)
    private var purchasedExtraCards = 0 // 구매한 추가 카드 수
    private val activeCardCount: Int
        get() = baseCardCount + purchasedExtraCards
    
    // 추가 카드 구매 비용
    private val extraCardCost = 100 // 추가 카드 1장당 100 자원
    
    private val cards = mutableListOf<Card>()
    private var replacesLeft = 2 // 교체 가능한 횟수
    private val selectedCardIndexes = mutableSetOf<Int>() // 선택된 카드의 인덱스
    
    // 현재 카드 게임이 진행 중인지 여부
    private var isGameActive = false

    // 포커 카드 매니저 인터페이스
    interface PokerCardListener {
        // 자원 관련
        fun getResource(): Int
        fun useResource(amount: Int): Boolean
        public fun updateGameInfoUI()
        
        // 포커 효과 적용
        fun applyPokerHandEffect(pokerHand: PokerHand)
    }
    
    init {
        setupListeners()
        updateAddCardButtonState()
        updateDrawCardButtonText()
    }
    
    private fun setupListeners() {
        // 카드 선택 이벤트 설정
        cardViews.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                toggleCardSelection(index)
            }
        }
        
        // 교체 버튼 이벤트
        replaceButton.setOnClickListener {
            replaceSelectedCards()
        }
        
        // 전체교체 버튼 이벤트
        replaceAllButton.setOnClickListener {
            replaceAllNonJokerCards()
        }
        
        // 확인 버튼 이벤트
        confirmButton.setOnClickListener {
            confirmSelection()
        }
        
        // 카드 추가 버튼 이벤트
        btnAddCard.setOnClickListener {
            purchaseExtraCard()
        }
        
        // 포커 카드 뽑기 버튼 이벤트
        btnDrawPokerCards.setOnClickListener {
            // 자원 소모 비용 설정
            val cardDrawCost = 50 // 기본 비용 50 자원
            
            // 현재 자원 확인
            val currentResource = listener.getResource()
            
            if (currentResource >= cardDrawCost) {
                // 자원 차감
                if (listener.useResource(cardDrawCost)) {
                    // 자원 차감 성공 시 카드 처리 시작
                    startPokerCards(1) // 기본 웨이브 1로 시작
                    
                    // 자원 정보 업데이트
                    listener.updateGameInfoUI()
                }
            } else {
                // 자원 부족 메시지
                Toast.makeText(context, "자원이 부족합니다! (필요: $cardDrawCost)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 추가 카드 구매
    private fun purchaseExtraCard() {
        // 이미 최대로 추가 구매한 경우
        if (purchasedExtraCards >= maxExtraCards) {
            Toast.makeText(context, "이미 최대 카드 수에 도달했습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 게임 진행 중인 경우 추가 구매 불가
        if (isGameActive) {
            Toast.makeText(context, "현재 게임이 진행 중입니다. 다음 게임에서 추가 카드를 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 자원 확인
        val currentResource = listener.getResource()
        if (currentResource >= extraCardCost) {
            // 자원 차감
            if (listener.useResource(extraCardCost)) {
                // 추가 카드 수 증가
                purchasedExtraCards++
                
                // 버튼 상태 업데이트
                updateAddCardButtonState()
                
                // 카드 뽑기 버튼 텍스트 업데이트
                updateDrawCardButtonText()
                
                // 자원 정보 업데이트
                listener.updateGameInfoUI()
                
                // 토스트 메시지 표시
                Toast.makeText(context, "다음 카드 게임에서 ${baseCardCount + purchasedExtraCards}장의 카드가 제공됩니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 자원 부족 메시지
            Toast.makeText(context, "자원이 부족합니다! (필요: $extraCardCost)", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 외부에서 호출할 카드 게임 시작 메서드
    fun startPokerCards(waveNumber: Int) {
        // 상태 초기화
        cards.clear()
        selectedCardIndexes.clear()
        replacesLeft = 2
        isGameActive = true
        
        // UI 초기화
        cardInfoLayout.visibility = View.VISIBLE
        cardButtonsLayout.visibility = View.GONE
        
        // 카드 생성 (추가 구매한 카드 수 반영)
        dealCards(waveNumber)
        
        // UI 업데이트
        updateUI()
    }
    
    // UI 관련 메서드들
    private fun updateAddCardButtonState() {
        if (purchasedExtraCards >= maxExtraCards) {
            btnAddCard.isEnabled = false
            btnAddCard.text = "최대 카드 수\n도달"
        } else {
            btnAddCard.isEnabled = true
            btnAddCard.text = "카드 추가 +1\n(💰 $extraCardCost 자원)"
        }
    }
    
    private fun updateDrawCardButtonText() {
        if (purchasedExtraCards > 0) {
            btnDrawPokerCards.text = "포커 카드 뽑기\n(${baseCardCount + purchasedExtraCards}장, 💰 50 자원)"
        } else {
            btnDrawPokerCards.text = "포커 카드 뽑기\n(💰 50 자원)"
        }
    }
    
    // 전체 UI 업데이트
    private fun updateUI() {
        // 기본 UI 업데이트
        updateBasicCardUI()
        
        // 카드가 5장을 초과하는 경우 최적의 5장 조합 찾기
        if (cards.size > 5) {
            val bestFiveCards = findBestFiveCards(cards)
            highlightBestCards(bestFiveCards)
            
            // 최적의 조합으로 족보 업데이트
            val tempDeck = PokerDeck()
            tempDeck.playerHand = bestFiveCards.toMutableList()
            val pokerHand = tempDeck.evaluateHand()
            
            // 족보 텍스트 업데이트
            currentHandText.text = "현재 족보: ${pokerHand.handName}"
            handDescriptionText.text = "효과: ${pokerHand.getDescription()}"
        } else {
            // 5장 이하인 경우 일반 족보 평가
            val pokerDeck = PokerDeck()
            pokerDeck.playerHand = cards.toMutableList()
            val pokerHand = pokerDeck.evaluateHand()
            
            // 족보 텍스트 업데이트
            currentHandText.text = "현재 족보: ${pokerHand.handName}"
            handDescriptionText.text = "효과: ${pokerHand.getDescription()}"
        }
        
        // 교체 버튼 활성화/비활성화
        replaceButton.isEnabled = replacesLeft > 0 && selectedCardIndexes.isNotEmpty()
        replaceAllButton.isEnabled = replacesLeft > 0
        
        // 교체 횟수 텍스트 업데이트
        replaceCountText.text = "교체 가능 횟수: $replacesLeft"
    }
    
    // 패널 초기 상태로 복귀
    private fun resetPanel() {
        cardInfoLayout.visibility = View.GONE
        cardButtonsLayout.visibility = View.VISIBLE
        isGameActive = false
    }
    
    // 카드 생성 관련 메서드들
    private fun dealCards(waveNumber: Int) {
        // 웨이브 번호에 따라 더 좋은 카드가 나올 확률 증가
        // 기본 확률 0.15에서 웨이브 번호에 따라 증가
        val goodHandProbability = minOf(0.15f + (waveNumber * 0.03f), 0.5f)
        
        // 좋은 패가 나올 확률 계산
        if (Random.nextFloat() < goodHandProbability) {
            // 좋은 패 생성 (스트레이트 이상)
            generateGoodHand(waveNumber)
        } else {
            // 일반 랜덤 패 생성
            generateRandomHand()
        }
        
        // 필요한 경우 추가 카드 생성
        addExtraCardsIfNeeded()
    }
    
    // 랜덤 패 생성
    private fun generateRandomHand() {
        cards.clear()
        
        // 저장된 덱 불러오기
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
        
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
                fillRemainingCards(usedCards)
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
    }
    
    // 부족한 카드를 채우는 함수
    private fun fillRemainingCards(usedCards: MutableSet<Pair<CardSuit, CardRank>>) {
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
    
    // 좋은 패 생성
    private fun generateGoodHand(waveNumber: Int) {
        // 웨이브 번호에 따라 더 좋은 족보 가능성 증가
        val handType = when {
            waveNumber >= 8 && Random.nextFloat() < 0.2f -> "royal_flush"
            waveNumber >= 6 && Random.nextFloat() < 0.3f -> "straight_flush"
            waveNumber >= 5 && Random.nextFloat() < 0.4f -> "four_of_a_kind"
            waveNumber >= 4 && Random.nextFloat() < 0.5f -> "full_house"
            waveNumber >= 3 && Random.nextFloat() < 0.6f -> "flush"
            else -> "straight"
        }
        
        cards.clear()
        
        // 저장된 덱 불러오기
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
        
        // 저장된 덱이 있고 충분한 카드가 있는 경우
        if (savedDeck != null && savedDeck.size >= 5) {
            // 조커 카드 추가 여부 결정 (20% 확률)
            val includeJoker = Random.nextFloat() < 0.2f
            
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
                        generateFlushLikeHand(handType, suitWithMostCards.key, suitWithMostCards.value, includeJoker)
                    } else {
                        // 없으면 대체 족보 생성
                        generateAlternateHand(handType, savedDeck, includeJoker)
                    }
                }
                "four_of_a_kind" -> {
                    // 같은 숫자 카드가 4장 있는지 확인 또는 3장 + 조커로 구성 가능한지
                    if (rankWithMostCards != null && (rankWithMostCards.value.size >= 4 || 
                        (rankWithMostCards.value.size >= 3 && includeJoker))) {
                        generateFourOfAKindHand(rankWithMostCards.key, rankWithMostCards.value, savedDeck, includeJoker)
                    } else {
                        // 없으면 대체 족보 생성
                        generateAlternateHand(handType, savedDeck, includeJoker)
                    }
                }
                "full_house" -> {
                    // 트리플과 페어를 구성할 수 있는지 확인
                    val ranksWithThreeOrMore = cardsByRank.filter { it.value.size >= 3 }
                    val ranksWithTwoOrMore = cardsByRank.filter { it.value.size >= 2 }
                    
                    if (ranksWithThreeOrMore.isNotEmpty() && ranksWithTwoOrMore.size >= 2) {
                        generateFullHouseHand(ranksWithThreeOrMore, ranksWithTwoOrMore, includeJoker)
                    } else {
                        // 없으면 대체 족보 생성
                        generateAlternateHand(handType, savedDeck, includeJoker)
                    }
                }
                else -> { // "straight"
                    // 스트레이트 구성 가능 여부 확인은 복잡하므로 간단하게 대체 족보 생성
                    generateAlternateHand(handType, savedDeck, includeJoker)
                }
            }
        } else {
            // 저장된 덱이 없거나 카드가 부족한 경우, 기존 로직으로 카드 생성
            // 조커 카드 추가 여부 결정 (20% 확률)
            val includeJoker = Random.nextFloat() < 0.2f
            
            // 기본 4장 패 생성 (조커를 추가할 예정이면 한 장 적게 생성)
            val cardsToGenerate = if (includeJoker) baseCardCount - 1 else baseCardCount
            
            // 기존 로직 사용
            generateDefaultGoodHand(handType, cardsToGenerate, includeJoker)
        }
        
        // 카드 순서 섞기
        cards.shuffle()
    }
    
    // 필요한 경우 추가 카드 생성
    private fun addExtraCardsIfNeeded() {
        if (purchasedExtraCards > 0 && cards.size < activeCardCount) {
            // 저장된 덱 불러오기
            val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
            
            // 현재 사용 중인 카드 패턴 확인
            val usedCards = cards.map { Pair(it.suit, it.rank) }.toMutableSet()
            
            // 조커 카드가 이미 있는지 확인
            val hasJoker = cards.any { CardUtils.isJokerCard(it) }
            
            // 저장된 덱이 있는 경우
            if (savedDeck != null && savedDeck.isNotEmpty()) {
                // 덱에서 사용하지 않은 카드만 필터링
                val availableCards = savedDeck.filter { 
                    Pair(it.suit, it.rank) !in usedCards 
                }.shuffled()
                
                // 필요한 만큼 카드 추가
                var availableIndex = 0
                while (cards.size < activeCardCount && availableIndex < availableCards.size) {
                    // 조커 추가 확인 (10% 확률, 이미 조커가 있으면 추가 안함)
                    if (Random.nextFloat() < 0.1f && !hasJoker && cards.size < activeCardCount - 1) {
                        cards.add(Card.createJoker())
                    } else if (availableIndex < availableCards.size) {
                        // 일반 카드 추가
                        val card = availableCards[availableIndex]
                        cards.add(Card(card.suit, card.rank))
                        availableIndex++
                    }
                }
                
                // 부족한 카드가 있으면 기본 방식으로 채우기
                if (cards.size < activeCardCount) {
                    fillRemainingExtraCards(usedCards, hasJoker)
                }
            } else {
                // 저장된 덱이 없는 경우 기본 방식으로 추가 카드 생성
                fillRemainingExtraCards(usedCards, hasJoker)
            }
        }
    }
    
    // 부족한 추가 카드 채우기
    private fun fillRemainingExtraCards(usedCards: MutableSet<Pair<CardSuit, CardRank>>, hasJoker: Boolean) {
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        // 필요한 만큼 추가 카드 생성
        while (cards.size < activeCardCount) {
            // 조커 카드 추가 여부 결정 (10%, 이미 조커가 있으면 추가하지 않음)
            if (Random.nextFloat() < 0.1f && !hasJoker) {
                // 조커 카드 추가
                cards.add(Card.createJoker())
            } else {
                // 일반 카드 추가
                var newCard: Card
                do {
                    val suit = suits.random()
                    val rank = ranks.random()
                    val cardPair = Pair(suit, rank)
                    
                    if (cardPair !in usedCards) {
                        newCard = Card(suit, rank)
                        usedCards.add(cardPair)
                        break
                    }
                } while (true)
                
                // 카드 추가
                cards.add(newCard)
            }
        }
    }
    
    // 플러시 계열 패 생성
    private fun generateFlushLikeHand(handType: String, suit: CardSuit, availableCards: List<Card>, includeJoker: Boolean) {
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
    
    // 포카드 패 생성
    private fun generateFourOfAKindHand(rank: CardRank, sameRankCards: List<Card>, allCards: List<Card>, includeJoker: Boolean) {
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
    
    // 풀하우스 패 생성
    private fun generateFullHouseHand(ranksWithThree: Map<CardRank, List<Card>>, ranksWithTwo: Map<CardRank, List<Card>>, includeJoker: Boolean) {
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
    
    // 대체 족보 생성 (덱에서 무작위로 선택)
    private fun generateAlternateHand(originalHandType: String, deck: List<Card>, includeJoker: Boolean) {
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
            fillRemainingCards(usedCards)
        }
    }
    
    // 기존 좋은 패 생성 로직 (덱이 없는 경우 사용)
    private fun generateDefaultGoodHand(handType: String, cardsToGenerate: Int, includeJoker: Boolean) {
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
    
    // UI 관련 메서드들
    private fun updateBasicCardUI() {
        for (i in 0 until cardViews.size) {
            val cardView = cardViews[i]
            
            // 활성화된 카드 인덱스 범위만 표시
            if (i < activeCardCount) {
                cardView.visibility = View.VISIBLE
                
                // 카드 정보가 있는 경우에만 표시
                if (i < cards.size) {
                    val card = cards[i]
                    
                    // 카드 정보 표시
                    val suitTextView = cardView.findViewById<TextView>(
                        when (i) {
                            0 -> R.id.tvCardSuit1
                            1 -> R.id.tvCardSuit2
                            2 -> R.id.tvCardSuit3
                            3 -> R.id.tvCardSuit4
                            4 -> R.id.tvCardSuit5
                            5 -> R.id.tvCardSuit6
                            else -> R.id.tvCardSuit7
                        }
                    )
                    
                    val rankTextView = cardView.findViewById<TextView>(
                        when (i) {
                            0 -> R.id.tvCardRank1
                            1 -> R.id.tvCardRank2
                            2 -> R.id.tvCardRank3
                            3 -> R.id.tvCardRank4
                            4 -> R.id.tvCardRank5
                            5 -> R.id.tvCardRank6
                            else -> R.id.tvCardRank7
                        }
                    )
                    
                    // 카드 무늬와 숫자 설정
                    suitTextView.text = card.suit.getSymbol()
                    suitTextView.setTextColor(card.suit.getColor())
                    
                    rankTextView.text = card.rank.getName()
                    rankTextView.setTextColor(card.suit.getColor())
                    
                    // 선택 상태 표시
                    if (i in selectedCardIndexes) {
                        cardView.setCardBackgroundColor(Color.YELLOW)
                    } else {
                        cardView.setCardBackgroundColor(Color.WHITE)
                    }
                    
                    // 조커 카드 체크
                    val isJoker = CardUtils.isJokerCard(card)
                    
                    // 카드 선택 가능 여부 설정 - 클릭 이벤트에만 적용
                    // 조커 카드는 항상 활성화(변환 가능), 다른 카드는 교체 횟수가 있을 때만 활성화
                    cardView.isEnabled = isJoker || replacesLeft > 0
                    
                    // 조커 카드인 경우 길게 누르면 변환 다이얼로그 표시
                    // 교체 횟수와 상관없이 항상 가능하도록 설정
                    if (isJoker) {
                        // 롱클릭 리스너 설정
                        cardView.setOnLongClickListener {
                            showJokerSelectionDialog(card, i)
                            true
                        }
                    } else {
                        cardView.setOnLongClickListener(null)
                    }
                }
            } else {
                // 활성화되지 않은 카드는 숨김
                cardView.visibility = View.GONE
            }
        }
    }
    
    // 최적의 카드 강조 표시 - 족보에 따라 관련 카드만 강조
    private fun highlightBestCards(bestFiveCards: List<Card>) {
        // 모든 카드를 일단 하얀색/노란색으로 초기화
        for (i in 0 until cards.size) {
            val cardView = cardViews[i]
            if (i in selectedCardIndexes) {
                cardView.setCardBackgroundColor(Color.YELLOW)
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
            }
        }
        
        // 족보를 분석하여 관련 카드만 초록색으로 표시
        val pokerDeck = PokerDeck()
        pokerDeck.playerHand = bestFiveCards.toMutableList()
        val pokerHand = pokerDeck.evaluateHand()
        
        // 족보에 따라 강조할 카드 결정
        val cardsToHighlight = findCardsToHighlight(bestFiveCards, pokerHand.handName)
        
        // 강조할 카드 초록색으로 표시
        for (i in 0 until cards.size) {
            if (i in selectedCardIndexes) continue // 선택된 카드는 건너뛰기
            
            val card = cards[i]
            if (cardsToHighlight.any { it.suit == card.suit && it.rank == card.rank }) {
                cardViews[i].setCardBackgroundColor(Color.GREEN)
            }
        }
    }
    
    // 족보에 따라 강조할 카드 찾기
    private fun findCardsToHighlight(bestCards: List<Card>, handName: String): List<Card> {
        // 족보별로 강조할 카드 결정
        return when (handName) {
            "원 페어" -> {
                // 같은 숫자 2장 찾기
                val rankGroups = bestCards.groupBy { it.rank }
                val pairRank = rankGroups.entries.find { it.value.size == 2 }?.key
                bestCards.filter { it.rank == pairRank }
            }
            "투 페어" -> {
                // 두 쌍의 같은 숫자 찾기
                val rankGroups = bestCards.groupBy { it.rank }
                val pairRanks = rankGroups.entries.filter { it.value.size == 2 }.map { it.key }
                bestCards.filter { it.rank in pairRanks }
            }
            "트리플" -> {
                // 같은 숫자 3장 찾기
                val rankGroups = bestCards.groupBy { it.rank }
                val tripleRank = rankGroups.entries.find { it.value.size == 3 }?.key
                bestCards.filter { it.rank == tripleRank }
            }
            "포카드" -> {
                // 같은 숫자 4장 찾기
                val rankGroups = bestCards.groupBy { it.rank }
                val fourOfAKindRank = rankGroups.entries.find { it.value.size == 4 }?.key
                bestCards.filter { it.rank == fourOfAKindRank }
            }
            "풀 하우스" -> {
                // 트리플 + 페어 찾기
                val rankGroups = bestCards.groupBy { it.rank }
                val tripleRank = rankGroups.entries.find { it.value.size == 3 }?.key
                val pairRank = rankGroups.entries.find { it.value.size == 2 }?.key
                bestCards.filter { it.rank == tripleRank || it.rank == pairRank }
            }
            "플러시" -> {
                // 같은 무늬 5장 - 모두 강조
                bestCards
            }
            "스트레이트" -> {
                // 연속된 숫자 5장 - 모두 강조
                bestCards
            }
            "스트레이트 플러시" -> {
                // 같은 무늬 연속된 숫자 5장 - 모두 강조
                bestCards
            }
            "로얄 플러시" -> {
                // 스페이드 10,J,Q,K,A - 모두 강조
                bestCards
            }
            else -> {
                // 하이카드인 경우 가장 높은 카드 1장만 강조
                val highestCard = bestCards.maxByOrNull { 
                    if (it.rank == CardRank.ACE) 14 else it.rank.value 
                }
                listOfNotNull(highestCard)
            }
        }
    }
    
    // 카드 선택 토글
    private fun toggleCardSelection(index: Int) {
        // 교체 횟수가 남아있는 경우에만 선택 가능
        if (replacesLeft <= 0) return
        
        if (index in selectedCardIndexes) {
            selectedCardIndexes.remove(index)
        } else {
            selectedCardIndexes.add(index)
        }
        
        updateUI()
    }
    
    // 선택된 카드 교체
    private fun replaceSelectedCards() {
        if (selectedCardIndexes.isEmpty() || replacesLeft <= 0) return
        
        // 저장된 덱 불러오기
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
        
        // 현재 사용 중인 카드 확인 (중복 방지)
        val usedCards = cards
            .filterIndexed { index, _ -> index !in selectedCardIndexes }
            .map { Pair(it.suit, it.rank) }
            .toMutableSet()
        
        // 선택된 카드 교체
        for (index in selectedCardIndexes) {
            // 저장된 덱이 있는 경우
            if (savedDeck != null && savedDeck.isNotEmpty()) {
                // 덱에서 사용하지 않은 카드 필터링
                val availableCards = savedDeck.filter { 
                    Pair(it.suit, it.rank) !in usedCards 
                }
                
                if (availableCards.isNotEmpty()) {
                    // 사용 가능한 카드 중 랜덤 선택
                    val randomCard = availableCards.random()
                    cards[index] = Card(randomCard.suit, randomCard.rank)
                    usedCards.add(Pair(randomCard.suit, randomCard.rank))
                } else {
                    // 덱에 사용 가능한 카드가 없는 경우, 기존 방식으로 랜덤 카드 생성
                    createRandomCard(index, usedCards)
                }
            } else {
                // 저장된 덱이 없는 경우, 기존 방식으로 랜덤 카드 생성
                createRandomCard(index, usedCards)
            }
        }
        
        // 교체 횟수 감소
        replacesLeft--
        
        // 선택 초기화
        selectedCardIndexes.clear()
        
        // UI 업데이트
        updateUI()
    }
    
    // 랜덤 카드 생성 헬퍼 메서드
    private fun createRandomCard(index: Int, usedCards: MutableSet<Pair<CardSuit, CardRank>>) {
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        var newCard: Card
        do {
            val suit = suits.random()
            val rank = ranks.random()
            val cardPair = Pair(suit, rank)
            
            if (cardPair !in usedCards) {
                newCard = Card(suit, rank)
                usedCards.add(cardPair)
                break
            }
        } while (true)
        
        cards[index] = newCard
    }
    
    // 전체 카드 교체 (조커 카드 제외)
    private fun replaceAllNonJokerCards() {
        if (replacesLeft <= 0) {
            Toast.makeText(context, "교체 횟수를 모두 사용했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 조커가 아닌 카드의 인덱스 찾기
        val nonJokerCardIndices = cards.indices.filter { index ->
            !CardUtils.isJokerCard(cards[index])
        }.toMutableSet()
        
        // 선택된 카드가 없는 경우 (조커 카드만 있는 경우) 메시지 표시
        if (nonJokerCardIndices.isEmpty()) {
            Toast.makeText(context, "교체할 일반 카드가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 저장된 덱 불러오기
        val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
        
        // 현재 사용 중인 카드 확인 (중복 방지)
        val usedCards = cards
            .filterIndexed { index, _ -> index !in nonJokerCardIndices }
            .map { Pair(it.suit, it.rank) }
            .toMutableSet()
        
        // 모든 일반 카드 교체
        if (savedDeck != null && savedDeck.isNotEmpty()) {
            // 사용 가능한 모든 카드를 섞음
            val availableCards = savedDeck.shuffled()
            var availableIndex = 0
            
            for (index in nonJokerCardIndices) {
                // 사용 가능한 카드가 남아있는지 확인
                var cardFound = false
                
                // 사용하지 않은 카드 찾기
                while (availableIndex < availableCards.size) {
                    val card = availableCards[availableIndex]
                    val cardPair = Pair(card.suit, card.rank)
                    availableIndex++
                    
                    if (cardPair !in usedCards) {
                        cards[index] = Card(card.suit, card.rank)
                        usedCards.add(cardPair)
                        cardFound = true
                        break
                    }
                }
                
                // 사용 가능한 카드를 모두 확인했지만 적합한 카드가 없는 경우
                if (!cardFound) {
                    // 기존 방식으로 랜덤 카드 생성
                    createRandomCard(index, usedCards)
                }
            }
        } else {
            // 저장된 덱이 없는 경우, 기존 방식으로 모든 카드 교체
            for (index in nonJokerCardIndices) {
                createRandomCard(index, usedCards)
            }
        }
        
        // 교체 횟수 감소
        replacesLeft--
        
        // 선택 초기화
        selectedCardIndexes.clear()
        
        // UI 업데이트
        updateUI()
        
        // 토스트 메시지 표시
        Toast.makeText(context, "${nonJokerCardIndices.size}장의 카드가 교체되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    // 카드 선택 확정
    private fun confirmSelection() {
        // 카드가 5장 이상인 경우 최적의 5장 조합 찾기
        val bestFiveCards = if (cards.size > 5) {
            findBestFiveCards(cards)
        } else {
            cards
        }
        
        // 조커 카드가 있는 경우 PokerDeck을 사용하여 가장 유리한 카드로 변환
        val pokerDeck = PokerDeck()
        pokerDeck.playerHand = bestFiveCards.toMutableList()
        
        // 현재 패 평가 결과 전달
        val pokerHand = pokerDeck.evaluateHand() // PokerDeck.evaluateHand()를 통해 조커 처리
        listener.applyPokerHandEffect(pokerHand)
        
        // 패널 초기 상태로 복귀
        resetPanel()
    }
    
    // 최적의 5장 카드 조합 찾기
    private fun findBestFiveCards(allCards: List<Card>): List<Card> {
        // 모든 가능한 5장 조합 생성
        val cardCombinations = generateCombinations(allCards, 5)
        
        // 각 조합에 대한 족보 평가 결과와 함께 저장
        val rankedCombinations = cardCombinations.map { combo ->
            // 조커 카드가 있는 경우 처리
            val tempDeck = PokerDeck()
            tempDeck.playerHand = combo.toMutableList()
            val handRank = getHandRank(tempDeck.evaluateHand())
            Pair(combo, handRank)
        }
        
        // 가장 높은 족보의 조합 반환
        val bestCombo = rankedCombinations.maxByOrNull { it.second }?.first ?: allCards.take(5)
        return bestCombo
    }
    
    // 카드 조합 생성 - 재귀 함수 사용
    private fun <T> generateCombinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.isEmpty()) return emptyList()
        
        val head = items.first()
        val tail = items.drop(1)
        
        val withHead = generateCombinations(tail, k - 1).map { listOf(head) + it }
        val withoutHead = generateCombinations(tail, k)
        
        return withHead + withoutHead
    }
    
    // 족보 순위 반환
    private fun getHandRank(hand: PokerHand): Int {
        return when (hand.handName) {
            "로얄 플러시" -> 10
            "스트레이트 플러시" -> 9
            "포카드" -> 8
            "풀 하우스" -> 7
            "플러시" -> 6
            "스트레이트" -> 5
            "트리플" -> 4
            "투 페어" -> 3
            "원 페어" -> 2
            else -> 1 // 하이 카드
        }
    }
    
    // 조커 카드 선택 다이얼로그 표시
    private fun showJokerSelectionDialog(card: Card, cardIndex: Int) {
        // 커스텀 다이얼로그 생성
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_joker_number_picker)
        dialog.setCancelable(true)
        
        // 다이얼로그 창 배경을 투명하게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 다이얼로그 제목 설정
        val titleTextView = dialog.findViewById<TextView>(R.id.tvTitle)
        titleTextView.text = "조커 카드 변환"
        
        // 무늬 선택기 설정
        val suitPicker = dialog.findViewById<NumberPicker>(R.id.suitPicker)
        val suits = arrayOf(CardSuit.HEART, CardSuit.DIAMOND, CardSuit.CLUB, CardSuit.SPADE)
        val suitSymbols = arrayOf("♥", "♦", "♣", "♠")
        
        suitPicker.minValue = 0
        suitPicker.maxValue = suits.size - 1
        suitPicker.displayedValues = suitSymbols
        
        // 숫자 선택기 설정
        val rankPicker = dialog.findViewById<NumberPicker>(R.id.rankPicker)
        val rankValues = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val ranks = arrayOf(
            CardRank.ACE, CardRank.TWO, CardRank.THREE, CardRank.FOUR, CardRank.FIVE,
            CardRank.SIX, CardRank.SEVEN, CardRank.EIGHT, CardRank.NINE, CardRank.TEN,
            CardRank.JACK, CardRank.QUEEN, CardRank.KING
        )
        
        rankPicker.minValue = 0
        rankPicker.maxValue = ranks.size - 1
        rankPicker.displayedValues = rankValues
        
        // 확인 버튼 설정
        val confirmButton = dialog.findViewById<Button>(R.id.btnConfirm)
        confirmButton.setOnClickListener {
            // 선택된 카드로 조커 카드 교체
            val selectedSuit = suits[suitPicker.value]
            val selectedRank = ranks[rankPicker.value]
            
            replaceJokerCard(card, selectedSuit, selectedRank, cardIndex)
            
            // 토스트 메시지 표시
            Toast.makeText(
                context,
                "조커가 ${selectedSuit.getName()} ${selectedRank.getName()}(으)로 변환되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
            
            dialog.dismiss()
        }
        
        // 취소 버튼 설정
        val cancelButton = dialog.findViewById<Button>(R.id.btnCancel)
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // 다이얼로그 표시
        dialog.show()
    }
    
    // 조커 카드 변환 처리
    private fun replaceJokerCard(originalCard: Card, newSuit: CardSuit, newRank: CardRank, cardIndex: Int) {
        // 새 카드 생성 (isJoker 속성 유지)
        val newCard = Card(
            suit = newSuit,
            rank = newRank,
            isSelected = false,
            isJoker = true  // 여전히 조커지만 보이는 모양과 숫자만 변경
        )
        
        // 카드 교체
        if (cardIndex in cards.indices) {
            cards[cardIndex] = newCard
            
            // 선택된 카드였다면 선택 상태 유지
            if (cardIndex in selectedCardIndexes) {
                // 선택 상태 유지
            } else {
                selectedCardIndexes.remove(cardIndex)
            }
        }
        
        // UI 업데이트
        // 이 메서드는 다음 PR에서 구현됩니다
    }
} 