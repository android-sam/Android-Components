package uk.co.conjure.components.auth.forgotpassword

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.lifecycle.RxViewModel
import java.util.*
import java.util.concurrent.TimeUnit

open class ForgottenPasswordViewModel(
    private val auth: AuthInteractor,
    ui: Scheduler,
    io: Scheduler,
    computation: Scheduler
) : RxViewModel() {
    private val emailSubject: PublishSubject<String> = PublishSubject.create()
    private val sendClicksSubject: PublishSubject<Unit> = PublishSubject.create()

    val emailInput: Observer<String> = emailSubject
    val sendClicks: Observer<Unit> = sendClicksSubject

    val email = emailSubject.distinctUntilChanged().hot()

    private val sendEmailResults =
        sendClicksSubject.withLatestFrom(emailSubject, { _, email -> email })
            .observeOn(io)
            .switchMapSingle { auth.requestPasswordReset(it) }
            .share()

    val loading: Observable<Boolean> = sendClicksSubject
        .switchMap {
            sendEmailResults
                .take(1)
                .map { false }
                .startWithItem(true)
        }.startWithItem(false)
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    private val emailValid: Observable<Boolean> = emailSubject
        .map { auth.isValidEmail(it) }
        .share()

    val notifyValidEmail: Observable<Boolean> = emailValid
        .switchMap {
            //Notify the user after 2 seconds if the email they entered is invalid
            if (it) Observable.just(true)
            else Observable.timer(2, TimeUnit.SECONDS, computation).take(1).map { false }
        }
        .observeOn(ui)
        .hot()

    val sendButtonEnabled: Observable<Boolean> = Observable
        .combineLatest(loading, emailValid, { loading, valid -> !loading && valid })
        .startWithItem(false)
        .distinctUntilChanged()
        .observeOn(ui)
        .hot()

    val emailSent: Observable<Boolean> = sendEmailResults
        .filter { it is AuthInteractor.RequestPasswordResetResult.Success }
        .map { true }
        .startWithItem(false)
        .distinctUntilChanged()
        .hot()

    val error: Observable<Optional<AuthInteractor.RequestPasswordResetError>> = Observable
        .merge(
            sendEmailResults
                .filter { it is AuthInteractor.RequestPasswordResetResult.Failure }
                .map { Optional.of((it as AuthInteractor.RequestPasswordResetResult.Failure).error) },
            loading
                .filter { it }
                .map { Optional.empty<AuthInteractor.RequestPasswordResetError>() }
        )
        .observeOn(ui)
        .hot()
}