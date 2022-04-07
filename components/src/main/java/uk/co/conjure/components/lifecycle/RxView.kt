package uk.co.conjure.components.lifecycle

import android.annotation.SuppressLint
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import uk.co.conjure.components.ui.changes

open class RxView<B : ViewBinding> : LifecycleView<B>() {

    protected val subscriptions = CompositeDisposable()

    override fun onStop() {
        subscriptions.clear()
        super.onStop()
    }

    override fun onDestroy() {
        subscriptions.dispose()
        super.onDestroy()
    }

    /**
     * Bind the EditText to the ViewModel
     *
     * @param observer An observer
     */
    @SuppressLint("RxSubscribeOnError")
    protected fun EditText.bind(
        observer: Observer<String>,
        observable: Observable<String>
    ) {
        this.changes()
            .doOnSubscribe { disposable -> subscriptions.add(disposable) }
            .subscribe(observer)
        observable.subscribe { text ->
            if (text != this.text.toString()) {
                this.setText(text)
                this.setSelection(text.length)
            }
        }.addTo(subscriptions)
    }

    @SuppressLint("RxSubscribeOnError")
    protected fun TextView.bind(observable: Observable<String>) {
        observable.subscribe { text -> this.text = text }.addTo(subscriptions)
    }

    @SuppressLint("RxSubscribeOnError")
    protected fun Button.bind(
        observer: Observer<Unit>,
        observable: Observable<Boolean> = Observable.just(true)
    ) {
        this.clicks()
            .doOnSubscribe { disposable -> subscriptions.add(disposable) }
            .subscribe(observer)
        observable
            .subscribe { isEnabled -> this.isEnabled = isEnabled }
            .addTo(subscriptions)
    }

    @SuppressLint("RxSubscribeOnError")
    protected fun View.bindClicks(
        observer: Observer<Unit>,
        observable: Observable<Boolean> = Observable.just(true)
    ) {
        this.clicks()
            .doOnSubscribe { disposable -> subscriptions.add(disposable) }
            .subscribe(observer)
        observable
            .subscribe { isEnabled -> this.isEnabled = isEnabled }
            .addTo(subscriptions)
    }

}