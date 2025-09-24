package java.app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.app.entity.Epic;
import java.app.entity.StatusTask;
import java.app.entity.SubTask;
import java.app.entity.Task;
import java.app.management.Managers;
import java.app.management.TaskManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты валидации задач")
class ValidationExceptionTest {
    private ValidationException validator;
    private Task newTask;
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        validator = new ValidationException();
        taskManager = Managers.getDefault();

        Epic epic = new Epic(taskManager.generateId(), "Test Epic", "Test description");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при создании задачи с дублирующим именем")
    void tesValidateTaskCreation_shouldThrowException_whenDuplicateName() {
        // Given
        Task existingTask = new Task(taskManager.generateId(), "existingTask",
                "existingTask description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), "existingTask",
                "newTask description", StatusTask.NEW);
        Collection<Task> tasks = List.of(existingTask);

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateTaskForCreation(newTask, tasks));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при создании задачи с уникальным именем")
    void testValidateTaskCreation_shouldNotThrowException_whenUniqueName() {
        // Given
        Task existingTask = new Task(taskManager.generateId(), "existingTask",
                "existingTask description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), "newTask",
                "newTask description", StatusTask.NEW);
        Collection<Task> tasks = List.of(existingTask);

        // When & Then
        assertDoesNotThrow(() -> validator.validateTaskForCreation(newTask, tasks));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при создании подзадачи с несуществующим эпиком")
    void testValidateForSubtaskCreation_shouldThrowException_whenEpicDoesNotExist() {
        // Given
        Epic epic = new Epic(taskManager.generateId(), "Epic 1",
                "Epic 1 description");
        SubTask subtask = new SubTask(taskManager.generateId(), "Subtask 1",
                "Subtask 1 description", StatusTask.IN_PROGRESS, epic.getId() + 1);
        Map<Integer, Epic> epics = new HashMap<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateForSubTaskCreation(subtask, epics));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при создании подзадачи с существующим эпиком")
    void testValidateForSubtaskCreation_shouldNotThrowException_whenEpicExists() {
        // Given
        Epic epic = new Epic(taskManager.generateId(), "Epic 1",
                "Epic 1 description");
        SubTask subtask = new SubTask(taskManager.generateId(), "Subtask 1",
                "Subtask 1 description", StatusTask.IN_PROGRESS, epic.getId());
        Map<Integer, Epic> epics = new HashMap<>();
        epics.put(epic.getId(), epic);

        // When & Then
        assertDoesNotThrow(() -> validator.validateForSubTaskCreation(subtask, epics));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при обновлении несуществующей задачи")
    void testValidateTaskUpdate_shouldThrowException_whenTaskDoesNotExist() {
        // Given
        Task task = new Task(taskManager.generateId() + 1, "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Map<Integer, Task> tasks = new HashMap<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateTaskForUpdate(task, tasks));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при обновлении существующей задачи")
    void testValidateTaskUpdate_shouldNotThrowException_whenTaskExists() {
        // Given
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Map<Integer, Task> tasks = new HashMap<>();
        tasks.put(task.getId(), task);

        // When & Then
        assertDoesNotThrow(() -> validator.validateTaskForUpdate(task, tasks));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда id равен 0")
    void testValidatePositiveId_shouldThrowException_whenIdIsZero() {
        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validatePositiveId(0));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда id отрицательный")
    void testValidatePositiveId_shouldThrowException_whenIdIsNegative() {
        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validatePositiveId(-1));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, когда id положительный")
    void testValidatePositiveId_shouldNotThrowException_whenIdIsPositive() {
        // When & Then
        assertDoesNotThrow(() -> validator.validatePositiveId(1));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда объект равен null")
    void testValidateNotNull_shouldThrowException_whenObjectIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateNotNull(null, "entityName"));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, когда объект не равен null")
    void testValidateNotNull_shouldNotThrowException_whenObjectIsNotNull() {
        // When & Then
        assertDoesNotThrow(() -> validator.validateNotNull(new Object(), "entityName"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, если ID нет в Map")
    void testValidateIdExist_shouldThrowException_whenIdNotInMap() {
        // Given
        Map<?, ?> map = new HashMap<>();
        int id = 1;

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateIdExist(map, id));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, если ID есть в Map")
    void testValidateIdExist_shouldNotThrowException_whenIdInMap() {
        // Given
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();
        Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
        map.put(epicId, epic);

        // When & Then
        assertDoesNotThrow(() -> validator.validateIdExist(map, epicId));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при проверке существования эпика, если эпика нет")
    void testValidateEpicExist_shouldThrowException_whenEpicDoesNotExist() {
        // Given
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateEpicExist(map, epicId));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при проверке существования эпика, если эпик есть")
    void testValidateEpicExist_shouldNotThrowException_whenEpicExists() {
        // Given
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();
        Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
        map.put(epicId, epic);

        // When & Then
        assertDoesNotThrow(() -> validator.validateEpicExist(map, epicId));
    }

    @Test
    @DisplayName("Должен определять дубликат задачи по имени")
    void testValidateNotDuplicate_shouldDetectDuplicateByName() {
        // Given
        Task existingTask = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), existingTask.getName(),
                "Task 2 description", StatusTask.NEW);
        Collection<Task> tasks = List.of(existingTask);

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateNotDuplicate(newTask, tasks));
    }

    @Test
    @DisplayName("Не должен считать задачу дубликатом самой себя")
    void testValidateNotDuplicate_shouldNotDetectSelfAsDuplicate() {
        // Given
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Collection<Task> tasks = List.of(task);

        // When & Then
        assertDoesNotThrow(() -> validator.validateNotDuplicate(task, tasks));
    }

    @Test
    @DisplayName("Должен корректно определять одинаковые задачи")
    void testIsSameTask_shouldReturnTrueForSameTask() {
        // Given
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task task1 = new Task(task.getId(), task.getName(), task.getDescription(), task.getStatus());

        // When & Then
        assertTrue(validator.isSameTask(task, task1));
    }

    @Test
    @DisplayName("Должен корректно определять разные задачи")
    void testIsSameTask_shouldReturnFalseForDifferentTasks() {
        // Given
        Task task1 = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task task2 = new Task(taskManager.generateId(), "Task 2",
                "Task 2 description", StatusTask.NEW);

        // When & Then
        assertFalse(validator.isSameTask(task1, task2));
    }

    @Test
    @DisplayName("Должен обрабатывать пустую коллекцию задач при проверке на дубликат")
    void testValidateNotEmpty_shouldHandleEmptyCollection() {
        // Given
        Collection<Task> tasks = List.of();
        Task newTask = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.NEW);

        // When & Then
        assertDoesNotThrow(() -> validator.validateNotDuplicate(newTask, tasks));
    }

    @Test
    @DisplayName("Должен обрабатывать пустую Map при проверке на существование ID")
    void testValidateIdExist_shouldHandleEmptyMap() {
        // Given
        Map<Integer, Task> tasks = new HashMap<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateIdExist(tasks, 1));
    }

    @Test
    @DisplayName("должен корректно обрабатывать null коллекцию при проверке на дубликат")
    void testValidateNotDuplicate_shouldHandleNullCollection() {
        // Given
        Task newTask = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.NEW);

        // When & Then
        assertDoesNotThrow(() -> validator.validateNotDuplicate(newTask, null));
    }
}