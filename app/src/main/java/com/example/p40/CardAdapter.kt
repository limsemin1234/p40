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
    private val deckBuilderFragment: DeckBuilderFragment? = null,
    private val onCardLongClick: ((Card) -> Boolean)? = null
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    // 선택된 카드를 관리하기 위한 맵
    private val selectedCards = mutableMapOf<Card, Boolean>()
    
    // 다중 선택 모드 여부
    private var isMultiSelectMode = false
    
    // 선택된 카드 목록 반환
    fun getSelectedCards(): List<Card> {
        return selectedCards.filter { it.value }.keys.toList()
    }
    
    // 모든 카드 선택 상태 초기화
    fun clearSelections() {
        selectedCards.clear()
        isMultiSelectMode = false
        notifyDataSetChanged()
    }
    
    // 다중 선택 모드 설정
    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode != enabled) {
            isMultiSelectMode = enabled
            if (!enabled) {
                clearSelections()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        
        // 카드의 선택 상태 설정
        val isSelected = selectedCards[card] == true
        
        // 카드 데이터 및 선택 상태 바인딩
        holder.bind(card, isSelected)
        
        // 카드 클릭 이벤트 설정
        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) {
                // 다중 선택 모드: 선택 상태 토글
                toggleCardSelection(card)
                holder.updateSelectionState(selectedCards[card] == true)
            } else {
                // 단일 선택 모드: 기존 콜백 호출
                onCardClick(card)
            }
        }
        
        // 카드 롱클릭 이벤트 설정
        holder.itemView.setOnLongClickListener {
            if (onCardLongClick != null && onCardLongClick.invoke(card)) {
                // 롱클릭 핸들러에서 처리됨
                true
            } else if (!isMultiSelectMode) {
                // 다중 선택 모드 시작
                isMultiSelectMode = true
                toggleCardSelection(card)
                holder.updateSelectionState(selectedCards[card] == true)
                true
            } else {
                false
            }
        }
    }
    
    // 카드 선택 상태 토글
    private fun toggleCardSelection(card: Card) {
        val currentState = selectedCards[card] ?: false
        selectedCards[card] = !currentState
    }

    override fun getItemCount() = cards.size

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvCardRank: TextView = itemView.findViewById(R.id.tvCardRank)
        private val tvCardSuit: TextView = itemView.findViewById(R.id.tvCardSuit)
        private val tvNewLabel: TextView = itemView.findViewById(R.id.tvNewLabel)

        fun bind(card: Card, isSelected: Boolean) {
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
            val cardColor = if (card.suit == CardSuit.HEART || card.suit == CardSuit.DIAMOND || card.suit == CardSuit.JOKER) {
                Color.RED
            } else {
                Color.BLACK
            }
            tvCardSuit.setTextColor(cardColor)
            tvCardRank.setTextColor(cardColor)
            
            // 신규 카드 라벨 표시
            if (showNewLabel && deckBuilderFragment != null && deckBuilderFragment.isNewCard(card)) {
                tvNewLabel.visibility = View.VISIBLE
            } else {
                tvNewLabel.visibility = View.GONE
            }
            
            // 선택 상태 표시
            updateSelectionState(isSelected)
        }
        
        // 카드의 선택 상태 업데이트
        fun updateSelectionState(isSelected: Boolean) {
            if (isSelected) {
                cardView.setCardBackgroundColor(Color.parseColor("#FFD700")) // 골드 색상
                
                // 테두리 색상 변경 (MaterialCardView API를 사용하지 않고 대체 방법 사용)
                cardView.alpha = 0.85f // 약간 투명하게 만들어 선택 상태 표시
                
                // 스케일 애니메이션으로 선택 표시
                cardView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .start()
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.alpha = 1.0f
                
                // 원래 크기로 복원
                cardView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
        }
    }
} 