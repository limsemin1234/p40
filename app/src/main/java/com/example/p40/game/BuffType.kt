package com.example.p40.game

/**
 * 버프 종류 정의
 */
enum class BuffType {
    MISSILE_DAMAGE,   // 미사일 데미지 증가
    ATTACK_SPEED,     // 공격 속도 증가
    MISSILE_SPEED,    // 미사일 속도 증가
    MULTI_DIRECTION,  // 다방향 발사
    MISSILE_PIERCE,   // 미사일 관통
    ENEMY_SLOW,       // 적 이동속도 감소
    DOT_DAMAGE,       // 지속 데미지
    MASS_DAMAGE,      // 주기적 대량 데미지
    RESOURCE_GAIN,    // 자원 획득량 증가
    
    // 플러시 특수 스킬 타입
    HEART_FLUSH_SKILL,    // 하트 플러시 - 체력 전체 회복 스킬
    SPADE_FLUSH_SKILL,    // 스페이드 플러시 - 화면 내 모든 적 제거 스킬 (보스 제외)
    CLUB_FLUSH_SKILL,     // 클로버 플러시 - 시간 멈춤 스킬 (5초)
    DIAMOND_FLUSH_SKILL   // 다이아 플러시 - 무적 스킬 (5초)
} 