package com.example.p40

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.Card
import com.example.p40.game.CardSuit

/**
 * 덱 구성 화면에서 카드 리스트를 표시하기 위한 어댑터
 */
class CardAdapter(
    private val cards: List<Card>,
    private val onCardClick: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.bind(card)
        
        // 카드 클릭 이벤트 설정
        holder.itemView.setOnClickListener {
            onCardClick(card)
        }
    }

    override fun getItemCount() = cards.size

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvCardRank: TextView = itemView.findViewById(R.id.tvCardRank)
        private val tvCardSuit: TextView = itemView.findViewById(R.id.tvCardSuit)

        fun bind(card: Card) {
            // 카드 무늬 표시
            val suitSymbol = when (card.suit) {
                CardSuit.HEART -> "♥"
                CardSuit.DIAMOND -> "♦"
                CardSuit.CLUB -> "♣"
                CardSuit.SPADE -> "♠"
                CardSuit.JOKER -> "★"
            }
            tvCardSuit.text = suitSymbol

            // 카드 숫자 표시
            tvCardRank.text = card.rank.getName()

            // 카드 색상 설정
            val cardColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND || card.suit == CardSuit.JOKER) {
                Color.RED
            } else {
                Color.BLACK
            }
            tvCardSuit.setTextColor(cardColor)
            tvCardRank.setTextColor(cardColor)
        }
    }
} 