package uk.co.conjure.components.showroom.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uk.co.conjure.components.auth.forgotpassword.ForgottenPasswordViewModel
import uk.co.conjure.components.lifecycle.whileStarted
import uk.co.conjure.components.showroom.ShowroomApp
import uk.co.conjure.components.showroom.databinding.ActivityForgottenPasswordBinding

class ForgottenPasswordActivity : AppCompatActivity() {

    private lateinit var view : ForgottenPasswordView
    private lateinit var viewModel : ForgottenPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.viewModel = ViewModelProvider(
            this,
            (applicationContext as ShowroomApp).viewModelFactory
        )[ForgottenPasswordViewModel::class.java]

        view = ForgottenPasswordView(viewModel)
        view.registerBinding(ActivityForgottenPasswordBinding.inflate(layoutInflater), this)
        setContentView(view.requireBinding().root)
    }

    override fun onStart() {
        super.onStart()
        viewModel.emailSent.filter { it }.whileStarted(this, { finish() })
    }
}