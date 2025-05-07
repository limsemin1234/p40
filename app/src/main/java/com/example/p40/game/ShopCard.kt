package com.example.p40.game

/**
 * 상점에서 판매될 카드 정보
 */
data class ShopCard(
    val id: Int,               // 카드 고유 ID
    val suit: CardSuit,        // 카드 문양
    val name: String,          // 카드 이름
    val description: String,   // 카드 설명
    val price: Int,            // 카드 가격
    val isNew: Boolean = false, // 신규 카드 여부 (false로 변경)
    var isPurchased: Boolean = false // 구매 여부
) {
    // 카드 문양 심볼 반환 - CardUtils 활용
    fun getSuitSymbol(): String {
        // 조커의 경우 별 모양 사용
        return if (suit == CardSuit.JOKER) {
            "⭐"
        } else {
            CardUtils.getSuitSymbol(suit)
        }
    }
    
    // 카드 문양 색상 반환 - CardUtils 활용
    fun getSuitColor(): Int {
        return CardUtils.getSuitColor(suit)
    }
    
    // 실제 카드 객체로 변환 (구매 시 덱에 추가될 카드)
    fun toCard(): Card {
        // 문양 조커 대신 별 조커만 사용
        return Card.createJoker()
    }
    
    companion object {
        // 상점에서 판매할 기본 카드 목록 생성
        fun getDefaultShopCards(): List<ShopCard> {
            return listOf(
                ShopCard(
                    id = 1,
                    suit = CardSuit.JOKER,
                    name = "별 조커",
                    description = "모든 카드로 변환 가능한 특수 카드",
                    price = 500,
                    isNew = false
                )
            )
        }
    }
} 