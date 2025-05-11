package com.example.p40.game

import android.content.Context

/**
 * 게임 상태 관련 데이터 클래스들
 * 연관된 데이터를 그룹화하여 관리 용이성 향상
 */
data class UnitStats(
    val health: Int,
    val maxHealth: Int,
    val attackPower: Int,
    val attackSpeed: Float,
    val attackRange: Float
)

data class GameProgress(
    val resource: Int,
    val waveCount: Int,
    val killCount: Int,
    val spawnedCount: Int,
    val totalEnemiesInWave: Int,
    val bossSpawned: Boolean
)

data class UpgradeInfo(
    val level: Int,
    val cost: Int
)

/**
 * 게임 상태 관리 클래스
 * GameView에서 게임 상태 변수와 관련 메서드들을 분리함
 */
class GameStats(
    private val gameConfig: GameConfig,
    private val context: Context
) {
    // 싱글톤 구현
    companion object {
        @Volatile
        private var instance: GameStats? = null
        
        fun initialize(gameConfig: GameConfig, context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = GameStats(gameConfig, context)
                    }
                }
            }
        }
        
        fun getInstance(): GameStats {
            return instance ?: throw IllegalStateException("GameStats 클래스가 초기화되지 않았습니다. initialize()를 먼저 호출하세요.")
        }
    }
    
    // 게임 상태
    private var resource = 0
    private var waveCount = 1
    private var killCount = 0
    private var spawnedCount = 0
    private var totalEnemiesInWave = gameConfig.ENEMIES_PER_WAVE
    private var bossSpawned = false
    
    // 디펜스 유닛 스탯
    private var unitHealth = gameConfig.DEFENSE_UNIT_INITIAL_HEALTH
    private var unitMaxHealth = gameConfig.DEFENSE_UNIT_INITIAL_MAX_HEALTH
    private var unitAttackPower = gameConfig.MISSILE_DAMAGE
    private var unitAttackSpeed = gameConfig.DEFENSE_UNIT_ATTACK_COOLDOWN
    private var unitAttackRange = gameConfig.DEFENSE_UNIT_ATTACK_RANGE
    
    // 포커 효과
    private var activePokerHand: PokerHand? = null
    
    // 업그레이드 레벨 및 비용 (0부터 시작하도록 변경)
    private var damageLevel = 0
    private var attackSpeedLevel = 0
    private var attackRangeLevel = 0
    private var defenseLevel = 0
    private var damageCost = gameConfig.DAMAGE_UPGRADE_INITIAL_COST
    private var attackSpeedCost = gameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST
    private var attackRangeCost = gameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST
    private var defenseCost = gameConfig.DEFENSE_UPGRADE_INITIAL_COST
    
    // 버프 관리자
    private val buffManager = BuffManager(context)
    
    /**
     * 게임 상태 초기화
     */
    fun resetGame() {
        resource = 0
        waveCount = 1
        killCount = 0
        spawnedCount = 0
        totalEnemiesInWave = gameConfig.ENEMIES_PER_WAVE
        bossSpawned = false
        unitHealth = gameConfig.DEFENSE_UNIT_INITIAL_HEALTH
        unitMaxHealth = gameConfig.DEFENSE_UNIT_INITIAL_MAX_HEALTH
        unitAttackPower = gameConfig.MISSILE_DAMAGE
        unitAttackSpeed = gameConfig.DEFENSE_UNIT_ATTACK_COOLDOWN
        unitAttackRange = gameConfig.DEFENSE_UNIT_ATTACK_RANGE
        
        // 업그레이드 레벨 및 비용 초기화 (0부터 시작하도록 변경)
        damageLevel = 0
        attackSpeedLevel = 0
        attackRangeLevel = 0
        defenseLevel = 0
        damageCost = gameConfig.DAMAGE_UPGRADE_INITIAL_COST
        attackSpeedCost = gameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST
        attackRangeCost = gameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST
        defenseCost = gameConfig.DEFENSE_UPGRADE_INITIAL_COST
        
        // 버프 초기화
        buffManager.clearAllBuffs()
    }
    
    /**
     * 게임 상태 리셋
     * resetGame과 동일한 기능을 하는 별칭 메서드
     */
    fun reset() {
        resetGame()
    }
    
    /**
     * 적 처치 처리
     * @param isBoss 보스 여부
     * @return 처치된 적이 보스인 경우 true
     */
    fun enemyKilled(isBoss: Boolean): Boolean {
        resource += if (isBoss) {
            gameConfig.SCORE_PER_BOSS
        } else {
            killCount++
            gameConfig.SCORE_PER_NORMAL_ENEMY
        }
        
        return isBoss
    }
    
    /**
     * 웨이브 완료 후 다음 웨이브 시작
     */
    fun nextWave() {
        waveCount++
        killCount = 0
        spawnedCount = 0
        bossSpawned = false
        totalEnemiesInWave = gameConfig.ENEMIES_PER_WAVE
    }
    
    /**
     * 보스 출현 처리
     */
    fun spawnBoss() {
        bossSpawned = true
    }
    
    /**
     * 적 생성 카운트 증가
     */
    fun incrementSpawnCount() {
        spawnedCount++
    }
    
    /**
     * 킬 카운트 증가
     */
    fun incrementKillCount() {
        killCount++
    }
    
    /**
     * 디펜스 유닛 데미지 적용
     * @param damage 받은 데미지
     * @return 체력이 0 이하면 true
     */
    fun applyDamageToUnit(damage: Int): Boolean {
        unitHealth = (unitHealth - damage).coerceAtLeast(0)
        return unitHealth <= 0
    }
    
    /**
     * 디펜스 유닛 체력 완전 회복
     */
    fun restoreFullHealth() {
        unitHealth = unitMaxHealth
    }
    
    /**
     * 디펜스 유닛 체력 회복
     * @param amount 회복할 체력량
     */
    fun healUnit(amount: Int) {
        unitHealth = (unitHealth + amount).coerceAtMost(unitMaxHealth)
    }
    
    /**
     * 자원 추가
     * @param amount 추가할 자원량
     */
    fun addResource(amount: Int) {
        resource += amount
    }
    
    /**
     * 포커 족보 효과 적용
     */
    fun applyPokerHandEffect(pokerHand: PokerHand) {
        activePokerHand = pokerHand
        buffManager.addPokerHandBuff(pokerHand)
    }
    
    /**
     * 데미지 업그레이드
     * @return 업그레이드 성공 시 true
     */
    fun upgradeDamage(): Boolean {
        if (damageLevel >= gameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= damageCost) {
            resource -= damageCost
            unitAttackPower += gameConfig.DAMAGE_UPGRADE_VALUE
            damageCost += gameConfig.DAMAGE_UPGRADE_COST_INCREASE
            damageLevel++
            return true
        }
        return false
    }
    
    /**
     * 공격 속도 업그레이드
     * @return 업그레이드 성공 시 true
     */
    fun upgradeAttackSpeed(): Boolean {
        if (attackSpeedLevel >= gameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= attackSpeedCost) {
            resource -= attackSpeedCost
            
            // 현재 공격 속도에 따라 다른 감소량 적용
            val decreaseAmount = when {
                unitAttackSpeed > gameConfig.ATTACK_SPEED_TIER1_THRESHOLD -> gameConfig.ATTACK_SPEED_DECREASE_TIER1
                unitAttackSpeed > gameConfig.ATTACK_SPEED_TIER2_THRESHOLD -> gameConfig.ATTACK_SPEED_DECREASE_TIER2
                unitAttackSpeed > gameConfig.ATTACK_SPEED_TIER3_THRESHOLD -> gameConfig.ATTACK_SPEED_DECREASE_TIER3
                else -> 0L // 최소 임계값 도달 시 더 이상 감소하지 않음
            }
            
            // 최소값 50ms 이하로는 내려가지 않도록 설정
            unitAttackSpeed = maxOf(50L, unitAttackSpeed - decreaseAmount)
            
            attackSpeedCost += gameConfig.ATTACK_SPEED_UPGRADE_COST_INCREASE
            attackSpeedLevel++
            return true
        }
        return false
    }
    
    /**
     * 공격 범위 업그레이드
     * @return 업그레이드 성공 시 true
     */
    fun upgradeAttackRange(): Boolean {
        if (attackRangeLevel >= gameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= attackRangeCost) {
            resource -= attackRangeCost
            unitAttackRange += gameConfig.ATTACK_RANGE_UPGRADE_VALUE
            attackRangeCost += gameConfig.ATTACK_RANGE_UPGRADE_COST_INCREASE
            attackRangeLevel++
            return true
        }
        return false
    }
    
    /**
     * 방어력 업그레이드
     * @return 업그레이드 성공 시 true
     */
    fun upgradeDefense(): Boolean {
        if (defenseLevel >= gameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= defenseCost) {
            resource -= defenseCost
            unitMaxHealth += gameConfig.DEFENSE_UPGRADE_VALUE
            unitHealth += gameConfig.DEFENSE_UPGRADE_VALUE
            defenseCost += gameConfig.DEFENSE_UPGRADE_COST_INCREASE
            defenseLevel++
            return true
        }
        return false
    }
    
    /**
     * 자원 소모 메서드
     * @return 자원 소모 성공 시 true
     */
    fun useResource(amount: Int): Boolean {
        if (resource >= amount) {
            resource -= amount
            return true
        }
        return false
    }
    
    // 게터 메서드들
    fun getResource(): Int = resource
    fun getWaveCount(): Int = waveCount
    fun getKillCount(): Int = killCount
    fun getTotalEnemiesInWave(): Int = totalEnemiesInWave
    fun isBossSpawned(): Boolean = bossSpawned
    fun getSpawnedCount(): Int = spawnedCount
    
    fun getUnitHealth(): Int = unitHealth
    fun getUnitMaxHealth(): Int = unitMaxHealth
    fun getUnitAttackPower(): Int = unitAttackPower
    fun getUnitAttackSpeed(): Long = unitAttackSpeed
    fun getUnitAttackRange(): Float = unitAttackRange
    
    // 현재 체력 가져오기 메서드 추가
    fun getCurrentHealth(): Int = unitHealth
    
    fun getDamageLevel(): Int = damageLevel
    fun getAttackSpeedLevel(): Int = attackSpeedLevel
    fun getAttackRangeLevel(): Int = attackRangeLevel
    fun getDefenseLevel(): Int = defenseLevel
    
    fun getDamageCost(): Int = damageCost
    fun getAttackSpeedCost(): Int = attackSpeedCost
    fun getAttackRangeCost(): Int = attackRangeCost
    fun getDefenseCost(): Int = defenseCost
    
    fun getActivePokerHandInfo(): String {
        return activePokerHand?.let {
            "${it.handName}: ${it.getDescription()}"
        } ?: "없음"
    }
    
    fun getBuffManager(): BuffManager {
        return buffManager
    }
    
    fun getActiveBuffs(): List<Buff> = buffManager.getAllBuffs()
    
    // 데미지 버프만 반환하도록 수정
    fun getDefenseBuffs(): List<Buff> = buffManager.getAllBuffs().filter { it.type == BuffType.MISSILE_DAMAGE }
    
    // 플러시 스킬 버프만 반환하도록 수정
    fun getEnemyNerfs(): List<Buff> = buffManager.getAllBuffs().filter { 
        it.type == BuffType.HEART_FLUSH_SKILL || 
        it.type == BuffType.SPADE_FLUSH_SKILL || 
        it.type == BuffType.CLUB_FLUSH_SKILL || 
        it.type == BuffType.DIAMOND_FLUSH_SKILL 
    }
    
    // 특수 계산 메서드들
    fun getEffectiveAttackPower(): Int {
        return (unitAttackPower * buffManager.getMissileDamageMultiplier()).toInt()
    }
    
    /**
     * 유닛 스탯 정보를 한번에 반환
     */
    fun getUnitStats(): UnitStats {
        return UnitStats(
            health = unitHealth,
            maxHealth = unitMaxHealth,
            attackPower = getEffectiveAttackPower(),
            attackSpeed = unitAttackSpeed.toFloat(),
            attackRange = unitAttackRange
        )
    }
    
    /**
     * 게임 진행 정보를 한번에 반환
     */
    fun getGameProgress(): GameProgress {
        return GameProgress(
            resource = resource,
            waveCount = waveCount,
            killCount = killCount,
            spawnedCount = spawnedCount,
            totalEnemiesInWave = totalEnemiesInWave,
            bossSpawned = bossSpawned
        )
    }
    
    /**
     * 데미지 업그레이드 정보 반환
     */
    fun getDamageUpgradeInfo(): UpgradeInfo {
        return UpgradeInfo(level = damageLevel, cost = damageCost)
    }
    
    /**
     * 공격 속도 업그레이드 정보 반환
     */
    fun getAttackSpeedUpgradeInfo(): UpgradeInfo {
        return UpgradeInfo(level = attackSpeedLevel, cost = attackSpeedCost)
    }
    
    /**
     * 공격 범위 업그레이드 정보 반환
     */
    fun getAttackRangeUpgradeInfo(): UpgradeInfo {
        return UpgradeInfo(level = attackRangeLevel, cost = attackRangeCost)
    }
    
    /**
     * 방어력 업그레이드 정보 반환
     */
    fun getDefenseUpgradeInfo(): UpgradeInfo {
        return UpgradeInfo(level = defenseLevel, cost = defenseCost)
    }
    
    // 스탯 직접 설정 메서드들 (StatsManager 연동용)
    
    /**
     * 유닛 체력 설정
     */
    fun setUnitHealth(health: Int) {
        this.unitHealth = health
    }
    
    /**
     * 유닛 최대 체력 설정
     */
    fun setUnitMaxHealth(maxHealth: Int) {
        this.unitMaxHealth = maxHealth
    }
    
    /**
     * 유닛 공격력 설정
     */
    fun setUnitAttackPower(attackPower: Int) {
        this.unitAttackPower = attackPower
    }
    
    /**
     * 유닛 공격 속도 설정
     */
    fun setUnitAttackSpeed(attackSpeed: Long) {
        // 최소 공격속도를 50ms로 제한
        this.unitAttackSpeed = maxOf(50L, attackSpeed)
    }
    
    /**
     * 유닛 공격 범위 설정
     */
    fun setUnitAttackRange(attackRange: Float) {
        this.unitAttackRange = attackRange
    }
} 