package com.example.p40

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 상점 탭 어댑터 - 카드와 디펜스유닛 탭을 관리
 */
class ShopTabAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragments = listOf(
        CardTabFragment(), // 카드 탭
        DefenseUnitTabFragment() // 디펜스유닛 탭
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
} 