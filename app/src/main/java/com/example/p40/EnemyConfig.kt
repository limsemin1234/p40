package com.example.p40

import android.graphics.Color

/**
 * 게임의 적 유닛 관련 설정을 관리하는 객체
 * 적 유닛 밸런스 조정이 필요할 때 이 파일만 수정하면 됨
 */
object EnemyConfig {
    // --------- 공통 설정 ----------
    
    // 기타 설정
    const val CENTER_REACHED_DAMAGE = 1000  // 중앙 도달 시 입히는 데미지

    // 처치 시 자원 획득
    const val SCORE_PER_NORMAL_ENEMY = 1000  // 일반 적 처치 시 얻는 점수(자원)
    const val SCORE_PER_BOSS = 200  // 보스 처치 시 얻는 점수(자원)
    const val FLYING_ENEMY_SCORE = 20 // 공중 적 처치 시 획득 점수(자원)
    
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
    
    // 적 생성 및 이동 속도 기본 설정
    const val BASE_ENEMY_SPAWN_INTERVAL = 2000L  // 기본 적 생성 간격 (2초)
    const val MIN_ENEMY_SPAWN_INTERVAL = 500L   // 최소 적 생성 간격 (밀리초)
    const val ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE = 0.1f // 웨이브당 생성 간격 감소율 (10%)

    // --------- 일반 적 설정 ----------
    
    // 일반 적 기본 설정
    const val ENEMY_COLOR = Color.RED  // 일반 적 색상
    const val ENEMY_BASE_SIZE = 10f  // 일반 적 크기
    const val NORMAL_ENEMY_DAMAGE = 5  // 일반 적의 공격력
    const val ENEMY_BASE_HEALTH = 50  // 일반 적 기본 체력
    const val BASE_ENEMY_SPEED = 1.0f  // 일반 적 기본 이동 속도
    const val ENEMY_DAMAGE_PER_WAVE = 5  // 웨이브당 일반 적 데미지 증가량
    const val ENEMY_HEALTH_INCREASE_PER_WAVE = 5  // 웨이브당 일반 적 체력 증가량
    const val ENEMY_SPEED_INCREASE_PER_WAVE = 0.05f  // 웨이브당 일반 적 이동 속도 증가율 (5%)
    
    // 일반 적 추가 설정
    const val NORMAL_ENEMY_DAMAGE_REDUCTION = 1.0f // 일반 적 데미지 감소율 (감소 없음)
    const val NORMAL_ENEMY_RENDER_MARGIN_X = ENEMY_RENDER_MARGIN_X // 일반 적 렌더링 X축 마진
    const val NORMAL_ENEMY_RENDER_MARGIN_Y = ENEMY_RENDER_MARGIN_Y // 일반 적 렌더링 Y축 마진
    const val NORMAL_ENEMY_SPAWN_DISTANCE_FACTOR = ENEMY_SPAWN_DISTANCE_FACTOR // 일반 적 생성 거리 계수
    
    /**
     * 일반 적 웨이브별 체력 계산
     * @param wave 현재 웨이브
     * @return 일반 적 체력
     */
    fun getNormalEnemyHealthForWave(wave: Int): Int {
        // 기본 체력 + (웨이브 - 1) * 웨이브당 체력 증가량
        return ENEMY_BASE_HEALTH + (wave - 1) * ENEMY_HEALTH_INCREASE_PER_WAVE
    }
    
    /**
     * 일반 적 웨이브별 데미지 계산
     * @param wave 현재 웨이브
     * @return 일반 적 데미지
     */
    fun getNormalEnemyDamageForWave(wave: Int): Int {
        return NORMAL_ENEMY_DAMAGE + ((wave - 1) * ENEMY_DAMAGE_PER_WAVE)
    }
    
    /**
     * 일반 적 웨이브별 이동 속도 계산
     * @param wave 현재 웨이브
     * @return 일반 적 이동 속도
     */
    fun getNormalEnemySpeedForWave(wave: Int): Float {
        val increase = 1 + ((wave - 1) * ENEMY_SPEED_INCREASE_PER_WAVE)
        return BASE_ENEMY_SPEED * increase
    }

    // --------- 공중 적 설정 ----------
    
    // 공중 적 기본 설정
    const val FLYING_ENEMY_WAVE_THRESHOLD = 6  // 공중 적이 등장하기 시작하는 웨이브
    const val FLYING_ENEMY_SPAWN_CHANCE = 0.3f  // 공중 적 등장 확률 30% (0~1)

