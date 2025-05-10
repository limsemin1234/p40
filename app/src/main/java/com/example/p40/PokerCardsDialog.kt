package com.example.p40

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.PokerHand
import com.example.p40.game.PokerHandEvaluator
import com.example.p40.game.HighCard
import kotlin.random.Random

/**
 * 포커 카드 다이얼로그 클래스
 * 자원을 소모하여 포커 카드를 뽑고 선택할 수 있는 다이얼로그
 */
class PokerCardsDialog(
    context: Context,
    private val waveNumber: Int,
    private val onPokerHandSelected: (PokerHand) -> Unit
) : Dialog(context) {

    private val cards = mutableListOf<Card>()
    private var replacesLeft = 2 // 교체 가능한 횟수
    private val selectedCardIndexes = mutableSetOf<Int>() // 선택된 카드의 인덱스
    
    private lateinit var cardViews: List<CardView>
    private lateinit var replaceButton: Button
    private lateinit var confirmButton: Button
    private lateinit var replaceCountText: TextView
    private lateinit var pokerHandText: TextView
    private lateinit var pokerGuideButton: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_poker_cards)
        
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        initializeViews()
        dealCards()
        updateUI()
    }
    
    private fun initializeViews() {
        cardViews = listOf(
            findViewById(R.id.cardView1),
            findViewById(R.id.cardView2),
            findViewById(R.id.cardView3),
            findViewById(R.id.cardView4),
            findViewById(R.id.cardView5)
        )
        
        replaceButton = findViewById(R.id.btnReplaceCards)
        confirmButton = findViewById(R.id.btnConfirmHand)
        replaceCountText = findViewById(R.id.tvReplaceCount)
        pokerHandText = findViewById(R.id.tvCurrentHand)
        pokerGuideButton = findViewById(R.id.btnPokerGuide)
        
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
        
        // 확인 버튼 이벤트
        confirmButton.setOnClickListener {
            confirmSelection()
        }
        
        // 족보 가이드 버튼 이벤트
        pokerGuideButton.setOnClickListener {
            showPokerGuide()
        }
    }
    
    private fun dealCards() {
        // 웨이브 번호에 따라 더 좋은 카드가 나올 확률 증가
        // 기본 확률 0.15에서 웨이브 번호에 따라 증가
        val goodHandProbability = minOf(0.15f + (waveNumber * 0.03f), 0.5f)
        
        // 좋은 패가 나올 확률 계산
        if (Random.nextFloat() < goodHandProbability) {
            // 좋은 패 생성 (스트레이트 이상)
            generateGoodHand()
        } else {
            // 일반 랜덤 패 생성
            generateRandomHand()
        }
    }
    
    private fun generateRandomHand() {
        cards.clear()
        
        // 랜덤 카드 5장 생성
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        // 중복 없는 카드 5장 생성
        val usedCards = mutableSetOf<Pair<CardSuit, CardRank>>()
        
        while (cards.size < 5) {
            val suit = suits.random()
            val rank = ranks.random()
            val cardPair = Pair(suit, rank)
            
            if (cardPair !in usedCards) {
                usedCards.add(cardPair)
                cards.add(Card(suit, rank))
            }
        }
    }
    
    private fun generateGoodHand() {
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
        
        when (handType) {
            "royal_flush" -> {
                // 로얄 플러시 (스페이드 10, J, Q, K, A)
                val suit = CardSuit.SPADE
                cards.add(Card(suit, CardRank.TEN))
                cards.add(Card(suit, CardRank.JACK))
                cards.add(Card(suit, CardRank.QUEEN))
                cards.add(Card(suit, CardRank.KING))
                cards.add(Card(suit, CardRank.ACE))
            }
            "straight_flush" -> {
                // 스트레이트 플러시 (같은 무늬의 연속된 숫자)
                val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                
                for (i in 0 until 5) {
                    val rankValue = startRank + i
                    val rank = CardRank.values().find { it.value == rankValue }
                        ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                        ?: CardRank.ACE
                    
                    cards.add(Card(suit, rank))
                }
            }
            "four_of_a_kind" -> {
                // 포카드 (같은 숫자 4장)
                val rank = CardRank.values().filter { it != CardRank.JOKER }.random()
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                
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
            "full_house" -> {
                // 풀하우스 (트리플 + 원페어)
                val tripleRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                var pairRank: CardRank
                do {
                    pairRank = CardRank.values().filter { it != CardRank.JOKER }.random()
                } while (pairRank == tripleRank)
                
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }.shuffled()
                
                // 같은 숫자 3장
                for (i in 0 until 3) {
                    cards.add(Card(suits[i], tripleRank))
                }
                
                // 다른 같은 숫자 2장
                for (i in 0 until 2) {
                    cards.add(Card(suits[i], pairRank))
                }
            }
            "flush" -> {
                // 플러시 (같은 무늬 5장)
                val suit = CardSuit.values().filter { it != CardSuit.JOKER }.random()
                val ranks = CardRank.values().filter { it != CardRank.JOKER }.shuffled().take(5)
                
                for (rank in ranks) {
                    cards.add(Card(suit, rank))
                }
            }
            else -> { // 스트레이트
                // 스트레이트 (연속된 숫자 5장)
                val startRank = Random.nextInt(1, 10) // 1(A)부터 9까지의 시작 숫자
                val suits = CardSuit.values().filter { it != CardSuit.JOKER }
                
                for (i in 0 until 5) {
                    val rankValue = startRank + i
                    val rank = CardRank.values().find { it.value == rankValue }
                        ?: CardRank.values().find { it.value == (rankValue % 13) + 1 }
                        ?: CardRank.ACE
                    
                    cards.add(Card(suits.random(), rank))
                }
            }
        }
        
        // 카드 순서 섞기
        cards.shuffle()
    }
    
    private fun updateUI() {
        // 카드 이미지 업데이트
        cards.forEachIndexed { index, card ->
            val cardView = cardViews[index]
            
            // 카드 정보 표시
            val suitTextView = cardView.findViewById<TextView>(
                when (index) {
                    0 -> R.id.tvCardSuit1
                    1 -> R.id.tvCardSuit2
                    2 -> R.id.tvCardSuit3
                    3 -> R.id.tvCardSuit4
                    else -> R.id.tvCardSuit5
                }
            )
            
            val rankTextView = cardView.findViewById<TextView>(
                when (index) {
                    0 -> R.id.tvCardRank1
                    1 -> R.id.tvCardRank2
                    2 -> R.id.tvCardRank3
                    3 -> R.id.tvCardRank4
                    else -> R.id.tvCardRank5
                }
            )
            
            // 카드 무늬와 숫자 설정
            suitTextView.text = card.suit.getSymbol()
            suitTextView.setTextColor(card.suit.getColor())
            
            rankTextView.text = card.rank.getName()
            rankTextView.setTextColor(card.suit.getColor())
            
            // 선택 상태 표시
            cardView.setCardBackgroundColor(
                if (index in selectedCardIndexes) Color.YELLOW else Color.WHITE
            )
            
            // 카드 선택 가능 여부 설정
            cardView.isEnabled = replacesLeft > 0
        }
        
        // 교체 버튼 활성화/비활성화
        replaceButton.isEnabled = replacesLeft > 0 && selectedCardIndexes.isNotEmpty()
        
        // 교체 횟수 텍스트 업데이트
        replaceCountText.text = "교체 가능 횟수: $replacesLeft"
        
        // 현재 패 평가 및 표시
        val currentHand = PokerHandEvaluator.evaluate(cards)
        
        // UI 업데이트
        if (currentHand is HighCard) {
            // 하이카드(족보 없음)일 경우
            pokerHandText.text = "현재 족보: 족보 없음"
            findViewById<TextView>(R.id.tvHandDescription).text = "효과: 없음"
        } else {
            // 족보가 있는 경우
            pokerHandText.text = "현재 족보: ${currentHand.handName}"
            findViewById<TextView>(R.id.tvHandDescription).text = "효과: ${currentHand.getDescription()}"
        }
    }
    
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
    
    private fun replaceSelectedCards() {
        if (selectedCardIndexes.isEmpty() || replacesLeft <= 0) return
        
        // 선택된 카드 교체
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        // 현재 사용 중인 카드 확인 (중복 방지)
        val usedCards = cards
            .filterIndexed { index, _ -> index !in selectedCardIndexes }
            .map { Pair(it.suit, it.rank) }
            .toMutableSet()
        
        // 선택된 카드 교체
        for (index in selectedCardIndexes) {
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
        
        // 교체 횟수 감소
        replacesLeft--
        
        // 선택 초기화
        selectedCardIndexes.clear()
        
        // UI 업데이트
        updateUI()
    }
    
    private fun confirmSelection() {
        // 현재 패 평가 결과 전달
        val pokerHand = PokerHandEvaluator.evaluate(cards)
        onPokerHandSelected(pokerHand)
        dismiss()
    }
    
    // 족보 가이드 다이얼로그 표시
    private fun showPokerGuide() {
        val pokerGuideDialog = PokerGuideDialog(context)
        pokerGuideDialog.show()
    }
} 