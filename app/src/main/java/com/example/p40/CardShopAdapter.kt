package com.example.p40

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.ShopCard

class CardShopAdapter(
    private val shopCards: List<ShopCard>,
    private val onBuyCard: (ShopCard) -> Unit
) : RecyclerView.Adapter<CardShopAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = shopCards[position]
        holder.bind(card)
    }

    override fun getItemCount(): Int = shopCards.size

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCardSuit: TextView = itemView.findViewById(R.id.tvCardSuit)
        private val tvCardType: TextView = itemView.findViewById(R.id.tvCardType)
        private val tvCardName: TextView = itemView.findViewById(R.id.tvCardName)
        private val tvCardDesc: TextView = itemView.findViewById(R.id.tvCardDesc)
        private val tvCardPrice: TextView = itemView.findViewById(R.id.tvCardPrice)
        private val tvNewLabel: TextView = itemView.findViewById(R.id.tvNewLabel)
        private val btnBuyCard: Button = itemView.findViewById(R.id.btnBuyCard)

        fun bind(card: ShopCard) {
            // 카드 문양 설정
            tvCardSuit.text = card.getSuitSymbol()
            tvCardSuit.setTextColor(card.getSuitColor())
            
            // 카드 타입 설정
            tvCardType.text = "조커"
            
            // 카드 이름 및 설명 설정
            tvCardName.text = card.name
            tvCardDesc.text = card.description
            
            // 카드 가격 설정
            tvCardPrice.text = "가격: ${card.price} 코인"
            
            // 신규 카드 라벨 표시
            tvNewLabel.visibility = if (card.isNew) View.VISIBLE else View.GONE
            
            // 구매 버튼 상태 설정
            if (card.isPurchased) {
                btnBuyCard.text = "구매완료"
                btnBuyCard.isEnabled = false
                btnBuyCard.alpha = 0.5f
            } else {
                btnBuyCard.text = "구매"
                btnBuyCard.isEnabled = true
                btnBuyCard.alpha = 1.0f
                
                // 구매 버튼 이벤트
                btnBuyCard.setOnClickListener {
                    onBuyCard(card)
                }
            }
        }
    }
    
    // 카드 구매 완료 표시 업데이트
    fun updateCardPurchased(cardId: Int) {
        val position = shopCards.indexOfFirst { it.id == cardId }
        if (position != -1) {
            shopCards[position].isPurchased = true
            notifyItemChanged(position)
        }
    }
} 