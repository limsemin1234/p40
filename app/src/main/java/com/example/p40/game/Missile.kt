package com.example.p40.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Missile(
    private var position: PointF,
    private val target: Enemy,
    private val speed: Float = 8f,
    private val damage: Int = 50,
    private val maxDistance: Float = 2000f // 최대 이동 거리
) {
    private val paint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    
    private var isDead = false
    private val size = 15f
    
    private var directionX: Float
    private var directionY: Float
    private var distanceTraveled = 0f
    
    init {
        // 목표 위치로 향하는 방향 계산
        val targetPos = target.getPosition()
        val dx = targetPos.x - position.x
        val dy = targetPos.y - position.y
        val angle = atan2(dy, dx)
        
        directionX = cos(angle)
        directionY = sin(angle)
        
        // 방향 벡터 정규화
        val length = sqrt(directionX * directionX + directionY * directionY)
        directionX /= length
        directionY /= length
    }
    
    fun update() {
        // 이미 죽은 미사일은 업데이트하지 않음
        if (isDead) return
        
        // 이전 위치 저장
        val oldX = position.x
        val oldY = position.y
        
        // 목표 방향으로 이동
        position.x += directionX * speed
        position.y += directionY * speed
        
        // 이동 거리 계산
        val deltaX = position.x - oldX
        val deltaY = position.y - oldY
        distanceTraveled += sqrt(deltaX * deltaX + deltaY * deltaY)
        
        // 최대 거리 초과 시 제거
        if (distanceTraveled > maxDistance) {
            isDead = true
            return
        }
        
        // 타겟이 아직 유효하고 살아있는 경우만 충돌 체크
        if (!target.isDead()) {
            val targetPos = target.getPosition()
            val dx = position.x - targetPos.x
            val dy = position.y - targetPos.y
            val distance = sqrt(dx * dx + dy * dy)
            
            // 충돌 했을 경우 데미지를 입히고 미사일 제거
            if (distance < (size + target.getSize())) {
                target.takeDamage(damage)
                isDead = true
            }
        }
        // 타겟이 죽은 경우는 아무것도 하지 않고 계속 날아감
    }
    
    fun draw(canvas: Canvas) {
        canvas.drawCircle(position.x, position.y, size, paint)
    }
    
    fun isDead(): Boolean = isDead
} 