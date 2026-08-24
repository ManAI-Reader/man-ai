package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.highliuk.manai.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    /**
     * Searches conversations by title and message content. [escapedQuery] must
     * already have LIKE metacharacters escaped with a backslash. Title matches
     * rank first, then conversations with more matching messages, then most
     * recently updated.
     */
    @Query(
        """
        SELECT c.* FROM conversation c
        LEFT JOIN chat_message m ON m.conversationId = c.id
            AND m.content LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        GROUP BY c.id
        HAVING c.title LIKE '%' || :escapedQuery || '%' ESCAPE '\' OR COUNT(m.id) > 0
        ORDER BY (c.title LIKE '%' || :escapedQuery || '%' ESCAPE '\') DESC,
            COUNT(m.id) DESC,
            c.updatedAt DESC
        """
    )
    fun search(escapedQuery: String): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insert(entity: ConversationEntity): Long

    @Query("UPDATE conversation SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun delete(id: Long)
}
