package taskmanager.app.service.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.management.Managers;
import taskmanager.app.management.TaskManager;
import taskmanager.app.service.history.InMemoryHistoryManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты менеджера задач в оперативной памяти")
class InMemoryTaskManagerTest {
    private TaskManager taskManager;
    private InMemoryHistoryManager historyManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager();
        historyManager = new InMemoryHistoryManager();
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
            taskManager = new InMemoryTaskManager();
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);

            // When
            int taskId = taskManager.createTask(task);

            // Then
            assertNotEquals(0, taskId, "ID задачи не должен быть равен 0");
            assertEquals(task, taskManager.getTaskById(taskId), "Задача создана");
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
            assertNotEquals(0, epicId, "ID задачи не должен быть равен 0");
            assertEquals(epic, taskManager.getEpicById(epicId), "Задача создана");
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
            assertEquals(subTask, taskManager.getSubTaskById(subTaskId),
                    "Подзадача создана");
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
            taskManager.getAllTasks();

            // Then
            assertEquals(2, taskManager.getAllTasks().size());
            assertTrue(taskManager.getAllTasks().contains(task1));
            assertTrue(taskManager.getAllTasks().contains(task2));
        }

        @Test
        @DisplayName("должен возвращаться null, если задача не найдена")
        void testGetTaskById_shouldReturnNullWhenTaskNotFound() {
            // Given
            int invalidId = 999;

            // When
            taskManager.getTaskById(invalidId);

            // Then
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("Подзадача с несуществующим ID должна возвращаться null")
        void testGetSubTaskById_shouldReturnNullWhenSubTaskNotFound() {
            // Given
            int invalidId = 999;

            // When
            taskManager.getSubTaskById(invalidId);

            // Then
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("Эпик с несуществующим ID должен возвращаться null")
        void testGetEpicById_shouldReturnNullWhenEpicNotFound() {
            // Given
            int invalidId = 999;

            // When
            taskManager.getEpicById(invalidId);

            // Then
            assertNull(taskManager.getEpicById(999));
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
            updatedTask.setId(taskId);

            // When
            taskManager.updateTask(updatedTask);

            // Then
            assertEquals(updatedTask, taskManager.getTaskById(taskId));
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
        @DisplayName("должен устанавливаться статус NEW, если нет подзадач")
        void testUpdateEpicStatus_shouldSetNewWhenNoSubtasks() {
            // Then
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
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
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если подзадачи разных статусов (NEW и DONE)")
        void testEpicStatusShouldBeInProgressWhenSubtasksMixed() throws IOException {
            // When
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTaskDone);

            // Then
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика DONE, если все подзадачи DONE")
        void testEpicStatusShouldBeDoneWhenAllSubtasksDone() throws IOException {
            // When
            taskManager.createSubTask(subTaskDone);
            taskManager.createSubTask(subTaskDone);

            // Then
            assertEquals(StatusTask.DONE, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если хотя бы одна подзадача IN_PROGRESS")
        void testEpicStatusShouldBeInProgressWhenAnySubtaskInProgress() throws IOException {
            // When
            taskManager.createSubTask(subTaskInProgress);
            taskManager.createSubTask(subTaskNew);

            // Then
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
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
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("должно выдать исключение")
        void testDeleteNonExistentEpic_shouldNotThrow() {
            // When & Then
            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.deleteEpicById(999));
            assertNull(taskManager.getEpicById(999));
        }

        @Test
        @DisplayName("должно завершаться без ошибок")
        void testDeleteNonExistentSubtask_shouldNotThrow() {
            // When & Then
            assertDoesNotThrow(() -> taskManager.deleteSubTaskById(999));
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("должна удаляться задача")
        void testDeleteTaskById_shouldRemoveTask() throws IOException {
            // When
            taskManager.deleteTaskById(taskId);

            // Then
            assertNull(taskManager.getTaskById(taskId));
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
            assertNull(taskManager.getEpicById(epicId));
            assertNull(taskManager.getSubTaskById(subTaskId));
        }

        @Test
        @DisplayName("должна удаляться подзадача и обновляться эпик")
        void testDeleteSubTaskById_shouldRemoveSubtaskAndUpdateEpic() throws IOException {
            // When
            taskManager.deleteSubTaskById(subTaskId);

            // Then
            assertNull(taskManager.getSubTaskById(subTaskId));
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
            taskManager.getTaskById(taskId);
            taskManager.getEpicById(epicId);
            List<Task> history = (List<Task>) taskManager.getHistory();

            // Then
            assertEquals(2, history.size());
            assertTrue(history.contains(task));
            assertTrue(history.contains(epic));
            assertTrue(history.stream().anyMatch(t -> t.getId() == taskId));
            assertTrue(history.stream().anyMatch(t -> t.getId() == epicId));
        }

        @Test
        @DisplayName("Повторный просмотр задачи: история не должна дублироваться")
        void testShouldNotDuplicateHistoryWhenTaskViewedAgain() throws IOException {
            // Given
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            // When
            taskManager.getTaskById(taskId);
            taskManager.getTaskById(taskId);

            // Then
            List<Task> history = (List<Task>) taskManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.getFirst());
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
}