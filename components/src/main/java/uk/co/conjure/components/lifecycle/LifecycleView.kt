package uk.co.conjure.components.lifecycle

import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding

/**
 * Parent class for the View layer in an MVVM architecture.
 * It is a ViewBindingHolder (the View) that is also Lifecycle aware.
 */
abstract class LifecycleView<B : ViewBinding> : ViewBindingHolder<B>() {

    // This is to ensure a sub class calls super.onStart() and super.onStop()
    private var called = false

    final override fun onBindingRegistered(binding: B, owner: LifecycleOwner) {
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                called = false
                onStart()
                if (!called) {
                    throw IllegalStateException("View $this did not call through to super.onCreate()")
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                called = false
                onStop()
                if (!called) {
                    throw IllegalStateException("View $this did not call through to super.onCreate()")
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                called = false
                onDestroy()
                if (!called) {
                    throw IllegalStateException("View $this did not call through to super.onCreate()")
                }
            }
        })
    }

    @CallSuper
    open fun onStart() {
        called = true
    }

    @CallSuper
    open fun onStop() {
        called = true
    }

    @CallSuper
    open fun onDestroy() {
        called = true
    }
}