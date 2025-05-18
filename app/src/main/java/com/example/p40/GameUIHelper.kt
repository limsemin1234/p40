package com.example.p40

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * GameFragment의 UI 관련 로직을 분리한 헬퍼 클래스
 * 단계적 리팩토링의 첫 번째 단계로, UI 관련 기능을 모듈화합니다.
 */
class GameUIHelper(
    private val context: Context,
    private val gameConfig: GameConfig,
    private val userManager: UserManager,
    private val statsManager: StatsManager,
    private val messageManager: MessageManager
) {
    // 필수 UI 요소만 유지
    private var tvWaveInfo: TextView? = null
    private var tvBuffList: TextView? = null
    private var tvResourceInfo: TextView? = null
    private var tvKillInfo: TextView? = null
    private var unitHealthText: TextView? = null
    private var unitAttackText: TextView? = null
    private var unitAttackSpeedText: TextView? = null
    private var unitRangeText: TextView? = null
    private var enemyHealthText: TextView? = null
    private var enemyAttackText: TextView? = null
    private var enemySpeedText: TextView? = null
    private var bossHealthText: TextView? = null
    private var bossAttackText: TextView? = null
    private var bossSpeedText: TextView? = null
    
    // rootView 참조 추가 (findViewById 접근용)
    private var rootView: View? = null
    
    // 게임 뷰 참조 (점진적 리팩토링을 위해 추가)
    private var gameView: GameView? = null
    
    // 플러시 스킬 매니저 참조 (점진적 리팩토링을 위해 추가)
    private var flushSkillManager: FlushSkillManager? = null
    
    // UI 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateGameInfoUI()
            updateBuffUI()
            updateUnitStatsUI()
            updateEnemyStatsUI()
            updateBossStatsUI()
            updateCoinUI()
            handler.postDelayed(this, 500) // 500ms마다 업데이트
        }
    }
    
    // 업데이트 상태 추적
    private var isUpdating = false
    
    // 현재 웨이브 정보
    private var currentWave = 1
    
    // 게임에서 획득한 코인 추적
    private var earnedCoins = 0
    
    // 현재 열려있는 패널 추적
    private var currentOpenPanel: LinearLayout? = null
    
    /**
     * 게임 뷰 설정
     */
    fun setGameView(gameView: GameView) {
        this.gameView = gameView
    }
    
    /**
     * 플러시 스킬 매니저 설정
     */
    fun setFlushSkillManager(flushSkillManager: FlushSkillManager) {
        this.flushSkillManager = flushSkillManager
    }
    
    /**
     * UI 요소 초기화
     */
    fun initUIElements(view: View) {
        // rootView 저장
        rootView = view
        
        // 기본 텍스트뷰 초기화
        try {
            tvWaveInfo = view.findViewById(R.id.tvWaveInfo)
            tvBuffList = view.findViewById(R.id.tvBuffList)
            tvResourceInfo = view.findViewById(R.id.tvResourceInfo)
            tvKillInfo = view.findViewById(R.id.tvKillInfo)
            
            // 방어 유닛 스탯 요소
            unitHealthText = view.findViewById(R.id.unitHealthText)
            unitAttackText = view.findViewById(R.id.unitAttackText)
            unitAttackSpeedText = view.findViewById(R.id.unitAttackSpeedText)
            unitRangeText = view.findViewById(R.id.unitRangeText)
            
            // 일반 적 스탯 요소
            enemyHealthText = view.findViewById(R.id.enemyHealthText)
            enemyAttackText = view.findViewById(R.id.enemyAttackText)
            enemySpeedText = view.findViewById(R.id.enemySpeedText)
            
            // 보스 스탯 요소
            bossHealthText = view.findViewById(R.id.bossHealthText)
            bossAttackText = view.findViewById(R.id.bossAttackText)
            bossSpeedText = view.findViewById(R.id.bossSpeedText)
            
            // 버프 리스트 텍스트뷰 초기 설정
            tvBuffList?.visibility = View.VISIBLE
            tvBuffList?.text = "활성화된 버프 없음"
        } catch (e: Exception) {
            // findViewById 실패 시 로그 출력 (개발 중 디버깅용)
            e.printStackTrace()
        }
        
        // 초기 업데이트
        updateCoinUI()
        updateUnitStatsUI()
        updateEnemyStatsUI()
        updateBossStatsUI()
    }
    
    /**
     * 게임 정보 UI 업데이트
     */
    fun updateGameInfoUI() {
        // 안전한 UI 업데이트 (null 체크)
        rootView?.let { view ->
            // GameView가 설정되지 않은 경우 기본 정보만 표시
            if (gameView == null) {
                view.findViewById<TextView>(R.id.tvWaveInfo)?.text = "웨이브: $currentWave / ${gameConfig.getTotalWaves()}"
                return
            }
            
            val resource = gameView?.getResource() ?: 0
            val wave = gameView?.getWaveCount() ?: 1
            val enemiesKilled = gameView?.getKillCount() ?: 0
            val totalEnemies = gameView?.getTotalEnemiesInWave() ?: 0
            
            // 현재 웨이브 정보 업데이트
            currentWave = wave
            
            // 웨이브 정보 업데이트
            view.findViewById<TextView>(R.id.tvWaveInfo)?.text = "웨이브: $wave/${gameConfig.getTotalWaves()}"
            
            // 자원 정보 업데이트
            view.findViewById<TextView>(R.id.tvResourceInfo)?.text = "자원: $resource"
            
            // 적 처치 정보 업데이트
            view.findViewById<TextView>(R.id.tvKillInfo)?.text = "처치: $enemiesKilled/$totalEnemies"
        }
    }
    
    /**
     * 버프 정보 UI 업데이트
     */
    fun updateBuffUI() {
        // rootView가 없으면 처리 중단
        val rootView = this.rootView ?: return
        // GameView가 설정되지 않은 경우 처리 중단
        if (gameView == null) return
        
        // 현재 적용된 모든 버프 목록 가져오기
        val buffs = gameView?.getBuffManager()?.getAllBuffs() ?: emptyList()
        
        // 플러시 스킬 감지 및 활성화
        checkAndActivateFlushSkills(buffs)
        
        // 일반 버프 표시 처리
        val displayBuffs = buffs.filter { buff ->
            // 플러시 스킬 버프는 목록에서 제외
            buff.type !in listOf(
                BuffType.HEART_FLUSH_SKILL,
                BuffType.SPADE_FLUSH_SKILL, 
                BuffType.CLUB_FLUSH_SKILL,
                BuffType.DIAMOND_FLUSH_SKILL
            )
        }
        
        // 버프 컨테이너 찾기
        val buffContainer = rootView.findViewById<LinearLayout>(R.id.buffContainer)
        buffContainer?.removeAllViews()
        
        val tvBuffList = rootView.findViewById<TextView>(R.id.tvBuffList)
        
        if (displayBuffs.isNotEmpty() && buffContainer != null) {
            // BuffManager를 통해 생성된 버프 뷰 추가
            displayBuffs.forEach { buff ->
                val buffView = gameView?.getBuffManager()?.createBuffView(buff)
                
                // 레이아웃 파라미터 확인 및 조정
                buffView?.let { view ->
                    val params = view.layoutParams as? LinearLayout.LayoutParams
                        ?: LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    
                    view.layoutParams = params
                    buffContainer.addView(view)
                }
            }
            
            // 기존 텍스트 뷰 숨기기
            tvBuffList?.visibility = View.GONE
            buffContainer.visibility = View.VISIBLE
        } else {
            // 버프가 없을 경우
            tvBuffList?.text = "버프 없음"
            tvBuffList?.visibility = View.VISIBLE
            buffContainer?.visibility = View.GONE
        }
        
        // 레이아웃 강제 갱신
        buffContainer?.parent?.requestLayout()
    }
    
    /**
     * 방어 유닛 스탯 UI 업데이트
     */
    fun updateUnitStatsUI() {
        // rootView가 없으면 처리 중단
        val rootView = this.rootView ?: return
        
        // 기본 공격 속도 값 (1초)
        val defaultAttackSpeed = 1000L
        
        // GameView가 설정되지 않은 경우 기본 스탯 정보 표시
        if (gameView == null) {
            rootView.findViewById<TextView>(R.id.unitHealthText)?.text = "체력: ${statsManager.getHealth()}/${statsManager.getHealth()}"
            rootView.findViewById<TextView>(R.id.unitAttackText)?.text = "공격력: ${statsManager.getAttack()}"
            rootView.findViewById<TextView>(R.id.unitAttackSpeedText)?.text = "공격속도: ${statsManager.getAttackSpeed()}"
            rootView.findViewById<TextView>(R.id.unitRangeText)?.text = "사거리: ${statsManager.getRange()}"
            return
        }
        
        // 체력 정보 업데이트
        val health = gameView?.getUnitHealth() ?: 0
        val maxHealth = gameView?.getUnitMaxHealth() ?: 0
        rootView.findViewById<TextView>(R.id.unitHealthText)?.text = "체력: $health/$maxHealth"
        
        // 공격력 정보 업데이트
        val attack = gameView?.getUnitAttack() ?: 0
        rootView.findViewById<TextView>(R.id.unitAttackText)?.text = "공격력: $attack"
        
        // 공격속도 정보 업데이트 - 안전한 호출 사용
        var attackSpeedText = ""
        gameView?.let {
            try {
                val speed = it.getUnitAttackSpeed()
                if (speed > 0) {
                    val attacksPerSecond = 1000.0 / speed
                    val formattedAttackSpeed = String.format("%.2f", attacksPerSecond)
                    attackSpeedText = "공속: ${formattedAttackSpeed}회/초\n(${speed}ms)"
                } else {
                    attackSpeedText = "공속: 0.00회/초\n(0ms)"
                }
            } catch (e: Exception) {
                // 예외 발생 시 기본값 사용
                attackSpeedText = "공속: 1.00회/초\n(1000ms)"
            }
        } ?: run {
            // gameView가 null인 경우
            attackSpeedText = "공속: 1.00회/초\n(1000ms)"
        }
        rootView.findViewById<TextView>(R.id.unitAttackSpeedText)?.text = attackSpeedText
        
        // 사거리 정보 업데이트
        val attackRange = gameView?.getUnitAttackRange() ?: 0f
        rootView.findViewById<TextView>(R.id.unitRangeText)?.text = "사거리: ${attackRange.toInt()}"
    }
    
    /**
     * 적 스탯 UI 업데이트
     */
    fun updateEnemyStatsUI() {
        // rootView가 없으면 처리 중단
        val rootView = this.rootView ?: return
        
        // GameView가 설정되지 않은 경우 현재 웨이브 기준 정보만 표시
        val wave = gameView?.getWaveCount() ?: currentWave
        
        // GameConfig를 통해 웨이브별 적 스탯 정보 계산
        val normalHealth = EnemyConfig.getEnemyHealthForWave(wave)
        val normalDamage = EnemyConfig.getEnemyDamageForWave(wave, false)
        val normalSpeed = EnemyConfig.getEnemySpeedForWave(wave)
        
        // 공중적 정보 (6웨이브부터 등장하는 경우만)
        val showFlyingInfo = wave >= gameConfig.FLYING_ENEMY_WAVE_THRESHOLD
        val flyingHealth = if (showFlyingInfo) {
            EnemyConfig.getEnemyHealthForWave(wave, false, true)
        } else 0
        val flyingDamage = if (showFlyingInfo) {
            EnemyConfig.getEnemyDamageForWave(wave, false, true)
        } else 0
        val flyingSpeed = if (showFlyingInfo) {
            EnemyConfig.getEnemySpeedForWave(wave, false, true)
        } else 0f
        
        // 체력 정보 업데이트
        if (showFlyingInfo) {
            rootView.findViewById<TextView>(R.id.enemyHealthText)?.text = "체력: $normalHealth\n공중적: $flyingHealth"
        } else {
            rootView.findViewById<TextView>(R.id.enemyHealthText)?.text = "체력: $normalHealth"
        }
        
        // 공격력 정보 업데이트
        if (showFlyingInfo) {
            rootView.findViewById<TextView>(R.id.enemyAttackText)?.text = "공격력: $normalDamage\n공중적: $flyingDamage"
        } else {
            rootView.findViewById<TextView>(R.id.enemyAttackText)?.text = "공격력: $normalDamage"
        }
        
        // 이동속도 정보 업데이트
        val formattedNormalSpeed = String.format("%.2f", normalSpeed)
        if (showFlyingInfo) {
            val formattedFlyingSpeed = String.format("%.2f", flyingSpeed)
            rootView.findViewById<TextView>(R.id.enemySpeedText)?.text = "이동속도: $formattedNormalSpeed\n공중적: $formattedFlyingSpeed"
        } else {
            rootView.findViewById<TextView>(R.id.enemySpeedText)?.text = "이동속도: $formattedNormalSpeed"
        }
    }
    
    /**
     * 보스 스탯 UI 업데이트
     */
    fun updateBossStatsUI() {
        // rootView가 없으면 처리 중단
        val rootView = this.rootView ?: return
        
        // GameView가 설정되지 않은 경우 현재 웨이브 기준 정보만 표시
        val wave = gameView?.getWaveCount() ?: currentWave
        
        // GameConfig를 통해 웨이브별 보스 스탯 정보 계산
        val maxHealth = EnemyConfig.getEnemyHealthForWave(wave, true)
        val damage = EnemyConfig.getEnemyDamageForWave(wave, true)
        val speed = EnemyConfig.getEnemySpeedForWave(wave, true)
        
        // 현재 보스 체력 정보 가져오기
        val currentBossHealth = gameView?.getCurrentBossHealth() ?: 0
        
        // 체력 정보 업데이트 - 현재/최대 체력 표시 형식으로 변경
        val healthText = if (currentBossHealth > 0) {
            "체력: $currentBossHealth/$maxHealth"
        } else {
            "체력: $maxHealth"
        }
        rootView.findViewById<TextView>(R.id.bossHealthText)?.text = healthText
        
        // 공격력 정보 업데이트
        rootView.findViewById<TextView>(R.id.bossAttackText)?.text = "공격력: $damage"
        
        // 이동속도 정보 업데이트
        val formattedSpeed = String.format("%.2f", speed)
        rootView.findViewById<TextView>(R.id.bossSpeedText)?.text = "이동속도: $formattedSpeed"
    }
    
    /**
     * 코인 UI 업데이트
     */
    fun updateCoinUI() {
        // rootView가 없으면 처리 중단
        rootView?.findViewById<TextView>(R.id.tvCoinInfo)?.text = "코인: ${userManager.getCoin()} (+$earnedCoins)"
    }
    
    /**
     * UI 업데이트 시작
     */
    fun startUiUpdates() {
        if (!isUpdating) {
            isUpdating = true
            handler.post(uiUpdateRunnable)
        }
    }
    
    /**
     * UI 업데이트 중지
     */
    fun stopUiUpdates() {
        isUpdating = false
        handler.removeCallbacks(uiUpdateRunnable)
    }
    
    /**
     * 리소스 정리 메서드 - 메모리 누수 방지
     */
    fun clear() {
        // 모든 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null)
        isUpdating = false
        
        // 뷰 참조 제거
        rootView = null
        tvWaveInfo = null
        tvBuffList = null
        tvResourceInfo = null
        tvKillInfo = null
        unitHealthText = null
        unitAttackText = null
        unitAttackSpeedText = null
        unitRangeText = null
        enemyHealthText = null
        enemyAttackText = null
        enemySpeedText = null
        bossHealthText = null
        bossAttackText = null
        bossSpeedText = null
        
        // 게임 뷰 참조 해제
        gameView = null
        
        // 플러시 스킬 매니저 참조 해제
        flushSkillManager = null
        
        // 패널 참조 해제
        currentOpenPanel = null
    }
    
    /**
     * 패널 토글 (열기/닫기)
     */
    fun togglePanel(panel: LinearLayout) {
        // 이미 열려있는 패널 닫기
        currentOpenPanel?.let {
            if (it != panel) {
                it.visibility = View.GONE
            }
        }
        
        // 선택한 패널 토글
        if (panel.visibility == View.VISIBLE) {
            panel.visibility = View.GONE
            currentOpenPanel = null
        } else {
            panel.visibility = View.VISIBLE
            currentOpenPanel = panel
        }
    }
    
    /**
     * 현재 웨이브 설정
     */
    fun setCurrentWave(wave: Int) {
        currentWave = wave
        updateGameInfoUI()
        updateEnemyStatsUI()
        updateBossStatsUI()
    }
    
    /**
     * 획득한 코인 증가
     */
    fun addEarnedCoins(coins: Int) {
        earnedCoins += coins
        updateCoinUI()
    }
    
    /**
     * 획득한 코인 설정
     */
    fun setEarnedCoins(coins: Int) {
        earnedCoins = coins
        updateCoinUI()
    }
    
    /**
     * 버프 타입에 따른 문자열 반환
     */
    private fun getBuffTypeString(type: BuffType): String {
        return when (type) {
            BuffType.MISSILE_DAMAGE -> "데미지 증가"
            BuffType.ATTACK_SPEED -> "공격속도 증가"
            BuffType.ATTACK_RANGE -> "사거리 증가"
            BuffType.HEALTH -> "체력 증가"
            BuffType.HEART_FLUSH_SKILL -> "하트 플러시 스킬"
            BuffType.SPADE_FLUSH_SKILL -> "스페이드 플러시 스킬"
            BuffType.CLUB_FLUSH_SKILL -> "클로버 플러시 스킬"
            BuffType.DIAMOND_FLUSH_SKILL -> "다이아 플러시 스킬"
            else -> "알 수 없음"
        }
    }

    
    /**
     * 플러시 스킬 감지 및 활성화
     */
    private fun checkAndActivateFlushSkills(buffs: List<Buff>) {
        // 플러시 스킬 매니저가 설정되지 않은 경우 처리 중단
        if (flushSkillManager == null || gameView == null) return
        
        // 각 문양별 플러시 스킬 버프가 있는지 확인
        // 버프가 있으면 해당 스킬 활성화
        
        // 하트 플러시 스킬
        if (buffs.any { it.type == BuffType.HEART_FLUSH_SKILL }) {
            flushSkillManager?.activateFlushSkill(CardSuit.HEART)
            // 버프 제거 (1회성)
            gameView?.getBuffManager()?.removeBuff(BuffType.HEART_FLUSH_SKILL)
        }
        
        // 스페이드 플러시 스킬
        if (buffs.any { it.type == BuffType.SPADE_FLUSH_SKILL }) {
            flushSkillManager?.activateFlushSkill(CardSuit.SPADE)
            // 버프 제거 (1회성)
            gameView?.getBuffManager()?.removeBuff(BuffType.SPADE_FLUSH_SKILL)
        }
        
        // 클로버 플러시 스킬
        if (buffs.any { it.type == BuffType.CLUB_FLUSH_SKILL }) {
            flushSkillManager?.activateFlushSkill(CardSuit.CLUB)
            // 버프 제거 (1회성)
            gameView?.getBuffManager()?.removeBuff(BuffType.CLUB_FLUSH_SKILL)
        }
        
        // 다이아 플러시 스킬
        if (buffs.any { it.type == BuffType.DIAMOND_FLUSH_SKILL }) {
            flushSkillManager?.activateFlushSkill(CardSuit.DIAMOND)
            // 버프 제거 (1회성)
            gameView?.getBuffManager()?.removeBuff(BuffType.DIAMOND_FLUSH_SKILL)
        }
    }
    
    /**
     * 테스트용 더미 버프 목록 가져오기
     * 실제 구현에서는 제거하고 GameFragment에서 실제 버프를 가져와야 함
     */
    private fun getActiveBuffs(): List<Buff> {
        // 테스트용 더미 데이터, 실제 구현 시 교체 필요
        return emptyList()
    }
} 