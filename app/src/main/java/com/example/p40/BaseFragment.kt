package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.game.PokerGuideDialog

/**
 * 모든 Fragment가 상속받는 기본 클래스
 * 공통 기능 (상단바 초기화 등)을 제공
 */
abstract class BaseFragment : Fragment {
    
    constructor() : super()
    
    constructor(layoutId: Int) : super(layoutId)

    // UserManager 공통 참조
    protected lateinit var userManager: UserManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 상단바 초기화
        initTopBar(view)
    }
    
    /**
     * 상단바 초기화
     * - 뒤로가기 버튼
     * - 족보가이드 버튼
     * - 업적 버튼
     * - 코인 표시
     */
    private fun initTopBar(view: View) {
        try {
            // topBar가 있는지 확인
            val topBarInclude = view.findViewById<View>(R.id.topBar) ?: return
            
            // 뒤로가기 버튼 설정
            val btnBack = topBarInclude.findViewById<ImageButton>(R.id.btnBack)
            btnBack?.setOnClickListener {
                // NavController를 사용하여 뒤로가기
                findNavController().navigateUp()
            }
            
            // 족보가이드 버튼 설정
            val btnPokerGuide = topBarInclude.findViewById<Button>(R.id.btnPokerGuide)
            btnPokerGuide?.setOnClickListener {
                // 족보가이드 다이얼로그 표시
                showPokerGuideDialog()
            }
            
            // 업적 버튼 설정 (기능은 아직 구현하지 않음)
            val btnAchievement = topBarInclude.findViewById<Button>(R.id.btnAchievement)
            btnAchievement?.setOnClickListener {
                // 향후 업적 기능 추가 시 구현
            }
            
            // 코인 정보 업데이트
            updateCoinInfo(topBarInclude)
        } catch (e: Exception) {
            // 예외 처리
            e.printStackTrace()
        }
    }
    
    /**
     * 코인 정보 업데이트
     */
    protected fun updateCoinInfo(view: View) {
        try {
            val tvCoinAmount = view.findViewById<TextView>(R.id.tvCoinAmount)
            tvCoinAmount?.text = "코인: ${userManager.getCoin()}"
        } catch (e: Exception) {
            // 예외 처리
            e.printStackTrace()
        }
    }
    
    /**
     * 포커 족보 가이드 다이얼로그 표시
     */
    private fun showPokerGuideDialog() {
        try {
            val dialog = PokerGuideDialog(requireContext())
            dialog.setCancelable(true)
            dialog.show()
        } catch (e: Exception) {
            // 예외 처리
            e.printStackTrace()
        }
    }
} 