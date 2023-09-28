package com.example.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class Crime(
    var title: String,
    var date: Date = Date(),
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var isSolved: Boolean = false,
    var suspect: String = "",
    var telNumberOfSuspect: String = "",
) {
    val photoFileName
        get() = "IMAGE$id.jpg"
}