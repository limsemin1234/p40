package com.example.p40

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat

/**
 * 게임 내 시각적 효과를 관리하는 클래스
 */
class VisualEffectManager(
    private val context: Context,
    private val gameContainer: FrameLayout
) {
    // 핸들러
    private val handler = Handler(Looper.getMainLooper())
    
    // 효과에 사용할 뷰들
    private var effectOverlay: View? = null
    private var effectIcon: ImageView? = null
    
    // 현재 실행 중인 애니메이션을 추적
    private var currentAnimators = mutableListOf<ValueAnimator>()
    private var currentAnimations = mutableListOf<Animation>()
    
    /**
     * 하트 플러시 스킬 효과 (체력 회복)
     */
    fun showHeartFlushEffect() {
        // 오버레이 제거 (기존에 있는 경우)
        clearEffects()
        
        // 붉은색 오버레이 생성 및 추가
        createOverlay(Color.parseColor("#33FF0000")) // 반투명 빨간색
        
        // 하트 아이콘 추가
        val heartIcon = createEffectIcon(R.drawable.heart_effect)
        
        // 하트가 화면 중앙에서 확대되는 애니메이션
        val scaleAnimation = ScaleAnimation(
            0f, 3f, 0f, 3f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            fillAfter = false
        }
        
        // 투명도 애니메이션 (나타났다 사라짐)
        val alphaAnimation = AlphaAnimation(1f, 0f).apply {
            duration = 1000
            startOffset = 500
            fillAfter = true
        }
        
        // 애니메이션 세트 생성 및 시작
        val animSet = AnimationSet(true).apply {
            addAnimation(scaleAnimation)
            addAnimation(alphaAnimation)
        }
        
        heartIcon.startAnimation(animSet)
        currentAnimations.add(animSet)
        
        // 1.5초 후 효과 제거
        handler.postDelayed({ clearEffects() }, 1500)
    }
    
    /**
     * 스페이드 플러시 스킬 효과 (적 제거)
     */
    fun showSpadeFlushEffect() {
        // 오버레이 제거 (기존에 있는 경우)
        clearEffects()
        
        // 오버레이 색상을 푸른색 계열로 변경
        createOverlay(Color.parseColor("#334169E1")) // 반투명 로얄 블루
        
        // 스페이드 아이콘 추가
        val spadeIcon = createEffectIcon(R.drawable.spade_effect)
        
        // 아이콘에 밝은 색 테두리 설정
        spadeIcon.setColorFilter(Color.parseColor("#B0E0E6")) // 파우더 블루 색상 필터 적용
        
        // 스페이드가 회전하며 확대되는 애니메이션
        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            fillAfter = false
        }
        
        val scaleAnimation = ScaleAnimation(
            0f, 2f, 0f, 2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            fillAfter = false
        }
        
        // 투명도 애니메이션
        val alphaAnimation = AlphaAnimation(1f, 0f).apply {
            duration = 800
            startOffset = 700
            fillAfter = true
        }
        
        // 애니메이션 세트 생성 및 시작
        val animSet = AnimationSet(true).apply {
            addAnimation(rotateAnimation)
            addAnimation(scaleAnimation)
            addAnimation(alphaAnimation)
        }
        
        spadeIcon.startAnimation(animSet)
        currentAnimations.add(animSet)
        
        // 1.5초 후 효과 제거
        handler.postDelayed({ clearEffects() }, 1500)
    }
    
    /**
     * 클로버 플러시 스킬 효과 (시간 멈춤)
     */
    fun showClubFlushEffect(durationMs: Long = GameConfig.CLUB_FLUSH_DURATION) {
        // 오버레이 제거 (기존에 있는 경우)
        clearEffects()
        
        // 디펜스 유닛 위치 정보 가져오기 (일반적으로 GameView가 화면 중앙에 있다고 가정)
        val unitX = gameContainer.width / 2f
        val unitY = gameContainer.height / 2f
        
        // 모래시계 아이콘 생성
        val hourglassIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(180, 300).apply {
                leftMargin = (unitX - 90).toInt()
                topMargin = (unitY - 185).toInt()
            }
            setImageResource(R.drawable.hourglass_effect)
            alpha = 0.9f
        }
        
        gameContainer.addView(hourglassIcon)
        effectIcon = hourglassIcon // 효과 아이콘으로 저장하여 확실히 제거되도록 함
        
        // 모래시계 흘러내리는 애니메이션
        val sandAnimator = ObjectAnimator.ofFloat(hourglassIcon, "translationY", 0f, 100f).apply {
            duration = durationMs
            start()
        }
        
        // 모래시계 회전 애니메이션
        val rotateAnimator = ObjectAnimator.ofFloat(hourglassIcon, "rotation", 0f, 180f).apply {
            duration = durationMs
            start()
        }
        
        // 애니메이션 추적
        currentAnimators.add(sandAnimator)
        currentAnimators.add(rotateAnimator)
        
        // 지정된 시간 후 효과 제거
        handler.postDelayed({
            // 모든 애니메이션 종료 및 효과 제거
            clearEffects()
        }, durationMs)
    }
    
    /**
     * 다이아몬드 플러시 스킬 효과 (무적)
     */
    fun showDiamondFlushEffect(durationMs: Long = GameConfig.DIAMOND_FLUSH_DURATION) {
        // 오버레이 제거 (기존에 있는 경우)
        clearEffects()
        
        // 다이아몬드 아이콘 추가 - 크기 키우기
        val diamondIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = 350  // 더 큰 크기로 변경 (300에서 350으로)
                height = 350
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(R.drawable.diamond_effect)
            alpha = 0.6f  // 시작 투명도 설정
        }
        
        gameContainer.addView(diamondIcon)
        effectIcon = diamondIcon
        
        // 3D 효과를 위한 초기 설정
        diamondIcon.cameraDistance = 12000f  // 3D 효과를 위한 카메라 거리 설정 증가
        
        // X축 회전 애니메이션 (3D 효과)
        val rotateXAnimator = ObjectAnimator.ofFloat(diamondIcon, "rotationX", 0f, 360f).apply {
            duration = 5000  // 더 느린 회전
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        
        // Y축 회전 애니메이션 (3D 효과)
        val rotateYAnimator = ObjectAnimator.ofFloat(diamondIcon, "rotationY", 0f, 360f).apply {
            duration = 7000  // 더 느린 회전
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        
        // Z축 회전 애니메이션 (약간의 기울임 효과)
        val rotateZAnimator = ObjectAnimator.ofFloat(diamondIcon, "rotation", -5f, 5f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
        
        // 크기 변화 애니메이션 (약간 커졌다가 작아지는 효과)
        val scaleXAnimator = ObjectAnimator.ofFloat(diamondIcon, "scaleX", 0.85f, 1.15f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
        
        val scaleYAnimator = ObjectAnimator.ofFloat(diamondIcon, "scaleY", 0.85f, 1.15f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
        
        // 투명도 변화 애니메이션 (0.4에서 0.8로 변화)
        val alphaAnimator = ObjectAnimator.ofFloat(diamondIcon, "alpha", 0.4f, 0.8f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
        
        // 추적을 위해 저장
        currentAnimators.add(rotateXAnimator)
        currentAnimators.add(rotateYAnimator)
        currentAnimators.add(rotateZAnimator)
        currentAnimators.add(scaleXAnimator)
        currentAnimators.add(scaleYAnimator)
        currentAnimators.add(alphaAnimator)
        
        // 지정된 시간 후 효과 제거
        handler.postDelayed({
            // 모든 애니메이션 종료 및 효과 제거
            clearEffects()
        }, durationMs)
    }
    
    /**
     * 효과 오버레이 생성
     */
    private fun createOverlay(color: Int): View {
        val overlay = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(color)
            alpha = 0.3f
        }
        
        gameContainer.addView(overlay)
        effectOverlay = overlay
        return overlay
    }
    
    /**
     * 효과 아이콘 생성
     */
    private fun createEffectIcon(drawableResId: Int): ImageView {
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = 200
                height = 200
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(drawableResId)
            alpha = 0.9f
        }
        
        gameContainer.addView(icon)
        effectIcon = icon
        return icon
    }
    
    /**
     * 방패 아이콘 생성 (무적 효과용) - 더 이상 사용하지 않음
     * @param size 아이콘 크기 (기본값 200)
     */
    // 더 이상 사용하지 않는 메서드지만 호환성을 위해 주석으로 유지
    /*
    private fun createShieldIcon(size: Int = 200): ImageView {
        val shield = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = size
                height = size
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(R.drawable.shield_effect)
            alpha = 0.7f
        }
        
        gameContainer.addView(shield)
        return shield
    }
    */
    
    /**
     * 모든 효과 제거
     */
    fun clearEffects() {
        // 현재 실행 중인 모든 애니메이터 중지
        currentAnimators.forEach { it.cancel() }
        currentAnimators.clear()
        
        // 현재 실행 중인 모든 애니메이션 중지
        currentAnimations.forEach { it.cancel() }
        currentAnimations.clear()
        
        // 오버레이 제거
        effectOverlay?.let {
            gameContainer.removeView(it)
            effectOverlay = null
        }
        
        // 효과 아이콘 제거
        effectIcon?.let {
            gameContainer.removeView(it)
            effectIcon = null
        }
        
        // 기타 모든 이펙트 이미지 제거
        // 안전하게 모든 자식 뷰를 검사하여 효과 이미지 찾기
        val effectDrawableIds = arrayOf(
            R.drawable.heart_effect,
            R.drawable.spade_effect,
            R.drawable.club_effect,
            R.drawable.diamond_effect,
            R.drawable.shield_effect
        )
        
        // removeView는 UI 스레드에서만 호출할 수 있으므로 안전하게 처리
        val viewsToRemove = mutableListOf<View>()
        
        for (i in 0 until gameContainer.childCount) {
            val child = gameContainer.getChildAt(i)
            if (child is ImageView) {
                // 효과 이미지인지 확인
                for (drawableId in effectDrawableIds) {
                    try {
                        val childDrawable = child.drawable
                        val effectDrawable = ContextCompat.getDrawable(context, drawableId)
                        
                        if (childDrawable != null && effectDrawable != null &&
                            childDrawable.constantState == effectDrawable.constantState) {
                            viewsToRemove.add(child)
                            break
                        }
                    } catch (e: Exception) {
                        // 리소스 관련 예외 처리
                    }
                }
            }
        }
        
        // 찾은 뷰 모두 제거
        viewsToRemove.forEach {
            gameContainer.removeView(it)
        }
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        clearEffects()
        handler.removeCallbacksAndMessages(null)
    }
} 