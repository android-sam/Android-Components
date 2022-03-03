package uk.co.conjure.firebasecomponents.rxextensions

import com.google.android.gms.tasks.Task
import io.reactivex.rxjava3.core.Single
import java.lang.Exception

fun <T : Any> Task<T>.toSingleTask(): Single<Task<T>> {
    return Single.create { emitter ->
        //If the task has successfully completed already then a call
        // to the listener will be scheduled immediately
        this.addOnCompleteListener {
            if (!emitter.isDisposed) emitter.onSuccess(this)
        }
    }
}

fun <T : Any> Task<T>.toSingle(): Single<T> {
    return Single.create { emitter ->
        //If the task has successfully completed already then a call
        // to the listener will be scheduled immediately
        this.addOnSuccessListener {
            if (!emitter.isDisposed) emitter.onSuccess(it)
        }
        this.addOnFailureListener {
            if (!emitter.isDisposed) emitter.onError(it)
        }
        this.addOnCanceledListener {
            if (!emitter.isDisposed) emitter.onError(Exception())
        }
    }
}