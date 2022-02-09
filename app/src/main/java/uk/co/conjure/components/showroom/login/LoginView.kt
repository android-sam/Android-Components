package uk.co.conjure.components.showroom.login

import android.annotation.SuppressLint
import android.view.View
import android.widget.Toast
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.kotlin.addTo
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.login.LoginViewModel
import uk.co.conjure.components.lifecycle.RxView
import uk.co.conjure.components.showroom.R
import uk.co.conjure.components.showroom.databinding.LoginFragmentBinding
import java.util.*

class LoginView(private val viewModel: LoginViewModel) : RxView<LoginFragmentBinding>() {

    private val toErrorText =
        ObservableTransformer<Optional<AuthInteractor.SignInError>, String> { observable ->
            observable.map {
                if (!it.isPresent) return@map ""
                return@map when (it.get()) {
                    AuthInteractor.SignInError.ERROR_USER_NOT_FOUND -> context.getString(R.string.error_user_not_found)
                    AuthInteractor.SignInError.ERROR_INVALID_CREDENTIALS -> context.getString(R.string.error_invalid_password)
                    else -> context.getString(R.string.unknown_error_occurred)
                }
            }
        }

    @SuppressLint("RxSubscribeOnError")
    override fun onStart() {
        super.onStart()

        binding.etEmail.bind(viewModel.emailInput, viewModel.email)
        binding.etPassword.bind(viewModel.passwordInput, viewModel.password)
        binding.btnLogin.bind(viewModel.loginClicks, viewModel.loginButtonEnabled)
        binding.tvForgottenPassword.bindClicks(viewModel.forgottenPasswordClicks)

        viewModel.loading
            .subscribe { binding.pbLoading.visibility = if (it) View.VISIBLE else View.INVISIBLE }
            .addTo(subscriptions)

        viewModel.loginError.compose(toErrorText)
            .subscribe { binding.tvError.text = it }
            .addTo(subscriptions)

        viewModel.loginButtonEnabled.subscribe { enabled ->
            binding.btnLogin.isEnabled = enabled
        }.addTo(subscriptions)

        viewModel.loginComplete.subscribe { _ ->
            Toast.makeText(binding.root.context, "Successful login", Toast.LENGTH_LONG)
                .show()
        }.addTo(subscriptions)
    }
}