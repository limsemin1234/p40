package com.example.p40.game

import android.graphics.Color

/**
 * 게임의 모든 설정 값을 관리하는 객체
 * 게임 밸런스 조정이 필요할 때 이 파일만 수정하면 됨
 */
object GameConfig {
    
    // 게임 기본 설정
    const val TOTAL_WAVES = 10  // 전체 웨이브 수
    const val ENEMIES_PER_WAVE = 50  // 웨이브 당 적 수
    const val WAVE_MESSAGE_DURATION = 2000L  // 웨이브 메시지 표시 시간 (밀리초)
    
    // 디버그 모드 설정
    const val DEBUG_MODE = false // 디버그 정보 표시 여부
    
    // 성능 및 제한 설정
    const val FRAME_LIMIT = 60 // 최대 FPS
    const val MAX_ENEMIES = 100 // 화면에 표시되는 최대 적 수
    const val MAX_MISSILES = 200 // 화면에 표시되는 최대 미사일 수
    
    // 디펜스 유닛 설정
    const val DEFENSE_UNIT_SIZE = 30f  // 디펜스 유닛 크기
    const val DEFENSE_UNIT_COLOR = Color.BLUE  // 디펜스 유닛 색상
    const val DEFENSE_UNIT_ATTACK_RANGE = 300f  // 공격 범위
    const val DEFENSE_UNIT_ATTACK_COOLDOWN = 1000L  // 공격속도 1초
    const val DEFENSE_UNIT_INITIAL_HEALTH = 100 // 초기 체력
    const val DEFENSE_UNIT_INITIAL_MAX_HEALTH = 100 // 초기 최대 체력
    
    // 미사일 설정
    const val MISSILE_SIZE = 5f  // 미사일 크기
    const val MISSILE_SPEED = 10f  // 미사일 속도
    const val MISSILE_DAMAGE = 50  // 미사일 기본 데미지
    const val MISSILE_COLOR = Color.YELLOW  // 미사일 색상
    
    // 일반 적 설정
    const val ENEMY_BASE_SIZE = 10f  // 기본 크기
    const val ENEMY_BASE_HEALTH = 50  // 기본 체력
    const val ENEMY_COLOR = Color.RED  // 적 색상
    const val NORMAL_ENEMY_DAMAGE = 5  // 일반 적의 공격력
    
    // 공중 적(Flying Enemy) 설정
    const val FLYING_ENEMY_WAVE_THRESHOLD = 6  // 공중 적이 등장하기 시작하는 웨이브
    const val FLYING_ENEMY_SPAWN_CHANCE = 0.3f  // 공중 적 등장 확률 (0~1)
    const val FLYING_ENEMY_SPEED_MULTIPLIER = 1.2f  // 공중 적 이동 속도 계수
    const val FLYING_ENEMY_DAMAGE_MULTIPLIER = 1.2f  // 공중 적이 받는 데미지 계수 (취약함)
    const val FLYING_ENEMY_HOVER_AMPLITUDE = 3.0  // 공중 적 호버링 진폭
    const val FLYING_ENEMY_HOVER_PERIOD = 300.0  // 공중 적 호버링 주기 (밀리초)
    
    // 적 생성 및 이동 속도 기본 설정
    const val BASE_ENEMY_SPAWN_INTERVAL = 2000L  // 기본 적 생성 간격 (2초)
    const val BASE_ENEMY_SPEED = 1.0f           // 기본 적 이동 속도
    const val ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE = 0.1f // 웨이브당 생성 간격 감소율 (10%)
    const val ENEMY_SPEED_INCREASE_PER_WAVE = 0.05f        // 웨이브당 이동 속도 증가율 (5%)
    const val MIN_ENEMY_SPAWN_INTERVAL = 500L   // 최소 적 생성 간격 (밀리초)
    
    // 웨이브별 적 체력 배율 (ENEMY_BASE_HEALTH에 곱해짐)
    const val WAVE_1_HEALTH_MULTIPLIER = 1.0f
    const val WAVE_2_HEALTH_MULTIPLIER = 1.1f
    const val WAVE_3_HEALTH_MULTIPLIER = 1.2f
    const val WAVE_4_HEALTH_MULTIPLIER = 1.3f
    const val WAVE_5_HEALTH_MULTIPLIER = 1.4f
    const val WAVE_6_HEALTH_MULTIPLIER = 1.5f
    const val WAVE_7_HEALTH_MULTIPLIER = 1.6f
    const val WAVE_8_HEALTH_MULTIPLIER = 1.7f
    const val WAVE_9_HEALTH_MULTIPLIER = 1.8f
    const val WAVE_10_HEALTH_MULTIPLIER = 2.0f
    
