package test.managers;

import managers.Managers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ManagersTest {
    private InMemoryTaskManager taskManager;
    private InMemoryHistoryManager historyManager;

    @Test
    @DisplayName("Проверка создания менеджера задач по умолчанию")
    void getDefault_ShouldReturnInMemoryTaskManagerInstance() {
        taskManager = Managers.getDefault();
        assertNotNull(taskManager);
        assertInstanceOf(InMemoryTaskManager.class, taskManager);
    }

    @Test
    @DisplayName("Проверка создания менеджера истории по умолчанию")
    void getDefaultHistory_ShouldReturnInMemoryHistoryManagerInstance() {
        historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager);
        assertInstanceOf(InMemoryHistoryManager.class, historyManager);
    }

}
