package com.tuk.tukar

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tuk.tukar.databinding.ActivityNonMemberMenuBinding

class Non_member_menu : AppCompatActivity() {
    private lateinit var binding: ActivityNonMemberMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNonMemberMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button10.setOnClickListener {
            val intent = Intent(this, Arnavigation_search::class.java)
            startActivity(intent)
        }
    }
}