    // 보스 설정 - 멀티플라이어 방식에서 고정값으로 변경
    const val BOSS_SIZE = 40f  // 보스 크기
    const val BOSS_BASE_HEALTH = 200  // 보스 기본 체력
    const val BOSS_BASE_SPEED = 0.8f  // 보스 기본 이동 속도
    const val BOSS_DAMAGE = 20  // 보스 공격력
    const val BOSS_COLOR = Color.MAGENTA  // 보스 색상
    const val BOSS_BORDER_COLOR = Color.YELLOW  // 보스 테두리 색상
    const val BOSS_BORDER_WIDTH = 5f  // 보스 테두리 두께
    const val BOSS_DAMAGE_REDUCTION = 0.75f  // 보스의 데미지 감소율 (받는 데미지의 75%만 적용)
    const val BOSS_ENRAGE_HEALTH_RATIO = 0.5f  // 보스 분노 모드 진입 체력 비율 (최대 체력의 50%)
    const val BOSS_SPEED_MULTIPLIER = 0.8f  // 보스 이동 속도 계수 (일반 적보다 느림)
    const val BOSS_ZIGZAG_AMPLITUDE = 2.0  // 보스 지그재그 좌우 진폭(값을 높이면 더 넒게 좌우로 진동)
    const val BOSS_ZIGZAG_PERIOD = 500.0  // 보스 지그재그 주기 (밀리초)(값을 낮추면 보스가 더 빠르게 좌우 진동)
    
    // 웨이브별 보스 체력 증가율
    const val BOSS_HEALTH_INCREASE_PER_WAVE = 100  // 웨이브당 보스 체력 증가량
    const val BOSS_DAMAGE_INCREASE_PER_WAVE = 10   // 웨이브당 보스 공격력 증가량
    const val BOSS_SPEED_INCREASE_PER_WAVE = 0.05f // 웨이브당 보스 속도 증가량
    
    // 웨이브별 적 데미지 증가량
    const val ENEMY_DAMAGE_PER_WAVE = 5  // 웨이브당 적 데미지 증가량
    
    // 점수 설정
    const val SCORE_PER_NORMAL_ENEMY = 15  // 일반 적 처치 시 얻는 점수(자원) - 주석과 일치하도록 수정
    const val SCORE_PER_BOSS = 120  // 보스 처치 시 얻는 점수(자원)
    
    // 게임 오버 조건
    const val CENTER_REACHED_DAMAGE = 1000  // 중앙 도달 시 입히는 데미지
    
    // UI 설정
    const val TEXT_SIZE_NORMAL = 48f  // 일반 텍스트 크기
    const val TEXT_SIZE_WAVE = 100f  // 웨이브 텍스트 크기
    const val TEXT_SIZE_PAUSE = 72f  // 일시정지 텍스트 크기
    const val TEXT_COLOR = Color.WHITE  // 텍스트 색상
    const val WAVE_TEXT_COLOR = Color.YELLOW  // 웨이브 텍스트 색상
    
    // --------- 업그레이드 관련 설정 ----------
    
    // 데미지 업그레이드 설정
    const val DAMAGE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val DAMAGE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val DAMAGE_UPGRADE_VALUE = 1  // 업그레이드당 데미지 증가량
    const val DAMAGE_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨
    
    // 공격속도 업그레이드 설정
    const val ATTACK_SPEED_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_SPEED_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val ATTACK_SPEED_UPGRADE_PERCENT = 0.02f  // 업그레이드당 속도 증가율 0.02%
    const val ATTACK_SPEED_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨
    
    // 공격범위 업그레이드 설정
    const val ATTACK_RANGE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_RANGE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val ATTACK_RANGE_UPGRADE_VALUE = 5f  // 업그레이드당 범위 증가량
    const val ATTACK_RANGE_UPGRADE_MAX_LEVEL = 60  // 최대 업그레이드 레벨
    
    // 체력 업그레이드 설정
    const val DEFENSE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val DEFENSE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val DEFENSE_UPGRADE_VALUE = 20  // 업그레이드당 최대 체력 증가량
    const val DEFENSE_UPGRADE_MAX_LEVEL = 100  // 최대 업그레이드 레벨
    
    // --------- 버프 관련 설정 ----------
    
    // 버프 UI 설정
    val BUFF_DEFENSE_COLOR = Color.parseColor("#3F51B5")  // 디펜스 버프 배경색
    val BUFF_ENEMY_NERF_COLOR = Color.parseColor("#F44336")  // 적 너프 배경색
    val BUFF_DEFENSE_STROKE_COLOR = Color.parseColor("#5F71D5")  // 디펜스 버프 테두리 색상
    val BUFF_ENEMY_NERF_STROKE_COLOR = Color.parseColor("#FF6356")  // 적 너프 테두리 색상
    
