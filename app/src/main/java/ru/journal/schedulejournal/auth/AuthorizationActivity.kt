package ru.journal.schedulejournal.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.journal.schedulejournal.MainMenuActivity
import ru.journal.schedulejournal.R
import ru.journal.schedulejournal.databinding.ActivityAuthorizationBinding

class AuthorizationActivity : AppCompatActivity() {
    private var _binding: ActivityAuthorizationBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding для ActivityVisitsBinding не может быть null после onCreate")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityAuthorizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkLoginData()

        with(binding) {
            btnLogin.setOnClickListener {
                val userLogin: String = tieUserLogin.getText().toString()
                val userPassword: String = tieUserPassword.getText().toString()

                if (userLogin.isNotEmpty() && userPassword.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {

                            llLoginForm.isVisible = false
                            tvProgressBar.isVisible = true

                            if(login(this@AuthorizationActivity, userLogin, userPassword)) {
                                startActivity(Intent(this@AuthorizationActivity, MainMenuActivity::class.java))
                                finish()
                            } else {
                                throw Exception("Login error")
                            }
                        } catch (e: Exception) {
                            tvProgressBar.isVisible = false
                            llLoginForm.isVisible = true

                            tilUserLogin.isErrorEnabled = true
                            tieUserLogin.error = getString(R.string.error)
                            tilUserPassword.isErrorEnabled = true
                            tieUserPassword.error = getString(R.string.error)
                        }

                    }
                } else {
                    userLogin.ifEmpty {
                        tilUserLogin.isErrorEnabled = true
                        tieUserLogin.error = getString(R.string.empty)
                    }
                    userPassword.ifEmpty {
                        tilUserPassword.isErrorEnabled = true
                        tieUserPassword.error = getString(R.string.empty)
                    }
                }
            }
        }
    }

    private fun checkLoginData() {
        val userAuthData: AuthData = loadAuthData(this@AuthorizationActivity)

        if(userAuthData.login != null && userAuthData.password != null) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.llLoginForm.isVisible = false
                binding.tvProgressBar.isVisible = true
                if(login(this@AuthorizationActivity, userAuthData.login, userAuthData.password)) {
                    startActivity(Intent(this@AuthorizationActivity, MainMenuActivity::class.java))
                    finish()
                }
            }
        }
    }
}