package uk.co.conjure.components.auth.resetpassword

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.stateviewmodel.StateViewModelBase
import uk.co.conjure.components.auth.stateviewmodel.ViewModelAction
import uk.co.conjure.components.auth.stateviewmodel.ViewModelResult
import uk.co.conjure.components.auth.stateviewmodel.ViewModelState
import java.util.*

open class ResetPasswordViewModel(
    private val authInteractor: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val initialPassword: String = "",
    private val isPasswordValid: ((String) -> Boolean)? = null
) : StateViewModelBase<ResetPasswordViewModel.State, ResetPasswordViewModel.Result, ResetPasswordViewModel.Action>(
    ui
) {
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

    override fun getActions(): Observable<Action> = Observable.merge(
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

    override fun getDefaultState() = State(
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

    private fun <T : Any> Observable<T>.distinctUiHot(): Observable<T> {
        return this
            .distinctUntilChanged()
            .observeOn(ui)
            .hot()
    }

    val password = stateSubject
        .map { it.passwordText }
        .distinctUiHot()

    val confirmPassword = stateSubject
        .map { it.confirmPasswordText }
        .distinctUiHot()

    val passwordsMatch = stateSubject
        .map { it.passwordsMatch }
        .distinctUiHot()

    val loading = stateSubject
        .map { it.loading }
        .distinctUiHot()

    val error = stateSubject
        .map { Optional.ofNullable(it.error) }
        .distinctUiHot()

    val passwordChangeComplete: Single<Unit> = stateSubject
        .filter { it.success }
        .firstOrError()
        .map { }

    val failedToGetOob = stateSubject
        .map { it.failedToGetOob }
        .distinctUiHot()

    private fun passwordValid(password: String): Boolean {
        return isPasswordValid?.invoke(password) ?: authInteractor.isValidPassword(password)
    }

    sealed class Result : ViewModelResult<State> {
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

    sealed class Action : ViewModelAction<State, Result> {

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

    data class State(
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
