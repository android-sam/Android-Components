package uk.co.conjure.components.showroom.signup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import uk.co.conjure.components.auth.signup.SignUpViewModel
import uk.co.conjure.components.showroom.ShowroomApp
import uk.co.conjure.components.showroom.databinding.LoginFragmentBinding
import uk.co.conjure.components.showroom.databinding.SignupFragmentBinding

class SignUpFragment : Fragment() {

    private lateinit var signupView: SignUpView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        signupView = SignUpView(
            viewModel = ViewModelProvider(
                requireActivity(),
                (context.applicationContext as ShowroomApp).viewModelFactory
            )[SignUpViewModel::class.java]
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        signupView.registerBinding(SignupFragmentBinding.inflate(inflater, container, false), this)
        return signupView.requireBinding().root
    }
}