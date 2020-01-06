package com.example.vincentagram

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSigninClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        google_sign_in_button.setOnClickListener {
            // First Step
            googleLogin()
        }
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSigninClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()
    }

    private fun facebookLogin() {
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    handleFacebookAccessToken(result?.accessToken)
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                }

            })
    }

    private fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                //Login
                moveMainPage(task.result?.user)
            }else{
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                var account = result.signInAccount
                // Second Step
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                //Login
                moveMainPage(task.result?.user)
            }else{
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun googleLogin(){
        var signInIntent = googleSigninClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun signinAndSignup(){

        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
            task ->
                if(task.isSuccessful){
                    //Creating a user account
                    moveMainPage(task.result?.user)
                }else if(task.exception?.message.isNullOrEmpty()){
                    // Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }else{
                    // Login if you have acount
                    signinEmail()
                }
        }
    }

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                //Login
                moveMainPage(task.result?.user)
            }else{
                // Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun moveMainPage(user:FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
