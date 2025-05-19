package com.example.p40

import android.graphics.Color

/**
 * 게임의 모든 설정 값을 관리하는 객체
 * 게임 밸런스 조정이 필요할 때 이 파일만 수정하면 됨
 */
object GameConfig {
    
    // --------- 게임 기본 설정 ----------
    
    // 게임 진행 설정
    const val TOTAL_WAVES = 10  // 전체 웨이브 수
    const val ENEMIES_PER_WAVE = 50  // 웨이브 당 적 수
    const val WAVE_MESSAGE_DURATION = 2000L  // 웨이브 메시지 표시 시간 (밀리초)
    
    // 적 생성 관련 설정
    const val ENEMY_SPAWN_DISTANCE_FACTOR = EnemyConfig.ENEMY_SPAWN_DISTANCE_FACTOR  // 적 생성 거리 계수 (화면 크기 대비)
    const val ENEMY_UPDATE_MARGIN = EnemyConfig.ENEMY_UPDATE_MARGIN  // 적 업데이트 마진 (적 생성 거리 외 여유 공간)
    
    // 유저 관련 설정
    const val INITIAL_COIN = 5000  // 게임 시작 시 주어지는 초기 코인 량
    
    // 게임 레벨 설정
    private var currentTotalWaves: Int = 10  // 기본 총 웨이브 수
    

    // 유닛 기본 능력치 설정
    const val BASE_DAMAGE = 10           // 기본 공격력
    const val BASE_ATTACK_SPEED = 1000   // 기본 공격 속도 (ms)
    const val BASE_ATTACK_RANGE = 300f   // 기본 공격 범위
    const val BASE_HEALTH = 100          // 기본 체력
    const val MIN_ATTACK_SPEED = 100     // 최소 공격 속도 (ms)
    
    // 적 유닛 등장 설정
    const val FLYING_ENEMY_WAVE_THRESHOLD = EnemyConfig.FLYING_ENEMY_WAVE_THRESHOLD // 날아다니는 적이 등장하기 시작하는 웨이브
    
    // --------- 디버그/테스트 설정 ----------
    
    // 디버그 모드 설정
    const val DEBUG_MODE = false // 디버그 정보 표시 여부
    
    // 테스트 모드 설정
    const val TEST_ENABLE_ALL_SKILLS = true // 웨이브 시작 시 모든 스킬 활성화 여부
    
    // --------- 성능 및 제한 설정 ----------
    
    // 성능 설정
    const val FRAME_LIMIT = 60 // 최대 FPS
    const val MAX_ENEMIES = 150 // 화면에 표시되는 최대 적 수 (증가)
    const val MAX_MISSILES = 300 // 화면에 표시되는 최대 미사일 수 (증가)
    const val BACKGROUND_COLOR = Color.BLACK // 배경색
    
    // 공간 분할 그리드 설정
    const val USE_SPATIAL_GRID = true // 공간 분할 그리드 사용 여부
    const val GRID_SIZE = 4 // 그리드 분할 크기 (4x4)
    
    // 객체 풀 설정
    const val MISSILE_POOL_INITIAL_SIZE = 200 // 미사일 객체 풀 초기 크기
    const val MISSILE_POOL_MAX_SIZE = 500 // 미사일 객체 풀 최대 크기
    
    // 렌더링 영역 설정
    const val MISSILE_RENDER_MARGIN_X = 20f // 미사일 렌더링 X축 마진
    const val MISSILE_RENDER_MARGIN_Y = 20f // 미사일 렌더링 Y축 마진
    const val ENEMY_RENDER_MARGIN_X = EnemyConfig.ENEMY_RENDER_MARGIN_X // 적 렌더링 X축 마진
    const val ENEMY_RENDER_MARGIN_Y = EnemyConfig.ENEMY_RENDER_MARGIN_Y // 적 렌더링 Y축 마진
    const val FAR_OFFSCREEN_MARGIN = EnemyConfig.FAR_OFFSCREEN_MARGIN // 게임 오브젝트가 제거되는 화면 외부 거리
    
    // --------- UI 및 메시지 설정 ----------
    
