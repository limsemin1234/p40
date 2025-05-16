package com.example.p40

/**
 * 게임 내 업그레이드 관리를 담당하는 클래스
 * 업그레이드 레벨에 따른 비용 계산 및 능력치 증가량 관리
 */
class GameUpgradeManager {
    // 현재 업그레이드 레벨
    private var damageLevel = 0
    private var attackSpeedLevel = 0
    private var attackRangeLevel = 0
    private var defenseLevel = 0
    
    // 업그레이드 시 증가하는 능력치 값 저장
    private var damageValue = 0
    private var attackSpeedValue = 0L
    private var attackRangeValue = 0f
    private var defenseValue = 0
    
    // 기본 능력치
    private var baseDamage = GameConfig.BASE_DAMAGE
    private var baseAttackSpeed = GameConfig.BASE_ATTACK_SPEED.toLong()
    private var baseAttackRange = GameConfig.BASE_ATTACK_RANGE
    private var baseDefense = GameConfig.BASE_HEALTH
    
    /**
     * 초기화
     */
    fun initialize() {
        // 레벨 초기화
        damageLevel = 0
        attackSpeedLevel = 0
        attackRangeLevel = 0
        defenseLevel = 0
        
        // 증가값 초기화
        damageValue = 0
        attackSpeedValue = 0L
        attackRangeValue = 0f
        defenseValue = 0
        
        // 기본값 설정
        baseDamage = GameConfig.BASE_DAMAGE
        baseAttackSpeed = GameConfig.BASE_ATTACK_SPEED.toLong()
        baseAttackRange = GameConfig.BASE_ATTACK_RANGE
        baseDefense = GameConfig.BASE_HEALTH
    }
    
    /**
     * 현재 데미지 레벨 반환
     */
    fun getDamageLevel(): Int = damageLevel
    
    /**
     * 현재 공격속도 레벨 반환
     */
    fun getAttackSpeedLevel(): Int = attackSpeedLevel
    
    /**
     * 현재 공격범위 레벨 반환
     */
    fun getAttackRangeLevel(): Int = attackRangeLevel
    
    /**
     * 현재 방어 레벨 반환
     */
    fun getDefenseLevel(): Int = defenseLevel
    
    /**
     * 모든 업그레이드 레벨 설정
     */
    fun setUpgradeLevels(damage: Int, attackSpeed: Int, attackRange: Int, defense: Int) {
        damageLevel = damage
        attackSpeedLevel = attackSpeed
        attackRangeLevel = attackRange
        defenseLevel = defense
        
        // 레벨에 따른 증가값 계산
        recalculateUpgradeValues()
    }
    
    /**
     * 증가값 재계산
     */
    private fun recalculateUpgradeValues() {
        // 데미지 증가값 = 레벨 * 증가량
        damageValue = damageLevel * GameConfig.DAMAGE_UPGRADE_VALUE
        
        // 공격 속도 감소량 계산 (단계별로 다른 감소량)
        attackSpeedValue = 0L
        for (i in 1..attackSpeedLevel) {
            val currentSpeed = baseAttackSpeed - attackSpeedValue
            attackSpeedValue += when {
                currentSpeed > GameConfig.ATTACK_SPEED_TIER1_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER1
                currentSpeed > GameConfig.ATTACK_SPEED_TIER2_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER2
                currentSpeed > GameConfig.ATTACK_SPEED_TIER3_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER3
                else -> 0L
            }
        }
        
        // 공격 범위 증가값 = 레벨 * 증가량
        attackRangeValue = attackRangeLevel * GameConfig.ATTACK_RANGE_UPGRADE_VALUE
        
        // 방어 증가값 = 레벨 * 증가량
        defenseValue = defenseLevel * GameConfig.DEFENSE_UPGRADE_VALUE
    }
    
    /**
     * 현재 데미지 반환
     */
    fun getCurrentDamage(): Int = baseDamage + damageValue
    
