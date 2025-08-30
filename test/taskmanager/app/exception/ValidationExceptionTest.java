package taskmanager.app.exception;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.management.Managers;
import taskmanager.app.management.TaskManager;

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
        // Создаем задачу с дублирующим именем
        Task existingTask = new Task(taskManager.generateId(), "existingTask",
                "existingTask description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), "existingTask",
                "newTask description", StatusTask.NEW);
        // Создаем коллекцию с существующих задач
        Collection<Task> tasks = List.of(existingTask);
        // Проверяем, что выбрасывается исключение при создании задачи с дублирующим именем
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateTaskForCreation(newTask, tasks));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при создании задачи с уникальным именем")
    void testValidateTaskCreation_shouldNotThrowException_whenUniqueName() {
        // Создаем задачу с уникальным именем
        Task existingTask = new Task(taskManager.generateId(), "existingTask",
                "existingTask description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), "newTask",
                "newTask description", StatusTask.NEW);
        // Создаем коллекцию с существующими задачами
        Collection<Task> tasks = List.of(existingTask);
        // Проверяем, что не выбрасывается исключение при создании задачи с уникальным именем
        assertDoesNotThrow(() -> validator.validateTaskForCreation(newTask, tasks));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при создании подзадачи с несуществующим эпиком")
    void testValidateForSubtaskCreation_shouldThrowException_whenEpicDoesNotExist() {
        // Создаем подзадачу с несуществующим id
        Epic epic = new Epic(taskManager.generateId(), "Epic 1",
                "Epic 1 description");
        SubTask subtask = new SubTask(taskManager.generateId(), "Subtask 1",
                "Subtask 1 description", StatusTask.IN_PROGRESS, epic.getId() + 1);
        // Создаем коллекцию с существующими задачами
        Map<Integer, Epic> epics = new HashMap<>();
        // Проверяем, что выбрасывается исключение при создании подзадачи с несуществующим id
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateForSubTaskCreation(subtask, epics));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при создании подзадачи с существующим эпиком")
    void testValidateForSubtaskCreation_shouldNotThrowException_whenEpicExists() {
        // Создаем подзадачу с существующим id
        Epic epic = new Epic(taskManager.generateId(), "Epic 1",
                "Epic 1 description");
        SubTask subtask = new SubTask(taskManager.generateId(), "Subtask 1",
                "Subtask 1 description", StatusTask.IN_PROGRESS, epic.getId());
        // Создаем коллекцию с существующими задачами
        Map<Integer, Epic> epics = new HashMap<>();
        epics.put(epic.getId(), epic);
        // Проверяем, что не выбрасывается исключение при создании подзадачи с существующим id
        assertDoesNotThrow(() -> validator.validateForSubTaskCreation(subtask, epics));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при обновлении несуществующей задачи")
    void testValidateTaskUpdate_shouldThrowException_whenTaskDoesNotExist() {
        // Создаем задачу с несуществующим id
        Task task = new Task(taskManager.generateId() + 1, "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        // Создаем коллекцию с существующими задачами
        Map<Integer, Task> tasks = new HashMap<>();
        // Проверяем, что выбрасывается исключение при обновлении несуществующей задачи
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateTaskForUpdate(task, tasks));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при обновлении существующей задачи")
    void testValidateTaskUpdate_shouldNotThrowException_whenTaskExists() {
        // Создаем задачу с существующим id
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        // Создаем коллекцию с существующими задачами
        Map<Integer, Task> tasks = new HashMap<>();
        tasks.put(task.getId(), task);
        // Проверяем, что не выбрасывается исключение при обновлении существующей задачи
        assertDoesNotThrow(() -> validator.validateTaskForUpdate(task, tasks));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда id равен 0")
    void testValidatePositiveId_shouldThrowException_whenIdIsZero() {
        // Проверяем, что выбрасывается исключение при вызове метода с id равным 0
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validatePositiveId(0));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда id отрицательный")
    void testValidatePositiveId_shouldThrowException_whenIdIsNegative() {
        // Проверяем, что выбрасывается исключение при вызове метода с отрицательным id
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validatePositiveId(-1));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, когда id положительный")
    void testValidatePositiveId_shouldNotThrowException_whenIdIsPositive() {
        // Проверяем, что не выбрасывается исключение при вызове метода с положительным id
        assertDoesNotThrow(() -> validator.validatePositiveId(1));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, когда объект равен null")
    void testValidateNotNull_shouldThrowException_whenObjectIsNull() {
        // Проверяем, что выбрасывается исключение при вызове метода с null объектом
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateNotNull(null, "entityName"));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, когда объект не равен null")
    void testValidateNotNull_shouldNotThrowException_whenObjectIsNotNull() {
        // Проверяем, что не выбрасывается исключение при вызове метода с не null объектом
        assertDoesNotThrow(() -> validator.validateNotNull(new Object(), "entityName"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение, если ID нет в Map")
    void testValidateIdExist_shouldThrowException_whenIdNotInMap() {
        // Создаем пустую Map
        Map<?, ?> map = new HashMap<>();
        int id = 1;
        // Проверяем, что выбрасывается исключение при вызове метода с несуществующим ID
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateIdExist(map, id));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение, если ID есть в Map")
    void testValidateIdExist_shouldNotThrowException_whenIdInMap() {
        //создаем map и добавляем эпик с id
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();
        Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
        map.put(epicId, epic);
        //вызываем валидацию и проверяем что не выбрасывается исключение
        assertDoesNotThrow(() -> validator.validateIdExist(map, epicId));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при проверке существования эпика, если эпика нет")
    void testValidateEpicExist_shouldThrowException_whenEpicDoesNotExist() {
        //создаем пустую map
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();
        //вызываем валидацию и проверяем что выбрасывается исключение
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateEpicExist(map, epicId));
    }

    @Test
    @DisplayName("Не должен выбрасывать исключение при проверке существования эпика, если эпик есть")
    void testValidateEpicExist_shouldNotThrowException_whenEpicExists() {
        //создаем пустую map с id для эпика и добавляем его в map
        Map<Integer, Epic> map = new HashMap<>();
        int epicId = taskManager.generateId();
        Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
        map.put(epicId, epic);
        //вызываем валидацию и проверяем что не выбрасывается исключение
        assertDoesNotThrow(() -> validator.validateEpicExist(map, epicId));
    }

    @Test
    @DisplayName("Должен определять дубликат задачи по имени")
    void testValidateNotDuplicate_shouldDetectDuplicateByName() {
        //создаем две задачи с одинаковым именем
        Task existingTask = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task newTask = new Task(taskManager.generateId(), existingTask.getName(),
                "Task 2 description", StatusTask.NEW);
        //создаем коллекцию с существующей задачей
        Collection<Task> tasks = List.of(existingTask);
        //вызываем валидацию и проверяем что выбрасывается исключение
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateNotDuplicate(newTask, tasks));
    }

    @Test
    @DisplayName("Не должен считать задачу дубликатом самой себя")
    void testValidateNotDuplicate_shouldNotDetectSelfAsDuplicate() {
        //создаем задачу
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        //создаем коллекцию с задачей
        Collection<Task> tasks = List.of(task);
        //вызываем валидацию и проверяем что не выбрасывается исключение
        assertDoesNotThrow(() -> validator.validateNotDuplicate(task, tasks));
    }

    @Test
    @DisplayName("Должен корректно определять одинаковые задачи")
    void testIsSameTask_shouldReturnTrueForSameTask() {
        //создаем копию задачи
        Task task = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task task1 = new Task(task.getId(), task.getName(), task.getDescription(), task.getStatus());
        //вызываем валидацию и проверяем что возвращается true
        assertTrue(validator.isSameTask(task, task1));
    }

    @Test
    @DisplayName("Должен корректно определять разные задачи")
    void testIsSameTask_shouldReturnFalseForDifferentTasks() {

        //создаем другую задачу
        Task task1 = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.IN_PROGRESS);
        Task task2 = new Task(taskManager.generateId(), "Task 2",
                "Task 2 description", StatusTask.NEW);
        //вызываем валидацию и проверяем что возвращается false
        assertFalse(validator.isSameTask(task1, task2));
    }

    @Test
    @DisplayName("Должен обрабатывать пустую коллекцию задач при проверке на дубликат")
    void testValidateNotEmpty_shouldHandleEmptyCollection() {

        //создаем пустую коллекцию задач
        Collection<Task> tasks = List.of();
        //вызываем валидацию и проверяем что не выбрасывается исключение
        assertDoesNotThrow(() -> validator.validateNotDuplicate(newTask, tasks));
    }

    @Test
    @DisplayName("Должен обрабатывать пустую Map при проверке на существование ID")
    void testValidateIdExist_shouldHandleEmptyMap() {
        //создаем пустую Map
        Map<Integer, Task> tasks = new HashMap<>();
        //вызываем валидацию и проверяем что выбрасывается исключение
        assertThrows(IllegalArgumentException.class, ()
                -> validator.validateIdExist(tasks, 1));
    }

    @Test
    @DisplayName("должен корректно обрабатывать null коллекцию при проверке на дубликат")
    void testValidateNotDuplicate_shouldHandleNullCollection() {

        //создаем новую задачу
        Task newTask = new Task(taskManager.generateId(), "Task 1",
                "Task 1 description", StatusTask.NEW);
        //вызываем валидацию и проверяем что не выбрасывается исключение
        assertDoesNotThrow(() -> validator.validateNotDuplicate(newTask, null));
    }

}