    // UI 텍스트 설정
    const val TEXT_SIZE_NORMAL = 48f  // 일반 텍스트 크기
    const val TEXT_SIZE_WAVE = 100f  // 웨이브 텍스트 크기
    const val TEXT_SIZE_PAUSE = 72f  // 일시정지 텍스트 크기
    const val TEXT_COLOR = Color.WHITE  // 텍스트 색상
    const val WAVE_TEXT_COLOR = Color.YELLOW  // 웨이브 텍스트 색상
    
    // 메시지 표시 설정
    const val MESSAGE_OPACITY = 0.3f // 메시지 불투명도 (0~1)
    const val MESSAGE_MIN_WIDTH = 200 // 메시지 최소 너비 (dp)
    const val MESSAGE_MAX_WIDTH = 350 // 메시지 최대 너비 (dp)
    const val MESSAGE_PADDING_HORIZONTAL = 16 // 메시지 가로 패딩 (dp)
    const val MESSAGE_PADDING_VERTICAL = 10 // 메시지 세로 패딩 (dp)
    const val MESSAGE_CORNER_RADIUS = 8 // 메시지 모서리 둥글기 (dp)
    const val MESSAGE_DURATION = 1500L // 메시지 표시 시간 (ms)
    const val MESSAGE_MAX_COUNT = 5 // 최대 메시지 수
    const val MESSAGE_TEXT_SIZE = 15f // 메시지 텍스트 크기 (sp)
    
    // --------- 업그레이드 관련 설정 ----------

    
    // 데미지 업그레이드 설정
    const val DAMAGE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val DAMAGE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val DAMAGE_UPGRADE_VALUE = 1  // 업그레이드당 데미지 증가량
    const val DAMAGE_UPGRADE_MAX_LEVEL = 500  // 최대 업그레이드 레벨
    
    // 공격속도 업그레이드 설정
    const val ATTACK_SPEED_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_SPEED_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val ATTACK_SPEED_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨
    
    // 공격속도 구간별 감소량 설정
    const val ATTACK_SPEED_DECREASE_TIER1 = 20L  // 1000ms~800ms 구간에서의 감소량 (ms)
    const val ATTACK_SPEED_DECREASE_TIER2 = 10L  // 800ms~500ms 구간에서의 감소량 (ms)
    const val ATTACK_SPEED_DECREASE_TIER3 = 5L   // 500ms~250ms 구간에서의 감소량 (ms)
    const val ATTACK_SPEED_TIER1_THRESHOLD = 800L // 첫 번째 구간 임계값
    const val ATTACK_SPEED_TIER2_THRESHOLD = 500L // 두 번째 구간 임계값
    const val ATTACK_SPEED_TIER3_THRESHOLD = 250L // 세 번째 구간 임계값
    
    // 공격범위 업그레이드 설정
    const val ATTACK_RANGE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_RANGE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val ATTACK_RANGE_UPGRADE_VALUE = 5f  // 업그레이드당 범위 증가량
    const val ATTACK_RANGE_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨
    
    // 체력 업그레이드 설정
    const val DEFENSE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val DEFENSE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val DEFENSE_UPGRADE_VALUE = 10  // 업그레이드당 최대 체력 증가량
    const val DEFENSE_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨

    
    // --------- 렌더링 관련 설정 ----------
    
    // 디펜스 유닛 설정
    const val DEFENSE_UNIT_SIZE = 40f  // 디펜스 유닛 크기
    const val DEFENSE_UNIT_COLOR = Color.BLUE  // 디펜스 유닛 색상
    const val DEFENSE_UNIT_ATTACK_RANGE = 300f  // 공격 범위
    const val DEFENSE_UNIT_ATTACK_COOLDOWN = 1000L  // 공격속도 1초
    const val DEFENSE_UNIT_INITIAL_HEALTH = 100 // 초기 체력
    const val DEFENSE_UNIT_INITIAL_MAX_HEALTH = 100 // 초기 최대 체력
    const val DEFENSE_UNIT_THORN_DAMAGE = 10 // 디펜스 유닛 가시 데미지 (충돌 시 적에게 입히는 데미지)
    
    // 미사일 설정
    const val MISSILE_SIZE = 5f  // 미사일 크기
    const val MISSILE_SPEED = 10f  // 미사일 속도
    const val MISSILE_DAMAGE = 50  // 미사일 기본 데미지
    const val MISSILE_COLOR = Color.YELLOW  // 미사일 색상
    
    // --------- 스탯 강화 관련 설정 ----------
    
