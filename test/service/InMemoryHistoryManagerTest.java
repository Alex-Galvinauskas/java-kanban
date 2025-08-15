package test.service;

import core.StatusTask;
import core.Task;
import managers.HistoryManager;
import managers.Managers;
import managers.TaskManager;
import org.junit.jupiter.api.*;
import service.InMemoryHistoryManager;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    private HistoryManager historyManager;
    private TaskManager taskManager;

    private Task task1;
    private Task task2;
    private Task task3;


    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
        taskManager = Managers.getDefault();

        // Инициализация задач для тестов
        task1 = new Task(1, "Task 1", "Task 1 description", StatusTask.NEW);
        task2 = new Task(2, "Task 2", "Task 2 description", null);
        task3 = new Task(3, "Task 3", "Task 3 description", null);
    }

    @Test
    @DisplayName("Добавление задачи в пустую историю")
    void testAddTaskToEmptyHistory() {
        //пустая история
        List<Task> emptyHistory = historyManager.getHistory();
        assertTrue(emptyHistory.isEmpty());

        //добавление задачи в историю
        historyManager.add(task1);

        //проверяем, что история содержит ровно одну задачу и это добавленная задача
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task1, history.getFirst());
    }

    @Test
    @DisplayName("Добавление нескольких задач")
    void testAddMultipleTasks() {
        //пустая история
        List<Task> emptyHistory = historyManager.getHistory();
        assertTrue(emptyHistory.isEmpty());

        //добавление задач в историю
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //проверяем, что история содержит все три задачи в правильном порядке
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertEquals(task1, history.get(0));
        assertEquals(task2, history.get(1));
        assertEquals(task3, history.get(2));
    }

    @Test
    @DisplayName("Дубликат должен перемещаться в конец истории")
    void add_shouldMoveDuplicateTaskToEnd() {
        //две задачи
        historyManager.add(task1);
        historyManager.add(task2);

        //добавляем дубликат
        historyManager.add(task1);

        //проверяем, что история содержит две задачи в правильном порядке
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertEquals(task2, history.get(0));
        assertEquals(task1, history.get(1));
    }

    @Test
    @DisplayName("Null задача не должна добавляться в историю")
    void add_shouldNotAddNullTask() {
        //пустая история

        //добавляем null задачу
        historyManager.add(null);

        //история должна быть пустой
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Удаление задачи из истории")
    void remove_shouldRemoveTaskFromHistory() {
        // добавляем три задачи
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //удаляем задачу
        historyManager.remove(task2.getId());

        //проверяем, что история содержит две задачи в правильном порядке
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertFalse(history.contains(task2));
        assertEquals(List.of(task1, task3), history);
    }

    @Test
    @DisplayName("Удаление несуществующей задачи из истории")
    void remove_shouldHandleNonExistingTask() {
        // добавляем одну задачу
        historyManager.add(task1);

        //удаляем несуществующую задачу
        historyManager.remove(999);

        //история остается неизменной
        assertEquals(1, historyManager.getHistory().size());
    }

    @Test
    @DisplayName("Проверка сохранения разных версий задачи")
    public void testHistoryVersions() {

        //добавляем задачу в исходном состоянии
        historyManager.add(task1);
        Task firstVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.NEW, firstVersion.getStatus());

        //изменяем статус задачи и добавляем ее снова
        task1.setStatus(StatusTask.IN_PROGRESS);
        historyManager.add(task1);

        //проверяем, что история содержит только измененную версию задачи
        Task secondVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.IN_PROGRESS, secondVersion.getStatus());
    }

    @Test
    @DisplayName("Очистка истории")
    void clear_shouldClearHistory() {
        //добавляем три задачи
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //очищаем историю
        historyManager.clear();

        //история должна быть пустой
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Проверка получения пустой истории")
    void testGetEmptyHistory() {
        //получаем пустую историю
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Проверка, что HistoryManager сохраняет задачу при вызове метода getTaskById")
    public void testHistoryManagerIsUsed() {

        //создаем задачу в менеджере
        taskManager.createTask(task1);

        //получаем задачу по идентификатору и добавляем ее в историю
        Task retievedTask = taskManager.getTaskById(1);
        historyManager.add(retievedTask);

        //проверяем, что история содержит эту задачу
        assertEquals(retievedTask, historyManager.getHistory().getFirst());
    }
}