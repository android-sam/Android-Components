package uk.co.conjure.firebasecomponents.auth

import android.util.Patterns
import com.google.firebase.auth.*
import io.reactivex.rxjava3.core.Single
import uk.co.conjure.components.auth.AuthInteractor

class FirebaseAuthInteractor(
    private val auth: FirebaseAuth,
    private val isValidEmailFun: ((email: String) -> Boolean)? = null,
    private val isValidLoginFun: ((email: String, password: String) -> Boolean)? = null
) : AuthInteractor {
    override fun isValidLogin(email: String, password: String): Boolean {
        return isValidLoginFun?.invoke(email, password)
            ?: (isValidEmail(email) && password.isNotEmpty())
    }

    override fun isValidEmail(email: String): Boolean {
        return isValidEmailFun?.invoke(email) ?: Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun signIn(email: String, password: String): Single<AuthInteractor.SignInResult> {
        if (auth.currentUser != null) auth.signOut()
        return Single.create { emitter ->
            val task = auth.signInWithEmailAndPassword(email, password)
            task.addOnCompleteListener {
                if (emitter.isDisposed) return@addOnCompleteListener
                if (it.isSuccessful && it.result?.user != null) {
                    emitter.onSuccess(
                        AuthInteractor.SignInResult.Success(
                            FirebaseUserInfo(it.result?.user as UserInfo)
                        )
                    )
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> AuthInteractor.SignInError.ERROR_USER_NOT_FOUND
                        is FirebaseAuthInvalidCredentialsException -> AuthInteractor.SignInError.ERROR_INVALID_CREDENTIALS
                        else -> AuthInteractor.SignInError.ERROR
                    }
                    emitter.onSuccess(AuthInteractor.SignInResult.Failure(error))
                }
            }
        }
    }

    override fun signUp(email: String, password: String): Single<AuthInteractor.SignUpResult> {
        return Single.create { emitter ->
            val task = auth.createUserWithEmailAndPassword(email, password)
            task.addOnCompleteListener {
                if (emitter.isDisposed) return@addOnCompleteListener
                if (it.isSuccessful) {
                    emitter.onSuccess(AuthInteractor.SignUpResult.SUCCESS)
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> AuthInteractor.SignUpResult.ERROR_INVALID_PASSWORD
                        is FirebaseAuthInvalidCredentialsException -> AuthInteractor.SignUpResult.ERROR_INVALID_EMAIL
                        is FirebaseAuthUserCollisionException -> AuthInteractor.SignUpResult.ERROR_USER_ALREADY_PRESENT
                        else -> AuthInteractor.SignUpResult.ERROR
                    }
                    emitter.onSuccess(error)
                }
            }
        }
    }

    override fun requestPasswordReset(email: String): Single<AuthInteractor.RequestPasswordResetResult> {
        return Single.create { emitter ->
            val task = auth.sendPasswordResetEmail(email)
            task.addOnCompleteListener {
                if (emitter.isDisposed) return@addOnCompleteListener
                if (it.isSuccessful) {
                    emitter.onSuccess(AuthInteractor.RequestPasswordResetResult.Success(null))
                } else {
                    emitter.onSuccess(
                        AuthInteractor.RequestPasswordResetResult.Failure(
                            AuthInteractor.RequestPasswordResetError.ERROR
                        )
                    )
                }
            }
        }
    }
}