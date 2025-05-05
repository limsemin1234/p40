package com.example.p40.game

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 게임 논리 처리 클래스
 * GameView에서 게임 논리(적 생성, 충돌, 업데이트 등) 관련 로직을 분리함
 */
class GameLogic(
    private val gameStats: GameStats,
    private val gameConfig: GameConfig,
    private val gameOverListener: GameOverListener? = null,
    private val bossKillListener: BossKillListener? = null
) {
    // 게임 상태
    private var isRunning = false
    private var isPaused = false
    private var isGameOver = false
    private var showWaveMessage = false
    private var waveMessageStartTime = 0L
    private var waveMessageDuration = gameConfig.WAVE_MESSAGE_DURATION
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val missiles = CopyOnWriteArrayList<Missile>()
    
    // 객체 풀 인스턴스
    private val enemyPool = EnemyPool.getInstance()
    private val missilePool = MissilePool.getInstance()
    
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // 타이밍 관련
    private var lastEnemySpawnTime = 0L
    private var gameStartTime = 0L
    
    /**
     * 게임 초기화
     */
    fun initGame(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        
        // 중앙에 방어 유닛 배치
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        defenseUnit = DefenseUnit(
            position = PointF(centerX, centerY),
            attackRange = gameStats.getUnitAttackRange(),
            attackCooldown = gameStats.getUnitAttackSpeed()
        )
        
        gameStartTime = System.currentTimeMillis()
        lastEnemySpawnTime = gameStartTime
        isRunning = true
        isGameOver = false
        isPaused = false
        
        // 게임 요소 초기화
        enemies.clear()
        missiles.clear()
    }
    
    /**
     * 게임 업데이트 메인 로직
     */
    fun update() {
        if (isPaused || isGameOver) return
        
        val currentTime = System.currentTimeMillis()
        
        // 웨이브 메시지 표시 시간 체크
        if (showWaveMessage && currentTime - waveMessageStartTime > waveMessageDuration) {
            showWaveMessage = false
        }
        
        // 보스를 처치하고 다음 웨이브로 넘어가거나 웨이브 내 적 생성
        if (!showWaveMessage) {
            if (gameStats.getWaveCount() <= gameConfig.getTotalWaves()) {
                handleEnemySpawning(currentTime)
            }
        }
        
        // 화면 범위 계산 (성능 최적화를 위한 값)
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val visibleMargin = 100f // 화면 밖 여유 공간
        val farOffScreenMargin = 300f // 더 먼 화면 밖 거리
        
        // 화면 범위 계산 (화면 밖 일정 거리까지 포함)
        val minX = -visibleMargin
        val minY = -visibleMargin
        val maxX = screenWidth + visibleMargin
        val maxY = screenHeight + visibleMargin
        
        // 버프에 의한 이동 속도 계산 (한 번만 계산하여 재사용)
        val enemySpeedMultiplier = gameStats.getEnemySpeedMultiplier()
        val missileSpeedMultiplier = gameStats.getMissileSpeedMultiplier()
        
        // 1. 화면 내부 또는 가장자리에 있는 적만 업데이트 (최적화)
        val screenRect = ScreenRect(minX, minY, maxX, maxY)
        val deadEnemies = mutableListOf<Enemy>()
        
        updateEnemies(screenRect, farOffScreenMargin, centerX, centerY, enemySpeedMultiplier, deadEnemies)
        
        // 2. 미사일 업데이트 최적화
        updateMissiles(screenRect, farOffScreenMargin, missileSpeedMultiplier)
        
        // 3. 방어 유닛 공격 로직
        updateDefenseUnitAttack(screenRect, currentTime)
        
        // 4. 버프 효과 처리
        applyBuffEffects(currentTime, screenRect)
        
        // 5. 적 처리 및 점수 계산
        processDeadEnemies(deadEnemies)
        
        // 웨이브의 일반 적 모두 처치 시 보스 소환
        if (!gameStats.isBossSpawned() && gameStats.getKillCount() >= gameStats.getTotalEnemiesInWave()) {
            spawnBoss()
            gameStats.spawnBoss()
        }
    }
    
    /**
     * 적 업데이트 처리
     */
    private fun updateEnemies(
        screenRect: ScreenRect,
        farOffScreenMargin: Float,
        centerX: Float,
        centerY: Float,
        enemySpeedMultiplier: Float,
        deadEnemies: MutableList<Enemy>
    ) {
        for (enemy in enemies) {
            val enemyPos = enemy.getPosition()
            
            // 화면 범위에서 멀리 벗어난 적은 자동 제거 (최적화)
            if (enemyPos.x < -farOffScreenMargin || enemyPos.x > screenWidth + farOffScreenMargin ||
                enemyPos.y < -farOffScreenMargin || enemyPos.y > screenHeight + farOffScreenMargin) {
                deadEnemies.add(enemy)
                continue
            }
            
            // 화면 안쪽이나 가장자리에 있는 적만 업데이트
            if (screenRect.contains(enemyPos.x, enemyPos.y)) {
                // 버프에 의한 적 이동 속도 조정 적용
                enemy.update(enemySpeedMultiplier)
                
                // 중앙에 도달했는지 확인
                val dx = enemyPos.x - centerX
                val dy = enemyPos.y - centerY
                val distanceToCenter = kotlin.math.sqrt(dx * dx + dy * dy)
                
                if (distanceToCenter < gameConfig.DEFENSE_UNIT_SIZE) {
                    enemy.takeDamage(gameConfig.CENTER_REACHED_DAMAGE) // 중앙에 도달하면 죽음
                    
                    // 적의 공격력에 따라 디펜스 유닛 체력 감소
                    val enemyDamage = enemy.getDamage()
                    val isUnitDead = gameStats.applyDamageToUnit(enemyDamage)
                    
                    // 체력이 0이 되면 게임 오버
                    if (isUnitDead && !isGameOver) {
                        isGameOver = true
                        // 게임 오버 처리 - UI 스레드에서 실행
                        Handler(Looper.getMainLooper()).post {
                            gameOverListener?.onGameOver(gameStats.getResource(), gameStats.getWaveCount())
                        }
                    }
                }
            }
            
            // 죽은 적 확인
            if (enemy.isDead()) {
                deadEnemies.add(enemy)
            }
        }
    }
    
    /**
     * 미사일 업데이트 처리
     */
    private fun updateMissiles(
        screenRect: ScreenRect,
        farOffScreenMargin: Float,
        missileSpeedMultiplier: Float
    ) {
        val deadMissiles = mutableListOf<Missile>()
        
        for (missile in missiles) {
            val missilePos = missile.getPosition()
            
            // 화면 밖으로 완전히 벗어난 미사일은 즉시 제거 표시
            if (missilePos.x < -farOffScreenMargin || missilePos.x > screenWidth + farOffScreenMargin ||
                missilePos.y < -farOffScreenMargin || missilePos.y > screenHeight + farOffScreenMargin) {
                missile.setOutOfBounds()
                deadMissiles.add(missile)
                continue
            }
            
            // 화면 내부 또는 가까운 범위의 미사일만 업데이트
            if (screenRect.contains(missilePos.x, missilePos.y)) {
                missile.update(missileSpeedMultiplier)
                
                // 미사일이 죽지 않았고 화면 내부에 있는 경우에만 충돌 체크
                if (!missile.isDead()) {
                    val pierceCount = gameStats.getMissilePierceCount()
                    var hitCount = 0
                    
                    // 화면 내부의 적들과만 충돌 체크 (최적화)
                    for (enemy in enemies) {
                        val enemyPos = enemy.getPosition()
                        if (screenRect.contains(enemyPos.x, enemyPos.y) && !enemy.isDead()) {
                            if (missile.checkCollision(enemy)) {
                                hitCount++
                                // 관통 횟수를 초과하면 미사일 제거
                                if (hitCount > pierceCount) {
                                    deadMissiles.add(missile)
                                    break
                                }
                            }
                        }
                    }
                }
            }
            
            // 죽은 미사일 확인
            if (missile.isDead()) {
                deadMissiles.add(missile)
            }
        }
        
        // 미사일 제거 및 풀로 반환
        if (deadMissiles.isNotEmpty()) {
            missiles.removeAll(deadMissiles)
            // 미사일 객체 풀에 반환
            deadMissiles.forEach { missilePool.recycle(it) }
        }
    }
    
    /**
     * 디펜스 유닛 공격 처리
     */
    private fun updateDefenseUnitAttack(screenRect: ScreenRect, currentTime: Long) {
        // 공격 쿨다운 계산 (한 번만 계산)
        val attackSpeedMultiplier = gameStats.getBuffManager().getAttackSpeedMultiplier()
        val adjustedAttackCooldown = (gameStats.getUnitAttackSpeed() * attackSpeedMultiplier).toLong()
        val missileDamageMultiplier = gameStats.getBuffManager().getMissileDamageMultiplier()
        
        // 화면 내 적이 있는 경우에만 공격 처리
        val screenEnemies = enemies.filter { 
            val pos = it.getPosition()
            screenRect.contains(pos.x, pos.y) && !it.isDead()
        }
        
        // 다방향 발사 지원
        val multiDirCount = gameStats.getMultiDirectionCount()
        if (multiDirCount > 1) {
            // 다방향 발사 (기본 1방향 + 추가 방향)
            val angleStep = (2 * Math.PI) / multiDirCount
            
            for (i in 0 until multiDirCount) {
                val newMissile = defenseUnit.attack(
                    screenEnemies, 
                    currentTime, 
                    adjustedAttackCooldown, 
                    missileDamageMultiplier,
                    i * angleStep
                )
                
                if (newMissile != null) {
                    missiles.add(newMissile)
                }
            }
        } else {
            // 기본 1방향 발사
            val newMissile = defenseUnit.attack(
                screenEnemies, 
                currentTime, 
                adjustedAttackCooldown,
                missileDamageMultiplier
            )
            
            if (newMissile != null) {
                missiles.add(newMissile)
            }
        }
    }
    
    /**
     * 버프 효과 처리
     */
    private fun applyBuffEffects(currentTime: Long, screenRect: ScreenRect) {
        val buffManager = gameStats.getBuffManager()
        val dotLevel = buffManager.getBuffLevel(BuffType.DOT_DAMAGE)
        val massLevel = buffManager.getBuffLevel(BuffType.MASS_DAMAGE)
        
        // DoT 효과 (1초마다)
        if (dotLevel > 0 && currentTime % 1000 < 20) {
            val dotDamage = dotLevel * 2 // 레벨당 2 데미지
            for (enemy in enemies) {
                if (!enemy.isDead() && screenRect.contains(enemy.getPosition().x, enemy.getPosition().y)) {
                    enemy.takeDamage(dotDamage)
                }
            }
        }
        
        // 대량 데미지 효과 (5초마다)
        if (massLevel > 0 && currentTime % 5000 < 20) {
            val massDamage = massLevel * 100 // 레벨당 100 데미지
            for (enemy in enemies) {
                if (!enemy.isDead() && screenRect.contains(enemy.getPosition().x, enemy.getPosition().y)) {
                    enemy.takeDamage(massDamage)
                }
            }
        }
    }
    
    /**
     * 죽은 적 처리
     */
    private fun processDeadEnemies(deadEnemies: List<Enemy>) {
        if (deadEnemies.isEmpty()) return
        
        // 킬 카운트 및 점수(자원) 갱신
        for (enemy in deadEnemies) {
            if (!enemies.remove(enemy)) continue // 이미 제거된 경우 스킵
            
            if (enemy.isDead()) { // 실제로 죽은 경우만 점수 처리
                val isBossKilled = gameStats.enemyKilled(enemy.isBoss())
                
                // 객체 풀에 적 반환 (재사용)
                enemyPool.recycle(enemy)
                
                if (isBossKilled) {
                    // 보스 처치 이벤트 발생
                    bossKillListener?.onBossKilled()
                    
                    // 보스 처치 시 다음 웨이브로 이동
                    if (gameStats.getWaveCount() < gameConfig.getTotalWaves()) {
                        startNextWave()
                    }
                }
            }
        }
    }
    
    /**
     * 적 생성 처리
     */
    private fun handleEnemySpawning(currentTime: Long) {
        try {
            val waveCount = gameStats.getWaveCount()
            // 웨이브당 정확히 적 수만큼만 생성되도록 수정
            if (gameStats.getSpawnedCount() < gameStats.getTotalEnemiesInWave() && 
                !gameStats.isBossSpawned() && !isGameOver) {
                
                // 웨이브에 맞는 적 생성 간격으로 적 생성
                val spawnCooldown = gameConfig.getEnemySpawnIntervalForWave(waveCount)
                
                if (currentTime - lastEnemySpawnTime > spawnCooldown) {
                    spawnEnemy()
                    lastEnemySpawnTime = currentTime
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 로그 출력 (실제 앱에서는 logger 사용)
            e.printStackTrace()
        }
    }
    
    /**
     * 보통 적 생성
     */
    private fun spawnEnemy() {
        // 적 수 제한 확인
        if (enemies.size >= gameConfig.MAX_ENEMIES) {
            // 가장 멀리 있는 적 제거 후 새로운 적 생성
            removeDistantEnemy()
        }
        
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 화면 가장자리에서 적 스폰
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * 0.6f
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        val waveCount = gameStats.getWaveCount()
        // 현재 웨이브에 맞는 적 능력치 설정
        val speed = gameConfig.getEnemySpeedForWave(waveCount)
        val health = gameConfig.getEnemyHealthForWave(waveCount)
        
        // 객체 풀에서 적 가져오기
        val enemy = enemyPool.obtain(
            position = PointF(spawnX, spawnY),
            target = PointF(centerX, centerY),
            speed = speed,
            health = health,
            wave = waveCount
        )
        
        enemies.add(enemy)
        gameStats.incrementSpawnCount()
    }
    
    /**
     * 보스 생성
     */
    private fun spawnBoss() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 보스는 랜덤한 방향에서 생성
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * 0.7f
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        val waveCount = gameStats.getWaveCount()
        // 현재 웨이브에 맞는 보스 능력치 설정 (일반 적보다 느리지만 크고 강함)
        val speed = gameConfig.getEnemySpeedForWave(waveCount, true)
        val health = gameConfig.getEnemyHealthForWave(waveCount, true)
        val bossSize = gameConfig.ENEMY_BASE_SIZE * gameConfig.BOSS_SIZE_MULTIPLIER
        
        // 객체 풀에서 보스 가져오기
        val boss = enemyPool.obtain(
            position = PointF(spawnX, spawnY),
            target = PointF(centerX, centerY),
            speed = speed,
            size = bossSize,
            health = health,
            isBoss = true,
            wave = waveCount
        )
        
        enemies.add(boss)
    }
    
    /**
     * 다음 웨이브 시작
     */
    private fun startNextWave() {
        try {
            // 다음 웨이브가 최대 웨이브를 초과하면 종료
            if (gameStats.getWaveCount() >= gameConfig.getTotalWaves()) return
            
            // 웨이브 상태 업데이트
            gameStats.nextWave()
            
            // 화면 바깥에 있는 적이나 일정 거리 이상 떨어진 적은 새 웨이브에서 제거
            val visibleMargin = 200f
            val enemiesInView = enemies.filter { enemy ->
                val pos = enemy.getPosition()
                pos.x >= -visibleMargin && pos.x <= screenWidth + visibleMargin &&
                pos.y >= -visibleMargin && pos.y <= screenHeight + visibleMargin
            }
            
            enemies.clear()
            enemies.addAll(enemiesInView)
            
            // 남아있는 적들의 웨이브 번호 업데이트
            val newWaveCount = gameStats.getWaveCount()
            enemies.forEach { enemy ->
                enemy.setWave(newWaveCount)
            }
            
            // 웨이브 시작 메시지 표시
            showWaveMessage = true
            waveMessageStartTime = System.currentTimeMillis()
            
            // 미사일 초기화
            missiles.clear()
            
            // 웨이브 시작 준비 시간 설정
            lastEnemySpawnTime = System.currentTimeMillis() + gameConfig.WAVE_MESSAGE_DURATION
        } catch (e: Exception) {
            // 예외 발생 시 로그 출력 (실제 앱에서는 logger 사용)
            e.printStackTrace()
        }
    }
    
    /**
     * 모든 적에게 데미지를 주는 카드 사용 효과
     */
    fun useCard() {
        enemies.forEach { enemy ->
            // 보스는 카드 효과가 약하게
            val damage = if (enemy.isBoss()) gameConfig.CARD_DAMAGE_BOSS else gameConfig.CARD_DAMAGE_NORMAL
            enemy.takeDamage(damage)
        }
    }
    
    /**
     * 일시정지 처리
     */
    fun pause() {
        isPaused = true
    }
    
    /**
     * 게임 재개
     */
    fun resume() {
        isPaused = false
    }
    
    /**
     * 게임 재시작
     */
    fun restartGame() {
        gameStats.resetGame()
        
        // 게임 요소 초기화 (및 객체 풀 반환)
        enemies.forEach { enemyPool.recycle(it) }
        enemies.clear()
        
        missiles.forEach { missilePool.recycle(it) }
        missiles.clear()
        
        // 게임 상태 초기화
        isGameOver = false
        isPaused = false
        
        // 게임 시작 시간 재설정
        gameStartTime = System.currentTimeMillis()
        lastEnemySpawnTime = gameStartTime
        
        // 디펜스 유닛 업데이트
        if (this::defenseUnit.isInitialized) {
            defenseUnit.setAttackRange(gameStats.getUnitAttackRange())
            defenseUnit.setAttackCooldown(gameStats.getUnitAttackSpeed())
        }
    }
    
    /**
     * 중앙에서 가장 멀리 있는 적 제거
     */
    private fun removeDistantEnemy() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        var farthestEnemy: Enemy? = null
        var maxDistanceSquared = 0f
        
        for (enemy in enemies) {
            val pos = enemy.getPosition()
            val dx = pos.x - centerX
            val dy = pos.y - centerY
            val distanceSquared = dx * dx + dy * dy
            
            if (distanceSquared > maxDistanceSquared) {
                maxDistanceSquared = distanceSquared
                farthestEnemy = enemy
            }
        }
        
        // 가장 멀리 있는 적 제거
        farthestEnemy?.let {
            enemies.remove(it)
            // 객체 풀로 반환
            enemyPool.recycle(it)
        }
    }
    
    // 게임 상태 접근자 메서드들
    fun isGamePaused(): Boolean = isPaused
    fun isGameOver(): Boolean = isGameOver
    fun isShowingWaveMessage(): Boolean = showWaveMessage
    
    // 게임 요소 접근자 메서드들
    fun getDefenseUnit(): DefenseUnit = defenseUnit
    fun getEnemies(): CopyOnWriteArrayList<Enemy> = enemies
    fun getMissiles(): CopyOnWriteArrayList<Missile> = missiles
    
    // 화면 정보 접근자 메서드들
    fun getScreenWidth(): Float = screenWidth
    fun getScreenHeight(): Float = screenHeight
    
    /**
     * 화면 영역 체크를 위한 헬퍼 클래스 (객체 할당 최소화)
     */
    private class ScreenRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun contains(x: Float, y: Float): Boolean {
            return x >= left && x <= right && y >= top && y <= bottom
        }
    }
} 