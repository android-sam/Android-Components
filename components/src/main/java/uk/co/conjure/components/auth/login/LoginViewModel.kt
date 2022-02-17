package uk.co.conjure.components.auth.login

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.user.UserInfo
import uk.co.conjure.components.lifecycle.RxViewModel
import java.util.*

open class LoginViewModel(
    private val auth: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val computation: Scheduler
) : RxViewModel() {

    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val passwordSubject: PublishSubject<String> = PublishSubject.create()
    private val loginClicksSubject: PublishSubject<Unit> = PublishSubject.create()
    private val forgottenPasswordClicksSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val passwordInput: Observer<String> = passwordSubject
    val loginClicks: Observer<Unit> = loginClicksSubject
    val forgottenPasswordClicks: Observer<Unit> = forgottenPasswordClicksSubject

    val onForgottenPasswordClicked: Observable<Unit> = forgottenPasswordClicksSubject
        .observeOn(ui)

    private val actions = Observable.merge(
        loginClicksSubject.map { Action.ClickLogin(auth, io) },
        emailSubject.map { Action.EmailChange(auth, it) },
        passwordSubject.map { Action.PasswordChange(auth, it) }
    )

    private val defaultState = State.Idle("", "", null, false)
    private val stateSubject = BehaviorSubject.createDefault<State>(defaultState)

    init {
        val currentState = { stateSubject.value ?: defaultState }
        keepAlive.add(actions
            .filter { it.validAction(currentState()) }
            .observeOn(computation)
            .flatMap { it.takeAction(currentState()) }
            .observeOn(ui)
            .map { it.transformState(currentState()) }
            .subscribe({ stateSubject.onNext(it) }, { stateSubject.onNext(defaultState) })
        )
    }

    val email = stateSubject
        .map { it.emailText }
        .observeOn(ui)
        .hot()

    val password = stateSubject
        .map { it.passwordText }
        .observeOn(ui)
        .hot()

    val loginButtonEnabled = stateSubject
        .map { it.loginButtonEnabled }
        .observeOn(ui)
        .hot()

    val loading = stateSubject
        .map { it is State.Loading }
        .observeOn(ui)
        .hot()

    val loginError = stateSubject
        .map { Optional.ofNullable(it.error) }
        .observeOn(ui)
        .hot()

    val loginComplete = stateSubject
        .filter { it is State.Successful }
        .firstOrError()
        .map { Unit }
        .observeOn(ui)
        .hot()

    private sealed class Result {
        data class UpdateEmail(val email: String, val loginButtonEnabled: Boolean) : Result() {
            override fun transformState(state: State): State {
                return State.Idle(email, state.passwordText, state.error, loginButtonEnabled)
            }
        }

        data class UpdatePassword(val password: String, val loginButtonEnabled: Boolean) :
            Result() {
            override fun transformState(state: State): State {
                return State.Idle(state.emailText, password, state.error, loginButtonEnabled)
            }
        }

        object BeginLogin : Result() {
            override fun transformState(state: State): State {
                return State.Loading(state.emailText, state.passwordText)
            }
        }

        data class LoginComplete(val result: AuthInteractor.SignInResult) : Result() {
            override fun transformState(state: State): State {
                return when (result) {
                    is AuthInteractor.SignInResult.Success ->
                        State.Successful(result.userInfo)
                    is AuthInteractor.SignInResult.Failure ->
                        State.Idle(state.emailText, state.passwordText, result.error, true)
                }
            }
        }

        abstract fun transformState(state: State): State
    }

    private sealed class Action {
        data class ClickLogin(val auth: AuthInteractor, val io: Scheduler) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return auth.signIn(state.emailText, state.passwordText)
                    .map<Result> { Result.LoginComplete(it) }
                    .toObservable()
                    .startWithItem(Result.BeginLogin)
                    .subscribeOn(io)
            }

            override fun validAction(state: State): Boolean {
                return auth.isValidLogin(state.emailText, state.passwordText)
            }
        }

        data class EmailChange(val auth: AuthInteractor, val email: String) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(
                    Result.UpdateEmail(
                        email,
                        auth.isValidLogin(email, state.passwordText)
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return state is State.Idle
            }
        }

        data class PasswordChange(val auth: AuthInteractor, val password: String) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(
                    Result.UpdatePassword(
                        password,
                        auth.isValidLogin(state.emailText, password)
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return state is State.Idle
            }
        }

        abstract fun takeAction(state: State): Observable<Result>

        abstract fun validAction(state: State): Boolean
    }

    private sealed class State(
        val emailText: String,
        val passwordText: String,
        val error: AuthInteractor.SignInError?,
        val loginButtonEnabled: Boolean
    ) {
        data class Loading(
            val email: String,
            val password: String
        ) : State(email, password, null, false)

        data class Idle(
            val email: String,
            val password: String,
            val err: AuthInteractor.SignInError?,
            val btnEnabled: Boolean
        ) : State(email, password, err, btnEnabled)

        data class Successful(val userInfo: UserInfo) : State("", "", null, false)
    }
}