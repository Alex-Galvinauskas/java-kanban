package java.app.service.history;

import org.junit.jupiter.api.*;

import java.app.entity.StatusTask;
import java.app.entity.Task;
import java.app.management.HistoryManager;
import java.app.management.Managers;
import java.app.management.TaskManager;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты менеджера истории")
class InMemoryHistoryManagerTest {
    private HistoryManager historyManager;
    private TaskManager taskManager;
    private TestInfo testInfo;

    private Task task1;
    private Task task2;
    private Task task3;


    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());

        historyManager = new InMemoryHistoryManager();
        taskManager = Managers.getDefault();

        task1 = new Task(1, "Task 1", "Task 1 description", StatusTask.NEW);
        task2 = new Task(2, "Task 2", "Task 2 description", StatusTask.IN_PROGRESS);
        task3 = new Task(3, "Task 3", "Task 3 description", StatusTask.DONE);
    }

    @AfterEach
    void tearDown() {
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Добавление задачи в пустую историю")
    void testAddTaskToEmptyHistory() {
        //Given
        List<Task> emptyHistory = historyManager.getHistory();
        assertTrue(emptyHistory.isEmpty());

        //When
        historyManager.add(task1);

        //Then
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task1, history.getFirst());
    }

    @Test
    @DisplayName("Добавление нескольких задач")
    void testAddMultipleTasks() {
        //Given
        List<Task> emptyHistory = historyManager.getHistory();
        assertTrue(emptyHistory.isEmpty());

        //When
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //Then
        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertEquals(task1, history.get(0));
        assertEquals(task2, history.get(1));
        assertEquals(task3, history.get(2));
    }

    @Test
    @DisplayName("Дубликат должен перемещаться в конец истории")
    void testAdd_shouldMoveDuplicateTaskToEnd() {
        //Given
        historyManager.add(task1);
        historyManager.add(task2);

        //When
        historyManager.add(task1);

        //Then
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertEquals(task2, history.get(0));
        assertEquals(task1, history.get(1));
    }

    @Test
    @DisplayName("Null задача не должна добавляться в историю")
    void tetsAdd_shouldNotAddNullTask() {
        //Given & When
        historyManager.add(null);

        //Then
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Удаление задачи из истории")
    void testRemove_shouldRemoveTaskFromHistory() {
        //Given
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //When
        historyManager.remove(task2.getId());

        //Then
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertFalse(history.contains(task2));
        assertEquals(List.of(task1, task3), history);
    }

    @Test
    @DisplayName("Удаление несуществующей задачи из истории")
    void testRemove_shouldHandleNonExistingTask() {
        //Given
        historyManager.add(task1);

        //When
        historyManager.remove(999);

        //Then
        assertEquals(1, historyManager.getHistory().size());
    }

    @Test
    @DisplayName("Проверка сохранения разных версий задачи")
    void testHistoryVersions() {

        //Given
        historyManager.add(task1);
        Task firstVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.NEW, firstVersion.getStatus());

        //When
        task1.setStatus(StatusTask.IN_PROGRESS);
        historyManager.add(task1);

        //Then
        Task secondVersion = historyManager.getHistory().getFirst();
        assertEquals(StatusTask.IN_PROGRESS, secondVersion.getStatus());
    }

    @Test
    @DisplayName("Очистка истории")
    void testClear_shouldClearHistory() {
        //Given
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        //When
        historyManager.clear();

        //Then
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Проверка получения пустой истории")
    void testGetEmptyHistory() {
        //Then
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Проверка, что HistoryManager сохраняет задачу при вызове метода getTaskById")
    void testHistoryManagerIsUsed() throws IOException {

        //Given
        taskManager.createTask(task1);

        //When
        Optional<Task> retrievedTask = taskManager.getTaskById(1);
        historyManager.add(retrievedTask.orElse(null));

        //Then
        assertTrue(retrievedTask.isPresent(), "Задача должна существовать");
        assertEquals(retrievedTask.get(), historyManager.getHistory().getFirst());
    }
}