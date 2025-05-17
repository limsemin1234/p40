package com.example.p40

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button

/**
 * 버튼 애니메이션 관련 유틸리티 클래스
 */
object ButtonAnimationUtils {
    
    /**
     * 버튼에 클릭 애니메이션을 적용합니다.
     * @param button 애니메이션을 적용할 버튼
     * @param context 컨텍스트
     */
    fun applyButtonAnimation(button: View, context: Context) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 클릭 시 축소 애니메이션 적용
                    val scaleDownAnim = AnimationUtils.loadAnimation(context, R.anim.btn_scale_down)
                    v.startAnimation(scaleDownAnim)
                    playClickSound(context)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 클릭 해제 시 원래 크기로 복원 애니메이션 적용
                    val scaleUpAnim = AnimationUtils.loadAnimation(context, R.anim.btn_scale_up)
                    v.startAnimation(scaleUpAnim)
                    
                    // ACTION_UP일 때만 클릭 이벤트 전파
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                }
            }
            true
        }
    }
    
    /**
     * 버튼 클릭 사운드를 재생합니다.
     * @param context 컨텍스트
     */
    private fun playClickSound(context: Context) {
        try {
            // 클릭 사운드 리소스 사용 (android.R.raw.sound_name이 없으면 주석 처리하세요)
            // MediaPlayer.create(context, android.R.raw.sound_name)?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 프로퍼티 애니메이션을 사용하여 버튼에 클릭 애니메이션을 적용합니다.
     * @param button 애니메이션을 적용할 버튼
     */
    fun applyButtonAnimationProperty(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 클릭 시 축소 애니메이션
                    val scaleXDown = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 0.95f)
                    val scaleYDown = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 0.95f)
                    val animSetDown = AnimatorSet()
                    animSetDown.playTogether(scaleXDown, scaleYDown)
                    animSetDown.duration = 100
                    animSetDown.start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 클릭 해제 시 복원 애니메이션
                    val scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1.0f)
                    val scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1.0f)
                    val animSetUp = AnimatorSet()
                    animSetUp.playTogether(scaleXUp, scaleYUp)
                    animSetUp.duration = 100
                    animSetUp.start()
                    
                    // ACTION_UP일 때만 클릭 이벤트 전파
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                }
            }
            true
        }
    }
    
    /**
     * 레이아웃 내의 모든 버튼에 애니메이션을 적용합니다.
     * @param rootView 루트 뷰
     * @param context 컨텍스트
     */
    fun applyButtonAnimationToAllButtons(rootView: View, context: Context) {
        if (rootView is Button) {
            applyButtonAnimation(rootView, context)
        } else if (rootView is android.view.ViewGroup) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                applyButtonAnimationToAllButtons(child, context)
            }
        }
    }
    
    /**
     * 버튼에 배경색을 직접 설정하고 테마 스타일을 무시합니다.
     * @param button 버튼 뷰
     * @param backgroundColor 적용할 배경색 (예: "#FFC107")
     * @param strokeColor 테두리 색상 (예: "#FFD700")
     * @param strokeWidth 테두리 두께 (픽셀)
     * @param cornerRadius 모서리 둥글기 (픽셀)
     */
    fun setButtonBackgroundDirectly(
        button: View, 
        backgroundColor: String, 
        strokeColor: String? = null,
        strokeWidth: Int = 2,
        cornerRadius: Float = 8f
    ) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        
        // 배경색 설정
        shape.setColor(android.graphics.Color.parseColor(backgroundColor))
        
        // 테두리 설정 (옵션)
        if (strokeColor != null) {
            shape.setStroke(
                strokeWidth, 
                android.graphics.Color.parseColor(strokeColor)
            )
        }
        
        // 모서리 둥글기 설정
        shape.cornerRadius = cornerRadius
        
        // 배경으로 설정
        button.background = shape
        
        // backgroundTint 속성 초기화 (테마나 style에서 설정된 경우 재정의)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            button.backgroundTintList = null
        }
    }
} 