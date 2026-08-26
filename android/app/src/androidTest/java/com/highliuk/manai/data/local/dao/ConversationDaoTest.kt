package com.highliuk.manai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.entity.ChatMessageEntity
import com.highliuk.manai.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConversationDaoTest {

    private lateinit var database: ManAiDatabase
    private lateinit var dao: ConversationDao
    private lateinit var messageDao: ChatMessageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ManAiDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.conversationDao()
        messageDao = database.chatMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertConversation(title: String, updatedAt: Long = 0L): Long =
        dao.insert(
            ConversationEntity(
                title = title,
                mangaId = null,
                pageIndex = null,
                regionIndex = null,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            )
        )

    private suspend fun insertMessage(conversationId: Long, content: String) {
        messageDao.insert(
            ChatMessageEntity(
                conversationId = conversationId,
                role = "user",
                content = content,
                timestamp = 0L,
            )
        )
    }

    @Test
    fun search_matchesOnTitleOnly() = runTest {
        val id = insertConversation("Grammar question")
        insertConversation("Vocabulary")

        val result = dao.search("grammar").first()

        assertEquals(listOf(id), result.map { it.id })
    }

    @Test
    fun search_matchesOnMessageBodyOnly() = runTest {
        val id = insertConversation("Untitled")
        insertMessage(id, "What does kanji mean here?")
        insertConversation("Other")

        val result = dao.search("kanji").first()

        assertEquals(listOf(id), result.map { it.id })
    }

    @Test
    fun search_ranksTitleMatchesBeforeBodyMatches() = runTest {
        val bodyMatchId = insertConversation("Other", updatedAt = 200L)
        insertMessage(bodyMatchId, "talking about grammar")
        val titleMatchId = insertConversation("Grammar", updatedAt = 100L)

        val result = dao.search("grammar").first()

        assertEquals(listOf(titleMatchId, bodyMatchId), result.map { it.id })
    }

    @Test
    fun search_ranksByMatchingMessageCountWhenTitleRankTies() = runTest {
        val oneMatchId = insertConversation("A", updatedAt = 200L)
        insertMessage(oneMatchId, "grammar once")
        val twoMatchesId = insertConversation("B", updatedAt = 100L)
        insertMessage(twoMatchesId, "grammar first")
        insertMessage(twoMatchesId, "grammar second")

        val result = dao.search("grammar").first()

        assertEquals(listOf(twoMatchesId, oneMatchId), result.map { it.id })
    }

    @Test
    fun search_ranksByUpdatedAtWhenEverythingElseTies() = runTest {
        val olderId = insertConversation("Grammar old", updatedAt = 100L)
        val newerId = insertConversation("Grammar new", updatedAt = 200L)

        val result = dao.search("grammar").first()

        assertEquals(listOf(newerId, olderId), result.map { it.id })
    }

    @Test
    fun search_excludesConversationsWithoutAnyMatch() = runTest {
        val id = insertConversation("Unrelated")
        insertMessage(id, "nothing to see")

        val result = dao.search("grammar").first()

        assertEquals(emptyList<Long>(), result.map { it.id })
    }

    @Test
    fun search_isCaseInsensitiveForAscii() = runTest {
        val id = insertConversation("GRAMMAR TIME")

        val result = dao.search("grammar").first()

        assertEquals(listOf(id), result.map { it.id })
    }

    @Test
    fun search_treatsEscapedPercentAsLiteral() = runTest {
        val literalId = insertConversation("Discount 50% off")
        insertConversation("Discount 50 units off")

        val result = dao.search("50\\%").first()

        assertEquals(listOf(literalId), result.map { it.id })
    }

    @Test
    fun search_treatsEscapedUnderscoreAsLiteral() = runTest {
        val literalId = insertConversation("snake_case naming")
        insertConversation("snakeXcase naming")

        val result = dao.search("snake\\_case").first()

        assertEquals(listOf(literalId), result.map { it.id })
    }
}
