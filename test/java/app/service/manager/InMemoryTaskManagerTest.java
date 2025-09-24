package java.app.service.manager;

import org.junit.jupiter.api.*;

import java.app.entity.Epic;
import java.app.entity.StatusTask;
import java.app.entity.SubTask;
import java.app.entity.Task;
import java.app.management.Managers;
import java.app.management.TaskManager;
import java.app.service.history.InMemoryHistoryManager;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты менеджера задач в оперативной памяти")
class InMemoryTaskManagerTest {
    private TaskManager taskManager;
    private InMemoryHistoryManager historyManager;
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());
        taskManager = new InMemoryTaskManager();
        historyManager = new InMemoryHistoryManager();
    }

    @AfterEach
    void tearDown() {
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("Тесты менеджера задач")
    class ManagersTest {

        @Test
        @DisplayName("getDefault() возвращает проинициализированный InMemoryTaskManager")
        void testGetDefault_shouldReturnInitializedInMemoryTaskManager() {
            // When
            taskManager = Managers.getDefault();

            // Then
            assertNotNull(taskManager);
        }

        @Test
        @DisplayName("getDefaultHistory() возвращает проинициализированный HistoryManager")
        void testGetDefaultHistory_shouldReturnInitializedHistoryManager() {
            // When
            historyManager = Managers.getDefaultHistory();

            // Then
            assertNotNull(historyManager);
        }
    }

    @Nested
    @DisplayName("Тесты генерации ID")
    class GeneratedIdTest {

        @Test
        @DisplayName("Генерация ID: должен генерироваться уникальный ID")
        void testGenerateId_shouldGenerateUniqueId() {
            // When
            int firstId = taskManager.generateId();
            int secondId = taskManager.generateId();

            // Then
            assertEquals(firstId + 1, secondId);
        }
    }

    @Nested
    @DisplayName("Тесты создания задач")
    class CreateTaskTest {
        private Epic epic;
        private int epicId;

        @BeforeEach
        void setUpCreateTask() throws IOException {
            // Given
            epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            epicId = taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Создание задачи: должна создаваться и возвращаться задача с ID")
        void testCreateTask_shouldCreateAndReturnTaskWithId() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);

            // When
            int taskId = taskManager.createTask(task);

            // Then
            assertNotEquals(0, taskId, "ID задачи не должен быть равен 0");
            Optional<Task> createdTask = taskManager.getTaskById(taskId);
            assertTrue(createdTask.isPresent());
            assertEquals(task, createdTask.get());
        }

        @Test
        @DisplayName("Создание задачи: должно выбрасываться исключение, когда задача null")
        void testCreateTask_shouldThrowWhenTaskNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.createTask(null));
        }

        @Test
        @DisplayName("Создание эпика: должен создаваться и возвращаться эпик с ID")
        void testCreateEpic_shouldCreateAndReturnEpicWithId() {
            // Then
            assertNotEquals(0, epicId, "ID эпика не должен быть равен 0");
            Optional<Epic> createdEpic = taskManager.getEpicById(epicId);
            assertTrue(createdEpic.isPresent());
            assertEquals(epic, createdEpic.get());
        }

        @Test
        @DisplayName("Эпик не может быть добавлен в самого себя как подзадача")
        void testEpicCannotAddItselfAsSubtask() {
            // Given
            SubTask invalidSubTask = new SubTask(epicId, "Invalid Subtask",
                    "Invalid Subtask description", StatusTask.NEW, epicId);

            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.createSubTask(invalidSubTask));
        }

        @Test
        @DisplayName("Создание подзадачи: должна создаваться и возвращаться подзадача с ID")
        void testCreateSubTask_shouldCreateAndReturnsSubtaskWitchId() throws IOException {
            // Given
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, epicId);

            // When
            int subTaskId = taskManager.createSubTask(subTask);

            // Then
            assertNotEquals(0, subTaskId, "ID подзадачи не должен быть равен 0");

            Optional<SubTask> createdSubTask = taskManager.getSubTaskById(subTaskId);
            assertTrue(createdSubTask.isPresent(), "Подзадача должна существовать");
            assertEquals(subTask, createdSubTask.get(), "Подзадача создана");

            assertTrue(taskManager.getSubTasksByEpicId(epicId).contains(subTask),
                    "Подзадача добавлена в список");
        }

        @Test
        @DisplayName("Подзадача не может быть добавлена в саму себя")
        void testSubTaskCannotAddItselfAsParent() {
            // Given
            int invalidEpicId = taskManager.generateId();
            SubTask subTask = new SubTask(invalidEpicId, "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, invalidEpicId);

            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.createSubTask(subTask));
        }

        @Test
        @DisplayName("Должно выбрасываться исключение, когда эпика не существует")
        void testCreateSubTask_shouldThrowWhenEpicNotExist() {
            // Given
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, 999);

            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.createSubTask(subTask));
        }
    }

    @Nested
    @DisplayName("Тесты методов получения задач, подзадач и эпиков")
    class GetTaskAndSubTaskAndEpicTest {
        private Task task1;
        private Task task2;

        @BeforeEach
        void setUpGetTask() throws IOException {
            // Given
            task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            taskManager.createTask(task1);
            taskManager.createTask(task2);
        }

        @Test
        @DisplayName("должен возвращаться пустой список, если задач нет")
        void testGetAllTasks_shouldReturnEmptyListWhenNoTasks() throws IOException {
            // When
            taskManager.deleteAllTasks();
            taskManager.getAllTasks();

            // Then
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("должны возвращаться все созданные задачи")
        void testGetAllTasks_shouldReturnAllCreatedTasks() {
            // When
            Collection<Task> allTasks = taskManager.getAllTasks();

            // Then
            assertEquals(2, allTasks.size());
            assertTrue(allTasks.contains(task1));
            assertTrue(allTasks.contains(task2));
        }

        @Test
        @DisplayName("должен возвращаться empty, если задача не найдена")
        void testGetTaskById_shouldReturnNullWhenTaskNotFound() {
            // Given
            int invalidId = 999;

            // When
            Optional<Task> task = taskManager.getTaskById(invalidId);

            // Then
            assertTrue(task.isEmpty());
        }

        @Test
        @DisplayName("Подзадача с несуществующим ID должна возвращаться empty")
        void testGetSubTaskById_shouldReturnEmptyWhenSubTaskNotFound() {
            // Given
            int invalidId = 999;

            // When
            Optional<SubTask> subTask = taskManager.getSubTaskById(invalidId);

            // Then
            assertTrue(subTask.isEmpty());
        }

        @Test
        @DisplayName("Эпик с несуществующим ID должен возвращаться empty")
        void testGetEpicById_shouldReturnNullWhenEpicNotFound() {
            // Given
            int invalidId = 999;

            // When
            Optional<Epic> epic = taskManager.getEpicById(invalidId);

            // Then
            assertTrue(epic.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты метода getSubTasksByEpicId")
    class GetSubTasksByEpicIdTest {
        @Test
        @DisplayName("должен возвращаться пустой список, если подзадач нет")
        void testGetSubTasksByEpicId_shouldReturnEmptyListWhenSubtasks() throws IOException {
            // Given
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);

            // When
            List<SubTask> subTasks = taskManager.getSubTasksByEpicId(epic.getId());

            // Then
            assertTrue(subTasks.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты методов обновления задач, подзадач и эпиков")
    class UpdateTaskAndEpicAndSubtaskTest {
        private Task task;
        private int taskId;
        private int epicId;
        private SubTask subTaskNew;
        private SubTask subTaskInProgress;
        private SubTask subTaskDone;

        @BeforeEach
        void setUpdateTest() throws IOException {
            // Given
            task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            epicId = taskManager.createEpic(epic);

            subTaskNew = new SubTask(taskManager.generateId(), "SubTask 1",
                    "SubTask 1 description", StatusTask.NEW, epicId);
            subTaskInProgress = new SubTask(taskManager.generateId(), "SubTask 2",
                    "SubTask 2 description", StatusTask.IN_PROGRESS, epicId);
            subTaskDone = new SubTask(taskManager.generateId(), "SubTask 3",
                    "SubTask 3 description", StatusTask.DONE, epicId);
        }

        @Test
        @DisplayName("должна обновляться существующая задача")
        void testUpdateTask_shouldUpdateExistingTask() throws IOException {
            // Given
            Task updatedTask = new Task(task.getId(), "Updated Task 1",
                    "Updated Task 1 description", StatusTask.IN_PROGRESS);

            // When
            taskManager.updateTask(updatedTask);

            // Then
            Optional<Task> result = taskManager.getTaskById(taskId);
            assertTrue(result.isPresent());
            assertEquals(updatedTask, result.get());
        }

        @Test
        @DisplayName("должно выбрасывать исключение когда задачи не существует")
        void tetsUpdateTask_shouldThrowWhenTaskNotExist() {
            // Given
            task.setId(taskId + 1);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(task));
        }

        @Test
        @DisplayName("Статус эпика NEW, если все подзадачи NEW")
        void testEpicStatusShouldBeNewWhenAllSubtasksNew() throws IOException {
            // Given
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.NEW, epicId);

            // When
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTask2);

            // Then
            Optional<Epic> epic = taskManager.getEpicById(epicId);
            assertTrue(epic.isPresent());
            assertEquals(StatusTask.NEW, epic.get().getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если подзадачи разных статусов (NEW и DONE)")
        void testEpicStatusShouldBeInProgressWhenSubtasksMixed() throws IOException {
            // When
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTaskDone);

            // Then
            Optional<Epic> epic = taskManager.getEpicById(epicId);
            assertTrue(epic.isPresent());
            assertEquals(StatusTask.IN_PROGRESS, epic.get().getStatus());
        }

        @Test
        @DisplayName("Статус эпика DONE, если все подзадачи DONE")
        void testEpicStatusShouldBeDoneWhenAllSubtasksDone() throws IOException {
            // When
            taskManager.createSubTask(subTaskDone);
            taskManager.createSubTask(subTaskDone);

            // Then
            Optional<Epic> epic = taskManager.getEpicById(epicId);
            assertTrue(epic.isPresent());
            assertEquals(StatusTask.DONE, epic.get().getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если хотя бы одна подзадача IN_PROGRESS")
        void testEpicStatusShouldBeInProgressWhenAnySubtaskInProgress() throws IOException {
            // When
            taskManager.createSubTask(subTaskInProgress);
            taskManager.createSubTask(subTaskNew);

            // Then
            Optional<Epic> epic = taskManager.getEpicById(epicId);
            assertTrue(epic.isPresent());
            assertEquals(StatusTask.IN_PROGRESS, epic.get().getStatus());
        }

        @Test
        @DisplayName("Обновление подзадачи с несуществующим эпиком")
        void testUpdateSubTask_shouldThrowWhenEpicNotExist() {
            // Given
            subTaskNew = new SubTask(subTaskNew.getId(), "SubTask 1",
                    "SubTask 1 description", StatusTask.NEW, epicId + 1);

            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.updateSubTask(subTaskNew));
        }

        @Test
        @DisplayName("Обновление подзадачи с null выбросит исключение")
        void testUpdateSubTask_shouldThrowWhenSubTaskIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.updateSubTask(null));
        }
    }

    @Nested
    @DisplayName("Тесты методов удаления задач, подзадач и эпиков")
    class DeleteTaskAndEpicAndSubtaskTest {
        int taskId;
        int epicId;
        int subTaskId;

        @BeforeEach
        void setUpDeleteTest() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            epicId = taskManager.createEpic(epic);

            SubTask subTask = new SubTask(taskManager.generateId(), "SubTask 1",
                    "Описание п", StatusTask.NEW, epic.getId());
            subTaskId = taskManager.createSubTask(subTask);

            Task taskForHistory1 = new Task(taskManager.generateId(), "Task for history 1",
                    "Task for history 1 description", StatusTask.NEW);
            Task taskForHistory2 = new Task(taskManager.generateId(), "Task for history 2",
                    "Task for history 2 description", StatusTask.IN_PROGRESS);
            taskManager.createTask(taskForHistory1);
            taskManager.createTask(taskForHistory2);
            taskManager.getTaskById(taskForHistory1.getId());
            taskManager.getTaskById(taskForHistory2.getId());
        }

        @Test
        @DisplayName("должно завершаться без ошибок")
        void testDeleteNonExistentTask_shouldNotThrow() {
            // When & Then
            assertDoesNotThrow(() -> taskManager.deleteTaskById(999));
            assertTrue(taskManager.getTaskById(999).isEmpty());
        }

        @Test
        @DisplayName("должно выдать исключение")
        void testDeleteNonExistentEpic_shouldThrow() {
            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.deleteEpicById(999));
            assertTrue(taskManager.getEpicById(999).isEmpty());
        }

        @Test
        @DisplayName("должно завершаться без ошибок")
        void testDeleteNonExistentSubtask_shouldNotThrow() {
            // When & Then
            assertDoesNotThrow(() -> taskManager.deleteSubTaskById(999));
            assertTrue(taskManager.getSubTaskById(999).isEmpty()); // Изменено на isEmpty()
        }

        @Test
        @DisplayName("должна удаляться задача")
        void testDeleteTaskById_shouldRemoveTask() throws IOException {
            // When
            taskManager.deleteTaskById(taskId);

            // Then
            assertTrue(taskManager.getTaskById(taskId).isEmpty());
        }

        @Test
        @DisplayName("должны удаляться все задачи")
        void testDeleteAllTasks_shouldRemoveAllTasks() throws IOException {
            // When
            taskManager.deleteAllTasks();

            // Then
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("должен удаляться эпик и его подзадачи")
        void testDeleteEpicById_shouldRemoveAllSubtasks() throws IOException {
            // When
            taskManager.deleteEpicById(epicId);

            // Then
            assertTrue(taskManager.getEpicById(epicId).isEmpty());
            assertTrue(taskManager.getSubTaskById(subTaskId).isEmpty());
        }

        @Test
        @DisplayName("должна удаляться подзадача и обновляться эпик")
        void testDeleteSubTaskById_shouldRemoveSubtaskAndUpdateEpic() throws IOException {
            // When
            taskManager.deleteSubTaskById(subTaskId);

            // Then
            assertTrue(taskManager.getSubTaskById(subTaskId).isEmpty());
            assertTrue(taskManager.getSubTasksByEpicId(epicId).isEmpty());
        }

        @Test
        @DisplayName("история просмотров должна очищаться")
        void testDeleteAllTasks_shouldClearHistory() throws IOException {
            // When
            taskManager.deleteAllTasks();

            // Then
            assertTrue(taskManager.getAllTasks().isEmpty());
            assertTrue(historyManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление всех эпиков должно удалять все подзадачи")
        void testDeleteAllEpics_shouldRemoveAllSubtasks() throws IOException {
            // When
            taskManager.deleteAllEpics();

            // Then
            assertTrue(taskManager.getAllSubTasks().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты равенства задач")
    class TaskEqualityTest {
        private Task task1;
        private Task task2;
        private Epic epic1;
        private Epic epic2;
        private SubTask subtask1;
        private SubTask subtask2;

        @BeforeEach
        void setUpEqualityTest() {
            // Given
            task1 = new Task(1, "Task 1", "Task 1 description", StatusTask.NEW);
            task2 = new Task(1, "Task 2", "Task 2 description", StatusTask.DONE);

            epic1 = new Epic(1, "Epic 1", "Epic 1 description");
            epic2 = new Epic(1, "Epic 2", "Epic 2 description");

            subtask1 = new SubTask(1, "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, 1);
            subtask2 = new SubTask(1, "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, 1);
        }

        @Test
        @DisplayName("Задачи равны, если имеют одинаковый ID")
        void testTasksWithSameIdShouldBeEqual() {
            // Then
            assertEquals(task1, task2);
            assertEquals(task1.hashCode(), task2.hashCode());
        }

        @Test
        @DisplayName("Эпики равны, если имеют одинаковый ID")
        void testEpicsWithSameIdShouldBeEqual() {
            // Then
            assertEquals(epic1, epic2);
            assertEquals(epic1.hashCode(), epic2.hashCode());
        }

        @Test
        @DisplayName("Подзадачи равны, если имеют одинаковый ID")
        void testSubtasksWithSameIdShouldBeEqual() {
            // Then
            assertEquals(subtask1, subtask2);
            assertEquals(subtask1.hashCode(), subtask2.hashCode());
        }
    }

    @Nested
    @DisplayName("Тесты истории просмотров")
    class HistoryTest {

        @Test
        @DisplayName("Получение истории: должны возвращаться просмотренные задачи")
        void testGetHistory_shouldReturnViewedTasks() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            // When
            Optional<Task> retrievedTask = taskManager.getTaskById(taskId);
            Optional<Epic> retrievedEpic = taskManager.getEpicById(epicId);
            Collection<Task> history = taskManager.getHistory();

            // Then
            assertEquals(2, history.size());
            assertTrue(retrievedTask.isPresent());
            assertTrue(retrievedEpic.isPresent());
            assertTrue(history.contains(retrievedTask.get()));
            assertTrue(history.contains(retrievedEpic.get()));
        }


        @Test
        @DisplayName("Повторный просмотр задачи: история не должна дублироваться")
        void testShouldNotDuplicateHistoryWhenTaskViewedAgain() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            // When
            Optional<Task> retrievedTask1 = taskManager.getTaskById(taskId);

            // Then
            Collection<Task> history = taskManager.getHistory();
            assertEquals(1, history.size());
            assertTrue(retrievedTask1.isPresent());
            assertEquals(retrievedTask1.get(), history.iterator().next());
        }

        @Test
        @DisplayName("Удаление задачи: задача должна удаляться из истории")
        void testShouldRemoveTaskFromHistoryWhenTaskDeleted() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);
            taskManager.getTaskById(taskId);

            // When
            taskManager.deleteTaskById(taskId);

            // Then
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление эпика: эпик и его подзадачи должны удаляться из истории")
        void testShouldRemoveEpicAndSubtasksFromHistoryWhenEpicDeleted() throws IOException {
            // Given
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.IN_PROGRESS, epicId);

            taskManager.getEpicById(epicId);
            taskManager.getSubTaskById(subTask1.getId());
            taskManager.getSubTaskById(subTask2.getId());

            // When
            taskManager.deleteEpicById(epicId);

            // Then
            Collection<Task> historyAfter = taskManager.getHistory();
            assertEquals(0, historyAfter.size());
        }

        @Test
        @DisplayName("Удаление подзадачи: подзадача должна удаляться из истории")
        void testShouldRemoveSubtaskFromHistoryWhenSubtaskDeleted() throws IOException {
            // Given
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            int subTaskId = taskManager.createSubTask(subTask);

            // When
            taskManager.getSubTaskById(subTaskId);
            taskManager.deleteSubTaskById(subTaskId);

            // Then
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление задачи не должно влиять на другие задачи в истории")
        void testShouldNotAffectOtherTasksInHistoryWhenDeletingOneTask() throws IOException {
            // Given
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);

            // When
            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);
            taskManager.deleteTaskById(taskId2);

            // Then
            assertEquals(1, taskManager.getHistory().size());
            assertTrue(taskManager.getHistory().contains(task1));
        }

        @Test
        @DisplayName("Очистка всех задач")
        void testShouldClearHistoryWhenAllTasksDeleted() throws IOException {
            // Given
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);

            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);

            // When
            taskManager.deleteAllTasks();

            // Then
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех эпиков")
        void testShouldClearHistoryWhenAllEpicsDeleted() throws IOException {
            // Given
            Epic epic1 = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            Epic epic2 = new Epic(taskManager.generateId(), "Epic 2", "Epic 2 description");

            int epicId1 = taskManager.createEpic(epic1);
            int epicId2 = taskManager.createEpic(epic2);

            taskManager.getEpicById(epicId1);
            taskManager.getEpicById(epicId2);

            // When
            taskManager.deleteAllEpics();

            // Then
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех подзадач")
        void testShouldClearHistoryWhenAllSubtasksDeleted() throws IOException {
            // Given
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.IN_PROGRESS, epicId);

            int subTaskId1 = taskManager.createSubTask(subTask1);
            int subTaskId2 = taskManager.createSubTask(subTask2);

            taskManager.getSubTaskById(subTaskId1);
            taskManager.getSubTaskById(subTaskId2);

            // When
            taskManager.deleteAllSubTasks();

            // Then
            assertTrue(taskManager.getHistory().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты неизменности задач")
    public class CheckingTheImmutabilityOfTasksTest {
        private int taskId;
        private int epicId;
        private int subTaskId;

        @BeforeEach
        void setUpImmutabilityTest() throws IOException {
            // Given
            taskId = taskManager.generateId();
            epicId = taskManager.generateId();
            subTaskId = taskManager.generateId();
            Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Задача остается неизменной при добавлении в менеджер")
        void testTaskRemainsUnchangedWhenAddedToManager() throws IOException {
            // Given
            Task original = new Task(taskId, "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task copy = new Task(original.getId(), original.getName(),
                    original.getDescription(), original.getStatus());

            // When
            taskManager.createTask(original);

            // Then
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
        }

        @Test
        @DisplayName("Эпик остается неизменной при добавлении в менеджер")
        void testEpicRemainsUnchangedWhenAddedToManager() throws IOException {
            // Given
            Epic original = new Epic(epicId, "Epic 1", "Epic 1 description");
            Epic copy = new Epic(original.getId(), original.getName(), original.getDescription());

            // When
            taskManager.createEpic(original);

            // Then
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
        }

        @Test
        @DisplayName("Подзадача остается неизменной при добавлении в менеджер")
        void testSubtaskRemainsUnchangedWhenAddedToManager() throws IOException {
            // Given
            SubTask original = new SubTask(subTaskId, "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask copy = new SubTask(original.getId(), original.getName(),
                    original.getDescription(), original.getStatus(), original.getEpicId());

            // When
            taskManager.createSubTask(original);

            // Then
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
            assertEquals(copy.getEpicId(), original.getEpicId());
        }
    }


        @Nested
        @DisplayName("Тесты временных параметров задач")
        class TaskTimeTests {

            @Test
            @DisplayName("Создание задачи с временными параметрами")
            void testCreateTaskWithTimeParameters() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task with time",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                // When
                int taskId = taskManager.createTask(task);
                Optional<Task> createdTask = taskManager.getTaskById(taskId);

                // Then
                assertTrue(createdTask.isPresent());
                assertEquals(LocalDateTime.of(2025, 9, 8, 10, 0),
                        createdTask.get().getStartTime());
                assertEquals(Duration.ofHours(2), createdTask.get().getDuration());
                assertEquals(LocalDateTime.of(2025, 9, 8, 12, 0),
                        createdTask.get().getEndTime());
            }

            @Test
            @DisplayName("Создание задачи без временных параметров")
            void testCreateTaskWithoutTimeParameters() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task without time",
                        "Description", StatusTask.NEW, null, null);

                // When
                int taskId = taskManager.createTask(task);
                Optional<Task> createdTask = taskManager.getTaskById(taskId);

                // Then
                assertTrue(createdTask.isPresent());
                assertNull(createdTask.get().getStartTime());
                assertNull(createdTask.get().getDuration());
                assertNull(createdTask.get().getEndTime());
            }

            @Test
            @DisplayName("Обновление задачи с добавлением временных параметров")
            void testUpdateTaskAddTimeParameters() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task",
                        "Description", StatusTask.NEW, null, null);
                int taskId = taskManager.createTask(task);

                Task updatedTask = new Task(taskId, "Updated Task",
                        "Updated Description", StatusTask.IN_PROGRESS,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 14, 0));

                // When
                taskManager.updateTask(updatedTask);
                Optional<Task> result = taskManager.getTaskById(taskId);

                // Then
                assertTrue(result.isPresent());
                assertEquals(LocalDateTime.of(2025, 9, 8, 14, 0),
                        result.get().getStartTime());
                assertEquals(Duration.ofHours(1), result.get().getDuration());
                assertEquals(LocalDateTime.of(2025, 9, 8, 15, 0),
                        result.get().getEndTime());
            }

            @Test
            @DisplayName("Обновление задачи с удалением временных параметров")
            void testUpdateTaskRemoveTimeParameters() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));
                int taskId = taskManager.createTask(task);

                Task updatedTask = new Task(taskId, "Updated Task",
                        "Updated Description", StatusTask.IN_PROGRESS,
                        null, null);

                // When
                taskManager.updateTask(updatedTask);
                Optional<Task> result = taskManager.getTaskById(taskId);

                // Then
                assertTrue(result.isPresent());
                assertNull(result.get().getStartTime());
                assertNull(result.get().getDuration());
                assertNull(result.get().getEndTime());
            }
        }

        @Nested
        @DisplayName("Тесты приоритетного списка задач")
        class PrioritizedTasksTests {

            @Test
            @DisplayName("Получение приоритетного списка задач")
            void testGetPrioritizedTasks() throws IOException {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 14, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task3 = new Task(taskManager.generateId(), "Task 3",
                        "Description", StatusTask.NEW, null, null);

                taskManager.createTask(task1);
                taskManager.createTask(task2);
                taskManager.createTask(task3);

                // When
                List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();

                // Then
                assertEquals(2, prioritizedTasks.size());
                assertEquals("Task 2", prioritizedTasks.get(0).getName());
                assertEquals("Task 1", prioritizedTasks.get(1).getName());
            }

            @Test
            @DisplayName(" Задачи без времени не должны добавляться в приоритетный список")
            void testPrioritizedTasksWithTwoTasksWithTime() throws IOException {
                Task task1 = new Task("Task 1", "Description");
                task1.setStartTime(LocalDateTime.now());

                Task task2 = new Task("Task 2", "Description");
                task2.setStartTime(LocalDateTime.now().plusHours(1));

                Task taskWithoutTime = new Task("No time", "Description");

                taskManager.createTask(task1);
                taskManager.createTask(task2);
                taskManager.createTask(taskWithoutTime);

                assertEquals(2, taskManager.getPrioritizedTasks().size());
            }
        }

        @Nested
        @DisplayName("Тесты временных параметров эпиков")
        class EpicTimeTests {

            @Test
            @DisplayName("Расчет времени эпика на основе подзадач")
            void testEpicTimeCalculationFromSubtasks() throws IOException {
                // Given
                Epic epic = new Epic(taskManager.generateId(), "Epic", "Description");
                int epicId = taskManager.createEpic(epic);

                SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0), epicId);

                SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 12, 0), epicId);

                // When
                taskManager.createSubTask(subTask1);
                taskManager.createSubTask(subTask2);
                Optional<Epic> resultEpic = taskManager.getEpicById(epicId);

                // Then
                assertTrue(resultEpic.isPresent());
                assertEquals(LocalDateTime.of(2025, 9,
                        8, 10, 0), resultEpic.get().getStartTime());
                assertEquals(Duration.ofHours(3), resultEpic.get().getDuration());
                assertEquals(LocalDateTime.of(2025, 9,
                        8, 14, 0), resultEpic.get().getEndTime());
            }

            @Test
            @DisplayName("Обновление времени эпика при удалении подзадачи")
            void testEpicTimeUpdateWhenSubtaskRemoved() throws IOException {
                // Given
                Epic epic = new Epic(taskManager.generateId(), "Epic", "Description");
                int epicId = taskManager.createEpic(epic);

                SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0), epicId);

                SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 12, 0), epicId);

                int subTaskId1 = taskManager.createSubTask(subTask1);
                int subTaskId2 = taskManager.createSubTask(subTask2);

                // When
                taskManager.deleteSubTaskById(subTaskId1);
                Optional<Epic> resultEpic = taskManager.getEpicById(epicId);

                // Then
                assertTrue(resultEpic.isPresent());
                assertEquals(LocalDateTime.of(2025, 9, 8, 12, 0),
                        resultEpic.get().getStartTime());
                assertEquals(Duration.ofHours(2), resultEpic.get().getDuration());
                assertEquals(LocalDateTime.of(2025, 9, 8, 14, 0),
                        resultEpic.get().getEndTime());
            }

            @Test
            @DisplayName("Эпик без подзадач не имеет временных параметров")
            void testEpicWithoutSubtasksHasNoTimeParameters() throws IOException {
                // Given
                Epic epic = new Epic(taskManager.generateId(), "Epic", "Description");
                int epicId = taskManager.createEpic(epic);

                // When
                Optional<Epic> resultEpic = taskManager.getEpicById(epicId);

                // Then
                assertTrue(resultEpic.isPresent());
                assertNull(resultEpic.get().getStartTime());
                assertNull(resultEpic.get().getDuration());
                assertNull(resultEpic.get().getEndTime());
            }

            @Test
            @DisplayName("Эпик с подзадачами без времени не имеет временных параметров")
            void testEpicWithSubtasksWithoutTimeHasNoTimeParameters() throws IOException {
                // Given
                Epic epic = new Epic(taskManager.generateId(), "Epic", "Description");
                int epicId = taskManager.createEpic(epic);

                SubTask subTask = new SubTask(taskManager.generateId(), "Subtask",
                        "Description", StatusTask.NEW, null, null, epicId);

                // When
                taskManager.createSubTask(subTask);
                Optional<Epic> resultEpic = taskManager.getEpicById(epicId);

                // Then
                assertTrue(resultEpic.isPresent());
                assertNull(resultEpic.get().getStartTime());
                assertNull(resultEpic.get().getDuration());
                assertNull(resultEpic.get().getEndTime());
            }
        }

        @Nested
        @DisplayName("Тесты проверки пересечений по времени")
        class TimeOverlapTests {

            @Test
            @DisplayName("Создание непересекающихся задач должно быть успешным")
            void testCreateNonOverlappingTasksShouldSucceed() {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 11, 30));

                // When & Then
                assertDoesNotThrow(() -> {
                    taskManager.createTask(task1);
                    taskManager.createTask(task2);
                });
            }

            @Test
            @DisplayName("Создание пересекающихся задач должно выбрасывать исключение")
            void testCreateOverlappingTasksShouldThrowException() throws IOException {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 11, 0));

                // When & Then
                taskManager.createTask(task1);
                assertThrows(RuntimeException.class, () -> taskManager.createTask(task2));
            }

            @Test
            @DisplayName("Обновление задачи с созданием пересечения должно выбрасывать исключение")
            void testUpdateTaskCreatingOverlapShouldThrowException() throws IOException {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 13, 0));

                int taskId1 = taskManager.createTask(task1);
                int taskId2 = taskManager.createTask(task2);

                Task updatedTask2 = new Task(taskId2, "Updated Task 2",
                        "Updated Description", StatusTask.IN_PROGRESS,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 11, 0));

                // When & Then
                assertThrows(RuntimeException.class, () -> taskManager.updateTask(updatedTask2));
            }

            @Test
            @DisplayName("Задачи без времени не создают пересечений")
            void testTasksWithoutTimeDoNotCreateOverlaps() {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW, null, null);

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW, null, null);

                // When & Then
                assertDoesNotThrow(() -> {
                    taskManager.createTask(task1);
                    taskManager.createTask(task2);
                });
            }
        }

        @Nested
        @DisplayName("Тесты удаления задач из временных слотов")
        class TimeSlotsRemovalTests {

            @Test
            @DisplayName("Удаление задачи должно освобождать временные слоты")
            void testDeleteTaskShouldFreeTimeSlots() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                int taskId = taskManager.createTask(task);

                Task overlappingTask = new Task(taskManager.generateId(), "Overlapping Task",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 11, 0));

                // When
                taskManager.deleteTaskById(taskId);

                // Then
                assertDoesNotThrow(() -> taskManager.createTask(overlappingTask));
            }

            @Test
            @DisplayName("Удаление всех задач должно освобождать все временные слоты")
            void testDeleteAllTasksShouldFreeAllTimeSlots() throws IOException {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 12, 0));

                taskManager.createTask(task1);
                taskManager.createTask(task2);

                Task overlappingTask = new Task(taskManager.generateId(), "Overlapping Task",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(3),
                        LocalDateTime.of(2025, 9, 8, 9, 0));

                // When
                taskManager.deleteAllTasks();

                // Then
                assertDoesNotThrow(() -> taskManager.createTask(overlappingTask));
            }

            @Test
            @DisplayName("Удаление подзадачи должно освобождать временные слоты")
            void testDeleteSubTaskShouldFreeTimeSlots() throws IOException {
                // Given
                Epic epic = new Epic(taskManager.generateId(), "Epic", "Description");
                int epicId = taskManager.createEpic(epic);

                SubTask subTask = new SubTask(taskManager.generateId(), "Subtask",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(2),
                        LocalDateTime.of(2025, 9, 8, 10, 0), epicId);

                int subTaskId = taskManager.createSubTask(subTask);

                Task overlappingTask = new Task(taskManager.generateId(), "Overlapping Task",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 11, 0));

                // When
                taskManager.deleteSubTaskById(subTaskId);

                // Then
                assertDoesNotThrow(() -> taskManager.createTask(overlappingTask));
            }
        }

        @Nested
        @DisplayName("Тесты граничных случаев временных параметров")
        class EdgeCaseTimeTests {

            @Test
            @DisplayName("Задача с нулевой длительностью")
            void testTaskWithZeroDuration() throws IOException {
                // Given
                Task task = new Task(taskManager.generateId(), "Task",
                        "Description", StatusTask.NEW,
                        Duration.ZERO,
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                // When
                int taskId = taskManager.createTask(task);
                Optional<Task> createdTask = taskManager.getTaskById(taskId);

                // Then
                assertTrue(createdTask.isPresent());
                assertEquals(LocalDateTime.of(2025, 9, 8, 10, 0),
                        createdTask.get().getStartTime());
                assertEquals(Duration.ZERO, createdTask.get().getDuration());
                assertEquals(LocalDateTime.of(2025, 9, 8, 10, 0),
                        createdTask.get().getEndTime());
            }

            @Test
            @DisplayName("Задачи с одинаковым временем начала должны создавать пересечение")
            void testTasksWithSameStartTimeShouldOverlap() throws IOException {
                // Given
                Task task1 = new Task(taskManager.generateId(), "Task 1",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                Task task2 = new Task(taskManager.generateId(), "Task 2",
                        "Description", StatusTask.NEW,
                        Duration.ofHours(1),
                        LocalDateTime.of(2025, 9, 8, 10, 0));

                // When & Then
                taskManager.createTask(task1);
                assertThrows(RuntimeException.class, () -> taskManager.createTask(task2));
            }
        }
    }