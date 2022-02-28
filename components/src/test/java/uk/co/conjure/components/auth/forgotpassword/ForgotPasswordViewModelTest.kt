package uk.co.conjure.components.auth.forgotpassword

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers.trampoline
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.co.conjure.components.auth.AuthInteractor

class ForgotPasswordViewModelTest {

    lateinit var authInteractor: AuthInteractor

    lateinit var io: Scheduler

    lateinit var ui: Scheduler

    lateinit var uut: ForgottenPasswordViewModel

    @Before
    fun setup() {
        authInteractor = mock()
        io = trampoline()
        ui = trampoline()
    }

    @Test
    fun testEmailValid() {
        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        uut = ForgottenPasswordViewModel(authInteractor, ui, io)
        assertFalse(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("some.email@url.com")
        assertTrue(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("")
        assertFalse(uut.emailValid.blockingFirst())
    }

    @Test
    fun testSuccess() {
        val requestPasswordResetSubject =
            PublishSubject.create<AuthInteractor.RequestPasswordResetResult>()

        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.requestPasswordReset(anyString())).thenAnswer {
            requestPasswordResetSubject.firstOrError()
        }
        uut = ForgottenPasswordViewModel(authInteractor, ui, io)
        uut.emailInput.onNext("some.email@url.com")
        uut.sendClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        requestPasswordResetSubject.onNext(AuthInteractor.RequestPasswordResetResult.Success(""))
        assertFalse(uut.loading.blockingFirst())
    }

    @Test
    fun testError() {
        val requestPasswordResetSubject =
            PublishSubject.create<AuthInteractor.RequestPasswordResetResult>()

        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.requestPasswordReset(anyString())).thenAnswer {
            requestPasswordResetSubject.firstOrError()
        }
        uut = ForgottenPasswordViewModel(authInteractor, ui, io)
        uut.emailInput.onNext("some.email@url.com")
        uut.sendClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        requestPasswordResetSubject.onNext(
            AuthInteractor.RequestPasswordResetResult.Failure(
                AuthInteractor.RequestPasswordResetError.ERROR_EMAIL
            )
        )
        assertFalse(uut.loading.blockingFirst())
        assertEquals(
            AuthInteractor.RequestPasswordResetError.ERROR_EMAIL,
            uut.error.blockingFirst().get()
        )
    }

    @Test
    fun testErrorClearedAfterSendAgain() {
        val requestPasswordResetSubject =
            PublishSubject.create<AuthInteractor.RequestPasswordResetResult>()

        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.requestPasswordReset(anyString())).thenAnswer {
            requestPasswordResetSubject.firstOrError()
        }
        uut = ForgottenPasswordViewModel(authInteractor, ui, io)
        uut.emailInput.onNext("some.email@url.com")
        uut.sendClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        requestPasswordResetSubject.onNext(
            AuthInteractor.RequestPasswordResetResult.Failure(
                AuthInteractor.RequestPasswordResetError.ERROR_EMAIL
            )
        )
        assertFalse(uut.loading.blockingFirst())
        assertEquals(
            AuthInteractor.RequestPasswordResetError.ERROR_EMAIL,
            uut.error.blockingFirst().get()
        )
        uut.sendClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        requestPasswordResetSubject.onNext(
            AuthInteractor.RequestPasswordResetResult.Success(null)
        )
        assertEquals(null, uut.error.blockingFirst().orElseGet { null })
        assertFalse(uut.loading.blockingFirst())
    }
}