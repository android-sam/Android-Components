package uk.co.conjure.components.auth

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.user.UserInfo
import uk.co.conjure.components.lifecycle.RxViewModel
import java.util.*

abstract class LoginSignupViewModelBase(
    private val ui: Scheduler,
    private val io: Scheduler,
    private val computation: Scheduler,
    private val initialEmail: String = "",
    private val initialPassword: String = "",
    private val initialEmailValid: Boolean = false,
    private val initialPasswordValid: Boolean = false
) : RxViewModel() {
    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val passwordSubject: PublishSubject<String> = PublishSubject.create()
    private val buttonClickSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val passwordInput: Observer<String> = passwordSubject
    val buttonClicks: Observer<Unit> = buttonClickSubject

    private val actions = Observable.merge(
        buttonClickSubject.map {
            Action.ClickButton(
                io,
                this::isEmailValid,
                this::isPasswordValid,
                this::takeAction
            )
        },
        emailSubject.map { Action.EmailChange(it, this::isEmailValid) },
        passwordSubject.map { Action.PasswordChange(it, this::isPasswordValid) }
    )

    private val defaultState = State.Idle(
        initialEmail,
        initialPassword,
        initialEmailValid,
        initialPasswordValid,
        null
    )
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

    val buttonEnabled = stateSubject
        .map { it.emailValid && it.passwordValid }
        .observeOn(ui)
        .hot()

    val loading = stateSubject
        .map { it is State.Loading }
        .observeOn(ui)
        .hot()

    protected val errorObject = stateSubject
        .map { Optional.ofNullable(it.error) }
        .observeOn(ui)
        .hot()

    protected val actionComplete = stateSubject
        .filter { it is State.Successful }
        .firstOrError()
        .map { Unit }
        .observeOn(ui)
        .hot()

    private sealed class Result {
        data class UpdateEmail(
            val email: String,
            val emailValid: Boolean
        ) : Result() {
            override fun transformState(state: State): State {
                return State.Idle(
                    email,
                    state.passwordText,
                    emailValid,
                    state.passwordValid,
                    state.error
                )
            }
        }

        data class UpdatePassword(
            val password: String,
            val passwordValid: Boolean
        ) :
            Result() {
            override fun transformState(state: State): State {
                return State.Idle(
                    state.emailText,
                    password,
                    state.emailValid,
                    passwordValid,
                    state.error
                )
            }
        }

        object BeginAction : Result() {
            override fun transformState(state: State): State {
                return State.Loading(state.emailText, state.passwordText)
            }
        }

        data class ActionComplete(val result: ActionResult) : Result() {
            override fun transformState(state: State): State {
                return when {
                    result.userInfo != null ->
                        State.Successful(result.userInfo)
                    result.error != null ->
                        State.Idle(
                            state.emailText, state.passwordText,
                            isEmailValid = true,
                            isPasswordValid = true,
                            err = result.error
                        )
                    else ->
                        State.Idle(
                            state.emailText, state.passwordText,
                            isEmailValid = true,
                            isPasswordValid = true,
                            err = null
                        )
                }
            }
        }

        abstract fun transformState(state: State): State
    }

    protected abstract fun takeAction(email: String, password: String): Observable<ActionResult>

    protected abstract fun isEmailValid(email: String): Boolean

    protected abstract fun isPasswordValid(password: String): Boolean

    protected data class ActionResult(
        val userInfo: UserInfo?,
        val error: Any?
    )

    private sealed class Action {
        data class ClickButton(
            val io: Scheduler,
            val emailValid: (e: String) -> Boolean,
            val passwordValid: (p: String) -> Boolean,
            val takeAction: (e: String, p: String) -> Observable<ActionResult>
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return takeAction(state.emailText, state.passwordText)
                    .map<Result> { Result.ActionComplete(it) }
                    .startWithItem(Result.BeginAction)
                    .subscribeOn(io)
            }

            override fun validAction(state: State): Boolean {
                return emailValid(state.emailText) && passwordValid(state.passwordText)
            }
        }

        data class EmailChange(
            val email: String,
            val emailValid: (e: String) -> Boolean
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                val valid = emailValid(email)
                return Observable.just(Result.UpdateEmail(email, valid))
            }

            override fun validAction(state: State): Boolean {
                return state is State.Idle
            }
        }

        data class PasswordChange(
            val password: String,
            val passwordValid: (p: String) -> Boolean
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                val valid = passwordValid(password)
                return Observable.just(Result.UpdatePassword(password, valid))
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
        val emailValid: Boolean,
        val passwordValid: Boolean,
        val error: Any?
    ) {
        data class Loading(
            val email: String,
            val password: String
        ) : State(email, password, true, true, null)

        data class Idle(
            val email: String,
            val password: String,
            val isEmailValid: Boolean,
            val isPasswordValid: Boolean,
            val err: Any?
        ) : State(email, password, isEmailValid, isPasswordValid, err)

        data class Successful(val userInfo: UserInfo) : State("", "", true, true, null)
    }
}