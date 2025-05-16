package com.example.p40

import android.graphics.PointF
import android.graphics.RectF

/**
 * 화면 영역 체크를 위한 헬퍼 클래스
 * 성능 최적화: 공간 분할 그리드 기반 충돌 검출 구현
 */
class ScreenRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    // 화면 영역 Rect 객체
    private val rectF = RectF(left, top, right, bottom)
    
    // 그리드 분할 설정
    companion object {
        const val GRID_SIZE = 4 // 화면을 4x4 그리드로 분할
    }
    
    // 그리드 셀 크기
    private val cellWidth = (right - left) / GRID_SIZE
    private val cellHeight = (bottom - top) / GRID_SIZE
    
    // 그리드 셀마다 객체 목록 저장
    private val gridCells = Array(GRID_SIZE) { Array(GRID_SIZE) { mutableListOf<GridObject>() } }
    
    /**
     * 특정 좌표가 화면 영역 내에 있는지 확인
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
    
    /**
     * 특정 좌표가 화면 영역 내에 있는지 확인 (Rect 객체 활용 - 경계 케이스 일관성)
     */
    fun containsPoint(x: Float, y: Float): Boolean {
        return rectF.contains(x, y)
    }
    
    /**
     * 두 Rect가 겹치는지 확인
     */
    fun intersects(other: ScreenRect): Boolean {
        return RectF(left, top, right, bottom).intersect(RectF(other.left, other.top, other.right, other.bottom))
    }
    
    /**
     * 객체 그리드 초기화
     */
    fun clearGrid() {
        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                gridCells[i][j].clear()
            }
        }
    }
    
    /**
     * 좌표에 해당하는 그리드 셀 인덱스 계산
     */
    private fun getGridCell(x: Float, y: Float): Pair<Int, Int>? {
        if (!contains(x, y)) return null
        
        val gridX = ((x - left) / cellWidth).toInt().coerceIn(0, GRID_SIZE - 1)
        val gridY = ((y - top) / cellHeight).toInt().coerceIn(0, GRID_SIZE - 1)
        
        return Pair(gridX, gridY)
    }
    
    /**
     * 객체를 그리드에 추가
     */
    fun addObjectToGrid(obj: Any, position: PointF, radius: Float) {
        val cell = getGridCell(position.x, position.y) ?: return
        
        val gridObj = GridObject(obj, position, radius)
        gridCells[cell.first][cell.second].add(gridObj)
    }
    
    /**
     * 지정된 위치와 충돌할 수 있는 모든 객체 찾기
     */
    fun findPotentialCollisions(position: PointF, radius: Float): List<Any> {
        val cell = getGridCell(position.x, position.y) ?: return emptyList()
        
        val x = cell.first
        val y = cell.second
        
        // 현재 셀과 주변 셀 탐색 (경계를 넘지 않도록)
        val result = mutableListOf<Any>()
        
        // 9개의 인접 셀 확인 (자신 포함)
        for (i in (x - 1).coerceAtLeast(0)..(x + 1).coerceAtMost(GRID_SIZE - 1)) {
            for (j in (y - 1).coerceAtLeast(0)..(y + 1).coerceAtMost(GRID_SIZE - 1)) {
                // 각 셀의 객체들과 충돌 검사
                for (gridObj in gridCells[i][j]) {
                    // 거리 제곱 계산 (제곱근 연산 회피)
                    val dx = position.x - gridObj.position.x
                    val dy = position.y - gridObj.position.y
                    val distanceSquared = dx * dx + dy * dy
                    
                    // 충돌 반경 합의 제곱
                    val combinedRadius = radius + gridObj.radius
                    val combinedRadiusSquared = combinedRadius * combinedRadius
                    
                    if (distanceSquared <= combinedRadiusSquared) {
                        result.add(gridObj.obj)
                    }
                }
            }
        }
        
        return result
    }
}

/**
 * 그리드에 저장할 객체 정보
 */
data class GridObject(
    val obj: Any,           // 실제 객체 (Enemy, Missile 등)
    val position: PointF,    // 객체 위치
    val radius: Float        // 객체 충돌 반경
) 