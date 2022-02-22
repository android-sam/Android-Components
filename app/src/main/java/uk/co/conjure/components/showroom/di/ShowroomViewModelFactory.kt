package uk.co.conjure.components.showroom.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.forgotpassword.ForgottenPasswordViewModel
import uk.co.conjure.components.auth.login.LoginViewModel
import uk.co.conjure.components.auth.signup.SignUpViewModel
import java.lang.IllegalArgumentException

class ShowroomViewModelFactory(val authInteractor: AuthInteractor) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                return LoginViewModel(
                    authInteractor,
                    AndroidSchedulers.mainThread(),
                    Schedulers.io(),
                    Schedulers.computation()
                ) as T
            }
            modelClass.isAssignableFrom(SignUpViewModel::class.java) -> {
                return SignUpViewModel(
                    authInteractor,
                    AndroidSchedulers.mainThread(),
                    Schedulers.io(),
                    Schedulers.computation()
                ) as T
            }
            modelClass.isAssignableFrom(ForgottenPasswordViewModel::class.java) -> {
                return ForgottenPasswordViewModel(
                    authInteractor,
                    AndroidSchedulers.mainThread(),
                    Schedulers.io(),
                    Schedulers.computation()
                ) as T
            }
            else -> throw IllegalArgumentException("ViewModel not supported")
        }
    }
}