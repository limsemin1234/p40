package com.example.p40.game

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import com.example.p40.R

/**
 * 조커 카드를 다른 카드로 대체하기 위한 선택 다이얼로그
 */
class JokerSelectionDialog(
    context: Context,
    private val onCardSelected: (Card) -> Unit
) : Dialog(context) {

    private var selectedSuit: CardSuit? = null
    private var selectedRank: CardRank? = null
    
    private lateinit var btnConfirm: Button
    private lateinit var tvSelectedCard: TextView
    
    private val suitButtons = mutableListOf<Button>()
    private val rankButtons = mutableListOf<Button>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 타이틀 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 레이아웃 설정
        setContentView(R.layout.dialog_joker_selection)
        
        // 뷰 초기화
        initViews()
        
        // 버튼 이벤트 설정
        setupButtons()
        
        // 선택 상태 업데이트
        updateSelectedCardDisplay()
    }
    
    private fun initViews() {
        // 선택된 카드 텍스트뷰
        tvSelectedCard = findViewById(R.id.tvSelectedCard)
        
        // 확인 버튼
        btnConfirm = findViewById(R.id.btnConfirm)
        btnConfirm.isEnabled = false // 초기에는 비활성화
        
        // 슈트(모양) 버튼 설정
        val suitLayout = findViewById<GridLayout>(R.id.suitLayout)
        setupSuitButtons(suitLayout)
        
        // 랭크(숫자) 버튼 설정
        val rankLayout = findViewById<GridLayout>(R.id.rankLayout)
        setupRankButtons(rankLayout)
    }
    
    private fun setupSuitButtons(suitLayout: GridLayout) {
        // 슈트(모양) 버튼 동적 생성 - JOKER 제외
        val suits = CardSuit.values().filter { it != CardSuit.JOKER }
        
        for (suit in suits) {
            val button = Button(context).apply {
                text = when (suit) {
                    CardSuit.HEART -> "♥"
                    CardSuit.DIAMOND -> "♦"
                    CardSuit.CLUB -> "♣"
                    CardSuit.SPADE -> "♠"
                    else -> ""
                }
                
                // 텍스트 색상 설정
                setTextColor(
                    when (suit) {
                        CardSuit.HEART, CardSuit.DIAMOND -> Color.RED
                        else -> Color.BLACK
                    }
                )
                
                // 크기 및 마진 설정
                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.WRAP_CONTENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(10, 10, 10, 10)
                layoutParams = params
                
                // 클릭 이벤트
                setOnClickListener {
                    selectSuit(suit)
                }
            }
            
            suitButtons.add(button)
            suitLayout.addView(button)
        }
    }
    
    private fun setupRankButtons(rankLayout: GridLayout) {
        // 랭크(숫자) 버튼 동적 생성 - JOKER 제외
        val ranks = CardRank.values().filter { it != CardRank.JOKER }
        
        for (rank in ranks) {
            val button = Button(context).apply {
                text = rank.getName()
                
                // 크기 및 마진 설정
                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.WRAP_CONTENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(8, 8, 8, 8)
                layoutParams = params
                
                // 클릭 이벤트
                setOnClickListener {
                    selectRank(rank)
                }
            }
            
            rankButtons.add(button)
            rankLayout.addView(button)
        }
    }
    
    private fun selectSuit(suit: CardSuit) {
        selectedSuit = suit
        
        // 선택된 슈트 버튼 강조
        suitButtons.forEachIndexed { index, button ->
            val currentSuit = CardSuit.values().filter { it != CardSuit.JOKER }[index]
            button.isSelected = (currentSuit == suit)
            button.setBackgroundResource(if (button.isSelected) android.R.color.holo_blue_light else android.R.color.background_light)
        }
        
        updateSelectedCardDisplay()
        updateConfirmButton()
    }
    
    private fun selectRank(rank: CardRank) {
        selectedRank = rank
        
        // 선택된 랭크 버튼 강조
        rankButtons.forEachIndexed { index, button ->
            val currentRank = CardRank.values().filter { it != CardRank.JOKER }[index]
            button.isSelected = (currentRank == rank)
            button.setBackgroundResource(if (button.isSelected) android.R.color.holo_blue_light else android.R.color.background_light)
        }
        
        updateSelectedCardDisplay()
        updateConfirmButton()
    }
    
    private fun updateSelectedCardDisplay() {
        tvSelectedCard.text = if (selectedSuit != null && selectedRank != null) {
            "선택된 카드: ${selectedSuit!!.getName()} ${selectedRank!!.getName()}"
        } else if (selectedSuit != null) {
            "선택된 모양: ${selectedSuit!!.getName()}"
        } else if (selectedRank != null) {
            "선택된 숫자: ${selectedRank!!.getName()}"
        } else {
            "조커를 변환할 카드를 선택하세요"
        }
    }
    
    private fun updateConfirmButton() {
        btnConfirm.isEnabled = (selectedSuit != null && selectedRank != null)
    }
    
    private fun setupButtons() {
        // 확인 버튼
        btnConfirm.setOnClickListener {
            if (selectedSuit != null && selectedRank != null) {
                // 선택된 카드 생성
                val selectedCard = Card(selectedSuit!!, selectedRank!!)
                
                // 콜백 호출
                onCardSelected(selectedCard)
                
                // 다이얼로그 닫기
                dismiss()
            } else {
                Toast.makeText(context, "카드 모양과 숫자를 모두 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 취소 버튼
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }
} 