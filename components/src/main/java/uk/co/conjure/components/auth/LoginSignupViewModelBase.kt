package uk.co.conjure.components.auth

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.stateviewmodel.*
import uk.co.conjure.components.auth.user.UserInfo
import java.util.*
import java.util.concurrent.TimeUnit

abstract class LoginSignupViewModelBase(
    private val ui: Scheduler,
    private val io: Scheduler,
    private val computation: Scheduler,
    private val initialEmail: String = "",
    private val initialPassword: String = "",
    private val initialEmailValid: Boolean = false,
    private val initialPasswordValid: Boolean = false
) : StateViewModelBase<LoginSignupViewModelBase.State, LoginSignupViewModelBase.Result, LoginSignupViewModelBase.Action>(
    ui
) {

    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val passwordSubject: PublishSubject<String> = PublishSubject.create()
    private val buttonClickSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val passwordInput: Observer<String> = passwordSubject
    val buttonClicks: Observer<Unit> = buttonClickSubject

    override fun getActions() = Observable.merge(
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

    override fun getDefaultState() = State(
        email = SynchronizedText(initialEmail),
        password = SynchronizedText(initialPassword),
        emailValid = initialEmailValid,
        passwordValid = initialPasswordValid,
        error = null,
        loading = false,
        success = false,
        userInfo = null
    )

    val email: Observable<String> = stateSubject
        .map { it.email.text }
        //when we produce an output we should try and wait until the input has stopped emitting
        .debounce(100, TimeUnit.MILLISECONDS, computation)
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val password: Observable<String> = stateSubject
        .map { it.password.text }
        //when we produce an output we should try and wait until the input has stopped emitting
        .debounce(100, TimeUnit.MILLISECONDS, computation)
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val emailValid: Observable<Boolean> = stateSubject
        .map { it.emailValid }
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val passwordValid: Observable<Boolean> = stateSubject
        .map { it.passwordValid }
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val buttonEnabled: Observable<Boolean> = stateSubject
        .map { it.emailValid && it.passwordValid }
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val loading: Observable<Boolean> = stateSubject
        .map { it.loading }
        .distinctUntilChanged()
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

    sealed class Result : ViewModelResult<State> {
        data class UpdateEmail(
            val email: SynchronizedText,
            val emailValid: Boolean,
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    email = email,
                    emailValid = emailValid
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.email.updateTime < email.updateTime
            }
        }

        data class UpdatePassword(
            val password: SynchronizedText,
            val passwordValid: Boolean,
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    password = password,
                    passwordValid = passwordValid,
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.password.updateTime < password.updateTime
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

    data class ActionResult(
        val userInfo: UserInfo?,
        val error: Any?
    )

    sealed class Action : ViewModelAction<State, Result> {
        data class ClickButton(
            val io: Scheduler,
            val emailValid: (e: String) -> Boolean,
            val passwordValid: (p: String) -> Boolean,
            val takeAction: (e: String, p: String) -> Observable<ActionResult>
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return takeAction(state.email.text, state.password.text)
                    .map<Result> { Result.ActionComplete(it) }
                    .startWithItem(Result.BeginAction)
                    .subscribeOn(io)
            }

            override fun validAction(state: State): Boolean {
                return emailValid(state.email.text) && passwordValid(state.password.text)
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
                        SynchronizedText(email, System.nanoTime()),
                        valid
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
                        SynchronizedText(password, System.nanoTime()),
                        valid
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return !state.loading
            }
        }
    }

    data class State(
        val email: SynchronizedText,
        val password: SynchronizedText,
        val emailValid: Boolean,
        val passwordValid: Boolean,
        val error: Any?,
        val loading: Boolean,
        val success: Boolean,
        val userInfo: UserInfo?,
    ) : ViewModelState
}