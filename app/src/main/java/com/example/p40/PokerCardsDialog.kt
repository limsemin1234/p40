package com.example.p40

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
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.GameConfig
import com.example.p40.game.PokerDeck
import com.example.p40.game.PokerHand

/**
 * 덱 구성 또는 웨이브 종료 후 표시되는 포커 카드 다이얼로그
 */
class PokerCardsDialog(
    context: Context,
    private val waveNumber: Int = 0, // 0이면 덱 구성 모드, 1 이상이면 웨이브 보상 모드
    private val onHandConfirmed: (PokerHand) -> Unit
) : Dialog(context) {

    private val pokerDeck = PokerDeck()
    private var currentCards = mutableListOf<Card>()
    private var cardViews = arrayOfNulls<CardView>(5)
    private var cardSuitViews = arrayOfNulls<TextView>(5)
    private var cardRankViews = arrayOfNulls<TextView>(5)
    
    private lateinit var tvCurrentHand: TextView
    private lateinit var tvHandDescription: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnReplaceCards: Button
    private lateinit var btnConfirmHand: Button
    private lateinit var btnPrevCards: Button
    private lateinit var btnNextCards: Button
    private lateinit var tvReplaceCount: TextView
    
    // 덱 구성 모드에서 사용할 변수
    private var allCards = mutableListOf<Card>()
    private var currentPage = 0
    private val cardsPerPage = 5
    private var selectedDeckCards = mutableListOf<Card>()
    
    // 포커 카드 교체 횟수
    private var remainingReplaceCount = GameConfig.POKER_CARD_REPLACE_COUNT
    
    init {
        if (waveNumber == 0) {
            // 덱 구성 모드일 경우 모든 카드 준비
            allCards = mutableListOf()
            CardSuit.values().forEach { suit ->
                if (suit != CardSuit.JOKER) {
                    CardRank.values().forEach { rank ->
                        if (rank != CardRank.JOKER) {
                            allCards.add(Card(suit, rank))
                        }
                    }
                }
            }
            // 조커 카드도 추가
            allCards.add(Card.createJoker())
            
            // 문양 조커 카드도 추가 (하트, 스페이드, 다이아몬드, 클럽)
            allCards.add(Card.createHeartJoker(CardRank.JOKER))
            allCards.add(Card.createSpadeJoker(CardRank.JOKER))
            allCards.add(Card.createDiamondJoker(CardRank.JOKER))
            allCards.add(Card.createClubJoker(CardRank.JOKER))
        } else {
            // 웨이브 보상 모드일 때 - 저장된 덱 로드 또는 기본 덱 사용
            val savedDeck = DeckBuilderFragment.loadDeckFromPrefs(context)
            
            if (savedDeck != null && savedDeck.isNotEmpty()) {
                // 저장된 덱 사용
                pokerDeck.initializeWithCards(savedDeck)
            } else {
                // 저장된 덱이 없으면 기본 덱 사용
                pokerDeck.initializeDeck()
            }
            
            // 보스 웨이브는 조커 카드 추가 (5의 배수 웨이브)
            if (waveNumber % 5 == 0) {
                pokerDeck.addJoker()
            }
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
        
        // 다이얼로그 크기 설정 (화면 너비의 95% 사용)
        val window = window
        if (window != null) {
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.95).toInt()
            
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(android.view.Gravity.CENTER)
        }
        
        // 뷰 초기화
        initViews()
        
        if (waveNumber == 0) {
            // 덱 구성 모드
            setupDeckBuilderMode()
        } else {
            // 웨이브 보상 모드
            drawCards()
        }
        
        // 버튼 이벤트 설정
        setupButtons()
    }
    
    private fun initViews() {
        tvTitle = findViewById(R.id.tvWaveCompleted)
        tvCurrentHand = findViewById(R.id.tvCurrentHand)
        tvHandDescription = findViewById(R.id.tvHandDescription)
        tvReplaceCount = findViewById(R.id.tvReplaceCount)
        
        // 타이틀 설정
        if (waveNumber == 0) {
            tvTitle.text = "덱 구성"
            tvReplaceCount.visibility = View.GONE
        } else {
            tvTitle.text = "웨이브 ${waveNumber} 완료!"
            tvReplaceCount.visibility = View.VISIBLE
            tvReplaceCount.text = "교체 가능 횟수: ${remainingReplaceCount}"
        }
        
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
        
        // 버튼 초기화
        btnReplaceCards = findViewById(R.id.btnReplaceCards)
        btnConfirmHand = findViewById(R.id.btnConfirmHand)
        btnPrevCards = findViewById(R.id.btnPrevCards) // 이전 카드 페이지 버튼
        btnNextCards = findViewById(R.id.btnNextCards) // 다음 카드 페이지 버튼
        
        // 덱 구성 모드에서만 이전/다음 버튼 표시
        if (waveNumber == 0) {
            btnPrevCards.visibility = View.VISIBLE
            btnNextCards.visibility = View.VISIBLE
            btnReplaceCards.text = "덱에 추가"
            btnConfirmHand.text = "완료"
        } else {
            btnPrevCards.visibility = View.GONE
            btnNextCards.visibility = View.GONE
        }
        
        // 카드 클릭 이벤트 설정
        for (i in cardViews.indices) {
            cardViews[i]?.setOnClickListener {
                toggleCardSelection(i)
            }
            
            // 카드 롱클릭 이벤트 설정 - 문양 조커 카드 변환용
            cardViews[i]?.setOnLongClickListener {
                if (i < currentCards.size) {
                    val card = currentCards[i]
                    // 문양 조커인 경우에만 숫자 선택 다이얼로그 표시
                    if (card.isJoker && card.suit != CardSuit.JOKER) {
                        showJokerNumberPickerDialog(card, i)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }
    
    private fun setupDeckBuilderMode() {
        // 첫 페이지 카드 표시
        showCardsPage(0)
        
        // 덱 구성 모드에서는 선택한 카드 정보 표시
        updateDeckInfo()
    }
    
    private fun showCardsPage(page: Int) {
        currentPage = page
        val startIndex = page * cardsPerPage
        
        // 현재 페이지에 표시할 카드
        currentCards = if (startIndex < allCards.size) {
            val endIndex = minOf(startIndex + cardsPerPage, allCards.size)
            val pageCards = allCards.subList(startIndex, endIndex).toMutableList()
            
            // 5장이 안 되는 경우 빈 카드로 채우기
            while (pageCards.size < cardsPerPage) {
                pageCards.add(Card(CardSuit.JOKER, CardRank.JOKER, isSelected = false))
            }
            
            pageCards
        } else {
            // 페이지가 범위를 벗어나면 빈 카드로 채우기
            MutableList(cardsPerPage) { Card(CardSuit.JOKER, CardRank.JOKER, isSelected = false) }
        }
        
        // 카드 표시 업데이트
        updateCardDisplay()
        
        // 이전/다음 버튼 활성화 상태 설정
        btnPrevCards.isEnabled = page > 0
        btnNextCards.isEnabled = (page + 1) * cardsPerPage < allCards.size
    }
    
    private fun drawCards() {
        // 카드 5장 뽑기
        pokerDeck.drawCards(5)
        
        // 플레이어 핸드 가져오기
        currentCards = pokerDeck.playerHand.toMutableList()
        
        // 카드 표시 업데이트
        updateCardDisplay()
        
        // 족보 업데이트
        updateHandDisplay()
        
        // 조커 카드 포함 여부 확인 및 안내
        if (waveNumber > 0 && currentCards.any { it.suit == CardSuit.JOKER && it.rank == CardRank.JOKER }) {
            Toast.makeText(
                context,
                "조커 카드를 길게 누르면 원하는 카드로 변환할 수 있습니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun updateCardDisplay() {
        for (i in currentCards.indices) {
            val card = currentCards[i]
            
            // 빈 카드 처리
            if (card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER && waveNumber == 0 && !card.isSelected) {
                cardViews[i]?.visibility = View.INVISIBLE
                continue
            } else {
                cardViews[i]?.visibility = View.VISIBLE
            }
            
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
            if (card.isJoker) {
                if (card.suit == CardSuit.JOKER || card.rank == CardRank.JOKER) {
                    cardRankViews[i]?.text = "조커"
                } else {
                    cardRankViews[i]?.text = card.rank.getName()
                }
            } else {
                cardRankViews[i]?.text = card.rank.getName()
            }
            
            // 카드 색상 설정
            val cardColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND || card.suit == CardSuit.JOKER) {
                Color.RED
            } else {
                Color.BLACK
            }
            cardSuitViews[i]?.setTextColor(cardColor)
            cardRankViews[i]?.setTextColor(cardColor)
            
            // 덱 구성 모드에서 이미 선택된 카드 표시
            if (waveNumber == 0) {
                card.isSelected = selectedDeckCards.any { 
                    it.suit == card.suit && it.rank == card.rank 
                }
            }
            
            // 선택 상태 업데이트
            updateCardSelectionDisplay(i)
            
            // 조커 카드에 롱클릭 리스너 추가
            if (waveNumber > 0) {
                // 별 모양 조커 처리
                if (card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER) {
                    cardViews[i]?.setOnLongClickListener {
                        showJokerSelectionDialog(i)
                        true
                    }
                    
                    // 조커 카드는 "변환 가능" 힌트 텍스트 추가
                    cardViews[i]?.contentDescription = "조커 카드 - 길게 누르면 변환할 수 있습니다"
                }
                // 문양 조커 처리 (하트, 스페이드, 다이아, 클로버 조커)
                else if (card.isJoker && card.suit != CardSuit.JOKER) {
                    cardViews[i]?.setOnLongClickListener {
                        showJokerNumberPickerDialog(card, i)
                        true
                    }
                    
                    // 문양 조커 카드도 힌트 텍스트 추가
                    cardViews[i]?.contentDescription = "${card.suit.getName()} 조커 - 길게 누르면 숫자를 선택할 수 있습니다"
                } else {
                    cardViews[i]?.setOnLongClickListener(null)
                    cardViews[i]?.contentDescription = null
                }
            }
        }
    }
    
    private fun toggleCardSelection(index: Int) {
        if (index < currentCards.size) {
            val card = currentCards[index]
            
            // 빈 카드는 선택할 수 없음
            if (card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER && waveNumber == 0 && !card.isSelected) {
                return
            }
            
            // 덱 구성 모드에서는 카드 선택을 토글
            if (waveNumber == 0) {
                val existingCard = selectedDeckCards.find { 
                    it.suit == card.suit && it.rank == card.rank 
                }
                
                if (existingCard != null) {
                    // 이미 선택된 카드면 제거
                    selectedDeckCards.remove(existingCard)
                    card.isSelected = false
                } else {
                    // 아직 덱에 추가하지 않은 카드
                    card.isSelected = !card.isSelected
                }
                
                updateDeckInfo()
            } else {
                // 웨이브 보상 모드에서 조커 카드는 롱클릭으로 처리
                if (card.suit == CardSuit.JOKER && card.rank == CardRank.JOKER) {
                    // 별 조커 변환 힌트 토스트 표시
                    Toast.makeText(context, "조커 카드를 길게 누르면 원하는 카드로 변환할 수 있습니다.", Toast.LENGTH_SHORT).show()
                }
                // 문양 조커도 안내 메시지 표시
                else if (card.isJoker && card.suit != CardSuit.JOKER) {
                    // 문양 조커 안내 메시지 표시
                    Toast.makeText(context, "${card.suit.getName()} 조커를 길게 누르면 숫자를 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 일반 카드는 교체를 위한 선택 상태 토글
                    card.isSelected = !card.isSelected
                }
            }
            
            // 선택 상태 UI 업데이트
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
    
    private fun updateDeckInfo() {
        // 선택된 카드 수 표시
        tvCurrentHand.text = "선택된 카드: ${selectedDeckCards.size}장"
        tvHandDescription.text = "포커 덱은 52장의 카드와 조커로 구성됩니다."
    }
    
    private fun setupButtons() {
        if (waveNumber == 0) {
            // 덱 구성 모드일 때 버튼 설정
            
            // 덱에 추가 버튼
            btnReplaceCards.setOnClickListener {
                // 선택된 카드를 덱에 추가
                val selectedCards = currentCards.filter { it.isSelected && !(it.suit == CardSuit.JOKER && it.rank == CardRank.JOKER) }
                
                for (card in selectedCards) {
                    // 이미 추가된 카드가 아니면 추가
                    if (!selectedDeckCards.any { it.suit == card.suit && it.rank == card.rank }) {
                        selectedDeckCards.add(Card(card.suit, card.rank))
                    }
                }
                
                // 선택 상태 초기화
                for (card in currentCards) {
                    card.isSelected = false
                }
                
                // 카드 표시 업데이트
                updateCardDisplay()
                
                // 덱 정보 업데이트
                updateDeckInfo()
                
                Toast.makeText(context, "선택한 카드가 덱에 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
            
            // 이전 페이지 버튼
            btnPrevCards.setOnClickListener {
                if (currentPage > 0) {
                    showCardsPage(currentPage - 1)
                }
            }
            
            // 다음 페이지 버튼
            btnNextCards.setOnClickListener {
                showCardsPage(currentPage + 1)
            }
            
            // 완료 버튼
            btnConfirmHand.setOnClickListener {
                // 임의의 족보 확정 및 콜백 호출
                onHandConfirmed(PokerHand.HIGH_CARD)
                
                // 덱 구성 완료 메시지
                Toast.makeText(context, "덱 구성이 완료되었습니다. (${selectedDeckCards.size}장)", Toast.LENGTH_SHORT).show()
                
                // 다이얼로그 닫기
                dismiss()
            }
        } else {
            // 웨이브 보상 모드일 때 버튼 설정
            
            // 교체 버튼
            btnReplaceCards.setOnClickListener {
                // 교체 횟수가 남아있는지 확인
                if (remainingReplaceCount <= 0) {
                    Toast.makeText(context, "더 이상 교체할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // 선택된 카드가 있는지 확인
                val hasSelectedCards = currentCards.any { it.isSelected }
                if (!hasSelectedCards) {
                    Toast.makeText(context, "교체할 카드를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // 선택된 카드 교체
                pokerDeck.replaceSelectedCards()
                currentCards = pokerDeck.playerHand.toMutableList()
                
                // 교체 횟수 감소
                remainingReplaceCount--
                tvReplaceCount.text = "교체 가능 횟수: ${remainingReplaceCount}"
                
                // 카드 표시 업데이트
                updateCardDisplay()
                
                // 족보 업데이트
                updateHandDisplay()
                
                // 교체 후 버튼 상태 업데이트
                if (remainingReplaceCount <= 0) {
                    btnReplaceCards.isEnabled = false
                    btnReplaceCards.text = "교체 완료"
                }
            }
            
            // 확정 버튼
            btnConfirmHand.setOnClickListener {
                // 현재 족보 확정 및 콜백 호출
                val finalHand = pokerDeck.evaluateHand()
                onHandConfirmed(finalHand)
                
                // 다이얼로그 닫기
                dismiss()
            }
        }
    }
    
    // 조커 선택 다이얼로그 표시 메서드 추가
    private fun showJokerSelectionDialog(jokerIndex: Int) {
        // 조커 카드가 맞는지 확인
        if (jokerIndex >= currentCards.size || currentCards[jokerIndex].suit != CardSuit.JOKER) {
            return
        }
        
        // 조커 선택 다이얼로그 생성 및 표시
        val jokerDialog = com.example.p40.game.JokerSelectionDialog(context) { selectedCard ->
            // 선택된 카드로 조커 카드 대체
            currentCards[jokerIndex] = selectedCard
            
            // 플레이어 핸드 업데이트 (PokerDeck 클래스의 playerHand 변수)
            pokerDeck.playerHand = currentCards.toMutableList()
            
            // 카드 표시 업데이트
            updateCardDisplay()
            
            // 족보 업데이트
            updateHandDisplay()
            
            // 토스트 메시지
            Toast.makeText(
                context,
                "조커 카드가 ${selectedCard.suit.getName()} ${selectedCard.rank.getName()}(으)로 변환되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        jokerDialog.show()
    }
    
    /**
     * 조커 카드의 숫자를 선택하는 다이얼로그 표시
     */
    private fun showJokerNumberPickerDialog(card: Card, cardIndex: Int) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_joker_number_picker)
        
        // 윈도우 크기 조정
        val window = dialog.window
        if (window != null) {
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(android.view.Gravity.CENTER)
        }
        
        // 뷰 초기화
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)
        val tvPreviewSuit = dialog.findViewById<TextView>(R.id.tvPreviewSuit)
        val tvPreviewRank = dialog.findViewById<TextView>(R.id.tvPreviewRank)
        val numberPicker = dialog.findViewById<NumberPicker>(R.id.numberPicker)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        
        // 타이틀 설정
        tvTitle.text = "${card.suit.getName()} 조커 변환"
        tvDescription.text = "조커를 변환할 숫자를 선택하세요"
        
        // 카드 미리보기 초기화
        val suitSymbol = when (card.suit) {
            CardSuit.HEART -> "♥"
            CardSuit.DIAMOND -> "♦"
            CardSuit.CLUB -> "♣"
            CardSuit.SPADE -> "♠"
            CardSuit.JOKER -> "★"
        }
        tvPreviewSuit.text = suitSymbol
        
        // 카드 색상 설정
        val cardColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND) {
            Color.RED
        } else {
            Color.BLACK
        }
        tvPreviewSuit.setTextColor(cardColor)
        tvPreviewRank.setTextColor(cardColor)
        
        // 숫자 선택기 설정
        numberPicker.minValue = 1
        numberPicker.maxValue = 13
        val rankValues = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        numberPicker.displayedValues = rankValues
        
        // 현재 값이 JOKER가 아니면 그 값을 기본값으로 설정
        if (card.rank != CardRank.JOKER) {
            val value = card.rank.value
            if (value in 1..13) {
                numberPicker.value = value
            } else {
                numberPicker.value = 1 // 기본값 A
            }
        } else {
            numberPicker.value = 1 // 기본값 A
        }
        
        // 미리보기 업데이트
        updatePreview(tvPreviewRank, numberPicker.value - 1, rankValues)
        
        // 값 변경 리스너
        numberPicker.setOnValueChangedListener { _, _, newVal ->
            updatePreview(tvPreviewRank, newVal - 1, rankValues)
        }
        
        // 취소 버튼
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 확인 버튼
        btnConfirm.setOnClickListener {
            // 선택된 카드 랭크로 변환
            val selectedRank = getCardRankByValue(numberPicker.value)
            replaceJokerCard(card, selectedRank, cardIndex)
            dialog.dismiss()
            
            // 변경 알림
            Toast.makeText(
                context,
                "${card.suit.getName()} 조커를 ${selectedRank.getName()}으로 변환했습니다", 
                Toast.LENGTH_SHORT
            ).show()
            
            // 카드 조합 업데이트
            updatePokerHand()
        }
        
        dialog.show()
    }
    
    // 미리보기 업데이트
    private fun updatePreview(tvPreviewRank: TextView, index: Int, rankValues: Array<String>) {
        tvPreviewRank.text = rankValues[index]
    }
    
    // 카드 랭크 값으로 CardRank 반환
    private fun getCardRankByValue(value: Int): CardRank {
        return when (value) {
            1 -> CardRank.ACE
            2 -> CardRank.TWO
            3 -> CardRank.THREE
            4 -> CardRank.FOUR
            5 -> CardRank.FIVE
            6 -> CardRank.SIX
            7 -> CardRank.SEVEN
            8 -> CardRank.EIGHT
            9 -> CardRank.NINE
            10 -> CardRank.TEN
            11 -> CardRank.JACK
            12 -> CardRank.QUEEN
            13 -> CardRank.KING
            else -> CardRank.ACE // 기본값
        }
    }
    
    // 조커 카드 변환
    private fun replaceJokerCard(card: Card, newRank: CardRank, cardIndex: Int) {
        if (cardIndex >= 0 && cardIndex < currentCards.size) {
            // 새 카드 생성
            val newCard = Card(
                suit = card.suit,
                rank = newRank,
                isJoker = true  // isJoker 속성 유지
            )
            
            // 카드 교체
            currentCards[cardIndex] = newCard
            
            // UI 업데이트
            updateCardView(cardIndex, newCard)
            
            // 토스트 메시지 표시
            Toast.makeText(
                context,
                "${card.suit.getName()} 조커를 ${newRank.getName()}(으)로 변환했습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // 카드 모양과 숫자 업데이트 (추가 메서드)
    private fun updateCardView(index: Int, card: Card) {
        if (index < 0 || index >= cardViews.size) return
        
        // 카드 무늬 설정
        val suitSymbol = when (card.suit) {
            CardSuit.HEART -> "♥"
            CardSuit.DIAMOND -> "♦"
            CardSuit.CLUB -> "♣"
            CardSuit.SPADE -> "♠"
            CardSuit.JOKER -> "★"
        }
        cardSuitViews[index]?.text = suitSymbol
        
        // 카드 숫자 설정
        if (card.isJoker) {
            if (card.suit == CardSuit.JOKER || card.rank == CardRank.JOKER) {
                cardRankViews[index]?.text = "조커"
            } else {
                cardRankViews[index]?.text = card.rank.getName()
            }
        } else {
            cardRankViews[index]?.text = card.rank.getName()
        }
        
        // 카드 색상 설정
        val textColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND || card.suit == CardSuit.JOKER) {
            Color.RED
        } else {
            Color.BLACK
        }
        cardSuitViews[index]?.setTextColor(textColor)
        cardRankViews[index]?.setTextColor(textColor)
        
        // 카드 배경색 설정 (일반적으로 흰색)
        cardViews[index]?.setCardBackgroundColor(Color.WHITE)
        
        // 카드 선택 여부 표시 유지
        if (card.isSelected) {
            cardViews[index]?.setCardBackgroundColor(Color.YELLOW)
        }
    }
    
    /**
     * 조커 카드 변경 후 포커 핸드 업데이트
     */
    private fun updatePokerHand() {
        // 웨이브 보상 모드에서만 족보 업데이트
        if (waveNumber > 0) {
            // 플레이어 핸드 업데이트 (PokerDeck 클래스의 playerHand 변수)
            pokerDeck.playerHand = currentCards.toMutableList()
            
            // 족보 평가 및 UI 업데이트
            updateHandDisplay()
        }
    }
} 