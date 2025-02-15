package ru.journal.schedulejournal.visits

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
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
import ru.journal.schedulejournal.databinding.ActivityVisitsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VisitsActivity:AppCompatActivity() {
    private var _binding: ActivityVisitsBinding? = null
    private val binding
        get() = _binding ?: throw Exception("Binding для ActivityVisitsBinding не может быть null после onCreate")

    private lateinit var userAuthData: AuthData
    private val visitsURL: String = "https://msapi.top-academy.ru/api/v2/progress/operations/student-visits"
    private val specsURL: String = "https://msapi.top-academy.ru/api/v2/settings/history-specs"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dayVisitIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val visitsColumnCount = 4
    private val dateVisits: MutableMap<String, MutableList<DateVisit>> = mutableMapOf()
    private var specs: List<ShortName> = mutableListOf()
    private val localDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityVisitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userAuthData = loadAuthData(this@VisitsActivity)

        val monthNum: Int = localDate.monthValue

        with(binding) {
            lifecycleScope.launch {
                getVisitsWithProgressBar(monthNum)
            }

            tvTitle.setOnClickListener {
                showSpecsMenu(clHeader)
            }

            ivOpenDateMenu.setOnClickListener {
                showIntervalChoiceMenu(clHeader)
            }

            ivSearch.setOnClickListener {
                if(llSearch.isVisible) llSearch.visibility = View.GONE else llSearch.visibility = View.VISIBLE
                llSearch.bringToFront()
            }

            btnGoSearch.setOnClickListener {
                llSearch.visibility = View.GONE

                val searchDateText = etDateForSearch.text.toString()
                tvTitle.text = getString(R.string.all)
                displayVisitsAll(null)

                try {
                    val dayVisitsID = LocalDate.parse(searchDateText, DateTimeFormatter.ofPattern("yyyy.MM.dd")).format(dayVisitIdFormatter).toInt()
                    svVisits.post {
                        val targetViewTop = findViewById<LinearLayout>(dayVisitsID)?.top ?: 0
                        svVisits.smoothScrollTo(0, targetViewTop)
                    }
                } catch (e: Exception) {
                    println("Ошибка парсинга даты для поиска: ${e.message}")
                }
            }

            ivReload.setOnClickListener {
                lifecycleScope.launch {
                    tvProgressBar.text = getString(R.string.loading___)
                    getVisitsWithProgressBar(monthNum)
                }
            }

            ivGoBack.setOnClickListener {
                startActivity(Intent(this@VisitsActivity, MainMenuActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun getVisitsWithProgressBar(month: Int?) {
        with(binding) {
            try {
                dateVisits.clear()
                llVisitsBox.isVisible = false
                tvProgressBar.isVisible = true
                tvTitle.text = getString(R.string.all)
                tvProgressBar.text = getString(R.string.loading___)

                val request: Requests = Requests()

                val headers: List<Header> = listOf(
                    Header("Content-Type", "application/json"),
                    Header("Accept", "application/json, text/plain, */*"),
                    Header("Authorization", "Bearer ${userAuthData.accessToken}"),
                    Header("Origin", "https://journal.top-academy.ru"),
                    Header("Referer", "https://journal.top-academy.ru")
                )

                val responseVisits: String? = request.getRequest(visitsURL, headers)
                val responseSpecs: String? = request.getRequest(specsURL, headers)

                val json = Json { ignoreUnknownKeys = true }

                if (responseVisits != null && responseSpecs != null) {
                    val dateVisitList: List<DateVisit> = json.decodeFromString(ListSerializer(
                        DateVisit.serializer()), responseVisits)
                    specs = json.decodeFromString(ListSerializer(ShortName.serializer()), responseSpecs)
                    dateVisitList.forEach { visit ->
                        specs.forEach {
                            if (it.name == visit.name) {
                                visit.short = it.short
                                visit.id = it.id
                            }
                        }
                        dateVisits.computeIfAbsent(visit.date) { mutableListOf() }.add(visit)
                    }
                } else {
                    throw Exception(getString(R.string.visits_error_response_failed))
                }

                tvProgressBar.isVisible = false
                llVisitsBox.isVisible = false
                if (month != null) displayVisitsAll(month) else displayVisitsAll(null)

            } catch (e: Exception) {
                println("Ошибка при загрузке посещений: ${e.message}")
                tvProgressBar.text = getString(R.string.try_again_later)
                llVisitsBox.isVisible = false
            }
            finally {
                if (!llVisitsBox.isVisible && !tvProgressBar.isVisible) {
                    llVisitsBox.isVisible = true
                }
                tvProgressBar.isVisible = false
            }
        }
    }

    private fun displayVisitsAll(month: Int?) {
        val inflater = LayoutInflater.from(this@VisitsActivity)

        with(binding) {

            if (tvProgressBar.isVisible) return

            llVisitsBox.removeAllViews()

            dateVisits.forEach { (dateKey, dayVisits) ->
                val dayVisitsDate = LocalDate.parse(dateKey, dateFormatter)

                val LL_visits: LinearLayout =
                    inflater.inflate(R.layout.ll_day_visits, llVisitsBox, false) as LinearLayout

                val dayVisitsID = dayVisitsDate.format(dayVisitIdFormatter).toInt()
                LL_visits.id = dayVisitsID
                LL_visits.visibility = View.VISIBLE
                llVisitsBox.addView(LL_visits)

                val title: TextView = LL_visits.getChildAt(0) as TextView

                title.text = when (dayVisitsDate) {
                    localDate -> "Сегодня"
                    localDate.minusDays(1) -> "Вчера"
                    else -> dateKey
                }

                if ((localDate.year == dayVisitsDate.year && month == dayVisitsDate.monthValue) || month == null) {
                    displayVisits(LL_visits, dayVisits)
                } else {
                    LL_visits.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun displayVisitsForSpec(id: Int) {

        val inflater = LayoutInflater.from(this@VisitsActivity)

        with(binding) {
            try {
                llVisitsBox.isVisible = false
                llVisitsBox.removeAllViews()
                tvProgressBar.isVisible = true
                tvProgressBar.text = getString(R.string.loading___)

                val request: Requests = Requests()

                val headers: List<Header> = listOf(
                    Header("Content-Type", "application/json"),
                    Header("Accept", "application/json, text/plain, */*"),
                    Header("Authorization", "Bearer ${userAuthData.accessToken}"),
                    Header("Origin", "https://journal.top-academy.ru"),
                    Header("Referer", "https://journal.top-academy.ru")
                )

                val responseVisits: String? = request.getRequest("$visitsURL?spec=$id", headers)

                val json = Json { ignoreUnknownKeys = true }

                val GL_visits: GridLayout = inflater.inflate(R.layout.gl_visits_box, llVisitsBox, false) as GridLayout
                llVisitsBox.addView(GL_visits)

                if (responseVisits != null) {
                    val dateVisitList: List<DateVisit> = json.decodeFromString(
                        ListSerializer(
                            DateVisit.serializer()
                        ), responseVisits
                    )
                    dateVisitList.forEach { visit ->
                        specs.forEach {
                            if (it.name == visit.name) {
                                visit.short = it.short
                            }
                        }
                    }
                    displayVisits(GL_visits, dateVisitList)

                    tvProgressBar.isVisible = false
                    llVisitsBox.isVisible = true
                } else {
                    throw Exception(getString(R.string.visits_error_response_failed))
                }
                tvProgressBar.isVisible = false

            } catch (e: Exception) {
                println("Ошибка при загрузке посещений: ${e.message}")
                tvProgressBar.text = getString(R.string.try_again_later)
                llVisitsBox.isVisible = false
            } finally {
                if (!llVisitsBox.isVisible && !tvProgressBar.isVisible) {
                    llVisitsBox.isVisible = true
                }
                tvProgressBar.isVisible = false
            }
        }
    }

    private fun displayVisits(visitsContainer: View, dayVisits: List<DateVisit>) {
        dayVisits.sortedBy { it.number }
        val inflater = LayoutInflater.from(this@VisitsActivity)

        if (visitsContainer is LinearLayout && dayVisits.size != 4) {
            for (i in 0 until visitsColumnCount) {
                val GL_visits: GridLayout = visitsContainer.getChildAt(1) as GridLayout
                val nonEmptyLesson = dayVisits.map { it.number }.toList()

                if (i + 1 !in nonEmptyLesson) {
                    val visitContainer = inflater.inflate(R.layout.ll_visit_no_lesson, GL_visits, false) as LinearLayout
                    val params: GridLayout.LayoutParams = visitContainer.layoutParams as GridLayout.LayoutParams
                    params.apply {
                        columnSpec = GridLayout.spec(i, 1f)
                        setMargins(10)
                        setGravity(Gravity.FILL)
                    }

                    GL_visits.addView(visitContainer, params)
                }
            }
        }

        for((elIndex, dayVisit) in dayVisits.withIndex()) {
            var GL_visits = GridLayout(this@VisitsActivity)
            val visitContainer = inflater.inflate(R.layout.cl_visit, GL_visits, false) as ConstraintLayout
            val params: GridLayout.LayoutParams = visitContainer.layoutParams as GridLayout.LayoutParams

            if (visitsContainer is LinearLayout) {
                GL_visits = visitsContainer.getChildAt(1) as GridLayout

                params.columnSpec =
                    GridLayout.spec(if (dayVisit.number != 5) dayVisit.number - 1 else 2, 1f)
            } else if (visitsContainer is GridLayout) {
                GL_visits = visitsContainer.apply {
                    columnCount = visitsColumnCount
                    rowCount = dayVisits.size / visitsColumnCount + 1
                }

                val tv_visitDate: TextView = visitContainer.getChildAt(0) as TextView
                tv_visitDate.text = dayVisit.date
                tv_visitDate.visibility = View.VISIBLE

                params.apply {
                    columnSpec = GridLayout.spec(elIndex % visitsColumnCount, 1f)
                    rowSpec = GridLayout.spec(elIndex / visitsColumnCount)
                    height = (70 * this@VisitsActivity.resources.displayMetrics.density).toInt()
                }
            }

            params.setMargins(10)
            params.setGravity(Gravity.FILL)
            GL_visits.addView(visitContainer, params)

            when (dayVisit.status) {
                0 -> visitContainer.setBackgroundResource(R.drawable.shape_pass_background)
                2 -> visitContainer.setBackgroundResource(R.drawable.shape_lateness_background)
            }

            val TV_lesson: TextView = visitContainer.getChildAt(1) as TextView
            dayVisit.short = dayVisit.short.replaceFirst("Проф", "").replaceFirst("Раз", "").replaceFirst("2 курс", "")
            TV_lesson.text = dayVisit.short.split(" ")[0]

            if (dayVisit.mark != null) {
                val TV_mark: TextView = visitContainer.getChildAt(2) as TextView
                TV_mark.visibility = View.VISIBLE
                TV_mark.text = dayVisit.mark.toString()
            }
        }
    }

    private fun showIntervalChoiceMenu(view: View) {
        val popupMenu = PopupMenu(this@VisitsActivity, view, Gravity.END)

        popupMenu.inflate(R.menu.menu_interval_choice)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.i_forMonth -> {
                    lifecycleScope.launch {
                        val monthNum: Int = localDate.monthValue
                        displayVisitsAll(monthNum)
                    }
                    true
                }

                R.id.i_allTime -> {
                    displayVisitsAll(null)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showSpecsMenu(view: View) {
        val popupMenu = PopupMenu(this@VisitsActivity, view)
        val menu = popupMenu.menu

        lifecycleScope.launch(Dispatchers.Main) {
            menu.add(Menu.NONE, 0, Menu.NONE, getString(R.string.all))

            specs.forEachIndexed { i, spec ->
                menu.add(Menu.NONE, i+1, Menu.NONE, spec.short.split(" ")[0])
            }

            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                val selectedItemId = menuItem.itemId - 1
                binding.tvTitle.text = menuItem.title
                if(menuItem.title == getString(R.string.all)) {
                    lifecycleScope.launch {
                        getVisitsWithProgressBar(localDate.monthValue)
                    }
                } else {
                    lifecycleScope.launch {
                        displayVisitsForSpec(specs[selectedItemId].id)
                    }
                }
                true
            }
            popupMenu.show()
        }
    }
}