    // 버프 최대 레벨
    const val BUFF_MAX_LEVEL = 5  // 각 버프의 최대 레벨
    
    // 포커 카드 관련 설정
    const val POKER_CARD_REPLACE_COUNT = 3  // 포커 카드 교체 가능 횟수
    const val POKER_HAND_DURATION = 30000L  // 포커 핸드 효과 지속 시간 (30초)
    
    // 포커 핸드별 효과 설정
    const val ROYAL_FLUSH_DAMAGE_BOOST = 3.0f  // 로열 플러시 데미지 증가율 (300%)
    const val STRAIGHT_FLUSH_DAMAGE_BOOST = 2.5f  // 스트레이트 플러시 데미지 증가율 (250%)
    const val FOUR_OF_A_KIND_DAMAGE_BOOST = 2.0f  // 포카드 데미지 증가율 (200%)
    const val FULL_HOUSE_DAMAGE_BOOST = 1.8f  // 풀하우스 데미지 증가율 (180%)
    const val FLUSH_DAMAGE_BOOST = 1.5f  // 플러시 데미지 증가율 (150%)
    const val STRAIGHT_DAMAGE_BOOST = 1.3f  // 스트레이트 데미지 증가율 (130%)
    const val THREE_OF_A_KIND_DAMAGE_BOOST = 1.2f  // 트리플 데미지 증가율 (120%)
    const val TWO_PAIR_DAMAGE_BOOST = 1.1f  // 투페어 데미지 증가율 (110%)
    const val ONE_PAIR_DAMAGE_BOOST = 1.05f  // 원페어 데미지 증가율 (105%)
    
    // 플러시 스킬 설정
    const val FLUSH_SKILL_COOLDOWN = 10000L  // 플러시 스킬 쿨타임 (10초)
    const val FLUSH_SKILL_DURATION = 5000L   // 플러시 스킬 지속 시간 (5초)
    
    // 문양별 플러시 스킬 효과
    const val HEART_FLUSH_HEAL_AMOUNT = 50  // 하트 플러시 회복량
    const val SPADE_FLUSH_DAMAGE_BOOST = 2.0f  // 스페이드 플러시 데미지 증가율 (200%)
    const val DIAMOND_FLUSH_RESOURCE_GAIN = 100  // 다이아몬드 플러시 자원 획득량
    const val CLUB_FLUSH_SLOW_AMOUNT = 0.5f  // 클로버 플러시 적 이동속도 감소율 (50%)
    
    // 플러시 스킬 버튼 UI 설정
    const val FLUSH_SKILL_BUTTON_SIZE = 60  // 스킬 버튼 크기 (dp)
    const val FLUSH_SKILL_BUTTON_MARGIN = 8  // 스킬 버튼 간격 (dp)
    const val FLUSH_SKILL_BUTTON_ALPHA_DISABLED = 0.5f  // 비활성화 상태 투명도
    
    // 플러시 스킬 색상 설정
    val HEART_FLUSH_COLOR = Color.parseColor("#FF4081")  // 하트 플러시 색상
    val SPADE_FLUSH_COLOR = Color.parseColor("#212121")  // 스페이드 플러시 색상
    val DIAMOND_FLUSH_COLOR = Color.parseColor("#FF5252")  // 다이아몬드 플러시 색상
    val CLUB_FLUSH_COLOR = Color.parseColor("#4CAF50")  // 클로버 플러시 색상
    
    // 플러시 스킬 효과 표시 설정
    const val FLUSH_EFFECT_PARTICLE_COUNT = 20  // 효과 파티클 수
    const val FLUSH_EFFECT_DURATION = 1000L  // 효과 표시 지속 시간 (1초)
    const val FLUSH_EFFECT_SIZE = 30f  // 효과 파티클 크기
    
    // 성능 관련 설정
    const val OFFSCREEN_MARGIN = 300f // 화면 외부 마진 (오브젝트 제거 범위)
    const val OBJECT_POOL_SIZE = 100 // 오브젝트 풀 크기
    
    // --------- 렌더링 관련 설정 ----------
    
    // 적 렌더링 설정
    const val ENEMY_RENDER_MARGIN_X = 300f // 적 렌더링 X축 마진 (화면 밖에서도 그리기 위함)
    const val ENEMY_RENDER_MARGIN_Y = 300f // 적 렌더링 Y축 마진 (화면 밖에서도 그리기 위함)
    
