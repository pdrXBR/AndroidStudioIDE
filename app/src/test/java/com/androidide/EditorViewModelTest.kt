package com.androidide

import com.androidide.ai.AiRepository
import com.androidide.data.model.WorkspaceSettings
import com.androidide.data.repository.FileRepository
import com.androidide.data.repository.SettingsRepository
import com.androidide.ui.editor.EditorViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: EditorViewModel
    private val fileRepository: FileRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk()
    private val aiRepository: AiRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.settings } returns flowOf(WorkspaceSettings())
        viewModel = EditorViewModel(fileRepository, settingsRepository, aiRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial nao tem tabs`() {
        val state = viewModel.uiState.value
        assertTrue(state.tabs.isEmpty())
        assertEquals(0, state.activeTabIndex)
    }

    @Test
    fun `openFile adiciona nova tab`() = runTest {
        val tmpFile = File.createTempFile("test", ".py").also { it.writeText("# test") }
        coEvery { fileRepository.readFile(any()) } returns Result.success("# test")

        viewModel.openFile(tmpFile)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.tabs.size)
        assertEquals(tmpFile.name, viewModel.uiState.value.tabs[0].name)
        tmpFile.delete()
    }

    @Test
    fun `closeTab remove tab corretamente`() = runTest {
        val tmpFile = File.createTempFile("test", ".py").also { it.writeText("") }
        coEvery { fileRepository.readFile(any()) } returns Result.success("")

        viewModel.openFile(tmpFile)
        advanceUntilIdle()

        val tabId = viewModel.uiState.value.tabs.first().id
        viewModel.closeTab(tabId)

        assertEquals(0, viewModel.uiState.value.tabs.size)
        tmpFile.delete()
    }

    @Test
    fun `setActiveTab atualiza indice ativo`() = runTest {
        val f1 = File.createTempFile("f1", ".py").also { it.writeText("") }
        val f2 = File.createTempFile("f2", ".py").also { it.writeText("") }
        coEvery { fileRepository.readFile(any()) } returns Result.success("")

        viewModel.openFile(f1); advanceUntilIdle()
        viewModel.openFile(f2); advanceUntilIdle()

        viewModel.setActiveTab(0)
        assertEquals(0, viewModel.uiState.value.activeTabIndex)

        f1.delete(); f2.delete()
    }
}
