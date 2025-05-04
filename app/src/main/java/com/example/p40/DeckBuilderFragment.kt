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
import com.example.p40.game.CardUtils
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
            // 기본 게임 시작 시 조커 카드는 더 이상 자동 추가되지 않음
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
        
        // 선택한 카드 추가 버튼 설정
        view?.findViewById<Button>(R.id.btnAddSelected)?.setOnClickListener {
            addSelectedCards()
        }
        
        // 선택한 카드 제거 버튼 설정
        view?.findViewById<Button>(R.id.btnRemoveSelected)?.setOnClickListener {
            removeSelectedCards()
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
                // 덱에서 카드 롱클릭 - 별 조커인 경우만 처리
                if (CardUtils.isStarJoker(card)) {
                    showJokerSelectionDialog(card, true)
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
                // 컬렉션에서 카드 롱클릭 - 별 조커인 경우만 처리
                if (CardUtils.isStarJoker(card)) {
                    showJokerSelectionDialog(card, false)
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
            
            // 기본 게임 시작 시 조커 카드는 더 이상 자동 추가되지 않음
            
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
     * 카드가 이미 덱에 있는지 확인
     */
    private fun isCardInDeck(card: Card): Boolean {
        return deckCards.any { it.suit == card.suit && it.rank == card.rank }
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
     * 별 조커 카드 선택 다이얼로그 표시
     */
    private fun showJokerSelectionDialog(card: Card, isInDeck: Boolean) {
        // 조커 선택 다이얼로그 생성 및 표시
        val jokerDialog = com.example.p40.game.JokerSelectionDialog(requireContext()) { selectedCard ->
            // 선택된 카드로 조커 카드 대체
            replaceJokerCard(card, selectedCard.suit, selectedCard.rank, isInDeck)
            
            // 토스트 메시지
            Toast.makeText(
                requireContext(),
                "조커 카드가 ${selectedCard.suit.getName()} ${selectedCard.rank.getName()}(으)로 변환되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        jokerDialog.show()
    }
    
    /**
     * 조커 카드 변환 처리
     */
    private fun replaceJokerCard(card: Card, newSuit: CardSuit, newRank: CardRank, isInDeck: Boolean) {
        // 새 카드 생성
        val newCard = Card(
            suit = newSuit,
            rank = newRank,
            isSelected = false,
            isJoker = true
        )
        
        if (isInDeck) {
            // 덱에서 카드 교체
            val index = deckCards.indexOf(card)
            if (index != -1) {
                deckCards[index] = newCard
                deckAdapter.notifyItemChanged(index)
            }
        } else {
            // 컬렉션에서 카드 교체
            val index = collectionCards.indexOf(card)
            if (index != -1) {
                collectionCards[index] = newCard
                collectionAdapter.notifyItemChanged(index)
            }
        }
        
        // 변경사항 저장
        autoSaveDeckAndCollection()
    }
    
    /**
     * 선택한 카드들을 덱에 추가
     */
    private fun addSelectedCards() {
        val selectedCards = collectionAdapter.getSelectedCards()
        
        if (selectedCards.isEmpty()) {
            Toast.makeText(requireContext(), "추가할 카드를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        var addedCount = 0
        
        // 덱 최대 크기 체크
        if (deckCards.size + selectedCards.size > 53) {
            Toast.makeText(requireContext(), "덱에 최대 53장까지만 넣을 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 선택한 카드들 처리
        for (card in selectedCards) {
            // 이미 덱에 있는 카드인지 확인
            if (isCardInDeck(card)) {
                continue
            }
            
            // 덱에 카드 추가
            deckCards.add(Card(card.suit, card.rank, isJoker = card.isJoker))
            
            // 컬렉션에서 해당 카드 찾기
            val index = collectionCards.indexOfFirst { 
                it.suit == card.suit && it.rank == card.rank && it.isJoker == card.isJoker
            }
            
            // 컬렉션에서 카드 제거
            if (index != -1) {
                collectionCards.removeAt(index)
                addedCount++
            }
        }
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        collectionAdapter.notifyDataSetChanged()
        collectionAdapter.clearSelections()
        
        // 카드 수량 업데이트
        updateDeckCount()
        
        // 결과 안내 메시지
        if (addedCount > 0) {
            Toast.makeText(requireContext(), "${addedCount}장의 카드를 덱에 추가했습니다.", Toast.LENGTH_SHORT).show()
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        } else {
            Toast.makeText(requireContext(), "추가할 수 있는 카드가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 선택한 카드들을 덱에서 제거
     */
    private fun removeSelectedCards() {
        val selectedCards = deckAdapter.getSelectedCards()
        
        if (selectedCards.isEmpty()) {
            Toast.makeText(requireContext(), "제거할 카드를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        var removedCount = 0
        
        // 선택된 카드들을 임시 리스트에 복사 (인덱스 변경 방지)
        val cardsToRemove = selectedCards.toList()
        
        // 선택한 카드들 처리
        for (card in cardsToRemove) {
            // 덱에서 카드 찾기
            val index = deckCards.indexOfFirst { 
                it.suit == card.suit && it.rank == card.rank && it.isJoker == card.isJoker
            }
            
            // 덱에서 카드 제거
            if (index != -1) {
                val removedCard = deckCards.removeAt(index)
                
                // 컬렉션에 카드 추가
                collectionCards.add(Card(
                    removedCard.suit, 
                    removedCard.rank, 
                    isJoker = removedCard.isJoker
                ))
                
                removedCount++
            }
        }
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        collectionAdapter.notifyDataSetChanged()
        deckAdapter.clearSelections()
        
        // 카드 수량 업데이트
        updateDeckCount()
        
        // 결과 안내 메시지
        if (removedCount > 0) {
            Toast.makeText(requireContext(), "${removedCount}장의 카드를 덱에서 제거했습니다.", Toast.LENGTH_SHORT).show()
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        } else {
            Toast.makeText(requireContext(), "제거할 수 있는 카드가 없습니다.", Toast.LENGTH_SHORT).show()
        }
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