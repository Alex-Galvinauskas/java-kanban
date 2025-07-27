package test.service;

import core.*;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;
import managers.Managers;

import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private InMemoryTaskManager taskManager = Managers.getDefault();
    private InMemoryHistoryManager historyManager = Managers.getDefaultHistory();

    @BeforeEach
    public void setUp() {
        taskManager = new InMemoryTaskManager();
        historyManager = new InMemoryHistoryManager();
    }

    @Nested
    @DisplayName("Тесты менеджера задач")
    public class ManagersTest {
        @Test
        @DisplayName("getDefault() возвращает проинициализированный InMemoryTaskManager")
        void getDefault_shouldReturnInitializedInMemoryTaskManager() {

            //When Вызываем метод для получения дефолтного менеджера
            taskManager = Managers.getDefault();

            //Then Проверяем, что менеджер не равен null
            assertNotNull(taskManager);
        }

        @Test
        @DisplayName("getDefaultHistory() возвращает проинициализированный HistoryManager")
        void getDefaultHistory_shouldReturnInitializedHistoryManager() {

            //When Вызываем метод для получения дефолтного менеджера
            historyManager = Managers.getDefaultHistory();

            //Then Проверяем, что менеджер не равен null
            assertNotNull(historyManager);
        }
    }

    @Nested
    @DisplayName("Тесты генерации ID")
    class generatedIdTest {

        @Test
        @DisplayName("Генерация ID: должен генерироваться уникальный ID")
        void generateId_shouldGenerateUniqueId() {

            //Given - генерирует уникальный ID

            //When - генерируем два ID
            int firstId = taskManager.generateId();
            int secondId = taskManager.generateId();

            //Then - проверяем, что второй ID больше первого на единицу
            assertEquals(firstId + 1, secondId);
        }
    }

    @Nested
    @DisplayName("Тесты создания задач")
    class createTaskTest {
        private Epic epic;
        private int epicId;

        @BeforeEach
        void setUpCreateTaskTest() {
            //Given
            epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            epicId = taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Создание задачи: должна создаваться и возвращаться задача с ID")
        void createTask_shouldCreateAndReturnTaskWithId() {

            //Given - создаем менеджер задач и новую задачу
            taskManager = new InMemoryTaskManager();
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);

            //When - когда задача создана
            int taskId = taskManager.createTask(task);

            //Then - проверяем, что идентификатор задачи не равен 0 и задача создана
            assertNotEquals(0, taskId, "ID задачи не должен быть равен 0");
            assertEquals(task, taskManager.getTaskById(taskId), "Задача создана");
        }

        @Test
        @DisplayName("Создание задачи: должно выбрасываться исключение, когда задача null")
        void createTask_shouldThrowWhenTaskNull() {

            //When/Then создаем null - задачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createTask(null));
        }

        @Test
        @DisplayName("Создание эпика: должен создаваться и возвращаться эпик с ID")
        void createEpic_shouldCreateAndReturnEpicWithId() {

            //Then - проверяем, что идентификатор эпика не равен 0 и эпик создан
            assertNotEquals(0, epicId, "ID задачи не должен быть равен 0");
            assertEquals(epic, taskManager.getEpicById(epicId), "Задача создана");
        }

        @Test
        @DisplayName("Эпик не может быть добавлен в самого себя как подзадача")
        void epicCannotAddItselfAsSubtask() {

            //When - пытаемся создать подзадачу с эпиком в качестве родителя
            SubTask invalidSubTask = new SubTask(epicId, "Invalid Subtask", "Invalid Subtask description",
                    StatusTask.NEW, epicId);

            //Then - проверяем, что исключение выброшено
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(invalidSubTask));
        }

        @Test
        @DisplayName("Создание подзадачи: должна создаваться и возвращаться подзадача с ID")
        void createSubTask_shouldCreateAndReturnsSubtaskWitchId() {

            //Given - эпик и подзадача для него
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1", "Описание подзадачи 1",
                    StatusTask.NEW, epicId);

            //When - когда подзадача создана
            int subTaskId = taskManager.createSubTask(subTask);

            //Then - проверяем, что идентификатор подзадачи не равен 0 и подзадача создана и добавлена в эпик
            assertNotEquals(0, subTaskId, "ID подзадачи не должен быть равен 0");
            assertEquals(subTask, taskManager.getSubTaskById(subTaskId), "Подзадача создана");
            assertTrue(taskManager.getSubTasksByEpicId(epicId).contains(subTask), "Подзадача добавлена в список");
        }

        @Test
        @DisplayName("Подзадача не может быть добавлена в самого себя как родитель")
        void subTaskCannotAddItselfAsParent() {

            //Given - подзадача с самим себя в качестве родителя
            int invalidEpicId = taskManager.generateId();
            SubTask subTask = new SubTask(invalidEpicId, "Подзадача 1", "Описание подзадачи 1",
                    StatusTask.NEW, invalidEpicId);

            //When/Then создаем подзадачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(subTask));
        }

        @Test
        @DisplayName("Создание подзадачи: должно выбрасываться исключение, когда эпика не существует")
        void createSubTask_shouldThrowWhenEpicNotExist() {

            //Given - подзадача с несуществующим эпиком
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1", "Описание подзадачи 1",
                    StatusTask.NEW, 999);

            //When/Then создаем подзадачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(subTask));
        }

    }

    @Nested
    @DisplayName("Тесты метода getTaskById и getAllTasks")
    class getTaskByIdAndGetAllTaskTest {
        private  Task task1;
        private  Task task2;

        @BeforeEach
        void setUpGetTaskTest() {
            task1 = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            task2 = new Task(taskManager.generateId(), "Task 2", "Task 2 description", StatusTask.IN_PROGRESS);

            taskManager.createTask(task1);
            taskManager.createTask(task2);
        }

        @Test
        @DisplayName("Получение всех задач: должен возвращаться пустой список, если задач нет")
        void getAllTasks_shouldReturnEmptyListWhenNoTasks() {

            //Given - удаляем все задачи
            taskManager.deleteAllTasks();

            //When - получаем все задачи
            taskManager.getAllTasks();

            //Then - проверяем, что список пуст
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("Получение всех задач: должны возвращаться все созданные задачи")
        void getAllTasks_shouldReturnAllCreatedTasks() {

            //When - когда задачи созданы
            taskManager.getAllTasks();

            //Then - проверяем, что список содержит две задачи
            assertEquals(2, taskManager.getAllTasks().size());
            assertTrue(taskManager.getAllTasks().contains(task1));
            assertTrue(taskManager.getAllTasks().contains(task2));
        }

        @Test
        @DisplayName("Получение задачи по ID: должен возвращаться null, если задача не найдена")
        void getTaskById_shouldReturnNullWhenTaskNotFound() {

            //Given используем несуществующий ID
            int invalidId = 999;

            //When - получаем задачу по несуществующему ID
            taskManager.getTaskById(invalidId);

            //Then - проверяем, что задача не найдена(возвращен null)
            assertNull(taskManager.getTaskById(999));
        }
    }

    @Nested
    @DisplayName("Тесты метода getSubTasksByEpicId")
    class getSubTasksByEpicIdTest {
        @Test
        @DisplayName("Получение подзадач эпика: должен возвращаться пустой список, если подзадач нет")
        void getSubTasksByEpicId_shouldReturnEmptyListWhenSubtasks() {

            //Given - эпик без подзадач

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);

            //When - получаем подзадачи по ID
            List<SubTask> subTasks = taskManager.getSubTasksByEpicId(epic.getId());

            //Then - проверяем, что список пуст
            assertTrue(subTasks.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты метода updateTask, updateEpic, updateSubTask")
    class UpdateTaskAndEpicAndSubtaskTest {
        private Task task;
        private int taskId;
        private int epicId;
        private SubTask subTaskNew;
        private SubTask subTaskInProgress;
        private SubTask subTaskDone;

        @BeforeEach
        void setUpdateTest() {

            task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
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
        @DisplayName("Обновление задачи: должна обновляться существующая задача")
        void updateTask_shouldUpdateExistingTask() {

            //When - обновляем задачу
            Task updatedTask = new Task(task.getId(), "Updated Task 1",
                    "Updated Task 1 description", StatusTask.IN_PROGRESS);
            updatedTask.setId(taskId);
            taskManager.updateTask(updatedTask);

            //Then - проверяем, что задача обновлена
            assertEquals(updatedTask, taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("Обновление задачи: должно выбрасывать исключение когда задачи не существует")
        void updateTask_shouldThrowWhenTaskNotExist() {

            //When - обновляем задачу с несуществующим ID
            task.setId(taskId + 1);

            //Then - проверяем, что исключение выбрасывается
            assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(task));
        }

        @Test
        @DisplayName("Обновление статуса эпика: должен устанавливаться статус NEW, если нет подзадач")
        void updateEpicStatus_shouldSetNewWhenNoSubtasks() {

            //When/Then - проверяем, что статус установлен NEW
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика NEW, если все подзадачи NEW")
        void epicStatusShouldBeNewWhenAllSubtasksNew() {
            //Given
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.NEW, epicId);

            //When - добавляем подзадачи
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTask2);

            //Then - проверяем, что статус установлен NEW
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если подзадачи разных статусов (NEW и DONE)")
        void epicStatusShouldBeInProgressWhenSubtasksMixed() {

            //When - добавляем подзадачи
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTaskDone);

            //Then - проверяем, что статус установлен IN_PROGRESS
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика DONE, если все подзадачи DONE")
        void epicStatusShouldBeDoneWhenAllSubtasksDone() {
            //When - добавляем подзадачи
            taskManager.createSubTask(subTaskDone);
            taskManager.createSubTask(subTaskDone);

            //Then - проверяем, что статус установлен DONE
            assertEquals(StatusTask.DONE, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если хотя бы одна подзадача IN_PROGRESS")
        void epicStatusShouldBeInProgressWhenAnySubtaskInProgress() {

            //When - добавляем подзадачи
            taskManager.createSubTask(subTaskInProgress);
            taskManager.createSubTask(subTaskNew);

            //Then - проверяем, что статус установлен IN_PROGRESS
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteTask, deleteEpic, deleteSubTask")
    class DeleteTaskAndEpicAndSubtaskTest {
        int taskId;
        int epicId;
        int subTaskId;

        @BeforeEach
        void setUpDeleteTest() {
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
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
        @DisplayName("Удаление несуществующей задачи: должно завершаться без ошибок")
        void deleteNonExistentTask_shouldNotThrow() {

            //Given - задача с несуществующим ID
            //When - удаляем несуществующую задачу
            //Then - проверяем, что не выдает исключений(задача не существует)
            assertDoesNotThrow(() -> taskManager.deleteTaskById(999));
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("Удаление несуществующего эпика: должно выдать исключение")
        void deleteNonExistentEpic_shouldNotThrow() {

            //Given - эпик с несуществующим ID
            //When - удаляем несуществующий эпик
            //Then - проверяем, что выдает исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.deleteEpicById(999));
            assertNull(taskManager.getEpicById(999));
        }

        @Test
        @DisplayName("Удаление несуществующей подзадачи: должно завершаться без ошибок")
        void deleteNonExistentSubtask_shouldNotThrow() {

            //Given - подзадача с несуществующим ID
            //When - удаляем несуществующую подзадачу
            //Then - проверяем, что не выдает исключений(подзадача не существует)
            assertDoesNotThrow(() -> taskManager.deleteSubTaskById(999));
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("Удаление задачи по ID: должна удаляться задача")
        void deleteTaskById_shouldRemoveTask() {

            //When - удаляем задачу по её ID
            taskManager.deleteTaskById(taskId);

            //Then - проверяем, что задача удалена
            assertNull(taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("Удаление всех задач: должны удаляться все задачи")
        void deleteAllTasks_shouldRemoveAllTasks() {

            //When - удаляем все задачи
            taskManager.deleteAllTasks();

            //Then - проверяем, что все задачи удалены и список пуст
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("Удаление эпика по ID: должен удаляться эпик и его подзадачи")
        void deleteEpicById_shouldRemoveAllSubtasks() {

            //When - удаляем эпик по его ID
            taskManager.deleteEpicById(epicId);

            //Then - проверяем, что удалены все задачи и список пуст
            assertNull(taskManager.getEpicById(epicId));
            assertNull(taskManager.getSubTaskById(subTaskId));
        }

        @Test
        @DisplayName("Удаление подзадачи по ID: должна удаляться подзадача и обновляться эпик")
        void deleteSubTaskById_shouldRemoveSubtaskAndUpdateEpic() {


            //When - удаляем подзадачу по её ID
            taskManager.deleteSubTaskById(subTaskId);

            //Then проверяем что подзадачи удалены и список подзадач эпика пуст
            assertNull(taskManager.getSubTaskById(subTaskId));
            assertTrue(taskManager.getSubTasksByEpicId(epicId).isEmpty());
        }

        @Test
        @DisplayName("Удаление всех задач: история просмотров должна очищаться")
        void deleteAllTasks_shouldClearHistory() {

            //When - удаляем все задачи
            taskManager.deleteAllTasks();

            //Then - проверяем, что список задач и история просмотров очищена
            assertTrue(taskManager.getAllTasks().isEmpty());
            assertTrue(historyManager.getHistory().isEmpty());
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
        void tasksWithSameIdShouldBeEqual() {

            //When/Then - проверяем равенство задач и их хеш-кодов
            assertEquals(task1, task2);
            assertEquals(task1.hashCode(), task2.hashCode());
        }

        @Test
        @DisplayName("Эпики равны, если имеют одинаковый ID")
        void epicsWithSameIdShouldBeEqual() {

            //When/Then - проверяем равенство эпиков и их хеш-кодов
            assertEquals(epic1, epic2);
            assertEquals(epic1.hashCode(), epic2.hashCode());
        }

        @Test
        @DisplayName("Подзадачи равны, если имеют одинаковый ID")
        void subtasksWithSameIdShouldBeEqual() {

            //When/Then - проверяем равенство подзадач и их хеш-кодов
            assertEquals(subtask1, subtask2);
            assertEquals(subtask1.hashCode(), subtask2.hashCode());
        }
    }

    @Nested
    @DisplayName("Тесты истории просмотров")
    class HistoryTest {

        @Test
        @DisplayName("Получение истории: должны возвращаться просмотренные задачи")
        void getHistory_shouldReturnViewedTasks() {

            //Given - созданы задача и эпик
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            //When - задачи просмотрены и запрошена история
            taskManager.getTaskById(taskId);
            taskManager.getEpicById(epicId);
            List<Task> history = (List<Task>) taskManager.getHistory();

            //Then - проверяем, что история содержит задачу и эпик
            assertEquals(2, history.size());
            assertTrue(history.contains(task));
            assertTrue(history.contains(epic));
            assertTrue(history.stream().anyMatch(t -> t.getId() == taskId));
            assertTrue(history.stream().anyMatch(t -> t.getId() == epicId));
        }

        @Test
        @DisplayName("Повторный просмотр задачи: история не должна дублироваться")
        void shouldNotDuplicateHistoryWhenTaskViewedAgain() {
            //Given - создана задача
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            //Then - задача просмотрена дважды
            taskManager.getTaskById(taskId);
            taskManager.getTaskById(taskId);

            //Then - проверяем, что история содержит одну запись
            List<Task> history = (List<Task>) taskManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.getFirst());
        }

        @Test
        @DisplayName("Удаление задачи: задача должна удаляться из истории")
        void shouldRemoveTaskFromHistoryWhenTaskDeleted() {
            //Given - создана задача
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);
            taskManager.getTaskById(taskId);

            //When - задача удалена
            taskManager.deleteTaskById(taskId);

            //Then - проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление эпика: эпик и его подзадачи должны удаляться из истории")
        void shouldRemoveEpicAndSubtasksFromHistoryWhenEpicDeleted() {
            //Given - создан эпик и подзадачи
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask1 = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.IN_PROGRESS, epicId);

            taskManager.getEpicById(epicId);
            taskManager.getSubTaskById(subTask1.getId());
            taskManager.getSubTaskById(subTask2.getId());

            taskManager.deleteEpicById(epicId);

            Collection<Task> historyAfter = taskManager.getHistory();
            assertEquals(0, historyAfter.size());
        }

        @Test
        @DisplayName("Удаление подзадачи: подзадача должна удаляться из истории")
        void shouldRemoveSubtaskFromHistoryWhenSubtaskDeleted() {
            //Given - созданы эпик и подзадача
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            int subTaskId = taskManager.createSubTask(subTask);

            //When - подзадача просмотрена и удалена
            taskManager.getSubTaskById(subTaskId);
            taskManager.deleteSubTaskById(subTaskId);

            //Then - проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление задачи не должно влиять на другие задачи в истории")
        void shouldNotAffectOtherTasksInHistoryWhenDeletingOneTask() {

            //Given - созданы две задачи
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);
            //When - обе задачи просмотрены и одна удалена
            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);
            taskManager.deleteTaskById(taskId2);

            //Then - проверяем, что история содержит только одну запись
            assertEquals(1, taskManager.getHistory().size());
            assertTrue(taskManager.getHistory().contains(task1));

        }


        @Test
        @DisplayName("Очистка всех задач: история должна очищаться")
        void shouldClearHistoryWhenAllTasksDeleted() {
            //Given - созданы две задачи и просмотрены
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);

            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);
            //проверяем, что история содержит две записи
            assertEquals(2, taskManager.getHistory().size());
            //When - все задачи удалены
            taskManager.deleteAllTasks();

            //Then - проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех эпиков: история должна очищаться")
        void shouldClearHistoryWhenAllEpicsDeleted() {
            //Given - созданы два эпика и просмотрены
            Epic epic1 = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            Epic epic2 = new Epic(taskManager.generateId(), "Epic 2", "Epic 2 description");

            int epicId1 = taskManager.createEpic(epic1);
            int epicId2 = taskManager.createEpic(epic2);

            taskManager.getEpicById(epicId1);
            taskManager.getEpicById(epicId2);

            //When - удаляем все эпики
            taskManager.deleteAllEpics();

            //Then - проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех подзадач: история должна очищаться")
        void shouldClearHistoryWhenAllSubtasksDeleted() {

            //Given - созданы эпик и две его подзадачи и просмотрены
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


            //When - удаляем все подзадачи
            taskManager.deleteAllSubTasks();

            //Then - проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты неизменности задач")
    public class CheckingTheImmutabilityOfTasksTest {
        private int taskId;
        private int epicId;
        private int subTaskId;
        private Epic epic;

        @BeforeEach
        void setUpImmutabilityTest() {
            taskId = taskManager.generateId();
            epicId = taskManager.generateId();
            subTaskId = taskManager.generateId();
            epic = new Epic(epicId, "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Задача остается неизменной при добавлении в менеджер")
        void taskRemainsUnchangedWhenAddedToManager() {
            //Given - создаём оригинальную задачу и её копию, генерируем ID
            Task original = new Task(taskId, "Task 1", "Task 1 description", StatusTask.NEW);
            Task copy = new Task(original.getId(), original.getName(), original.getDescription(), original.getStatus());
            //When - добавляем задачу в менеджер
            taskManager.createTask(original);

            //Then - проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
        }

        @Test
        @DisplayName("Эпик остается неизменной при добавлении в менеджер")
        void epicRemainsUnchangedWhenAddedToManager() {
            //Given - создаём оригинальный эпик и его копию, генерируем ID
            Epic original = new Epic(epicId, "Epic 1", "Epic 1 description");
            Epic copy = new Epic(original.getId(), original.getName(), original.getDescription());

            //When - добавляем эпик в менеджер
            taskManager.createEpic(original);

            //Then - проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
        }

        @Test
        @DisplayName("Подзадача остается неизменной при добавлении в менеджер")
        void subtaskRemainsUnchangedWhenAddedToManager() {
            //Given - Создаём оригинальную подзадачу и ее копию, генерируем ID
            SubTask original = new SubTask(subTaskId, "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask copy = new SubTask(original.getId(), original.getName(),
                    original.getDescription(), original.getStatus(), original.getEpicId());

            //When - добавляем подзадачу в менеджер
            taskManager.createSubTask(original);

            //Then - проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
            assertEquals(copy.getEpicId(), original.getEpicId());
        }
    }
}