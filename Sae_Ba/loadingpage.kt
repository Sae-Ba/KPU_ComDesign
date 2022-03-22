//created by Choi-ji-hoon(Sae_ba) 22/03/13 ver 0.1


package com.tuk.tukar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper

class Loadingpage : AppCompatActivity() {
    private val SPLASH_TIME_OUT:Long = 3000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loadingpage)
        Handler(Looper.getMainLooper()).postDelayed({

            startActivity(Intent(this, Mainmenu::class.java))

            finish()
        }, SPLASH_TIME_OUT)
    }
}