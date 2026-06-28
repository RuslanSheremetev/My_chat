package com.mychat.app.data

import androidx.room.*

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_settings WHERE chatKey = :chatKey")
    fun getChatSettings(chatKey: String): ChatSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveChatSettings(settings: ChatSettings)
    
    @Query("UPDATE chat_settings SET isMuted = :muted WHERE chatKey = :chatKey")
    fun updateMute(chatKey: String, muted: Boolean)
    
    @Query("UPDATE chat_settings SET isBlocked = :blocked WHERE chatKey = :chatKey")
    fun updateBlocked(chatKey: String, blocked: Boolean)

    @Query("SELECT * FROM messages WHERE chatKey = :chatKey ORDER BY time ASC")
    fun getMessages(chatKey: String): List<MessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)
    
    @Query("DELETE FROM messages WHERE chatKey = :chatKey")
    fun deleteChat(chatKey: String)
    
    @Query("SELECT * FROM messages WHERE chatKey = :chatKey AND text LIKE '%' || :query || '%' ORDER BY time ASC")
    fun searchMessages(chatKey: String, query: String): List<MessageEntity>
    
    @Query("DELETE FROM messages")
    fun clearAll()
    
    @Query("DELETE FROM messages WHERE id IN (:ids)")
    fun deleteTempMessages(ids: List<String>)
    
    @Query("UPDATE messages SET status = 'sent' WHERE chatKey = :chatKey AND status = 'sending'")
    fun markSent(chatKey: String)
    
    // Удаление старых сообщений (оставляем последние 500)
    @Query("DELETE FROM messages WHERE chatKey = :chatKey AND id NOT IN (SELECT id FROM messages WHERE chatKey = :chatKey ORDER BY time DESC LIMIT 500)")
    fun deleteOldMessages(chatKey: String)

    @Query("UPDATE messages SET reactionsJson = :json WHERE id = :msgId")
    fun updateReactions(msgId: String, json: String)

}
