package com.example.p40

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.PokerDeck
import com.example.p40.game.PokerHand

/**
 * 웨이브 종료 후 표시되는 포커 카드 선택 다이얼로그
 */
class PokerCardsDialog(
    context: Context,
    private val waveNumber: Int,
    private val onHandConfirmed: (PokerHand) -> Unit
) : Dialog(context) {

    private val pokerDeck = PokerDeck()
    private var currentCards = listOf<Card>()
    private var cardViews = arrayOfNulls<CardView>(5)
    private var cardSuitViews = arrayOfNulls<TextView>(5)
    private var cardRankViews = arrayOfNulls<TextView>(5)
    
    private lateinit var tvCurrentHand: TextView
    private lateinit var tvHandDescription: TextView
    private lateinit var tvWaveCompleted: TextView
    
    init {
        // 최초 덱 초기화
        pokerDeck.initializeDeck()
        
        // 보스 웨이브였다면 조커 카드 추가
        if (waveNumber % 5 == 0) {
            pokerDeck.addJoker()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 타이틀 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 백그라운드 클릭 시 종료 방지
        setCancelable(false)
        
        // 레이아웃 설정
        setContentView(R.layout.dialog_poker_cards)
        
        // 뷰 초기화
        initViews()
        
        // 카드 뽑기
        drawCards()
        
        // 버튼 이벤트 설정
        setupButtons()
    }
    
    private fun initViews() {
        tvWaveCompleted = findViewById(R.id.tvWaveCompleted)
        tvWaveCompleted.text = "웨이브 ${waveNumber} 완료!"
        
        tvCurrentHand = findViewById(R.id.tvCurrentHand)
        tvHandDescription = findViewById(R.id.tvHandDescription)
        
        // 카드 뷰 초기화
        cardViews[0] = findViewById(R.id.cardView1)
        cardViews[1] = findViewById(R.id.cardView2)
        cardViews[2] = findViewById(R.id.cardView3)
        cardViews[3] = findViewById(R.id.cardView4)
        cardViews[4] = findViewById(R.id.cardView5)
        
        cardSuitViews[0] = findViewById(R.id.tvCardSuit1)
        cardSuitViews[1] = findViewById(R.id.tvCardSuit2)
        cardSuitViews[2] = findViewById(R.id.tvCardSuit3)
        cardSuitViews[3] = findViewById(R.id.tvCardSuit4)
        cardSuitViews[4] = findViewById(R.id.tvCardSuit5)
        
        cardRankViews[0] = findViewById(R.id.tvCardRank1)
        cardRankViews[1] = findViewById(R.id.tvCardRank2)
        cardRankViews[2] = findViewById(R.id.tvCardRank3)
        cardRankViews[3] = findViewById(R.id.tvCardRank4)
        cardRankViews[4] = findViewById(R.id.tvCardRank5)
        
        // 카드 클릭 이벤트 설정
        for (i in cardViews.indices) {
            cardViews[i]?.setOnClickListener {
                toggleCardSelection(i)
            }
        }
    }
    
    private fun drawCards() {
        // 5장 카드 뽑기
        currentCards = pokerDeck.draw5Cards()
        
        // 카드 표시
        updateCardDisplay()
        
        // 현재 족보 평가
        updateHandDisplay()
    }
    
    private fun updateCardDisplay() {
        for (i in currentCards.indices) {
            val card = currentCards[i]
            
            // 카드 무늬 표시
            val suitSymbol = when (card.suit) {
                CardSuit.HEART -> "♥"
                CardSuit.DIAMOND -> "♦"
                CardSuit.CLUB -> "♣"
                CardSuit.SPADE -> "♠"
                CardSuit.JOKER -> "★"
            }
            cardSuitViews[i]?.text = suitSymbol
            
            // 카드 숫자 표시
            cardRankViews[i]?.text = card.rank.getName()
            
            // 카드 색상 설정
            val cardColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND || card.suit == CardSuit.JOKER) {
                Color.RED
            } else {
                Color.BLACK
            }
            cardSuitViews[i]?.setTextColor(cardColor)
            cardRankViews[i]?.setTextColor(cardColor)
            
            // 선택 상태 업데이트
            updateCardSelectionDisplay(i)
        }
    }
    
    private fun toggleCardSelection(index: Int) {
        if (index < currentCards.size) {
            val card = currentCards[index]
            card.isSelected = !card.isSelected
            updateCardSelectionDisplay(index)
        }
    }
    
    private fun updateCardSelectionDisplay(index: Int) {
        val isSelected = currentCards[index].isSelected
        
        // 선택된 카드는 테두리 색상 변경
        if (isSelected) {
            cardViews[index]?.setCardBackgroundColor(Color.parseColor("#FFD700")) // 골드 색상
        } else {
            cardViews[index]?.setCardBackgroundColor(Color.WHITE)
        }
    }
    
    private fun updateHandDisplay() {
        // 족보 평가
        val currentHand = pokerDeck.evaluateHand()
        
        // 족보 표시
        tvCurrentHand.text = "현재 족보: ${currentHand.handName}"
        tvHandDescription.text = "효과: ${currentHand.getDescription()}"
    }
    
    private fun setupButtons() {
        // 교체 버튼
        val btnReplaceCards = findViewById<Button>(R.id.btnReplaceCards)
        btnReplaceCards.setOnClickListener {
            // 선택된 카드 교체
            pokerDeck.replaceSelectedCards()
            currentCards = pokerDeck.playerHand
            
            // 카드 표시 업데이트
            updateCardDisplay()
            
            // 족보 업데이트
            updateHandDisplay()
            
            // 교체 후 버튼 비활성화
            btnReplaceCards.isEnabled = false
            btnReplaceCards.text = "교체 완료"
        }
        
        // 확정 버튼
        val btnConfirmHand = findViewById<Button>(R.id.btnConfirmHand)
        btnConfirmHand.setOnClickListener {
            // 현재 족보 확정 및 콜백 호출
            val finalHand = pokerDeck.evaluateHand()
            onHandConfirmed(finalHand)
            
            // 다이얼로그 닫기
            dismiss()
        }
    }
} 