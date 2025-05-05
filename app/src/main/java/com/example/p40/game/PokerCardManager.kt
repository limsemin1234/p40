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
 * - 리팩토링: 여러 매니저 클래스로 로직 분리
 */
class PokerCardManager(
    private val context: Context,
    private val rootView: View,
    private val listener: PokerCardListener
) {
    // 분리된 매니저 클래스들
    private val cardGenManager = CardGenerationManager(context)
    private val cardUIManager = CardUIManager(context, rootView)
    private val cardSelectionManager = CardSelectionManager(context)
    
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
        cardUIManager.cardViews.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                toggleCardSelection(index)
            }
            
            // 조커 카드 롱클릭 이벤트 설정
            cardView.setOnLongClickListener {
                if (index < cards.size && CardUtils.isJokerCard(cards[index])) {
                    cardSelectionManager.showJokerSelectionDialog(cards[index], index) { newCard, cardIndex ->
                        cards[cardIndex] = newCard
                        updateUI()
                    }
                    true
                } else {
                    false
                }
            }
        }
        
        // 교체 버튼 이벤트
        cardUIManager.getReplaceButton().setOnClickListener {
            replaceSelectedCards()
        }
        
        // 전체교체 버튼 이벤트
        cardUIManager.getReplaceAllButton().setOnClickListener {
            replaceAllNonJokerCards()
        }
        
        // 확인 버튼 이벤트
        cardUIManager.getConfirmButton().setOnClickListener {
            confirmSelection()
        }
        
        // 카드 추가 버튼 이벤트
        cardUIManager.getAddCardButton().setOnClickListener {
            purchaseExtraCard()
        }
        
        // 포커 카드 뽑기 버튼 이벤트
        cardUIManager.getDrawPokerCardsButton().setOnClickListener {
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
                cardUIManager.showToast("자원이 부족합니다! (필요: $cardDrawCost)")
            }
        }
    }

    // 추가 카드 구매
    private fun purchaseExtraCard() {
        // 이미 최대로 추가 구매한 경우
        if (purchasedExtraCards >= maxExtraCards) {
            cardUIManager.showToast("이미 최대 카드 수에 도달했습니다")
            return
        }
        
        // 게임 진행 중인 경우 추가 구매 불가
        if (isGameActive) {
            cardUIManager.showToast("현재 게임이 진행 중입니다. 다음 게임에서 추가 카드를 사용할 수 있습니다.")
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
                cardUIManager.showToast("다음 카드 게임에서 ${baseCardCount + purchasedExtraCards}장의 카드가 제공됩니다")
            }
        } else {
            // 자원 부족 메시지
            cardUIManager.showToast("자원이 부족합니다! (필요: $extraCardCost)")
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
        cardUIManager.initGameUI()
        
        // 카드 생성 (추가 구매한 카드 수 반영)
        cards.addAll(cardGenManager.dealCards(waveNumber, activeCardCount))
        
        // UI 업데이트
        updateUI()
    }
    
    // UI 관련 메서드들
    private fun updateAddCardButtonState() {
        cardUIManager.updateAddCardButtonState(purchasedExtraCards, extraCardCost)
    }
    
    private fun updateDrawCardButtonText() {
        cardUIManager.updateDrawCardButtonText(purchasedExtraCards)
    }
    
    // 전체 UI 업데이트
    private fun updateUI() {
        // 카드가 5장을 초과하는 경우 최적의 5장 조합 찾기
        val bestFiveCards = if (cards.size > 5) {
            cardSelectionManager.findBestFiveCards(cards)
        } else {
            null
        }
        
        cardUIManager.updateHandUI(
            cards = cards,
            activeCardCount = activeCardCount,
            selectedCardIndexes = selectedCardIndexes,
            replacesLeft = replacesLeft,
            bestFiveCards = bestFiveCards
        )
    }
    
    // 패널 초기 상태로 복귀
    private fun resetPanel() {
        cardUIManager.resetUI()
        isGameActive = false
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
        
        // 카드 교체 요청
        if (cardSelectionManager.replaceSelectedCards(cards, selectedCardIndexes)) {
            // 교체 횟수 감소
            replacesLeft--
            
            // 선택 초기화
            selectedCardIndexes.clear()
            
            // UI 업데이트
            updateUI()
        }
    }
    
    // 전체 카드 교체 (조커 카드 제외)
    private fun replaceAllNonJokerCards() {
        if (replacesLeft <= 0) {
            cardUIManager.showToast("교체 횟수를 모두 사용했습니다.")
            return
        }
        
        // 전체 교체 요청
        val result = cardSelectionManager.replaceAllNonJokerCards(cards)
        
        if (!result.first) {
            // 교체할 카드가 없는 경우
            cardUIManager.showToast("교체할 일반 카드가 없습니다.")
            return
        }
        
        // 교체 횟수 감소
        replacesLeft--
        
        // 선택 초기화
        selectedCardIndexes.clear()
        
        // UI 업데이트
        updateUI()
        
        // 토스트 메시지 표시
        cardUIManager.showToast("${result.second}장의 카드가 교체되었습니다.")
    }
    
    // 카드 선택 확정
    private fun confirmSelection() {
        // 카드가 5장 이상인 경우 최적의 5장 조합 찾기
        val bestFiveCards = if (cards.size > 5) {
            cardSelectionManager.findBestFiveCards(cards)
        } else {
            cards
        }
        
        // 현재 패 평가
        val pokerDeck = PokerDeck()
        pokerDeck.playerHand = bestFiveCards.toMutableList()
        
        // 현재 패 평가 결과 전달
        val pokerHand = pokerDeck.evaluateHand()
        listener.applyPokerHandEffect(pokerHand)
        
        // 패널 초기 상태로 복귀
        resetPanel()
    }
} 