    /**
     * 현재 공격속도 반환 (ms)
     */
    fun getCurrentAttackSpeed(): Long {
        val calculatedSpeed = baseAttackSpeed - attackSpeedValue
        // 최소 속도 보장 (너무 빨라지지 않도록)
        return calculatedSpeed.coerceAtLeast(GameConfig.MIN_ATTACK_SPEED.toLong())
    }
    
    /**
     * 현재 공격범위 반환
     */
    fun getCurrentAttackRange(): Float = baseAttackRange + attackRangeValue
    
    /**
     * 현재 최대 체력 반환
     */
    fun getCurrentMaxHealth(): Int = baseDefense + defenseValue
    
    /**
     * 데미지 업그레이드 비용 계산
     */
    fun getDamageUpgradeCost(): Int {
        return if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
            Int.MAX_VALUE // 최대 레벨이면 구매 불가능
        } else {
            GameConfig.DAMAGE_UPGRADE_INITIAL_COST + (damageLevel * GameConfig.DAMAGE_UPGRADE_COST_INCREASE)
        }
    }
    
    /**
     * 공격속도 업그레이드 비용 계산
     */
    fun getAttackSpeedUpgradeCost(): Int {
        return if (attackSpeedLevel >= GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
            Int.MAX_VALUE // 최대 레벨이면 구매 불가능
        } else {
            GameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST + (attackSpeedLevel * GameConfig.ATTACK_SPEED_UPGRADE_COST_INCREASE)
        }
    }
    
    /**
     * 공격범위 업그레이드 비용 계산
     */
    fun getAttackRangeUpgradeCost(): Int {
        return if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
            Int.MAX_VALUE // 최대 레벨이면 구매 불가능
        } else {
            GameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST + (attackRangeLevel * GameConfig.ATTACK_RANGE_UPGRADE_COST_INCREASE)
        }
    }
    
    /**
     * 방어 업그레이드 비용 계산
     */
    fun getDefenseUpgradeCost(): Int {
        return if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
            Int.MAX_VALUE // 최대 레벨이면 구매 불가능
        } else {
            GameConfig.DEFENSE_UPGRADE_INITIAL_COST + (defenseLevel * GameConfig.DEFENSE_UPGRADE_COST_INCREASE)
        }
    }
    
    /**
     * 데미지 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradeDamage(): Boolean {
        if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        damageLevel++
        damageValue += GameConfig.DAMAGE_UPGRADE_VALUE
        return true
    }
    
    /**
     * 공격속도 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradeAttackSpeed(): Boolean {
        if (attackSpeedLevel >= GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        val currentSpeed = getCurrentAttackSpeed()
        val decreaseAmount = when {
            currentSpeed > GameConfig.ATTACK_SPEED_TIER1_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER1
            currentSpeed > GameConfig.ATTACK_SPEED_TIER2_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER2
            currentSpeed > GameConfig.ATTACK_SPEED_TIER3_THRESHOLD -> GameConfig.ATTACK_SPEED_DECREASE_TIER3
            else -> 0L
        }
        
        attackSpeedLevel++
        attackSpeedValue += decreaseAmount
        return true
    }
    
    /**
     * 공격범위 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradeAttackRange(): Boolean {
        if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        attackRangeLevel++
        attackRangeValue += GameConfig.ATTACK_RANGE_UPGRADE_VALUE
        return true
    }
    
    /**
     * 방어 업그레이드
     * @return 업그레이드 성공 여부
     */
    fun upgradeDefense(): Boolean {
        if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
            return false
        }
        
        defenseLevel++
        defenseValue += GameConfig.DEFENSE_UPGRADE_VALUE
        return true
    }
    
    /**
     * 포커 핸드에 따른 데미지 보너스 계산
     * @return 데미지 증가 배율 (1.0 = 100%, 1.5 = 150% 등)
     */
    fun calculateDamageBonus(handType: PokerHandType): Float {
        return 1.0f + handType.getDamageBonus()
    }
} 