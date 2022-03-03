package uk.co.conjure.components.auth.resetpassword

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.ViewModelAction
import uk.co.conjure.components.auth.ViewModelResult
import uk.co.conjure.components.auth.ViewModelState
import uk.co.conjure.components.lifecycle.RxViewModel
import java.util.*

open class ResetPasswordViewModel(
    private val authInteractor: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val initialPassword: String = "",
    private val isPasswordValid: ((String) -> Boolean)? = null
) : RxViewModel() {
    private val passwordSubject: PublishSubject<String> = PublishSubject.create()
    private val confirmPasswordSubject: PublishSubject<String> = PublishSubject.create()
    private val buttonClickSubject: PublishSubject<Unit> = PublishSubject.create()
    private val oobCodeSubject: PublishSubject<String> = PublishSubject.create()
    private val failedToGetLinkSubject: PublishSubject<Unit> = PublishSubject.create()

    val passwordInput: Observer<String> = passwordSubject
    val confirmPasswordInput: Observer<String> = confirmPasswordSubject
    val buttonClicks: Observer<Unit> = buttonClickSubject

    private val currentPassword = passwordSubject.startWithItem(initialPassword)
    private val currentConfirmPassword = confirmPasswordSubject.startWithItem(initialPassword)

    private val actions = Observable.merge(
        listOf(
            oobCodeSubject.map { Action.OobCode(it) },
            failedToGetLinkSubject.map { Action.FailedToGetLink },
            currentPassword
                .withLatestFrom(currentConfirmPassword) { a, b -> Pair(a, b) }
                .map { Action.PasswordChange(it.first, it.second) },
            currentConfirmPassword
                .withLatestFrom(currentPassword) { a, b -> Pair(b, a) }
                .map { Action.ConfirmPasswordChange(it.first, it.second) },
            buttonClickSubject.map { Action.ClickButton(io, authInteractor, this::passwordValid) }
        )
    )

    private val defaultState = State(
        passwordText = initialPassword,
        confirmPasswordText = initialPassword,
        passwordsMatch = true,
        passwordChangeCount = 0,
        error = null,
        failedToGetOob = false,
        loading = true,
        success = false,
        oobCode = null
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

    val password = stateSubject
        .map { it.passwordText }
        .observeOn(ui)
        .hot()

    val confirmPassword = stateSubject
        .map { it.confirmPasswordText }
        .observeOn(ui)
        .hot()

    val passwordsMatch = stateSubject
        .map { it.passwordsMatch }
        .observeOn(ui)
        .hot()

    val loading = stateSubject
        .map { it.loading }
        .observeOn(ui)
        .hot()

    val error = stateSubject
        .map { Optional.ofNullable(it.error) }
        .observeOn(ui)
        .hot()

    val passwordChangeComplete = stateSubject
        .filter { it.success }
        .firstOrError()
        .map { Unit }
        .observeOn(ui)
        .hot()

    val failedToGetOob = stateSubject
        .map { it.failedToGetOob }
        .observeOn(ui)
        .hot()

    private fun passwordValid(password: String): Boolean {
        return isPasswordValid?.invoke(password) ?: authInteractor.isValidPassword(password)
    }

    private sealed class Result : ViewModelResult<State> {
        data class SetOobCode(val oobCode: String) : Result() {
            override fun transformState(state: State): State {
                return state.copy(oobCode = oobCode, failedToGetOob = false, loading = false)
            }

            override fun validTransformation(state: State): Boolean {
                return state.oobCode == null && state.loading
            }
        }

        object FailedToGetLink : Result() {
            override fun transformState(state: State): State {
                return state.copy(failedToGetOob = true, oobCode = null, loading = false)
            }

            override fun validTransformation(state: State): Boolean {
                return state.oobCode == null
            }
        }

        data class UpdateConfirmPassword(
            val password: String,
            val passwordsMatch: Boolean,
            val passwordChangeCount: Long
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    confirmPasswordText = password,
                    passwordsMatch = passwordsMatch,
                    passwordChangeCount = passwordChangeCount
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.passwordChangeCount < passwordChangeCount
            }
        }

        data class UpdatePassword(
            val password: String,
            val passwordsMatch: Boolean,
            val passwordChangeCount: Long
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    passwordText = password,
                    passwordsMatch = passwordsMatch,
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

        data class ActionComplete(val result: AuthInteractor.ResetPasswordResult) : Result() {
            override fun transformState(state: State): State {
                return when (result) {
                    is AuthInteractor.ResetPasswordResult.Success -> state
                        .copy(loading = false, success = true)
                    is AuthInteractor.ResetPasswordResult.Failure -> state
                        .copy(loading = false, error = result.error)
                }
            }

            override fun validTransformation(state: State): Boolean {
                return state.loading
            }
        }
    }

    private sealed class Action : ViewModelAction<State, Result> {

        object FailedToGetLink : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(Result.FailedToGetLink)
            }

            override fun validAction(state: State): Boolean {
                return state.oobCode == null
            }
        }

        data class OobCode(val oobCode: String) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(Result.SetOobCode(oobCode))
            }

            override fun validAction(state: State): Boolean {
                return state.oobCode == null
            }
        }

        data class ClickButton(
            val io: Scheduler,
            val authInteractor: AuthInteractor,
            val passwordValid: (String) -> Boolean
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return if (passwordValid(state.passwordText)) {
                    authInteractor.performPasswordReset(state.oobCode ?: "", state.passwordText)
                        .map<Result> { Result.ActionComplete(it) }
                        .toObservable()
                } else {
                    Observable.just<Result>(
                        Result.ActionComplete(
                            AuthInteractor.ResetPasswordResult
                                .Failure(AuthInteractor.ResetPasswordError.ERROR_INVALID_PASSWORD)
                        )
                    )
                }.startWithItem(Result.BeginAction)
            }

            override fun validAction(state: State): Boolean {
                return !state.loading && state.oobCode != null && state.passwordsMatch
            }
        }

        data class ConfirmPasswordChange(
            val password: String,
            val confirmPassword: String
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                val match = password == confirmPassword
                return Observable.just(
                    Result.UpdateConfirmPassword(
                        confirmPassword,
                        match,
                        state.passwordChangeCount + 1
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return !state.loading && state.oobCode != null
            }
        }

        data class PasswordChange(
            val password: String,
            val confirmPassword: String
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(
                    Result.UpdatePassword(
                        password,
                        password == confirmPassword,
                        state.passwordChangeCount + 1
                    )
                )
            }

            override fun validAction(state: State): Boolean {
                return !state.loading && state.oobCode != null
            }
        }
    }

    private data class State(
        val passwordText: String,
        val confirmPasswordText: String,
        val passwordsMatch: Boolean,
        val passwordChangeCount: Long,
        val error: AuthInteractor.ResetPasswordError?,
        val failedToGetOob: Boolean,
        val loading: Boolean,
        val success: Boolean,
        val oobCode: String?
    ) : ViewModelState

    fun setOoBCode(oobCode: String) = oobCodeSubject.onNext(oobCode)
    fun onFailedToGetLink() = failedToGetLinkSubject.onNext(Unit)
}
