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
    val isNew: Boolean = true, // 신규 카드 여부
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
        // 각 문양에 맞는 조커 카드 생성
        // 모든 문양 조커는 isJoker = true로 설정됨
        return when (id) {
            1 -> Card.createHeartJoker(CardRank.JOKER)
            2 -> Card.createSpadeJoker(CardRank.JOKER)
            3 -> Card.createDiamondJoker(CardRank.JOKER)
            4 -> Card.createClubJoker(CardRank.JOKER)
            else -> Card.createJoker()
        }
    }
    
    companion object {
        // 상점에서 판매할 기본 카드 목록 생성
        fun getDefaultShopCards(): List<ShopCard> {
            return listOf(
                ShopCard(
                    id = 1,
                    suit = CardSuit.HEART,
                    name = "하트 조커",
                    description = "모든 하트 숫자로 변환 가능한 특수 카드",
                    price = 500
                ),
                ShopCard(
                    id = 2,
                    suit = CardSuit.SPADE,
                    name = "스페이드 조커",
                    description = "모든 스페이드 숫자로 변환 가능한 특수 카드",
                    price = 500
                ),
                ShopCard(
                    id = 3,
                    suit = CardSuit.DIAMOND,
                    name = "다이아 조커",
                    description = "모든 다이아 숫자로 변환 가능한 특수 카드",
                    price = 500
                ),
                ShopCard(
                    id = 4,
                    suit = CardSuit.CLUB,
                    name = "클로버 조커",
                    description = "모든 클로버 숫자로 변환 가능한 특수 카드",
                    price = 500
                )
            )
        }
    }
} 