    const val FLYING_ENEMY_COLOR = Color.CYAN  // 공중 적 색상
    const val FLYING_ENEMY_SIZE = 12f  // 공중 적 크기 (일반 적보다 약간 큼)
    const val FLYING_ENEMY_DAMAGE = 20  // 공중 적의 기본 공격력
    const val FLYING_ENEMY_BASE_HEALTH = 60  // 공중 적의 기본 체력
    
    // 공중 적 움직임 설정
    const val FLYING_ENEMY_SPEED_MULTIPLIER = 1.2f  // 공중 적 이동 속도 계수
    const val FLYING_ENEMY_BASE_SPEED = BASE_ENEMY_SPEED * FLYING_ENEMY_SPEED_MULTIPLIER  // 공중 적 기본 이동 속도
    const val FLYING_ENEMY_HOVER_AMPLITUDE = 3.0  // 공중 적 호버링 진폭
    const val FLYING_ENEMY_HOVER_PERIOD = 300.0  // 공중 적 호버링 주기 (밀리초)
    
    // 공중 적 증가율 설정
    const val FLYING_ENEMY_DAMAGE_INCREASE_PER_WAVE = 10  // 웨이브당 공중 적 공격력 증가량
    const val FLYING_ENEMY_HEALTH_INCREASE_PER_WAVE = 10  // 웨이브당 공중 적 체력 증가량
    const val FLYING_ENEMY_SPEED_INCREASE_PER_WAVE = 0.1f  // 웨이브당 공중 적 속도 증가량
    
    // 공중 적 추가 설정
    const val FLYING_ENEMY_DAMAGE_MULTIPLIER = 1.2f  // 공중 적이 받는 데미지 계수 (취약함)
    const val FLYING_ENEMY_RENDER_MARGIN_X = ENEMY_RENDER_MARGIN_X // 공중 적 렌더링 X축 마진
    const val FLYING_ENEMY_RENDER_MARGIN_Y = ENEMY_RENDER_MARGIN_Y // 공중 적 렌더링 Y축 마진
    const val FLYING_ENEMY_SPAWN_DISTANCE_FACTOR = ENEMY_SPAWN_DISTANCE_FACTOR // 공중 적 생성 거리 계수
    
    /**
     * 공중 적 웨이브별 체력 계산
     * @param wave 현재 웨이브
     * @return 공중 적 체력
     */
    fun getFlyingEnemyHealthForWave(wave: Int): Int {
        val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
        val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
        return FLYING_ENEMY_BASE_HEALTH + (waveIncrease * FLYING_ENEMY_HEALTH_INCREASE_PER_WAVE)
    }
    
    /**
     * 공중 적 웨이브별 데미지 계산
     * @param wave 현재 웨이브
     * @return 공중 적 데미지
     */
    fun getFlyingEnemyDamageForWave(wave: Int): Int {
        val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
        val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
        return FLYING_ENEMY_DAMAGE + (waveIncrease * FLYING_ENEMY_DAMAGE_INCREASE_PER_WAVE)
    }
    
    /**
     * 공중 적 웨이브별 이동 속도 계산
     * @param wave 현재 웨이브
     * @return 공중 적 이동 속도
     */
    fun getFlyingEnemySpeedForWave(wave: Int): Float {
        val flyingWave = wave - FLYING_ENEMY_WAVE_THRESHOLD + 1
        val waveIncrease = if (flyingWave > 0) flyingWave - 1 else 0
        return FLYING_ENEMY_BASE_SPEED + (waveIncrease * FLYING_ENEMY_SPEED_INCREASE_PER_WAVE)
    }

    // --------- 보스 설정 ----------
    
    // 보스 기본 설정
    const val BOSS_SIZE = 40f  // 보스 크기
    const val BOSS_BASE_HEALTH = 200 // 보스 기본 체력
    const val BOSS_BASE_SPEED = 0.8f  // 보스 기본 이동 속도
    const val BOSS_DAMAGE = 20  // 보스 공격력
    const val BOSS_COLOR = Color.MAGENTA  // 보스 색상
    
    // 보스 시각 효과 설정
    const val BOSS_BORDER_COLOR = Color.YELLOW  // 보스 테두리 색상
    const val BOSS_BORDER_WIDTH = 5f  // 보스 테두리 두께
    
    // 보스 움직임 설정
    const val BOSS_SPEED_MULTIPLIER = 0.8f  // 보스 이동 속도 계수 (일반 적보다 느림)
    const val BOSS_ZIGZAG_AMPLITUDE = 3.0  // 보스 지그재그 좌우 진폭
    const val BOSS_ZIGZAG_PERIOD = 400.0  // 보스 지그재그 주기 (밀리초)
    const val BOSS_SPEED_INCREASE_PER_WAVE = 0.05f // 웨이브당 보스 속도 증가량
    
