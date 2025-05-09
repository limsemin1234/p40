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
    private var timeFrozen = false  // 시간 멈춤 상태 변수 추가
    private var rangeBasedTimeFrozen = false  // 범위 기반 시간 멈춤 상태 변수
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val missiles = CopyOnWriteArrayList<Missile>()
    
    // 동기화를 위한 락 객체
    private val missilesLock = Any()
    
    // 객체 풀 인스턴스
    private val enemyPool = EnemyPool.getInstance()
    private val missilePool = MissilePool.getInstance()
    
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // 타이밍 관련
    private var lastEnemySpawnTime = 0L
    private var gameStartTime = 0L
    
    // 무적 상태 변수 추가
    private var isInvincible = false
    
    /**
     * 보스 소환 조건 체크
     */
    private var lastBossCheckTime = 0L
    private val bossForceSpawnDelay = 5000L // 5초 후 강제 소환
    
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
        lastBossCheckTime = 0L  // 보스 체크 타이머 초기화
        isRunning = true
        isGameOver = false
        isPaused = false
        
        // 게임 요소 초기화
        enemies.clear()
        missiles.clear()
        
        // 게임 시작 시 1웨이브 메시지 표시
        showWaveMessage = true
        waveMessageStartTime = System.currentTimeMillis()
    }
    
    /**
     * 시간 멈춤 상태 설정 (플러시 스킬용)
     */
    fun setTimeFrozen(frozen: Boolean) {
        this.timeFrozen = frozen
        
        // 범위 기반 시간 멈춤이 설정될 때 기존 전체 시간 멈춤은 비활성화
        if (!frozen) {
            this.rangeBasedTimeFrozen = false
        }
    }
    
    /**
     * 범위 기반 시간 멈춤 상태 설정 (클로버 플러시 스킬용)
     */
    fun setRangeBasedTimeFrozen(frozen: Boolean) {
        this.rangeBasedTimeFrozen = frozen
        
        // 범위 기반 시간 멈춤이 켜지면 전체 시간 멈춤은 끄기
        if (frozen) {
            this.timeFrozen = false
        }
    }
    
    /**
     * 적이 시간 멈춤 범위 내에 있는지 확인
     */
    private fun isEnemyInTimeFrozenRange(enemy: Enemy): Boolean {
        if (!rangeBasedTimeFrozen) return false
        
        // 방어 유닛의 위치와 공격 범위 가져오기
        val centerX = defenseUnit.getPosition().x
        val centerY = defenseUnit.getPosition().y
        val attackRange = defenseUnit.attackRange
        
        // 적과 방어 유닛 사이의 거리 계산
        val enemyPos = enemy.getPosition()
        val dx = enemyPos.x - centerX
        val dy = enemyPos.y - centerY
        val distanceSquared = dx * dx + dy * dy
        
        // 공격 범위 내에 있는지 확인
        return distanceSquared <= (attackRange * attackRange)
    }
    
    /**
     * 무적 상태 설정 (다이아몬드 플러시 스킬용)
     */
    fun setInvincible(invincible: Boolean) {
        this.isInvincible = invincible
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
        
        // 웨이브 내 적 생성 (웨이브 메시지와 상관없이 항상 적 생성)
        if (gameStats.getWaveCount() <= gameConfig.getTotalWaves()) {
            handleEnemySpawning(currentTime)
        }
        
        // 보스 소환 조건 체크
        checkBossSpawnCondition(currentTime)
        
        // 화면 범위 계산 (성능 최적화를 위한 값)
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 적 생성 거리보다 크게 visibleMargin 설정 (적 생성 거리 + 여유 공간)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * gameConfig.ENEMY_SPAWN_DISTANCE_FACTOR
        val visibleMargin = spawnDistance + gameConfig.ENEMY_UPDATE_MARGIN // GameConfig에서 여유 공간 값 사용
        
        val farOffScreenMargin = gameConfig.FAR_OFFSCREEN_MARGIN // GameConfig에서 설정값 사용
        
        // 화면 범위 계산 (화면 밖 일정 거리까지 포함)
        val minX = -visibleMargin
        val minY = -visibleMargin
        val maxX = screenWidth + visibleMargin
        val maxY = screenHeight + visibleMargin
        
        // 이동 속도 계산 - 버프 삭제로 기본값 사용
        val enemySpeedMultiplier = 1.0f
        
        // 1. 화면 내부 또는 가장자리에 있는 적만 업데이트 (최적화)
        val screenRect = ScreenRect(minX, minY, maxX, maxY)
        val deadEnemies = mutableListOf<Enemy>()
        
        // 시간 멈춤 상태가 아닐 때만 적 업데이트
        if (!timeFrozen) {
            updateEnemies(screenRect, farOffScreenMargin, centerX, centerY, enemySpeedMultiplier, deadEnemies)
        }
        
        // 2. 디펜스 유닛 공격 로직 (미사일 발사)
        updateDefenseUnitAttack(screenRect, currentTime)
        
        // 3. 미사일 업데이트 최적화
        updateMissiles(screenRect, farOffScreenMargin)
        
        // 4. 버프 효과 처리
        applyBuffEffects(currentTime, screenRect)
        
        // 5. 적 처리 및 점수 계산
        processDeadEnemies(deadEnemies)
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
        val currentTime = System.currentTimeMillis()
        
        // 업데이트 체크를 위한 카운터 추가
        var updatedCount = 0
        var notUpdatedCount = 0
        
        for (enemy in enemies) {
            val enemyPos = enemy.getPosition()
            
            // 화면 범위에서 멀리 벗어난 적은 자동 제거 (최적화)
            if (enemyPos.x < -farOffScreenMargin || enemyPos.x > screenWidth + farOffScreenMargin ||
                enemyPos.y < -farOffScreenMargin || enemyPos.y > screenHeight + farOffScreenMargin) {
                deadEnemies.add(enemy)
                // 화면 밖으로 완전히 벗어난 적의 경우 처리된 것으로 간주
                if (!enemy.isBoss()) {
                    enemy.takeDamage(1000) // 충분한 데미지를 줘서 죽은 것으로 처리
                }
                continue
            }
            
            // 화면 안쪽이나 가장자리에 있는 적만 업데이트
            if (screenRect.contains(enemyPos.x, enemyPos.y)) {
                // 범위 기반 시간 멈춤 체크 - 범위 내에 있으면 이동하지 않음
                val isFrozen = rangeBasedTimeFrozen && isEnemyInTimeFrozenRange(enemy)
                
                if (!isFrozen) {
                    // 버프에 의한 적 이동 속도 조정 적용
                    enemy.update(enemySpeedMultiplier)
                    
                    // 적 위치가 변경되었으므로 디펜스 유닛의 캐시 업데이트
                    defenseUnit.updateEnemyPosition(enemy)
                    
                    // 적이 이동한 후 범위 안으로 들어왔는지 체크
                    if (rangeBasedTimeFrozen && isEnemyInTimeFrozenRange(enemy)) {
                        // 범위 안으로 들어온 적은 움직임 멈춤
                        // 이미 이번 프레임에서 이동했지만 다음 프레임부터 멈춤
                    }
                }
                
                // 중앙에 도달했는지 확인
                val dx = enemyPos.x - centerX
                val dy = enemyPos.y - centerY
                val distanceToCenter = kotlin.math.sqrt(dx * dx + dy * dy)
                
                updatedCount++
                
                try {
                    // 디펜스 유닛과의 충돌 처리
                    if (distanceToCenter < gameConfig.DEFENSE_UNIT_SIZE) {
                        // 중앙에 도달한 적 처리
                        if (enemy.isBoss()) {
                            // 보스의 경우 다르게 처리 (죽이지는 않음)
                            // 플레이어에게 데미지를 주고 약간 밀어내기
                            val enemyDamage = enemy.getDamage()
                            
                            // 무적 상태가 아닐 때만 데미지 적용
                            val isUnitDead = if (!isInvincible) {
                                gameStats.applyDamageToUnit(enemyDamage)
                            } else {
                                false // 무적 상태일 때는 데미지를 입지 않음
                            }
                            
                            // 보스를 약간 밀어내기 (중앙에서 같은 방향으로 조금 더 멀어지게)
                            val pushDistance = gameConfig.DEFENSE_UNIT_SIZE * 1.5f
                            val pushDirX = if (dx != 0f) dx / distanceToCenter else 0f
                            val pushDirY = if (dy != 0f) dy / distanceToCenter else 0f
                            
                            // NaN이나 무한대 값 검사
                            val newX = if (pushDirX.isNaN() || pushDirX.isInfinite()) enemyPos.x else enemyPos.x + pushDirX * pushDistance
                            val newY = if (pushDirY.isNaN() || pushDirY.isInfinite()) enemyPos.y else enemyPos.y + pushDirY * pushDistance
                            
                            // 유효한 위치 값인지 확인 (화면 내부로 제한)
                            val safeX = newX.coerceIn(0f, screenWidth)
                            val safeY = newY.coerceIn(0f, screenHeight)
                            
                            enemy.setPosition(safeX, safeY)
                            
                            if (gameConfig.DEBUG_MODE) {
                                // 디버그 로그 제거
                            }
                            
                            // 체력이 0 이하일 때만 게임오버 처리
                            if (isUnitDead && !isGameOver) {
                                isGameOver = true
                                // 게임 오버 처리 - UI 스레드에서 실행
                                Handler(Looper.getMainLooper()).post {
                                    gameOverListener?.onGameOver(gameStats.getResource(), gameStats.getWaveCount())
                                }
                            }
                        } else {
                            // 공중적과 일반 적은 기존 방식대로 처리
                            enemy.takeDamage(gameConfig.CENTER_REACHED_DAMAGE) // 중앙에 도달하면 죽음
                            
                            // 적의 공격력에 따라 디펜스 유닛 체력 감소 (무적 상태가 아닐 때만)
                            if (!isInvincible) {
                                val enemyDamage = enemy.getDamage()
                                val isUnitDead = gameStats.applyDamageToUnit(enemyDamage)
                                
                                // 체력이 0 이하일 때만 게임오버 처리
                                if (isUnitDead && !isGameOver) {
                                    isGameOver = true
                                    // 게임 오버 처리 - UI 스레드에서 실행
                                    Handler(Looper.getMainLooper()).post {
                                        gameOverListener?.onGameOver(gameStats.getResource(), gameStats.getWaveCount())
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 충돌 처리 중 예외 발생 시 적 제거하고 로그 출력
                    if (gameConfig.DEBUG_MODE) {
                        // 디버그 로그 제거
                    }
                    deadEnemies.add(enemy)
                }
            } else {
                notUpdatedCount++
            }
            
            // 죽은 적 확인
            if (enemy.isDead()) {
                deadEnemies.add(enemy)
            }
        }
        
        // 업데이트 현황 로깅 (디버깅용)
        if (currentTime % 3000 < 20) { // 3초마다 로그
            // 디버그 로그 제거
        }
    }
    
    /**
     * 미사일 업데이트 처리
     */
    private fun updateMissiles(
        screenRect: ScreenRect,
        farOffScreenMargin: Float
    ) {
        // 현재 미사일 상태의 스냅샷을 만들어 작업 (CopyOnWriteArrayList를 안전하게 활용)
        val deadMissiles = mutableListOf<Missile>()
        val currentMissiles: List<Missile>
        
        // 미사일 목록의 스냅샷 생성 (스레드 안전하게)
        synchronized(missilesLock) {
            currentMissiles = ArrayList(missiles) // 완전히 새로운 리스트로 복사
        }
        
        // 디버깅을 위한 미사일 카운터
        val initialCount = currentMissiles.size
        var processedCount = 0
        var collidedCount = 0
        
        for (missile in currentMissiles) {
            processedCount++
            val missilePos = missile.getPosition()
            val missileId = missile.getId()
            
            // 이미 제거된 미사일은 건너뛰기
            if (missile.isDead()) {
                deadMissiles.add(missile)
                continue
            }
            
            // 화면 밖으로 완전히 벗어난 미사일은 즉시 제거 표시
            if (missilePos.x < -farOffScreenMargin || missilePos.x > screenWidth + farOffScreenMargin ||
                missilePos.y < -farOffScreenMargin || missilePos.y > screenHeight + farOffScreenMargin) {
                missile.setOutOfBounds()
                deadMissiles.add(missile)
                continue
            }
            
            // 화면 내부 또는 가까운 범위의 미사일만 업데이트
            if (screenRect.contains(missilePos.x, missilePos.y)) {
                // 항상 원래의 속도로 이동 (웨이브에 관계없이 동일한 속도)
                missile.update(1.0f)
                
                // 미사일이 죽지 않았고 화면 내부에 있는 경우에만 충돌 체크
                if (!missile.isDead()) {
                    var hasCollided = false
                    
                    // 화면 내부의 적들과만 충돌 체크 (최적화)
                    for (enemy in enemies) {
                        // 죽은 적과는 충돌 체크하지 않음
                        if (enemy.isDead()) continue
                        
                        val enemyPos = enemy.getPosition()
                        if (screenRect.contains(enemyPos.x, enemyPos.y)) {
                            if (missile.checkCollision(enemy)) {
                                // 하트 문양 효과: 적 공격 시 GameConfig에 정의된 양만큼 체력 회복
                                if (defenseUnit.isHealOnDamage()) {
                                    gameStats.healUnit(GameConfig.HEART_HEAL_ON_DAMAGE)
                                }
                                
                                // 충돌 발생
                                hasCollided = true
                                collidedCount++
                                deadMissiles.add(missile)
                                break
                            }
                        }
                    }
                    
                    // 충돌이 발생했으면 다음 미사일로 넘어감
                    if (hasCollided) continue
                }
            }
            
            // 죽은 미사일 확인
            if (missile.isDead()) {
                deadMissiles.add(missile)
            }
        }
        
        // 미사일 제거 및 풀로 반환 (중복 제거 해결)
        if (deadMissiles.isNotEmpty()) {
            // 제거할 미사일이 있는 경우에만 처리
            val uniqueDeadMissiles = deadMissiles.distinctBy { it.getId() }.toSet()
            
            // 스레드 안전하게 미사일 제거
            synchronized(missilesLock) {
                missiles.removeAll(uniqueDeadMissiles)
            }
            
            // 미사일 객체 풀에 반환
            uniqueDeadMissiles.forEach { missilePool.recycle(it) }
        }
    }
    
    /**
     * 디펜스 유닛 공격 처리
     */
    private fun updateDefenseUnitAttack(screenRect: ScreenRect, currentTime: Long) {
        // 공격 쿨다운 계산
        val attackCooldown = gameStats.getUnitAttackSpeed()
        
        // 모든 적을 대상으로 공격 처리 (죽지 않은 모든 적)
        val aliveEnemies = enemies.filter { !it.isDead() }
        
        // 적이 없으면 처리하지 않음
        if (aliveEnemies.isEmpty()) return
        
        // 기본 1방향 발사
        val missileDamageMultiplier = gameStats.getBuffManager().getMissileDamageMultiplier()
        
        // 업그레이드된 공격력 값 전달
        val baseDamage = gameStats.getUnitAttackPower()
        
        val newMissile = defenseUnit.attack(
            aliveEnemies, 
            currentTime, 
            attackCooldown,
            missileDamageMultiplier,
            0.0,  // 기본 각도 오프셋
            baseDamage  // 업그레이드된 공격력 전달
        )
        
        // 새 미사일 추가 - 스레드 안전하게
        if (newMissile != null) {
            synchronized(missilesLock) {
                // 추가하기 전에 기존 미사일이 있는지 확인
                val beforeCount = missiles.size
                
                // 새 미사일 추가
                missiles.add(newMissile)
                
                // 정말 추가되었는지 확인
                val afterCount = missiles.size
                if (afterCount <= beforeCount) {
                    // 미사일이 추가되지 않은 경우 (드문 경우지만 확인)
                    missilePool.recycle(newMissile) // 미사일 반환
                }
            }
        }
    }
    
    /**
     * 버프 효과 처리
     */
    private fun applyBuffEffects(currentTime: Long, screenRect: ScreenRect) {
        // 플러시 스킬 효과는 다른 곳에서 처리됨
    }
    
    /**
     * 죽은 적 처리
     */
    private fun processDeadEnemies(deadEnemies: List<Enemy>) {
        if (deadEnemies.isEmpty()) return
        
        // 디버그 로그 제거
        
        // 킬 카운트 및 점수(자원) 갱신
        for (enemy in deadEnemies) {
            if (!enemies.remove(enemy)) {
                // 디버그 로그 제거
                continue
            } // 이미 제거된 경우 스킵
            
            if (enemy.isDead()) { // 실제로 죽은 경우만 점수 처리
                val isBossKilled = gameStats.enemyKilled(enemy.isBoss())
                
                // 객체 풀에 적 반환 (재사용)
                enemyPool.recycle(enemy)
                
                // 디버그 로그 제거
                
                if (isBossKilled && enemy.isDead()) {
                    // 현재 웨이브 저장 (다음 웨이브로 넘어가기 전)
                    val currentWave = gameStats.getWaveCount()
                    
                    // 보스 처치 이벤트 발생
                    // 디버그 로그 제거
                    bossKillListener?.onBossKilled(currentWave)
                    
                    // 보스 처치 시 다음 웨이브로 이동
                    if (currentWave < gameConfig.getTotalWaves()) {
                        startNextWave()
                    }
                }
            } else {
                // 디버그 로그 제거
                enemyPool.recycle(enemy)
            }
        }
    }
    
    /**
     * 적 생성 처리
     */
    private fun handleEnemySpawning(currentTime: Long) {
        try {
            val waveCount = gameStats.getWaveCount()
            val spawnedCount = gameStats.getSpawnedCount()
            val totalEnemiesInWave = gameStats.getTotalEnemiesInWave()
            val isBossSpawned = gameStats.isBossSpawned()
            
            // 디버그: 현재 적 상태 및 생성 조건 확인
            if (currentTime % 3000 < 20) {  // 3초마다 한 번씩만 로그 출력
                // 디버그 로그 제거
            }
            
            // 웨이브당 정확히 적 수만큼만 생성되도록 수정
            if (spawnedCount < totalEnemiesInWave && !isBossSpawned && !isGameOver) {
                
                // 웨이브에 맞는 적 생성 간격으로 적 생성
                val spawnCooldown = gameConfig.getEnemySpawnIntervalForWave(waveCount)
                
                if (currentTime - lastEnemySpawnTime > spawnCooldown) {
                    // 디버그 로그 제거
                    
                    val prevCount = enemies.size
                    spawnEnemy()
                    val newCount = enemies.size
                    lastEnemySpawnTime = currentTime
                    
                    // 디버그 로그 제거
                }
            }
        } catch (e: Exception) {
            // 예외 처리만 하고 로그는 출력하지 않음
        }
    }
    
    /**
     * 적 생성
     */
    private fun spawnEnemy() {
        try {
            // 최대 적 수 제한 확인
            if (enemies.size >= gameConfig.MAX_ENEMIES) {
                // 디버그 로그 제거
                return
            }
            
            val centerX = screenWidth / 2
            val centerY = screenHeight / 2
            
            // 화면 가장자리에서 적 스폰
            val angle = Random.nextDouble(0.0, 2 * PI)
            val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * gameConfig.ENEMY_SPAWN_DISTANCE_FACTOR
            
            val spawnX = centerX + cos(angle).toFloat() * spawnDistance
            val spawnY = centerY + sin(angle).toFloat() * spawnDistance
            
            // 생성 좌표가 화면 범위를 벗어나는지 확인
            val isOffScreen = spawnX < 0 || spawnX > screenWidth || spawnY < 0 || spawnY > screenHeight
            
            // 디버그 로그 제거
            
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
            
            // 생성된 적 추가 및 스폰 카운트 증가
            enemies.add(enemy)
            gameStats.incrementSpawnCount()
            
            // 디버그 로그 제거
            
        } catch (e: Exception) {
            // 예외 처리만 하고 로그는 출력하지 않음
        }
    }
    
    /**
     * 보스 생성
     */
    private fun spawnBoss() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 보스는 랜덤한 방향에서 생성
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * gameConfig.BOSS_SPAWN_DISTANCE_FACTOR // 보스는 더 멀리서 생성
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        val waveCount = gameStats.getWaveCount()
        // 현재 웨이브에 맞는 보스 능력치 설정
        val speed = gameConfig.getEnemySpeedForWave(waveCount, true)
        val health = gameConfig.getEnemyHealthForWave(waveCount, true)
        val bossSize = gameConfig.BOSS_SIZE // 배율이 아닌 직접 크기 사용
        
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
            
            // 웨이브 시작 준비 시간 설정 (웨이브 메시지가 끝나면 즉시 적 생성 시작)
            lastEnemySpawnTime = System.currentTimeMillis()
            
            // 보스 타이머 초기화
            lastBossCheckTime = 0L
        } catch (e: Exception) {
            // 예외 처리만 하고 로그는 출력하지 않음
        }
    }
    
    /**
     * 보스 소환 조건 체크
     */
    private fun checkBossSpawnCondition(currentTime: Long) {
        // 이미 보스가 소환되었으면 체크하지 않음
        if (gameStats.isBossSpawned()) {
            return
        }
        
        val spawnedCount = gameStats.getSpawnedCount()
        val totalEnemiesInWave = gameStats.getTotalEnemiesInWave()
        
        // 모든 일반 적이 생성되면 타이머 시작
        if (spawnedCount >= totalEnemiesInWave) {
            // 첫 체크 시 시간 기록
            if (lastBossCheckTime == 0L) {
                lastBossCheckTime = currentTime
                // 디버그 로그 제거
            }
            
            // 현재 진행 상황 로깅
            if (currentTime % 1000 < 20) { // 1초마다 로그
                val timeLeft = bossForceSpawnDelay - (currentTime - lastBossCheckTime)
                // 디버그 로그 제거
            }
            
            // 5초 경과 후 보스 소환
            if (currentTime - lastBossCheckTime > bossForceSpawnDelay) {
                // 디버그 로그 제거
                
                // 보스 소환
                spawnBoss()
                gameStats.spawnBoss()
                
                // 디버그 로그 제거
                
                // 타이머 초기화
                lastBossCheckTime = 0L
            }
        } else if (currentTime % 5000 < 20) { // 5초마다 로그
            // 디버그 로그 제거
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
        lastBossCheckTime = 0L  // 보스 체크 타이머 초기화
        
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
    
    /**
     * 화면 내 모든 적 제거 (보스 제외) - 플러시 스킬용
     * @return 제거된 적의 수
     */
    fun removeAllEnemiesExceptBoss(): Int {
        var killCount = 0
        
        // 디펜스 유닛의 위치와 공격 범위 가져오기
        val centerX = defenseUnit.getPosition().x
        val centerY = defenseUnit.getPosition().y
        val attackRange = defenseUnit.attackRange
        
        // 보스가 아니고 공격 범위 내에 있는 적만 제거
        val enemiesToRemove = enemies.filter { enemy -> 
            if (enemy.isBoss()) return@filter false
            
            // 적과 방어 유닛 사이의 거리 계산
            val enemyPos = enemy.getPosition()
            val dx = enemyPos.x - centerX
            val dy = enemyPos.y - centerY
            val distanceSquared = dx * dx + dy * dy
            
            // 공격 범위 내에 있는지 확인
            distanceSquared <= (attackRange * attackRange)
        }
        
        killCount = enemiesToRemove.size
        
        // 적 제거
        for (enemy in enemiesToRemove) {
            // GameConfig에 정의된 스페이드 플러시 데미지를 적용
            enemy.takeDamage(gameConfig.SPADE_FLUSH_DAMAGE)
            enemies.remove(enemy)
            // 적 객체를 풀에 반환
            enemyPool.recycle(enemy)
        }
        
        return killCount
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
    
    /**
     * 현재 보스 체력 반환
     * 목록에서 보스를 찾아 체력 반환, 없으면 0
     */
    fun getCurrentBossHealth(): Int {
        // enemies 목록에서 보스 찾기
        val boss = enemies.find { it.isBoss() && !it.isDead() }
        return boss?.getHealth() ?: 0
    }
} 