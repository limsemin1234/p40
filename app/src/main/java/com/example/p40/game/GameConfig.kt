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
    const val DEBUG_MODE = true // 디버그 정보 표시 여부
    
    // 성능 및 제한 설정
    const val FRAME_LIMIT = 60 // 최대 FPS
    const val MAX_ENEMIES = 100 // 화면에 표시되는 최대 적 수
    const val MAX_MISSILES = 200 // 화면에 표시되는 최대 미사일 수
    
    // 디펜스 유닛 설정
    const val DEFENSE_UNIT_SIZE = 30f  // 디펜스 유닛 크기
    const val DEFENSE_UNIT_COLOR = Color.BLUE  // 디펜스 유닛 색상
    const val DEFENSE_UNIT_ATTACK_RANGE = 500f  // 공격 범위
    const val DEFENSE_UNIT_ATTACK_COOLDOWN = 1000L  // 공격속도 1초
    const val DEFENSE_UNIT_INITIAL_HEALTH = 100 // 초기 체력
    const val DEFENSE_UNIT_INITIAL_MAX_HEALTH = 100 // 초기 최대 체력
    
    // 미사일 설정
    const val MISSILE_SIZE = 5f  // 미사일 크기
    const val MISSILE_SPEED = 10f  // 미사일 속도 (5f -> 10f로 증가)
    const val MISSILE_DAMAGE = 50  // 미사일 기본 데미지 (50에서 100으로 증가)
    const val MISSILE_COLOR = Color.YELLOW  // 미사일 색상
    const val MISSILE_MAX_DISTANCE = 3000f  // 미사일 최대 이동 거리 (2000f -> 3000f로 증가)
    
    // 일반 적 설정
    const val ENEMY_BASE_SIZE = 10f  // 기본 크기 (30f에서 10f로 줄임)
    const val ENEMY_BASE_HEALTH = 50  // 기본 체력
    const val ENEMY_COLOR = Color.RED  // 적 색상
    const val NORMAL_ENEMY_DAMAGE = 5  // 일반 적의 공격력
    
    // 적 생성 및 이동 속도 기본 설정
    const val BASE_ENEMY_SPAWN_INTERVAL = 2000L  // 기본 적 생성 간격 (2초로 변경)
    const val BASE_ENEMY_SPEED = 1.0f           // 기본 적 이동 속도
    const val ENEMY_SPAWN_INTERVAL_DECREASE_PER_WAVE = 0.1f // 웨이브당 생성 간격 감소율 (10%)
    const val ENEMY_SPEED_INCREASE_PER_WAVE = 0.15f        // 웨이브당 이동 속도 증가율 (15%)
    const val MIN_ENEMY_SPAWN_INTERVAL = 500L   // 최소 적 생성 간격 (밀리초)
    
    // 웨이브별 적 생성 간격 및 이동 속도 맵
    val WAVE_ENEMY_SPAWN_COOLDOWNS: Map<Int, Long> = (1..TOTAL_WAVES).associateWith { wave ->
        getEnemySpawnIntervalForWave(wave)
    }
    
    val WAVE_ENEMY_SPEEDS: Map<Int, Float> = (1..TOTAL_WAVES).associateWith { wave ->
        getEnemySpeedForWave(wave)
    }
    
    // 웨이브별 적 체력 배율 (ENEMY_BASE_HEALTH에 곱해짐)
    const val WAVE_1_HEALTH_MULTIPLIER = 1.0f
    const val WAVE_2_HEALTH_MULTIPLIER = 1.15f
    const val WAVE_3_HEALTH_MULTIPLIER = 1.3f
    const val WAVE_4_HEALTH_MULTIPLIER = 1.45f
    const val WAVE_5_HEALTH_MULTIPLIER = 1.6f
    const val WAVE_6_HEALTH_MULTIPLIER = 1.75f
    const val WAVE_7_HEALTH_MULTIPLIER = 1.9f
    const val WAVE_8_HEALTH_MULTIPLIER = 2.05f
    const val WAVE_9_HEALTH_MULTIPLIER = 2.2f
    const val WAVE_10_HEALTH_MULTIPLIER = 2.4f
    
    // 보스 설정
    const val BOSS_SIZE_MULTIPLIER = 2.0f  // 보스 크기 배율
    const val BOSS_HEALTH_MULTIPLIER = 4  // 보스 체력 배율 (5에서 4로 감소)
    const val BOSS_SPEED_MULTIPLIER = 0.8f  // 보스 속도 배율 (0.7에서 0.8로 조정)
    const val BOSS_COLOR = Color.MAGENTA  // 보스 색상
    const val BOSS_BORDER_COLOR = Color.YELLOW  // 보스 테두리 색상
    const val BOSS_BORDER_WIDTH = 5f  // 보스 테두리 두께
    const val BOSS_DAMAGE = 12  // 보스 적의 공격력 (15에서 12로 감소)
    
    // 웨이브별 적 데미지 증가량 (웨이브 당 기본 데미지에 더해짐)
    const val ENEMY_DAMAGE_PER_WAVE = 1  // 웨이브당 적 데미지 증가량
    
    // 카드 스킬 설정
    const val CARD_DAMAGE_NORMAL = 120  // 일반 적에게 주는 데미지 (100에서 120으로 증가)
    const val CARD_DAMAGE_BOSS = 80  // 보스에게 주는 데미지 (50에서 80으로 증가)
    const val CARD_COOLDOWN = 8000L  // 카드 사용 쿨다운 (10000에서 8000으로 감소)
    
    /**
     * 웨이브별 적 체력 계산 (ENEMY_BASE_HEALTH에 배율을 곱함)
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @return 적 체력
     */
    fun getEnemyHealthForWave(wave: Int, isBoss: Boolean = false): Int {
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
        val baseHealth = ENEMY_BASE_HEALTH
        
        return if (isBoss) {
            (baseHealth * multiplier * BOSS_HEALTH_MULTIPLIER).toInt()
        } else {
            (baseHealth * multiplier).toInt()
        }
    }
    
    /**
     * 웨이브별 적 데미지 계산
     * @param wave 웨이브 번호
     * @param isBoss 보스 여부
     * @return 적 데미지
     */
    fun getEnemyDamageForWave(wave: Int, isBoss: Boolean): Int {
        val baseDamage = if (isBoss) BOSS_DAMAGE else NORMAL_ENEMY_DAMAGE
        val additionalDamage = (wave - 1) * ENEMY_DAMAGE_PER_WAVE
        return baseDamage + additionalDamage
    }
    
    /**
     * 웨이브에 따른 적 생성 간격 계산 (밀리초)
     * @param wave 현재 웨이브
     * @return 적 생성 간격 (밀리초)
     */
    fun getEnemySpawnIntervalForWave(wave: Int): Long {
        // 웨이브당 100ms씩 생성 간격 감소
        val decreasePerWave = 100L
        // 기본 생성 간격에서 감소 시간 적용
        val interval = BASE_ENEMY_SPAWN_INTERVAL - ((wave - 1) * decreasePerWave)
        return maxOf(interval, MIN_ENEMY_SPAWN_INTERVAL)
    }
    
    /**
     * 웨이브에 따른 적 이동 속도 계산
     * @param wave 현재 웨이브
     * @param isBoss 보스 여부
     * @return 적 이동 속도
     */
    fun getEnemySpeedForWave(wave: Int, isBoss: Boolean = false): Float {
        // 웨이브가 증가할 수록 이동 속도 증가
        val increase = 1 + ((wave - 1) * ENEMY_SPEED_INCREASE_PER_WAVE)
        // 기본 이동 속도에 증가율 적용
        val speed = BASE_ENEMY_SPEED * increase
        
        // 보스인 경우 속도 조정
        return if (isBoss) {
            speed * BOSS_SPEED_MULTIPLIER
        } else {
            speed
        }
    }
    
    // 점수 설정
    const val SCORE_PER_NORMAL_ENEMY = 10  // 일반 적 처치 시 얻는 점수(자원) (10에서 15로 증가)
    const val SCORE_PER_BOSS = 120  // 보스 처치 시 얻는 점수(자원) (100에서 120으로 증가)
    
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
    const val DAMAGE_UPGRADE_COST_INCREASE = 4  // 레벨당 비용 증가량 (5에서 4로 감소)
    const val DAMAGE_UPGRADE_VALUE = 2  // 업그레이드당 데미지 증가량 (1에서 2로 증가)
    const val DAMAGE_UPGRADE_MAX_LEVEL = 40  // 최대 업그레이드 레벨 (50에서 40으로 감소)
    
    // 공격속도 업그레이드 설정
    const val ATTACK_SPEED_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_SPEED_UPGRADE_COST_INCREASE = 6  // 레벨당 비용 증가량 (5에서 6으로 증가)
    const val ATTACK_SPEED_UPGRADE_PERCENT = 0.015f  // 업그레이드당 속도 증가율 (1%에서 1.5%로 증가)
    const val ATTACK_SPEED_UPGRADE_MAX_LEVEL = 25  // 최대 업그레이드 레벨 (30에서 25로 감소)
    
    // 공격범위 업그레이드 설정
    const val ATTACK_RANGE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val ATTACK_RANGE_UPGRADE_COST_INCREASE = 5  // 레벨당 비용 증가량
    const val ATTACK_RANGE_UPGRADE_VALUE = 8f  // 업그레이드당 범위 증가량 (5에서 8로 증가)
    const val ATTACK_RANGE_UPGRADE_MAX_LEVEL = 30  // 최대 업그레이드 레벨 (40에서 30으로 감소)
    
    // 체력 업그레이드 설정
    const val DEFENSE_UPGRADE_INITIAL_COST = 10  // 초기 비용
    const val DEFENSE_UPGRADE_COST_INCREASE = 4  // 레벨당 비용 증가량 (5에서 4로 감소)
    const val DEFENSE_UPGRADE_VALUE = 25  // 업그레이드당 최대 체력 증가량 (20에서 25로 증가)
    const val DEFENSE_UPGRADE_MAX_LEVEL = 40  // 최대 업그레이드 레벨 (50에서 40으로 감소)
    
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
    
    // 디펜스 유닛 버프 효과 설정
    const val MISSILE_DAMAGE_BUFF_PER_LEVEL = 0.1f  // 레벨당 데미지 증가율(10%)
    const val ATTACK_SPEED_BUFF_PER_LEVEL = 0.1f  // 레벨당 공격속도 증가율(10%)
    const val MISSILE_SPEED_BUFF_PER_LEVEL = 0.2f  // 레벨당 미사일 속도 증가율(20%)
    const val MISSILE_RANGE_BUFF_PER_LEVEL = 0.2f  // 레벨당 미사일 사거리 증가율(20%)
    
    // 적 너프 효과 설정
    const val ENEMY_SLOW_BUFF_PER_LEVEL = 0.1f  // 레벨당 적 이동속도 감소율(10%)
    const val DOT_DAMAGE_PER_LEVEL = 1  // 레벨당 초당 데미지
    const val MASS_DAMAGE_PER_LEVEL = 100  // 레벨당 대량 데미지
    
    // --------- 기타 게임 밸런스 설정 ----------
    
    // 적이 중앙에 도달할 때 플레이어 체력 감소량 (이것은 제거될 예정)
    const val ENEMY_CENTER_DAMAGE = 10  // 적이 중앙에 도달했을 때 입히는 데미지
    
    // 보스 처치 보상 설정
    const val BOSS_KILL_DAMAGE_BUFF = 0.05f  // 보스 처치 시 데미지 증가 (5%)
    
    // 특수 이벤트 설정
    const val SPECIAL_BUFF_WAVE_INTERVAL = 3  // 3, 6, 9 웨이브마다 특수 버프 제공
    
    // 자원 획득 설정
    const val RESOURCE_OVER_TIME_INTERVAL = 30000L  // 자원 자동 획득 간격 (밀리초)
    const val RESOURCE_OVER_TIME_AMOUNT = 5  // 자동 획득 자원량
    
    // 난이도 조정
    const val DIFFICULTY_MULTIPLIER_EASY = 0.8f  // 쉬움 난이도 배율 (데미지, 체력)
    const val DIFFICULTY_MULTIPLIER_NORMAL = 1.0f  // 보통 난이도 배율
    const val DIFFICULTY_MULTIPLIER_HARD = 1.3f  // 어려움 난이도 배율

    // 게임 레벨 설정
    private var currentDifficulty: Float = 1.0f   // 기본 난이도
    private var currentTotalWaves: Int = 10       // 기본 총 웨이브 수
    
    // 성능 관련 설정
    const val OFFSCREEN_MARGIN = 300f // 화면 외부 마진 (오브젝트 제거 범위)
    const val OBJECT_POOL_SIZE = 100 // 오브젝트 풀 크기
    
    // --------- 렌더링 관련 설정 ----------
    
    // 적 렌더링 설정
    const val ENEMY_RENDER_MARGIN_X = 200f // 적 렌더링 X축 마진 (화면 밖에서도 그리기 위함)
    const val ENEMY_RENDER_MARGIN_Y = 200f // 적 렌더링 Y축 마진 (화면 밖에서도 그리기 위함)
    
    // 미사일 렌더링 설정
    const val MISSILE_RENDER_MARGIN_X = 20f // 미사일 렌더링 X축 마진
    const val MISSILE_RENDER_MARGIN_Y = 20f // 미사일 렌더링 Y축 마진
    
    // 디버그 정보 설정
    const val DEBUG_TEXT_SIZE = 30f // 디버그 텍스트 크기
    const val DEBUG_TEXT_COLOR = Color.GREEN // 디버그 텍스트 색상
    const val DEBUG_TEXT_MARGIN_X = 10f // 디버그 텍스트 X축 마진
    const val DEBUG_TEXT_SPACING = 30f // 디버그 텍스트 줄 간격
    
    // 게임 로직 설정
    const val FAR_OFFSCREEN_MARGIN = 1000f // 적이 제거되는 화면 외부 거리 (300f에서 1000f로 증가)
    const val ENEMY_SPAWN_DISTANCE_FACTOR = 0.4f // 적 생성 거리 계수 (화면 크기의 비율)
    const val BOSS_SPAWN_DISTANCE_FACTOR = 0.7f // 보스 생성 거리 계수 (화면 크기의 비율)

    /**
     * 게임 난이도 설정
     * @param difficulty 난이도 배율 (1.0 = 기본)
     * @param totalWaves 총 웨이브 수
     */
    fun setGameLevel(difficulty: Float, totalWaves: Int) {
        currentDifficulty = difficulty
        currentTotalWaves = totalWaves
    }

    /**
     * 현재 설정된 난이도 배율 반환
     */
    fun getDifficulty(): Float = currentDifficulty

    /**
     * 현재 설정된 총 웨이브 수 반환
     */
    fun getTotalWaves(): Int = currentTotalWaves

    /**
     * 난이도가 적용된 적 체력 계산
     * @param baseHealth 기본 체력
     * @param waveCount 현재 웨이브
     * @param isBoss 보스 여부
     */
    fun getScaledEnemyHealth(baseHealth: Int, waveCount: Int, isBoss: Boolean): Int {
        // 웨이브가 증가할수록 체력 증가, 난이도 배율 적용
        val waveMultiplier = 1.0f + ((waveCount - 1) * 0.1f)
        return (baseHealth * waveMultiplier * currentDifficulty).toInt()
    }

    /**
     * 난이도가 적용된 적 공격력 계산
     * @param baseDamage 기본 공격력
     * @param waveCount 현재 웨이브
     * @param isBoss 보스 여부
     */
    fun getScaledEnemyDamage(baseDamage: Int, waveCount: Int, isBoss: Boolean): Int {
        // 웨이브가 증가할수록 공격력 증가, 난이도 배율 적용
        val waveMultiplier = 1.0f + ((waveCount - 1) * 0.1f)
        return (baseDamage * waveMultiplier * currentDifficulty).toInt()
    }

    /**
     * 웨이브별 적 생성 간격 반환
     * @param wave 웨이브 번호
     * @return 적 생성 간격 (밀리초)
     */
    fun getEnemySpawnCooldown(wave: Int): Long {
        return getEnemySpawnIntervalForWave(wave)
    }
    
    /**
     * 기본 적 이동 속도 반환 (웨이브 1 기준)
     * @param isBoss 보스 여부
     * @return 기본 적 이동 속도
     */
    fun getEnemySpeed(isBoss: Boolean): Float {
        val baseSpeed = getEnemySpeedForWave(1, isBoss)
        return if (isBoss) {
            baseSpeed * BOSS_SPEED_MULTIPLIER
        } else {
            baseSpeed
        }
    }
} 