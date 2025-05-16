package com.example.p40

import android.content.Context
import com.example.p40.UserManager

/**
 * 게임 내 카드 관리를 담당하는 클래스
 * 플레이어의 덱, 카드 드로우, 교체 로직을 관리
 */
class GameCardManager(
    private val context: Context,
    private val userManager: UserManager
) {
    // 현재 플레이어가 가진 카드 목록
    private val playerCards = mutableListOf<Card>()
    
    // 카드 덱 관리 객체
    private val pokerDeck = PokerDeck()
    
    // 카드 평가기 
    private val handEvaluator = PokerHandEvaluator
    
    // 현재 핸드의 포커 족보
    private var currentPokerHand: PokerHand = HighCard()
    
    // 교체 가능 횟수
    private var replacementsLeft = 1
    
    // 선택된 카드 인덱스
    private val selectedCardIndices = mutableSetOf<Int>()
    
    /**
     * 초기화 및 카드 드로우
     */
    fun initialize() {
        // 덱 생성
        pokerDeck.initializeDeck()
        
        // 조커 추가 (사용자 프리미엄 여부 확인)
        if (userManager.isPremium()) {
            pokerDeck.addJoker()
        }
        
        // 카드 섞기
        pokerDeck.shuffle()
        
        // 초기 카드 5장 드로우
        drawInitialCards()
        
        // 교체 횟수 초기화
        resetReplacements()
    }
    
    /**
     * 초기 카드 5장 드로우
     */
    private fun drawInitialCards() {
        // 기존 카드 초기화
        playerCards.clear()
        selectedCardIndices.clear()
        
        // 새 카드 드로우
        val drawnCards = pokerDeck.drawCards(5)
        playerCards.addAll(drawnCards)
        
        // 현재 핸드 평가
        evaluateCurrentHand()
    }
    
    /**
     * 현재 핸드 평가
     */
    fun evaluateCurrentHand(): PokerHand {
        currentPokerHand = PokerHandEvaluator.evaluate(playerCards)
        return currentPokerHand
    }
    
    /**
     * 현재 포커 핸드 반환
     */
    fun getCurrentPokerHand(): PokerHand {
        return currentPokerHand
    }
    
    /**
     * 현재 포커 핸드 타입 반환
     */
    fun getCurrentPokerHandType(): PokerHandType {
        return when (currentPokerHand) {
            is HighCard -> PokerHandType.HIGH_CARD
            is OnePair -> PokerHandType.ONE_PAIR
            is TwoPair -> PokerHandType.TWO_PAIR
            is ThreeOfAKind -> PokerHandType.THREE_OF_A_KIND
            is Straight -> PokerHandType.STRAIGHT
            is Flush -> PokerHandType.FLUSH
            is FullHouse -> PokerHandType.FULL_HOUSE
            is FourOfAKind -> PokerHandType.FOUR_OF_A_KIND
            is StraightFlush -> PokerHandType.STRAIGHT_FLUSH
            is RoyalFlush -> PokerHandType.ROYAL_FLUSH
            else -> PokerHandType.HIGH_CARD
        }
    }
    
    /**
     * 현재 카드 리스트 반환
     */
    fun getPlayerCards(): List<Card> {
        return playerCards.toList()
    }
    
    /**
     * 카드 교체 가능 횟수 설정
     */
    fun setReplacementsLeft(count: Int) {
        replacementsLeft = count
    }
    
    /**
     * 교체 가능 횟수 반환
     */
    fun getReplacementsLeft(): Int {
        return replacementsLeft
    }
    
    /**
     * 교체 횟수 초기화
     */
    fun resetReplacements() {
        // 프리미엄 유저는 교체 횟수 2회
        replacementsLeft = if (userManager.isPremium()) 2 else 1
    }
    
    /**
     * 카드 선택 토글
     */
    fun toggleCardSelection(index: Int): Boolean {
        if (index < 0 || index >= playerCards.size) return false
        
        // 교체 기회가 없으면 선택 불가
        if (replacementsLeft <= 0) return false
        
        // 선택 상태 토글
        if (selectedCardIndices.contains(index)) {
            selectedCardIndices.remove(index)
            playerCards[index].isSelected = false
        } else {
            selectedCardIndices.add(index)
            playerCards[index].isSelected = true
        }
        
        return true
    }
    
    /**
     * 선택된 카드 교체
     */
    fun replaceSelectedCards(): Boolean {
        // 교체 기회가 없거나 선택된 카드가 없으면 실패
        if (replacementsLeft <= 0 || selectedCardIndices.isEmpty()) {
            return false
        }
        
        // 현재 덱에 남은 카드 확인
        val remainingCards = pokerDeck.getCardsLeft()
        
        // 교체할 카드가 더 많으면 덱 리셋
        if (remainingCards < selectedCardIndices.size) {
            pokerDeck.initializeDeck()
            pokerDeck.shuffle()
        }
        
        // 선택된 카드들을 새 카드로 교체
        for (index in selectedCardIndices) {
            val newCard = pokerDeck.drawSingleCard() ?: continue
            playerCards[index] = newCard
        }
        
        // 교체 횟수 감소
        replacementsLeft--
        
        // 선택 초기화
        clearSelections()
        
        // 핸드 재평가
        evaluateCurrentHand()
        
        return true
    }
    
    /**
     * 모든 카드 선택 초기화
     */
    fun clearSelections() {
        selectedCardIndices.clear()
        playerCards.forEach { it.isSelected = false }
    }
    
    /**
     * 선택된 카드 인덱스 목록 반환
     */
    fun getSelectedIndices(): Set<Int> {
        return selectedCardIndices.toSet()
    }
    
    /**
     * 특정 슈트의 플러시 효과 활성화 여부 확인
     */
    fun hasFlushOfSuit(suit: CardSuit): Boolean {
        // 현재 핸드가 플러시이고 그 슈트가 지정된 슈트인지 확인
        if (currentPokerHand is Flush) {
            // 플러시는 같은 무늬 5장이므로 첫 번째 카드의 무늬만 확인하면 됨
            return playerCards.isNotEmpty() && playerCards[0].getEffectiveSuit() == suit
        }
        return false
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        playerCards.clear()
        selectedCardIndices.clear()
    }
} 