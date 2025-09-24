package java.app.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.app.service.history.InMemoryHistoryManager;
import java.app.service.manager.InMemoryTaskManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Тесты менеджера задач")
class ManagersTest {

    @Test
    @DisplayName("Проверка создания менеджера задач по умолчанию")
    void testGetDefault_ShouldReturnInMemoryTaskManagerInstance() {
        // When
        TaskManager taskManager = Managers.getDefault();

        // Then
        assertNotNull(taskManager);
        assertInstanceOf(InMemoryTaskManager.class, taskManager);
    }

    @Test
    @DisplayName("Проверка создания менеджера истории по умолчанию")
    void testGetDefaultHistory_ShouldReturnInMemoryHistoryManagerInstance() {
        // When
        InMemoryHistoryManager historyManager = Managers.getDefaultHistory();

        // Then
        assertNotNull(historyManager);
        assertInstanceOf(InMemoryHistoryManager.class, historyManager);
    }
}