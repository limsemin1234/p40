package com.example.p40

import android.content.Context
import android.content.SharedPreferences

class StatsManager private constructor(context: Context) {
    
    companion object {
        private const val PREF_NAME = "stats_prefs"
        
        // 스탯 키 정의
        private const val KEY_HEALTH = "health"
        private const val KEY_ATTACK = "attack"
        private const val KEY_ATTACK_SPEED = "attack_speed"
        private const val KEY_RANGE = "range"
        
        // 스탯 레벨 키 정의
        private const val KEY_HEALTH_LEVEL = "health_level"
        private const val KEY_ATTACK_LEVEL = "attack_level"
        private const val KEY_ATTACK_SPEED_LEVEL = "attack_speed_level"
        private const val KEY_RANGE_LEVEL = "range_level"
        
        // 게임 시작 시 코인 추적을 위한 키
        private const val KEY_INITIAL_GAME_COINS = "initial_game_coins"
        
        // 기본 스탯 값 - GameConfig에서 가져오기
        private val DEFAULT_HEALTH = GameConfig.DEFENSE_UNIT_INITIAL_HEALTH
        private val DEFAULT_ATTACK = GameConfig.MISSILE_DAMAGE
        private val DEFAULT_ATTACK_SPEED = 1.0f
        private val DEFAULT_RANGE = GameConfig.DEFENSE_UNIT_ATTACK_RANGE.toInt()
        
        // 싱글톤 인스턴스
        private var instance: StatsManager? = null
        
        // 플레이 통계 키
        const val KEY_GAMES_STARTED = "games_started"
        const val KEY_GAMES_COMPLETED = "games_completed"
        const val KEY_GAMES_OVER = "games_over"
        
        // 상태 변경 브로드캐스트 키
        const val STATS_CHANGED_ACTION = "com.example.p40.STATS_CHANGED"
        const val EXTRA_STAT_NAME = "stat_name"
        const val EXTRA_STAT_VALUE = "stat_value"
        
        // 통계 변경 상수 - 브로드캐스트용
        const val STAT_GAMES_STARTED = "games_started"
        const val STAT_GAMES_COMPLETED = "games_completed"
        const val STAT_GAMES_OVER = "games_over"
        
        /**
         * 싱글톤 인스턴스 가져오기
         */
        fun getInstance(context: Context): StatsManager {
            if (instance == null) {
                instance = StatsManager(context.applicationContext)
            }
            return instance!!
        }
    }
    
    // SharedPreferences 객체
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // 체력 관련
    fun getHealth(): Int {
        return prefs.getInt(KEY_HEALTH, DEFAULT_HEALTH)
    }
    
    fun setHealth(health: Int) {
        prefs.edit().putInt(KEY_HEALTH, health).apply()
    }
    
    fun getHealthLevel(): Int {
        return prefs.getInt(KEY_HEALTH_LEVEL, 0)
    }
    
    fun upgradeHealth(amount: Int = GameConfig.STATS_HEALTH_UPGRADE_AMOUNT): Boolean {
        val currentLevel = getHealthLevel()
        
        // 최대 레벨 체크
        if (currentLevel >= GameConfig.STATS_MAX_LEVEL) {
            return false
        }
        
        val currentHealth = getHealth()
        setHealth(currentHealth + amount)
        prefs.edit().putInt(KEY_HEALTH_LEVEL, currentLevel + 1).apply()
        return true
    }
    
    // 공격력 관련
    fun getAttack(): Int {
        return prefs.getInt(KEY_ATTACK, DEFAULT_ATTACK)
    }
    
    fun setAttack(attack: Int) {
        prefs.edit().putInt(KEY_ATTACK, attack).apply()
    }
    
    fun getAttackLevel(): Int {
        return prefs.getInt(KEY_ATTACK_LEVEL, 0)
    }
    
    fun upgradeAttack(amount: Int = GameConfig.STATS_ATTACK_UPGRADE_AMOUNT): Boolean {
        val currentLevel = getAttackLevel()
        
        // 최대 레벨 체크
        if (currentLevel >= GameConfig.STATS_MAX_LEVEL) {
            return false
        }
        
        val currentAttack = getAttack()
        setAttack(currentAttack + amount)
        prefs.edit().putInt(KEY_ATTACK_LEVEL, currentLevel + 1).apply()
        return true
    }
    
    // 공격 속도 관련
    fun getAttackSpeed(): Float {
        return prefs.getFloat(KEY_ATTACK_SPEED, DEFAULT_ATTACK_SPEED)
    }
    
    fun setAttackSpeed(attackSpeed: Float) {
        prefs.edit().putFloat(KEY_ATTACK_SPEED, attackSpeed).apply()
    }
    
    fun getAttackSpeedLevel(): Int {
        return prefs.getInt(KEY_ATTACK_SPEED_LEVEL, 0)
    }
    
