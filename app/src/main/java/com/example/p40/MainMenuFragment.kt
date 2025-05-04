package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 게임 시작 버튼 클릭 시 게임 화면으로 이동
        val btnStartGame = view.findViewById<Button>(R.id.btnStartGame)
        btnStartGame.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_game)
        }

        // 설정 버튼 클릭 시 설정 화면으로 이동 (추가적인 네비게이션 필요)
        val btnSettings = view.findViewById<Button>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            // 예시: findNavController().navigate(R.id.action_mainMenu_to_settings)
        }

        // 게임 종료 버튼 클릭 시 앱 종료
        val btnExit = view.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            activity?.finish()
        }
    }
}
