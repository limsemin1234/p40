package com.example.p40

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView

import androidx.cardview.widget.CardView
import androidx.navigation.fragment.findNavController

class MainMenuFragment : BaseFragment(R.layout.fragment_main_menu) {
    
    private val TAG = "MainMenuFragment"

    // MessageManager 참조
    private lateinit var messageManager: MessageManager
    
    // 애니메이션 상태 관리
    private var isAnimationPlaying = false
    private lateinit var rotationAnimation: Animation
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // MessageManager 초기화
        messageManager = MessageManager.getInstance()
        messageManager.init(view.findViewById(android.R.id.content) ?: view as ViewGroup)
        
        // 상단바의 뒤로가기 버튼 숨기기
        view.findViewById<ImageButton>(R.id.btnBack)?.visibility = View.INVISIBLE
        
        // 카드 회전 시 클리핑 문제 해결을 위해 클리핑 비활성화
        try {
            // 프래그먼트의 부모 뷰들에 대해 클리핑 비활성화
            (view as? ViewGroup)?.clipChildren = false
            (view as? ViewGroup)?.clipToPadding = false
            
            // 상위 레이아웃까지 클리핑 비활성화
            val mainLayout = view.findViewById<ViewGroup>(R.id.mainMenuLayout)
            mainLayout?.clipChildren = false
            mainLayout?.clipToPadding = false
            
            // 모든 부모 뷰에 클리핑 비활성화를 적용
            val parentActivty = requireActivity()
            (parentActivty.findViewById<ViewGroup>(android.R.id.content))?.apply {
                clipChildren = false
                clipToPadding = false
            }
            
            // 모든 내부 레이아웃도 클리핑 비활성화
            view.findViewById<ViewGroup>(R.id.mainMenuLayout)?.let { layout ->
                disableClippingInAllChildViews(layout)
            }
            
            Log.d(TAG, "View clipping disabled for card animation")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling view clipping: ${e.message}")
        }
        
