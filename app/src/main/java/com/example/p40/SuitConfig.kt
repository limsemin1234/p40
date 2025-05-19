package com.example.p40

/**
 * 디펜스 유닛의 문양 효과 관련 설정을 관리하는 객체
 * 문양 효과 밸런스 조정이 필요할 때 이 파일만 수정하면 됨
 */
object SuitConfig {
    // --------- 디펜스 유닛 문양 효과 설정 ----------
    
    // 스페이드 문양 효과 - 기본 상태(영향 없음)
    const val SPADE_DAMAGE_MULTIPLIER = 1.0f  // 공격력 배율
    const val SPADE_SPEED_MULTIPLIER = 1.0f   // 공격속도 배율
    const val SPADE_RANGE_MULTIPLIER = 1.0f   // 공격범위 배율
    
    // 하트 문양 효과 - 공격력 감소, 데미지 시 체력 회복
    const val HEART_DAMAGE_MULTIPLIER = 0.5f  // 공격력 50% 감소
    const val HEART_SPEED_MULTIPLIER = 1.0f   // 공격속도 영향 없음
    const val HEART_RANGE_MULTIPLIER = 1.0f   // 공격범위 영향 없음
    const val HEART_HEAL_ON_DAMAGE = 1        // 데미지 시 회복량
    
    // 다이아몬드 문양 효과 - 공격속도 증가, 공격범위 감소
    const val DIAMOND_DAMAGE_MULTIPLIER = 1.0f  // 공격력 영향 없음
    const val DIAMOND_SPEED_MULTIPLIER = 2.0f   // 공격속도 2배 증가
    const val DIAMOND_RANGE_MULTIPLIER = 0.5f   // 공격범위 50% 감소
    
    // 클로버 문양 효과 - 공격범위 증가, 공격속도 감소
    const val CLUB_DAMAGE_MULTIPLIER = 1.0f   // 공격력 영향 없음
    const val CLUB_SPEED_MULTIPLIER = 0.5f    // 공격속도 50% 감소
    const val CLUB_RANGE_MULTIPLIER = 1.5f    // 공격범위 50% 증가
    
    // --------- 플러시 스킬 관련 설정 ----------
    
    // 하트 플러시 스킬: 체력 회복 관련 설정
    const val HEART_FLUSH_HEAL_AMOUNT = 500   // -1은 전체 회복, 양수는 해당 수치만큼 회복
    
    // 스페이드 플러시 스킬: 적 제거 관련 설정
    const val SPADE_FLUSH_DAMAGE = 500     // 적에게 입히는 즉사 데미지 (매우 높은 값으로 설정)
    
    // 클로버 플러시 스킬: 시간 정지 관련 설정
    const val CLUB_FLUSH_DURATION = 5000L    // 시간 정지 지속 시간 (밀리초)
    
    // 다이아몬드 플러시 스킬: 무적 관련 설정
    const val DIAMOND_FLUSH_DURATION = 5000L // 무적 지속 시간 (밀리초)
} 