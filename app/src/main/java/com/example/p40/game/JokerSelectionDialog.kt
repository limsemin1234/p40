package com.example.p40.game

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.p40.R

/**
 * 조커 카드를 다른 카드로 대체하기 위한 선택 다이얼로그
 */
class JokerSelectionDialog(
    context: Context,
    private val onCardSelected: (Card) -> Unit
) : Dialog(context) {

    private var selectedSuit: CardSuit = CardSuit.HEART
    private var selectedRank: CardRank = CardRank.ACE
    
    private lateinit var btnConfirm: Button
    private lateinit var tvSelectedCard: TextView
    private lateinit var tvPreviewSuit: TextView
    private lateinit var tvPreviewRank: TextView
    private lateinit var suitPicker: NumberPicker
    private lateinit var rankPicker: NumberPicker
    
    // 카드 모양 배열
    private val suits = arrayOf(
        CardSuit.HEART,
        CardSuit.DIAMOND,
        CardSuit.CLUB,
        CardSuit.SPADE
    )
    
    // 카드 숫자 배열
    private val ranks = arrayOf(
        CardRank.ACE,
        CardRank.TWO,
        CardRank.THREE,
        CardRank.FOUR,
        CardRank.FIVE,
        CardRank.SIX,
        CardRank.SEVEN,
        CardRank.EIGHT,
        CardRank.NINE,
        CardRank.TEN,
        CardRank.JACK,
        CardRank.QUEEN,
        CardRank.KING
    )
    
    // 카드 모양 표시 문자열
    private val suitDisplayValues = arrayOf("♥", "♦", "♣", "♠")
    
    // 카드 숫자 표시 문자열
    private val rankDisplayValues = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 타이틀 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 레이아웃 설정
        setContentView(R.layout.dialog_joker_selection)
        
        // 다이얼로그 크기 설정 (화면 너비의 90% 사용)
        val window = window
        if (window != null) {
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(android.view.Gravity.CENTER)
        }
        
        // 뷰 초기화
        initViews()
        
        // 선택기 설정
        setupPickers()
        
        // 버튼 이벤트 설정
        setupButtons()
        
        // 선택 상태 업데이트
        updateSelectedCardDisplay()
    }
    
    private fun initViews() {
        // 선택된 카드 텍스트뷰
        tvSelectedCard = findViewById(R.id.tvSelectedCard)
        
        // 카드 미리보기
        tvPreviewSuit = findViewById(R.id.tvPreviewSuit)
        tvPreviewRank = findViewById(R.id.tvPreviewRank)
        
        // 확인 버튼
        btnConfirm = findViewById(R.id.btnConfirm)
        
        // 선택기
        suitPicker = findViewById(R.id.suitPicker)
        rankPicker = findViewById(R.id.rankPicker)
    }
    
    private fun setupPickers() {
        // 카드 모양 선택기 설정
        suitPicker.minValue = 0
        suitPicker.maxValue = suitDisplayValues.size - 1
        suitPicker.displayedValues = suitDisplayValues
        
        // 카드 숫자 선택기 설정
        rankPicker.minValue = 0
        rankPicker.maxValue = rankDisplayValues.size - 1
        rankPicker.displayedValues = rankDisplayValues
        
        // 선택기 변경 리스너 설정
        suitPicker.setOnValueChangedListener { _, _, newVal ->
            selectedSuit = suits[newVal]
            updateCardPreview()
            updateSelectedCardDisplay()
        }
        
        rankPicker.setOnValueChangedListener { _, _, newVal ->
            selectedRank = ranks[newVal]
            updateCardPreview()
            updateSelectedCardDisplay()
        }
    }
    
    private fun updateCardPreview() {
        // 카드 모양 및 색상 설정
        tvPreviewSuit.text = getSuitSymbol(selectedSuit)
        tvPreviewSuit.setTextColor(getSuitColor(selectedSuit))
        
        // 카드 숫자 및 색상 설정
        tvPreviewRank.text = getRankSymbol(selectedRank)
        tvPreviewRank.setTextColor(getSuitColor(selectedSuit))
    }
    
    private fun getSuitSymbol(suit: CardSuit): String {
        return when (suit) {
            CardSuit.HEART -> "♥"
            CardSuit.DIAMOND -> "♦"
            CardSuit.CLUB -> "♣"
            CardSuit.SPADE -> "♠"
            else -> "?"
        }
    }
    
    private fun getRankSymbol(rank: CardRank): String {
        return rank.getName()
    }
    
    private fun getSuitColor(suit: CardSuit): Int {
        return when (suit) {
            CardSuit.HEART, CardSuit.DIAMOND -> Color.RED
            else -> Color.BLACK
        }
    }
    
    private fun updateSelectedCardDisplay() {
        tvSelectedCard.text = "선택된 카드: ${selectedSuit.getName()} ${selectedRank.getName()}"
    }
    
    private fun setupButtons() {
        // 확인 버튼
        btnConfirm.setOnClickListener {
            // 선택된 카드 생성
            val selectedCard = Card(selectedSuit, selectedRank)
            
            // 콜백 호출
            onCardSelected(selectedCard)
            
            // 다이얼로그 닫기
            dismiss()
        }
        
        // 취소 버튼
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }
} 