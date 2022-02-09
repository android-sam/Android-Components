package uk.co.conjure.components.showroom.login

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import uk.co.conjure.components.auth.login.LoginViewModel
import uk.co.conjure.components.lifecycle.whileStarted
import uk.co.conjure.components.showroom.R
import uk.co.conjure.components.showroom.ShowroomApp

class LoginActivity : AppCompatActivity() {
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        this.loginViewModel = ViewModelProvider(
            this,
            (applicationContext as ShowroomApp).viewModelFactory
        )[LoginViewModel::class.java]

        if (savedInstanceState == null) {
            loginViewModel.emailInput.onNext("simon.osim@")
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()
        loginViewModel.onForgottenPasswordClicked
            .whileStarted(this, { onForgottenPasswordClicked() })
    }

    private fun onForgottenPasswordClicked() {
        startActivity(Intent(this, ForgottenPasswordActivity::class.java))
    }
}