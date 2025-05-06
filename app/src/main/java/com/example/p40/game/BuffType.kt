package com.example.p40.game

/**
 * 버프 타입 정의
 */
enum class BuffType {
    MISSILE_DAMAGE,     // 미사일 데미지
    ATTACK_SPEED,      // 공격 속도
    MISSILE_SPEED,     // 미사일 속도
    MULTI_DIRECTION,   // 다방향 발사
    MISSILE_PIERCE,    // 미사일 관통
    ENEMY_SLOW,        // 적 이동속도 감소
    DOT_DAMAGE,        // 도트 데미지
    MASS_DAMAGE,       // 광역 데미지
    RESOURCE_GAIN,     // 자원 획득량
    
    // 플러시 스킬 관련
    HEART_FLUSH_SKILL,    // 하트 플러시 스킬 (체력 회복)
    SPADE_FLUSH_SKILL,    // 스페이드 플러시 스킬 (적 제거)
    CLUB_FLUSH_SKILL,     // 클로버 플러시 스킬 (적 이동속도 감소)
    DIAMOND_FLUSH_SKILL   // 다이아몬드 플러시 스킬 (자원 획득)
} 