package uk.co.conjure.firebasecomponents.auth

import android.util.Patterns
import com.google.firebase.auth.*
import io.reactivex.rxjava3.core.Single
import uk.co.conjure.components.auth.AuthInteractor
import uk.co.conjure.firebasecomponents.rxextensions.toSingleTask
import java.lang.Exception

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

    open fun requestPasswordReset(
        email: String,
        actionCodeSettings: ActionCodeSettings
    ): Single<AuthInteractor.RequestPasswordResetResult> {
        return try {
            auth.sendPasswordResetEmail(email, actionCodeSettings)
                .toSingleTask()
                .map {
                    if (it.isSuccessful) {
                        AuthInteractor.RequestPasswordResetResult.Success(null)
                    } else {
                        AuthInteractor.RequestPasswordResetResult.Failure(
                            AuthInteractor.RequestPasswordResetError.ERROR
                        )
                    }
                }
        } catch (t: Throwable) {
            return Single.just(
                AuthInteractor.RequestPasswordResetResult.Failure(
                    AuthInteractor.RequestPasswordResetError.ERROR
                )
            )
        }
    }

    override fun performPasswordReset(
        code: String,
        newPassword: String
    ): Single<AuthInteractor.ResetPasswordResult> {
        return Single.create { emitter ->
            val task = auth.confirmPasswordReset(code, newPassword)
            task.addOnSuccessListener {
                if (!emitter.isDisposed) {
                    emitter.onSuccess(AuthInteractor.ResetPasswordResult.Success)
                }
            }
            task.addOnFailureListener {
                if (!emitter.isDisposed) {
                    val error = resetPasswordExceptionToError(it)
                    emitter.onSuccess(AuthInteractor.ResetPasswordResult.Failure(error))
                }
            }
        }
    }

    override fun sendEmailVerificationLink(): Single<Boolean> {
        return (auth.currentUser ?: return Single.just(false))
            .sendEmailVerification()
            .toSingleTask()
            .map { it.isSuccessful }
    }

    private fun resetPasswordExceptionToError(exception: Exception?): AuthInteractor.ResetPasswordError {
        return when (exception) {
            is FirebaseAuthActionCodeException -> AuthInteractor.ResetPasswordError.ERROR_CODE_EXPIRED
            is FirebaseAuthInvalidUserException -> AuthInteractor.ResetPasswordError.ERROR_INVALID_USER
            else -> AuthInteractor.ResetPasswordError.ERROR_UNKOWN
        }
    }
}