    // 스탯 강화 최대 레벨 설정
    const val STATS_MAX_LEVEL = 50  // 모든 스탯의 최대 강화 레벨
    
    // 체력 강화 관련 설정
    const val STATS_HEALTH_UPGRADE_AMOUNT = 10    // 체력 증가량
    const val STATS_HEALTH_BASE_COST = 100        // 체력 강화 기본 비용
    const val STATS_HEALTH_COST_INCREASE = 100    // 체력 강화 비용 증가량
    
    // 공격력 강화 관련 설정
    const val STATS_ATTACK_UPGRADE_AMOUNT = 5     // 공격력 증가량
    const val STATS_ATTACK_BASE_COST = 100        // 공격력 강화 기본 비용
    const val STATS_ATTACK_COST_INCREASE = 100    // 공격력 강화 비용 증가량
    
    // 공격 속도 강화 관련 설정
    const val STATS_ATTACK_SPEED_UPGRADE_AMOUNT = 10  // 공격 속도 감소량 (ms)
    const val STATS_ATTACK_SPEED_BASE_COST = 100        // 공격 속도 강화 기본 비용
    const val STATS_ATTACK_SPEED_COST_INCREASE = 100    // 공격 속도 강화 비용 증가량
    
    // 사거리 강화 관련 설정
    const val STATS_RANGE_UPGRADE_AMOUNT = 5     // 사거리 증가량
    const val STATS_RANGE_BASE_COST = 100        // 사거리 강화 기본 비용
    const val STATS_RANGE_COST_INCREASE = 100    // 사거리 강화 비용 증가량

    // --------- 포커 족보별 스탯 증가 설정 ----------
    
    // 하이카드 (효과 없음)
    const val HIGH_CARD_DAMAGE_INCREASE = 0.0f        // 데미지 증가량 (%)
    const val HIGH_CARD_ATTACK_SPEED_INCREASE = 0.0f  // 공격속도 증가량 (%)
    const val HIGH_CARD_RANGE_INCREASE = 0.0f         // 사거리 증가량 (%)
    const val HIGH_CARD_HEALTH_INCREASE = 0.0f        // 체력 증가량 (%)

    // 원페어
    const val ONE_PAIR_DAMAGE_INCREASE = 0.1f         // 데미지 10% 증가
    const val ONE_PAIR_ATTACK_SPEED_INCREASE = 0.0f   // 공격속도 증가량 (%)
    const val ONE_PAIR_RANGE_INCREASE = 0.0f          // 사거리 증가량 (%)
    const val ONE_PAIR_HEALTH_INCREASE = 0.0f         // 체력 증가량 (%)

    // 투페어
    const val TWO_PAIR_DAMAGE_INCREASE = 0.2f         // 데미지 20% 증가
    const val TWO_PAIR_ATTACK_SPEED_INCREASE = 0.0f   // 공격속도 증가량 (%)
    const val TWO_PAIR_RANGE_INCREASE = 0.0f          // 사거리 증가량 (%)
    const val TWO_PAIR_HEALTH_INCREASE = 0.0f         // 체력 증가량 (%)

    // 트리플
    const val THREE_OF_A_KIND_DAMAGE_INCREASE = 0.3f        // 데미지 30% 증가
    const val THREE_OF_A_KIND_ATTACK_SPEED_INCREASE = 0.0f  // 공격속도 증가량 (%)
    const val THREE_OF_A_KIND_RANGE_INCREASE = 0.0f         // 사거리 증가량 (%)
    const val THREE_OF_A_KIND_HEALTH_INCREASE = 0.0f        // 체력 증가량 (%)

    // 스트레이트
    const val STRAIGHT_DAMAGE_INCREASE = 0.4f         // 데미지 40% 증가
    const val STRAIGHT_ATTACK_SPEED_INCREASE = 0.0f   // 공격속도 증가량 (%)
    const val STRAIGHT_RANGE_INCREASE = 0.0f          // 사거리 증가량 (%)
    const val STRAIGHT_HEALTH_INCREASE = 0.0f         // 체력 증가량 (%)

