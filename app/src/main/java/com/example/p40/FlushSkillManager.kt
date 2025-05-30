package com.example.p40

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.FrameLayout

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
    
    // 시각적 효과 관리자
    private var visualEffectManager: VisualEffectManager? = null
    
    // 버튼 크기 상수
    private val BUTTON_WIDTH = 80 // dp 단위로 설정 (100dp에서 60dp로 줄임)
    private val BUTTON_HEIGHT = ViewGroup.LayoutParams.WRAP_CONTENT
    
    // 초기화
    init {
        // 기본적으로 모든 스킬 비활성화
        CardSuit.values().forEach { suit ->
            if (suit != CardSuit.JOKER) {
                activeSkills[suit] = false
            }
        }
        
        // 버튼 컨테이너의 orientation과 gravity만 설정하고, 레이아웃 파라미터는 건드리지 않음
        skillButtonContainer.orientation = LinearLayout.HORIZONTAL
        skillButtonContainer.gravity = Gravity.START
        
        // 부모 뷰 (게임 컨테이너) 찾기
        val parent = skillButtonContainer.parent
        if (parent is FrameLayout) {
            visualEffectManager = VisualEffectManager(context, parent)
        }
    }
    
    /**
     * dp값을 픽셀로 변환하는 유틸리티 함수
     */
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * 문양별 플러시 스킬 활성화
     */
    fun activateFlushSkill(suit: CardSuit) {
        // 이미 해당 문양의 스킬이 활성화되어 있지 않은 경우에만 처리
        if (activeSkills[suit] == true) {
            return
        }
        
        // 문양에 따른 스킬 버튼 생성
        createSkillButton(suit)
        
        // 활성화 상태로 변경
        activeSkills[suit] = true
        
        // 버튼 컨테이너를 보이게 설정
        skillButtonContainer.post {
            skillButtonContainer.visibility = View.VISIBLE
        }
    }
    
    /**
     * 문양에 따른 스킬 버튼 생성
     */
    private fun createSkillButton(suit: CardSuit) {
        // 이미 버튼이 있으면 리턴
        if (skillButtons.containsKey(suit)) return
        
        // 스킬 이름 설정
        val skillName = when (suit) {
            CardSuit.HEART -> "회복"
            CardSuit.SPADE -> "소멸"
            CardSuit.CLUB -> "정지"
            CardSuit.DIAMOND -> "무적"
            else -> ""
        }
        
        // 버튼 생성
        val button = Button(context).apply {
            // 레이아웃 설정 - 고정 크기 사용
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(BUTTON_WIDTH),  // 고정 너비
                BUTTON_HEIGHT // 높이는 콘텐츠에 맞게
            ).apply {
                setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1)) // 마진 설정을 2dp로 줄임
            }
            
            // 버튼 스타일 설정
            setBackgroundResource(R.drawable.flush_skill_button_bg)
            
            // 문양 + 스킬명 설정
            text = "${suit.getSymbol()}\n$skillName"
            textSize = 12f // 텍스트 크기 키움
            
            // 버튼 텍스트 색상 설정 (더 밝은 색상으로)
            val textColor = when (suit) {
                CardSuit.HEART, CardSuit.DIAMOND -> Color.parseColor("#FF6666") // 밝은 빨간색
                CardSuit.SPADE, CardSuit.CLUB -> Color.parseColor("#FFFFFF") // 흰색
                else -> Color.WHITE
            }
            setTextColor(textColor)
            
            // 텍스트에 그림자 효과 추가하여 더 잘 보이게 함
            setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK)
            
            // 패딩 설정 - 좀 더 여유 있게
            setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3))
            
            // 버튼 텍스트를 가운데 정렬
            gravity = Gravity.CENTER
            
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
                // 하트 플러시: 체력 회복
                if (GameConfig.HEART_FLUSH_HEAL_AMOUNT < 0) {
                    // -1은 전체 회복
                    gameView.restoreFullHealth()
                    messageManager.showInfo("하트 플러시 스킬: 체력 전체 회복!")
                } else {
                    // 특정 수치만큼 회복
                    gameView.healUnit(GameConfig.HEART_FLUSH_HEAL_AMOUNT)
                    messageManager.showInfo("하트 플러시 스킬: 체력 ${GameConfig.HEART_FLUSH_HEAL_AMOUNT} 회복!")
                }
                
                // 하트 이펙트 보여주기
                visualEffectManager?.showHeartFlushEffect()
            }
            
            CardSuit.SPADE -> {
                // 스페이드 플러시: 화면 내 모든 적 제거 (보스 제외)
                val killedEnemies = gameView.removeAllEnemiesExceptBoss()
                
                // 스페이드 이펙트 보여주기
                visualEffectManager?.showSpadeFlushEffect()
                
                // 메시지 표시
                messageManager.showInfo("스페이드 플러시 스킬: ${killedEnemies}마리 적 제거!")
            }
            
            CardSuit.CLUB -> {
                // 클로버 플러시: 시간 멈춤 (설정된 시간 동안 공격 범위 내 적만 멈춤)
                val duration = GameConfig.CLUB_FLUSH_DURATION
                
                // 기존 전체 멈춤에서 범위 기반 멈춤으로 변경
                gameView.freezeEnemiesInRange(true)
                
                // 클로버 이펙트 보여주기 (모래시계 효과)
                visualEffectManager?.showClubFlushEffect(duration)
                
                // 메시지 표시 (초 단위로 변환)
                val durationInSeconds = duration / 1000
                messageManager.showInfo("클로버 플러시 스킬: ${durationInSeconds}초간 공격 범위 내 시간 정지!")
                
                // 설정된 시간 후 효과 해제
                handler.postDelayed({
                    gameView.freezeEnemiesInRange(false)
                    // 효과가 확실히 제거될 수 있도록 명시적 호출 추가
                    visualEffectManager?.clearEffects()
                    messageManager.showInfo("클로버 플러시 스킬 종료")
                }, duration)
            }
            
            CardSuit.DIAMOND -> {
                // 다이아 플러시: 무적 (설정된 시간 동안)
                val duration = GameConfig.DIAMOND_FLUSH_DURATION
                gameView.setInvincible(true)
                
                // 다이아 이펙트 보여주기
                visualEffectManager?.showDiamondFlushEffect(duration)
                
                // 메시지 표시 (초 단위로 변환)
                val durationInSeconds = duration / 1000
                messageManager.showInfo("다이아 플러시 스킬: ${durationInSeconds}초간 무적!")
                
                // 설정된 시간 후 효과 해제
                handler.postDelayed({
                    gameView.setInvincible(false)
                    // 효과가 확실히 제거될 수 있도록 명시적 호출 추가
                    visualEffectManager?.clearEffects()
                    messageManager.showInfo("다이아 플러시 스킬 종료")
                }, duration)
            }
            
            else -> return // 조커 등 다른 슈트는 처리 안함
        }
        
        // 스킬 사용 후 비활성화
        deactivateSkill(suit)
        
        // 모든 스킬이 비활성화되었는지 확인하여 컨테이너 숨김 처리
        if (activeSkills.none { it.value }) {
            skillButtonContainer.visibility = View.GONE
        }
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
        
        // 활성화된 스킬이 없으므로 컨테이너 숨김
        skillButtonContainer.visibility = View.GONE
    }
    
    /**
     * 모든 스킬을 초기화하고 핸들러 작업 취소 (게임 종료 시 사용)
     */
    fun resetAllSkills() {
        // 진행 중인 모든 지연 작업 취소
        handler.removeCallbacksAndMessages(null)
        
        try {
            // 클로버 플러시 효과(시간 멈춤) 해제
            gameView.freezeAllEnemies(false)
            gameView.freezeEnemiesInRange(false) // 범위 기반 시간 멈춤도 해제
            
            // 다이아몬드 플러시 효과(무적) 해제
            gameView.setInvincible(false)
            
            // 모든 시각적 효과 제거
            visualEffectManager?.clearEffects()
        } catch (e: Exception) {
            // 예외 발생 시 로그만 출력하고 계속 진행
            e.printStackTrace()
        }
        
        // 모든 스킬 비활성화
        deactivateAllSkills()
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        resetAllSkills()
        visualEffectManager?.cleanup()
        visualEffectManager = null
    }
    
    /**
     * 메모리 누수 방지를 위한 참조 정리
     * Fragment의 onDestroyView에서 호출해야 함
     */
    fun clearReferences() {
        // 모든 지연 작업 취소
        handler.removeCallbacksAndMessages(null)
        
        // 시각적 효과 제거
        visualEffectManager?.clearEffects()
        
        // 모든 스킬 버튼 클릭 리스너 제거
        skillButtons.values.forEach { button ->
            button.setOnClickListener(null)
        }
        
        // 버튼 참조 정리
        skillButtons.clear()
        
        // 활성화 상태 초기화
        activeSkills.clear()
    }
} 