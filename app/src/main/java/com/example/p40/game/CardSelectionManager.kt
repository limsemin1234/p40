package com.example.p40.game

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import com.example.p40.DeckBuilderFragment
import com.example.p40.R

/**
 * 카드 선택 및 교체 관리 클래스
 * PokerCardManager에서 카드 선택/교체 관련 로직만 분리함
 */
class CardSelectionManager(
    private val context: Context
) {
    private val cardGenManager = CardGenerationManager(context)
    
    /**
     * 선택된 카드 교체
     */
    fun replaceSelectedCards(
        cards: MutableList<Card>,
        selectedCardIndexes: Set<Int>
    ): Boolean {
        if (selectedCardIndexes.isEmpty()) return false
        
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
                    cards[index] = cardGenManager.createRandomCard(usedCards)
                }
            } else {
                // 저장된 덱이 없는 경우, 기존 방식으로 랜덤 카드 생성
                cards[index] = cardGenManager.createRandomCard(usedCards)
            }
        }
        
        return true
    }
    
    /**
     * 조커가 아닌 모든 카드 교체
     */
    fun replaceAllNonJokerCards(cards: MutableList<Card>): Pair<Boolean, Int> {
        // 조커가 아닌 카드의 인덱스 찾기
        val nonJokerCardIndices = cards.indices.filter { index ->
            !CardUtils.isJokerCard(cards[index])
        }.toMutableSet()
        
        // 선택된 카드가 없는 경우 (조커 카드만 있는 경우) 실패 반환
        if (nonJokerCardIndices.isEmpty()) {
            return Pair(false, 0)
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
                    cards[index] = cardGenManager.createRandomCard(usedCards)
                }
            }
        } else {
            // 저장된 덱이 없는 경우, 기존 방식으로 모든 카드 교체
            for (index in nonJokerCardIndices) {
                cards[index] = cardGenManager.createRandomCard(usedCards)
            }
        }
        
        return Pair(true, nonJokerCardIndices.size)
    }
    
    /**
     * 최적의 5장 카드 조합 찾기
     */
    fun findBestFiveCards(allCards: List<Card>): List<Card> {
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
    
    /**
     * 카드 조합 생성 - 재귀 함수 사용
     */
    private fun <T> generateCombinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.isEmpty()) return emptyList()
        
        val head = items.first()
        val tail = items.drop(1)
        
        val withHead = generateCombinations(tail, k - 1).map { listOf(head) + it }
        val withoutHead = generateCombinations(tail, k)
        
        return withHead + withoutHead
    }
    
    /**
     * 족보 순위 반환
     */
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
    
    /**
     * 조커 카드 선택 다이얼로그 표시
     */
    fun showJokerSelectionDialog(
        card: Card, 
        cardIndex: Int, 
        onJokerSelected: (Card, Int) -> Unit
    ) {
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
            
            val newCard = replaceJokerCard(card, selectedSuit, selectedRank)
            onJokerSelected(newCard, cardIndex)
            
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
    
    /**
     * 조커 카드 변환 처리
     */
    private fun replaceJokerCard(originalCard: Card, newSuit: CardSuit, newRank: CardRank): Card {
        // 새 카드 생성 (isJoker 속성 유지)
        return Card(
            suit = newSuit,
            rank = newRank,
            isSelected = false,
            isJoker = true  // 여전히 조커지만 보이는 모양과 숫자만 변경
        )
    }
} 