    // 플러시 (스킬 활성화만 있으므로 기본 데미지 증가 없음)
    const val FLUSH_DAMAGE_INCREASE = 0.0f            // 데미지 증가량 (%)
    const val FLUSH_ATTACK_SPEED_INCREASE = 0.0f      // 공격속도 증가량 (%)
    const val FLUSH_RANGE_INCREASE = 0.0f             // 사거리 증가량 (%)
    const val FLUSH_HEALTH_INCREASE = 0.0f            // 체력 증가량 (%)

    // 풀하우스
    const val FULL_HOUSE_DAMAGE_INCREASE = 0.6f       // 데미지 60% 증가
    const val FULL_HOUSE_ATTACK_SPEED_INCREASE = 0.0f // 공격속도 증가량 (%)
    const val FULL_HOUSE_RANGE_INCREASE = 0.0f        // 사거리 증가량 (%)
    const val FULL_HOUSE_HEALTH_INCREASE = 0.0f       // 체력 증가량 (%)

    // 포카드
    const val FOUR_OF_A_KIND_DAMAGE_INCREASE = 0.7f        // 데미지 70% 증가
    const val FOUR_OF_A_KIND_ATTACK_SPEED_INCREASE = 0.0f  // 공격속도 증가량 (%)
    const val FOUR_OF_A_KIND_RANGE_INCREASE = 0.0f         // 사거리 증가량 (%)
    const val FOUR_OF_A_KIND_HEALTH_INCREASE = 0.0f        // 체력 증가량 (%)

    // 스트레이트 플러시
    const val STRAIGHT_FLUSH_DAMAGE_INCREASE = 0.8f        // 데미지 80% 증가
    const val STRAIGHT_FLUSH_ATTACK_SPEED_INCREASE = 0.0f  // 공격속도 증가량 (%)
    const val STRAIGHT_FLUSH_RANGE_INCREASE = 0.0f         // 사거리 증가량 (%)
    const val STRAIGHT_FLUSH_HEALTH_INCREASE = 0.0f        // 체력 증가량 (%)

    // 로열 플러시
    const val ROYAL_FLUSH_DAMAGE_INCREASE = 0.9f           // 데미지 90% 증가
    const val ROYAL_FLUSH_ATTACK_SPEED_INCREASE = 0.0f     // 공격속도 증가량 (%)
    const val ROYAL_FLUSH_RANGE_INCREASE = 0.0f            // 사거리 증가량 (%)
    const val ROYAL_FLUSH_HEALTH_INCREASE = 0.0f           // 체력 증가량 (%)
    
    // --------- 포커 카드 및 스킬 관련 설정 ----------
    
    // 포커 카드 뽑기 비용
    const val POKER_CARD_DRAW_COST = 200  // 포커 카드 뽑기 기본 비용
    
    // 추가 카드 구매 비용
    const val FIRST_EXTRA_CARD_COST = 1000  // 첫 번째 추가 카드 비용
    const val SECOND_EXTRA_CARD_COST = 2000  // 두 번째 추가 카드 비용
    
    // 카드 교체 설정
    const val POKER_CARD_REPLACE_COUNT = 3  // 카드 교체 가능 횟수
    
    // --------- 플러시 스킬 관련 설정 ----------
    
    // 하트 플러시 스킬: 체력 회복 관련 설정
    const val HEART_FLUSH_HEAL_AMOUNT = 500   // -1은 전체 회복, 양수는 해당 수치만큼 회복
    
    // 스페이드 플러시 스킬: 적 제거 관련 설정
    const val SPADE_FLUSH_DAMAGE = 500     // 적에게 입히는 즉사 데미지 (매우 높은 값으로 설정)
    
    // 클로버 플러시 스킬: 시간 정지 관련 설정
    const val CLUB_FLUSH_DURATION = 5000L    // 시간 정지 지속 시간 (밀리초)
    
    // 다이아몬드 플러시 스킬: 무적 관련 설정
    const val DIAMOND_FLUSH_DURATION = 5000L // 무적 지속 시간 (밀리초)
    
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
    
    /**
     * 현재 설정된 총 웨이브 수 반환
     */
    fun getTotalWaves(): Int = currentTotalWaves
    
    /**
     * 웨이브 수 설정
     */
    fun setTotalWaves(waves: Int) {
        currentTotalWaves = waves
    }
    
    /**
     * 기본 게임 설정을 반환하는 함수
     * @return 이 게임 설정 객체
     */
    fun getDefaultConfig(): GameConfig {
        return this
    }
} 