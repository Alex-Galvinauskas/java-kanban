package taskmanager.app.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.service.manager.InMemoryTaskManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Тесты менеджера задач")
class ManagersTest {

    @Test
    @DisplayName("Проверка создания менеджера задач по умолчанию")
    void testGetDefault_ShouldReturnInMemoryTaskManagerInstance() {

        //вызываем менеджер задач
        TaskManager taskManager = Managers.getDefault();

        //проверяем что объект создан и с правильным типом
        assertNotNull(taskManager);
        assertInstanceOf(InMemoryTaskManager.class, taskManager);
    }

    @Test
    @DisplayName("Проверка создания менеджера истории по умолчанию")
    void testGetDefaultHistory_ShouldReturnInMemoryHistoryManagerInstance() {

        //вызываем менеджер истории
        InMemoryHistoryManager historyManager = Managers.getDefaultHistory();

        //проверяем что объект создан и с правильным типом
        assertNotNull(historyManager);
        assertInstanceOf(InMemoryHistoryManager.class, historyManager);
    }
}
