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

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var isRunning = false
    private var isPaused = false
    
    // 게임 요소
    private lateinit var defenseUnit: DefenseUnit
    private val enemies = CopyOnWriteArrayList<Enemy>()
    private val missiles = CopyOnWriteArrayList<Missile>()
    
    // 게임 상태
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var score = 0
    private var waveCount = 1
    private var killCount = 0
    private var spawnedCount = 0  // 생성된 적의 수를 추적
    private var totalEnemiesInWave = 50
    private var bossSpawned = false
    private var showWaveMessage = false
    private var waveMessageStartTime = 0L
    private var waveMessageDuration = 2000L // 2초 동안 웨이브 메시지 표시
    
    // 디펜스 유닛 설정
    private val DEFENSE_UNIT_SIZE = 40f  // 디펜스 유닛의 크기
    
    // 웨이브별 설정
    private val waveEnemySpawnCooldowns = mapOf(
        1 to 3000L,
        2 to 2500L,
        3 to 2000L,
        4 to 1800L,
        5 to 1600L,
        6 to 1400L,
        7 to 1200L,
        8 to 1000L,
        9 to 800L,
        10 to 600L
    )
    private val waveEnemySpeeds = mapOf(
        1 to 1.0f,
        2 to 1.2f,
        3 to 1.4f,
        4 to 1.6f,
        5 to 1.8f,
        6 to 2.0f,
        7 to 2.2f,
        8 to 2.4f,
        9 to 2.6f,
        10 to 2.8f
    )
    private val waveEnemyHealths = mapOf(
        1 to 100,
        2 to 120,
        3 to 140,
        4 to 160,
        5 to 180,
        6 to 200,
        7 to 220,
        8 to 240,
        9 to 260,
        10 to 280
    )
    
    private var enemySpawnCooldown = waveEnemySpawnCooldowns[1] ?: 3000L
    private var lastEnemySpawnTime = 0L
    private var gameStartTime = 0L
    
    // 그리기 도구
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.LEFT
    }
    
    private val waveMessagePaint = Paint().apply {
        color = Color.YELLOW
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }
    
    private val unitPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    
    // 일시정지 관련
    private val pauseTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        textAlign = Paint.Align.CENTER
    }
    
    init {
        holder.addCallback(this)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        
        // 중앙에 방어 유닛 배치
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        defenseUnit = DefenseUnit(PointF(centerX, centerY))
        
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
        if (isPaused) return
        
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
            enemy.update()
            
            // 중앙에 도달했는지 확인
            val enemyPos = enemy.getPosition()
            val dx = enemyPos.x - centerX
            val dy = enemyPos.y - centerY
            val distanceToCenter = kotlin.math.sqrt(dx * dx + dy * dy)
            
            if (distanceToCenter < DEFENSE_UNIT_SIZE) {  // 디펜스 유닛 크기 적용
                enemy.takeDamage(1000) // 중앙에 도달하면 죽음
            }
        }
        
        // 미사일 업데이트
        missiles.forEach { it.update() }
        
        // 방어 유닛이 적을 공격
        val newMissile = defenseUnit.attack(enemies, currentTime)
        if (newMissile != null) {
            missiles.add(newMissile)
        }
        
        // 죽은 적과 미사일 제거
        val deadEnemies = enemies.filter { it.isDead() }
        enemies.removeAll(deadEnemies)
        
        // 킬 카운트 및 점수 갱신
        deadEnemies.forEach { enemy ->
            score += if (enemy.isBoss) 100 else 10
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
        val health = waveEnemyHealths[waveCount] ?: 100
        
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
        val health = (waveEnemyHealths[waveCount] ?: 100) * 5
        
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
        
        // 점수 및 웨이브 정보 표시
        canvas.drawText("점수: $score  웨이브: $waveCount  처치: $killCount/$totalEnemiesInWave  생성: $spawnedCount/$totalEnemiesInWave", 20f, 70f, textPaint)
        
        // 웨이브 시작 메시지 표시
        if (showWaveMessage) {
            canvas.drawText("$waveCount WAVE", centerX, centerY - 100, waveMessagePaint)
        }
        
        // 일시정지 상태 표시
        if (isPaused) {
            canvas.drawText("일시 정지", centerX, centerY - 100, pauseTextPaint)
        }
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