    fun upgradeAttackSpeed(amount: Int = GameConfig.STATS_ATTACK_SPEED_UPGRADE_AMOUNT): Boolean {
        val currentLevel = getAttackSpeedLevel()
        
        // 최대 레벨 체크
        if (currentLevel >= GameConfig.STATS_MAX_LEVEL) {
            return false
        }
        
        val currentAttackSpeed = getAttackSpeed()
        
        // 밀리초 기준으로 계산하기 위해 현재 초당 공격 횟수를 밀리초로 변환
        val currentMs = (1000 / currentAttackSpeed).toInt()
        
        // amount(ms)만큼 감소시키되 최소값(250ms)보다 작아지지 않도록 함
        val newMs = maxOf(250, currentMs - amount)
        
        // 다시 초당 공격 횟수로 변환하여 저장
        val newAttackSpeed = 1000f / newMs
        
        setAttackSpeed(newAttackSpeed)
        prefs.edit().putInt(KEY_ATTACK_SPEED_LEVEL, currentLevel + 1).apply()
        return true
    }
    
    // 사거리 관련
    fun getRange(): Int {
        return prefs.getInt(KEY_RANGE, DEFAULT_RANGE)
    }
    
    fun setRange(range: Int) {
        prefs.edit().putInt(KEY_RANGE, range).apply()
    }
    
    fun getRangeLevel(): Int {
        return prefs.getInt(KEY_RANGE_LEVEL, 0)
    }
    
    fun upgradeRange(amount: Int = GameConfig.STATS_RANGE_UPGRADE_AMOUNT): Boolean {
        val currentLevel = getRangeLevel()
        
        // 최대 레벨 체크
        if (currentLevel >= GameConfig.STATS_MAX_LEVEL) {
            return false
        }
        
        val currentRange = getRange()
        setRange(currentRange + amount)
        prefs.edit().putInt(KEY_RANGE_LEVEL, currentLevel + 1).apply()
        return true
    }
    
    // 강화 비용 계산 메서드를 스탯별로 개별화
    
    // 체력 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getHealthUpgradeCost(): Int {
        return GameConfig.STATS_HEALTH_BASE_COST + (getHealthLevel() * GameConfig.STATS_HEALTH_COST_INCREASE)
    }
    
    // 공격력 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getAttackUpgradeCost(): Int {
        return GameConfig.STATS_ATTACK_BASE_COST + (getAttackLevel() * GameConfig.STATS_ATTACK_COST_INCREASE)
    }
    
    // 공격 속도 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getAttackSpeedUpgradeCost(): Int {
        return GameConfig.STATS_ATTACK_SPEED_BASE_COST + (getAttackSpeedLevel() * GameConfig.STATS_ATTACK_SPEED_COST_INCREASE)
    }
    
    // 사거리 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getRangeUpgradeCost(): Int {
        return GameConfig.STATS_RANGE_BASE_COST + (getRangeLevel() * GameConfig.STATS_RANGE_COST_INCREASE)
    }
    
    // 모든 스탯 초기화 (개발 테스트용)
    fun resetAllStats() {
        prefs.edit().apply {
            putInt(KEY_HEALTH, DEFAULT_HEALTH)
            putInt(KEY_ATTACK, DEFAULT_ATTACK)
            putFloat(KEY_ATTACK_SPEED, DEFAULT_ATTACK_SPEED)
            putInt(KEY_RANGE, DEFAULT_RANGE)
            
            putInt(KEY_HEALTH_LEVEL, 0)
            putInt(KEY_ATTACK_LEVEL, 0)
            putInt(KEY_ATTACK_SPEED_LEVEL, 0)
            putInt(KEY_RANGE_LEVEL, 0)
        }.apply()
    }
    
    fun incrementGamesStarted() {
        val count = prefs.getInt(KEY_GAMES_STARTED, 0) + 1
        prefs.edit().putInt(KEY_GAMES_STARTED, count).apply()
        broadcastStatChange(STAT_GAMES_STARTED, count)
    }
    
    /**
     * 게임 클리어 수 증가
     */
    fun incrementGamesCompleted() {
        val count = prefs.getInt(KEY_GAMES_COMPLETED, 0) + 1
        prefs.edit().putInt(KEY_GAMES_COMPLETED, count).apply()
        broadcastStatChange(STAT_GAMES_COMPLETED, count)
    }
    
    /**
     * 게임 오버 수 증가
     */
    fun incrementGamesOver() {
        val count = prefs.getInt(KEY_GAMES_OVER, 0) + 1
        prefs.edit().putInt(KEY_GAMES_OVER, count).apply()
        broadcastStatChange(STAT_GAMES_OVER, count)
    }
    
    fun broadcastStatChange(statName: String, statValue: Int) {
        // Implementation of broadcastStatChange method
    }
    
    /**
     * 게임 시작 시 초기 코인 값 설정
     * 게임 시작 시 호출되어야 함
     */
    fun setInitialGameCoins(coins: Int) {
        prefs.edit().putInt(KEY_INITIAL_GAME_COINS, coins).apply()
    }
    
    /**
     * 게임 시작 시 저장한 초기 코인 값 반환
     * 게임 중 획득한 코인을 계산하는 데 사용됨
     */
    fun getInitialGameCoins(): Int {
        return prefs.getInt(KEY_INITIAL_GAME_COINS, 0)
    }
} 