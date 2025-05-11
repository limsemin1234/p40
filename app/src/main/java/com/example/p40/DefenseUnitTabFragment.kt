package com.example.p40

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.p40.game.ShopDefenseUnit
import com.example.p40.game.MessageManager

/**
 * 상점의 디펜스유닛 탭 프래그먼트
 */
class DefenseUnitTabFragment : Fragment() {

    private lateinit var userManager: UserManager
    private lateinit var defenseUnitAdapter: DefenseUnitAdapter
    private val defenseUnits = mutableListOf<ShopDefenseUnit>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop_tab, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 리사이클러뷰 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvShopItems)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // 빈 메시지 숨기기
        view.findViewById<TextView>(R.id.tvEmptyMessage)?.visibility = View.GONE
        
        // 안내 메시지 표시 (레이아웃의 기존 tvInfoMessage 사용)
        val infoTextView = view.findViewById<TextView>(R.id.tvInfoMessage)
        infoTextView?.apply {
            text = "※ 디펜스유닛은 최대 3개까지 동시에 적용 가능합니다."
            visibility = View.VISIBLE
        }
        
        // 유닛 데이터 초기화
        initDefenseUnits()
        
        // 적용된 유닛 상태 확인 및 업데이트
        updateAppliedUnitState()
        
        // 어댑터 설정
        defenseUnitAdapter = DefenseUnitAdapter(
            defenseUnits = defenseUnits,
            onBuyUnit = { unit -> onUnitBuyClicked(unit) },
            onApplyUnit = { unit -> onUnitApplyClicked(unit) }
        )
        recyclerView.adapter = defenseUnitAdapter
    }
    
    // 디펜스유닛 초기화
    private fun initDefenseUnits() {
        // 디펜스유닛 목록 가져오기
        defenseUnits.clear()
        defenseUnits.addAll(ShopDefenseUnit.getDefaultDefenseUnits())
        
        // 이미 구매한 유닛 확인
        for (unit in defenseUnits) {
            // 스페이드 유닛(id=0)은 항상 구매된 상태로 설정
            if (unit.id == 0) {
                unit.isPurchased = true
            } else if (userManager.hasDefenseUnit(unit.id)) {
                unit.isPurchased = true
            }
        }
    }
    
    // 적용된 유닛 상태 업데이트
    private fun updateAppliedUnitState() {
        // 적용된 디펜스유닛 목록 가져오기
        val appliedUnits = userManager.getAppliedDefenseUnits()
        
        // 모든 유닛의 적용 상태 초기화
        for (unit in defenseUnits) {
            unit.isApplied = false
            unit.appliedOrder = -1 // 적용 순서 초기화
        }
        
        // 적용된 유닛 설정 및 순서 표시
        for (i in appliedUnits.indices) {
            val symbolTypeOrdinal = appliedUnits[i]
            for (unit in defenseUnits) {
                if (unit.symbolType.ordinal == symbolTypeOrdinal) {
                    unit.isApplied = true
                    unit.appliedOrder = i + 1 // 1부터 시작하는 순서
                    break
                }
            }
        }
    }
    
    // 유닛 구매 버튼 클릭 처리
    private fun onUnitBuyClicked(unit: ShopDefenseUnit) {
        // 이미 구매한 유닛인지 확인
        if (unit.isPurchased) {
            MessageManager.getInstance().showInfo(requireContext(), "이미 구매한 유닛입니다.")
            return
        }
        
        // 구매 확인 다이얼로그 - 카드탭과 동일한 스타일 적용
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_small_purchase)
        
        // 배경 투명하게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 제목과 메시지 설정
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        
        tvTitle.text = "디펜스유닛 구매"
        tvMessage.text = "${unit.name}을(를) ${unit.price} 코인에 구매하시겠습니까?"
        
        // 취소 버튼
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 구매 버튼
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            purchaseUnit(unit)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    // 유닛 적용 버튼 클릭 처리
    private fun onUnitApplyClicked(unit: ShopDefenseUnit) {
        // 구매하지 않은 유닛인지 확인
        if (!unit.isPurchased) {
            MessageManager.getInstance().showInfo(requireContext(), "먼저 유닛을 구매해야 합니다.")
            return
        }
        
        // 이미 적용된 유닛인지 확인
        if (unit.isApplied) {
            // 이미 적용된 경우 제거
            removeUnit(unit)
        } else {
            // 새로 적용
            applyUnit(unit)
        }
    }
    
    // 유닛 구매 처리
    private fun purchaseUnit(unit: ShopDefenseUnit) {
        // 코인 차감
        if (userManager.useCoin(unit.price)) {
            // 구매한 유닛을 유저 데이터에 추가
            userManager.addPurchasedDefenseUnit(unit.id)
            
            // UI 업데이트
            unit.isPurchased = true
            defenseUnitAdapter.notifyDataSetChanged()
            
            // 상위 프래그먼트 코인 UI 업데이트
            (parentFragment as? CardShopFragment)?.updateCurrencyUI()
            
            MessageManager.getInstance().showSuccess(requireContext(), "${unit.name} 구매 완료!")
        } else {
            // 코인 부족
            MessageManager.getInstance().showError(requireContext(), "코인이 부족합니다.")
        }
    }
    
    // 유닛 적용 처리
    private fun applyUnit(unit: ShopDefenseUnit) {
        // 새 방식: 유닛 추가
        val success = userManager.addAppliedDefenseUnit(unit.symbolType.ordinal)
        
        if (success) {
            // 적용된 유닛 정보 업데이트
            updateAppliedUnitState()
            
            // UI 업데이트
            defenseUnitAdapter.notifyDataSetChanged()
            
            // 메시지 표시 - 여러 유닛 적용 가능하므로 메시지 수정
            MessageManager.getInstance().showSuccess(requireContext(), "${unit.name}이(가) 적용되었습니다!")
        } else {
            // 이미 적용된 유닛인 경우
            MessageManager.getInstance().showInfo(requireContext(), "이미 적용 중인 유닛이거나 최대 3개까지만 적용 가능합니다.")
        }
    }
    
    // 유닛 제거 처리 (새 메소드)
    private fun removeUnit(unit: ShopDefenseUnit) {
        // 유닛 제거
        val success = userManager.removeAppliedDefenseUnit(unit.symbolType.ordinal)
        
        if (success) {
            // 적용된 유닛 정보 업데이트
            updateAppliedUnitState()
            
            // UI 업데이트
            defenseUnitAdapter.notifyDataSetChanged()
            
            // 메시지 표시
            MessageManager.getInstance().showInfo(requireContext(), "${unit.name}이(가) 제거되었습니다.")
        }
    }
} 