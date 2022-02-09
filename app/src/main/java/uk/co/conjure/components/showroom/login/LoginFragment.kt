package uk.co.conjure.components.showroom.login

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import uk.co.conjure.components.auth.login.LoginViewModel
import uk.co.conjure.components.showroom.ShowroomApp
import uk.co.conjure.components.showroom.databinding.LoginFragmentBinding

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var loginView: LoginView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        loginView = LoginView(
            viewModel = ViewModelProvider(
                requireActivity(),
                (context.applicationContext as ShowroomApp).viewModelFactory
            )[LoginViewModel::class.java]
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loginView.registerBinding(LoginFragmentBinding.inflate(inflater, container, false), this)
        return loginView.requireBinding().root
    }

}