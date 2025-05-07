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
        
        // 기본 스탯 값
        private const val DEFAULT_HEALTH = 100
        private const val DEFAULT_ATTACK = 50
        private const val DEFAULT_ATTACK_SPEED = 1.0f
        private const val DEFAULT_RANGE = 500
        
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
    
    fun upgradeHealth(amount: Int) {
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
    
    fun upgradeAttack(amount: Int) {
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
    
    fun upgradeAttackSpeed(amount: Float) {
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
    
    fun upgradeRange(amount: Int) {
        val currentRange = getRange()
        val currentLevel = getRangeLevel()
        
        setRange(currentRange + amount)
        prefs.edit().putInt(KEY_RANGE_LEVEL, currentLevel + 1).apply()
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