package test.service;

import core.StatusTask;
import core.Task;
import managers.Managers;
import org.junit.jupiter.api.*;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    private InMemoryHistoryManager historyManager = Managers.getDefaultHistory();

    private Task task1;
    private Task task2;
    private Task task3;


    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
        task1 = new Task(1, "Task 1", "Task 1 description", StatusTask.NEW);
        task2 = new Task(2, "Task 2", "Task 2 description", null);
        task3 = new Task(3, "Task 3", "Task 3 description", null);
    }

    @Test
    @DisplayName("Добавление задачи в пустую историю")
    void testAddTaskToEmptyHistory() {
        historyManager.add(task1);
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task1, history.getFirst());
    }

    @Test
    @DisplayName("Добавление задач и удаление дубликатов")
    void testAddTaskAndRemoveDuplicate() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertEquals(task3, history.getLast());
    }
    @Test
    @DisplayName("Проверка ограничения на количество задач в истории")
    void testHistorySizeLimit() {
        for (int i = 0; i < InMemoryHistoryManager.MAX_SIZE + 1; i++) {
            historyManager.add(new Task(i, "Task " + i, "Task description", null));
        }
        List<Task> history = historyManager.getHistory();
        assertEquals(InMemoryHistoryManager.MAX_SIZE, history.size());
    }

    @Test
    @DisplayName("Проверка сохранения разных версий задачи")
    public void testHistoryVersions() {
        historyManager.add(task1);
        Task firstVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.NEW, firstVersion.getStatus());

        task1.setStatus(StatusTask.IN_PROGRESS);
        historyManager.add(task1);
        Task secondVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.IN_PROGRESS, secondVersion.getStatus());
    }

    @Test
    @DisplayName("Проверка, что HistoryManager сохраняет задачу при вызове метода getTaskById")
    public void testHistoryManagerIsUsed() {
        InMemoryTaskManager taskManager = Managers.getDefault();
        taskManager.createTask(task1);
        Task retievedTask = taskManager.getTaskById(1);
        historyManager.add(retievedTask);
        assertEquals(1, historyManager.getHistory().size());
    }

}