package com.example.p40

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.MessageManager

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    // UserManager 추가
    private lateinit var userManager: UserManager
    private lateinit var messageManager: MessageManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UserManager 초기화
        userManager = UserManager.getInstance(requireContext())
        messageManager = MessageManager.getInstance()
        messageManager.init(view.findViewById(android.R.id.content) ?: view as ViewGroup)
        
        // UserManager에서 코인 정보 불러오기
        updateCoinUI(view)
        
        // 게임 로비 버튼 클릭 시 로비 화면으로 이동
        val cardLobby = view.findViewById<CardView>(R.id.cardLobby)
        cardLobby.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_game)
        }

        // 카드 구매 버튼 클릭 시
        val cardShop = view.findViewById<CardView>(R.id.cardShop)
        cardShop.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_cardShop)
        }
        
        // 스탯 강화 버튼 클릭 시 스탯 강화 화면으로 이동
        val cardStatsUpgrade = view.findViewById<CardView>(R.id.cardStatsUpgrade)
        cardStatsUpgrade.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_statsUpgrade)
        }

        // 덱 구성 버튼 클릭 시 덱 구성 화면으로 이동
        val cardDeckBuilder = view.findViewById<CardView>(R.id.cardDeckBuilder)
        cardDeckBuilder.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_deckBuilder)
        }

        // 게임 종료 버튼 클릭 시 앱 종료
        val cardExit = view.findViewById<CardView>(R.id.cardExit)
        cardExit.setOnClickListener {
            activity?.finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 UserManager에서 코인 정보 갱신
        view?.let { updateCoinUI(it) }
    }
    
    // 코인 UI 업데이트 (UserManager 사용)
    private fun updateCoinUI(view: View) {
        val tvCoinAmount = view.findViewById<TextView>(R.id.tvCoinAmount)
        tvCoinAmount.text = "코인: ${userManager.getCoin()}"
    }

    private fun showJokerCardsDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_joker_cards)
        
        // 윈도우 크기 조정
        val window = dialog.window
        if (window != null) {
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.95).toInt()
            
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(android.view.Gravity.CENTER)
        }
        
        // 하트 조커 설정
        val heartRankPicker = dialog.findViewById<NumberPicker>(R.id.heartRankPicker)
        val tvHeartJokerRank = dialog.findViewById<TextView>(R.id.tvHeartJokerRank)
        setupRankPicker(heartRankPicker, tvHeartJokerRank)
        
        // 스페이드 조커 설정
        val spadeRankPicker = dialog.findViewById<NumberPicker>(R.id.spadeRankPicker)
        val tvSpadeJokerRank = dialog.findViewById<TextView>(R.id.tvSpadeJokerRank)
        setupRankPicker(spadeRankPicker, tvSpadeJokerRank)
        
        // 다이아 조커 설정
        val diamondRankPicker = dialog.findViewById<NumberPicker>(R.id.diamondRankPicker)
        val tvDiamondJokerRank = dialog.findViewById<TextView>(R.id.tvDiamondJokerRank)
        setupRankPicker(diamondRankPicker, tvDiamondJokerRank)
        
        // 클로버 조커 설정
        val clubRankPicker = dialog.findViewById<NumberPicker>(R.id.clubRankPicker)
        val tvClubJokerRank = dialog.findViewById<TextView>(R.id.tvClubJokerRank)
        setupRankPicker(clubRankPicker, tvClubJokerRank)
        
        // 취소 버튼
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // 구매하기 버튼
        val btnBuyCards = dialog.findViewById<Button>(R.id.btnBuyCards)
        btnBuyCards.setOnClickListener {
            // 선택된 값으로 조커 카드 생성
            val heartJoker = createJokerCard(CardSuit.HEART, getCardRankByValue(heartRankPicker.value))
            val spadeJoker = createJokerCard(CardSuit.SPADE, getCardRankByValue(spadeRankPicker.value))
            val diamondJoker = createJokerCard(CardSuit.DIAMOND, getCardRankByValue(diamondRankPicker.value))
            val clubJoker = createJokerCard(CardSuit.CLUB, getCardRankByValue(clubRankPicker.value))
            
            // 생성된 카드를 덱에 추가
            val message = "하트 조커(${heartJoker.rank.getName()}), " +
                        "스페이드 조커(${spadeJoker.rank.getName()}), " +
                        "다이아 조커(${diamondJoker.rank.getName()}), " +
                        "클로버 조커(${clubJoker.rank.getName()}) 구매 완료!"
            
            messageManager.showSuccess(message)
            
            // 실제 구현에서는 여기서 덱에 추가하거나 인벤토리에 저장 필요
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun setupRankPicker(rankPicker: NumberPicker, rankTextView: TextView) {
        // 랭크 선택기 설정 (A부터 K까지)
        rankPicker.minValue = 1
        rankPicker.maxValue = 13
        
        val rankValues = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        rankPicker.displayedValues = rankValues
        
        // 기본값은 A (1)
        rankPicker.value = 1
        
        // 값 변경 시 카드 미리보기 업데이트
        rankPicker.setOnValueChangedListener { _, _, newVal ->
            rankTextView.text = rankValues[newVal - 1]
        }
    }
    
    private fun getCardRankByValue(value: Int): CardRank {
        return when (value) {
            1 -> CardRank.ACE
            2 -> CardRank.TWO
            3 -> CardRank.THREE
            4 -> CardRank.FOUR
            5 -> CardRank.FIVE
            6 -> CardRank.SIX
            7 -> CardRank.SEVEN
            8 -> CardRank.EIGHT
            9 -> CardRank.NINE
            10 -> CardRank.TEN
            11 -> CardRank.JACK
            12 -> CardRank.QUEEN
            13 -> CardRank.KING
            else -> CardRank.ACE // 기본값
        }
    }
    
    private fun createJokerCard(suit: CardSuit, rank: CardRank): Card {
        // 문양과 랭크를 가진 카드 생성 (별 조커로 대체)
        return Card(suit, rank, isJoker = true)
    }
}
