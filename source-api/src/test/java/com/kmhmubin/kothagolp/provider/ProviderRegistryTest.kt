package com.kmhmubin.kothagolp.provider

import com.kmhmubin.kothagolp.domain.model.MainPageResult
import com.kmhmubin.kothagolp.domain.model.Novel
import com.kmhmubin.kothagolp.domain.model.NovelDetails
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProviderRegistryTest {

    @Before
    fun setUp() {
        MainProvider.clearAll()
    }

    @After
    fun tearDown() {
        MainProvider.clearAll()
    }

    @Test
    fun providers_initiallyEmpty() {
        assertTrue(MainProvider.getProviders().isEmpty())
    }

    @Test
    fun register_addsProviderToList() {
        MainProvider.register(FakeProvider("test-provider"))
        assertEquals(1, MainProvider.getProviders().size)
    }

    @Test
    fun register_multipleProviders_allPresent() {
        MainProvider.register(FakeProvider("p1"))
        MainProvider.register(FakeProvider("p2"))
        MainProvider.register(FakeProvider("p3"))
        val names = MainProvider.getProviders().map { it.name }
        assertTrue(names.contains("p1"))
        assertTrue(names.contains("p2"))
        assertTrue(names.contains("p3"))
    }

    @Test
    fun register_duplicateName_notAddedTwice() {
        MainProvider.register(FakeProvider("dup"))
        MainProvider.register(FakeProvider("dup"))
        assertEquals(1, MainProvider.getProviders().size)
    }

    @Test
    fun getProvider_existingName_returnsProvider() {
        MainProvider.register(FakeProvider("findme"))
        val found = MainProvider.getProvider("findme")
        assertTrue(found != null)
        assertEquals("findme", found!!.name)
    }

    @Test
    fun getProvider_missingName_returnsNull() {
        val found = MainProvider.getProvider("nonexistent")
        assertTrue(found == null)
    }

    @Test
    fun providersFlow_reflectsRegistrations() {
        val provider = FakeProvider("flow-test")
        MainProvider.register(provider)
        val flowValue = MainProvider.providersState().value
        assertTrue(flowValue.any { it.name == "flow-test" })
    }

    @Test
    fun clearAll_emptiesRegistry() {
        MainProvider.register(FakeProvider("a"))
        MainProvider.register(FakeProvider("b"))
        MainProvider.clearAll()
        assertTrue(MainProvider.getProviders().isEmpty())
        assertTrue(MainProvider.providersState().value.isEmpty())
    }

    private class FakeProvider(override val name: String) : MainProvider() {
        override val mainUrl = "https://fake.test/$name"
        override val hasMainPage = false

        override suspend fun loadMainPage(
            page: Int,
            orderBy: String?,
            tag: String?,
            extraFilters: Map<String, String>
        ): MainPageResult = MainPageResult(mainUrl, emptyList())

        override suspend fun search(query: String): List<Novel> = emptyList()

        override suspend fun load(url: String): NovelDetails? = null

        override suspend fun loadChapterContent(url: String): String? = null
    }
}
