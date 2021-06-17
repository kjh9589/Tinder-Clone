package com.teamnoyes.tinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.teamnoyes.tinder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()

        // 로그인이 되어 있지 않다면 로그인 창으로 이동동
       if (auth.currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
           startActivity(Intent(this, LikeActivity::class.java))
           finish()
       }
    }
}