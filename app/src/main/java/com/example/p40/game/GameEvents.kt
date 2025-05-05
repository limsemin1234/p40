package com.example.p40.game

/**
 * 게임 이벤트 타입 정의
 */
enum class GameEventType {
    ENEMY_SPAWNED,       // 적 생성
    ENEMY_KILLED,        // 적 처치
    ENEMY_REACHED_CENTER, // 적이 중앙에 도달
    WAVE_COMPLETE,       // 웨이브 완료
    BOSS_SPAWN,          // 보스 출현
    BOSS_KILLED,         // 보스 처치
    GAME_OVER,           // 게임 오버
    RESOURCE_CHANGED,    // 자원 변경
    BUFF_APPLIED,        // 버프 적용
    UNIT_UPGRADED,       // 유닛 업그레이드
    CARD_EFFECT_APPLIED, // 카드 효과 적용
}

/**
 * 게임 이벤트 데이터 클래스
 */
data class GameEvent(
    val type: GameEventType,
    val data: Map<String, Any> = emptyMap()
)

/**
 * 게임 이벤트 리스너 인터페이스
 */
interface GameEventListener {
    fun onGameEvent(event: GameEvent)
}

/**
 * 게임 이벤트 관리자 클래스 - 옵저버 패턴 구현
 */
class GameEventManager {
    companion object {
        private val instance = GameEventManager()
        fun getInstance(): GameEventManager = instance
    }
    
    private val listeners = mutableMapOf<GameEventType, MutableList<GameEventListener>>()
    
    /**
     * 이벤트 리스너 등록
     */
    fun addListener(eventType: GameEventType, listener: GameEventListener) {
        val eventListeners = listeners.getOrPut(eventType) { mutableListOf() }
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    /**
     * 이벤트 리스너 등록 해제
     */
    fun removeListener(eventType: GameEventType, listener: GameEventListener) {
        listeners[eventType]?.remove(listener)
    }
    
    /**
     * 이벤트 발생 알림
     */
    fun dispatchEvent(event: GameEvent) {
        listeners[event.type]?.forEach { listener ->
            listener.onGameEvent(event)
        }
    }
    
    /**
     * 특정 타입의 이벤트 발생 알림 (간편 메서드)
     */
    fun dispatchEvent(type: GameEventType, data: Map<String, Any> = emptyMap()) {
        dispatchEvent(GameEvent(type, data))
    }
    
    /**
     * 모든 리스너 제거
     */
    fun clearAllListeners() {
        listeners.clear()
    }
} 