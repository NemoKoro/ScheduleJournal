package ru.journal.schedulejournal.schedule

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.journal.schedulejournal.Header
import ru.journal.schedulejournal.MainMenuActivity
import ru.journal.schedulejournal.R
import ru.journal.schedulejournal.Requests
import ru.journal.schedulejournal.auth.AuthData
import ru.journal.schedulejournal.auth.loadAuthData
import ru.journal.schedulejournal.databinding.ActivityScheduleBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScheduleActivity : AppCompatActivity() {

    private var _binding: ActivityScheduleBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding для ActivityVisitsBinding не может быть null после onCreate")

    private val URL = "https://msapi.top-academy.ru/api/v2/schedule/operations/get-by-date?date_filter="
    private lateinit var userAuthData: AuthData
    private val localDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val schedule: MutableMap<String, Map<Int, Lesson>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userAuthData = loadAuthData(this@ScheduleActivity)

        val today = localDate.format(dateFormatter)
        val tomorrow = localDate.plusDays(1).format(dateFormatter)

        CoroutineScope(Dispatchers.Main).launch {
            binding.tvProgressBar.isVisible = true
            try {
                getSchedule(today)
                getSchedule(tomorrow)
            } finally {
                binding.tvProgressBar.isVisible = false
                displaySchedule(today)
            }

        }

        with(binding) {
            btnGetSheduleOfDate.setOnClickListener {
                val _dayNum: String = tieDateNum.getText().toString()

                try {
                    val dayNum: LocalDate = localDate.withDayOfMonth(_dayNum.toInt())
                    val dayNumFormatted: String =  dayNum.format(dateFormatter)

                    if (schedule[dayNumFormatted]?.isNotEmpty() != true) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                tvNoLessons.isVisible = false
                                llLessonBox.isVisible = false
                                tvProgressBar.isVisible = true
                                getSchedule(dayNumFormatted)
                            } finally {
                                tvProgressBar.isVisible = false
                                displaySchedule(dayNumFormatted)
                            }
                        }
                    } else displaySchedule(dayNumFormatted)
                } catch (e: Exception) {
                    tilDateNum.isErrorEnabled = true
                    tieDateNum.error = "Нет такого дня в этом месяце"
                }
            }

            btnSetTodayDate.setOnClickListener {
                displaySchedule(today)
            }

            btnSetTomorrowDate.setOnClickListener {
                displaySchedule(tomorrow)
            }

            btnGoBack.setOnClickListener {
                startActivity(Intent(this@ScheduleActivity, MainMenuActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun getSchedule(date: String) {
        val request: Requests = Requests()

        val headers: List<Header> = listOf(
            Header("Content-Type", "application/json"),
            Header("Accept", "application/json, text/plain, */*"),
            Header("Authorization", "Bearer ${userAuthData.accessToken}"),
            Header("Origin", "https://journal.top-academy.ru"),
            Header("Referer", "https://journal.top-academy.ru")
        )

        val response: String? = request.getRequest(URL + date, headers)


        val json = Json { ignoreUnknownKeys = true }
        if (response != null) {
            val scheduleDay: List<Lesson> = json.decodeFromString(ListSerializer(Lesson.serializer()), response)

            schedule[date] = scheduleDay.associateBy { it.number }
        } else println(getString(R.string.journal_not_responding))
    }

    private fun displaySchedule(date: String) {
        with(binding) {
            if (schedule[date]?.isNotEmpty() == true) {
                tvNoLessons.isVisible = false
                llLessonBox.removeAllViews()
                llLessonBox.isVisible = true

                val inflater = LayoutInflater.from(this@ScheduleActivity)

                for(i in 0 until 4) {

                    val lessonLL: LinearLayout = inflater.inflate(R.layout.ll_lesson, llLessonBox, false) as LinearLayout
                    val noLessonTV: TextView = inflater.inflate(R.layout.tv_no_lesson, llLessonBox, false) as TextView

                    if (schedule[date]?.get(i+1) != null) {
                        if (i == 0) {
                            val layoutParams = lessonLL.layoutParams

                            if (layoutParams is ViewGroup.MarginLayoutParams) {
                                layoutParams.topMargin = 0
                                lessonLL.layoutParams = layoutParams
                            }
                        }

                        llLessonBox.addView(lessonLL)
                    } else llLessonBox.addView(noLessonTV)

                    val lessonItem:View? = llLessonBox.getChildAt(i)

                    if (lessonItem is LinearLayout) {

                        val lesson = schedule[date]?.get(i+1)

                        (lessonItem.getChildAt(0) as? TextView)?.text = lesson?.name
                        (lessonItem.getChildAt(2) as? TextView)?.text = "${lesson?.started ?: "null"}-${lesson?.finished ?: "null"}"
                        (lessonItem.getChildAt(4) as? TextView)?.text = lesson?.teacher

                    }
                }
            } else {
                llLessonBox.isVisible = false
                tvNoLessons.isVisible = true
            }
        }
    }
}