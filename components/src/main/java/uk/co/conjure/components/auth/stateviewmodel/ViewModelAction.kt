package uk.co.conjure.components.auth.stateviewmodel

import io.reactivex.rxjava3.core.Observable

interface ViewModelAction<S : ViewModelState, R : ViewModelResult<S>> {
    fun takeAction(state: S): Observable<R>

    fun validAction(state: S): Boolean
}