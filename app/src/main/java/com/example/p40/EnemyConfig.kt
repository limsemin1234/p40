package com.example.p40

import android.graphics.Color

/**
 * 게임의 적 유닛 관련 설정을 관리하는 객체
 * 적 유닛 밸런스 조정이 필요할 때 이 파일만 수정하면 됨
 */
object EnemyConfig {
    // --------- 적 유닛 관련 설정 ----------
    
    // 일반 적 설정
    const val ENEMY_BASE_SIZE = 10f  // 기본 크기
    const val ENEMY_BASE_HEALTH = 50  // 기본 체력
    const val ENEMY_COLOR = Color.RED  // 적 색상
    const val NORMAL_ENEMY_DAMAGE = 5  // 일반 적의 공격력
    
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
    
    // 웨이브별 적 데미지 증가량
    const val ENEMY_DAMAGE_PER_WAVE = 5  // 웨이브당 적 데미지 증가량
    
    // 공중 적(Flying Enemy) 설정
    const val FLYING_ENEMY_WAVE_THRESHOLD = 6  // 공중 적이 등장하기 시작하는 웨이브
    const val FLYING_ENEMY_SPAWN_CHANCE = 0.3f  // 공중 적 등장 확률 30% (0~1)
    const val FLYING_ENEMY_SPEED_MULTIPLIER = 1.2f  // 공중 적 이동 속도 계수
    const val FLYING_ENEMY_DAMAGE_MULTIPLIER = 1.2f  // 공중 적이 받는 데미지 계수 (취약함)
    const val FLYING_ENEMY_HOVER_AMPLITUDE = 3.0  // 공중 적 호버링 진폭
    const val FLYING_ENEMY_HOVER_PERIOD = 300.0  // 공중 적 호버링 주기 (밀리초)
    const val FLYING_ENEMY_DAMAGE = 20  // 공중 적의 기본 공격력
    const val FLYING_ENEMY_BASE_HEALTH = 60  // 공중 적의 기본 체력
    const val FLYING_ENEMY_DAMAGE_INCREASE_PER_WAVE = 10  // 웨이브당 공중 적 공격력 증가량
    const val FLYING_ENEMY_HEALTH_INCREASE_PER_WAVE = 10  // 웨이브당 공중 적 체력 증가량
    const val FLYING_ENEMY_SPEED_INCREASE_PER_WAVE = 0.5f  // 웨이브당 공중 적 속도 증가량
    
    // 보스 설정
    const val BOSS_SIZE = 40f  // 보스 크기
    const val BOSS_BASE_HEALTH = 200 // 보스 기본 체력
    const val BOSS_BASE_SPEED = 0.8f  // 보스 기본 이동 속도
    const val BOSS_DAMAGE = 20  // 보스 공격력
    const val BOSS_COLOR = Color.MAGENTA  // 보스 색상
    const val BOSS_BORDER_COLOR = Color.YELLOW  // 보스 테두리 색상
    const val BOSS_BORDER_WIDTH = 5f  // 보스 테두리 두께
    const val BOSS_DAMAGE_REDUCTION = 0.75f  // 보스의 데미지 감소율 (받는 데미지의 75%만 적용)
    const val BOSS_ENRAGE_HEALTH_RATIO = 0.5f  // 보스 분노 모드 진입 체력 비율 (최대 체력의 50%)
    const val BOSS_SPEED_MULTIPLIER = 0.8f  // 보스 이동 속도 계수 (일반 적보다 느림)
    const val BOSS_ZIGZAG_AMPLITUDE = 3.0  // 보스 지그재그 좌우 진폭(값을 높이면 더 넒게 좌우로 진동)
    const val BOSS_ZIGZAG_PERIOD = 400.0  // 보스 지그재그 주기 (밀리초)(값을 낮추면 보스가 더 빠르게 좌우 진동)
    
    // 웨이브별 보스 강화 설정
    const val BOSS_HEALTH_INCREASE_PER_WAVE = 100  // 웨이브당 보스 체력 증가량
    const val BOSS_DAMAGE_INCREASE_PER_WAVE = 10   // 웨이브당 보스 공격력 증가량
    const val BOSS_SPEED_INCREASE_PER_WAVE = 0.05f // 웨이브당 보스 속도 증가량
    
    // 코인 보상 설정
    const val BOSS_KILL_COIN_REWARD_BASE = 100  // 1웨이브 보스 처치 시 획득 기본 코인 보상
    const val BOSS_KILL_COIN_REWARD_INCREMENT = 50  // 웨이브당 증가하는 코인 보상량
    
