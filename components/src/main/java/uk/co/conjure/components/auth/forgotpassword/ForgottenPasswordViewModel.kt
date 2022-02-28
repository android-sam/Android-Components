package uk.co.conjure.components.auth.forgotpassword

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.*
import uk.co.conjure.components.lifecycle.RxViewModel
import java.util.*

open class ForgottenPasswordViewModel(
    private val auth: AuthInteractor,
    ui: Scheduler,
    io: Scheduler,
    private val validateEmail: ((e: String) -> Boolean)? = null,
    initialEmail: String = "",
    initialEmailValid: Boolean = false
) : RxViewModel() {
    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val sendClicksSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val sendClicks: Observer<Unit> = sendClicksSubject

    private val actions = Observable.merge(
        sendClicksSubject.map { Action.ClickSend(auth, io) },
        emailSubject.map { Action.EmailChange(it, this::isEmailValid) }
    )

    protected val defaultState = State(
        emailText = initialEmail,
        emailValid = initialEmailValid,
        loading = false,
        emailChanges = 0,
        error = null,
        success = false
    )
    protected val stateSubject = BehaviorSubject.createDefault(defaultState)

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

    val email: Observable<String> = stateSubject
        .map { it.emailText }
        .observeOn(ui)
        .hot()

    val loading: Observable<Boolean> = stateSubject
        .map { it.loading }
        .observeOn(ui)
        .hot()

    val emailSent: Observable<Boolean> = stateSubject
        .map { it.success }
        .observeOn(ui)
        .hot()

    val emailValid: Observable<Boolean> = stateSubject
        .map { it.emailValid }
        .observeOn(ui)
        .hot()

    val error: Observable<Optional<AuthInteractor.RequestPasswordResetError>> = stateSubject
        .map { Optional.ofNullable(it.error) }
        .observeOn(ui)
        .hot()

    fun isEmailValid(email: String) = validateEmail?.invoke(email) ?: auth.isValidEmail(email)

    protected sealed class Action : ViewModelAction<State, Result> {
        data class ClickSend(val auth: AuthInteractor, val io: Scheduler) : Action() {
            override fun takeAction(state: State): Observable<Result> {
                return Observable.just(1)
                    .observeOn(io)
                    .flatMapSingle { auth.requestPasswordReset(state.emailText) }
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
                return Observable.just(Result.UpdateEmail(email, state.emailChanges + 1, valid))
            }

            override fun validAction(state: State): Boolean {
                return !state.loading && !state.success
            }
        }
    }

    protected sealed class Result : ViewModelResult<State> {
        data class UpdateEmail(
            val email: String,
            val emailChanges: Long,
            val valid: Boolean
        ) : Result() {
            override fun transformState(state: State): State {
                return state.copy(
                    emailText = email,
                    emailValid = valid,
                    emailChanges = emailChanges
                )
            }

            override fun validTransformation(state: State): Boolean {
                return state.emailChanges < emailChanges
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

    protected data class State(
        val emailText: String,
        val emailValid: Boolean,
        val loading: Boolean,
        val emailChanges: Long,
        val error: AuthInteractor.RequestPasswordResetError?,
        val success: Boolean
    ) : ViewModelState
}