package uk.co.conjure.components.auth.stateviewmodel

interface ViewModelResult<S : ViewModelState> {
    fun transformState(state: S): S

    fun validTransformation(state: S): Boolean
}