package uk.co.conjure.components.showroom.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding4.view.clicks
import uk.co.conjure.components.lifecycle.whileStarted
import uk.co.conjure.components.showroom.databinding.ActivityHomeBinding
import uk.co.conjure.components.showroom.login.LoginActivity
import uk.co.conjure.components.showroom.signup.SignUpActivity

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        binding.btnLogin.clicks().whileStarted(this, {
            startActivity(Intent(this, LoginActivity::class.java))
        })
        binding.btnSignUp.clicks().whileStarted(this, {
            startActivity(Intent(this, SignUpActivity::class.java))
        })
    }

}