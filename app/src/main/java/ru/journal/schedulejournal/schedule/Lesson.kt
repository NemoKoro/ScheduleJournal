package ru.journal.schedulejournal.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    @SerialName("subject_name")val name: String,
    @SerialName("lesson") val number: Int,
    @SerialName("started_at")val started: String,
    @SerialName("finished_at") val finished: String,
    @SerialName("teacher_name")val teacher: String
)

