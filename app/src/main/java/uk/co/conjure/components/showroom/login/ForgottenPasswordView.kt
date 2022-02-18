package uk.co.conjure.components.showroom.login

import android.annotation.SuppressLint
import android.view.View
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.kotlin.addTo
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.forgotpassword.ForgottenPasswordViewModel
import uk.co.conjure.components.lifecycle.RxView
import uk.co.conjure.components.showroom.databinding.ActivityForgottenPasswordBinding
import java.util.*

class ForgottenPasswordView(
    private val viewModel: ForgottenPasswordViewModel
) : RxView<ActivityForgottenPasswordBinding>() {
    private val toErrorText =
        ObservableTransformer<Optional<AuthInteractor.RequestPasswordResetError>, String> { observable ->
            observable.map {
                if (!it.isPresent) return@map ""
                return@map when (it.get()) {
                    //TODO what's the best way to get a context here to localise strings?
                    AuthInteractor.RequestPasswordResetError.ERROR_EMAIL -> "Did not recognise email"
                    AuthInteractor.RequestPasswordResetError.ERROR -> "An unknown error occurred"
                    else -> "Unknown error occurred"
                }
            }
        }

    @SuppressLint("RxSubscribeOnError")
    override fun onStart() {
        super.onStart()
        binding.etEmail.bind(viewModel.emailInput, viewModel.email)
        binding.btnSend.bind(viewModel.sendClicks, viewModel.sendButtonEnabled)
        viewModel.notifyValidEmail.subscribe {
            binding.etEmail.error = if (it) null else "Please enter a valid email"
        }.addTo(subscriptions)
        viewModel.error.compose(toErrorText).subscribe {
            binding.tvError.text = it
        }.addTo(subscriptions)
        viewModel.loading.subscribe {
            binding.pbLoading.visibility = if (it) View.VISIBLE else View.GONE
        }.addTo(subscriptions)
    }
}