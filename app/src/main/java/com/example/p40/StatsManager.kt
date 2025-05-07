package com.example.p40

import android.content.Context
import android.content.SharedPreferences
import com.example.p40.game.GameConfig

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
        
        // 기본 스탯 값 - GameConfig에서 가져오기
        private val DEFAULT_HEALTH = GameConfig.DEFENSE_UNIT_INITIAL_HEALTH
        private val DEFAULT_ATTACK = GameConfig.MISSILE_DAMAGE
        private val DEFAULT_ATTACK_SPEED = 1.0f
        private val DEFAULT_RANGE = GameConfig.DEFENSE_UNIT_ATTACK_RANGE.toInt()
        
        // 싱글톤 인스턴스
        @Volatile
        private var instance: StatsManager? = null
        
        // 인스턴스 가져오기
        fun getInstance(context: Context): StatsManager {
            return instance ?: synchronized(this) {
                instance ?: StatsManager(context.applicationContext).also {
                    instance = it
                }
            }
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
    
    fun upgradeHealth(amount: Int = GameConfig.STATS_HEALTH_UPGRADE_AMOUNT) {
        val currentHealth = getHealth()
        val currentLevel = getHealthLevel()
        
        setHealth(currentHealth + amount)
        prefs.edit().putInt(KEY_HEALTH_LEVEL, currentLevel + 1).apply()
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
    
    fun upgradeAttack(amount: Int = GameConfig.STATS_ATTACK_UPGRADE_AMOUNT) {
        val currentAttack = getAttack()
        val currentLevel = getAttackLevel()
        
        setAttack(currentAttack + amount)
        prefs.edit().putInt(KEY_ATTACK_LEVEL, currentLevel + 1).apply()
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
    
    fun upgradeAttackSpeed(amount: Float = GameConfig.STATS_ATTACK_SPEED_UPGRADE_AMOUNT) {
        val currentAttackSpeed = getAttackSpeed()
        val currentLevel = getAttackSpeedLevel()
        
        setAttackSpeed(currentAttackSpeed + amount)
        prefs.edit().putInt(KEY_ATTACK_SPEED_LEVEL, currentLevel + 1).apply()
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
    
    fun upgradeRange(amount: Int = GameConfig.STATS_RANGE_UPGRADE_AMOUNT) {
        val currentRange = getRange()
        val currentLevel = getRangeLevel()
        
        setRange(currentRange + amount)
        prefs.edit().putInt(KEY_RANGE_LEVEL, currentLevel + 1).apply()
    }
    
    // 강화 비용 계산 메서드를 스탯별로 개별화
    
    // 체력 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getHealthUpgradeCost(): Int {
        return GameConfig.STATS_HEALTH_BASE_COST + (getHealthLevel() * GameConfig.STATS_HEALTH_COST_INCREASE.toInt())
    }
    
    // 공격력 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getAttackUpgradeCost(): Int {
        return GameConfig.STATS_ATTACK_BASE_COST + (getAttackLevel() * GameConfig.STATS_ATTACK_COST_INCREASE.toInt())
    }
    
    // 공격 속도 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getAttackSpeedUpgradeCost(): Int {
        return GameConfig.STATS_ATTACK_SPEED_BASE_COST + (getAttackSpeedLevel() * GameConfig.STATS_ATTACK_SPEED_COST_INCREASE.toInt())
    }
    
    // 사거리 강화 비용 계산 - 선형 증가 방식으로 변경
    fun getRangeUpgradeCost(): Int {
        return GameConfig.STATS_RANGE_BASE_COST + (getRangeLevel() * GameConfig.STATS_RANGE_COST_INCREASE.toInt())
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
} 