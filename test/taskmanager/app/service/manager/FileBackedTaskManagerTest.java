package taskmanager.app.service.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.exception.ManagerSaveException;
import taskmanager.app.management.TaskManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты файлового менеджера задач")
class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;
    private TaskManager manager;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test_tasks.csv");
        manager = new FileBackedTasksManager(testFile);
    }

    @Nested
    @DisplayName("Тесты сохранения и загрузки")
    class SaveAndLoadTest {

        @Test
        @DisplayName("Сохранение и загрузка задач")
        void testShouldSaveAndLoadTasks() throws IOException {
            //Given
            Task task = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = manager.createTask(task);
            Task original = manager.getTaskById(taskId);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Task loadedTask = loadedManager.getTaskById(taskId);

            //Then
            assertNotNull(loadedTask);
            assertEquals(original, loadedTask);
        }

        @Test
        @DisplayName("Сохранение и загрузка эпика")
        void testShouldSaveAndLoadEpic() throws IOException {
            //Given
            Epic epic = new Epic(manager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(manager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);

            manager.createSubTask(subTask);
            manager.createSubTask(subTask2);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Epic loadedEpic = loadedManager.getEpicById(epicId);

            //Then
            assertNotNull(loadedEpic);
            assertEquals(epic.getName(), loadedEpic.getName());
            assertEquals(epic.getDescription(), loadedEpic.getDescription());
            assertEquals(2, loadedEpic.getSubTaskIds().size());
            assertEquals(StatusTask.IN_PROGRESS, loadedEpic.getStatus());
        }

        @Test
        @DisplayName("Обработка пустого файла")
        void testShouldHandleEmptyFile() throws IOException {
            //Given & When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);

            //Then
            assertTrue(loadedManager.getAllTasks().isEmpty());
            assertTrue(loadedManager.getAllEpics().isEmpty());
            assertTrue(loadedManager.getAllSubTasks().isEmpty());
        }
    }

    @Nested
    @DisplayName("CSV форматирование")
    class CsvFormattingTest {

        @Test
        @DisplayName("тест экранирования спецсимволов")
        void testShouldEscapeSpecialCharacters() throws IOException {
            //Given
            Task task = new Task(manager.generateId(), "Task, with \"quotes\"",
                    "Description, with\nnewline", StatusTask.NEW);
            int taskId = manager.createTask(task);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Task loadedTask = loadedManager.getTaskById(taskId);

            //Then
            assertEquals("Task, with \"quotes\"", loadedTask.getName());
            assertEquals("Description, with\nnewline", loadedTask.getDescription());
        }

        @Test
        @DisplayName("тест парсинга строк")
        void testShouldParseLines() throws IOException {
            //Given
            String csvLines = "1,TASK,\"Task, with \"\"quotes\"\"\",NEW,\"Description, with\nnewline\",";

            //When
            Task task = FileBackedTasksManager.fromString(csvLines);

            //Then
            assertEquals(1, task.getId());
            assertEquals("Task, with \"quotes\"", task.getName());
            assertEquals("Description, with\nnewline", task.getDescription());
            assertEquals(StatusTask.NEW, task.getStatus());
        }
    }

    @Nested
    @DisplayName("Управление идентификаторами")
    class IdManagementTest {

        @Test
        @DisplayName("Тест генерации ID")
        void testShouldGenerateId() throws IOException {
            //Given
            Task task1 = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);

            Task task2 = new Task(manager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.NEW);

            //When
            int id1 = manager.createTask(task1);
            int id2 = manager.createTask(task2);

            //Then
            assertEquals(id1 + 1, id2);
        }

        @Test
        @DisplayName("Восстановление максимального идентификатора при загрузке")
        void testShouldRestoreMaxId() throws IOException {
            //Given
            Task task1 = new Task(5, "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(10, "Task 2",
                    "Task 2 description", StatusTask.NEW);

            manager.createTask(task1);
            manager.createTask(task2);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Task newTask = new Task(loadedManager.generateId(), "New Task",
                    "New Task description", StatusTask.NEW);

            int newId = loadedManager.createTask(newTask);

            //Then
            assertEquals(11, newId);
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Тест обработки ошибок при сохранении")
        void testShouldHandleSaveError() throws IOException {
            //Given & When
            Path readOnlyFile = tempDir.resolve("readonly.csv");
            Files.createFile(readOnlyFile);

            readOnlyFile.toFile().setReadOnly();

            FileBackedTasksManager readOnlyManager = new FileBackedTasksManager(readOnlyFile);
            Task task = new Task(readOnlyManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);

            //Then
            assertThrows(ManagerSaveException.class, () -> readOnlyManager.createTask(task)
            );
        }

        @Test
        @DisplayName("Тест обработки ошибок при неправильном CSV")
        void testShouldHandleInvalidCsv() {
            //Then
            assertThrows(IllegalArgumentException.class, () ->
                    FileBackedTasksManager.fromString("invalid,csv,line")
            );
        }

        @Test
        @DisplayName("Тест обработки ошибок при отсутствии epicId для подзадачи")
        void testShouldHandleMissingEpicIdForSubtask() {
            //Given & When
            String invalidSubtaskLine = "1,SUBTASK,Subtask 1,NEW,Subtask 1 description,";

            //Then
            assertThrows(IllegalArgumentException.class, () ->
                    FileBackedTasksManager.fromString(invalidSubtaskLine)
            );
        }
    }

    @Nested
    @DisplayName("Восстановление связей")
    class RelationshipRestorationTest {

        @Test
        @DisplayName("должен восстанавливать связи между эпиками и подзадачами")
        void testShouldRestoreRelationships() throws IOException {
            //Given
            Epic epic = new Epic(manager.generateId(), "Epic 1",
                    "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            manager.createSubTask(subTask);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Epic loadedEpic = loadedManager.getEpicById(epicId);

            //Then
            assertNotNull(loadedEpic);
            assertEquals(1, loadedEpic.getSubTaskIds().size());
            assertTrue(loadedEpic.getSubTaskIds().contains(subTask.getId()));
        }

        @Test
        @DisplayName("должен вычислять статус эпика")
        void testShouldCalculateEpicStatus() throws IOException {
            //Given
            Epic epic = new Epic(manager.generateId(), "Epic 1",
                    "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask1 = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(manager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);
            manager.createSubTask(subTask1);
            manager.createSubTask(subTask2);

            //When
            Epic restoredEpic = manager.getEpicById(epicId);
            //Then
            assertEquals(StatusTask.IN_PROGRESS, restoredEpic.getStatus());
        }

        @Test
        @DisplayName("должен напрямую восстанавливать задачу")
        void testShouldRestoreTaskDirectly() throws IOException {
            //Given
            Task task = new Task(100, "Task 100",
                    "Task 100 description", StatusTask.IN_PROGRESS);

            manager.createTask(task);

            //When
            Task restoredTask = manager.getTaskById(100);
            //Then
            assertNotNull(restoredTask);
            assertEquals("Task 100", restoredTask.getName());
        }

        @Test
        @DisplayName("должен напрямую восстанавливать подзадачу")
        void testShouldDirectlyRestoreSubTask() throws IOException {
            //Given
            Epic epic = new Epic(300, "Epic", "Description");
            manager.createEpic(epic);

            SubTask subTask = new SubTask(301, "SubTask",
                    "Description", StatusTask.DONE, 300);
            manager.createSubTask(subTask);

            //When
            SubTask restoredSubTask = manager.getSubTaskById(301);
            //Then
            assertNotNull(restoredSubTask);
            assertEquals("SubTask", restoredSubTask.getName());
            assertEquals(300, restoredSubTask.getEpicId());
        }

        @Test
        @DisplayName("должен создавать файл при его отсутствии")
        void testShouldCreateFileIfNotExists() throws IOException {
            //Given
            Path nonExistentFile = tempDir.resolve("non-existent.csv");
            FileBackedTasksManager newManager = new FileBackedTasksManager(nonExistentFile);

            Task task = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            //When
            newManager.createTask(task);

            //Then
            assertTrue(Files.exists(nonExistentFile));
        }

        @Test
        @DisplayName("должен сохранять после каждого изменения")
        void testShouldSaveAfterEachChange() throws IOException {
            //Given
            long initialSize = getFileSize();

            Task task = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            manager.createTask(task);

            //When
            long afterCreationSize = getFileSize();
            assertTrue(afterCreationSize > initialSize);

            task.setStatus(StatusTask.DONE);
            manager.updateTask(task);

            //Then
            long afterUpdateSize = getFileSize();
            assertTrue(afterUpdateSize > afterCreationSize);
        }

        private long getFileSize() {
            try {
                return Files.size(testFile);
            } catch (IOException e) {
                return -1;
            }
        }
    }
}
