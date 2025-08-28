package test.service;

import contracts.TaskManager;
import core.*;
import exceptions.ManagerSaveException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import service.FileBackedTaskManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.*;


public class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;
    private TaskManager manager;
    private Path testFile;

    @BeforeEach
    public void setUp() {
        testFile = tempDir.resolve("test_tasks.csv");
        manager = new FileBackedTaskManager(testFile);
    }

    @Nested
    @DisplayName("Тесты сохранения и загрузки")
    class SaveAndLoadTest {

        @Test
        @DisplayName("Сохранение и загрузка задач")
        void shouldSaveAndLoadTasks() throws IOException {
            // Создаем задачу
            Task task = new Task(manager.generateId(),"Task 1",
                    "Task 1 description", StatusTask.NEW);
            int taskId = manager.createTask(task);
            Task original = manager.getTaskById(taskId);

            //Загружаем менеджер из файла
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);
            Task loadedTask = loadedManager.getTaskById(taskId);

            //Проверяем, что загруженная задача совпадает с оригинальной
            assertNotNull(loadedTask);
            assertEquals(original, loadedTask);
        }

        @Test
        @DisplayName("Сохранение и загрузка эпика")
        void shouldSaveAndLoadEpic() throws IOException {
            //Создаем эпик и подзадачи
            Epic epic = new Epic(manager.generateId(), "Epic 1", "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(manager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);

            manager.createSubTask(subTask);
            manager.createSubTask(subTask2);

            //Загружаем менеджер из файла
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);
            Epic loadedEpic = loadedManager.getEpicById(epicId);

            //Проверяем, что загруженный Epic совпадает с оригинальным
            assertNotNull(loadedEpic);
            assertEquals(epic.getName(), loadedEpic.getName());
            assertEquals(epic.getDescription(), loadedEpic.getDescription());
            assertEquals(2, loadedEpic.getSubTaskIds().size());
            assertEquals(StatusTask.IN_PROGRESS, loadedEpic.getStatus());
        }

        @Test
        @DisplayName("Обработка пустого файла")
        void shouldHandleEmptyFile() throws IOException {
            //Создаем пустой файл
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);

            //Проверяем, что менеджер пустой
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
        void shouldEscapeSpecialCharacters() throws IOException {
            //Задача со спецсимволами
            Task task = new Task(manager.generateId(), "Task, with \"quotes\"",
                    "Description, with\nnewline", StatusTask.NEW);
            int taskId = manager.createTask(task);

            //Загружаем менеджер из файла
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);
            Task loadedTask = loadedManager.getTaskById(taskId);

            //Проверяем, что спецсимволы корректно обработаны
            assertEquals("Task, with \"quotes\"", loadedTask.getName());
            assertEquals("Description, with\nnewline", loadedTask.getDescription());
        }

        @Test
        @DisplayName("Тест парсинга строк")
        void shouldParseLines() throws IOException {
            //Строка CSV с экранированием
            String csvLines = "1,TASK,\"Task, with \"\"quotes\"\"\",NEW,\"Description, with\nnewline\",";

            //Парсим строку
            Task task = FileBackedTaskManager.fromString(csvLines);

            //Проверяем, что парсинг корректен
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
        void shouldGenerateId() throws IOException {
            //Создаем две задачи
            Task task1 = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);

            Task task2 = new Task(manager.generateId(), "Task 2",
                    "Task 2 description", StatusTask.NEW);

            //создаем их в менеджере
            int id1 = manager.createTask(task1);
            int id2 = manager.createTask(task2);

            //Проверяем, что ID генерируются корректно
            assertEquals(id1 + 1, id2);
        }

        @Test
        @DisplayName("Восстановление максимального идентификатора при загрузке")
        void shouldRestoreMaxId() throws IOException {
            //Создаем две задачи
            Task task1 = new Task(5, "Task 1",
                    "Task 1 description", StatusTask.NEW);
            Task task2 = new Task(10, "Task 2",
                    "Task 2 description", StatusTask.NEW);

            manager.createTask(task1);
            manager.createTask(task2);

            //Загружаем менеджер и создаем новую задачу
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);
            Task newTask = new Task(loadedManager.generateId(), "New Task",
                    "New Task description", StatusTask.NEW);

            int newId = loadedManager.createTask(newTask);

            //Проверяем, что новый ID совпадает с ожидаемым
            assertEquals(11, newId);
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Тест обработки ошибок при сохранении")
        void shouldHandleSaveError() throws IOException {
            //Создаем файл только для чтения
            Path readOnlyFile = tempDir.resolve("readonly.csv");
            Files.createFile(readOnlyFile);

            readOnlyFile.toFile().setReadOnly();

            FileBackedTaskManager readOnlyManager = new FileBackedTaskManager(readOnlyFile);
            Task task = new Task(readOnlyManager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);

            //Проверяем, что выбрасывается исключение при сохранении
            assertThrows(ManagerSaveException.class, () -> readOnlyManager.createTask(task)
            );
        }

        @Test
        @DisplayName("Тест обработки ошибок при неправильном CSV")
        void shouldHandleInvalidCsv() throws IOException {
            //Проверяем, что выбрасывается исключение при неправильном CSV
            assertThrows(IllegalArgumentException.class, () ->
                    FileBackedTaskManager.fromString("invalid,csv,line")
            );
        }

        @Test
        @DisplayName("Тест обработки ошибок при отсутствии epicId для подзадачи")
        void shouldHandleMissingEpicIdForSubtask() throws IOException {
            //Строка подзадачи без epicId
            String invalidSubtaskLine = "1,SUBTASK,Subtask 1,NEW,Subtask 1 description,";

            //Проверяем, что выбрасывается исключение при неправильной строке
            assertThrows(IllegalArgumentException.class, () ->
                    FileBackedTaskManager.fromString(invalidSubtaskLine)
            );
        }
    }

    @Nested
    @DisplayName("Восстановление связей")
    class RelationshipRestorationTest {

        @Test
        @DisplayName("должен восстанавливать связи между эпиками и подзадачами")
        void shouldRestoreRelationships() throws IOException {
            //Эпик с подзадачей
            Epic epic = new Epic(manager.generateId(), "Epic 1",
                    "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            manager.createSubTask(subTask);

            //Загружаем менеджер
            FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(testFile);
            Epic loadedEpic = loadedManager.getEpicById(epicId);

            //Проверяем, что связи восстановлены корректно
            assertNotNull(loadedEpic);
            assertEquals(1, loadedEpic.getSubTaskIds().size());
            assertTrue(loadedEpic.getSubTaskIds().contains(subTask.getId()));
        }

        @Test
        @DisplayName("должен вычислять статус эпика")
        void shouldCalculateEpicStatus() throws IOException {
            //эпик с подзадачами
            Epic epic = new Epic(manager.generateId(), "Epic 1",
                    "Epic 1 description");
            int epicId = manager.createEpic(epic);

            SubTask subTask1 = new SubTask(manager.generateId(), "Subtask 1",
                    "Subtask 1 description", StatusTask.NEW, epicId);
            SubTask subTask2 = new SubTask(manager.generateId(), "Subtask 2",
                    "Subtask 2 description", StatusTask.DONE, epicId);
            manager.createSubTask(subTask1);
            manager.createSubTask(subTask2);

            //Получаем эпик
            Epic restoredEpic = manager.getEpicById(epicId);
            //Проверяем, что статус вычислен корректно
            assertEquals(StatusTask.IN_PROGRESS, restoredEpic.getStatus());
        }

        @Test
        @DisplayName("должен напрямую восстанавливать задачу")
        void shouldRestoreTaskDirectly() throws IOException {
            //Задача с определенным ID
            Task task = new Task(100, "Task 100",
                    "Task 100 description", StatusTask.IN_PROGRESS);

            manager.createTask(task);

            //Получаем задачу по ID
            Task restoredTask = manager.getTaskById(100);
            //Проверяем, что задача восстановлена
            assertNotNull(restoredTask);
            assertEquals("Task 100", restoredTask.getName());
        }

        @Test
        @DisplayName("должен напрямую восстанавливать подзадачу")
        void shouldDirectlyRestoreSubTask() throws IOException {
            //Подзадача с определенным ID
            Epic epic = new Epic(300, "Epic", "Description");
            manager.createEpic(epic);

            SubTask subTask = new SubTask(301, "SubTask", "Description", StatusTask.DONE, 300);
            manager.createSubTask(subTask);

            //Получаем подзадачу по ID
            SubTask restoredSubTask = manager.getSubTaskById(301);
            //Проверяем, что подзадача восстановлена
            assertNotNull(restoredSubTask);
            assertEquals("SubTask", restoredSubTask.getName());
            assertEquals(300, restoredSubTask.getEpicId());
        }

        @Test
        @DisplayName("Должен создавать файл при его отсутствии")
        void shouldCreateFileIfNotExists() throws IOException {
            //создаем менеджер с несуществующим файлом
            Path nonExistentFile = tempDir.resolve("non-existent.csv");
            FileBackedTaskManager newManager = new FileBackedTaskManager(nonExistentFile);

            Task task = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            //создаем задачу
            newManager.createTask(task);

            //проверяем, что файл создан
            assertTrue(Files.exists(nonExistentFile));
        }

        @Test
        @DisplayName("Должен сохранять после каждого изменения")
        void shouldSaveAfterEachChange() throws IOException {
            //Начальный размер файла
            long initialSize = getFileSize();

            //Создаем задачу
            Task task = new Task(manager.generateId(), "Task 1",
                    "Task 1 description", StatusTask.NEW);
            manager.createTask(task);
            //Проверяем, что размер файла увеличился
            long afterCreationSize = getFileSize();
            assertTrue(afterCreationSize > initialSize);

            //Обновляем задачу
            task.setStatus(StatusTask.DONE);
            manager.updateTask(task);

            //Проверяем, что размер файла увеличился
            long afterUpdateSize = getFileSize();
            assertTrue(afterUpdateSize > afterCreationSize);
        }

        private long getFileSize()  {
            try {
                return Files.size(testFile);
            } catch (IOException e) {
                return -1;
            }
        }
    }
}