    // 보스 전투 설정
    const val BOSS_DAMAGE_REDUCTION = 0.75f  // 보스의 데미지 감소율 (받는 데미지의 75%만 적용)
    const val BOSS_ENRAGE_HEALTH_RATIO = 0.5f  // 보스 분노 모드 진입 체력 비율 (최대 체력의 50%)
    const val BOSS_HEALTH_INCREASE_PER_WAVE = 100  // 웨이브당 보스 체력 증가량
    const val BOSS_DAMAGE_INCREASE_PER_WAVE = 10   // 웨이브당 보스 공격력 증가량
    
    // 보스 추가 설정
    const val BOSS_RENDER_MARGIN_X = ENEMY_RENDER_MARGIN_X // 보스 렌더링 X축 마진
    const val BOSS_RENDER_MARGIN_Y = ENEMY_RENDER_MARGIN_Y // 보스 렌더링 Y축 마진
    const val BOSS_KILL_COIN_REWARD_BASE = 100  // 1웨이브 보스 처치 시 획득 기본 코인 보상
    const val BOSS_KILL_COIN_REWARD_INCREMENT = 50  // 웨이브당 증가하는 코인 보상량
    
    /**
     * 보스 웨이브별 체력 계산
     * @param wave 현재 웨이브
     * @return 보스 체력
     */
    fun getBossHealthForWave(wave: Int): Int {
        return BOSS_BASE_HEALTH + ((wave - 1) * BOSS_HEALTH_INCREASE_PER_WAVE)
    }
    
    /**
     * 보스 웨이브별 데미지 계산
     * @param wave 현재 웨이브
     * @return 보스 데미지
     */
    fun getBossDamageForWave(wave: Int): Int {
        return BOSS_DAMAGE + ((wave - 1) * BOSS_DAMAGE_INCREASE_PER_WAVE)
    }
    
    /**
     * 보스 웨이브별 이동 속도 계산
     * @param wave 현재 웨이브
     * @return 보스 이동 속도
     */
    fun getBossSpeedForWave(wave: Int): Float {
        return BOSS_BASE_SPEED + ((wave - 1) * BOSS_SPEED_INCREASE_PER_WAVE)
    }
    
    /**
     * 웨이브에 따른 보스 처치 코인 보상 계산
     * @param wave 현재 웨이브
     * @return 보스 처치 시 획득하는 코인
     */
    fun getBossKillCoinReward(wave: Int): Int {
        return BOSS_KILL_COIN_REWARD_BASE + (wave - 1) * BOSS_KILL_COIN_REWARD_INCREMENT
    }
    
    /**
     * 웨이브에 따른 적 생성 간격 계산 (밀리초)
     * @param wave 현재 웨이브
     * @return 적 생성 간격 (밀리초)
     */
    fun getEnemySpawnIntervalForWave(wave: Int): Long {
        // 웨이브가 증가할수록 적 생성 간격 감소
        val decreaseFactor = 1 - ((wave - 1) * ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE)
        val interval = (BASE_ENEMY_SPAWN_INTERVAL * decreaseFactor).toLong()
        return maxOf(interval, MIN_ENEMY_SPAWN_INTERVAL)
    }
    
    /**
     * 웨이브별 적 체력 계산 (하위 호환성 유지)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @param isFlying 공중적 여부
     * @return 적 체력
     */
    fun getEnemyHealthForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Int {
        return when {
            isBoss -> getBossHealthForWave(wave)
            isFlying -> getFlyingEnemyHealthForWave(wave)
            else -> getNormalEnemyHealthForWave(wave)
        }
    }
    
    /**
     * 웨이브별 적 데미지 계산 (하위 호환성 유지)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @param isFlying 공중적 여부
     * @return 적 데미지
     */
    fun getEnemyDamageForWave(wave: Int, isBoss: Boolean, isFlying: Boolean = false): Int {
        return when {
            isBoss -> getBossDamageForWave(wave)
            isFlying -> getFlyingEnemyDamageForWave(wave)
            else -> getNormalEnemyDamageForWave(wave)
        }
    }
    
    /**
     * 웨이브별 적 이동 속도 계산 (하위 호환성 유지)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @param isFlying 공중적 여부
     * @return 적 이동 속도
     */
    fun getEnemySpeedForWave(wave: Int, isBoss: Boolean = false, isFlying: Boolean = false): Float {
        return when {
            isBoss -> getBossSpeedForWave(wave)
            isFlying -> getFlyingEnemySpeedForWave(wave)
            else -> getNormalEnemySpeedForWave(wave)
        }
    }
} 