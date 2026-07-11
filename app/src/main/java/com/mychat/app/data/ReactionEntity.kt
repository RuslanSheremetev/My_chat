package com.mychat.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reactions")
data class ReactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val msgId: String,
    val emoji: String,
    val username: String
)
