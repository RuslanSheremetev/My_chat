package com.mychat.app.data

import androidx.room.*

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatKey = :chatKey ORDER BY time ASC")
    fun getMessages(chatKey: String): List<MessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)
    
    @Query("DELETE FROM messages WHERE chatKey = :chatKey")
    fun deleteChat(chatKey: String)
    
    @Query("DELETE FROM messages")
    fun clearAll()
}
