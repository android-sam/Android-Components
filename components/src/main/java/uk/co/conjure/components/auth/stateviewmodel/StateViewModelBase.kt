package uk.co.conjure.components.auth.stateviewmodel

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import uk.co.conjure.components.lifecycle.RxViewModel

/**
 * Represents a view model which holds a state. That state can be transformed by [ViewModelResult]'s
 * and those objects are produced by [ViewModelAction]'s. To implement this class you must provide a
 * default state and a stream of actions.
 *
 * The state will be transformed by a result only if that result returns true for [ViewModelResult.validTransformation]
 * when passed the current state. The stream of results will only be produced by the action if the
 * action returns true for [ViewModelAction.validAction] when given the current state.
 *
 * The inspiration behind this pattern was the lecture "Managing State with RxJava by Jake Wharton"
 * available on YouTube here: https://www.youtube.com/watch?v=0IKHxjkgop4
 */
abstract class StateViewModelBase<S : ViewModelState, R : ViewModelResult<S>, A : ViewModelAction<S, R>>(
    private val ui: Scheduler
) : RxViewModel() {

    protected abstract fun getActions(): Observable<A>

    protected abstract fun getDefaultState(): S

    protected val stateSubject: BehaviorSubject<S> by lazy {
        //So as not to call abstract functions from the base class let's initialize this lazily
        val stateSubject = BehaviorSubject.createDefault(getDefaultState())

        val currentState = { stateSubject.value ?: getDefaultState() }
        keepAlive.add(getActions()
            .filter { it.validAction(currentState()) }
            .flatMap { it.takeAction(currentState()) }
            .observeOn(ui)
            .map { Pair<R, S>(it, currentState()) }
            .filter { it.first.validTransformation(it.second) }
            .map { it.first.transformState(it.second) }
            .subscribe({ stateSubject.onNext(it) }, { stateSubject.onNext(getDefaultState()) })
        )

        return@lazy stateSubject
    }
}