    // 미사일 렌더링 설정
    const val MISSILE_RENDER_MARGIN_X = 20f // 미사일 렌더링 X축 마진
    const val MISSILE_RENDER_MARGIN_Y = 20f // 미사일 렌더링 Y축 마진
    
    // --------- 메시지 관련 설정 ----------
    
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
    
    // 디버그 정보 설정
    const val DEBUG_TEXT_SIZE = 30f // 디버그 텍스트 크기
    const val DEBUG_TEXT_COLOR = Color.GREEN // 디버그 텍스트 색상
    const val DEBUG_TEXT_MARGIN_X = 10f // 디버그 텍스트 X축 마진
    const val DEBUG_TEXT_SPACING = 30f // 디버그 텍스트 줄 간격
    
    // 게임 로직 설정
    const val FAR_OFFSCREEN_MARGIN = 1500f // 적이 제거되는 화면 외부 거리
    const val ENEMY_SPAWN_DISTANCE_FACTOR = 0.5f // 적 생성 거리 계수
    const val BOSS_SPAWN_DISTANCE_FACTOR = 0.5f // 보스 생성 거리 계수 (화면 크기의 비율)
    const val ENEMY_UPDATE_MARGIN = 200f // 적 생성 거리에 추가되는 여유 공간 (화면 밖 적 업데이트 범위 확장)

    // 게임 레벨 설정
    private var currentTotalWaves: Int = 10       // 기본 총 웨이브 수
    
    /**
     * 웨이브별 적 체력 계산 (ENEMY_BASE_HEALTH에 배율을 곱함)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @return 적 체력
     */
    fun getEnemyHealthForWave(wave: Int, isBoss: Boolean = false): Int {
        if (isBoss) {
            // 보스는 기본 체력 + 웨이브당 증가량
            return BOSS_BASE_HEALTH + ((wave - 1) * BOSS_HEALTH_INCREASE_PER_WAVE)
        } else {
            // 일반 적은 기존 방식대로 계산
            val multiplier = when(wave) {
                1 -> WAVE_1_HEALTH_MULTIPLIER
                2 -> WAVE_2_HEALTH_MULTIPLIER
                3 -> WAVE_3_HEALTH_MULTIPLIER
                4 -> WAVE_4_HEALTH_MULTIPLIER
                5 -> WAVE_5_HEALTH_MULTIPLIER
                6 -> WAVE_6_HEALTH_MULTIPLIER
                7 -> WAVE_7_HEALTH_MULTIPLIER
                8 -> WAVE_8_HEALTH_MULTIPLIER
                9 -> WAVE_9_HEALTH_MULTIPLIER
                10 -> WAVE_10_HEALTH_MULTIPLIER
                else -> WAVE_1_HEALTH_MULTIPLIER
            }
            return (ENEMY_BASE_HEALTH * multiplier).toInt()
        }
    }
    
    /**
     * 웨이브별 적 데미지 계산
     * @param wave 웨이브 번호
     * @param isBoss 보스 여부
     * @return 적 데미지
     */
    fun getEnemyDamageForWave(wave: Int, isBoss: Boolean): Int {
        if (isBoss) {
            // 보스는 기본 데미지 + 웨이브당 증가량
            return BOSS_DAMAGE + ((wave - 1) * BOSS_DAMAGE_INCREASE_PER_WAVE)
        } else {
            // 일반 적은 기존 방식대로 계산
            return NORMAL_ENEMY_DAMAGE + ((wave - 1) * ENEMY_DAMAGE_PER_WAVE)
        }
    }
    
    /**
     * 웨이브에 따른 적 생성 간격 계산 (밀리초)
     * @param wave 현재 웨이브
     * @return 적 생성 간격 (밀리초)
     */
    fun getEnemySpawnIntervalForWave(wave: Int): Long {
        // ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE 적용 (비율 기반 감소)
        val decreaseFactor = 1 - ((wave - 1) * ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE)
        val interval = (BASE_ENEMY_SPAWN_INTERVAL * decreaseFactor).toLong()
        return maxOf(interval, MIN_ENEMY_SPAWN_INTERVAL)
    }
    
    /**
     * 웨이브에 따른 적 이동 속도 계산
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @return 적 이동 속도
     */
    fun getEnemySpeedForWave(wave: Int, isBoss: Boolean = false): Float {
        if (isBoss) {
            // 보스는 기본 속도 + 웨이브당 증가량
            return BOSS_BASE_SPEED + ((wave - 1) * BOSS_SPEED_INCREASE_PER_WAVE)
        } else {
            // 일반 적은 기존 방식대로 계산
            val increase = 1 + ((wave - 1) * ENEMY_SPEED_INCREASE_PER_WAVE)
            return BASE_ENEMY_SPEED * increase
        }
    }
    
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
} 