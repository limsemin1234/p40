package com.example.p40.game

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
class GameStats(private val gameConfig: GameConfig) {
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
    
    // 업그레이드 레벨 및 비용
    private var damageLevel = 1
    private var attackSpeedLevel = 1
    private var attackRangeLevel = 1
    private var defenseLevel = 1
    private var damageCost = gameConfig.DAMAGE_UPGRADE_INITIAL_COST
    private var attackSpeedCost = gameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST
    private var attackRangeCost = gameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST
    private var defenseCost = gameConfig.DEFENSE_UPGRADE_INITIAL_COST
    
    // 버프 관리자
    private val buffManager = BuffManager()
    
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
        
        // 업그레이드 레벨 및 비용 초기화
        damageLevel = 1
        attackSpeedLevel = 1
        attackRangeLevel = 1
        defenseLevel = 1
        damageCost = gameConfig.DAMAGE_UPGRADE_INITIAL_COST
        attackSpeedCost = gameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST
        attackRangeCost = gameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST
        defenseCost = gameConfig.DEFENSE_UPGRADE_INITIAL_COST
        
        // 버프 초기화
        buffManager.clearAllBuffs()
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
            unitAttackSpeed = (unitAttackSpeed * (1f - gameConfig.ATTACK_SPEED_UPGRADE_PERCENT)).toLong()
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
    
    fun getBuffManager(): BuffManager = buffManager
    
    fun getActiveBuffs(): List<Buff> = buffManager.getAllBuffs()
    fun getDefenseBuffs(): List<Buff> = buffManager.getDefenseBuffs()
    fun getEnemyNerfs(): List<Buff> = buffManager.getEnemyNerfs()
    
    // 특수 계산 메서드들
    fun getEffectiveAttackPower(): Int {
        return (unitAttackPower * buffManager.getMissileDamageMultiplier()).toInt()
    }
    
    fun getEffectiveAttackSpeed(): Float {
        return unitAttackSpeed * buffManager.getAttackSpeedMultiplier()
    }
    
    fun getEnemySpeedMultiplier(): Float {
        return buffManager.getEnemySpeedMultiplier()
    }
    
    fun getMissileSpeedMultiplier(): Float {
        return buffManager.getMissileSpeedMultiplier()
    }
    
    fun getMultiDirectionCount(): Int {
        return buffManager.getMultiDirectionCount()
    }
    
    fun getMissilePierceCount(): Int {
        return buffManager.getMissilePierceCount()
    }
    
    /**
     * 유닛 스탯 정보를 한번에 반환
     */
    fun getUnitStats(): UnitStats {
        return UnitStats(
            health = unitHealth,
            maxHealth = unitMaxHealth,
            attackPower = getEffectiveAttackPower(),
            attackSpeed = getEffectiveAttackSpeed(),
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
} 