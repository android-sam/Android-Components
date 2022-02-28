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

    private val defaultState = State(
        emailText = initialEmail,
        passwordText = initialPassword,
        emailValid = initialEmailValid,
        passwordValid = initialPasswordValid,
        error = null,
        loading = false,
        success = false,
        userInfo = null,
        emailChangeCount = 0,
        passwordChangeCount = 0
    )
    private val stateSubject = BehaviorSubject.createDefault(defaultState)

    init {
        val currentState = { stateSubject.value ?: defaultState }
        keepAlive.add(actions
            .filter { it.validAction(currentState()) }
            .flatMap { it.takeAction(currentState()) }
            .observeOn(ui)
            .map { Pair(it, currentState()) }
            .filter { it.first.validTransformation(it.second) }
            .map { it.first.transformState(it.second) }
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

    val emailValid = stateSubject
        .map { it.emailValid }
        .observeOn(ui)
        .hot()

    val passwordValid = stateSubject
        .map { it.passwordValid }
        .observeOn(ui)
        .hot()

    val buttonEnabled = stateSubject
        .map { it.emailValid && it.passwordValid }
        .observeOn(ui)
        .hot()

    val loading = stateSubject
        .map { it.loading }
        .observeOn(ui)
        .hot()

    protected val errorObject = stateSubject
        .map { Optional.ofNullable(it.error) }
        .observeOn(ui)
        .hot()

    protected val actionComplete = stateSubject
        .filter { it.success }
        .firstOrError()
        .map { Unit }
        .observeOn(ui)
        .hot()

    private sealed class Result : ViewModelResult<State> {
        data class UpdateEmail(
            val email: String,
            val emailValid: Boolean,
            val emailChangeCount: Long
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(emailText = email, emailValid = emailValid)
            }

            override fun validTransformation(state: State): Boolean {
                return state.emailChangeCount < emailChangeCount
            }
        }

        data class UpdatePassword(
            val password: String,
            val passwordValid: Boolean,
            val passwordChangeCount: Long
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    passwordText = password,
                    passwordValid = passwordValid,
                    passwordChangeCount = passwordChangeCount
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.passwordChangeCount < passwordChangeCount
            }
        }

        object BeginAction : Result() {
            override fun transformState(state: State): State {
                return state.copy(loading = true, error = null)
            }

            override fun validTransformation(state: State): Boolean {
                return !state.loading
            }
        }

        data class ActionComplete(val result: ActionResult) : Result() {
            override fun transformState(state: State): State {
                return when {
                    result.userInfo != null -> state.copy(
                        success = true,
                        userInfo = result.userInfo,
                        loading = false
                    )
                    result.error != null -> state.copy(
                        error = result.error,
                        loading = false
                    )
                    else -> state.copy()
                }
            }

            override fun validTransformation(state: State): Boolean {
                return state.loading
            }
        }
    }

    protected abstract fun takeAction(email: String, password: String): Observable<ActionResult>

    protected abstract fun isEmailValid(email: String): Boolean

    protected abstract fun isPasswordValid(password: String): Boolean

    protected data class ActionResult(
        val userInfo: UserInfo?,
        val error: Any?
    )

    private sealed class Action : ViewModelAction<State, Result> {
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
                return Observable.just(
                    Result.UpdateEmail(
                        email,
                        valid,
                        state.emailChangeCount + 1
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return !state.loading
            }
        }

        data class PasswordChange(
            val password: String,
            val passwordValid: (p: String) -> Boolean
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                val valid = passwordValid(password)
                return Observable.just(
                    Result.UpdatePassword(
                        password,
                        valid,
                        state.passwordChangeCount + 1
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return !state.loading
            }
        }
    }

    private data class State(
        val emailText: String,
        val passwordText: String,
        val emailValid: Boolean,
        val passwordValid: Boolean,
        val error: Any?,
        val loading: Boolean,
        val success: Boolean,
        val userInfo: UserInfo?,
        val emailChangeCount: Long,
        val passwordChangeCount: Long
    ) : ViewModelState
}