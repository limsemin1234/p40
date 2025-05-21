package com.example.p40

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.Card
import com.example.p40.CardSuit
import com.example.p40.CardRank

/**
 * 덱 구성 화면에서 카드 리스트를 표시하기 위한 어댑터
 */
class CardAdapter(
    private val cards: List<Card>,
    private val onCardClick: (Card) -> Unit,
    private val showNewLabel: Boolean = false,
    private val deckBuilderFragment: DeckBuilderFragment? = null
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        
        // 카드 데이터 바인딩
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
        private val tvNewLabel: TextView = itemView.findViewById(R.id.tvNewLabel)

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
            if (card.isJoker) {
                // 일반 별 조커
                if (card.suit == CardSuit.JOKER) {
                    tvCardRank.text = "조커"
                } 
                // 문양 조커 (랭크가 JOKER면 '조커', 아니면 해당 숫자 표시)
                else if (card.rank == CardRank.JOKER) {
                    tvCardRank.text = "조커"
                } else {
                    tvCardRank.text = card.rank.getName()
                }
            } else {
                tvCardRank.text = card.rank.getName()
            }

            // 카드 색상 설정
            val cardColor = when {
                card.suit == CardSuit.JOKER -> Color.parseColor("#FFD700") // 황금색(#FFD700)
                card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND -> Color.RED
                else -> Color.BLACK
            }
            tvCardSuit.setTextColor(cardColor)
            tvCardRank.setTextColor(cardColor)
            
            // 신규 카드 라벨 표시
            if (showNewLabel && deckBuilderFragment != null && deckBuilderFragment.isNewCard(card)) {
                tvNewLabel.visibility = View.VISIBLE
            } else {
                tvNewLabel.visibility = View.GONE
            }
            
            // 기본 배경색 설정
            cardView.setCardBackgroundColor(Color.WHITE)
        }
    }
} 