        // 애니메이션 초기화 - 무한 반복 설정 확실히 추가
        try {
            rotationAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.card_rotation).apply {
                repeatCount = Animation.INFINITE
                repeatMode = Animation.RESTART
                // 애니메이션 속성 확실히 설정
                fillAfter = true
                fillBefore = true
                isFillEnabled = true
            }
            Log.d(TAG, "Rotation animation loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading rotation animation: ${e.message}")
            // Animation은 추상 클래스이므로 직접 인스턴스화할 수 없음
            // 간단한 애니메이션 생성
            try {
                rotationAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in).apply {
                    repeatCount = Animation.INFINITE
                    duration = 2000
                }
                Log.d(TAG, "Fallback animation loaded")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to load fallback animation: ${e2.message}")
                isAnimationPlaying = false
            }
        }
        
        // 로고 카드에 애니메이션 적용
        try {
            val gameLogo = view.findViewById<MainMenuLogoCard>(R.id.gameLogo)
            Log.d(TAG, "Logo card view found: ${gameLogo != null}")
            
            if (gameLogo != null && ::rotationAnimation.isInitialized) {
                // 강제로 가시성 설정
                gameLogo.visibility = View.VISIBLE
                
                // 처음에는 애니메이션 비활성화 상태로 시작
                isAnimationPlaying = false
                
                // 애니메이션 참조 및 상태 설정
                gameLogo.setAnimationActive(false)
                gameLogo.setCardAnimation(rotationAnimation)
                
                // 로그 카드에 추가 설정 - 더 명확한 로그 및 강화된 클릭 처리
                gameLogo.setOnClickListener { 
                    Log.d(TAG, "Logo card clicked directly from MainMenuFragment OnClickListener")
                    isAnimationPlaying = !isAnimationPlaying
                    gameLogo.toggleAnimation()
                }
                
                // 카드가 클릭 가능하도록 명시적 설정
                gameLogo.isClickable = true
                gameLogo.isFocusable = true
                
                // 클릭 시 피드백 추가
                gameLogo.isHapticFeedbackEnabled = true
                
                Log.d(TAG, "Logo card animation configured with enhanced click handling")
            } else {
                if (gameLogo == null) {
                    Log.e(TAG, "Failed to find gameLogo view")
                } else {
                    Log.e(TAG, "Animation not initialized")
                }
                isAnimationPlaying = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing logo card animation: ${e.message}")
            e.printStackTrace() // 스택 트레이스 출력
            isAnimationPlaying = false
        }
        
        // 게임 로비 버튼 클릭 시 로비 화면으로 이동
        val cardLobby = view.findViewById<CardView>(R.id.cardLobby)
        cardLobby.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_game)
        }
        
        // 스타일 및 애니메이션 적용
        applyMenuCardStyles(view)

        // 카드 구매 버튼 클릭 시
        val cardShop = view.findViewById<CardView>(R.id.cardShop)
        cardShop.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_cardShop)
        }
        
        // 스탯 강화 버튼 클릭 시 스탯 강화 화면으로 이동
        val cardStatsUpgrade = view.findViewById<CardView>(R.id.cardStatsUpgrade)
        cardStatsUpgrade.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_statsUpgrade)
        }

        // 덱 구성 버튼 클릭 시 덱 구성 화면으로 이동
        val cardDeckBuilder = view.findViewById<CardView>(R.id.cardDeckBuilder)
        cardDeckBuilder.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_deckBuilder)
        }

        // 게임 종료 버튼 클릭 시 앱 종료
        val cardExit = view.findViewById<CardView>(R.id.cardExit)
        cardExit.setOnClickListener {
            activity?.finish()
        }
    }
    
    /**
     * 메인 메뉴 카드들에 스타일 적용
     */
    private fun applyMenuCardStyles(view: View) {
        // 각 메뉴 카드에 있는 버튼들에 애니메이션 효과 적용
        val cardViews = listOf(
            view.findViewById<CardView>(R.id.cardLobby),
            view.findViewById<CardView>(R.id.cardShop),
            view.findViewById<CardView>(R.id.cardStatsUpgrade),
            view.findViewById<CardView>(R.id.cardDeckBuilder),
            view.findViewById<CardView>(R.id.cardExit)
        )
        
        // 각 카드에 애니메이션 적용
        cardViews.forEach { cardView ->
            cardView?.let {
                // 카드 뷰에 클릭 애니메이션 적용
                ButtonAnimationUtils.applyButtonAnimationProperty(it)
                
                // 카드뷰 내부 텍스트뷰 스타일링
                try {
                    // 카드뷰 내부의 첫번째 자식이 일반적으로 텍스트뷰
                    if (it is ViewGroup && it.childCount > 0) {
                        val childView = it.getChildAt(0)
                        if (childView is TextView) {
                            childView.setTextColor(android.graphics.Color.WHITE)
                            childView.textSize = 18f
                            childView.setTypeface(childView.typeface, android.graphics.Typeface.BOLD)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 코인 정보 갱신
        view?.let { 
            updateCoinInfo(it)
            
            // 화면이 다시 보일 때 애니메이션 상태 확인 후 재시작
            try {
                val gameLogo = it.findViewById<MainMenuLogoCard>(R.id.gameLogo)
                // 로고 뷰와 애니메이션이 모두 유효한지 확인
                if (gameLogo != null && ::rotationAnimation.isInitialized) {
                    // 애니메이션 설정 확인
                    if (rotationAnimation.repeatCount != Animation.INFINITE) {
                        rotationAnimation.repeatCount = Animation.INFINITE
                        rotationAnimation.repeatMode = Animation.RESTART
                    }
                    
                    // 이전 애니메이션 상태 복원 (재생 중이었다면 다시 시작)
                    gameLogo.setAnimationActive(isAnimationPlaying)
                    if (isAnimationPlaying) {
                        gameLogo.startCardAnimation(rotationAnimation)
                        Log.d(TAG, "Logo card animation restarted on resume")
                    } else {
                        Log.d(TAG, "Animation not playing, no need to restart")
                    }
                } else {
                    Log.e(TAG, "Failed to find gameLogo view or animation not initialized on resume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting animation on resume: ${e.message}")
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // 화면이 사라질 때 애니메이션 상태 저장
        view?.let {
            try {
                val gameLogo = it.findViewById<MainMenuLogoCard>(R.id.gameLogo)
                if (gameLogo != null) {
                    isAnimationPlaying = gameLogo.isAnimationActive()
                    Log.d(TAG, "Animation state saved on pause: $isAnimationPlaying")
                } else {
                    Log.e(TAG, "Failed to find gameLogo view on pause")
                    isAnimationPlaying = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving animation state on pause: ${e.message}")
                isAnimationPlaying = false
            }
        }
    }

    // 모든 자식 뷰에 재귀적으로 클리핑 비활성화 적용
    private fun disableClippingInAllChildViews(viewGroup: ViewGroup) {
        viewGroup.clipChildren = false
        viewGroup.clipToPadding = false
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                child.clipChildren = false
                child.clipToPadding = false
                disableClippingInAllChildViews(child)
            }
        }
    }
    
    // 메모리 누수 방지를 위한 onDestroy 메서드 추가
    override fun onDestroy() {
        super.onDestroy()
        
        try {
            // 애니메이션 정리
            view?.let {
                val gameLogo = it.findViewById<MainMenuLogoCard>(R.id.gameLogo)
                gameLogo?.let { logo ->
                    // 애니메이션 중지
                    logo.clearAnimation()
                    logo.setAnimationActive(false)
                    
                    // 클릭 리스너 제거
                    logo.setOnClickListener(null)
                    
                    Log.d(TAG, "Animation resources cleaned up in onDestroy")
                }
            }
            
            // MessageManager 정리
            if(::messageManager.isInitialized) {
                messageManager.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources in onDestroy: ${e.message}")
        }
    }
}
