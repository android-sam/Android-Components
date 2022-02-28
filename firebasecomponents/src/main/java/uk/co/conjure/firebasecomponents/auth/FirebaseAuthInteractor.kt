package uk.co.conjure.firebasecomponents.auth

import android.util.Patterns
import com.google.firebase.auth.*
import io.reactivex.rxjava3.core.Single
import uk.co.conjure.components.auth.AuthInteractor

open class FirebaseAuthInteractor(
    private val auth: FirebaseAuth,
    private val isValidEmailFun: ((email: String) -> Boolean)? = null,
    private val isValidPasswordFun: ((password: String) -> Boolean)? = null
) : AuthInteractor {

    override fun isValidEmail(email: String): Boolean {
        return isValidEmailFun?.invoke(email) ?: Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun isValidPassword(password: String): Boolean {
        return isValidPasswordFun?.invoke(password) ?: (password.length >= 6)
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
                    emitter.onSuccess(
                        AuthInteractor.SignUpResult.Success(
                            FirebaseUserInfo(it.result?.user as UserInfo)
                        )
                    )
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> AuthInteractor.SignUpError.ERROR_INVALID_PASSWORD
                        is FirebaseAuthInvalidCredentialsException -> AuthInteractor.SignUpError.ERROR_INVALID_EMAIL
                        is FirebaseAuthUserCollisionException -> AuthInteractor.SignUpError.ERROR_USER_ALREADY_PRESENT
                        else -> AuthInteractor.SignUpError.ERROR
                    }
                    emitter.onSuccess(AuthInteractor.SignUpResult.Failure(error))
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