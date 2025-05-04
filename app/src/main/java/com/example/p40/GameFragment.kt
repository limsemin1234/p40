package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.p40.game.GameConfig
import com.example.p40.game.GameView

class GameFragment : Fragment(R.layout.fragment_game) {

    private lateinit var gameView: GameView
    private var isPaused = false
    private var cardCooldown = false
    private val cardCooldownTime = GameConfig.CARD_COOLDOWN

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 게임 뷰 및 컨트롤 초기화
        gameView = view.findViewById(R.id.gameView)
        
        // 카드 사용 버튼 (공격 카드)
        val useCardButton = view.findViewById<Button>(R.id.useCardButton)
        useCardButton.setOnClickListener {
            if (!cardCooldown) {
                gameView.useCard()
                // 카드 쿨다운 설정
                cardCooldown = true
                useCardButton.isEnabled = false
                useCardButton.text = "쿨다운 중..."
                
                // 일정 시간 후 카드 다시 사용 가능하게
                view.postDelayed({
                    cardCooldown = false
                    useCardButton.isEnabled = true
                    useCardButton.text = "카드 사용"
                    if (isAdded) {
                        Toast.makeText(context, "카드가 다시 사용 가능합니다!", Toast.LENGTH_SHORT).show()
                    }
                }, cardCooldownTime)
            }
        }
        
        // 일시정지 버튼
        val pauseButton = view.findViewById<Button>(R.id.pauseButton)
        pauseButton.setOnClickListener {
            isPaused = !isPaused
            if (isPaused) {
                pauseButton.text = "재개"
                gameView.pause()
            } else {
                pauseButton.text = "일시정지"
                gameView.resume()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
    
    override fun onResume() {
        super.onResume()
        if (!isPaused) {
            gameView.resume()
        }
    }
}
