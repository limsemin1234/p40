package com.example.p40

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.PokerDeck
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 덱 구성 화면 프래그먼트
 */
class DeckBuilderFragment : Fragment(R.layout.fragment_deck_builder) {

    // 리사이클러뷰 어댑터
    private lateinit var deckAdapter: CardAdapter
    private lateinit var collectionAdapter: CardAdapter
    
    // 카드 데이터
    private val deckCards = mutableListOf<Card>() // 현재 덱에 있는 카드들
    private val collectionCards = mutableListOf<Card>() // 보유한 카드 컬렉션
    
    // 새로운 카드 목록 (NEW 표시용)
    private var newCards = mutableListOf<Card>()
    
    // 뷰 참조
    private lateinit var tvDeckCount: TextView
    private lateinit var btnSaveDeck: Button
    private lateinit var btnBack: Button
    
    // 유저 매니저
    private lateinit var userManager: UserManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 유저 매니저 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 뷰 초기화
        initViews(view)
        
        // 어댑터 생성 및 설정
        setupAdapters()
        
        // 저장된 덱과 컬렉션 불러오기 시도 또는 기본 덱 로드
        if (!loadSavedDeck()) {
            loadDefaultDeck()
            // 컬렉션이 비어있으면 조커 카드 추가
            if (collectionCards.isEmpty()) {
                addJokerToCollection()
            }
        }
        
        // 구매한 카드 로드 및 적용
        loadPurchasedCards()
        
        // 버튼 설정
        setupButtons()
        
