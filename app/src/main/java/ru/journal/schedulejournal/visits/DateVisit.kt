package ru.journal.schedulejournal.visits

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DateVisit(
    @SerialName("date_visit") val date: String,
    @SerialName("spec_name") val name: String,
    @SerialName("class_work_mark") val mark: Int?,
    @SerialName("status_was") val status: Int,
    @SerialName("lesson_number") val number: Int,
    var short: String = "",
    var id: Int = 0
)

@Serializable
data class ShortName(
    val name: String,
    @SerialName("id") val id: Int,
    @SerialName("short_name") val short: String = ""
)