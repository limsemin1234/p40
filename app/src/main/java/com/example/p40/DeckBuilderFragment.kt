package com.example.p40

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.TextView

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.CardUtils
import com.example.p40.game.PokerDeck
import com.example.p40.game.MessageManager
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
    private lateinit var tvCoinAmount: TextView
    
    // 유저 매니저
    private lateinit var userManager: UserManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 유저 매니저 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 뷰 초기화
        initViews(view)
        
        // 코인 정보 업데이트
        updateCoinUI()
        
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
        
        // 코인 정보 표시 텍스트뷰
        tvCoinAmount = view.findViewById(R.id.tvCoinAmount)
        
        // 버튼 초기화
        btnSaveDeck = view.findViewById(R.id.btnSaveDeck)
    }
    
    private fun setupButtons() {
        // 저장 버튼 설정
        btnSaveDeck.setOnClickListener {
            saveDeck()
        }
        
        // 뒤로가기 버튼 설정
        view?.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
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
        
        // 카드 삭제 버튼 설정
        view?.findViewById<Button>(R.id.btnDeleteCard)?.setOnClickListener {
            showDeleteCardDialog()
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
                // 롱클릭 기능 비활성화
                false
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
                // 롱클릭 기능 비활성화
                false
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
        
        // 카드 정렬
        sortDeckCards()
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        updateDeckCount()
    }
    
    /**
     * 컬렉션에서 덱으로 카드 추가
     */
    private fun addCardToDeck(card: Card) {
        // 덱 최대 크기 체크 (55장)
        if (deckCards.size >= 55) {
            MessageManager.getInstance().showInfo(requireContext(), "덱에 최대 55장까지만 넣을 수 있습니다.")
            return
        }
        
        // 해당 카드가 이미 덱에 있는지 확인
        if (isCardInDeck(card)) {
            MessageManager.getInstance().showInfo(requireContext(), "이미 덱에 있는 카드입니다.")
            return
        }
        
        // 덱에 카드 추가
        deckCards.add(card)
        
        // 카드 정렬
        sortDeckCards()
        
        // 덱 UI 갱신 (전체 갱신)
        deckAdapter.notifyDataSetChanged()
        
        // 컬렉션에서 카드 제거
        val index = collectionCards.indexOfFirst { it.suit == card.suit && it.rank == card.rank }
        if (index != -1) {
            collectionCards.removeAt(index)
            
            // 컬렉션 정렬
            sortCollectionCards()
            
            // 컬렉션 UI 갱신 (전체 갱신)
            collectionAdapter.notifyDataSetChanged()
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
            
            // 덱 정렬
            sortDeckCards()
            
            // 덱 UI 갱신
            deckAdapter.notifyDataSetChanged()
            
            // 컬렉션에 카드 추가
            collectionCards.add(card)
            
            // 컬렉션 정렬
            sortCollectionCards()
            
            // 컬렉션 UI 갱신
            collectionAdapter.notifyDataSetChanged()
            
            // 카드 수량 업데이트
            updateDeckCount()
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        }
    }
    
    /**
     * 덱 카드 정렬 함수: 무늬(스페이드, 하트, 다이아, 클로버) 순으로 정렬하고, 같은 무늬 내에서는 숫자 순으로 정렬
     */
    private fun sortDeckCards() {
        deckCards.sortWith(compareBy<Card> { 
            when (it.suit) {
                CardSuit.SPADE -> 0
                CardSuit.HEART -> 1
                CardSuit.DIAMOND -> 2
                CardSuit.CLUB -> 3
                CardSuit.JOKER -> 4
            }
        }.thenBy { it.rank.value })
    }
    
    /**
     * 컬렉션 카드 정렬 함수: 무늬(스페이드, 하트, 다이아, 클로버) 순으로 정렬하고, 같은 무늬 내에서는 숫자 순으로 정렬
     */
    private fun sortCollectionCards() {
        collectionCards.sortWith(compareBy<Card> { 
            when (it.suit) {
                CardSuit.SPADE -> 0
                CardSuit.HEART -> 1
                CardSuit.DIAMOND -> 2
                CardSuit.CLUB -> 3
                CardSuit.JOKER -> 4
            }
        }.thenBy { it.rank.value })
    }
    
    /**
     * 덱 카드 수량 업데이트
     */
    private fun updateDeckCount() {
        tvDeckCount.text = "덱 구성: ${deckCards.size}장/55장"
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
            
            // 덱 카드 정렬
            sortDeckCards()
            
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
                
                // 컬렉션 카드 정렬
                sortCollectionCards()
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
        if (deckCards.size < 40) {
            MessageManager.getInstance().showInfo(requireContext(), "최소 40장 이상의 카드로 덱을 구성해야 합니다.")
            return
        }
        
        // 최대 덱 크기 체크
        if (deckCards.size > 55) {
            MessageManager.getInstance().showInfo(requireContext(), "최대 55장까지의 카드로 덱을 구성해야 합니다.")
            return
        }
        
        // 덱 저장 처리 (기존 autoSaveDeckAndCollection() 함수 활용)
        autoSaveDeckAndCollection()
        
        MessageManager.getInstance().showInfo(requireContext(), "덱이 저장되었습니다. (${deckCards.size}장)")
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
        
        // 컬렉션 정렬
        sortCollectionCards()
        
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
     * 선택한 카드들을 덱에 추가
     */
    private fun addSelectedCards() {
        val selectedCards = collectionAdapter.getSelectedCards()
        
        if (selectedCards.isEmpty()) {
            MessageManager.getInstance().showInfo(requireContext(), "추가할 카드를 선택해주세요")
            return
        }
        
        var addedCount = 0
        
        // 덱 최대 크기 체크
        if (deckCards.size + selectedCards.size > 55) {
            MessageManager.getInstance().showInfo(requireContext(), "덱에 최대 55장까지만 넣을 수 있습니다.")
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
        
        // 카드 정렬
        sortDeckCards()
        sortCollectionCards()
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        collectionAdapter.notifyDataSetChanged()
        collectionAdapter.clearSelections()
        
        // 카드 수량 업데이트
        updateDeckCount()
        
        // 결과 안내 메시지
        if (addedCount > 0) {
            MessageManager.getInstance().showInfo(requireContext(), "${addedCount}장의 카드를 덱에 추가했습니다.")
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        } else {
            MessageManager.getInstance().showInfo(requireContext(), "추가할 수 있는 카드가 없습니다.")
        }
    }
    
    /**
     * 선택한 카드들을 덱에서 제거
     */
    private fun removeSelectedCards() {
        val selectedCards = deckAdapter.getSelectedCards()
        
        if (selectedCards.isEmpty()) {
            MessageManager.getInstance().showInfo(requireContext(), "제거할 카드를 선택해주세요")
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
        
        // 카드 정렬
        sortDeckCards()
        sortCollectionCards()
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        collectionAdapter.notifyDataSetChanged()
        deckAdapter.clearSelections()
        
        // 카드 수량 업데이트
        updateDeckCount()
        
        // 결과 안내 메시지
        if (removedCount > 0) {
            MessageManager.getInstance().showInfo(requireContext(), "${removedCount}장의 카드를 덱에서 제거했습니다.")
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
        } else {
            MessageManager.getInstance().showInfo(requireContext(), "제거할 수 있는 카드가 없습니다.")
        }
    }
    
    /**
     * 카드 삭제 다이얼로그 표시
     */
    private fun showDeleteCardDialog() {
        // 선택된 카드 가져오기
        val selectedDeckCards = deckAdapter.getSelectedCards()
        val selectedCollectionCards = collectionAdapter.getSelectedCards()
        
        // 선택된 카드가 없는 경우
        if (selectedDeckCards.isEmpty() && selectedCollectionCards.isEmpty()) {
            MessageManager.getInstance().showInfo(requireContext(), "삭제할 카드를 선택해주세요")
            return
        }
        
        // 선택된 카드 총 개수
        val totalSelectedCards = selectedDeckCards.size + selectedCollectionCards.size
        
        // 확인 다이얼로그 생성
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("카드 삭제")
        builder.setMessage("선택한 ${totalSelectedCards}장의 카드를 영구적으로 삭제하시겠습니까?\n삭제된 카드는 복구할 수 없습니다.")
        
        // 확인 버튼
        builder.setPositiveButton("삭제") { dialog, _ ->
            // 덱에서 선택된 카드 삭제
            if (selectedDeckCards.isNotEmpty()) {
                deleteCardsFromDeck(selectedDeckCards)
            }
            
            // 컬렉션에서 선택된 카드 삭제
            if (selectedCollectionCards.isNotEmpty()) {
                deleteCardsFromCollection(selectedCollectionCards)
            }
            
            // 다이얼로그 닫기
            dialog.dismiss()
            
            // 변경사항 자동 저장
            autoSaveDeckAndCollection()
            
            // 카드 수량 업데이트
            updateDeckCount()
            
            // 결과 안내 메시지
            MessageManager.getInstance().showInfo(requireContext(), "${totalSelectedCards}장의 카드가 삭제되었습니다.")
        }
        
        // 취소 버튼
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        
        // 다이얼로그 표시
        builder.show()
    }
    
    /**
     * 덱에서 카드 삭제
     */
    private fun deleteCardsFromDeck(cardsToDelete: List<Card>) {
        // 카드 하나씩 삭제
        for (card in cardsToDelete) {
            // 덱에서 카드 찾기
            val index = deckCards.indexOfFirst { 
                it.suit == card.suit && it.rank == card.rank && it.isJoker == card.isJoker
            }
            
            // 덱에서 카드 제거
            if (index != -1) {
                deckCards.removeAt(index)
            }
        }
        
        // 덱 정렬
        sortDeckCards()
        
        // UI 갱신
        deckAdapter.notifyDataSetChanged()
        deckAdapter.clearSelections()
    }
    
    /**
     * 컬렉션에서 카드 삭제
     */
    private fun deleteCardsFromCollection(cardsToDelete: List<Card>) {
        // 카드 하나씩 삭제
        for (card in cardsToDelete) {
            // 컬렉션에서 카드 찾기
            val index = collectionCards.indexOfFirst { 
                it.suit == card.suit && it.rank == card.rank && it.isJoker == card.isJoker
            }
            
            // 컬렉션에서 카드 제거
            if (index != -1) {
                collectionCards.removeAt(index)
            }
        }
        
        // 컬렉션 정렬
        sortCollectionCards()
        
        // UI 갱신
        collectionAdapter.notifyDataSetChanged()
        collectionAdapter.clearSelections()
    }
    
    // 카드 데이터 직렬화를 위한 클래스
    data class CardData(val suit: String, val rank: String)
    
    // 코인 정보 업데이트
    private fun updateCoinUI() {
        tvCoinAmount.text = "코인: ${userManager.getCoin()}"
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 코인 정보 갱신
        updateCoinUI()
    }
    
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