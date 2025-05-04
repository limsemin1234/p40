package com.example.p40

/**
 * 게임 레벨 정보를 담는 데이터 클래스
 */
data class GameLevel(
    val id: Int,             // 레벨 고유 ID
    val number: Int,         // 레벨 번호
    val title: String,       // 레벨 제목
    val description: String, // 레벨 설명
    val totalWaves: Int,     // 총 웨이브 수
    val difficulty: Float    // 난이도 배율 (적 체력, 데미지 등에 곱해짐)
) 