package test.service;

import core.*;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;
import managers.Managers;

import org.junit.jupiter.api.*;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private InMemoryTaskManager taskManager = Managers.getDefault();
    private InMemoryHistoryManager historyManager = Managers.getDefaultHistory();

    @BeforeEach
    public void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    @Nested
    class createTaskTest {

        @Test
        @DisplayName("Создание задачи: должна создаваться и возвращаться задача с ID")
        void createTask_shouldCreateAndReturnTaskWithId() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            assertNotEquals(0, taskId, "ID задачи не должен быть равен 0");
            assertEquals(task, taskManager.getTaskById(taskId), "Задача создана");
        }

        @Test
        @DisplayName("Создание задачи: должно выбрасываться исключение, когда задача null")
        void createTask_shouldThrowWhenTaskNull() {
            assertThrows(IllegalArgumentException.class, () -> taskManager.createTask(null));
        }

        @Test
        @DisplayName("Создание эпика: должен создаваться и возвращаться эпик с ID")
        void createEpic_shouldCreateAndReturnEpicWithId() {
            Epic epic = new Epic(taskManager.generateId(), "Эпик 1", "Описание эпик 1");
            int epicId = taskManager.createEpic(epic);

            assertNotEquals(0, epicId, "ID задачи не должен быть равен 0");
            assertEquals(epic, taskManager.getEpicById(epicId), "Задача создана");
        }

        @Test
        @DisplayName("Эпик не может быть добавлен в самого себя как подзадача")
        void epicCannotAddItselfAsSubtask() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask invalidSubTask = new SubTask(epicId, "Invalid Subtask", "Invalid Subtask description",
                    StatusTask.NEW, epicId);
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(invalidSubTask));
        }

        @Test
        @DisplayName("Создание подзадачи: должна создаваться и возвращаться подзадача с ID")
        void createSubTask_shouldCreateAndReturnsSubtaskWitchId() {
            Epic epic = new Epic(taskManager.generateId(), "Эпик 1", "Описание эпик 1");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1", "Описание подзадачи 1",
                    StatusTask.NEW, epicId);
            int subTaskId = taskManager.createSubTask(subTask);

            assertNotEquals(0, subTaskId, "ID подзадачи не должен быть равен 0");
            assertEquals(subTask, taskManager.getSubTaskById(subTaskId), "Подзадача создана");
            assertTrue(taskManager.getSubTasksByEpicId(epicId).contains(subTask), "Подзадача добавлена в список подзадач");
        }

        @Test
        @DisplayName("Создание подзадачи: должно выбрасываться исключение, когда эпика не существует")
        void createSubTask_shouldThrowWhenEpicNotExist() {
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1", "Описание подзадачи 1",
                    StatusTask.NEW, 999);
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(subTask));
        }

    }

    @Nested
    class getTaskByIdAndGetAllTaskTest {
        @Test
        @DisplayName("Получение всех задач: должен возвращаться пустой список, если задач нет")
        void getAllTasks_shouldReturnEmptyListWhenNoTasks() {
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("Получение всех задач: должны возвращаться все созданные задачи")
        void getAllTasks_shouldReturnAllCreatedTasks() {
            Task task1 = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2", "Task 2 description", StatusTask.IN_PROGRESS);
            taskManager.createTask(task1);
            taskManager.createTask(task2);

            assertEquals(2, taskManager.getAllTasks().size());
            assertTrue(taskManager.getAllTasks().contains(task1));
            assertTrue(taskManager.getAllTasks().contains(task2));
        }

        @Test
        @DisplayName("Получение задачи по ID: должен возвращаться null, если задача не найдена")
        void getTaskById_shouldReturnNullWhenTaskNotFound() {
            assertNull(taskManager.getTaskById(999));
        }
    }

    @Nested
    class getSubTasksByEpicIdTest {
        @Test
        @DisplayName("Получение подзадач эпика: должен возвращаться пустой список, если подзадач нет")
        void getSubTasksByEpicId_shouldReturnEmptyListWhenSubtasks() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);

            assertTrue(taskManager.getSubTasksByEpicId(epic.getId()).isEmpty());
        }
    }

    @Nested
    class UpdateTaskAndEpicAndSubtaskTest {

        @Test
        @DisplayName("Обновление задачи: должна обновляться существующая задача")
        void updateTask_shouldUpdateExistingTask() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            Task updatedTask = new Task(task.getId(), "Updated Task 1", "Updated Task 1 description", StatusTask.IN_PROGRESS);
            updatedTask.setId(taskId);
            taskManager.updateTask(updatedTask);

            assertEquals(updatedTask, taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("Обновление задачи: должно выбрасывать исключение когда задачи не существует")
        void updateTask_shouldThrowWhenTaskNotExist() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            task.setId(taskId + 1);
            assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(task));
        }

        @Test
        @DisplayName("Обновление статуса эпика: должен устанавливаться статус NEW, если нет подзадач")
        void updateEpicStatus_shouldSetNewWhenNoSubtasks() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика NEW, если все подзадачи NEW")
        void epicStatusShouldBeNewWhenAllSubtasksNew() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.NEW, epicId);

            taskManager.createSubTask(subTask1);
            taskManager.createSubTask(subTask2);

            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если подзадачи разных статусов (NEW и DONE)")
        void epicStatusShouldBeInProgressWhenSubtasksMixed() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);

            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);

            taskManager.createSubTask(subTask1);
            taskManager.createSubTask(subTask2);

            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика DONE, если все подзадачи DONE")
        void epicStatusShouldBeDoneWhenAllSubtasksDone() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.DONE, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);

            taskManager.createSubTask(subTask1);
            taskManager.createSubTask(subTask2);

            assertEquals(StatusTask.DONE, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если хотя бы одна подзадача IN_PROGRESS")
        void epicStatusShouldBeInProgressWhenAnySubtaskInProgress() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.IN_PROGRESS, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.NEW, epicId);
            taskManager.createSubTask(subTask1);
            taskManager.createSubTask(subTask2);

            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }
    }

    @Nested
    class DeleteTaskAndEpicAndSubtaskTest {

        @Test
        @DisplayName("Удаление несуществующей задачи: должно завершаться без ошибок")
        void deleteNonExistentTask_shouldNotThrow() {
            assertDoesNotThrow(() -> taskManager.deleteTaskById(999));
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("Удаление несуществующего эпика: должно выдать исключение")
        void deleteNonExistentEpic_shouldNotThrow() {
            assertThrows(IllegalArgumentException.class, () -> taskManager.deleteEpicById(999));
            assertNull(taskManager.getEpicById(999));
        }

        @Test
        @DisplayName("Удаление несуществующей подзадачи: должно завершаться без ошибок")
        void deleteNonExistentSubtask_shouldNotThrow() {
            assertDoesNotThrow(() -> taskManager.deleteSubTaskById(999));
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("Удаление задачи по ID: должна удаляться задача")
        void deleteTaskById_shouldRemoveTask() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            taskManager.deleteTaskById(taskId);

            assertNull(taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("Удаление всех задач: должны удаляться все задачи")
        void deleteAllTasks_shouldRemoveAllTasks() {
            Task task1 = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2", "Task 2 description", StatusTask.IN_PROGRESS);
            taskManager.createTask(task1);
            taskManager.createTask(task2);

            taskManager.deleteAllTasks();

            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("Удаление эпика по ID: должен удаляться эпик и его подзадачи")
        void deleteEpicById_shouldRemoveAllSubtasks() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.IN_PROGRESS, epicId);

            int subtaskId1 = taskManager.createSubTask(subTask1);
            int subtaskId2 = taskManager.createSubTask(subTask2);

            taskManager.deleteEpicById(epicId);

            assertNull(taskManager.getEpicById(epicId));
            assertNull(taskManager.getSubTaskById(subtaskId1));
            assertNull(taskManager.getSubTaskById(subtaskId2));

            assertTrue(historyManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление подзадачи по ID: должна удаляться подзадача и обновляться эпик")
        void deleteSubTaskById_shouldRemoveSubtaskAndUpdateEpic() {
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1", "Subtask 1 description", StatusTask.NEW, epicId);
            int subtaskId = taskManager.createSubTask(subTask1);

            taskManager.deleteSubTaskById(subtaskId);

            assertNull(taskManager.getSubTaskById(subtaskId));
            assertTrue(taskManager.getSubTasksByEpicId(epicId).isEmpty());
        }

        @Test
        @DisplayName("Удаление всех задач: история просмотров должна очищаться")
        void deleteAllTasks_shouldClearHistory() {
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            taskManager.createTask(task1);
            taskManager.createTask(task2);

            taskManager.getTaskById(task1.getId());
            taskManager.getTaskById(task2.getId());

            taskManager.deleteAllTasks();

            assertTrue(taskManager.getAllTasks().isEmpty());
            assertTrue(historyManager.getHistory().isEmpty());
        }
    }

    @Nested
    class generatedIdTest {

        @Test
        @DisplayName("Генерация ID: должен генерироваться уникальный ID")
        void generateId_shouldGenerateUniqueId() {
            int firstId = taskManager.generateId();
            int secondId = taskManager.generateId();

            assertEquals(firstId + 1, secondId);
        }
    }

    @Nested
    class TaskEqualityTest {
        @Test
        @DisplayName("Задачи равны, если имеют одинаковый ID")
        void tasksWithSameIdShouldBeEqual() {
            Task task1 = new Task(1, "Task 1", "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(1, "Task 2", "Task 2 description", StatusTask.DONE);

            assertEquals(task1, task2);
            assertEquals(task1.hashCode(), task2.hashCode());
        }

        @Test
        @DisplayName("Эпики равны, если имеют одинаковый ID")
        void epicsWithSameIdShouldBeEqual() {
            Epic epic1 = new Epic(1, "Epic 1", "Epic 1 description");
            Epic epic2 = new Epic(1, "Epic 2", "Epic 2 description");

            assertEquals(epic1, epic2);
            assertEquals(epic1.hashCode(), epic2.hashCode());
        }

        @Test
        @DisplayName("Подзадачи равны, если имеют одинаковый ID")
        void subtasksWithSameIdShouldBeEqual() {
            SubTask subTask1 = new SubTask(1, "Subtask 1", "Subtask 1 description", StatusTask.NEW, 1);
            SubTask subTask2 = new SubTask(1, "Subtask 2", "Subtask 2 description", StatusTask.DONE, 1);

            assertEquals(subTask1, subTask2);
            assertEquals(subTask1.hashCode(), subTask2.hashCode());
        }
    }

    @Nested
    class HistoryTest {

        @Test
        @DisplayName("Получение истории: должны возвращаться просмотренные задачи")
        void getHistory_shouldReturnViewedTasks() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            taskManager.getTaskById(taskId);
            taskManager.getEpicById(epicId);

            List<Task> history = (List<Task>) taskManager.getHistory();

            assertEquals(2, history.size());
            assertTrue(history.contains(task));
            assertTrue(history.contains(epic));

            assertTrue(history.stream().anyMatch(t -> t.getId() == taskId));
            assertTrue(history.stream().anyMatch(t -> t.getId() == epicId));
        }

        @Test
        @DisplayName("Повторный просмотр задачи: история не должна дублироваться")
        void shouldNotDuplicateHistoryWhenTaskViewedAgain() {
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            taskManager.getTaskById(taskId);
            taskManager.getTaskById(taskId);

            List<Task> history = (List<Task>) taskManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.get(0));
        }

        @Test
        @DisplayName("Удаление задачи: задача должна удаляться из истории")
        void shouldRemoveTaskFromHistoryWhenTaskDeleted() {
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            taskManager.getTaskById(taskId);
            taskManager.deleteTaskById(taskId);

            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех задач: история должна очищаться")
        void shouldClearHistoryWhenAllTasksDeleted() {
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);

            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);

            taskManager.deleteAllTasks();

            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех эпиков: история должна очищаться")
        void shouldClearHistoryWhenAllEpicsDeleted() {
            Epic epic1 = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            Epic epic2 = new Epic(taskManager.generateId(), "Epic 2", "Epic 2 description");

            int epicId1 = taskManager.createEpic(epic1);
            int epicId2 = taskManager.createEpic(epic2);

            taskManager.getEpicById(epicId1);
            taskManager.getEpicById(epicId2);

            taskManager.deleteAllEpics();

            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех подзадач: история должна очищаться")
        void shouldClearHistoryWhenAllSubtasksDeleted() {
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

            assertEquals(2, taskManager.getHistory().size(), "История должна содержать две задачи");

            taskManager.deleteAllSubTasks();

            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Nested
        public class ManagersTest {
            @Test
            @DisplayName("getDefault() возвращает проинициализированный InMemoryTaskManager")
            void getDefault_shouldReturnInitializedInMemoryTaskManager() {
                taskManager = Managers.getDefault();
                assertNotNull(taskManager);
            }

            @Test
            @DisplayName("getDefaultHistory() возвращает проинициализированный HistoryManager")
            void getDefaultHistory_shouldReturnInitializedHistoryManager() {
                historyManager = Managers.getDefaultHistory();
                assertNotNull(historyManager);
            }
        }

        @Nested
        public class CheckingTheImmutabilityOfTasksTest {

            @Test
            @DisplayName("Задача остается неизменной при добавлении в менеджер")
            void taskRemainsUnchangedWhenAddedToManager() {
                int taskId = taskManager.generateId();
                Task original = new Task(taskId, "Task 1", "Task 1 description", StatusTask.NEW);
                Task copy = new Task(original.getId(), original.getName(), original.getDescription(), original.getStatus());
                taskManager.createTask(original);

                assertEquals(copy, original);
                assertEquals(copy.getName(), original.getName());
                assertEquals(copy.getDescription(), original.getDescription());
                assertEquals(copy.getStatus(), original.getStatus());
            }

            @Test
            @DisplayName("Эпик остается неизменной при добавлении в менеджер")
            void epicRemainsUnchangedWhenAddedToManager() {
                int epicId = taskManager.generateId();
                Epic original = new Epic(epicId, "Epic 1", "Epic 1 description");
                Epic copy = new Epic(original.getId(), original.getName(), original.getDescription());
                taskManager.createEpic(original);

                assertEquals(copy, original);
                assertEquals(copy.getName(), original.getName());
                assertEquals(copy.getDescription(), original.getDescription());
            }

            @Test
            @DisplayName("Подзадача остается неизменной при добавлении в менеджер")
            void subtaskRemainsUnchangedWhenAddedToManager() {
                int epicId = taskManager.generateId();
                Epic epic = new Epic(epicId, "Epic 1", "Epic 1 description");
                taskManager.createEpic(epic);

                int subtaskId = taskManager.generateId();
                SubTask original = new SubTask(subtaskId, "Subtask 1", "Subtask 1 description", StatusTask.NEW, epicId);
                SubTask copy = new SubTask(original.getId(), original.getName(), original.getDescription(), original.getStatus(), original.getEpicId());
                taskManager.createSubTask(original);

                assertEquals(copy, original);
                assertEquals(copy.getName(), original.getName());
                assertEquals(copy.getDescription(), original.getDescription());
                assertEquals(copy.getStatus(), original.getStatus());
                assertEquals(copy.getEpicId(), original.getEpicId());
            }
        }
    }
}