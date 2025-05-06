package com.example.p40.game

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.example.p40.R
import com.example.p40.game.CardSuit

/**
 * 플러시 스킬을 관리하는 클래스
 */
class FlushSkillManager(
    private val context: Context,
    private val gameView: GameView,
    private val skillButtonContainer: LinearLayout,
    private val messageManager: MessageManager
) {
    // 스킬 버튼 참조
    private val skillButtons = mutableMapOf<CardSuit, Button>()
    
    // 스킬 활성화 여부
    private val activeSkills = mutableMapOf<CardSuit, Boolean>()
    
    // 시간 멈춤 관련 핸들러
    private val handler = Handler(Looper.getMainLooper())
    
    // 초기화
    init {
        // 기본적으로 모든 스킬 비활성화
        CardSuit.values().forEach { suit ->
            if (suit != CardSuit.JOKER) {
                activeSkills[suit] = false
            }
        }
    }
    
    /**
     * 문양별 플러시 스킬 활성화
     */
    fun activateFlushSkill(suit: CardSuit) {
        // 이미 해당 문양의 스킬이 활성화되어 있지 않은 경우에만 처리
        if (activeSkills[suit] == true) {
            messageManager.showWarning("이미 $suit 플러시 스킬이 활성화되어 있습니다")
            return
        }
        
        // 문양에 따른 스킬 버튼 생성
        createSkillButton(suit)
        
        // 활성화 상태로 변경
        activeSkills[suit] = true
        
        // 플러시 스킬 획득 메시지
        messageManager.showSuccess("${suit.getName()} 플러시 스킬이 활성화되었습니다! 하단 버튼을 눌러 사용하세요.")
    }
    
    /**
     * 문양에 따른 스킬 버튼 생성
     */
    private fun createSkillButton(suit: CardSuit) {
        // 이미 버튼이 있으면 리턴
        if (skillButtons.containsKey(suit)) return
        
        // 버튼 생성
        val button = Button(context).apply {
            // 레이아웃 설정
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // 마진 설정
                setMargins(2, 2, 2, 2)
            }
            
            // 버튼 스타일 설정
            setBackgroundResource(R.drawable.flush_skill_button_bg)
            minimumWidth = 0
            minimumHeight = 0
            
            // 문양 아이콘 설정
            text = suit.getSymbol()
            textSize = 14f
            setTextColor(suit.getColor())
            
            // 패딩 설정
            setPadding(4, 4, 4, 4)
            
            // 클릭 리스너 설정
            setOnClickListener {
                useFlushSkill(suit)
            }
        }
        
        // 버튼을 컨테이너에 추가
        skillButtonContainer.addView(button)
        
        // 버튼 참조 저장
        skillButtons[suit] = button
    }
    
    /**
     * 플러시 스킬 사용
     */
    private fun useFlushSkill(suit: CardSuit) {
        // 스킬이 활성화되어 있지 않으면 리턴
        if (activeSkills[suit] != true) return
        
        when (suit) {
            CardSuit.HEART -> {
                // 하트 플러시: 체력 전체 회복
                gameView.restoreFullHealth()
                messageManager.showSuccess("체력이 모두 회복되었습니다!")
            }
            
            CardSuit.SPADE -> {
                // 스페이드 플러시: 화면 내 모든 적 제거 (보스 제외)
                val killedEnemies = gameView.removeAllEnemiesExceptBoss()
                messageManager.showSuccess("모든 일반 적이 제거되었습니다! (${killedEnemies}마리)")
            }
            
            CardSuit.CLUB -> {
                // 클로버 플러시: 시간 멈춤 (5초 동안 모든 적 멈춤)
                gameView.freezeAllEnemies(true)
                messageManager.showInfo("시간이 멈췄습니다! (5초)")
                
                // 5초 후 효과 해제
                handler.postDelayed({
                    gameView.freezeAllEnemies(false)
                    messageManager.showWarning("시간 멈춤 효과가 해제되었습니다.")
                }, 5000)
            }
            
            CardSuit.DIAMOND -> {
                // 다이아 플러시: 무적 (5초)
                gameView.setInvincible(true)
                messageManager.showInfo("5초 동안 무적 상태가 되었습니다!")
                
                // 5초 후 효과 해제
                handler.postDelayed({
                    gameView.setInvincible(false)
                    messageManager.showWarning("무적 효과가 해제되었습니다.")
                }, 5000)
            }
            
            else -> return // 조커 등 다른 슈트는 처리 안함
        }
        
        // 스킬 사용 후 비활성화
        deactivateSkill(suit)
    }
    
    /**
     * 스킬 비활성화 및 버튼 제거
     */
    private fun deactivateSkill(suit: CardSuit) {
        activeSkills[suit] = false
        
        // 버튼 제거
        skillButtons[suit]?.let { button ->
            skillButtonContainer.removeView(button)
            skillButtons.remove(suit)
        }
    }
    
    /**
     * 모든 스킬 비활성화 (게임 재시작 등에 사용)
     */
    fun deactivateAllSkills() {
        CardSuit.values().forEach { suit ->
            if (suit != CardSuit.JOKER && activeSkills[suit] == true) {
                deactivateSkill(suit)
            }
        }
        
        // 버튼 컨테이너 비우기
        skillButtonContainer.removeAllViews()
        skillButtons.clear()
    }
} 