package com.example.p40

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LobbyFragment : Fragment(R.layout.fragment_lobby) {

    private lateinit var levelAdapter: GameLevelAdapter
    private lateinit var userManager: UserManager
    private lateinit var tvCurrency: TextView
    private var selectedLevel: GameLevel? = null
    
    // 게임 레벨 데이터
    private val gameLevels = listOf(
        GameLevel(
            id = 1,
            number = 1,
            title = "초급 난이도",
            description = "10 웨이브 구성, 보통 난이도",
            totalWaves = 10,
            difficulty = 1.0f
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        
        // 코인 정보 표시 텍스트뷰 초기화
        tvCurrency = view.findViewById(R.id.tvCurrency)
        
        // 코인 정보 업데이트
        updateCoinUI()
        
        // RecyclerView 설정
        val rvLevels = view.findViewById<RecyclerView>(R.id.rvLevels)
        rvLevels.layoutManager = LinearLayoutManager(requireContext())
        
        // 어댑터 설정
        levelAdapter = GameLevelAdapter(gameLevels) { level ->
            selectedLevel = level
        }
        rvLevels.adapter = levelAdapter
        
        // 기본적으로 첫 번째 레벨 선택
        selectedLevel = gameLevels.firstOrNull()
        
        // 상단바 뒤로가기 버튼 설정
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
        
        // 게임 시작 버튼
        val btnStartGame = view.findViewById<Button>(R.id.btnStartGame)
        btnStartGame.setOnClickListener {
            selectedLevel?.let { level ->
                // 선택된 레벨 정보를 담아서 게임 화면으로 이동
                val bundle = Bundle().apply {
                    putInt("levelId", level.id)
                    putInt("totalWaves", level.totalWaves)
                    putFloat("difficulty", level.difficulty)
                }
                findNavController().navigate(R.id.action_lobbyFragment_to_gameFragment, bundle)
            }
        }
        
        // 메인 메뉴로 돌아가기 버튼
        val btnBackToMain = view.findViewById<Button>(R.id.btnBackToMain)
        btnBackToMain.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    // 코인 정보 업데이트
    private fun updateCoinUI() {
        tvCurrency.text = "보유 코인: ${userManager.getCoin()}"
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 코인 정보 갱신
        updateCoinUI()
    }
} 