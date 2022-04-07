package uk.co.conjure.components.auth.forgotpassword

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.*
import uk.co.conjure.components.auth.stateviewmodel.*
import java.util.*
import java.util.concurrent.TimeUnit

open class ForgottenPasswordViewModel(
    private val auth: AuthInteractor,
    private val ui: Scheduler,
    private val io: Scheduler,
    private val validateEmail: ((e: String) -> Boolean)? = null,
    private val initialEmail: String = "",
    private val initialEmailValid: Boolean = false,
    private val onRequestPasswordReset: ((email: String) -> Single<AuthInteractor.RequestPasswordResetResult>)? = null
) : StateViewModelBase<ForgottenPasswordViewModel.State, ForgottenPasswordViewModel.Result, ForgottenPasswordViewModel.Action>(
    ui
) {
    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val sendClicksSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val sendClicks: Observer<Unit> = sendClicksSubject

    override fun getActions(): Observable<Action> = Observable.merge(
        sendClicksSubject.map { Action.ClickSend(this::requestPasswordReset, io) },
        emailSubject.map { Action.EmailChange(it, this::isEmailValid) }
    )

    override fun getDefaultState() = State(
        email = SynchronizedText(initialEmail),
        emailValid = initialEmailValid,
        loading = false,
        error = null,
        success = false
    )

    private fun <T : Any> Observable<T>.distinctUiHot(): Observable<T> {
        return this
            .distinctUntilChanged()
            .observeOn(ui)
            .hot()
    }

    val email: Observable<String> = stateSubject
        .map { it.email.text }
        .debounce(100, TimeUnit.MILLISECONDS, io)
        .distinctUiHot()

    val loading: Observable<Boolean> = stateSubject
        .map { it.loading }
        .distinctUiHot()

    val emailSent: Observable<Boolean> = stateSubject
        .map { it.success }
        .distinctUiHot()

    val emailValid: Observable<Boolean> = stateSubject
        .map { it.emailValid }
        .distinctUiHot()

    val error: Observable<Optional<AuthInteractor.RequestPasswordResetError>> = stateSubject
        .map { Optional.ofNullable(it.error) }
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    open fun requestPasswordReset(email: String): Single<AuthInteractor.RequestPasswordResetResult> {
        return onRequestPasswordReset?.invoke(email) ?: auth.requestPasswordReset(email)
    }

    open fun isEmailValid(email: String) = validateEmail?.invoke(email) ?: auth.isValidEmail(email)

    sealed class Action : ViewModelAction<State, Result> {
        data class ClickSend(
            val requestPasswordReset: ((email: String) -> Single<AuthInteractor.RequestPasswordResetResult>),
            val io: Scheduler
        ) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(1)
                    .observeOn(io)
                    .flatMapSingle { requestPasswordReset(state.email.text) }
                    .map<Result> { Result.ActionComplete(it) }
                    .startWithItem(Result.BeginAction)
            }

            override fun validAction(state: State): Boolean {
                return !state.loading && !state.success
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
                return !state.loading && !state.success
            }
        }
    }

    sealed class Result : ViewModelResult<State> {
        data class UpdateEmail(
            val email: SynchronizedText,
            val valid: Boolean
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    email = email,
                    emailValid = valid,
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.email.updateTime < email.updateTime
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

        data class ActionComplete(val result: AuthInteractor.RequestPasswordResetResult) :
            Result() {
            override fun transformState(state: State): State {
                return when (result) {
                    is AuthInteractor.RequestPasswordResetResult.Success -> state.copy(
                        success = true,
                        loading = false
                    )
                    is AuthInteractor.RequestPasswordResetResult.Failure -> state.copy(
                        loading = false,
                        error = result.error
                    )
                }
            }

            override fun validTransformation(state: State): Boolean {
                return state.loading
            }
        }

    }

    data class State(
        val email: SynchronizedText,
        val emailValid: Boolean,
        val loading: Boolean,
        val error: AuthInteractor.RequestPasswordResetError?,
        val success: Boolean
    ) : ViewModelState
}