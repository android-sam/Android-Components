package uk.co.conjure.components.auth.login

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers.trampoline
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.user.UserInfo

class LoginViewModelTest {

    lateinit var authInteractor: AuthInteractor

    lateinit var io: Scheduler

    lateinit var ui: Scheduler

    lateinit var computation: Scheduler

    lateinit var uut: LoginViewModel

    @Before
    fun setup() {
        authInteractor = mock()
        io = trampoline()
        ui = trampoline()
        computation = trampoline()
    }

    @Test
    fun testButtonEnabled() {
        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        uut = LoginViewModel(authInteractor, ui, io, computation)
        assertFalse(uut.buttonEnabled.blockingFirst())
        uut.emailInput.onNext("some.email@url.com")
        assertFalse(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("somepassword")
        assertTrue(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("")
        assertFalse(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("i")
        assertTrue(uut.buttonEnabled.blockingFirst())
        uut.emailInput.onNext("")
        assertFalse(uut.buttonEnabled.blockingFirst())
    }

    @Test
    fun testEmailValid() {
        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            val arg = it.getArgument<String>(0)
            return@thenAnswer arg.isNotBlank() && arg.contains("hi")
        }
        uut = LoginViewModel(authInteractor, ui, io, computation)
        assertFalse(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("some.email@url.com")
        assertFalse(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("hihihi")
        assertTrue(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("samhisam")
        assertTrue(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("samsam")
        assertFalse(uut.emailValid.blockingFirst())
    }

    @Test
    fun testPasswordValid() {
        whenever(authInteractor.isValidPassword(anyString())).thenAnswer {
            val arg = it.getArgument<String>(0)
            return@thenAnswer arg.isNotBlank() && arg.contains("hi")
        }
        uut = LoginViewModel(authInteractor, ui, io, computation)
        assertFalse(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("some.email@url.com")
        assertFalse(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("hihihi")
        assertTrue(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("samhisam")
        assertTrue(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("samsam")
        assertFalse(uut.passwordValid.blockingFirst())
    }

    @Test
    fun testLoading() {
        val signInSubject = PublishSubject.create<AuthInteractor.SignInResult>()

        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(
            authInteractor.signIn(
                anyString(),
                anyString()
            )
        ).thenReturn(signInSubject.firstOrError())
        uut = LoginViewModel(authInteractor, ui, io, computation)

        assertFalse(uut.loading.blockingFirst())
        uut.emailInput.onNext("email")
        uut.passwordInput.onNext("password")
        uut.buttonClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        signInSubject.onNext(AuthInteractor.SignInResult.Success(object : UserInfo {}))
        assertFalse(uut.loading.blockingFirst())
    }

    @Test
    fun testError() {
        val signInSubject = PublishSubject.create<AuthInteractor.SignInResult>()

        whenever(authInteractor.isValidEmail(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(
            authInteractor.signIn(
                anyString(),
                anyString()
            )
        ).thenReturn(signInSubject.firstOrError())
        uut = LoginViewModel(authInteractor, ui, io, computation)

        assertFalse(uut.loginError.blockingFirst().isPresent)
        uut.emailInput.onNext("email")
        uut.passwordInput.onNext("password")
        uut.buttonClicks.onNext(Unit)
        assertTrue(uut.loading.blockingFirst())
        signInSubject.onNext(AuthInteractor.SignInResult.Failure(AuthInteractor.SignInError.ERROR_INVALID_CREDENTIALS))
        assertFalse(uut.loading.blockingFirst())
        assertEquals(
            AuthInteractor.SignInError.ERROR_INVALID_CREDENTIALS,
            uut.loginError.blockingFirst().get()
        )
    }
}