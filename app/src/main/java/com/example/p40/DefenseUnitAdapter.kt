package com.example.p40

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.ShopDefenseUnit

class DefenseUnitAdapter(
    private val defenseUnits: List<ShopDefenseUnit>,
    private val onBuyUnit: (ShopDefenseUnit) -> Unit
) : RecyclerView.Adapter<DefenseUnitAdapter.UnitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_card, parent, false) // 재사용
        return UnitViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        val unit = defenseUnits[position]
        holder.bind(unit)
    }

    override fun getItemCount(): Int = defenseUnits.size

    inner class UnitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCardSuit: TextView = itemView.findViewById(R.id.tvCardSuit)
        private val tvCardType: TextView = itemView.findViewById(R.id.tvCardType)
        private val tvCardName: TextView = itemView.findViewById(R.id.tvCardName)
        private val tvCardDesc: TextView = itemView.findViewById(R.id.tvCardDesc)
        private val tvCardPrice: TextView = itemView.findViewById(R.id.tvCardPrice)
        private val tvNewLabel: TextView = itemView.findViewById(R.id.tvNewLabel)
        private val btnBuyCard: Button = itemView.findViewById(R.id.btnBuyCard)

        fun bind(unit: ShopDefenseUnit) {
            // 유닛 아이콘 (문양 대신 숫자로 표시)
            tvCardSuit.text = "${unit.id}"
            tvCardSuit.setTextColor(android.graphics.Color.WHITE)
            
            // 유닛 타입 설정
            tvCardType.text = "디펜스유닛"
            
            // 유닛 이름 및 설명 설정
            tvCardName.text = unit.name
            tvCardDesc.text = "${unit.description}\n공격력: ${unit.damage}, 사거리: ${unit.range}, 공격속도: ${unit.attackSpeed}초"
            
            // 유닛 가격 설정
            tvCardPrice.text = "가격: ${unit.price} 코인"
            
            // 신규 유닛 라벨 표시
            tvNewLabel.visibility = if (unit.isNew) View.VISIBLE else View.GONE
            
            // 구매 버튼 상태 설정
            if (unit.isPurchased) {
                btnBuyCard.text = "구매완료"
                btnBuyCard.isEnabled = false
                btnBuyCard.alpha = 0.5f
            } else {
                btnBuyCard.text = "구매"
                btnBuyCard.isEnabled = true
                btnBuyCard.alpha = 1.0f
                
                // 구매 버튼 이벤트
                btnBuyCard.setOnClickListener {
                    onBuyUnit(unit)
                }
            }
        }
    }
    
    // 유닛 구매 완료 표시 업데이트
    fun updateUnitPurchased(unitId: Int) {
        val position = defenseUnits.indexOfFirst { it.id == unitId }
        if (position != -1) {
            defenseUnits[position].isPurchased = true
            notifyItemChanged(position)
        }
    }
} 