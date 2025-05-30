package com.example.p40

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 적 관리 클래스
 * 적 생성, 처리, 업데이트 등 적 관련 모든 기능을 관리
 */
class EnemyManager(
    private val context: Context,
    private val gameStats: GameStats,
    private val gameConfig: GameConfig,
    private val gameView: GameView,
    private val gameOverListener: GameOverListener? = null,
    private val bossKillListener: BossKillListener? = null
) {
    // 화면 정보
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    // 적 관리
    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val enemyPool = EnemyPool.getInstance()
    
    // 타이밍 관련
    private var lastEnemySpawnTime = 0L
    private var lastBossCheckTime = 0L
    private val bossForceSpawnDelay = 5000L // 5초 후 강제 소환
    
    // 무적 상태 변수
    private var isInvincible = false
    
    // 시간 멈춤 상태 변수
    private var timeFrozen = false
    private var rangeBasedTimeFrozen = false
    
    /**
     * 초기화
     */
    fun init(width: Float, height: Float, startTime: Long) {
        screenWidth = width
        screenHeight = height
        lastEnemySpawnTime = startTime
        lastBossCheckTime = 0L
        enemies.clear()
    }
    
    /**
     * 시간 멈춤 상태 설정
     */
    fun setTimeFrozen(frozen: Boolean) {
        this.timeFrozen = frozen
        // 범위 기반 시간 멈춤이 설정될 때 기존 전체 시간 멈춤은 비활성화
        if (!frozen) {
            this.rangeBasedTimeFrozen = false
        }
    }
    
    /**
     * 범위 기반 시간 멈춤 상태 설정
     */
    fun setRangeBasedTimeFrozen(frozen: Boolean) {
        this.rangeBasedTimeFrozen = frozen
        // 범위 기반 시간 멈춤이 켜지면 전체 시간 멈춤은 끄기
        if (frozen) {
            this.timeFrozen = false
        }
    }
    
    /**
     * 무적 상태 설정
     */
    fun setInvincible(invincible: Boolean) {
        this.isInvincible = invincible
    }
    
    /**
     * 적이 시간 멈춤 범위 내에 있는지 확인
     */
    fun isEnemyInTimeFrozenRange(enemy: Enemy, centerX: Float, centerY: Float, range: Float): Boolean {
        val enemyPos = enemy.getPosition()
        val dx = enemyPos.x - centerX
        val dy = enemyPos.y - centerY
        val distanceSquared = dx * dx + dy * dy
        
        // 제곱 값으로 비교하여 제곱근 연산 회피
        val rangeSquared = range * range
        return distanceSquared <= rangeSquared
    }
    
    /**
     * 적 생성 처리
     */
    fun handleEnemySpawning(currentTime: Long, isGameOver: Boolean) {
        try {
            val waveCount = gameStats.getWaveCount()
            val spawnedCount = gameStats.getSpawnedCount()
            val totalEnemiesInWave = gameStats.getTotalEnemiesInWave()
            val isBossSpawned = gameStats.isBossSpawned()
            
            // 웨이브당 정확히 적 수만큼만 생성
            if (spawnedCount < totalEnemiesInWave && !isBossSpawned && !isGameOver) {
                // 웨이브에 맞는 적 생성 간격으로 적 생성
                val spawnCooldown = EnemyConfig.getEnemySpawnIntervalForWave(waveCount)
                
                if (currentTime - lastEnemySpawnTime > spawnCooldown) {
                    spawnEnemy(currentTime)
                    lastEnemySpawnTime = currentTime
                }
            }
        } catch (e: Exception) {
            // 예외 처리
        }
    }
    
    /**
     * 적 생성
     */
    private fun spawnEnemy(currentTime: Long) {
        try {
            // 최대 적 수 제한 확인
            if (enemies.size >= GameConfig.MAX_ENEMIES) {
                return
            }
            
            val centerX = screenWidth / 2
            val centerY = screenHeight / 2
            
            val waveCount = gameStats.getWaveCount()
            
            // 현재 웨이브에서 공중적이 등장할 수 있는지 확인
            val canSpawnFlyingEnemy = waveCount >= EnemyConfig.FLYING_ENEMY_WAVE_THRESHOLD
            val isSpawningFlyingEnemy = canSpawnFlyingEnemy && Random.nextDouble() < EnemyConfig.FLYING_ENEMY_SPAWN_CHANCE
            
            // 화면 가장자리에서 적 스폰
            val angle = Random.nextDouble(0.0, 2 * PI)
            
            // 적 타입에 따라 다른 생성 거리 사용
            val spawnDistance = when {
                isSpawningFlyingEnemy -> screenWidth.coerceAtLeast(screenHeight) * EnemyConfig.FLYING_ENEMY_SPAWN_DISTANCE_FACTOR
                else -> screenWidth.coerceAtLeast(screenHeight) * EnemyConfig.ENEMY_SPAWN_DISTANCE_FACTOR
            }
            
            val spawnX = centerX + cos(angle).toFloat() * spawnDistance
            val spawnY = centerY + sin(angle).toFloat() * spawnDistance
            
            // 현재 웨이브에 맞는 적 능력치 설정
            val speed = EnemyConfig.getEnemySpeedForWave(waveCount, false, isSpawningFlyingEnemy)
            val health = EnemyConfig.getEnemyHealthForWave(waveCount, false, isSpawningFlyingEnemy)
            
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
        } catch (e: Exception) {
            // 예외 처리
        }
    }
    
    /**
     * 보스 생성
     */
    fun spawnBoss() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 보스는 랜덤한 방향에서 생성
        val angle = Random.nextDouble(0.0, 2 * PI)
        val spawnDistance = screenWidth.coerceAtLeast(screenHeight) * EnemyConfig.BOSS_SPAWN_DISTANCE_FACTOR
        
        val spawnX = centerX + cos(angle).toFloat() * spawnDistance
        val spawnY = centerY + sin(angle).toFloat() * spawnDistance
        
        val waveCount = gameStats.getWaveCount()
        // 현재 웨이브에 맞는 보스 능력치 설정
        val speed = EnemyConfig.getEnemySpeedForWave(waveCount, true)
        val health = EnemyConfig.getEnemyHealthForWave(waveCount, true)
        val bossSize = EnemyConfig.BOSS_SIZE
        
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
     * 보스 소환 조건 체크
     */
    fun checkBossSpawnCondition(currentTime: Long) {
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
            }
            
            // 5초 경과 후 보스 소환
            if (currentTime - lastBossCheckTime > bossForceSpawnDelay) {
                // 보스 소환
                spawnBoss()
                gameStats.spawnBoss()
                
                // 타이머 초기화
                lastBossCheckTime = 0L
            }
        }
    }
    
    /**
     * 적 업데이트 처리 - 그리드 기반 최적화 적용
     */
    fun updateEnemies(screenRect: ScreenRect, centerX: Float, centerY: Float, defenseUnit: DefenseUnit, isGameOver: Boolean): List<Enemy> {
        val deadEnemies = mutableListOf<Enemy>()
        
        // 시간 멈춤 상태면 처리 중단
        if (timeFrozen) return deadEnemies
        
        val farOffScreenMargin = EnemyConfig.FAR_OFFSCREEN_MARGIN
        
        // 화면 그리드 초기화
        screenRect.clearGrid()
        
        // 화면 밖 적 처리 최적화를 위한 영역 계산
        val updateRect = ScreenRect(
            left = -EnemyConfig.ENEMY_UPDATE_MARGIN,
            top = -EnemyConfig.ENEMY_UPDATE_MARGIN,
            right = screenWidth + EnemyConfig.ENEMY_UPDATE_MARGIN,
            bottom = screenHeight + EnemyConfig.ENEMY_UPDATE_MARGIN
        )
        
        // 방어 유닛 위치 미리 계산
        val defenseUnitPos = defenseUnit.getPosition()
        
        for (enemy in enemies) {
            val enemyPos = enemy.getPosition()
            
            // 화면 범위에서 뀌리 벗어난 적은 자동 제거 (최적화)
            if (enemyPos.x < -farOffScreenMargin || enemyPos.x > screenWidth + farOffScreenMargin ||
                enemyPos.y < -farOffScreenMargin || enemyPos.y > screenHeight + farOffScreenMargin) {
                deadEnemies.add(enemy)
                // 화면 밖으로 완전히 벗어난 적의 경우 처리된 것으로 간주
                if (!enemy.isBoss()) {
                    enemy.takeDamage(1000) // 충분한 데미지를 줘서 죽은 것으로 처리
                }
                continue
            }
            
            // 화면 그리드에 적 추가
            if (updateRect.contains(enemyPos.x, enemyPos.y)) {
                // 화면 근처 또는 내부의 적만 그리드에 추가
                screenRect.addObjectToGrid(enemy, enemyPos, enemy.getSize())
            }
            
            // 화면 안쪽이나 가장자리에 있는 적만 업데이트
            if (updateRect.contains(enemyPos.x, enemyPos.y)) {
                // 범위 기반 시간 멈춤 체크 - 범위 내에 있으면 이동하지 않음
                val isFrozen = rangeBasedTimeFrozen && isEnemyInTimeFrozenRange(
                    enemy, 
                    defenseUnitPos.x, 
                    defenseUnitPos.y,
                    defenseUnit.attackRange
                )
                
                if (!isFrozen) {
                    // 기본 이동 속도 적용
                    enemy.update(1.0f)
                    
                    // 적 위치가 변경되었으므로 디펜스 유닛의 캐시 업데이트 
                    defenseUnit.updateEnemyPosition(enemy)
                }
                
                // 중앙에 도달했는지 확인
                val dx = enemyPos.x - centerX
                val dy = enemyPos.y - centerY
                val distanceToCenter = dx * dx + dy * dy // 제곱근 연산 회피
                
                // 외곽 테두리 기반 충돌 감지로 변경
                // 디펜스 유닛 반지름과 적 유닛 반지름의 합의 제곱
                val defenseUnitRadius = GameConfig.DEFENSE_UNIT_SIZE
                val enemyRadius = enemy.getSize()
                val collisionDistanceSquared = (defenseUnitRadius + enemyRadius) * (defenseUnitRadius + enemyRadius)
                
                try {
                    // 디펜스 유닛과의 충돌 처리 - 두 원의 외곽선이 겹치는 경우 충돌로 판정
                    if (distanceToCenter < collisionDistanceSquared) {
                        handleEnemyCollision(enemy, dx, dy, Math.sqrt(distanceToCenter.toDouble()).toFloat(), deadEnemies, defenseUnit, isGameOver)
                    }
                } catch (e: Exception) {
                    // 충돌 처리 중 예외 발생 시 적 제거
                    deadEnemies.add(enemy)
                }
            }
            
            // 죽은 적 확인
            if (enemy.isDead()) {
                deadEnemies.add(enemy)
            }
        }
        
        return deadEnemies
    }
    
    /**
     * 적과 디펜스 유닛 충돌 처리
     */
    private fun handleEnemyCollision(enemy: Enemy, dx: Float, dy: Float, distanceToCenter: Float,
                                     deadEnemies: MutableList<Enemy>, defenseUnit: DefenseUnit, isGameOver: Boolean) {
        // 가시 데미지 설정 - 현재 업그레이드 레벨에 따른 데미지 적용
        val thornDamage = gameView.getCurrentThornDamage()
        
        // 적에게 가시 데미지 적용
        enemy.takeDamage(thornDamage)
        
        // 튕겨내기 계산 - 현재 업그레이드 레벨에 따른 밀치기 거리 적용
        val pushDistance = GameConfig.DEFENSE_UNIT_SIZE * gameView.getCurrentPushDistance()
        val pushDirX = if (dx != 0f) dx / distanceToCenter else 0f
        val pushDirY = if (dy != 0f) dy / distanceToCenter else 0f
        
        // NaN이나 무한대 값 검사
        val newX = if (pushDirX.isNaN() || pushDirX.isInfinite()) enemy.getPosition().x 
                  else enemy.getPosition().x + pushDirX * pushDistance
        val newY = if (pushDirY.isNaN() || pushDirY.isInfinite()) enemy.getPosition().y 
                  else enemy.getPosition().y + pushDirY * pushDistance
        
        // 유효한 위치 값인지 확인 (화면 내부로 제한)
        val safeX = newX.coerceIn(0f, screenWidth)
        val safeY = newY.coerceIn(0f, screenHeight)
        
        // 적 위치 변경 (튕겨내기)
        enemy.setPosition(safeX, safeY)
        
        // 만약 적이 죽었다면 deadEnemies에 추가
        if (enemy.isDead()) {
            deadEnemies.add(enemy)
            return
        }
        
        // 적의 공격력에 따라 디펜스 유닛 체력 감소 (무적 상태가 아닐 때만)
        if (!isInvincible) {
            val enemyDamage = enemy.getDamage()
            val isUnitDead = gameStats.applyDamageToUnit(enemyDamage)
            
            // 체력이 0 이하일 때만 게임오버 처리
            if (isUnitDead && !isGameOver) {
                notifyGameOver()
            }
        }
    }
    
    /**
     * 죽은 적 처리
     */
    fun processDeadEnemies(deadEnemies: List<Enemy>, onNextWave: () -> Unit) {
        if (deadEnemies.isEmpty()) return
        
        // 킬 카운트 및 점수(자원) 갱신
        for (enemy in deadEnemies) {
            if (!enemies.remove(enemy)) {
                continue
            }
            
            if (enemy.isDead()) { // 실제로 죽은 경우만 점수 처리
                val isBoss = enemy.isBoss()
                val isFlying = enemy.isFlying()
                
                // 적 타입에 따라 다른 점수 적용
                val isBossKilled = gameStats.enemyKilled(isBoss, isFlying)
                
                // 객체 풀에 적 반환 (재사용)
                enemyPool.recycle(enemy)
                
                if (isBossKilled && enemy.isDead()) {
                    // 현재 웨이브 저장 (다음 웨이브로 넘어가기 전)
                    val currentWave = gameStats.getWaveCount()
                    
                    // 보스 처치 이벤트 발생
                    bossKillListener?.onBossKilled(currentWave)
                    
                    // 보스 처치 시 다음 웨이브로 이동
                    // 마지막 웨이브(10)인 경우에도 다음 웨이브로 넘어가서 클리어 화면이 나타나도록 함
                    onNextWave()
                }
            } else {
                enemyPool.recycle(enemy)
            }
        }
    }
    
    /**
     * 화면 내 모든 적 제거 (보스 제외) - 플러시 스킬용
     * @return 제거된 적의 수
     */
    fun removeAllEnemiesExceptBoss(centerX: Float, centerY: Float, attackRange: Float): Int {
        var killCount = 0
        
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
            enemy.takeDamage(GameConfig.SPADE_FLUSH_DAMAGE)
            enemies.remove(enemy)
            
            // 적 유형 확인
            val isFlying = enemy.isFlying()
            
            // 처치 수에 포함 (공식 킬 카운트 증가)
            gameStats.enemyKilled(false, isFlying)
            
            // 적 객체를 풀에 반환
            enemyPool.recycle(enemy)
        }
        
        return killCount
    }
    
    /**
     * 화면 내 모든 적 제거 (보스 포함) - 테스트용
     * @return 제거된 적의 수
     */
    fun removeAllEnemies(): Int {
        val count = enemies.size
        
        // 모든 적 제거
        for (enemy in enemies.toList()) {
            // 적 객체를 풀에 반환
            enemies.remove(enemy)
            enemyPool.recycle(enemy)
        }
        
        // 적 목록 비우기
        enemies.clear()
        
        return count
    }
    
    /**
     * 게임 오버 알림
     */
    private fun notifyGameOver() {
        Handler(Looper.getMainLooper()).post {
            gameOverListener?.onGameOver(gameStats.getResource(), gameStats.getWaveCount())
        }
    }
    
    /**
     * 게임 재시작 시 리셋
     */
    fun reset(startTime: Long) {
        // 게임 요소 초기화 (및 객체 풀 반환)
        enemies.forEach { enemyPool.recycle(it) }
        enemies.clear()
        
        // 타이머 초기화
        lastEnemySpawnTime = startTime
        lastBossCheckTime = 0L
    }
    
    /**
     * 현재 적 목록 반환
     */
    fun getEnemies(): CopyOnWriteArrayList<Enemy> = enemies
    
    /**
     * 현재 보스 체력 반환
     */
    fun getCurrentBossHealth(): Int {
        // enemies 목록에서 보스 찾기
        val boss = enemies.find { it.isBoss() && !it.isDead() }
        return boss?.getHealth() ?: 0
    }
} 