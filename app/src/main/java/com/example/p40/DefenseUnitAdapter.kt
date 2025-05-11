package com.example.p40

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.ShopDefenseUnit

class DefenseUnitAdapter(
    private val defenseUnits: List<ShopDefenseUnit>,
    private val onBuyUnit: (ShopDefenseUnit) -> Unit,
    private val onApplyUnit: (ShopDefenseUnit) -> Unit
) : RecyclerView.Adapter<DefenseUnitAdapter.UnitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_defense_unit, parent, false)
        return UnitViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        val unit = defenseUnits[position]
        holder.bind(unit)
    }

    override fun getItemCount(): Int = defenseUnits.size

    inner class UnitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUnitName: TextView = itemView.findViewById(R.id.tvUnitName)
        private val tvUnitDesc: TextView = itemView.findViewById(R.id.tvUnitDesc)
        private val tvUnitPrice: TextView = itemView.findViewById(R.id.tvUnitPrice)
        private val tvUnitSymbol: TextView = itemView.findViewById(R.id.tvUnitSymbol)
        private val btnBuyUnit: Button = itemView.findViewById(R.id.btnBuyUnit)
        private val btnApplyUnit: Button = itemView.findViewById(R.id.btnApplyUnit)
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvAppliedLabel: TextView = itemView.findViewById(R.id.tvAppliedLabel)
        private val tvNewLabel: TextView = itemView.findViewById(R.id.tvNewLabel)
        private val tvAppliedOrder: TextView = itemView.findViewById(R.id.tvAppliedOrder)

        fun bind(unit: ShopDefenseUnit) {
            // 유닛 이름 및 설명 설정
            tvUnitName.text = unit.name
            tvUnitDesc.text = unit.description
            
            // 유닛 가격 설정
            tvUnitPrice.text = "가격: ${unit.price} 코인"
            
            // 유닛 문양 타입 표시
            when (unit.symbolType) {
                com.example.p40.game.CardSymbolType.HEART -> {
                    tvUnitSymbol.text = "♥"
                    tvUnitSymbol.setTextColor(android.graphics.Color.RED)
                }
                com.example.p40.game.CardSymbolType.DIAMOND -> {
                    tvUnitSymbol.text = "♦"
                    tvUnitSymbol.setTextColor(android.graphics.Color.RED)
                }
                com.example.p40.game.CardSymbolType.CLUB -> {
                    tvUnitSymbol.text = "♣"
                    tvUnitSymbol.setTextColor(android.graphics.Color.BLACK)
                }
                else -> {
                    tvUnitSymbol.text = "♠"
                    tvUnitSymbol.setTextColor(android.graphics.Color.BLACK)
                }
            }
            
            // 신규 유닛 라벨 표시
            tvNewLabel.visibility = if (unit.isNew) View.VISIBLE else View.GONE
            
            // 적용 라벨 표시
            tvAppliedLabel.visibility = if (unit.isApplied) View.VISIBLE else View.GONE
            
            // 적용 순서 표시
            if (unit.isApplied && unit.appliedOrder > 0) {
                tvAppliedOrder.visibility = View.VISIBLE
                tvAppliedOrder.text = "${unit.appliedOrder}"
            } else {
                tvAppliedOrder.visibility = View.GONE
            }
            
            // 카드 배경색 설정 (적용된 유닛은 강조 표시)
            if (unit.isApplied) {
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F7FA"))
            } else {
                cardView.setCardBackgroundColor(android.graphics.Color.WHITE)
            }
            
            // 구매 버튼 상태 설정
            if (unit.isPurchased) {
                btnBuyUnit.text = "구매완료"
                btnBuyUnit.isEnabled = false
                btnBuyUnit.alpha = 0.5f
            } else {
                // 미구매 상태
                btnBuyUnit.text = "구매하기"
                btnBuyUnit.isEnabled = true
                btnBuyUnit.alpha = 1.0f
                
                // 구매 버튼 이벤트
                btnBuyUnit.setOnClickListener {
                    onBuyUnit(unit)
                }
            }
            
            // 적용 버튼 상태 설정 - 항상 표시하되 구매 여부에 따라 활성화/비활성화
            if (unit.isPurchased) {
                // 구매한 경우 활성화
                if (unit.isApplied) {
                    // 이미 적용된 경우 - 제거하기 버튼으로 변경
                    btnApplyUnit.text = "제거하기"
                    btnApplyUnit.isEnabled = true
                    btnApplyUnit.alpha = 1.0f
                    
                    // 적용 버튼 이벤트 - 제거 기능
                    btnApplyUnit.setOnClickListener {
                        onApplyUnit(unit)
                    }
                } else {
                    btnApplyUnit.text = "적용하기"
                    btnApplyUnit.isEnabled = true
                    btnApplyUnit.alpha = 1.0f
                    
                    // 적용 버튼 이벤트
                    btnApplyUnit.setOnClickListener {
                        onApplyUnit(unit)
                    }
                }
            } else {
                // 미구매 상태에서는 적용 버튼 비활성화
                btnApplyUnit.text = "적용하기"
                btnApplyUnit.isEnabled = false
                btnApplyUnit.alpha = 0.5f
            }
        }
    }
} 