    // 기타 설정
    const val CENTER_REACHED_DAMAGE = 1000  // 중앙 도달 시 입히는 데미지
    const val SCORE_PER_NORMAL_ENEMY = 1000  // 일반 적 처치 시 얻는 점수(자원)
    const val SCORE_PER_BOSS = 200  // 보스 처치 시 얻는 점수(자원)
    
    // 렌더링 및 업데이트 설정
    const val ENEMY_RENDER_MARGIN_X = 500f // 적 렌더링 X축 마진 (화면 밖에서도 그리기 위함)
    const val ENEMY_RENDER_MARGIN_Y = 500f // 적 렌더링 Y축 마진 (화면 밖에서도 그리기 위함)
    const val FAR_OFFSCREEN_MARGIN = 2000f // 적이 제거되는 화면 외부 거리
    const val ENEMY_SPAWN_DISTANCE_FACTOR = 0.5f // 적 생성 거리 계수
    const val BOSS_SPAWN_DISTANCE_FACTOR = 0.45f // 보스 생성 거리 계수 (화면 크기의 비율)
    const val ENEMY_UPDATE_MARGIN = 250f // 적 생성 거리에 추가되는 여유 공간 (화면 밖 적 업데이트 범위 확장)
    
    // 객체 풀 설정
    const val ENEMY_POOL_INITIAL_SIZE = 100 // 적 객체 풀 초기 크기
    const val ENEMY_POOL_MAX_SIZE = 300 // 적 객체 풀 최대 크기
    
    /**
     * 웨이브별 적 체력 계산 (ENEMY_BASE_HEALTH에 배율을 곱함)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @param isFlying 공중적 여부
     * @return 적 체력
     */
    fun getEnemyHealthForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Int {
        if (isBoss) {
            // 보스는 기본 체력 + 웨이브당 증가량
            return BOSS_BASE_HEALTH + ((wave - 1) * BOSS_HEALTH_INCREASE_PER_WAVE)
        } else if (isFlying) {
            // 공중적은 기본 체력 + 웨이브당 증가량
            val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
            val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
            return FLYING_ENEMY_BASE_HEALTH + (waveIncrease * FLYING_ENEMY_HEALTH_INCREASE_PER_WAVE)
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
     * @param isFlying 공중적 여부
     * @return 적 데미지
     */
    fun getEnemyDamageForWave(wave: Int, isBoss: Boolean, isFlying: Boolean = false): Int {
        if (isBoss) {
            // 보스는 기본 데미지 + 웨이브당 증가량
            return BOSS_DAMAGE + ((wave - 1) * BOSS_DAMAGE_INCREASE_PER_WAVE)
        } else if (isFlying) {
            // 공중적 공격력 계산
            // 웨이브 6부터 등장하므로, 웨이브 6에서는 증가량이 0이 되어 기본 공격력만 적용됨
            val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
            val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
            return FLYING_ENEMY_DAMAGE + (waveIncrease * FLYING_ENEMY_DAMAGE_INCREASE_PER_WAVE)
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
     * @param isFlying 공중적 여부
     * @return 적 이동 속도
     */
    fun getEnemySpeedForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Float {
        if (isBoss) {
            // 보스는 기본 속도 + 웨이브당 증가량
            return BOSS_BASE_SPEED + ((wave - 1) * BOSS_SPEED_INCREASE_PER_WAVE)
        } else if (isFlying) {
            // 공중적은 기본 속도에 계수를 곱하고 웨이브당 증가량 적용
            val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
            val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
            return (BASE_ENEMY_SPEED * FLYING_ENEMY_SPEED_MULTIPLIER) + (waveIncrease * FLYING_ENEMY_SPEED_INCREASE_PER_WAVE)
        } else {
            // 일반 적은 기존 방식대로 계산
            val increase = 1 + ((wave - 1) * ENEMY_SPEED_INCREASE_PER_WAVE)
            return BASE_ENEMY_SPEED * increase
        }
    }
    
    /**
     * 웨이브에 따른 보스 처치 코인 보상 계산
     * @param wave 현재 웨이브
     * @return 보스 처치 시 획득하는 코인
     */
    fun getBossKillCoinReward(wave: Int): Int {
        // 1웨이브: 기본값 150, 이후 매 웨이브마다 50씩 증가
        return BOSS_KILL_COIN_REWARD_BASE + (wave - 1) * BOSS_KILL_COIN_REWARD_INCREMENT
    }
} 