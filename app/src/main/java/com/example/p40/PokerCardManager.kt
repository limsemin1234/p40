package com.example.p40

import android.content.Context
import android.view.View

import com.example.p40.PokerGuideDialog

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
    private val cardSelectionManager = CardSelectionManager.instance
    
    // 기본 카드 수 및 최대 카드 수 설정
    private val baseCardCount = 5 // 기본 5장
    private val maxExtraCards = 2 // 최대 2장 추가 가능
    
    // 현재 사용 중인 카드 수 (기본 5장, 최대 7장까지 확장 가능)
    private var purchasedExtraCards = 0 // 구매한 추가 카드 수
    private val activeCardCount: Int
        get() = baseCardCount + purchasedExtraCards
    
    // 추가 카드 구매 비용 - 추가 카드 수에 따라 다르게 적용
    private fun getExtraCardCost(): Int {
        return when (purchasedExtraCards) {
            0 -> GameConfig.FIRST_EXTRA_CARD_COST // 첫 번째 추가 카드 비용
            1 -> GameConfig.SECOND_EXTRA_CARD_COST // 두 번째 추가 카드 비용
            else -> 0 // 이미 최대 카드 수에 도달
        }
    }
    
    private val cards = mutableListOf<Card>()
    private var replacesLeft = GameConfig.POKER_CARD_REPLACE_COUNT // 교체 가능한 횟수
    private val selectedCardIndexes = mutableSetOf<Int>() // 선택된 카드의 인덱스
    
    // 현재 카드 게임이 진행 중인지 여부
    private var isGameActive = false
    
    // 외부 포커 핸드 콜백 (PokerCardsDialog 대체용)
    private var externalPokerHandCallback: ((PokerHand) -> Unit)? = null

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
        // CardSelectionManager에 컨텍스트 설정
        cardSelectionManager.setContext(context)
        
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
                    cardSelectionManager.showJokerSelectionDialog(context, cards[index], index) { newCard, cardIndex ->
                        cards[cardIndex] = newCard
                        updateUI()
                    }
                    true
                } else {
                    false
                }
            }
        }
        
        // 족보 가이드 버튼 이벤트 설정
        rootView.findViewById<android.widget.ImageButton>(R.id.btnPokerGuide)?.setOnClickListener {
            showPokerGuide()
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
            // 자원 소모 비용 설정 (GameConfig에서 가져옴)
            val cardDrawCost = GameConfig.POKER_CARD_DRAW_COST
            
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
                MessageManager.getInstance().showError("자원이 부족합니다! (필요: $cardDrawCost)")
            }
        }
    }

    // 추가 카드 구매
    private fun purchaseExtraCard() {
        // 이미 최대로 추가 구매한 경우
        if (purchasedExtraCards >= maxExtraCards) {
            MessageManager.getInstance().showWarning("이미 최대 카드 수에 도달했습니다")
            return
        }
        
        // 게임 진행 중인 경우 추가 구매 불가
        if (isGameActive) {
            MessageManager.getInstance().showWarning("현재 게임이 진행 중입니다. 다음 게임에서 추가 카드를 사용할 수 있습니다.")
            return
        }
        
        // 자원 확인
        val currentResource = listener.getResource()
        if (currentResource >= getExtraCardCost()) {
            // 자원 차감
            if (listener.useResource(getExtraCardCost())) {
                // 추가 카드 수 증가
                purchasedExtraCards++
                
                // 버튼 상태 업데이트
                updateAddCardButtonState()
                
                // 카드 뽑기 버튼 텍스트 업데이트
                updateDrawCardButtonText()
                
                // 자원 정보 업데이트
                listener.updateGameInfoUI()
                
                // 토스트 메시지 표시
                MessageManager.getInstance().showSuccess("다음 카드 게임에서 ${baseCardCount + purchasedExtraCards}장의 카드가 제공됩니다")
            }
        } else {
            // 자원 부족 메시지
            MessageManager.getInstance().showError("자원이 부족합니다! (필요: ${getExtraCardCost()})")
        }
    }
    
    // 외부에서 호출할 카드 게임 시작 메서드
    fun startPokerCards(waveNumber: Int) {
        // 상태 초기화
        cards.clear()
        selectedCardIndexes.clear()
        replacesLeft = GameConfig.POKER_CARD_REPLACE_COUNT
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
        cardUIManager.updateAddCardButtonState(purchasedExtraCards, getExtraCardCost())
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
    
    /**
     * 미완료 작업 취소 (게임 종료 시 호출)
     */
    fun cancelPendingOperations() {
        // 게임 활성 상태 해제
        isGameActive = false
        
        // 카드 정리
        cards.clear()
        selectedCardIndexes.clear()
        
        // UI 업데이트
        cardUIManager.hideAllCardViewsAndButtons()
    }
    
    /**
     * 메모리 누수 방지를 위한 참조 정리
     * Fragment의 onDestroyView에서 호출해야 함
     */
    fun clearReferences() {
        // 카드 UI 리스너 제거
        cardUIManager.cardViews.forEachIndexed { index, cardView ->
            cardView.setOnClickListener(null)
            cardView.setOnLongClickListener(null)
        }
        
        // 버튼 리스너 제거
        try {
            // 족보 가이드 버튼
            rootView.findViewById<android.widget.ImageButton>(R.id.btnPokerGuide)?.setOnClickListener(null)
            
            // 기능 버튼들
            cardUIManager.getReplaceButton().setOnClickListener(null)
            cardUIManager.getReplaceAllButton().setOnClickListener(null)
            cardUIManager.getConfirmButton().setOnClickListener(null)
            cardUIManager.getAddCardButton().setOnClickListener(null)
            cardUIManager.getDrawPokerCardsButton().setOnClickListener(null)
        } catch (e: Exception) {
            // 예외 발생 시 무시 (이미 뷰가 분리되었을 수 있음)
            e.printStackTrace()
        }
        
        // CardSelectionManager 참조 정리
        cardSelectionManager.clear()
    }
    
    // 카드 선택 토글
    private fun toggleCardSelection(index: Int) {
        // 6장 이상인 경우 교체 횟수와 관계없이 선택 가능
        if (cards.size > 5) {
            if (index in selectedCardIndexes) {
                selectedCardIndexes.remove(index)
            } else {
                // 6장 이상이고 이미 5장 선택한 경우, 추가 선택 방지 (선택 해제는 가능)
                if (selectedCardIndexes.size >= 5) {
                    MessageManager.getInstance().showInfo("최대 5장까지만 선택할 수 있습니다.")
                    return
                }
                selectedCardIndexes.add(index)
            }
            updateUI()
            return
        }
        
        // 5장 이하인 경우는 교체 모드일 때만 선택 가능
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
            MessageManager.getInstance().showWarning("교체 횟수를 모두 사용했습니다.")
            return
        }
        
        // 전체 교체 요청
        val result = cardSelectionManager.replaceAllNonJokerCards(cards)
        
        if (!result.first) {
            // 교체할 카드가 없는 경우
            MessageManager.getInstance().showInfo("교체할 일반 카드가 없습니다.")
            return
        }
        
        // 교체 횟수 감소
        replacesLeft--
        
        // 선택 초기화
        selectedCardIndexes.clear()
        
        // UI 업데이트
        updateUI()
        
        // 토스트 메시지 표시
        MessageManager.getInstance().showInfo("${result.second}장의 카드가 교체되었습니다.")
    }
    
    // 카드 선택 관리
    private fun handleCardSelection(cardIndex: Int) {
        // 이미 선택된 카드인지 확인
        if (selectedCardIndexes.contains(cardIndex)) {
            // 선택 해제
            selectedCardIndexes.remove(cardIndex)
            cards[cardIndex].isSelected = false
        } else {
            // 선택
            selectedCardIndexes.add(cardIndex)
            cards[cardIndex].isSelected = true
        }
        
        // UI 업데이트
        updateUI()
        
        // 카드 선택 정보를 CardSelectionManager에 전달
        val selectedCards = selectedCardIndexes.map { cards[it] }
        CardSelectionManager.instance.setSelectedCards(selectedCards)
    }
    
    // 카드 선택 확정
    private fun confirmSelection() {
        // 변수 선언
        val pokerDeck = PokerDeck()
        var pokerHand: PokerHand
        
        // 6장 이상인 경우 5장 선택 여부 확인
        if (cards.size > 5) {
            // 5장 선택 검증
            if (!cardUIManager.validateCardSelection(cards, selectedCardIndexes)) {
                return // 검증 실패시 함수 종료
            }
            
            // 선택된 5장의 카드를 사용
            val selectedFiveCards = selectedCardIndexes.map { cards[it] }
            
            // 선택된 카드를 CardSelectionManager 싱글톤에 저장
            CardSelectionManager.instance.setSelectedCards(selectedFiveCards)
            
            // 현재 패 평가
            pokerDeck.playerHand = selectedFiveCards.toMutableList()
            
            // 현재 패 평가 결과 전달
            pokerHand = pokerDeck.evaluateHand()
        } else {
            // 5장 이하인 경우 모든 카드 사용
            CardSelectionManager.instance.setSelectedCards(cards)
            
            // 현재 패 평가
            pokerDeck.playerHand = cards.toMutableList()
            
            // 현재 패 평가 결과 전달
            pokerHand = pokerDeck.evaluateHand()
        }
        
        // 포커 패 효과 적용
        listener.applyPokerHandEffect(pokerHand)
        
        // 외부 콜백 호출 (PokerCardsDialog 대체)
        externalPokerHandCallback?.invoke(pokerHand)
        
        // 성공 메시지 표시
        if (pokerHand is HighCard) {
            // 하이카드(족보 없음)일 경우
            MessageManager.getInstance().showInfo("족보가 없습니다.")
        } else {
            // 다른 족보일 경우 (족보 이름과 효과를 함께 표시)
            val handName = getPokerHandKoreanName(pokerHand)
            val effectDescription = getEffectDescription(pokerHand)
            MessageManager.getInstance().showSuccess("$handName($effectDescription) 버프가 적용되었습니다!")
        }
        
        // 패널 초기 상태로 복귀
        resetPanel()
    }
    
    // 포커 패 한글 이름 반환
    private fun getPokerHandKoreanName(hand: PokerHand): String {
        return when (hand) {
            is HighCard -> "족보 없음"
            is OnePair -> "원페어"
            is TwoPair -> "투페어"
            is ThreeOfAKind -> "트리플"
            is Straight -> "스트레이트"
            is Flush -> "플러시"
            is FullHouse -> "풀하우스"
            is FourOfAKind -> "포카드"
            is StraightFlush -> "스트레이트 플러시"
            is RoyalFlush -> "로열 플러시"
            else -> "알 수 없는 패"
        }
    }
    
    // 포커 패 효과 설명 반환
    private fun getEffectDescription(hand: PokerHand): String {
        return when (hand) {
            is OnePair -> "데미지 ${(GameConfig.ONE_PAIR_DAMAGE_INCREASE * 100).toInt()}%"
            is TwoPair -> "데미지 ${(GameConfig.TWO_PAIR_DAMAGE_INCREASE * 100).toInt()}%"
            is ThreeOfAKind -> "데미지 ${(GameConfig.THREE_OF_A_KIND_DAMAGE_INCREASE * 100).toInt()}%"
            is Straight -> "데미지 ${(GameConfig.STRAIGHT_DAMAGE_INCREASE * 100).toInt()}%"
            is Flush -> "문양 스킬 활성화"
            is FullHouse -> "데미지 ${(GameConfig.FULL_HOUSE_DAMAGE_INCREASE * 100).toInt()}%"
            is FourOfAKind -> "데미지 ${(GameConfig.FOUR_OF_A_KIND_DAMAGE_INCREASE * 100).toInt()}%"
            is StraightFlush -> "데미지 ${(GameConfig.STRAIGHT_FLUSH_DAMAGE_INCREASE * 100).toInt()}%"
            is RoyalFlush -> "데미지 ${(GameConfig.ROYAL_FLUSH_DAMAGE_INCREASE * 100).toInt()}%"
            else -> ""
        }
    }

    // 족보 가이드 다이얼로그 표시 메서드
    private fun showPokerGuide() {
        val guideDialog = PokerGuideDialog(context)
        guideDialog.show()
    }

    /**
     * 외부에서 포커 핸드 콜백을 설정하기 위한 메서드
     * GameUIController에서 호출
     */
    fun setPokerHandCallback(callback: (PokerHand) -> Unit) {
        this.externalPokerHandCallback = callback
    }
} 