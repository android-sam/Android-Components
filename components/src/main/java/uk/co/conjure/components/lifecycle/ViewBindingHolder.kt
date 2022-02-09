package uk.co.conjure.components.lifecycle

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding

abstract class ViewBindingHolder<B : ViewBinding> {

    private var _binding: B? = null

    protected val binding: B get() = _binding!!

    protected val context: Context get() = _binding!!.root.context

    /**
     * Only valid for the lifecycle of the View
     * Call [registerBinding] to set the binding.
     *
     * For Activities it will be available until onDestroy().
     * For Fragments it will be available unit onDestroyView().
     */
    fun requireBinding() = checkNotNull(_binding)

    fun requireBinding(lambda: (B) -> Unit) {
        _binding?.run { lambda(this) }
    }

    /**
     * Register a ViewBinding for an Activity
     */
    fun registerBinding(binding: B, activity: AppCompatActivity) {
        registerBinding(binding, activity.lifecycle)
        onBindingRegistered(binding, activity)
    }

    /**
     * Register a ViewBinding for a Fragment
     */
    fun registerBinding(binding: B, fragment: Fragment) {
        registerBinding(binding, fragment.viewLifecycleOwner.lifecycle)
        onBindingRegistered(binding, fragment)
    }

    /**
     * Called when a Fragment or Activity registered a binding.
     * Subclasses may use this to register their View subscriptions.
     */
    abstract fun onBindingRegistered(binding: B, owner: LifecycleOwner)


    private fun registerBinding(binding: B, lifecycleOwner: Lifecycle) {
        this._binding = binding
        lifecycleOwner.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                this@ViewBindingHolder._binding = null
            }
        })
    }

}