//created by Choi-ji-hoon(Sae_ba) 22/03/13 ver 0.1

package com.tuk.tukar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tuk.tukar.databinding.ActivityMenuBinding

class Mainmenu : AppCompatActivity() {
    private var mbinding: ActivityMenuBinding? = null
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button4.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        binding.button5.setOnClickListener {
            val intent = Intent(this, Account::class.java)
            startActivity(intent)
        }
        binding.button6.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}