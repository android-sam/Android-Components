package uk.co.conjure.components.showroom.signup

import android.annotation.SuppressLint
import android.view.View
import android.widget.Toast
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.kotlin.addTo
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.signup.SignUpViewModel
import uk.co.conjure.components.lifecycle.RxView
import uk.co.conjure.components.showroom.R
import uk.co.conjure.components.showroom.databinding.SignupFragmentBinding
import java.util.*

class SignUpView(private val viewModel: SignUpViewModel) : RxView<SignupFragmentBinding>() {

    private val toErrorText =
        ObservableTransformer<Optional<AuthInteractor.SignUpError>, String> { observable ->
            observable.map {
                if (!it.isPresent) return@map ""
                return@map when (it.get()) {
                    AuthInteractor.SignUpError.ERROR_USER_ALREADY_PRESENT -> context.getString(R.string.error_user_already_exists)
                    AuthInteractor.SignUpError.ERROR_INVALID_EMAIL -> context.getString(R.string.error_invalid_email)
                    AuthInteractor.SignUpError.ERROR_INVALID_PASSWORD -> context.getString(R.string.error_invalid_password)
                    else -> context.getString(R.string.unknown_error_occurred)
                }
            }
        }

    @SuppressLint("RxSubscribeOnError")
    override fun onStart() {
        super.onStart()

        binding.etEmail.bind(viewModel.emailInput, viewModel.email)
        binding.etPassword.bind(viewModel.passwordInput, viewModel.password)
        binding.btnSignUp.bind(viewModel.buttonClicks, viewModel.buttonEnabled)

        viewModel.loading
            .subscribe { binding.pbLoading.visibility = if (it) View.VISIBLE else View.INVISIBLE }
            .addTo(subscriptions)

        viewModel.signUpError.compose(toErrorText)
            .subscribe { binding.tvError.text = it }
            .addTo(subscriptions)

        viewModel.signUpComplete
            .subscribe { _ ->
            Toast.makeText(binding.root.context, "Successfully created account", Toast.LENGTH_LONG)
                .show()
        }.addTo(subscriptions)
    }
}