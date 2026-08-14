package dev.gaphunter.jsontocodecompanion.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Real `update()` path through a constructed
 * [com.intellij.openapi.actionSystem.DataContext] -- same technique as
 * every other Gap Hunter Labs plugin built the night SDK_GOTCHAS.md
 * §17 was written.
 */
class GenerateClassFromJsonActionTest : BasePlatformTestCase() {

    private fun dataContextFromFixture() = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(CommonDataKeys.EDITOR, myFixture.editor)
        .add(CommonDataKeys.PSI_FILE, myFixture.file)
        .build()

    fun testActionIsEnabledWithARealSelection() {
        myFixture.configureByText("sample.json", "<selection>{ \"name\": \"Acme\" }</selection>")
        val action = GenerateClassFromJsonAction()
        val event = TestActionEvent.createTestEvent(action, dataContextFromFixture())
        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsDisabledWithNoSelection() {
        myFixture.configureByText("sample.json", "{ \"name\": \"Acme\" }")
        val action = GenerateClassFromJsonAction()
        val event = TestActionEvent.createTestEvent(action, dataContextFromFixture())
        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsDisabledWithNoEditorInTheDataContext() {
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val action = GenerateClassFromJsonAction()
        val event = TestActionEvent.createTestEvent(action, dataContext)
        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }
}
