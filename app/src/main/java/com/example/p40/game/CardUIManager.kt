package com.example.p40.game

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.p40.R

/**
 * 포커 카드 UI 관리 클래스
 * PokerCardManager에서 UI 관련 로직만 분리함
 */
class CardUIManager(
    private val context: Context,
    private val rootView: View
) {
    // UI 관련 요소들
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
    
    val cardViews: List<CardView> = listOf(
        rootView.findViewById(R.id.cardView1),
        rootView.findViewById(R.id.cardView2),
        rootView.findViewById(R.id.cardView3),
        rootView.findViewById(R.id.cardView4),
        rootView.findViewById(R.id.cardView5),
        rootView.findViewById(R.id.cardView6),
        rootView.findViewById(R.id.cardView7)
    )
    
    // 기본 설정
    private val baseCardCount = 5 // 기본 5장
    private val maxExtraCards = 2 // 최대 2장 추가 가능
    
    /**
     * 추가 카드 버튼 상태 업데이트
     */
    fun updateAddCardButtonState(purchasedExtraCards: Int, extraCardCost: Int) {
        if (purchasedExtraCards >= maxExtraCards) {
            btnAddCard.isEnabled = false
            btnAddCard.text = "최대 카드 수\n도달"
        } else {
            btnAddCard.isEnabled = true
            btnAddCard.text = "카드 추가 +1\n(💰 $extraCardCost 자원)"
        }
    }
    
    /**
     * 카드 뽑기 버튼 텍스트 업데이트
     */
    fun updateDrawCardButtonText(purchasedExtraCards: Int) {
        if (purchasedExtraCards > 0) {
            btnDrawPokerCards.text = "포커 카드 뽑기\n(${baseCardCount + purchasedExtraCards}장, 💰 50 자원)"
        } else {
            btnDrawPokerCards.text = "포커 카드 뽑기\n(💰 50 자원)"
        }
    }
    
    /**
     * 게임 시작 시 UI 초기화
     */
    fun initGameUI() {
        cardInfoLayout.visibility = View.VISIBLE
        cardButtonsLayout.visibility = View.GONE
    }
    
    /**
     * 게임 종료 시 UI 초기화
     */
    fun resetUI() {
        cardInfoLayout.visibility = View.GONE
        cardButtonsLayout.visibility = View.VISIBLE
    }
    
    /**
     * 전체 UI 업데이트
     */
    fun updateHandUI(
        cards: List<Card>, 
        activeCardCount: Int, 
        selectedCardIndexes: Set<Int>,
        replacesLeft: Int,
        bestFiveCards: List<Card>? = null
    ) {
        // 기본 카드 UI 업데이트
        updateBasicCardUI(cards, activeCardCount, selectedCardIndexes)
        
        // 카드가 5장을 초과하는 경우 최적의 5장 조합 찾기
        if (cards.size > 5 && bestFiveCards != null) {
            highlightBestCards(cards, bestFiveCards, selectedCardIndexes)
            
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
    
    /**
     * 기본 카드 UI 업데이트
     */
    private fun updateBasicCardUI(
        cards: List<Card>, 
        activeCardCount: Int, 
        selectedCardIndexes: Set<Int>
    ) {
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
                    cardView.isEnabled = true // 클릭은 항상 가능하도록 설정
                }
            } else {
                // 활성화되지 않은 카드는 숨김
                cardView.visibility = View.GONE
            }
        }
    }
    
    /**
     * 최적의 카드 강조 표시
     */
    private fun highlightBestCards(cards: List<Card>, bestFiveCards: List<Card>, selectedCardIndexes: Set<Int>) {
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
    
    /**
     * 족보에 따라 강조할 카드 찾기
     */
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
            "플러시", "스트레이트", "스트레이트 플러시", "로얄 플러시" -> {
                // 모든 카드 강조
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
    
    // 버튼 관련 getter
    fun getDrawPokerCardsButton() = btnDrawPokerCards
    fun getAddCardButton() = btnAddCard
    fun getReplaceButton() = replaceButton
    fun getReplaceAllButton() = replaceAllButton
    fun getConfirmButton() = confirmButton
    
    /**
     * 토스트 메시지 표시
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
} 