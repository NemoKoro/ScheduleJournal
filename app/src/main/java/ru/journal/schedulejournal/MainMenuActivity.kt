package ru.journal.schedulejournal

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import ru.journal.schedulejournal.auth.AuthData
import ru.journal.schedulejournal.auth.AuthorizationActivity
import ru.journal.schedulejournal.auth.loadAuthData
import ru.journal.schedulejournal.auth.saveAuthData
import ru.journal.schedulejournal.databinding.ActivityMainMenuBinding
import ru.journal.schedulejournal.schedule.ScheduleActivity
import ru.journal.schedulejournal.visits.VisitsActivity

class MainMenuActivity : AppCompatActivity() {

    private var _binding: ActivityMainMenuBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding для ActivityVisitsBinding не может быть null после onCreate")

    private var userAuthData: AuthData = AuthData(null, null, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userAuthData = loadAuthData(this@MainMenuActivity)

        with(binding) {

            btnGoToVisits.setOnClickListener {
                val intent = Intent(this@MainMenuActivity, VisitsActivity::class.java)
                intent.putExtra("EXTRA_AUTH_DATA", userAuthData)
                startActivity(intent)
            }

            btnGoToSchedule.setOnClickListener {
                val intent = Intent(this@MainMenuActivity, ScheduleActivity::class.java)
                intent.putExtra("EXTRA_AUTH_DATA", userAuthData)
                startActivity(intent)
            }

            btnLogOut.setOnClickListener {
                saveAuthData(this@MainMenuActivity, AuthData(null, null, null))
                startActivity(Intent(this@MainMenuActivity, AuthorizationActivity::class.java))
            }

        }
    }
}