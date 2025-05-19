package com.example.p40

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.p40.UserManager

/**
 * GameFragment의 비즈니스 로직을 담당하는 ViewModel 클래스
 * GameFragment와 GameView 사이의 통신을 관리하고 UI 상태를 유지합니다.
 */
class GameViewModel : ViewModel() {
    
    // 게임 상태 관련 LiveData
    private val _currentWave = MutableLiveData<Int>(1)
    val currentWave: LiveData<Int> = _currentWave
    
    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score
    
    private val _health = MutableLiveData<Int>(0)
    val health: LiveData<Int> = _health
    
    private val _maxHealth = MutableLiveData<Int>(0)
    val maxHealth: LiveData<Int> = _maxHealth
    
    private val _earnedCoins = MutableLiveData<Int>(0)
    val earnedCoins: LiveData<Int> = _earnedCoins
    
    private val _isPaused = MutableLiveData<Boolean>(false)
    val isPaused: LiveData<Boolean> = _isPaused
    
    // 업그레이드 관련 LiveData
    private val _damageLevel = MutableLiveData<Int>(0)
    val damageLevel: LiveData<Int> = _damageLevel
    
    private val _attackSpeedLevel = MutableLiveData<Int>(0)
    val attackSpeedLevel: LiveData<Int> = _attackSpeedLevel
    
    private val _attackRangeLevel = MutableLiveData<Int>(0)
    val attackRangeLevel: LiveData<Int> = _attackRangeLevel
    
    private val _defenseLevel = MutableLiveData<Int>(0)
    val defenseLevel: LiveData<Int> = _defenseLevel
    
    // 게임 설정 객체
    private var gameConfig: GameConfig? = null
    
    // UserManager, StatsManager 참조
    private var userManager: UserManager? = null
    private var statsManager: StatsManager? = null
    
    /**
     * 필요한 매니저 설정
     */
    fun initialize(userManager: UserManager, statsManager: StatsManager, gameConfig: GameConfig) {
        this.userManager = userManager
        this.statsManager = statsManager
        this.gameConfig = gameConfig
        
        // 초기 획득 코인 초기화
        _earnedCoins.value = 0
    }
    
    /**
     * 현재 웨이브 업데이트
     */
    fun updateWave(wave: Int) {
        _currentWave.value = wave
    }
    
    /**
     * 점수 업데이트
     */
    fun updateScore(score: Int) {
        _score.value = score
    }
    
    /**
     * 체력 업데이트
     */
    fun updateHealth(health: Int, maxHealth: Int) {
        _health.value = health
        _maxHealth.value = maxHealth
    }
    
    /**
     * 코인 획득
     */
    fun addCoins(amount: Int) {
        userManager?.let {
            it.addCoin(amount)
            _earnedCoins.value = _earnedCoins.value?.plus(amount) ?: amount
        }
    }
    
    /**
     * 코인 사용
     */
    fun useCoins(amount: Int): Boolean {
        return userManager?.let {
            if (it.getCoin() >= amount) {
                it.addCoin(-amount)
                true
            } else {
                false
            }
        } ?: false
    }
    
    /**
     * 일시 정지 상태 변경
     */
    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
    }
    
    /**
     * 업그레이드 레벨 업데이트
     */
    fun updateUpgradeLevels(damageLevel: Int, attackSpeedLevel: Int, attackRangeLevel: Int, defenseLevel: Int) {
        _damageLevel.value = damageLevel
        _attackSpeedLevel.value = attackSpeedLevel
        _attackRangeLevel.value = attackRangeLevel
        _defenseLevel.value = defenseLevel
    }
    
    /**
     * 웨이브별 적 체력 계산
     */
    fun getEnemyHealthForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Int {
        return EnemyConfig.getEnemyHealthForWave(wave, isBoss, isFlying)
    }
    
    /**
     * 웨이브별 적 데미지 계산
     */
    fun getEnemyDamageForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Int {
        return EnemyConfig.getEnemyDamageForWave(wave, isBoss, isFlying)
    }
    
    /**
     * 웨이브별 적 속도 계산
     */
    fun getEnemySpeedForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Float {
        return EnemyConfig.getEnemySpeedForWave(wave, isBoss, isFlying)
    }
    
    /**
     * 보스 처치 보상 코인량 계산
     */
    fun getBossKillCoinReward(wave: Int): Int {
        return EnemyConfig.getBossKillCoinReward(wave)
    }
    
    /**
     * 총 웨이브 수 가져오기
     */
    fun getTotalWaves(): Int {
        return gameConfig?.getTotalWaves() ?: 10
    }
    
    /**
     * 통계 업데이트 - 게임 완료
     */
    fun incrementGamesCompleted() {
        statsManager?.incrementGamesCompleted()
    }
    
    /**
     * 통계 업데이트 - 게임 시작
     */
    fun incrementGamesStarted() {
        statsManager?.incrementGamesStarted()
    }
    
    /**
     * 통계 업데이트 - 게임 오버
     */
    fun incrementGamesOver() {
        statsManager?.incrementGamesOver()
    }
} 