        // 뒤로가기 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        })
    }
    
    private fun initViews(view: View) {
        // 덱 카드 리사이클러뷰 설정
        val rvDeck = view.findViewById<RecyclerView>(R.id.rvDeck)
        rvDeck.layoutManager = GridLayoutManager(requireContext(), 7) // 7열 그리드
        
        // 카드 컬렉션 리사이클러뷰 설정
        val rvCollection = view.findViewById<RecyclerView>(R.id.rvCollection)
        rvCollection.layoutManager = GridLayoutManager(requireContext(), 7) // 7열 그리드
        
        // 카드 수량 표시 텍스트뷰
        tvDeckCount = view.findViewById(R.id.tvDeckCount)
        
        // 버튼 초기화
        btnSaveDeck = view.findViewById(R.id.btnSaveDeck)
        btnBack = view.findViewById(R.id.btnBack)
    }
    
    private fun setupButtons() {
        // 저장 버튼 설정
        btnSaveDeck.setOnClickListener {
            saveDeck()
        }
        
        // 뒤로가기 버튼 설정
        btnBack.setOnClickListener {
            navigateBack()
        }
    }
    
    private fun navigateBack() {
        // 뒤로 가기 전에 현재 덱과 컬렉션 상태 저장
        autoSaveDeckAndCollection()
        findNavController().popBackStack()
    }
    
    /**
     * 덱과 컬렉션을 자동 저장하는 함수
     */
    private fun autoSaveDeckAndCollection() {
        // 덱 데이터 준비
        val cardDataList = deckCards.map { card ->
            CardData(card.suit.name, card.rank.name)
        }
        
        // 컬렉션 카드 준비
        val collectionDataList = collectionCards.map { card ->
            CardData(card.suit.name, card.rank.name)
        }
        
        val gson = Gson()
        val deckJson = gson.toJson(cardDataList)
        val collectionJson = gson.toJson(collectionDataList)
        
        // SharedPreferences에 저장
        val sharedPrefs = requireActivity().getSharedPreferences(DECK_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString(DECK_KEY, deckJson)
            .putString(COLLECTION_KEY, collectionJson)
            .apply()
    }
    
    private fun setupAdapters() {
        // 덱 어댑터 설정
        deckAdapter = CardAdapter(
            cards = deckCards,
            onCardClick = { card ->
                // 덱에서 카드 제거하여 컬렉션으로 이동
                removeCardFromDeck(card)
            },
            onCardLongClick = { card ->
                // 덱에서 카드 롱클릭 - 조커인 경우 숫자 선택 다이얼로그 표시
                if (card.isJoker && card.suit != CardSuit.JOKER) {
                    showJokerNumberPickerDialog(card, true)
                    true
                } else {
                    false
                }
            }
        )
        
        // 컬렉션 어댑터 설정 - 신규 카드 표시 활성화
        collectionAdapter = CardAdapter(
            cards = collectionCards,
            onCardClick = { card ->
                // 컬렉션에서 카드 선택하여 덱에 추가
                addCardToDeck(card)
            },
            showNewLabel = true,
            deckBuilderFragment = this,
            onCardLongClick = { card ->
                // 컬렉션에서 카드 롱클릭 - 조커인 경우 숫자 선택 다이얼로그 표시
                if (card.isJoker && card.suit != CardSuit.JOKER) {
                    showJokerNumberPickerDialog(card, false)
                    true
                } else {
                    false
                }
            }
        )
        
        // RecyclerView에 어댑터 연결
        view?.findViewById<RecyclerView>(R.id.rvDeck)?.adapter = deckAdapter
        view?.findViewById<RecyclerView>(R.id.rvCollection)?.adapter = collectionAdapter
        
        // 카드 수량 업데이트
        updateDeckCount()
    }
    
    /**
     * 기본 덱(52장의 포커 카드) 로드
     */
    private fun loadDefaultDeck() {
        // 초기 덱은 비어있다고 가정
        deckCards.clear()
        
        // 기본 덱 적용 - 52장의 카드 모두 추가
        CardSuit.values().forEach { suit ->
            if (suit != CardSuit.JOKER) {
                CardRank.values().forEach { rank ->
                    if (rank != CardRank.JOKER) {
                        deckCards.add(Card(suit, rank))
                    }
                }
            }
        }
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        updateDeckCount()
    }
    
    /**
     * 컬렉션에 조커 카드 추가
     */
    private fun addJokerToCollection() {
        val jokerCard = Card.createJoker()
        if (!isCardInDeck(jokerCard)) {
            collectionCards.add(jokerCard)
            collectionAdapter.notifyDataSetChanged()
        }
    }
    
    /**
     * 카드가 이미 덱에 있는지 확인
     */
    private fun isCardInDeck(card: Card): Boolean {
        return deckCards.any { it.suit == card.suit && it.rank == card.rank }
    }
    
    /**
     * 컬렉션에서 덱으로 카드 추가
     */
    private fun addCardToDeck(card: Card) {
        // 덱 최대 크기 체크 (52장 + 조커)
        if (deckCards.size >= 53) {
            Toast.makeText(requireContext(), "덱에 최대 53장까지만 넣을 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 해당 카드가 이미 덱에 있는지 확인
        if (isCardInDeck(card)) {
            Toast.makeText(requireContext(), "이미 덱에 있는 카드입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 덱에 카드 추가
        deckCards.add(card)
        deckAdapter.notifyItemInserted(deckCards.size - 1)
        
        // 컬렉션에서 카드 제거
        val index = collectionCards.indexOfFirst { it.suit == card.suit && it.rank == card.rank }
        if (index != -1) {
            collectionCards.removeAt(index)
            collectionAdapter.notifyItemRemoved(index)
        }
        
        // 카드 수량 업데이트
        updateDeckCount()
        
        // 변경사항 자동 저장
        autoSaveDeckAndCollection()
    }
    
    /**
     * 덱에서 카드 제거
     */
    private fun removeCardFromDeck(card: Card) {
        // 덱에서 카드 제거
        val index = deckCards.indexOfFirst { it.suit == card.suit && it.rank == card.rank }
        if (index != -1) {
            deckCards.removeAt(index)
            deckAdapter.notifyItemRemoved(index)
            
            // 컬렉션에 카드 추가
            collectionCards.add(card)
            collectionAdapter.notifyItemInserted(collectionCards.size - 1)
            
            // 카드 수량 업데이트
            updateDeckCount()
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        }
    }
    
    /**
     * 덱 카드 수량 업데이트
     */
    private fun updateDeckCount() {
        tvDeckCount.text = "덱 구성: ${deckCards.size}장/53장"
    }
    
    /**
     * 저장된 덱과 컬렉션 불러오기
     */
    private fun loadSavedDeck(): Boolean {
        val sharedPrefs = requireActivity().getSharedPreferences(DECK_PREFS_NAME, Context.MODE_PRIVATE)
        val deckJson = sharedPrefs.getString(DECK_KEY, null) ?: return false
        
        try {
            val gson = Gson()
            val type = object : TypeToken<List<CardData>>() {}.type
            val savedCards = gson.fromJson<List<CardData>>(deckJson, type)
            
            deckCards.clear()
            savedCards.forEach { cardData ->
                val suit = CardSuit.valueOf(cardData.suit)
                val rank = CardRank.valueOf(cardData.rank)
                deckCards.add(Card(suit, rank))
            }
            
            // 저장된 컬렉션 카드도 불러오기
            val collectionJson = sharedPrefs.getString(COLLECTION_KEY, null)
            collectionCards.clear() // 컬렉션 초기화
            
            if (collectionJson != null) {
                val savedCollection = gson.fromJson<List<CardData>>(collectionJson, type)
                
                savedCollection.forEach { cardData ->
                    val suit = CardSuit.valueOf(cardData.suit)
                    val rank = CardRank.valueOf(cardData.rank)
                    collectionCards.add(Card(suit, rank))
                }
            }
            
            // 컬렉션이 비어있으면 조커 카드 추가
            if (collectionCards.isEmpty()) {
                addJokerToCollection()
            }
            
            // UI 갱신
            deckAdapter.notifyDataSetChanged()
            collectionAdapter.notifyDataSetChanged()
            updateDeckCount()
            return true
        } catch (e: Exception) {
            // 오류 발생 시 기본 덱 사용
            return false
        }
    }
    
    /**
     * 덱 저장
     */
    private fun saveDeck() {
        // 최소 덱 크기 체크
        if (deckCards.size < 20) {
            Toast.makeText(requireContext(), "최소 20장 이상의 카드로 덱을 구성해야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 덱 저장 처리 (기존 autoSaveDeckAndCollection() 함수 활용)
        autoSaveDeckAndCollection()
        
        Toast.makeText(requireContext(), "덱이 저장되었습니다. (${deckCards.size}장)", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 구매한 카드 불러오고 덱 컬렉션에 추가
     */
    private fun loadPurchasedCards() {
        // 구매한 카드 목록 가져오기
        val purchasedCards = userManager.getPurchasedCards()
        
        // 최근 구매한 카드 목록 (NEW 표시용)
        newCards = userManager.getRecentPurchasedCards().toMutableList()
        
        // 구매한 카드 각각 처리
        for (card in purchasedCards) {
            // 덱에 없고 컬렉션에도 없을 경우만 추가
            if (!isCardInDeck(card) && !isCardInCollection(card)) {
                collectionCards.add(card)
            }
        }
        
        // 컬렉션 어댑터 갱신
        collectionAdapter.notifyDataSetChanged()
    }
    
    /**
     * 카드가 이미 컬렉션에 있는지 확인
     */
    private fun isCardInCollection(card: Card): Boolean {
        return collectionCards.any { 
            it.suit == card.suit && 
            it.rank == card.rank && 
            it.isJoker == card.isJoker
        }
    }
    
    /**
     * 카드가 최근 구매한 카드(NEW)인지 확인
     */
    fun isNewCard(card: Card): Boolean {
        return newCards.any { 
            it.suit == card.suit && 
            it.rank == card.rank && 
            it.isJoker == card.isJoker
        }
    }
    
    /**
     * 조커 카드의 숫자를 선택하는 다이얼로그 표시
     */
    private fun showJokerNumberPickerDialog(card: Card, isInDeck: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_joker_number_picker)
        
        // 윈도우 크기 조정
        val window = dialog.window
        if (window != null) {
            val displayMetrics = requireContext().resources.displayMetrics
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
            android.graphics.Color.RED
        } else {
            android.graphics.Color.BLACK
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
            numberPicker.value = card.rank.value
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
            replaceJokerCard(card, selectedRank, isInDeck)
            dialog.dismiss()
            
            // 변경 알림
            Toast.makeText(
                requireContext(),
                "${card.suit.getName()} 조커를 ${selectedRank.getName()}으로 변환했습니다", 
                Toast.LENGTH_SHORT
            ).show()
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
    private fun replaceJokerCard(card: Card, newRank: CardRank, isInDeck: Boolean) {
        val newCard = Card(
            suit = card.suit,
            rank = newRank,
            isJoker = true
        )
        
        if (isInDeck) {
            // 덱에서 카드 교체
            val index = deckCards.indexOfFirst { 
                it.suit == card.suit && it.isJoker && 
                (it.rank == CardRank.JOKER || it.rank == card.rank) 
            }
            if (index != -1) {
                deckCards[index] = newCard
                deckAdapter.notifyItemChanged(index)
            }
        } else {
            // 컬렉션에서 카드 교체
            val index = collectionCards.indexOfFirst { 
                it.suit == card.suit && it.isJoker && 
                (it.rank == CardRank.JOKER || it.rank == card.rank) 
            }
            if (index != -1) {
                collectionCards[index] = newCard
                collectionAdapter.notifyItemChanged(index)
            }
        }
        
        // 변경사항 저장
        autoSaveDeckAndCollection()
    }
    
    // 카드 데이터 직렬화를 위한 클래스
    data class CardData(val suit: String, val rank: String)
    
    companion object {
        const val DECK_PREFS_NAME = "deck_preferences"
        const val DECK_KEY = "saved_deck"
        const val COLLECTION_KEY = "saved_collection"
        
        // 덱 데이터를 로드하는 정적 메서드 - 다른 화면에서 사용
        fun loadDeckFromPrefs(context: Context): List<Card>? {
            val sharedPrefs = context.getSharedPreferences(DECK_PREFS_NAME, Context.MODE_PRIVATE)
            val deckJson = sharedPrefs.getString(DECK_KEY, null) ?: return null
            
            try {
                val gson = Gson()
                val type = object : TypeToken<List<CardData>>() {}.type
                val savedCards = gson.fromJson<List<CardData>>(deckJson, type)
                
                return savedCards.map { cardData ->
                    val suit = CardSuit.valueOf(cardData.suit)
                    val rank = CardRank.valueOf(cardData.rank)
                    Card(suit, rank)
                }
            } catch (e: Exception) {
                return null
            }
        }
    }
} 