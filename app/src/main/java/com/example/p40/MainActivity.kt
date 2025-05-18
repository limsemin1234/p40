package com.example.p40

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NavHostFragment 설정
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 애플리케이션 종료 시 MessageManager 리소스 정리
        try {
            MessageManager.getInstance().clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
