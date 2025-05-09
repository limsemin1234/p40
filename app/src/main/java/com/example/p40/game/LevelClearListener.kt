package com.example.p40.game

/**
 * 게임 레벨 클리어 이벤트를 수신하는 리스너 인터페이스
 */
interface LevelClearListener {
    /**
     * 게임 레벨이 클리어되었을 때 호출됩니다.
     * @param wave 클리어한 웨이브 번호
     * @param score 최종 점수(자원)
     */
    fun onLevelCleared(wave: Int, score: Int)
} 