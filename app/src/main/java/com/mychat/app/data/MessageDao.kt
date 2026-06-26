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
    
    @Query("DELETE FROM messages WHERE id IN (:ids)")
    fun deleteTempMessages(ids: List<String>)
    
    // Удаление старых сообщений (оставляем последние 500)
    @Query("DELETE FROM messages WHERE chatKey = :chatKey AND id NOT IN (SELECT id FROM messages WHERE chatKey = :chatKey ORDER BY time DESC LIMIT 500)")
    fun deleteOldMessages(chatKey: String)
}
