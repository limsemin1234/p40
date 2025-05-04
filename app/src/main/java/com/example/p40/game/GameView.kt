package com.example.p40.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 게임 오버 콜백 인터페이스
 */
interface GameOverListener {
    fun onGameOver(resource: Int, waveCount: Int)
}

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var isRunning = false
    private var isPaused = false
    private var isGameOver = false // 게임 오버 상태 추가
    
    // 게임 오버 콜백
    private var gameOverListener: GameOverListener? = null
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val missiles = CopyOnWriteArrayList<Missile>()
    
    // 버프 관리자
    private val buffManager = BuffManager()
    
    // 게임 상태
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var resource = 0  // 점수를 자원으로 변경
    private var waveCount = 1
    private var killCount = 0
    private var spawnedCount = 0  // 생성된 적의 수를 추적
    private var totalEnemiesInWave = GameConfig.ENEMIES_PER_WAVE
    private var bossSpawned = false
    private var showWaveMessage = false
    private var waveMessageStartTime = 0L
    private var waveMessageDuration = GameConfig.WAVE_MESSAGE_DURATION
    
    // 디펜스 유닛 설정
    private val DEFENSE_UNIT_SIZE = GameConfig.DEFENSE_UNIT_SIZE
    
    // 디펜스 유닛 스탯
    private var unitHealth = GameConfig.DEFENSE_UNIT_INITIAL_HEALTH
    private var unitMaxHealth = GameConfig.DEFENSE_UNIT_INITIAL_MAX_HEALTH
    private var unitAttackPower = GameConfig.MISSILE_DAMAGE
    private var unitAttackSpeed = GameConfig.DEFENSE_UNIT_ATTACK_COOLDOWN
    private var unitAttackRange = GameConfig.DEFENSE_UNIT_ATTACK_RANGE
    
    // 웨이브별 설정
    private val waveEnemySpawnCooldowns = GameConfig.WAVE_ENEMY_SPAWN_COOLDOWNS
    private val waveEnemySpeeds = GameConfig.WAVE_ENEMY_SPEEDS
    
    private var enemySpawnCooldown = waveEnemySpawnCooldowns[1] ?: 3000L
    private var lastEnemySpawnTime = 0L
    private var gameStartTime = 0L
    
    // 그리기 도구
    private val textPaint = Paint().apply {
        color = GameConfig.TEXT_COLOR
        textSize = GameConfig.TEXT_SIZE_NORMAL
        textAlign = Paint.Align.LEFT
    }
    
    private val waveMessagePaint = Paint().apply {
        color = GameConfig.WAVE_TEXT_COLOR
        textSize = GameConfig.TEXT_SIZE_WAVE
        textAlign = Paint.Align.CENTER
    }
    
    private val unitPaint = Paint().apply {
        color = GameConfig.DEFENSE_UNIT_COLOR
        style = Paint.Style.FILL
    }
    
    // 일시정지 관련
    private val pauseTextPaint = Paint().apply {
        color = GameConfig.TEXT_COLOR
        textSize = GameConfig.TEXT_SIZE_PAUSE
        textAlign = Paint.Align.CENTER
    }
    
    // 현재 활성화된 포커 족보 효과
    private var activePokerHand: PokerHand? = null
    
    init {
        holder.addCallback(this)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        
        // 중앙에 방어 유닛 배치
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        defenseUnit = DefenseUnit(
            position = PointF(centerX, centerY),
            attackRange = unitAttackRange,
            attackCooldown = unitAttackSpeed
        )
        
        gameStartTime = System.currentTimeMillis()
        lastEnemySpawnTime = gameStartTime
        isRunning = true
        gameThread = GameThread()
        gameThread?.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // 스레드가 종료될 때까지 대기
            }
        }
    }
    
    private fun update() {
        if (isPaused || isGameOver) return
        
        val currentTime = System.currentTimeMillis()
        
        // 웨이브 메시지 표시 시간 체크
        if (showWaveMessage && currentTime - waveMessageStartTime > waveMessageDuration) {
            showWaveMessage = false
        }
        
        // 보스를 처치하고 다음 웨이브로 넘어가거나 웨이브 내 적 생성
        if (!showWaveMessage) {
            if (waveCount <= 10) {
                handleEnemySpawning(currentTime)
            }
        }
        
        // 적 업데이트
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        enemies.forEach { enemy ->
            // 버프에 의한 적 이동 속도 조정 적용
            val speedMultiplier = buffManager.getEnemySpeedMultiplier()
            enemy.update(speedMultiplier)
            
            // 중앙에 도달했는지 확인
            val enemyPos = enemy.getPosition()
            val dx = enemyPos.x - centerX
            val dy = enemyPos.y - centerY
            val distanceToCenter = kotlin.math.sqrt(dx * dx + dy * dy)
            
            if (distanceToCenter < DEFENSE_UNIT_SIZE) {  // 디펜스 유닛 크기 적용
                enemy.takeDamage(1000) // 중앙에 도달하면 죽음
                // 디펜스 유닛 체력 감소
                unitHealth = (unitHealth - 10).coerceAtLeast(0)
                
                // 체력이 0이 되면 게임 오버
                if (unitHealth <= 0 && !isGameOver) {
                    isGameOver = true
                    // 게임 오버 처리 - UI 스레드에서 실행
                    Handler(Looper.getMainLooper()).post {
                        gameOverListener?.onGameOver(resource, waveCount)
                    }
                }
            }
        }
        
        // 미사일 업데이트
        missiles.forEach { missile ->
            // 버프에 의한 미사일 속도 조정 적용
            val speedMultiplier = buffManager.getMissileSpeedMultiplier()
            missile.update(speedMultiplier)
            
            // 미사일이 모든 적과 충돌 체크
            if (!missile.isDead()) {
                val pierceCount = buffManager.getMissilePierceCount()
                var hitCount = 0
                
                for (enemy in enemies) {
                    // 충돌 체크 (원래 타겟이 아닌 다른 적과의 충돌 확인)
                    if (missile.checkCollision(enemy)) {
                        hitCount++
                        // 관통 횟수를 초과하면 미사일 제거
                        if (hitCount > pierceCount) {
                            break
                        }
                    }
                }
            }
        }
        
        // 방어 유닛이 적을 공격
        // 버프에 의한 공격 속도 조정 적용
        val attackSpeedMultiplier = buffManager.getAttackSpeedMultiplier()
        val adjustedAttackCooldown = (unitAttackSpeed * attackSpeedMultiplier).toLong()
        
        // 다방향 발사 지원
        val multiDirCount = buffManager.getMultiDirectionCount()
        if (multiDirCount > 1 && !enemies.isEmpty()) {
            // 다방향 발사 (기본 1방향 + 추가 방향)
            val angleStep = (2 * Math.PI) / multiDirCount
            
            for (i in 0 until multiDirCount) {
                val newMissile = defenseUnit.attack(
                    enemies, 
                    currentTime, 
                    adjustedAttackCooldown, 
                    buffManager.getMissileDamageMultiplier(),
                    i * angleStep
                )
                
                if (newMissile != null) {
                    missiles.add(newMissile)
                }
            }
        } else {
            // 기본 1방향 발사
            val newMissile = defenseUnit.attack(
                enemies, 
                currentTime, 
                adjustedAttackCooldown,
                buffManager.getMissileDamageMultiplier()
            )
            
            if (newMissile != null) {
                missiles.add(newMissile)
            }
        }
        
        // 지속 데미지 효과 (Full House)
        val dotLevel = buffManager.getBuffLevel(BuffType.DOT_DAMAGE)
        if (dotLevel > 0 && currentTime % 1000 < 20) { // 약 1초마다
            enemies.forEach { enemy ->
                enemy.takeDamage(dotLevel)
            }
        }
        
        // 주기적 대량 데미지 효과 (Royal Flush)
        val massLevel = buffManager.getBuffLevel(BuffType.MASS_DAMAGE)
        if (massLevel > 0 && currentTime % 5000 < 20) { // 약 5초마다
            enemies.forEach { enemy ->
                enemy.takeDamage(massLevel * 100)
            }
        }
        
        // 죽은 적과 미사일 제거
        val deadEnemies = enemies.filter { it.isDead() }
        enemies.removeAll(deadEnemies)
        
        // 킬 카운트 및 점수(자원) 갱신
        deadEnemies.forEach { enemy ->
            resource += if (enemy.isBoss) 100 else 10  // 자원 획득
            if (!enemy.isBoss) {
                killCount++
            } else {
                // 보스 처치 시 다음 웨이브로 이동
                if (waveCount < 10) {
                    startNextWave()
                }
            }
        }
        
        // 웨이브의 일반 적 모두 처치 시 보스 소환
        if (!bossSpawned && killCount >= totalEnemiesInWave) {
            spawnBoss()
            bossSpawned = true
        }
        
        missiles.removeAll { it.isDead() }
    }
    
    private fun handleEnemySpawning(currentTime: Long) {
        // 웨이브당 정확히 50마리만 생성되도록 수정
        if (spawnedCount < totalEnemiesInWave && !bossSpawned) {
            // 웨이브에 맞는 적 생성 간격으로 적 생성
            val spawnCooldown = waveEnemySpawnCooldowns[waveCount] ?: 3000L
            
            if (currentTime - lastEnemySpawnTime > spawnCooldown) {
                spawnEnemy()
                lastEnemySpawnTime = currentTime
            }
        }
    }
    
    private fun spawnEnemy() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 화면 가장자리에서 적 스폰
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * 0.6f
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        // 현재 웨이브에 맞는 적 능력치 설정
        val speed = waveEnemySpeeds[waveCount] ?: 1.0f
        val health = GameConfig.getEnemyHealthForWave(waveCount)
        
        val enemy = Enemy(
            position = PointF(spawnX, spawnY),
            target = PointF(centerX, centerY),
            speed = speed,
            health = health
        )
        
        enemies.add(enemy)
        spawnedCount++  // 생성된 적 카운트 증가
    }
    
    private fun spawnBoss() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 보스는 랜덤한 방향에서 생성
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * 0.7f
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        // 현재 웨이브에 맞는 보스 능력치 설정 (일반 적보다 느리지만 크고 강함)
        val speed = (waveEnemySpeeds[waveCount] ?: 1.0f) * 0.7f
        val health = GameConfig.getEnemyHealthForWave(waveCount) * GameConfig.BOSS_HEALTH_MULTIPLIER
        
        val boss = Enemy(
            position = PointF(spawnX, spawnY),
            target = PointF(centerX, centerY),
            speed = speed,
            size = 60f,  // 보스는 크기가 2배
            health = health,
            isBoss = true
        )
        
        enemies.add(boss)
    }
    
    private fun startNextWave() {
        waveCount++
        killCount = 0
        spawnedCount = 0  // 생성된 적 카운트 초기화
        bossSpawned = false
        
        // 웨이브 시작 메시지 표시
        showWaveMessage = true
        waveMessageStartTime = System.currentTimeMillis()
        
        // 새 웨이브에 맞는 적 생성 간격 설정
        enemySpawnCooldown = waveEnemySpawnCooldowns[waveCount] ?: 1000L
    }
    
    // 모든 적에게 데미지를 주는 카드 사용 효과
    fun useCard() {
        enemies.forEach { enemy ->
            // 보스는 카드 효과가 약하게
            val damage = if (enemy.isBoss) 50 else 100
            enemy.takeDamage(damage)
        }
    }
    
    // 공격력 업그레이드 - 미사일 데미지 증가
    private var damageLevel = 1
    private var damageCost = GameConfig.DAMAGE_UPGRADE_INITIAL_COST  // 초기 비용
    fun upgradeDamage(): Boolean {
        if (damageLevel >= GameConfig.DAMAGE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= damageCost) {
            resource -= damageCost  // 자원 사용
            unitAttackPower += GameConfig.DAMAGE_UPGRADE_VALUE  // 데미지 증가
            damageCost += GameConfig.DAMAGE_UPGRADE_COST_INCREASE  // 다음 업그레이드 비용 증가
            damageLevel++
            return true
        }
        return false  // 자원 부족
    }
    
    // 공격 속도 업그레이드 - 발사 속도 증가
    private var attackSpeedLevel = 1
    private var attackSpeedCost = GameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST  // 초기 비용
    fun upgradeAttackSpeed(): Boolean {
        if (attackSpeedLevel >= GameConfig.ATTACK_SPEED_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= attackSpeedCost) {
            resource -= attackSpeedCost  // 자원 사용
            // 공격속도 1% 향상 (쿨다운 시간을 0.99배로 줄임)
            unitAttackSpeed = (unitAttackSpeed * (1f - GameConfig.ATTACK_SPEED_UPGRADE_PERCENT)).toLong()
            attackSpeedCost += GameConfig.ATTACK_SPEED_UPGRADE_COST_INCREASE  // 다음 업그레이드 비용 증가
            attackSpeedLevel++
            return true
        }
        return false  // 자원 부족
    }
    
    // 공격 범위 업그레이드 - 공격 범위 증가
    private var attackRangeLevel = 1
    private var attackRangeCost = GameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST  // 초기 비용
    fun upgradeAttackRange(): Boolean {
        if (attackRangeLevel >= GameConfig.ATTACK_RANGE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= attackRangeCost) {
            resource -= attackRangeCost  // 자원 사용
            unitAttackRange += GameConfig.ATTACK_RANGE_UPGRADE_VALUE  // 공격 범위 증가
            defenseUnit.attackRange = unitAttackRange  // 디펜스 유닛에 적용
            attackRangeCost += GameConfig.ATTACK_RANGE_UPGRADE_COST_INCREASE  // 다음 업그레이드 비용 증가
            attackRangeLevel++
            return true
        }
        return false  // 자원 부족
    }
    
    // 방어력 업그레이드 - 디펜스 유닛 크기와 내구도 증가
    private var defenseLevel = 1
    private var defenseCost = GameConfig.DEFENSE_UPGRADE_INITIAL_COST  // 초기 비용
    fun upgradeDefense(): Boolean {
        if (defenseLevel >= GameConfig.DEFENSE_UPGRADE_MAX_LEVEL) {
            return false // 최대 레벨 도달
        }
        
        if (resource >= defenseCost) {
            resource -= defenseCost  // 자원 사용
            unitMaxHealth += GameConfig.DEFENSE_UPGRADE_VALUE  // 최대 체력 증가
            unitHealth += GameConfig.DEFENSE_UPGRADE_VALUE  // 현재 체력도 증가
            defenseCost += GameConfig.DEFENSE_UPGRADE_COST_INCREASE  // 다음 업그레이드 비용 증가
            defenseLevel++
            return true
        }
        return false  // 자원 부족
    }
    
    // 일시정지 처리
    fun pause() {
        isPaused = true
    }
    
    // 게임 재개
    fun resume() {
        isPaused = false
    }
    
    // 커스텀 그리기 메서드
    private fun renderGame(canvas: Canvas) {
        // 배경 지우기
        canvas.drawColor(Color.BLACK)
        
        // 중앙에 방어 타워 그리기
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        canvas.drawCircle(centerX, centerY, DEFENSE_UNIT_SIZE, unitPaint)  // 디펜스 유닛 크기 적용
        
        // 적 그리기
        enemies.forEach { it.draw(canvas) }
        
        // 미사일 그리기
        missiles.forEach { it.draw(canvas) }
        
        // 웨이브 시작 메시지 표시
        if (showWaveMessage) {
            canvas.drawText("$waveCount WAVE", centerX, centerY - 100, waveMessagePaint)
        }
        
        // 일시정지 상태 표시
        if (isPaused) {
            canvas.drawText("일시 정지", centerX, centerY - 100, pauseTextPaint)
        }
        
        // 게임 오버 상태 표시
        if (isGameOver) {
            val textPaint = Paint().apply {
                color = Color.RED
                textSize = 100f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("GAME OVER", centerX, centerY, textPaint)
        }
    }
    
    // 포커 족보 효과 적용
    fun applyPokerHandEffect(pokerHand: PokerHand) {
        activePokerHand = pokerHand
        buffManager.addPokerHandBuff(pokerHand)
    }
    
    // 현재 적용된 포커 족보 효과 정보 반환
    fun getActivePokerHandInfo(): String {
        return activePokerHand?.let {
            "${it.handName}: ${it.getDescription()}"
        } ?: "없음"
    }
    
    // 유닛 상태 정보 접근자 메서드들
    fun getUnitHealth(): Int = unitHealth
    
    fun getUnitMaxHealth(): Int = unitMaxHealth
    
    fun getUnitAttack(): Int {
        val baseDamage = unitAttackPower
        val multiplier = buffManager.getMissileDamageMultiplier()
        return (baseDamage * multiplier).toInt()
    }
    
    fun getUnitAttackSpeed(): Float {
        val baseSpeed = unitAttackSpeed
        val multiplier = buffManager.getAttackSpeedMultiplier()
        return baseSpeed * multiplier
    }
    
    fun getActiveBuffs(): List<Buff> {
        return buffManager.getAllBuffs()
    }
    
    // 디펜스 유닛 버프 목록 가져오기
    fun getDefenseBuffs(): List<Buff> {
        return buffManager.getDefenseBuffs()
    }
    
    // 적 너프 목록 가져오기
    fun getEnemyNerfs(): List<Buff> {
        return buffManager.getEnemyNerfs()
    }
    
    // 게임 상태 접근자 메서드들
    fun getResource(): Int = resource
    
    fun getWaveCount(): Int = waveCount
    
    fun getKillCount(): Int = killCount
    
    fun getTotalEnemiesInWave(): Int = totalEnemiesInWave
    
    // 현재 유닛 스탯 정보 가져오기
    fun getUnitAttackRange(): Float = unitAttackRange
    
    // 현재 업그레이드 비용 정보 반환
    fun getDamageCost(): Int = damageCost
    fun getAttackSpeedCost(): Int = attackSpeedCost
    fun getAttackRangeCost(): Int = attackRangeCost
    fun getDefenseCost(): Int = defenseCost
    
    // 현재 업그레이드 레벨 정보 반환
    fun getDamageLevel(): Int = damageLevel
    fun getAttackSpeedLevel(): Int = attackSpeedLevel
    fun getAttackRangeLevel(): Int = attackRangeLevel
    fun getDefenseLevel(): Int = defenseLevel
    
    // 게임 오버 콜백 설정
    fun setGameOverListener(listener: GameOverListener) {
        gameOverListener = listener
    }
    
    // 게임 재시작
    fun restartGame() {
        // 게임 상태 초기화
        resource = 0
        waveCount = 1
        killCount = 0
        spawnedCount = 0
        unitHealth = GameConfig.DEFENSE_UNIT_INITIAL_HEALTH
        unitMaxHealth = GameConfig.DEFENSE_UNIT_INITIAL_MAX_HEALTH
        unitAttackPower = GameConfig.MISSILE_DAMAGE
        unitAttackSpeed = GameConfig.DEFENSE_UNIT_ATTACK_COOLDOWN
        unitAttackRange = GameConfig.DEFENSE_UNIT_ATTACK_RANGE
        
        // 업그레이드 레벨 및 비용 초기화
        damageLevel = 1
        attackSpeedLevel = 1
        attackRangeLevel = 1
        defenseLevel = 1
        damageCost = GameConfig.DAMAGE_UPGRADE_INITIAL_COST
        attackSpeedCost = GameConfig.ATTACK_SPEED_UPGRADE_INITIAL_COST
        attackRangeCost = GameConfig.ATTACK_RANGE_UPGRADE_INITIAL_COST
        defenseCost = GameConfig.DEFENSE_UPGRADE_INITIAL_COST
        
        // 게임 요소 초기화
        enemies.clear()
        missiles.clear()
        
        // 게임 상태 초기화
        isGameOver = false
        isPaused = false
        
        // 버프 초기화
        buffManager.clearAllBuffs()
        
        // 게임 시작 시간 재설정
        gameStartTime = System.currentTimeMillis()
        lastEnemySpawnTime = gameStartTime
    }
    
    inner class GameThread : Thread() {
        override fun run() {
            while (isRunning) {
                val canvas = holder.lockCanvas() ?: continue
                try {
                    update()
                    renderGame(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                
                // 60 FPS 유지
                try {
                    sleep(16)
                } catch (e: InterruptedException) {
                    // 무시
                }
            }
        }
    }
} 