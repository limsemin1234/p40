package com.example.p40

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.Card
import com.example.p40.CardSuit
import com.example.p40.MessageManager
import com.example.p40.ShopCard

/**
 * 상점의 카드 탭 프래그먼트
 */
class CardTabFragment : Fragment() {

    private lateinit var userManager: UserManager
    private lateinit var cardShopAdapter: CardShopAdapter
    private val shopCards = mutableListOf<ShopCard>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop_tab, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 리사이클러뷰 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvShopItems)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // 카드 데이터 초기화
        initShopCards()
        
        // 어댑터 설정
        cardShopAdapter = CardShopAdapter(shopCards) { card ->
            onCardBuyClicked(card)
        }
        recyclerView.adapter = cardShopAdapter
    }
    
    // 상점 카드 초기화 - 골드 별 조커 카드 삭제
    private fun initShopCards() {
        // 기본 상점 카드만 가져오기 (삭제하지 않음)
        shopCards.addAll(ShopCard.getDefaultShopCards())
        
        // 골드 별 조커는 추가하지 않음 (삭제됨)
        
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
        
        // 모든 카드에 대해 작은 커스텀 다이얼로그 사용
        showSmallPurchaseDialog(card)
    }
    
    // 작은 커스텀 구매 다이얼로그 표시
    private fun showSmallPurchaseDialog(card: ShopCard) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_small_purchase)
        
        // 배경 투명하게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 제목과 메시지 설정
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        
        // 카드 종류에 따라 제목 설정
        val titleText = if (card.suit == CardSuit.JOKER) "별 조커 구매" else "카드 구매"
        tvTitle.text = titleText
        tvMessage.text = "${card.name}을(를) ${card.price} 코인에 구매하시겠습니까?"
        
        // 취소 버튼
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 구매 버튼
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            purchaseCard(card)
            dialog.dismiss()
        }
        
        dialog.show()
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
            
            // 상위 프래그먼트 코인 UI 업데이트
            (parentFragment as? CardShopFragment)?.updateCurrencyUI()
            
            MessageManager.getInstance().showSuccess(requireContext(), "${card.name} 구매 완료!")
        } else {
            // 코인 부족
            MessageManager.getInstance().showError(requireContext(), "코인이 부족합니다.")
        }
    }
} 