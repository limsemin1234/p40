package com.example.p40.game

/**
 * 보스 처치 이벤트를 수신하는 리스너 인터페이스
 */
interface BossKillListener {
    /**
     * 보스가 처치되었을 때 호출됩니다.
     */
    fun onBossKilled()
} 