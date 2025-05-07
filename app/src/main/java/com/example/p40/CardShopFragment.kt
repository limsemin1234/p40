package com.example.p40

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.Card
import com.example.p40.game.ShopCard
import com.example.p40.game.MessageManager

class CardShopFragment : Fragment(R.layout.fragment_card_shop) {

    private lateinit var userManager: UserManager
    private lateinit var cardShopAdapter: CardShopAdapter
    private lateinit var shopCards: MutableList<ShopCard>
    private lateinit var tvCurrency: TextView
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 뷰 초기화
        tvCurrency = view.findViewById(R.id.tvCurrency)
        val rvCardShop = view.findViewById<RecyclerView>(R.id.rvCardShop)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        
        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // 현재 코인 표시
        updateCurrencyUI()
        
        // 카드 데이터 초기화
        initShopCards()
        
        // 리사이클러뷰 설정
        rvCardShop.layoutManager = GridLayoutManager(requireContext(), 1)
        cardShopAdapter = CardShopAdapter(shopCards) { card ->
            onCardBuyClicked(card)
        }
        rvCardShop.adapter = cardShopAdapter
    }
    
    // 상점 카드 초기화
    private fun initShopCards() {
        // 기본 상점 카드 가져오기
        shopCards = ShopCard.getDefaultShopCards().toMutableList()
        
        // 이미 구매한 카드 확인
        for (card in shopCards) {
            if (userManager.hasCard(card.suit, true)) {
                card.isPurchased = true
            }
        }
    }
    
    // 카드 구매 버튼 클릭 처리
    private fun onCardBuyClicked(card: ShopCard) {
        // 이미 구매한 카드인지 확인
        if (card.isPurchased) {
            MessageManager.getInstance().showInfo(requireContext(), "이미 구매한 카드입니다.")
            return
        }
        
        // 구매 확인 다이얼로그
        AlertDialog.Builder(requireContext())
            .setTitle("카드 구매")
            .setMessage("${card.name}을(를) ${card.price} 코인에 구매하시겠습니까?")
            .setPositiveButton("구매") { _, _ ->
                purchaseCard(card)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    // 카드 구매 처리
    private fun purchaseCard(card: ShopCard) {
        // 코인 차감
        if (userManager.useCoin(card.price)) {
            // 구매한 카드를 유저 데이터에 추가
            val purchasedCard = card.toCard()
            userManager.addPurchasedCard(purchasedCard)
            
            // UI 업데이트
            card.isPurchased = true
            cardShopAdapter.updateCardPurchased(card.id)
            updateCurrencyUI()
            
            MessageManager.getInstance().showSuccess(requireContext(), "${card.name} 구매 완료!")
        } else {
            // 코인 부족
            MessageManager.getInstance().showError(requireContext(), "코인이 부족합니다.")
        }
    }
    
    // 코인 표시 UI 업데이트
    private fun updateCurrencyUI() {
        tvCurrency.text = "코인: ${userManager.getCoin()}"
    }
} 