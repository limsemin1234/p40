package com.example.p40

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import com.example.p40.R

/**
 * 포커 족보 가이드를 보여주는 다이얼로그 클래스
 */
class PokerGuideDialog(context: Context) {
    
    private val dialog: Dialog = Dialog(context)
    
    init {
        // 다이얼로그 초기화
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_poker_guide)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 다이얼로그 크기 설정 - 화면 너비의 95%로 설정
        val window = dialog.window
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.95).toInt()
        window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        
        // 닫기 버튼 클릭 이벤트 설정
        val btnCloseGuide = dialog.findViewById<Button>(R.id.btnCloseGuide)
        btnCloseGuide.setOnClickListener {
            dialog.dismiss()
        }
        
        // 닫기 버튼에 스타일 적용
        btnCloseGuide.setBackgroundResource(R.drawable.btn_game_primary)
        ButtonAnimationUtils.applyButtonAnimation(btnCloseGuide, context)
    }
    
    /**
     * 다이얼로그 표시
     */
    fun show() {
        dialog.show()
    }
    
    /**
     * 다이얼로그 닫기
     */
    fun dismiss() {
        dialog.dismiss()
    }
    
    /**
     * 백 버튼 처리를 위한 취소 가능 여부 설정
     */
    fun setCancelable(cancelable: Boolean) {
        dialog.setCancelable(cancelable)
    }
} 