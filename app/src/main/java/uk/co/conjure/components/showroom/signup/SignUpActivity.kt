package uk.co.conjure.components.showroom.signup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uk.co.conjure.components.auth.signup.SignUpViewModel
import uk.co.conjure.components.showroom.R
import uk.co.conjure.components.showroom.ShowroomApp

class SignUpActivity : AppCompatActivity() {

    private lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        this.viewModel = ViewModelProvider(
            this,
            (applicationContext as ShowroomApp).viewModelFactory
        )[SignUpViewModel::class.java]

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
    }
}