package uk.co.conjure.components.auth.signup

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.LoginSignupViewModelBase
import java.util.*

open class SignUpViewModel(
    private val auth: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val computation: Scheduler,
    private val validateEmail: ((e: String) -> Boolean)? = null,
    private val validatePassword: ((p: String) -> Boolean)? = null
) : LoginSignupViewModelBase(
    ui, io, computation, "", "", false, false
) {

    val signUpError: Observable<Optional<AuthInteractor.SignUpError>> = errorObject
        .map {
            if (!it.isPresent) Optional.empty<AuthInteractor.SignUpError>()
            else {
                val err = it.get()
                if (err is AuthInteractor.SignUpError) Optional.of(err)
                else Optional.of(AuthInteractor.SignUpError.ERROR)//This shouldn't be possible but good to have a fallback
            }
        }

    val signUpComplete = actionComplete

    override fun takeAction(email: String, password: String): Observable<ActionResult> {
        return auth.signUp(email, password)
            .map {
                when (it) {
                    is AuthInteractor.SignUpResult.Success -> ActionResult(it.userInfo, null)
                    is AuthInteractor.SignUpResult.Failure -> ActionResult(null, it.error)
                }
            }
            .toObservable()
    }

    override fun isEmailValid(email: String): Boolean {
        return validateEmail?.invoke(email) ?: auth.isValidEmail(email)
    }

    override fun isPasswordValid(password: String): Boolean {
        return validatePassword?.invoke(password) ?: auth.isValidPassword(password)
    }
}