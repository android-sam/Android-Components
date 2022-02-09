package uk.co.conjure.components.showroom

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import uk.co.conjure.components.showroom.di.ShowroomViewModelFactory
import uk.co.conjure.firebasecomponents.auth.FirebaseAuthInteractor

class ShowroomApp : Application() {
    lateinit var viewModelFactory: ShowroomViewModelFactory
        private set

    override fun onCreate() {
        super.onCreate()
        val authInteractor = FirebaseAuthInteractor(FirebaseAuth.getInstance())
        this.viewModelFactory = ShowroomViewModelFactory(authInteractor)
    }
}