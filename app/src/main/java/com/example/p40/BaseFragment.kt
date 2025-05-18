package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.PokerGuideDialog
import java.lang.ref.WeakReference

/**
 * 모든 Fragment가 상속받는 기본 클래스
 * 공통 기능 (상단바 초기화 등)을 제공
 */
abstract class BaseFragment : Fragment {
    
    constructor() : super()
    
    constructor(layoutId: Int) : super(layoutId)

    // UserManager 공통 참조
    protected lateinit var userManager: UserManager
    
    // 다이얼로그 참조 관리
    private var pokerGuideDialogRef: WeakReference<PokerGuideDialog>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 상단바 초기화
        initTopBar(view)
        
        // 버튼 스타일 및 애니메이션 적용
        applyButtonStyles(view)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 열려있는 다이얼로그 닫기
        dismissPokerGuideDialog()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 열려있는 다이얼로그 참조 해제
        pokerGuideDialogRef = null
    }
    
    /**
     * 버튼 스타일 및 애니메이션 적용
     */
    protected fun applyButtonStyles(rootView: View) {
        try {
            // 레이아웃 내의 모든 버튼에 애니메이션 적용
            ButtonAnimationUtils.applyButtonAnimationToAllButtons(rootView, requireContext())
            
            // 여기서 개별 버튼에 특정 스타일을 적용할 수도 있습니다.
            // 예: 특정 ID를 가진 버튼에 Primary 스타일 적용
            // rootView.findViewById<Button>(R.id.btnSave)?.setBackgroundResource(R.drawable.btn_game_primary)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            btnPokerGuide?.let {
                it.setOnClickListener {
                    // 족보가이드 다이얼로그 표시
                    showPokerGuideDialog()
                }
                // 버튼 스타일 적용
                it.setBackgroundResource(R.drawable.btn_game_secondary)
                ButtonAnimationUtils.applyButtonAnimation(it, requireContext())
            }
            
            // 업적 버튼 설정 (기능은 아직 구현하지 않음)
            val btnAchievement = topBarInclude.findViewById<Button>(R.id.btnAchievement)
            btnAchievement?.let {
                it.setOnClickListener {
                    // 향후 업적 기능 추가 시 구현
                }
                // 버튼 스타일 적용
                it.setBackgroundResource(R.drawable.btn_game_primary)
                ButtonAnimationUtils.applyButtonAnimation(it, requireContext())
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
            // 기존 다이얼로그가 있으면 닫기
            dismissPokerGuideDialog()
            
            // 새 다이얼로그 생성 및 표시
            val dialog = PokerGuideDialog(requireContext())
            dialog.setCancelable(true)
            dialog.show()
            
            // 약한 참조로 저장
            pokerGuideDialogRef = WeakReference(dialog)
        } catch (e: Exception) {
            // 예외 처리
            e.printStackTrace()
        }
    }
    
    /**
     * 포커 족보 가이드 다이얼로그 닫기
     */
    private fun dismissPokerGuideDialog() {
        pokerGuideDialogRef?.get()?.let { dialog ->
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 