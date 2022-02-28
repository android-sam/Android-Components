package uk.co.conjure.components.auth

import io.reactivex.rxjava3.core.Observable

interface ViewModelAction<S : ViewModelState, R : ViewModelResult<S>> {
    fun takeAction(state: S): Observable<R>

    fun validAction(state: S): Boolean
}

interface ViewModelState

interface ViewModelResult<S : ViewModelState> {
    fun transformState(state: S): S

    fun validTransformation(state: S): Boolean
}