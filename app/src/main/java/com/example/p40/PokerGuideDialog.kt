package com.example.p40

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button

/**
 * 포커 족보 가이드 다이얼로그 클래스
 * 포커 족보들과 효과를 설명하는 다이얼로그
 */
class PokerGuideDialog(context: Context) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_poker_guide)
        
        // 다이얼로그 배경을 투명하게 설정
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 닫기 버튼 클릭 이벤트 설정
        findViewById<Button>(R.id.btnCloseGuide).setOnClickListener {
            dismiss()
        }
    }
} 