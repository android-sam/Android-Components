package uk.co.conjure.components.auth.login

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.components.auth.signup.SignUpViewModel
import uk.co.conjure.components.auth.user.UserInfo

class SignUpViewModelTest {

    lateinit var authInteractor: AuthInteractor

    lateinit var io: Scheduler

    lateinit var ui: Scheduler

    lateinit var computation: Scheduler

    lateinit var uut: SignUpViewModel

    @Before
    fun setup() {
        authInteractor = mock()
        io = Schedulers.trampoline()
        ui = Schedulers.trampoline()
        computation = Schedulers.trampoline()
    }

    @Test
    fun testButtonEnabled() {
        whenever(authInteractor.isValidEmail(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        uut = SignUpViewModel(authInteractor, ui, io, computation)
        Assert.assertFalse(uut.buttonEnabled.blockingFirst())
        uut.emailInput.onNext("some.email@url.com")
        Assert.assertFalse(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("somepassword")
        Assert.assertTrue(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("")
        Assert.assertFalse(uut.buttonEnabled.blockingFirst())
        uut.passwordInput.onNext("i")
        Assert.assertTrue(uut.buttonEnabled.blockingFirst())
        uut.emailInput.onNext("")
        Assert.assertFalse(uut.buttonEnabled.blockingFirst())
    }

    @Test
    fun testEmailValid() {
        whenever(authInteractor.isValidEmail(ArgumentMatchers.anyString())).thenAnswer {
            val arg = it.getArgument<String>(0)
            return@thenAnswer arg.isNotBlank() && arg.contains("hi")
        }
        uut = SignUpViewModel(authInteractor, ui, io, computation)
        Assert.assertFalse(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("some.email@url.com")
        Assert.assertFalse(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("hihihi")
        Assert.assertTrue(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("samhisam")
        Assert.assertTrue(uut.emailValid.blockingFirst())
        uut.emailInput.onNext("samsam")
        Assert.assertFalse(uut.emailValid.blockingFirst())
    }

    @Test
    fun testPasswordValid() {
        whenever(authInteractor.isValidPassword(ArgumentMatchers.anyString())).thenAnswer {
            val arg = it.getArgument<String>(0)
            return@thenAnswer arg.isNotBlank() && arg.contains("hi")
        }
        uut = SignUpViewModel(authInteractor, ui, io, computation)
        Assert.assertFalse(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("some.email@url.com")
        Assert.assertFalse(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("hihihi")
        Assert.assertTrue(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("samhisam")
        Assert.assertTrue(uut.passwordValid.blockingFirst())
        uut.passwordInput.onNext("samsam")
        Assert.assertFalse(uut.passwordValid.blockingFirst())
    }

    @Test
    fun testLoading() {
        val signUpSubject = PublishSubject.create<AuthInteractor.SignUpResult>()

        whenever(authInteractor.isValidEmail(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(
            authInteractor.signUp(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(signUpSubject.firstOrError())
        uut = SignUpViewModel(authInteractor, ui, io, computation)

        Assert.assertFalse(uut.loading.blockingFirst())
        uut.emailInput.onNext("email")
        uut.passwordInput.onNext("password")
        uut.buttonClicks.onNext(Unit)
        Assert.assertTrue(uut.loading.blockingFirst())
        signUpSubject.onNext(AuthInteractor.SignUpResult.Success(object : UserInfo {}))
        Assert.assertFalse(uut.loading.blockingFirst())
    }

    @Test
    fun testError() {
        val signUpSubject = PublishSubject.create<AuthInteractor.SignUpResult>()

        whenever(authInteractor.isValidEmail(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(authInteractor.isValidPassword(ArgumentMatchers.anyString())).thenAnswer {
            it.getArgument<String>(0).isNotBlank()
        }
        whenever(
            authInteractor.signUp(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(signUpSubject.firstOrError())
        uut = SignUpViewModel(authInteractor, ui, io, computation)

        Assert.assertFalse(uut.signUpError.blockingFirst().isPresent)
        uut.emailInput.onNext("email")
        uut.passwordInput.onNext("password")
        uut.buttonClicks.onNext(Unit)
        Assert.assertTrue(uut.loading.blockingFirst())
        signUpSubject.onNext(AuthInteractor.SignUpResult.Failure(AuthInteractor.SignUpError.ERROR_INVALID_PASSWORD))
        Assert.assertFalse(uut.loading.blockingFirst())
        Assert.assertEquals(
            AuthInteractor.SignUpError.ERROR_INVALID_PASSWORD,
            uut.signUpError.blockingFirst().get()
        )
    }
}