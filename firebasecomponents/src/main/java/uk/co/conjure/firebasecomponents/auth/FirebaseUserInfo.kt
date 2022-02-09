package uk.co.conjure.firebasecomponents.auth

import uk.co.conjure.components.auth.user.UserInfo

class FirebaseUserInfo(val userInfo: com.google.firebase.auth.UserInfo) : UserInfo {
}