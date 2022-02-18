package uk.co.conjure.components.auth.login

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.LoginSignupViewModelBase
import java.util.*

open class LoginViewModel(
    private val auth: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val computation: Scheduler,
    private val validateEmail: ((e: String) -> Boolean)? = null,
    private val validatePassword: ((p: String) -> Boolean)? = null
) : LoginSignupViewModelBase(
    ui, io, computation, "", "", false, false
) {
    private val forgottenPasswordClicksSubject: PublishSubject<Unit> = PublishSubject.create()

    val forgottenPasswordClicks: Observer<Unit> = forgottenPasswordClicksSubject

    val onForgottenPasswordClicked: Observable<Unit> = forgottenPasswordClicksSubject.observeOn(ui)

    val loginError: Observable<Optional<AuthInteractor.SignInError>> = errorObject
        .map {
            if (!it.isPresent) Optional.empty<AuthInteractor.SignInError>()
            else {
                val err = it.get()
                if (err is AuthInteractor.SignInError) Optional.of(err)
                else Optional.of(AuthInteractor.SignInError.ERROR)//This shouldn't be possible but good to have a fallback
            }
        }

    val loginComplete = actionComplete

    override fun takeAction(email: String, password: String): Observable<ActionResult> {
        return auth.signIn(email, password)
            .map {
                when (it) {
                    is AuthInteractor.SignInResult.Success -> ActionResult(it.userInfo, null)
                    is AuthInteractor.SignInResult.Failure -> ActionResult(null, it.error)
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