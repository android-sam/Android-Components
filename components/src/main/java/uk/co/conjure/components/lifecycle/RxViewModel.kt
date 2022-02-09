package uk.co.conjure.components.lifecycle

import androidx.lifecycle.ViewModel as AndroidViewModel
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.flowables.ConnectableFlowable
import io.reactivex.rxjava3.observables.ConnectableObservable

abstract class RxViewModel : AndroidViewModel() {

    /**
     * This CompositeDisposable will be disposed in onCleared()
     * Therefore it survives orientation changes
     */
    protected val keepAlive: CompositeDisposable = CompositeDisposable()

    override fun onCleared() {
        keepAlive.dispose()
        super.onCleared()
    }

    /**
     * Applies a replay(1).refCount() to the Flowable and subscribes to it.
     * The Flowable will be alive as long as the ViewModel is alive (until the ViewModels
     * [onCleared] method is called).
     *
     *  @see [Observable.replay], [ConnectableFlowable.refCount]
     */
    protected fun <T : Any> Flowable<T>.hot(): Flowable<T> {
        return this.replay(1).refCount().also {
            keepAlive.add(it.subscribe({}, {}))
        }
    }

    /**
     * Applies a replay(1).refCount() to the Observable and subscribes to it.
     * The Observable will be alive as long as the ViewModel is alive (until the ViewModels
     * [onCleared] method is called).
     * @see [Observable.replay], [ConnectableObservable.refCount]
     */
    protected fun <T : Any> Observable<T>.hot(): Observable<T> {
        return this.replay(1).refCount().also {
            keepAlive.add(it.subscribe({}, {}))
        }
    }

    /**
     * Caches the Single and subscribes to it.
     * The Single will return it's result for any later subscriber as long as the ViewModel is
     * alive (until the ViewModels [onCleared] method is called).
     * @see [Single.cache]
     */
    protected fun <T : Any> Single<T>.hot(): Single<T> {
        return this.cache().also {
            keepAlive.add(it.subscribe({}, {}))
        }
    }

    /**
     * Caches the Maybe and subscribes to it.
     * The Maybe will return it's result for any later subscriber as long as the ViewModel is
     * alive (until the ViewModels [onCleared] method is called).
     * @see [Maybe.cache]
     */
    protected fun <T : Any> Maybe<T>.hot(): Maybe<T> {
        return this.cache().also {
            keepAlive.add(it.subscribe({}, {}))
        }
    }

    /**
     * Caches the Completable and subscribes to it.
     * The Completable will return it's result for any later subscriber as long as the ViewModel is
     * alive (until the ViewModels [onCleared] method is called).
     * @see [Completable.cache]
     */
    protected fun Completable.hot(): Completable {
        return this.cache().also {
            keepAlive.add(it.subscribe({}, {}))
        }
    }
}