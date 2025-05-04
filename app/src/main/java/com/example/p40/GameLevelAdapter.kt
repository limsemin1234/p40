package com.example.p40

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class GameLevelAdapter(
    private val levels: List<GameLevel>,
    private val onLevelSelected: (GameLevel) -> Unit
) : RecyclerView.Adapter<GameLevelAdapter.LevelViewHolder>() {

    private var selectedPosition = 0  // 기본적으로 첫 번째 레벨 선택

    inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardGameLevel: CardView = itemView.findViewById(R.id.cardGameLevel)
        private val tvLevelNumber: TextView = itemView.findViewById(R.id.tvLevelNumber)
        private val tvLevelTitle: TextView = itemView.findViewById(R.id.tvLevelTitle)
        private val tvLevelDescription: TextView = itemView.findViewById(R.id.tvLevelDescription)
        private val ivLevelSelected: ImageView = itemView.findViewById(R.id.ivLevelSelected)

        fun bind(level: GameLevel, position: Int) {
            tvLevelNumber.text = level.number.toString()
            tvLevelTitle.text = level.title
            tvLevelDescription.text = level.description

            // 선택된 아이템 표시
            if (position == selectedPosition) {
                ivLevelSelected.visibility = View.VISIBLE
                cardGameLevel.setCardBackgroundColor(0xFF333333.toInt())
            } else {
                ivLevelSelected.visibility = View.GONE
                cardGameLevel.setCardBackgroundColor(0xFF222222.toInt())
            }

            // 아이템 클릭 이벤트 설정
            itemView.setOnClickListener {
                if (selectedPosition != position) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onLevelSelected(level)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.bind(levels[position], position)
    }

    override fun getItemCount(): Int = levels.size

    // 현재 선택된 레벨 반환
    fun getSelectedLevel(): GameLevel = levels[selectedPosition]
} 