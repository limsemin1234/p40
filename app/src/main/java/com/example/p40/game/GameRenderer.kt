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
        
        // 디펜스 유닛 공격 범위 표시 - 더 명확한 색상과 두께로 항상 표시하도록 변경
        val rangePaint = Paint().apply {
            color = Color.argb(100, 100, 180, 255) // 조금 더 진한 반투명 파란색
            style = Paint.Style.STROKE
            strokeWidth = 3f // 두께 증가
        }
        canvas.drawCircle(screenWidth / 2, screenHeight / 2, defenseUnit.attackRange, rangePaint)
        
        // 중앙에 방어 타워 그리기 - 카드 형태로 변경
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        // 새로운 코드: 카드 형태로 유닛 그리기
        drawDefenseUnitAsCard(canvas, centerX, centerY, gameStats.getUnitHealth(), gameStats.getUnitMaxHealth(), defenseUnit.getSymbolType())
        
        // 적 그리기 - 화면 내 적만 그리기
        for (enemy in enemies) {
            val pos = enemy.getPosition()
            if (pos.x >= -gameConfig.ENEMY_RENDER_MARGIN_X && pos.x <= screenWidth + gameConfig.ENEMY_RENDER_MARGIN_X && 
                pos.y >= -gameConfig.ENEMY_RENDER_MARGIN_Y && pos.y <= screenHeight + gameConfig.ENEMY_RENDER_MARGIN_Y) {
                enemy.draw(canvas)
            }
        }
        
        // 미사일 그리기 - 화면 내 미사일만 그리기
        for (missile in missiles) {
            val pos = missile.getPosition()
            if (pos.x >= -gameConfig.MISSILE_RENDER_MARGIN_X && pos.x <= screenWidth + gameConfig.MISSILE_RENDER_MARGIN_X && 
                pos.y >= -gameConfig.MISSILE_RENDER_MARGIN_Y && pos.y <= screenHeight + gameConfig.MISSILE_RENDER_MARGIN_Y) {
                missile.draw(canvas)
            }
        }
        
        // 일시정지 상태 표시
        if (isPaused) {
            canvas.drawText("일시 정지", centerX, centerY - 100, pauseTextPaint)
        }
        
        // 게임 오버 상태 표시
        if (isGameOver) {
            drawGameOver(canvas, screenWidth, screenHeight)
        }
    }
    
    /**
     * 디펜스 유닛을 카드 형태로 그리기
     */
    private fun drawDefenseUnitAsCard(canvas: Canvas, centerX: Float, centerY: Float, unitHealth: Int, unitMaxHealth: Int, symbolType: CardSymbolType) {
        // 카드 크기 설정
        val cardWidth = gameConfig.DEFENSE_UNIT_SIZE * 2f
        val cardHeight = gameConfig.DEFENSE_UNIT_SIZE * 2.5f
        
        // 카드 배경 그리기 (투명하게 변경)
        val cardBgPaint = Paint().apply {
            color = Color.TRANSPARENT
            style = Paint.Style.FILL
        }
        val cardRect = android.graphics.RectF(
            centerX - cardWidth / 2,
            centerY - cardHeight / 2,
            centerX + cardWidth / 2,
            centerY + cardHeight / 2
        )
        canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
        
        // 카드 테두리 그리기
        val cardBorderPaint = Paint().apply {
            color = Color.rgb(255, 215, 0) // 골드 색상
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, cardBorderPaint)
        
        // 카드 내부 빛나는 효과
        val glowPaint = Paint().apply {
            color = Color.argb(50, 255, 215, 0)
            style = Paint.Style.FILL
        }
        val glowRadius = cardWidth * 0.4f
        val radialGradient = android.graphics.RadialGradient(
            centerX,
            centerY,
            glowRadius,
            intArrayOf(Color.argb(100, 255, 215, 0), Color.argb(0, 255, 215, 0)),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        glowPaint.shader = radialGradient
        canvas.drawCircle(centerX, centerY, glowRadius, glowPaint)
        
        // 선택한 문양 타입에 따라 심볼 그리기
        when (symbolType) {
            CardSymbolType.SPADE -> drawSpadeSymbol(canvas, centerX, centerY, cardWidth)
            CardSymbolType.HEART -> drawHeartSymbol(canvas, centerX, centerY, cardWidth)
            CardSymbolType.DIAMOND -> drawDiamondSymbol(canvas, centerX, centerY, cardWidth)
            CardSymbolType.CLUB -> drawClubSymbol(canvas, centerX, centerY, cardWidth)
        }
        
        // 체력바 그리기
        val healthBarWidth = cardWidth * 1.2f
        val healthBarHeight = 8f
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
        val healthBarTop = centerY + cardHeight / 2 + 10f
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
     * 스페이드 심볼 그리기
     */
    private fun drawSpadeSymbol(canvas: Canvas, centerX: Float, centerY: Float, cardWidth: Float) {
        // 먼저 테두리를 그리기 위한 흰색 심볼
        val spadeSymbolBorderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = cardWidth * 0.55f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♠", centerX, centerY + cardWidth * 0.15f, spadeSymbolBorderPaint)
        
        // 그 위에 검은색 심볼 그리기
        val spadeSymbolPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = cardWidth * 0.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♠", centerX, centerY + cardWidth * 0.15f, spadeSymbolPaint)
    }
    
    /**
     * 하트 심볼 그리기
     */
    private fun drawHeartSymbol(canvas: Canvas, centerX: Float, centerY: Float, cardWidth: Float) {
        // 먼저 테두리를 그리기 위한 흰색 심볼
        val heartSymbolBorderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = cardWidth * 0.55f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♥", centerX, centerY + cardWidth * 0.15f, heartSymbolBorderPaint)
        
        // 그 위에 빨간색 심볼 그리기
        val heartSymbolPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            textSize = cardWidth * 0.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♥", centerX, centerY + cardWidth * 0.15f, heartSymbolPaint)
    }
    
    /**
     * 다이아몬드 심볼 그리기
     */
    private fun drawDiamondSymbol(canvas: Canvas, centerX: Float, centerY: Float, cardWidth: Float) {
        // 먼저 테두리를 그리기 위한 흰색 심볼
        val diamondSymbolBorderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = cardWidth * 0.55f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♦", centerX, centerY + cardWidth * 0.15f, diamondSymbolBorderPaint)
        
        // 그 위에 빨간색 심볼 그리기
        val diamondSymbolPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            textSize = cardWidth * 0.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♦", centerX, centerY + cardWidth * 0.15f, diamondSymbolPaint)
    }
    
    /**
     * 클로버 심볼 그리기
     */
    private fun drawClubSymbol(canvas: Canvas, centerX: Float, centerY: Float, cardWidth: Float) {
        // 먼저 테두리를 그리기 위한 흰색 심볼
        val clubSymbolBorderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = cardWidth * 0.55f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♣", centerX, centerY + cardWidth * 0.15f, clubSymbolBorderPaint)
        
        // 그 위에 검은색 심볼 그리기
        val clubSymbolPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = cardWidth * 0.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♣", centerX, centerY + cardWidth * 0.15f, clubSymbolPaint)
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
     * 모든 리소스 정리
     */
    fun clearResources() {
        // 비트맵 리소스가 있다면 정리
        // 메모리에 로드된 이미지 등 해제
    }
    
    /**
     * 디펜스 유닛 그리기 (호환성 유지를 위한 메서드)
     */
    fun drawDefenseUnit(canvas: Canvas, width: Float, height: Float, unitHealth: Int, unitMaxHealth: Int, symbolType: CardSymbolType = CardSymbolType.SPADE) {
        val centerX = width / 2f
        val centerY = height / 2f
        
        // 새로운 카드 형태 디펜스 유닛 그리기
        drawDefenseUnitAsCard(canvas, centerX, centerY, unitHealth, unitMaxHealth, symbolType)
    }
} 