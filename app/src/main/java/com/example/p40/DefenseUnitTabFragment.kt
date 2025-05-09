package com.example.p40

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        
        // 유닛 데이터 초기화
        initDefenseUnits()
        
        // 어댑터 설정
        defenseUnitAdapter = DefenseUnitAdapter(defenseUnits) { unit ->
            onUnitBuyClicked(unit)
        }
        recyclerView.adapter = defenseUnitAdapter
    }
    
    // 디펜스유닛 초기화
    private fun initDefenseUnits() {
        // 디펜스유닛 목록을 비웁니다 (기존 유닛들을 추가하지 않음)
        defenseUnits.clear()
        
        // 안내 메시지를 표시하기 위한 더미 아이템 추가
        val emptyMessage = view?.findViewById<TextView>(R.id.tvEmptyMessage)
        emptyMessage?.visibility = View.VISIBLE
        emptyMessage?.text = "현재 구매 가능한 디펜스유닛이 없습니다."
    }
    
    // 유닛 구매 버튼 클릭 처리
    private fun onUnitBuyClicked(unit: ShopDefenseUnit) {
        // 이미 구매한 유닛인지 확인
        if (unit.isPurchased) {
            MessageManager.getInstance().showInfo(requireContext(), "이미 구매한 유닛입니다.")
            return
        }
        
        // 구매 확인 다이얼로그
        AlertDialog.Builder(requireContext())
            .setTitle("디펜스유닛 구매")
            .setMessage("${unit.name}을(를) ${unit.price} 코인에 구매하시겠습니까?")
            .setPositiveButton("구매") { _, _ ->
                purchaseUnit(unit)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    // 유닛 구매 처리
    private fun purchaseUnit(unit: ShopDefenseUnit) {
        // 코인 차감
        if (userManager.useCoin(unit.price)) {
            // 구매한 유닛을 유저 데이터에 추가
            userManager.addPurchasedDefenseUnit(unit.id)
            
            // UI 업데이트
            unit.isPurchased = true
            defenseUnitAdapter.updateUnitPurchased(unit.id)
            
            // 상위 프래그먼트 코인 UI 업데이트
            (parentFragment as? CardShopFragment)?.updateCurrencyUI()
            
            MessageManager.getInstance().showSuccess(requireContext(), "${unit.name} 구매 완료!")
        } else {
            // 코인 부족
            MessageManager.getInstance().showError(requireContext(), "코인이 부족합니다.")
        }
    }
} 