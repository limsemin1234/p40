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
    
    // 디펜스 유닛 설정
    const val DEFENSE_UNIT_SIZE = 30f  // 디펜스 유닛 크기
    const val DEFENSE_UNIT_COLOR = Color.BLUE  // 디펜스 유닛 색상
    const val DEFENSE_UNIT_ATTACK_RANGE = 500f  // 공격 범위
    const val DEFENSE_UNIT_ATTACK_COOLDOWN = 1000L  // 공격속도 1초
    
    // 미사일 설정
    const val MISSILE_SIZE = 5f  // 미사일 크기
    const val MISSILE_SPEED = 5f  // 미사일 속도
    const val MISSILE_DAMAGE = 50  // 미사일 기본 데미지
    const val MISSILE_COLOR = Color.YELLOW  // 미사일 색상
    const val MISSILE_MAX_DISTANCE = 2000f  // 미사일 최대 이동 거리
    
    // 일반 적 설정
    const val ENEMY_BASE_SIZE = 10f  // 기본 크기
    const val ENEMY_BASE_HEALTH = 50  // 기본 체력
    const val ENEMY_COLOR = Color.RED  // 적 색상
    
    // 웨이브별 적 체력 배율 (ENEMY_BASE_HEALTH에 곱해짐)
    const val WAVE_1_HEALTH_MULTIPLIER = 1.0f
    const val WAVE_2_HEALTH_MULTIPLIER = 1.2f
    const val WAVE_3_HEALTH_MULTIPLIER = 1.4f
    const val WAVE_4_HEALTH_MULTIPLIER = 1.6f
    const val WAVE_5_HEALTH_MULTIPLIER = 1.8f
    const val WAVE_6_HEALTH_MULTIPLIER = 2.0f
    const val WAVE_7_HEALTH_MULTIPLIER = 2.2f
    const val WAVE_8_HEALTH_MULTIPLIER = 2.4f
    const val WAVE_9_HEALTH_MULTIPLIER = 2.6f
    const val WAVE_10_HEALTH_MULTIPLIER = 2.8f
    
    // 보스 설정
    const val BOSS_SIZE_MULTIPLIER = 2.0f  // 보스 크기 배율
    const val BOSS_HEALTH_MULTIPLIER = 5  // 보스 체력 배율
    const val BOSS_SPEED_MULTIPLIER = 0.7f  // 보스 속도 배율 (일반 적보다 느림)
    const val BOSS_COLOR = Color.MAGENTA  // 보스 색상
    const val BOSS_BORDER_COLOR = Color.YELLOW  // 보스 테두리 색상
    const val BOSS_BORDER_WIDTH = 5f  // 보스 테두리 두께
    
    // 카드 스킬 설정
    const val CARD_DAMAGE_NORMAL = 100  // 일반 적에게 주는 데미지
    const val CARD_DAMAGE_BOSS = 50  // 보스에게 주는 데미지
    const val CARD_COOLDOWN = 10000L  // 카드 사용 쿨다운 (밀리초)
    
    // 웨이브별 적 생성 간격 (밀리초)
    val WAVE_ENEMY_SPAWN_COOLDOWNS = mapOf(
        1 to 3000L,
        2 to 2500L,
        3 to 2000L,
        4 to 1800L,
        5 to 1600L,
        6 to 1400L,
        7 to 1200L,
        8 to 1000L,
        9 to 800L,
        10 to 600L
    )
    
    // 웨이브별 적 이동 속도
    val WAVE_ENEMY_SPEEDS = mapOf(
        1 to 1.0f,
        2 to 1.2f,
        3 to 1.4f,
        4 to 1.6f,
        5 to 1.8f,
        6 to 2.0f,
        7 to 2.2f,
        8 to 2.4f,
        9 to 2.6f,
        10 to 2.8f
    )
    
    // 웨이브별 적 체력 계산 (ENEMY_BASE_HEALTH에 배율을 곱함)
    fun getEnemyHealthForWave(wave: Int): Int {
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
    
    // 점수 설정
    const val SCORE_PER_NORMAL_ENEMY = 10  // 일반 적 처치 시 얻는 점수
    const val SCORE_PER_BOSS = 100  // 보스 처치 시 얻는 점수
    
    // 게임 오버 조건
    const val CENTER_REACHED_DAMAGE = 1000  // 중앙 도달 시 입히는 데미지
    
    // UI 설정
    const val TEXT_SIZE_NORMAL = 48f  // 일반 텍스트 크기
    const val TEXT_SIZE_WAVE = 100f  // 웨이브 텍스트 크기
    const val TEXT_SIZE_PAUSE = 72f  // 일시정지 텍스트 크기
    const val TEXT_COLOR = Color.WHITE  // 텍스트 색상
    const val WAVE_TEXT_COLOR = Color.YELLOW  // 웨이브 텍스트 색상
} 