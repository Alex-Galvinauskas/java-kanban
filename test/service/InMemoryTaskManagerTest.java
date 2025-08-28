package test.service;

import core.*;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;
import managers.Managers;

import org.junit.jupiter.api.*;

import java.io.IOException;
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

            //Вызываем метод для получения дефолтного менеджера
            taskManager = Managers.getDefault();

            //Проверяем, что менеджер не равен null
            assertNotNull(taskManager);
        }

        @Test
        @DisplayName("getDefaultHistory() возвращает проинициализированный HistoryManager")
        void getDefaultHistory_shouldReturnInitializedHistoryManager() {

            //Вызываем метод для получения дефолтного менеджера
            historyManager = Managers.getDefaultHistory();

            //Проверяем, что менеджер не равен null
            assertNotNull(historyManager);
        }
    }

    @Nested
    @DisplayName("Тесты генерации ID")
    class GeneratedIdTest {

        @Test
        @DisplayName("Генерация ID: должен генерироваться уникальный ID")
        void generateId_shouldGenerateUniqueId() {

            //генерирует уникальный ID

            //генерируем два ID
            int firstId = taskManager.generateId();
            int secondId = taskManager.generateId();

            //проверяем, что второй ID больше первого на единицу
            assertEquals(firstId + 1, secondId);
        }
    }

    @Nested
    @DisplayName("Тесты создания задач")
    class CreateTaskTest {
        private Epic epic;
        private int epicId;

        @BeforeEach
        void setUpCreateTaskTest() throws IOException {
            //создаем эпик
            epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            epicId = taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Создание задачи: должна создаваться и возвращаться задача с ID")
        void createTask_shouldCreateAndReturnTaskWithId() throws IOException {

            //создаем менеджер задач и новую задачу
            taskManager = new InMemoryTaskManager();
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description",
                    StatusTask.NEW);

            //когда задача создана
            int taskId = taskManager.createTask(task);

            //проверяем, что идентификатор задачи не равен 0 и задача создана
            assertNotEquals(0, taskId, "ID задачи не должен быть равен 0");
            assertEquals(task, taskManager.getTaskById(taskId), "Задача создана");
        }

        @Test
        @DisplayName("Создание задачи: должно выбрасываться исключение, когда задача null")
        void createTask_shouldThrowWhenTaskNull() {

            //создаем null - задачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createTask(null));
        }

        @Test
        @DisplayName("Создание эпика: должен создаваться и возвращаться эпик с ID")
        void createEpic_shouldCreateAndReturnEpicWithId() {

            //проверяем, что идентификатор эпика не равен 0 и эпик создан
            assertNotEquals(0, epicId, "ID задачи не должен быть равен 0");
            assertEquals(epic, taskManager.getEpicById(epicId), "Задача создана");
        }

        @Test
        @DisplayName("Эпик не может быть добавлен в самого себя как подзадача")
        void epicCannotAddItselfAsSubtask() {

            //пытаемся создать подзадачу с эпиком в качестве родителя
            SubTask invalidSubTask = new SubTask(epicId, "Invalid Subtask",
                    "Invalid Subtask description", StatusTask.NEW, epicId);

            //проверяем, что исключение выброшено
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(invalidSubTask));
        }

        @Test
        @DisplayName("Создание подзадачи: должна создаваться и возвращаться подзадача с ID")
        void createSubTask_shouldCreateAndReturnsSubtaskWitchId() throws IOException {

            //эпик и подзадача для него
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, epicId);

            //когда подзадача создана
            int subTaskId = taskManager.createSubTask(subTask);

            //проверяем, что идентификатор подзадачи не равен 0 и подзадача создана и добавлена в эпик
            assertNotEquals(0, subTaskId, "ID подзадачи не должен быть равен 0");
            assertEquals(subTask, taskManager.getSubTaskById(subTaskId), "Подзадача создана");
            assertTrue(taskManager.getSubTasksByEpicId(epicId).contains(subTask), "Подзадача добавлена в список");
        }

        @Test
        @DisplayName("Подзадача не может быть добавлена в саму себя")
        void subTaskCannotAddItselfAsParent() {

            //Given - подзадача с самим себя в качестве родителя
            int invalidEpicId = taskManager.generateId();
            SubTask subTask = new SubTask(invalidEpicId, "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, invalidEpicId);

            //создаем подзадачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(subTask));
        }

        @Test
        @DisplayName("Должно выбрасываться исключение, когда эпика не существует")
        void createSubTask_shouldThrowWhenEpicNotExist() {

            //подзадача с несуществующим эпиком
            SubTask subTask = new SubTask(taskManager.generateId(), "Подзадача 1",
                    "Описание подзадачи 1", StatusTask.NEW, 999);

            //создаем подзадачу и ожидаем исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.createSubTask(subTask));
        }

    }

    @Nested
    @DisplayName("Тесты методов получения задач, подзадач и эпиков")
    class GetTaskAndSubTaskAndEpicTest {
        private  Task task1;
        private  Task task2;

        @BeforeEach
        void setUpGetTaskTest() throws IOException {
            task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            taskManager.createTask(task1);
            taskManager.createTask(task2);
        }

        @Test
        @DisplayName("должен возвращаться пустой список, если задач нет")
        void getAllTasks_shouldReturnEmptyListWhenNoTasks() throws IOException {

            //удаляем все задачи
            taskManager.deleteAllTasks();

            //получаем все задачи
            taskManager.getAllTasks();

            //проверяем, что список пуст
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("должны возвращаться все созданные задачи")
        void getAllTasks_shouldReturnAllCreatedTasks() {

            //когда задачи созданы
            taskManager.getAllTasks();

            //проверяем, что список содержит две задачи
            assertEquals(2, taskManager.getAllTasks().size());
            assertTrue(taskManager.getAllTasks().contains(task1));
            assertTrue(taskManager.getAllTasks().contains(task2));
        }

        @Test
        @DisplayName("должен возвращаться null, если задача не найдена")
        void getTaskById_shouldReturnNullWhenTaskNotFound() {

            //используем несуществующий ID
            int invalidId = 999;

            //получаем задачу по несуществующему ID
            taskManager.getTaskById(invalidId);

            //проверяем, что задача не найдена(возвращен null)
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("Подзадача с несуществующим ID должна возвращаться null")
        void getSubTaskById_shouldReturnNullWhenSubTaskNotFound() {
            //используем несуществующий ID
            int invalidId = 999;

            //получаем подзадачу по несуществующему ID
            taskManager.getSubTaskById(invalidId);

            //проверяем, что подзадача не найдена(возвращен null)
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("Эпик с несуществующим ID должен возвращаться null")
        void getEpicById_shouldReturnNullWhenEpicNotFound() {
            // используем несуществующий ID
            int invalidId = 999;

            // получаем эпик по несуществующему ID
            taskManager.getEpicById(invalidId);

            // проверяем, что епик не найден(возвращен null)
            assertNull(taskManager.getEpicById(999));
        }
    }

    @Nested
    @DisplayName("Тесты метода getSubTasksByEpicId")
    class GetSubTasksByEpicIdTest {
        @Test
        @DisplayName("должен возвращаться пустой список, если подзадач нет")
        void getSubTasksByEpicId_shouldReturnEmptyListWhenSubtasks() throws IOException {

            //эпик без подзадач
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);

            //получаем подзадачи по ID
            List<SubTask> subTasks = taskManager.getSubTasksByEpicId(epic.getId());

            //проверяем, что список пуст
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
        @DisplayName("должна обновляться существующая задача")
        void updateTask_shouldUpdateExistingTask() throws IOException {

            //обновляем задачу
            Task updatedTask = new Task(task.getId(), "Updated Task 1",
                    "Updated Task 1 description", StatusTask.IN_PROGRESS);
            updatedTask.setId(taskId);
            taskManager.updateTask(updatedTask);

            //проверяем, что задача обновлена
            assertEquals(updatedTask, taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("должно выбрасывать исключение когда задачи не существует")
        void updateTask_shouldThrowWhenTaskNotExist() {

            //обновляем задачу с несуществующим ID
            task.setId(taskId + 1);

            //проверяем, что исключение выбрасывается
            assertThrows(IllegalArgumentException.class, () -> taskManager.updateTask(task));
        }

        @Test
        @DisplayName("должен устанавливаться статус NEW, если нет подзадач")
        void updateEpicStatus_shouldSetNewWhenNoSubtasks() {

            //проверяем, что статус установлен NEW
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика NEW, если все подзадачи NEW")
        void epicStatusShouldBeNewWhenAllSubtasksNew() throws IOException {
            //создаем эпик и подзадачу
            SubTask subTask2 = new SubTask(taskManager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.NEW, epicId);

            //добавляем подзадачи
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTask2);

            //проверяем, что статус установлен NEW
            assertEquals(StatusTask.NEW, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если подзадачи разных статусов (NEW и DONE)")
        void epicStatusShouldBeInProgressWhenSubtasksMixed() throws IOException {

            //добавляем подзадачи
            taskManager.createSubTask(subTaskNew);
            taskManager.createSubTask(subTaskDone);

            //проверяем, что статус установлен IN_PROGRESS
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика DONE, если все подзадачи DONE")
        void epicStatusShouldBeDoneWhenAllSubtasksDone() throws IOException {

            //добавляем подзадачи
            taskManager.createSubTask(subTaskDone);
            taskManager.createSubTask(subTaskDone);

            //проверяем, что статус установлен DONE
            assertEquals(StatusTask.DONE, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Статус эпика IN_PROGRESS, если хотя бы одна подзадача IN_PROGRESS")
        void epicStatusShouldBeInProgressWhenAnySubtaskInProgress() throws IOException {

            //добавляем подзадачи
            taskManager.createSubTask(subTaskInProgress);
            taskManager.createSubTask(subTaskNew);

            //проверяем, что статус установлен IN_PROGRESS
            assertEquals(StatusTask.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
        }

        @Test
        @DisplayName("Обновление подзадачи с несуществующим эпиком")
        void updateSubTask_shouldThrowWhenEpicNotExist() {
            //создаем подзадачу с несуществующим ID
            subTaskNew = new SubTask(subTaskNew.getId(), "SubTask 1",
                    "SubTask 1 description", StatusTask.NEW, epicId + 1);

            assertThrows(IllegalArgumentException.class, ()
                    -> taskManager.updateSubTask(subTaskNew));
        }

        @Test
        @DisplayName("Обновление подзадачи с null выбросит исключение")
        void updateSubTask_shouldThrowWhenSubTaskIsNull() {
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
        @DisplayName("должно завершаться без ошибок")
        void deleteNonExistentTask_shouldNotThrow() {

            //проверяем, что не выдает исключений(задача не существует)
            assertDoesNotThrow(() -> taskManager.deleteTaskById(999));
            assertNull(taskManager.getTaskById(999));
        }

        @Test
        @DisplayName("должно выдать исключение")
        void deleteNonExistentEpic_shouldNotThrow() {

            //проверяем, что выдает исключение
            assertThrows(IllegalArgumentException.class, () -> taskManager.deleteEpicById(999));
            assertNull(taskManager.getEpicById(999));
        }

        @Test
        @DisplayName("должно завершаться без ошибок")
        void deleteNonExistentSubtask_shouldNotThrow() {

            //проверяем, что не выдает исключений(подзадача не существует)
            assertDoesNotThrow(() -> taskManager.deleteSubTaskById(999));
            assertNull(taskManager.getSubTaskById(999));
        }

        @Test
        @DisplayName("должна удаляться задача")
        void deleteTaskById_shouldRemoveTask() throws IOException {

            //удаляем задачу по её ID
            taskManager.deleteTaskById(taskId);

            //проверяем, что задача удалена
            assertNull(taskManager.getTaskById(taskId));
        }

        @Test
        @DisplayName("должны удаляться все задачи")
        void deleteAllTasks_shouldRemoveAllTasks() throws IOException {

            //удаляем все задачи
            taskManager.deleteAllTasks();

            //проверяем, что все задачи удалены и список пуст
            assertTrue(taskManager.getAllTasks().isEmpty());
        }

        @Test
        @DisplayName("должен удаляться эпик и его подзадачи")
        void deleteEpicById_shouldRemoveAllSubtasks() throws IOException {

            //удаляем эпик по его ID
            taskManager.deleteEpicById(epicId);

            //проверяем, что удалены все задачи и список пуст
            assertNull(taskManager.getEpicById(epicId));
            assertNull(taskManager.getSubTaskById(subTaskId));
        }

        @Test
        @DisplayName("должна удаляться подзадача и обновляться эпик")
        void deleteSubTaskById_shouldRemoveSubtaskAndUpdateEpic() throws IOException {


            //удаляем подзадачу по её ID
            taskManager.deleteSubTaskById(subTaskId);

            //проверяем что подзадачи удалены и список подзадач эпика пуст
            assertNull(taskManager.getSubTaskById(subTaskId));
            assertTrue(taskManager.getSubTasksByEpicId(epicId).isEmpty());
        }

        @Test
        @DisplayName("история просмотров должна очищаться")
        void deleteAllTasks_shouldClearHistory() throws IOException {

            //удаляем все задачи
            taskManager.deleteAllTasks();

            //проверяем, что список задач и история просмотров очищена
            assertTrue(taskManager.getAllTasks().isEmpty());
            assertTrue(historyManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление всех эпиков должно удалять все подзадачи")
        void deleteAllEpics_shouldRemoveAllSubtasks() throws IOException {
            taskManager.deleteAllEpics();
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

            //проверяем равенство задач и их хеш-кодов
            assertEquals(task1, task2);
            assertEquals(task1.hashCode(), task2.hashCode());
        }

        @Test
        @DisplayName("Эпики равны, если имеют одинаковый ID")
        void epicsWithSameIdShouldBeEqual() {

            //проверяем равенство эпиков и их хеш-кодов
            assertEquals(epic1, epic2);
            assertEquals(epic1.hashCode(), epic2.hashCode());
        }

        @Test
        @DisplayName("Подзадачи равны, если имеют одинаковый ID")
        void subtasksWithSameIdShouldBeEqual() {

            //проверяем равенство подзадач и их хеш-кодов
            assertEquals(subtask1, subtask2);
            assertEquals(subtask1.hashCode(), subtask2.hashCode());
        }
    }

    @Nested
    @DisplayName("Тесты истории просмотров")
    class HistoryTest {

        @Test
        @DisplayName("Получение истории: должны возвращаться просмотренные задачи")
        void getHistory_shouldReturnViewedTasks() throws IOException {

            //созданы задача и эпик
            Task task = new Task(taskManager.generateId(), "Task 1", "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            //задачи просмотрены и запрошена история
            taskManager.getTaskById(taskId);
            taskManager.getEpicById(epicId);
            List<Task> history = (List<Task>) taskManager.getHistory();

            //проверяем, что история содержит задачу и эпик
            assertEquals(2, history.size());
            assertTrue(history.contains(task));
            assertTrue(history.contains(epic));
            assertTrue(history.stream().anyMatch(t -> t.getId() == taskId));
            assertTrue(history.stream().anyMatch(t -> t.getId() == epicId));
        }

        @Test
        @DisplayName("Повторный просмотр задачи: история не должна дублироваться")
        void shouldNotDuplicateHistoryWhenTaskViewedAgain() throws IOException {
            //создана задача
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);

            //задача просмотрена дважды
            taskManager.getTaskById(taskId);
            taskManager.getTaskById(taskId);

            //проверяем, что история содержит одну запись
            List<Task> history = (List<Task>) taskManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.getFirst());
        }

        @Test
        @DisplayName("Удаление задачи: задача должна удаляться из истории")
        void shouldRemoveTaskFromHistoryWhenTaskDeleted() throws IOException {
            //создана задача
            Task task = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = taskManager.createTask(task);
            taskManager.getTaskById(taskId);

            //задача удалена
            taskManager.deleteTaskById(taskId);

            //проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление эпика: эпик и его подзадачи должны удаляться из истории")
        void shouldRemoveEpicAndSubtasksFromHistoryWhenEpicDeleted() throws IOException {
            //создан эпик и подзадачи
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
        void shouldRemoveSubtaskFromHistoryWhenSubtaskDeleted() throws IOException {
            //созданы эпик и подзадача
            Epic epic = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = taskManager.createEpic(epic);

            SubTask subTask = new SubTask(taskManager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            int subTaskId = taskManager.createSubTask(subTask);

            //подзадача просмотрена и удалена
            taskManager.getSubTaskById(subTaskId);
            taskManager.deleteSubTaskById(subTaskId);

            //проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Удаление задачи не должно влиять на другие задачи в истории")
        void shouldNotAffectOtherTasksInHistoryWhenDeletingOneTask() throws IOException {

            //созданы две задачи
            Task task1 = new Task(taskManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(taskManager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.IN_PROGRESS);

            int taskId1 = taskManager.createTask(task1);
            int taskId2 = taskManager.createTask(task2);
            //обе задачи просмотрены и одна удалена
            taskManager.getTaskById(taskId1);
            taskManager.getTaskById(taskId2);
            taskManager.deleteTaskById(taskId2);

            //проверяем, что история содержит только одну запись
            assertEquals(1, taskManager.getHistory().size());
            assertTrue(taskManager.getHistory().contains(task1));

        }


        @Test
        @DisplayName("Очистка всех задач")
        void shouldClearHistoryWhenAllTasksDeleted() throws IOException {
            //созданы две задачи и просмотрены
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
            // все задачи удалены
            taskManager.deleteAllTasks();

            //проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех эпиков")
        void shouldClearHistoryWhenAllEpicsDeleted() throws IOException {
            //созданы два эпика и просмотрены
            Epic epic1 = new Epic(taskManager.generateId(), "Epic 1", "Epic 1 description");
            Epic epic2 = new Epic(taskManager.generateId(), "Epic 2", "Epic 2 description");

            int epicId1 = taskManager.createEpic(epic1);
            int epicId2 = taskManager.createEpic(epic2);

            taskManager.getEpicById(epicId1);
            taskManager.getEpicById(epicId2);

            //удаляем все эпики
            taskManager.deleteAllEpics();

            //проверяем, что история пуста
            assertTrue(taskManager.getHistory().isEmpty());
        }

        @Test
        @DisplayName("Очистка всех подзадач")
        void shouldClearHistoryWhenAllSubtasksDeleted() throws IOException {

            //созданы эпик и две его подзадачи и просмотрены
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


            //удаляем все подзадачи
            taskManager.deleteAllSubTasks();

            //проверяем, что история пуста
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
        void setUpImmutabilityTest() throws IOException {
            taskId = taskManager.generateId();
            epicId = taskManager.generateId();
            subTaskId = taskManager.generateId();
            epic = new Epic(epicId, "Epic 1", "Epic 1 description");
            taskManager.createEpic(epic);
        }

        @Test
        @DisplayName("Задача остается неизменной при добавлении в менеджер")
        void taskRemainsUnchangedWhenAddedToManager() throws IOException {
            //создаём оригинальную задачу и её копию, генерируем ID
            Task original = new Task(taskId, "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task copy = new Task(original.getId(), original.getName(),
                    original.getDescription(), original.getStatus());
            //добавляем задачу в менеджер
            taskManager.createTask(original);

            //проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
        }

        @Test
        @DisplayName("Эпик остается неизменной при добавлении в менеджер")
        void epicRemainsUnchangedWhenAddedToManager() throws IOException {
            //создаём оригинальный эпик и его копию, генерируем ID
            Epic original = new Epic(epicId, "Epic 1", "Epic 1 description");
            Epic copy = new Epic(original.getId(), original.getName(), original.getDescription());

            //добавляем эпик в менеджер
            taskManager.createEpic(original);

            //проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
        }

        @Test
        @DisplayName("Подзадача остается неизменной при добавлении в менеджер")
        void subtaskRemainsUnchangedWhenAddedToManager() throws IOException {
            //Создаём оригинальную подзадачу и ее копию, генерируем ID
            SubTask original = new SubTask(subTaskId, "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask copy = new SubTask(original.getId(), original.getName(),
                    original.getDescription(), original.getStatus(), original.getEpicId());

            //добавляем подзадачу в менеджер
            taskManager.createSubTask(original);

            //проверяем, что копия равна оригиналу
            assertEquals(copy, original);
            assertEquals(copy.getName(), original.getName());
            assertEquals(copy.getDescription(), original.getDescription());
            assertEquals(copy.getStatus(), original.getStatus());
            assertEquals(copy.getEpicId(), original.getEpicId());
        }
    }
}