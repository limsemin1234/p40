package com.example.p40.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 게임 렌더링 담당 클래스
 * GameView에서 그리기(rendering) 관련 로직만 분리함
 */
class GameRenderer(
    private val gameStats: GameStats,
    private val gameConfig: GameConfig
) {
    // 그리기 도구
    private val textPaint = Paint().apply {
        color = gameConfig.TEXT_COLOR
        textSize = gameConfig.TEXT_SIZE_NORMAL
        textAlign = Paint.Align.LEFT
    }
    
    private val waveMessagePaint = Paint().apply {
        color = gameConfig.WAVE_TEXT_COLOR
        textSize = gameConfig.TEXT_SIZE_WAVE
        textAlign = Paint.Align.CENTER
    }
    
    private val unitPaint = Paint().apply {
        color = gameConfig.DEFENSE_UNIT_COLOR
        style = Paint.Style.FILL
    }
    
    // 일시정지 관련
    private val pauseTextPaint = Paint().apply {
        color = gameConfig.TEXT_COLOR
        textSize = gameConfig.TEXT_SIZE_PAUSE
        textAlign = Paint.Align.CENTER
    }
    
    // 렌더링 최적화를 위한 FPS 추적
    private var currentFPS: Long = 0
    
    /**
     * 게임 화면 렌더링
     */
    fun renderGame(
        canvas: Canvas, 
        defenseUnit: DefenseUnit,
        enemies: CopyOnWriteArrayList<Enemy>, 
        missiles: CopyOnWriteArrayList<Missile>,
        screenWidth: Float,
        screenHeight: Float,
        isPaused: Boolean,
        isGameOver: Boolean,
        showWaveMessage: Boolean
    ) {
        // 배경 지우기
        canvas.drawColor(Color.BLACK)
        
        // 중앙에 방어 타워 그리기
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        canvas.drawCircle(centerX, centerY, gameConfig.DEFENSE_UNIT_SIZE, unitPaint)
        
        // 적 그리기 - 화면 내 적만 그리기
        for (enemy in enemies) {
            val pos = enemy.getPosition()
            if (pos.x >= -50 && pos.x <= screenWidth + 50 && 
                pos.y >= -50 && pos.y <= screenHeight + 50) {
                enemy.draw(canvas)
            }
        }
        
        // 미사일 그리기 - 화면 내 미사일만 그리기
        for (missile in missiles) {
            val pos = missile.getPosition()
            if (pos.x >= -20 && pos.x <= screenWidth + 20 && 
                pos.y >= -20 && pos.y <= screenHeight + 20) {
                missile.draw(canvas)
            }
        }
        
        // 웨이브 시작 메시지 표시
        if (showWaveMessage) {
            canvas.drawText("${gameStats.getWaveCount()} WAVE", centerX, centerY - 100, waveMessagePaint)
        }
        
        // 일시정지 상태 표시
        if (isPaused) {
            canvas.drawText("일시 정지", centerX, centerY - 100, pauseTextPaint)
        }
        
        // 게임 오버 상태 표시
        if (isGameOver) {
            drawGameOver(canvas, screenWidth, screenHeight)
        }
        
        // 디버그 정보 표시
        if (gameConfig.DEBUG_MODE) {
            drawDebugInfo(canvas, enemies.size, missiles.size)
        }
    }
    
    /**
     * 디펜스 유닛 그리기
     */
    fun drawDefenseUnit(canvas: Canvas, width: Float, height: Float, unitHealth: Int, unitMaxHealth: Int) {
        val centerX = width / 2f
        val centerY = height / 2f
        
        // 디펜스 유닛 그리기
        val unitPaint = Paint().apply {
            color = gameConfig.DEFENSE_UNIT_COLOR
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, gameConfig.DEFENSE_UNIT_SIZE, unitPaint)
        
        // 디펜스 유닛 체력바 그리기
        val healthBarWidth = gameConfig.DEFENSE_UNIT_SIZE * 3
        val healthBarHeight = 10f
        val healthRatio = unitHealth.toFloat() / unitMaxHealth
        
        // 체력바 배경
        val bgPaint = Paint().apply { 
            color = Color.DKGRAY 
            style = Paint.Style.FILL
        }
        
        // 체력바
        val healthPaint = Paint().apply {
            color = if (healthRatio > 0.5f) Color.GREEN else if (healthRatio > 0.25f) Color.YELLOW else Color.RED
            style = Paint.Style.FILL
        }
        
        // 체력바 위치
        val healthBarTop = centerY + gameConfig.DEFENSE_UNIT_SIZE + 10f
        val healthBarLeft = centerX - healthBarWidth / 2
        
        // 체력바 배경 그리기
        canvas.drawRect(
            healthBarLeft, 
            healthBarTop, 
            healthBarLeft + healthBarWidth, 
            healthBarTop + healthBarHeight, 
            bgPaint
        )
        
        // 체력바 그리기
        canvas.drawRect(
            healthBarLeft, 
            healthBarTop, 
            healthBarLeft + healthBarWidth * healthRatio, 
            healthBarTop + healthBarHeight, 
            healthPaint
        )
    }
    
    /**
     * 게임 오버 화면 그리기
     */
    private fun drawGameOver(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.RED
            textSize = 100f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        canvas.drawText("GAME OVER", centerX, centerY, paint)
    }
    
    /**
     * 디버그 정보 표시
     */
    private fun drawDebugInfo(canvas: Canvas, enemyCount: Int, missileCount: Int) {
        val debugPaint = Paint().apply {
            color = Color.GREEN
            textSize = 30f
            textAlign = Paint.Align.LEFT
        }
        
        // 디버그 정보 표시
        canvas.drawText("FPS: $currentFPS", 10f, 30f, debugPaint)
        canvas.drawText("적: $enemyCount/${gameConfig.MAX_ENEMIES}", 10f, 60f, debugPaint)
        canvas.drawText("미사일: $missileCount/${gameConfig.MAX_MISSILES}", 10f, 90f, debugPaint)
        canvas.drawText("Wave: ${gameStats.getWaveCount()}", 10f, 120f, debugPaint)
    }
    
    /**
     * FPS 업데이트
     */
    fun updateFPS(fps: Long) {
        currentFPS = fps
    }
} 