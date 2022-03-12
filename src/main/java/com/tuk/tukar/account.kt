//created by Choi-ji-hoon(Sae_ba) 22/03/13 ver 0.1

package com.tuk.tukar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

//Account create
class Account : AppCompatActivity() {
    private lateinit var edt_email: EditText
    private lateinit var edt_password: EditText
    private lateinit var loginBtn: Button

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account)

        auth = FirebaseAuth.getInstance()

        edt_email = findViewById(R.id.edt_email)
        edt_password = findViewById(R.id.edt_password)
        loginBtn = findViewById(R.id.btn_sign_in)

        loginBtn.setOnClickListener {
            var email = edt_email.text.toString()
            var password = edt_password.text.toString()
            auth.createUserWithEmailAndPassword(email,password) // 회원 가입
                .addOnCompleteListener {
                        result ->
                    if(result.isSuccessful){
                        Toast.makeText(this,"회원가입이 완료되었습니다.",Toast.LENGTH_SHORT).show()
                        if(auth.currentUser!=null){
                            var intent = Intent(this, Mainmenu::class.java)
                            startActivity(intent)
                        }
                    }
                    else if(result.exception?.message.isNullOrEmpty()){
                        Toast.makeText(this,"오류가 발생했습니다.",Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

}