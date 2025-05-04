package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 게임 로비 버튼 클릭 시 로비 화면으로 이동
        val btnLobby = view.findViewById<Button>(R.id.btnLobby)
        btnLobby.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_game)
        }

        // 카드 구매 버튼 클릭 시
        val btnBuyCards = view.findViewById<Button>(R.id.btnBuyCards)
        btnBuyCards.setOnClickListener {
            Toast.makeText(requireContext(), "카드 구매 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
        }

        // 덱 구성 버튼 클릭 시 덱 구성 화면으로 이동
        val btnDeckBuilder = view.findViewById<Button>(R.id.btnDeckBuilder)
        btnDeckBuilder.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_deckBuilder)
        }

        // 게임 종료 버튼 클릭 시 앱 종료
        val btnExit = view.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            activity?.finish()
        }
    }

    private fun showPokerDeckBuilder() {
        val dialog = PokerCardsDialog(requireContext(), 0) { pokerHand ->
            Toast.makeText(requireContext(), 
                "선택한 족보: ${pokerHand.handName}", 
                Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }
}
