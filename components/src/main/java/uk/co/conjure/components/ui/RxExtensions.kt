package uk.co.conjure.components.ui

import android.widget.TextView
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.core.Observable

/**
 * Warning: This skips the initial value!
 */
fun TextView.changes(): Observable<String> {
    return this.textChanges().skipInitialValue().map { it.toString() }
}