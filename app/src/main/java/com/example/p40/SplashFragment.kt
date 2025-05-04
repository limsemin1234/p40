package com.example.p40

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val splashDuration = 2000L // 2초 동안 스플래시 화면 표시

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 일정 시간 후 메인 메뉴 화면으로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) { // Fragment가 여전히 Activity에 attached된 상태인지 확인
                findNavController().navigate(R.id.action_splashFragment_to_mainMenuFragment)
            }
        }, splashDuration)
    }
} 