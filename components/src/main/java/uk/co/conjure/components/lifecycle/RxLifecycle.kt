package uk.co.conjure.components.lifecycle

import android.annotation.SuppressLint
import androidx.lifecycle.*
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer

fun <T : Any> Observable<T>.whileStarted(
    owner: LifecycleOwner,
    onNext: Consumer<T>,
    onError: Consumer<in Throwable>? = null,
    onComplete: Action? = null,
) {
    val observer = object : DefaultLifecycleObserver {
        var disposable: Disposable? = null

        @SuppressLint("RxSubscribeOnError")
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            disposable = if (onError != null) {
                if (onComplete != null) {
                    subscribe(onNext, onError, onComplete)
                } else {
                    subscribe(onNext, onError)
                }
            } else {
                subscribe(onNext)
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            disposable?.dispose()
            owner.lifecycle.removeObserver(this)
            super.onStop(owner)
        }
    }

    owner.lifecycle.addObserver(observer)
}


fun <T : Any> Observable<T>.whileCreated(
    owner: LifecycleOwner,
    onNext: Consumer<T>,
    onError: Consumer<in Throwable>? = null
) {

    val observer = object : DefaultLifecycleObserver {
        var disposable: Disposable? = null

        @SuppressLint("RxSubscribeOnError")
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            disposable = if (onError != null) {
                subscribe(onNext, onError)
            } else {
                subscribe(onNext)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            disposable?.dispose()
            owner.lifecycle.removeObserver(this)
            super.onDestroy(owner)
        }
    }

    owner.lifecycle.addObserver(observer)
}

fun <T : Any> Flowable<T>.whileStarted(
    owner: LifecycleOwner,
    onNext: Consumer<T>,
    onError: Consumer<in Throwable>? = null,
    onComplete: Action? = null,
) {
    val observer = object : DefaultLifecycleObserver {
        var disposable: Disposable? = null

        @SuppressLint("RxSubscribeOnError")
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            disposable = if (onError != null) {
                if (onComplete != null) {
                    subscribe(onNext, onError, onComplete)
                } else {
                    subscribe(onNext, onError)
                }
            } else {
                subscribe(onNext)
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            disposable?.dispose()
            owner.lifecycle.removeObserver(this)
            super.onStop(owner)
        }
    }

    owner.lifecycle.addObserver(observer)
}


fun <T : Any> Flowable<T>.whileCreated(
    owner: LifecycleOwner,
    onNext: Consumer<T>,
    onError: Consumer<in Throwable>? = null
) {

    val observer = object : DefaultLifecycleObserver {
        var disposable: Disposable? = null

        @SuppressLint("RxSubscribeOnError")
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            disposable = if (onError != null) {
                subscribe(onNext, onError)
            } else {
                subscribe(onNext)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            disposable?.dispose()
            owner.lifecycle.removeObserver(this)
            super.onDestroy(owner)
        }
    }

    owner.lifecycle.addObserver(observer)
}
