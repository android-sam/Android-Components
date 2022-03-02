package uk.co.conjure.components.auth.resetpassword

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.co.conjure.components.auth.AuthInteractor

class ResetPasswordViewModelTest {
    lateinit var authInteractor: AuthInteractor

    lateinit var io: Scheduler

    lateinit var ui: Scheduler

    lateinit var uut: ResetPasswordViewModel

    @Before
    fun setup() {
        authInteractor = mock()
        io = Schedulers.trampoline()
        ui = Schedulers.trampoline()
    }

    @Test
    fun testLoadingUntilOoB() {
        uut = ResetPasswordViewModel(authInteractor, ui, io)
        assertTrue(uut.loading.blockingFirst())
        uut.setOoBCode("some code")
        assertFalse(uut.loading.blockingFirst())
    }

    @Test
    fun testFailedOob() {
        uut = ResetPasswordViewModel(authInteractor, ui, io)
        assertTrue(uut.loading.blockingFirst())
        uut.onFailedToGetLink()
        assertFalse(uut.loading.blockingFirst())
        assertTrue(uut.failedToGetOob.blockingFirst())
    }

    @Test
    fun testPasswordsMatch() {
        uut = ResetPasswordViewModel(authInteractor, ui, io)
        assertTrue(uut.loading.blockingFirst())
        uut.setOoBCode("some code")
        assertFalse(uut.loading.blockingFirst())
        assertTrue(uut.passwordsMatch.blockingFirst())
        uut.passwordInput.onNext("some password")
        assertFalse(uut.passwordsMatch.blockingFirst())
        uut.confirmPasswordInput.onNext("some password")
        assertTrue(uut.passwordsMatch.blockingFirst())
        uut.confirmPasswordInput.onNext("some password2")
        assertFalse(uut.passwordsMatch.blockingFirst())
    }

    @Test
    fun testPasswordOutput() {
        uut = ResetPasswordViewModel(authInteractor, ui, io, "sam", null)
        assertTrue(uut.loading.blockingFirst())
        uut.setOoBCode("some code")
        assertFalse(uut.loading.blockingFirst())

        assertEquals("sam", uut.password.blockingFirst())
        assertEquals("sam", uut.confirmPassword.blockingFirst())
        assertTrue(uut.passwordsMatch.blockingFirst())

        uut.passwordInput.onNext("some password")
        assertEquals("some password", uut.password.blockingFirst())
        assertEquals("sam", uut.confirmPassword.blockingFirst())
        assertFalse(uut.passwordsMatch.blockingFirst())

        uut.confirmPasswordInput.onNext("some password2")
        assertEquals("some password", uut.password.blockingFirst())
        assertEquals("some password2", uut.confirmPassword.blockingFirst())
        assertFalse(uut.passwordsMatch.blockingFirst())

        uut.passwordInput.onNext("some password2")
        assertEquals("some password2", uut.password.blockingFirst())
        assertEquals("some password2", uut.confirmPassword.blockingFirst())
        assertTrue(uut.passwordsMatch.blockingFirst())
    }

    @Test
    fun testResetPasswordFail() {
        val resultSubject = PublishSubject.create<AuthInteractor.ResetPasswordResult>()
        whenever(authInteractor.performPasswordReset(anyString(), anyString())).thenAnswer {
            return@thenAnswer resultSubject.firstOrError()
        }
        uut = ResetPasswordViewModel(authInteractor, ui, io, "") { it.isNotBlank() }
        assertTrue(uut.loading.blockingFirst())
        uut.setOoBCode("some code")
        assertFalse(uut.loading.blockingFirst())

        uut.passwordInput.onNext("some password")
        uut.confirmPasswordInput.onNext("some password")

        assertFalse(uut.error.blockingFirst().isPresent)
        uut.buttonClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        resultSubject.onNext(AuthInteractor.ResetPasswordResult.Failure(AuthInteractor.ResetPasswordError.ERROR_CODE_EXPIRED))
        assertTrue(uut.error.blockingFirst().isPresent)
        assertEquals(AuthInteractor.ResetPasswordError.ERROR_CODE_EXPIRED, uut.error.blockingFirst().get())

        uut.buttonClicks.onNext(Unit)
        assertFalse(uut.error.blockingFirst().isPresent)
        assertTrue(uut.loading.blockingFirst())

        verify(authInteractor, times(2)).performPasswordReset("some code", "some password")
    }

    @Test
    fun testResetPasswordSuccess() {
        val resultSubject = PublishSubject.create<AuthInteractor.ResetPasswordResult>()
        whenever(authInteractor.performPasswordReset(anyString(), anyString())).thenAnswer {
            return@thenAnswer resultSubject.firstOrError()
        }
        uut = ResetPasswordViewModel(authInteractor, ui, io, "") { it.isNotBlank() }
        assertTrue(uut.loading.blockingFirst())
        uut.setOoBCode("some code")
        assertFalse(uut.loading.blockingFirst())

        uut.passwordInput.onNext("some password")
        uut.confirmPasswordInput.onNext("some password")

        assertFalse(uut.error.blockingFirst().isPresent)
        uut.buttonClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        resultSubject.onNext(AuthInteractor.ResetPasswordResult.Success)
        assertFalse(uut.error.blockingFirst().isPresent)
        assertTrue(uut.passwordChangeComplete.toObservable().map { true }.blockingFirst())

        verify(authInteractor, times(1)).performPasswordReset("some code", "some password")
    }
}