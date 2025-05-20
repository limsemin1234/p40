package com.example.p40

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

/**
 * 게임 다이얼로그를 관리하는 클래스
 * GameFragment에서 다이얼로그 관련 로직을 분리했습니다.
 */
class GameDialogManager(
    private val context: Context,
    private val gameView: GameView,
    private val messageManager: MessageManager,
    private val userManager: UserManager,
    private val statsManager: StatsManager,
    private val lifecycleOwner: LifecycleOwner?,
    private val navController: NavController?,
    private val gameConfig: GameConfig
) {
    // 게임에서 획득한 코인 추적
    private var earnedCoins = 0
    
    // 리소스 정리 콜백
    private var cleanupResourcesCallback: (() -> Unit)? = null
    
    // 게임 오버 콜백
    private var onGameOverCallback: ((Int, Int) -> Unit)? = null
    
    // 포커 핸드 효과 적용 콜백
    private var onPokerHandAppliedCallback: ((PokerHand) -> Unit)? = null
    
    /**
     * 콜백 설정
     */
    fun setCleanupResourcesCallback(callback: () -> Unit) {
        cleanupResourcesCallback = callback
    }
    
    fun setGameOverCallback(callback: (Int, Int) -> Unit) {
        onGameOverCallback = callback
    }
    
    fun setPokerHandAppliedCallback(callback: (PokerHand) -> Unit) {
        onPokerHandAppliedCallback = callback
    }
    
    /**
     * 획득한 코인 설정
     */
    fun setEarnedCoins(coins: Int) {
        earnedCoins = coins
    }
    
    /**
     * 일시정지 다이얼로그 표시
     */
    fun showPauseDialog(isPaused: Boolean, uiUpdateRunnable: Runnable, handler: Handler) {
        // 다이얼로그 생성
        val dialog = Dialog(context)
        
        // 레이아웃 설정
        dialog.setContentView(R.layout.dialog_pause_menu)
        dialog.setCancelable(false)
        
        // 버튼 설정
        // 1. 게임 계속하기 버튼
        val btnResume = dialog.findViewById<Button>(R.id.btnResume)
        btnResume.setOnClickListener {
            dialog.dismiss()
            
            // 게임 재개
            gameView.resume()
            handler.post(uiUpdateRunnable)
        }
        
        // 2. 끝내기 버튼 (게임 오버로 처리)
        val btnExitGame = dialog.findViewById<Button>(R.id.btnExitGame)
        btnExitGame.setOnClickListener {
            dialog.dismiss()
            
            // 게임 리소스 정리
            cleanupResourcesCallback?.invoke()
            
            // 현재 웨이브와 자원 정보를 활용하여 게임 오버 처리
            val currentResource = gameView.getResource()
            val currentWave = gameView.getWaveCount()
            
            // 현재 획득한 코인 정보를 가져오기 (GameUIHelper에서 표시하는 코인 값)
            val tvCoinInfo = userManager.getCoin()
            val previousCoin = try {
                // UserManager에 저장된 이전 코인 값과의 차이를 계산
                val gameStartTotalCoins = statsManager.getInitialGameCoins()
                val earnedInGame = tvCoinInfo - gameStartTotalCoins
                if (earnedInGame > 0) earnedInGame else earnedCoins
            } catch (e: Exception) {
                // 예외 발생 시 기존 earnedCoins 값 사용
                earnedCoins
            }
            
            // 획득한 코인 설정
            setEarnedCoins(previousCoin)
            
            // 게임 오버 다이얼로그 표시
            onGameOver(currentResource, currentWave)
        }
        
        // 다이얼로그 표시
        dialog.show()
    }
    
    /**
     * 게임 오버 다이얼로그 표시
     */
    fun onGameOver(resource: Int, waveCount: Int) {
        // 사용자 정의 콜백 호출
        onGameOverCallback?.invoke(resource, waveCount)
        
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_game_over)
        
        // 점수와 웨이브 표시
        val tvGameOverScore = dialog.findViewById<TextView>(R.id.tvGameOverScore)
        val tvGameOverWave = dialog.findViewById<TextView>(R.id.tvGameOverWave)
        
        tvGameOverScore.text = "최종 자원: $resource"
        tvGameOverWave.text = "도달한 웨이브: $waveCount"
        
        // 코인 관련 텍스트뷰 - 획득한 코인 표시
        val tvGameOverCoins = dialog.findViewById<TextView>(R.id.tvGameOverCoins)
        tvGameOverCoins.text = "획득한 코인: $earnedCoins"
        
        // 종료 버튼 - 앱 종료
        val btnExit = dialog.findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            dialog.dismiss()
            (context as? android.app.Activity)?.finish()
        }
        
        // 메인 메뉴 버튼
        val btnMainMenu = dialog.findViewById<Button>(R.id.btnMainMenu)
        btnMainMenu.setOnClickListener {
            dialog.dismiss()
            
            // 게임 리소스 정리
            cleanupResourcesCallback?.invoke()
            
            // 게임 완전 초기화 (1웨이브부터 시작하도록)
            gameView.resetGame(gameConfig)
            
            // 메인 메뉴로 이동
            navController?.navigate(R.id.action_gameFragment_to_mainMenuFragment)
        }
        
        dialog.setCancelable(false)
        
        // 프래그먼트가 분리될 때 다이얼로그 닫기
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }
            }
        })
        
        dialog.show()
    }
    
    /**
     * 포커 카드 다이얼로그 표시
     */
    fun showPokerCardsDialog(waveNumber: Int) {
        // 다이얼로그 대신 카드 패널 활성화
        val gameFragment = context as? GameFragment
        if (gameFragment != null) {
            // 포커 카드 매니저 접근
            val pokerCardManager = gameFragment.getPokerCardManager()
            
            // 카드 게임 시작
            pokerCardManager.startPokerCards(waveNumber)
            
            // 카드 패널 표시
            gameFragment.showCardPanel()
        } else {
            // 오류 메시지
            MessageManager.getInstance().showError("카드 패널을 표시할 수 없습니다.")
        }
    }
    
    /**
     * 포커 족보 효과 적용
     */
    private fun applyPokerHandEffect(pokerHand: PokerHand) {
        // GameView에 포커 족보 효과 적용
        gameView.applyPokerHandEffect(pokerHand)
        
        // 콜백 호출
        onPokerHandAppliedCallback?.invoke(pokerHand)
    }
    
    /**
     * 레벨 클리어 다이얼로그 표시
     */
    fun showLevelClearedDialog(wave: Int, score: Int) {
        // 게임 일시 정지
        gameView.pause()
        
        // 레벨 클리어 보상 (1단계 난이도 클리어 시 500 코인)
        val levelClearReward = 500
        
        // 코인 보상 지급
        userManager.addCoin(levelClearReward)
        earnedCoins += levelClearReward
        
        // 레벨 클리어 다이얼로그 표시
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_level_clear)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 텍스트 설정
        val tvLevelTitle = dialog.findViewById<TextView>(R.id.tvLevelTitle)
        val tvClearedWaves = dialog.findViewById<TextView>(R.id.tvClearedWaves)
        val tvRewardCoins = dialog.findViewById<TextView>(R.id.tvRewardCoins)
        
        tvLevelTitle.text = "1단계 난이도"
        tvClearedWaves.text = "$wave 웨이브 클리어!"
        tvRewardCoins.text = "$levelClearReward 코인"
        
        // 메인화면으로 버튼
        val btnToMainMenu = dialog.findViewById<Button>(R.id.btnToMainMenu)
        btnToMainMenu.setOnClickListener {
            dialog.dismiss()
            
            // 게임 리소스 정리
            cleanupResourcesCallback?.invoke()
            
            // 게임 완전 초기화 (1웨이브부터 시작하도록)
            gameView.resetGame(gameConfig)
            
            // 메인 메뉴로 이동
            navController?.navigate(R.id.action_gameFragment_to_mainMenuFragment)
        }
        
        dialog.setCancelable(false)
        dialog.show()
        
        // 통계 업데이트 - 게임 클리어 횟수 증가
        statsManager.incrementGamesCompleted()
    }
} 