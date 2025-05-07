package com.example.p40

import android.content.Context
import android.content.SharedPreferences
import com.example.p40.game.Card
import com.example.p40.game.CardRank
import com.example.p40.game.CardSuit
import com.example.p40.game.GameConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 유저 정보 관리 클래스 (싱글톤)
 */
class UserManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 유저의 현재 코인
    fun getCoin(): Int {
        return prefs.getInt(KEY_COIN, DEFAULT_COIN)
    }
    
    // 코인 추가
    fun addCoin(amount: Int) {
        val currentCoin = getCoin()
        prefs.edit().putInt(KEY_COIN, currentCoin + amount).apply()
    }
    
    // 코인 감소
    fun decreaseCoin(amount: Int) {
        val currentCoin = getCoin()
        val newAmount = if (currentCoin >= amount) currentCoin - amount else 0
        prefs.edit().putInt(KEY_COIN, newAmount).apply()
    }
    
    // 코인 사용
    fun useCoin(amount: Int): Boolean {
        val currentCoin = getCoin()
        if (currentCoin >= amount) {
            prefs.edit().putInt(KEY_COIN, currentCoin - amount).apply()
            return true
        }
        return false
    }
    
    // 구매한 카드 목록 가져오기
    fun getPurchasedCards(): List<Card> {
        val json = prefs.getString(KEY_PURCHASED_CARDS, "[]")
        val type = object : TypeToken<List<PurchasedCardInfo>>() {}.type
        val cardInfoList: List<PurchasedCardInfo> = gson.fromJson(json, type)
        
        return cardInfoList.map { cardInfo ->
            // 카드 정보로부터 실제 Card 객체 생성
            val suit = CardSuit.valueOf(cardInfo.suit)
            val rank = CardRank.valueOf(cardInfo.rank)
            Card(suit, rank, isJoker = cardInfo.isJoker)
        }
    }
    
    // 카드 구매 기록
    fun addPurchasedCard(card: Card) {
        val currentCards = getPurchasedCardInfoList()
        val cardInfo = PurchasedCardInfo(
            suit = card.suit.name,
            rank = card.rank.name,
            isJoker = card.isJoker,
            purchaseDate = System.currentTimeMillis()
        )
        
        // 이미 구매한 카드인지 확인
        val exists = currentCards.any { 
            it.suit == cardInfo.suit && 
            it.rank == cardInfo.rank && 
            it.isJoker == cardInfo.isJoker 
        }
        
        if (!exists) {
            val updatedCards = currentCards + cardInfo
            val json = gson.toJson(updatedCards)
            prefs.edit().putString(KEY_PURCHASED_CARDS, json).apply()
        }
    }
    
    // 최근 구매한 카드 목록 (7일 이내)
    fun getRecentPurchasedCards(): List<Card> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val cardInfoList = getPurchasedCardInfoList()
        
        val recentCardInfoList = cardInfoList.filter { it.purchaseDate >= sevenDaysAgo }
        
        return recentCardInfoList.map { cardInfo ->
            val suit = CardSuit.valueOf(cardInfo.suit)
            val rank = CardRank.valueOf(cardInfo.rank)
            Card(suit, rank, isJoker = cardInfo.isJoker)
        }
    }
    
    // 특정 카드 구매 여부 확인
    fun hasCard(cardSuit: CardSuit, isJoker: Boolean): Boolean {
        val cardInfoList = getPurchasedCardInfoList()
        return cardInfoList.any { it.suit == cardSuit.name && it.isJoker == isJoker }
    }
    
    // 구매 카드 정보 목록 가져오기
    private fun getPurchasedCardInfoList(): List<PurchasedCardInfo> {
        val json = prefs.getString(KEY_PURCHASED_CARDS, "[]")
        val type = object : TypeToken<List<PurchasedCardInfo>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // 데이터 초기화 (테스트용)
    fun resetData() {
        prefs.edit()
            .putInt(KEY_COIN, DEFAULT_COIN)
            .putString(KEY_PURCHASED_CARDS, "[]")
            .apply()
    }
    
    companion object {
        private const val PREFS_NAME = "user_data"
        private const val KEY_COIN = "user_coin"
        private const val KEY_PURCHASED_CARDS = "user_purchased_cards"
        private val DEFAULT_COIN = GameConfig.INITIAL_COIN // GameConfig에서 초기 코인 값 가져오기
        
        @Volatile
        private var instance: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    // 구매한 카드 정보 저장 데이터 클래스
    private data class PurchasedCardInfo(
        val suit: String,
        val rank: String,
        val isJoker: Boolean,
        val purchaseDate: Long
    )
} 