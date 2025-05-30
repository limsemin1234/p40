package com.example.p40

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

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
            
            // 추가 카드 순서에 따라 텍스트 구성
            val currentCardCount = baseCardCount + purchasedExtraCards
            val nextCardCount = currentCardCount + 1
            
            // HTML 서식 적용 (API 레벨에 따른 호환성 처리)
            val htmlText = "카드 ${nextCardCount}장으로 변경<br/><font color='#FFFF99'>💰 $extraCardCost 자원</font>"
            val buttonText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(htmlText)
            }
            
            btnAddCard.text = buttonText
        }
    }
    
    /**
     * 카드 뽑기 버튼 텍스트 업데이트
     */
    fun updateDrawCardButtonText(purchasedExtraCards: Int) {
        val totalCards = if (purchasedExtraCards > 0) {
            baseCardCount + purchasedExtraCards
        } else {
            5
        }
        
        // HTML 서식 적용 (API 레벨에 따른 호환성 처리)
        val htmlText = "포커 카드 뽑기 (${totalCards}장)<br/><font color='#FFFF99'>💰 ${GameConfig.POKER_CARD_DRAW_COST} 자원</font>"
        val buttonText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(htmlText)
        }
        
        btnDrawPokerCards.text = buttonText
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
            // 최적의 조합 카드 로그 출력 (디버깅용)
            android.util.Log.d("CardUIManager", "최적 조합 카드 목록: ${bestFiveCards.map { "${it.suit}:${it.rank}" }}")
            
            // 사용자에게 보여줄 카드 조합을 출력 (랭크별로 그룹화)
            val rankGroups = bestFiveCards.groupBy { it.rank }
            android.util.Log.d("CardUIManager", "최적 조합 랭크 그룹: ${rankGroups.map { "${it.key}: ${it.value.size}장" }}")
            
            // 최적의 조합으로 족보 업데이트
            val tempDeck = PokerDeck()
            tempDeck.playerHand = bestFiveCards.toMutableList()
            val pokerHand = tempDeck.evaluateHand()
            
            // 족보 평가 결과 출력
            android.util.Log.d("CardUIManager", "최종 족보 평가 결과: ${pokerHand.handName}")
            
            // 족보 텍스트 업데이트
            if (pokerHand is HighCard) {
                currentHandText.text = "현재 족보: 족보 없음"
                handDescriptionText.text = "효과: 없음"
            } else {
                currentHandText.text = "현재 족보: ${pokerHand.handName}"
                handDescriptionText.text = "효과: ${pokerHand.getDescription()}"
            }
        } else {
            // 5장 이하인 경우 일반 족보 평가
            val pokerDeck = PokerDeck()
            pokerDeck.playerHand = cards.toMutableList()
            val pokerHand = pokerDeck.evaluateHand()
            
            // 족보 평가 결과 출력
            android.util.Log.d("CardUIManager", "일반(5장) 족보 평가 결과: ${pokerHand.handName}")
            
            // 족보 텍스트 업데이트
            if (pokerHand is HighCard) {
                currentHandText.text = "현재 족보: 족보 없음"
                handDescriptionText.text = "효과: 없음"
            } else {
                currentHandText.text = "현재 족보: ${pokerHand.handName}"
                handDescriptionText.text = "효과: ${pokerHand.getDescription()}"
            }
        }
        
        // 선택된 카드 중 조커 카드가 있는지 확인
        val hasSelectedJoker = selectedCardIndexes.any { index ->
            index < cards.size && CardUtils.isJokerCard(cards[index])
        }
        
        // 교체 버튼 활성화/비활성화
        // 조커 카드가 선택되어 있으면 교체 횟수와 상관없이 활성화
        replaceButton.isEnabled = (replacesLeft > 0 && selectedCardIndexes.isNotEmpty()) || 
                                 (hasSelectedJoker && selectedCardIndexes.isNotEmpty())
        
        // 전체 교체 버튼은 교체 횟수에 따라 활성화/비활성화
        replaceAllButton.isEnabled = replacesLeft > 0
        
        // 6장 이상인 경우의 처리
        if (activeCardCount > 5) {
            // 확정 버튼은 항상 활성화 (선택된 5장 또는 자동 선택된 최적의 5장 사용)
            confirmButton.isEnabled = true
            
            // 교체 횟수 표시 (교체 가능 여부 함께 표시)
            if (replacesLeft > 0) {
                if (selectedCardIndexes.isEmpty()) {
                    replaceCountText.text = "교체 가능 횟수: $replacesLeft\n6장 이상 카드 중 최적의 5장으로 판정됩니다. 원하는 카드 5장을 직접 선택할 수도 있습니다."
                } else if (selectedCardIndexes.size != 5) {
                    replaceCountText.text = "교체 가능 횟수: $replacesLeft\n정확히 5장을 선택하거나, 선택하지 않고 확정하기를 눌러 최적의 5장을 사용하세요."
                } else {
                    replaceCountText.text = "교체 가능 횟수: $replacesLeft"
                }
            } else {
                if (selectedCardIndexes.isEmpty()) {
                    replaceCountText.text = "교체 불가능\n6장 이상 카드 중 최적의 5장으로 판정됩니다. 원하는 카드 5장을 직접 선택할 수도 있습니다."
                } else if (selectedCardIndexes.size != 5) {
                    replaceCountText.text = "교체 불가능\n정확히 5장을 선택하거나, 선택하지 않고 확정하기를 눌러 최적의 5장을 사용하세요."
                } else {
                    replaceCountText.text = "교체 불가능"
                }
            }
            
            if (selectedCardIndexes.size != 0 && selectedCardIndexes.size != 5) {
                // 선택된 카드가 0장도 아니고 5장도 아닌 경우 안내 메시지
                handDescriptionText.text = "5장의 카드를 선택하세요 (현재 ${selectedCardIndexes.size}장 선택됨)"
            }
        } else {
            // 원래 교체 횟수 표시
            replaceCountText.text = "교체 가능 횟수: $replacesLeft"
        }
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
                    
                    // 카드 선택 가능 여부 설정
                    // 모든 카드는 항상 클릭 가능하도록 설정 (선택/해제는 toggleCardSelection에서 관리)
                    cardView.isEnabled = true
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
        
        // 초록색 강조 표시 기능 삭제 - 모든 카드는 선택 여부에 따라 노란색 또는 흰색으로만 표시
    }
    
    /**
     * 족보에 따라 강조할 카드 찾기 (사용하지 않음)
     */
    private fun findCardsToHighlight(bestCards: List<Card>, handName: String): List<Card> {
        // 더 이상 초록색 강조가 없으므로 빈 리스트 반환
        return emptyList()
    }
    
    // 버튼 관련 getter
    fun getDrawPokerCardsButton() = btnDrawPokerCards
    fun getAddCardButton() = btnAddCard
    fun getReplaceButton() = replaceButton
    fun getReplaceAllButton() = replaceAllButton
    fun getConfirmButton() = confirmButton
    
    /**
     * 메시지 표시
     */
    fun showMessage(message: String) {
        // MessageManager를 사용하여 상단에 메시지 표시
        MessageManager.getInstance().showInfo(message)
    }
    
    /**
     * 확정 버튼 클릭 처리 - 5장 선택 확인
     * 새로 추가된 메서드
     */
    fun validateCardSelection(cards: List<Card>, selectedCardIndexes: Set<Int>): Boolean {
        // 카드가 5장 이하인 경우는 항상 유효
        if (cards.size <= 5) return true
        
        // 6장 이상일 경우 정확히 5장이 선택되었는지 또는 아예 선택이 없는지 확인
        if (selectedCardIndexes.size != 5 && selectedCardIndexes.size != 0) {
            showMessage("카드를 정확히 5장 선택하거나, 선택하지 않고 최적의 5장을 자동으로 사용하세요. (현재 ${selectedCardIndexes.size}장 선택됨)")
            return false
        }
        
        return true
    }
    
    /**
     * 모든 카드 뷰와 버튼을 숨기는 메서드
     * 게임이 종료되거나 미완료 작업 취소 시 호출됨
     */
    fun hideAllCardViewsAndButtons() {
        // 모든 카드 뷰 숨기기
        for (cardView in cardViews) {
            cardView.visibility = View.GONE
        }
        
        // 카드 정보 레이아웃 숨기기
        cardInfoLayout.visibility = View.GONE
        
        // 버튼 레이아웃 숨기기
        cardButtonsLayout.visibility = View.GONE
        
        // 텍스트 초기화
        currentHandText.text = "현재 족보: 없음"
        handDescriptionText.text = "효과: 없음"
        replaceCountText.text = "교체 가능 횟수: 0"
    }
} 