package com.example.p40

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.p40.game.CardSymbolType
import kotlin.math.abs

/**
 * 메인 메뉴의 로고 카드 관리 클래스
 * 카드 문양 변경 및 스와이프 처리를 담당
 */
class MainMenuLogoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "MainMenuLogoCard"

    // 뷰 요소들
    private lateinit var cardView: CardView
    private lateinit var cardSymbolCenter: TextView
    private lateinit var cardSymbolTopLeft: TextView
    private lateinit var cardRankTopLeft: TextView
    private lateinit var cardSymbolBottomRight: TextView
    private lateinit var cardRankBottomRight: TextView
    
    // 카드 문양 상태
    private var currentSymbol: CardSymbolType = CardSymbolType.SPADE
    
    // 제스처 감지
    private lateinit var gestureDetector: GestureDetector
    
    // 애니메이션 참조
    private var currentAnimation: Animation? = null
    private var isAnimationPlaying = false
    
    // 초기화
    init {
        // 로고 카드 뷰 초기화
        initView()
        
        // 제스처 감지 설정
        setupGestureDetector()
        
        // 초기 카드 문양 설정
        updateCardSymbol()
        
        // 클릭 및 터치 이벤트 활성화
        isClickable = true
        isFocusable = true
        
        Log.d(TAG, "MainMenuLogoCard initialized")
    }
    
    /**
     * 로고 카드 뷰 초기화
     */
    private fun initView() {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.logo_card_content, this, true)
        
        try {
            // 클리핑 비활성화 - 회전 시 카드가 잘리지 않도록 함
            clipChildren = false
            clipToPadding = false
            
            // 부모 뷰에도 클리핑 비활성화 설정 요청
            (parent as? ViewGroup)?.let {
                it.clipChildren = false
                it.clipToPadding = false
            }
            
            // 가시성 확인
            visibility = View.VISIBLE
            
            // 뷰 요소 참조 가져오기
            cardView = findViewById(R.id.cardView)
            cardSymbolCenter = findViewById(R.id.tvCardSymbolCenter)
            cardSymbolTopLeft = findViewById(R.id.tvCardSymbolTopLeft)
            cardRankTopLeft = findViewById(R.id.tvCardRankTopLeft)
            cardSymbolBottomRight = findViewById(R.id.tvCardSymbolBottomRight)
            cardRankBottomRight = findViewById(R.id.tvCardRankBottomRight)
            
            // 배경 및 가시성 설정
            cardView.visibility = View.VISIBLE
            
            Log.d(TAG, "View elements initialized successfully with clipping disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing view elements: ${e.message}")
        }
    }
    
    /**
     * 제스처 감지 설정
     */
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            // 플링(스와이프) 감지
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // 수평 스와이프가 수직 스와이프보다 더 뚜렷할 때
                if (abs(diffX) > abs(diffY)) {
                    if (diffX > 0) {
                        // 오른쪽으로 스와이프: 다음 문양으로
                        Log.d(TAG, "Right swipe detected, changing to next symbol")
                        changeToNextSymbol()
                    } else {
                        // 왼쪽으로 스와이프: 이전 문양으로
                        Log.d(TAG, "Left swipe detected, changing to previous symbol")
                        changeToPreviousSymbol()
                    }
                    return true
                }
                return false
            }
            
            // 일반 탭 감지 - 이것만으로 충분함
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d(TAG, "Single tap up detected, toggling animation")
                toggleAnimation()
                return true
            }
            
            // 일반 탭 감지 (싱글 탭보다 먼저 호출됨)
            override fun onDown(e: MotionEvent): Boolean {
                return true // 이벤트 소비, 다른 이벤트 호출 허용
            }
        })
        
        Log.d(TAG, "Gesture detector set up")
    }
    
    private fun showToast(message: String) {
        // Toast 제거
    }
    
    /**
     * 상위 뷰의 터치 이벤트 가로채기 방지
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true // 터치 이벤트를 가로채서 자식 뷰가 처리하도록 함
    }
    
    /**
     * 터치 이벤트 처리
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 제스처 감지기에 이벤트 전달
        val gestureResult = gestureDetector.onTouchEvent(event)
        
        // 대부분의 제스처는 GestureDetector에서 처리
        // 여기서는 추가 처리하지 않고 제스처 결과만 반환
        return gestureResult || super.onTouchEvent(event)
    }
    
    /**
     * 클릭 이벤트 처리
     */
    override fun performClick(): Boolean {
        Log.d(TAG, "performClick called")
        // 클릭은 GestureDetector에서 이미 처리하므로 여기서는 애니메이션 토글하지 않음
        return super.performClick()
    }
    
    /**
     * 다음 문양으로 변경
     */
    fun changeToNextSymbol() {
        currentSymbol = currentSymbol.next()
        updateCardSymbol()
    }
    
    /**
     * 이전 문양으로 변경
     */
    fun changeToPreviousSymbol() {
        currentSymbol = when (currentSymbol) {
            CardSymbolType.SPADE -> CardSymbolType.CLUB
            CardSymbolType.HEART -> CardSymbolType.SPADE
            CardSymbolType.DIAMOND -> CardSymbolType.HEART
            CardSymbolType.CLUB -> CardSymbolType.DIAMOND
        }
        updateCardSymbol()
    }
    
    /**
     * 카드 문양 업데이트
     */
    private fun updateCardSymbol() {
        val (symbol, color) = when (currentSymbol) {
            CardSymbolType.SPADE -> Pair("♠", Color.BLACK)
            CardSymbolType.HEART -> Pair("♥", Color.RED)
            CardSymbolType.DIAMOND -> Pair("♦", Color.RED)
            CardSymbolType.CLUB -> Pair("♣", Color.BLACK)
        }
        
        // 중앙 큰 문양 업데이트
        cardSymbolCenter.text = symbol
        cardSymbolCenter.setTextColor(color)
        
        // 왼쪽 상단 문양 업데이트
        cardSymbolTopLeft.text = symbol
        cardSymbolTopLeft.setTextColor(color)
        
        // 오른쪽 하단 문양 업데이트
        cardSymbolBottomRight.text = symbol
        cardSymbolBottomRight.setTextColor(color)
        
        Log.d(TAG, "Card symbol updated to: ${currentSymbol.name}")
    }
    
    /**
     * 애니메이션 설정
     */
    fun setCardAnimation(animation: Animation) {
        try {
            // 애니메이션 설정 및 속성 지정
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = Animation.RESTART
            currentAnimation = animation
            Log.d(TAG, "Card animation set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting animation: ${e.message}")
        }
    }
    
    /**
     * 애니메이션 시작
     */
    fun startCardAnimation(animation: Animation) {
        try {
            // 기존 애니메이션이 있으면 제거
            cardView.clearAnimation()
            
            // 애니메이션 무한 반복 설정 확인
            if (animation.repeatCount != Animation.INFINITE) {
                animation.repeatCount = Animation.INFINITE
                animation.repeatMode = Animation.RESTART
            }
            
            // 새 애니메이션 설정 및 시작
            currentAnimation = animation
            cardView.startAnimation(animation)
            isAnimationPlaying = true
            
            Log.d(TAG, "Card animation started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting animation: ${e.message}")
        }
    }
    
    /**
     * 애니메이션 중지
     */
    fun stopAnimation() {
        try {
            cardView.clearAnimation()
            isAnimationPlaying = false
            
            Log.d(TAG, "Card animation stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping animation: ${e.message}")
        }
    }
    
    /**
     * 애니메이션 토글
     */
    fun toggleAnimation() {
        if (isAnimationPlaying) {
            stopAnimation()
        } else {
            currentAnimation?.let {
                startCardAnimation(it)
            } ?: run {
                Log.w(TAG, "No animation set to toggle")
            }
        }
    }
    
    /**
     * 현재 애니메이션 상태 반환
     */
    fun isAnimationActive(): Boolean = isAnimationPlaying
    
    /**
     * 애니메이션 상태 설정
     */
    fun setAnimationActive(active: Boolean) {
        isAnimationPlaying = active
    }
} 