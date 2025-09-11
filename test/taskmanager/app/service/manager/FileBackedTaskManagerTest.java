package taskmanager.app.service.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import taskmanager.app.entity.*;
import taskmanager.app.exception.ManagerSaveException;
import taskmanager.app.management.TaskManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
            Optional<Task> original = manager.getTaskById(taskId);

            //When
            FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
            Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

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
            Optional<Epic> loadedEpic = loadedManager.getEpicById(epicId);

            //Then
            assertTrue(loadedEpic.isPresent(), "Эпик должен существовать");
            assertEquals(epic.getName(), loadedEpic.get().getName());
            assertEquals(epic.getDescription(), loadedEpic.get().getDescription());
            assertEquals(2, loadedEpic.get().getSubTaskIds().size());
            assertEquals(StatusTask.IN_PROGRESS, loadedEpic.get().getStatus());
        }

        @Test
        @DisplayName("Обработка пустого файла")
        void testShouldHandleEmptyFile() {
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
            Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

            //Then
            assertTrue(loadedTask.isPresent(), "Задача должна существовать");
            assertEquals("Task, with \"quotes\"", loadedTask.get().getName());
            assertEquals("Description, with\nnewline", loadedTask.get().getDescription());
        }

        @Test
        @DisplayName("тест парсинга строк")
        void testShouldParseLines() {
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

            boolean isReadOnly = readOnlyFile.toFile().setReadOnly();
            assertTrue(isReadOnly, "Файл должен быть установлен в режим только для чтения");

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
            Optional<Epic> loadedEpic = loadedManager.getEpicById(epicId);

            //Then
            assertTrue(loadedEpic.isPresent(), "Эпик должен существовать");
            assertEquals(1, loadedEpic.get().getSubTaskIds().size());
            assertTrue(loadedEpic.get().getSubTaskIds().contains(subTask.getId()));
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
            Optional<Epic> restoredEpic = manager.getEpicById(epicId);
            //Then
            assertTrue(restoredEpic.isPresent(), "Эпик должен существовать");
            assertEquals(StatusTask.IN_PROGRESS, restoredEpic.get().getStatus());
        }

        @Test
        @DisplayName("должен напрямую восстанавливать задачу")
        void testShouldRestoreTaskDirectly() throws IOException {
            //Given
            Task task = new Task(100, "Task 100",
                    "Task 100 description", StatusTask.IN_PROGRESS);

            manager.createTask(task);

            //When
            Optional<Task> restoredTask = manager.getTaskById(100);
            //Then
            assertTrue(restoredTask.isPresent(), "Задача должна существовать");
            assertEquals("Task 100", restoredTask.get().getName());
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
            Optional<SubTask> restoredSubTask = manager.getSubTaskById(301);
            //Then
            assertNotNull(restoredSubTask);
            assertTrue(restoredSubTask.isPresent(), "Подзадача должна существовать");
            assertEquals("SubTask", restoredSubTask.get().getName());
            assertEquals(300, restoredSubTask.get().getEpicId());
        }

        @Test
        @DisplayName("должен создавать файл при его отсутствии")
        void testShouldCreateFileIfNotExists() {
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

        @Nested
        @DisplayName("Тесты сохранения и загрузки всех полей")
        class AllFieldsSaveAndLoadTest {

            @Test
            @DisplayName("Сохранение и загрузка задачи со всеми полями включая время")
            void testShouldSaveAndLoadTaskWithAllFields() throws IOException {
                // Given
                LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);
                Duration duration = Duration.ofHours(2);

                Task task = new Task(manager.generateId(), "Complete Task",
                        "Task with all fields", StatusTask.IN_PROGRESS, duration, startTime);
                int taskId = manager.createTask(task);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

                // Then
                assertTrue(loadedTask.isPresent(), "Задача должна существовать");
                assertEquals("Complete Task", loadedTask.get().getName());
                assertEquals("Task with all fields", loadedTask.get().getDescription());
                assertEquals(StatusTask.IN_PROGRESS, loadedTask.get().getStatus());
                assertEquals(duration, loadedTask.get().getDuration());
                assertEquals(startTime, loadedTask.get().getStartTime());
                assertEquals(startTime.plus(duration), loadedTask.get().getEndTime());
            }

            @Test
            @DisplayName("Сохранение и загрузка эпика со всеми полями")
            void testShouldSaveAndLoadEpicWithAllFields() throws IOException {
                // Given
                Epic epic = new Epic(manager.generateId(), "Test Epic", "Epic description");
                int epicId = manager.createEpic(epic);

                LocalDateTime subTaskTime = LocalDateTime.of(2024, 1, 15, 11, 0);
                SubTask subTask = new SubTask(manager.generateId(), "SubTask",
                        "SubTask description", StatusTask.DONE, Duration.ofHours(1), subTaskTime, epicId);
                manager.createSubTask(subTask);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Epic> loadedEpic = loadedManager.getEpicById(epicId);

                // Then
                assertTrue(loadedEpic.isPresent(), "Эпик должен существовать");
                assertEquals("Test Epic", loadedEpic.get().getName());
                assertEquals("Epic description", loadedEpic.get().getDescription());
                assertEquals(StatusTask.DONE, loadedEpic.get().getStatus());
                assertNotNull(loadedEpic.get().getStartTime());
                assertNotNull(loadedEpic.get().getDuration());
                assertNotNull(loadedEpic.get().getEndTime());
            }

            @Test
            @DisplayName("Сохранение и загрузка подзадачи со всеми полями")
            void testShouldSaveAndLoadSubTaskWithAllFields() throws IOException {
                // Given
                Epic epic = new Epic(manager.generateId(), "Parent Epic", "Epic description");
                int epicId = manager.createEpic(epic);

                LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 12, 0);
                Duration duration = Duration.ofMinutes(45);

                SubTask subTask = new SubTask(manager.generateId(), "Detailed SubTask",
                        "SubTask with all details", StatusTask.IN_PROGRESS, duration, startTime, epicId);
                int subTaskId = manager.createSubTask(subTask);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<SubTask> loadedSubTask = loadedManager.getSubTaskById(subTaskId);

                // Then
                assertTrue(loadedSubTask.isPresent(), "Подзадача должна существовать");
                assertEquals("Detailed SubTask", loadedSubTask.get().getName());
                assertEquals("SubTask with all details", loadedSubTask.get().getDescription());
                assertEquals(StatusTask.IN_PROGRESS, loadedSubTask.get().getStatus());
                assertEquals(duration, loadedSubTask.get().getDuration());
                assertEquals(startTime, loadedSubTask.get().getStartTime());
                assertEquals(epicId, loadedSubTask.get().getEpicId());
            }
        }

        @Nested
        @DisplayName("Тесты обработки опциональных полей")
        class OptionalFieldsTest {

            @Test
            @DisplayName("Сохранение и загрузка задачи без времени и duration")
            void testShouldHandleTaskWithoutTimeFields() throws IOException {
                // Given
                Task task = new Task(manager.generateId(), "Simple Task",
                        "Task without time", StatusTask.NEW, null, null);
                int taskId = manager.createTask(task);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

                // Then
                assertTrue(loadedTask.isPresent(), "Задача должна существовать");
                assertNull(loadedTask.get().getStartTime());
                assertNull(loadedTask.get().getDuration());
                assertNull(loadedTask.get().getEndTime());
            }

            @Test
            @DisplayName("Сохранение и загрузка эпика без подзадач")
            void testShouldHandleEpicWithoutSubtasks() throws IOException {
                // Given
                Epic epic = new Epic(manager.generateId(), "Empty Epic",
                        "Epic without subtasks");
                int epicId = manager.createEpic(epic);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Epic> loadedEpic = loadedManager.getEpicById(epicId);

                // Then
                assertTrue(loadedEpic.isPresent(), "Эпик должен существовать");
                assertNull(loadedEpic.get().getStartTime());
                assertNull(loadedEpic.get().getDuration());
                assertNull(loadedEpic.get().getEndTime());
                assertEquals(StatusTask.NEW, loadedEpic.get().getStatus());
                assertTrue(loadedEpic.get().getSubTaskIds().isEmpty());
            }
        }

        @Nested
        @DisplayName("Тесты CSV формата и экранирования")
        class CsvFormatTest {

            @Test
            @DisplayName("Проверка корректности CSV формата всех полей")
            void testShouldHaveCorrectCsvFormatForAllFields() throws IOException {
                // Given
                LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 14, 30);
                Duration duration = Duration.ofMinutes(90);

                Task task = new Task(manager.generateId(), "CSV Test Task",
                        "Description for CSV test", StatusTask.DONE, duration, startTime);
                manager.createTask(task);

                // When
                String csvContent = Files.readString(testFile);

                // Then
                assertTrue(csvContent.contains("id,type,name,status,description,epic,start_time,duration,end_time"));
                assertTrue(csvContent.contains("TASK"));
                assertTrue(csvContent.contains("CSV Test Task"));
                assertTrue(csvContent.contains("DONE"));
                assertTrue(csvContent.contains("Description for CSV test"));
                assertTrue(csvContent.contains("2024-01-15T14:30:00"));
                assertTrue(csvContent.contains("90"));
            }

            @Test
            @DisplayName("Экранирование специальных символов в CSV")
            void testShouldEscapeSpecialCharactersInAllFields() throws IOException {
                // Given
                String nameWithSpecialChars = "Task, with \"quotes\" and\nnew line";
                String descriptionWithSpecialChars = "Description, with \"double\" quotes\nand line breaks";

                Task task = new Task(manager.generateId(), nameWithSpecialChars,
                        descriptionWithSpecialChars, StatusTask.NEW, null, null);
                int taskId = manager.createTask(task);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

                // Then
                assertTrue(loadedTask.isPresent(), "Задача должна существовать");
                assertEquals(nameWithSpecialChars, loadedTask.get().getName());
                assertEquals(descriptionWithSpecialChars, loadedTask.get().getDescription());
            }

            @Test
            @DisplayName("Парсинг CSV строки со всеми полями")
            void testShouldParseCsvLineWithAllFields() {
                // Given
                String csvLine = "1,TASK,Test Task,IN_PROGRESS,Test Description,,2024-01-15T10:00:00,120,";

                // When
                Task task = FileBackedTasksManager.fromString(csvLine);

                // Then
                assertEquals(1, task.getId());
                assertEquals(TaskType.TASK, task.getType());
                assertEquals("Test Task", task.getName());
                assertEquals(StatusTask.IN_PROGRESS, task.getStatus());
                assertEquals("Test Description", task.getDescription());
                assertEquals(LocalDateTime.of(2024, 1, 15, 10, 0), task.getStartTime());
                assertEquals(Duration.ofMinutes(120), task.getDuration());
            }
        }

        @Nested
        @DisplayName("Тесты порядка и целостности данных")
        class DataIntegrityTest {

            @Test
            @DisplayName("Сохранение порядка задач при записи/чтении")
            void testShouldMaintainTaskOrder() throws IOException {
                // Given
                Task task1 = new Task(manager.generateId(), "Task 1",
                        "Description 1", StatusTask.NEW, null, null);
                Task task2 = new Task(manager.generateId(), "Task 2",
                        "Description 2", StatusTask.IN_PROGRESS, null, null);

                manager.createTask(task1);
                manager.createTask(task2);

                Epic epic = new Epic(manager.generateId(), "Epic", "Epic description");
                manager.createEpic(epic);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                List<Task> tasks = loadedManager.getAllTasks();
                List<Epic> epics = loadedManager.getAllEpics();

                // Then
                assertEquals(2, tasks.size());
                assertEquals("Task 1", tasks.get(0).getName());
                assertEquals("Task 2", tasks.get(1).getName());
                assertEquals(1, epics.size());
                assertEquals("Epic", epics.getFirst().getName());
            }

            @Test
            @DisplayName("Целостность данных после многократного сохранения/загрузки")
            void testShouldMaintainDataIntegrityAfterMultipleSaves() throws IOException {
                // Given
                LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 9, 0);
                Duration duration = Duration.ofHours(3);

                Task originalTask = new Task(manager.generateId(), "Original Task",
                        "Original description", StatusTask.NEW, duration, startTime);
                int taskId = manager.createTask(originalTask);

                // When - первая загрузка
                FileBackedTasksManager firstLoad = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> firstLoadedTask = firstLoad.getTaskById(taskId);

                // Изменяем задачу и сохраняем снова
                firstLoadedTask.ifPresent(task -> {
                    task.setStatus(StatusTask.DONE);
                    firstLoad.updateTask(task);
                });

                // Вторая загрузка
                FileBackedTasksManager secondLoad = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> secondLoadedTask = secondLoad.getTaskById(taskId);

                // Then
                assertTrue(firstLoadedTask.isPresent(),
                        "Задача должна существовать при первой загрузке");
                assertTrue(secondLoadedTask.isPresent(),
                        "Задача должна существовать при второй загрузке");

                assertEquals(StatusTask.DONE, secondLoadedTask.get().getStatus());
                assertEquals("Original Task", secondLoadedTask.get().getName());
                assertEquals("Original description", secondLoadedTask.get().getDescription());
                assertEquals(duration, secondLoadedTask.get().getDuration());
                assertEquals(startTime, secondLoadedTask.get().getStartTime());
            }
        }

        @Nested
        @DisplayName("Тесты граничных случаев")
        class EdgeCasesTest {

            @Test
            @DisplayName("Обработка максимальных значений duration")
            void testShouldHandleMaxDuration() throws IOException {
                // Given
                Duration maxDuration = Duration.ofDays(365);
                LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 0, 0);

                Task task = new Task(manager.generateId(), "Long Task",
                        "Task with long duration", StatusTask.NEW, maxDuration, startTime);
                int taskId = manager.createTask(task);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

                // Then
                assertTrue(loadedTask.isPresent(), "Задача должна существовать");
                assertEquals(maxDuration, loadedTask.get().getDuration());
                assertEquals(startTime.plus(maxDuration), loadedTask.get().getEndTime());
            }

            @Test
            @DisplayName("Обработка нулевого duration")
            void testShouldHandleZeroDuration() throws IOException {
                // Given
                Duration zeroDuration = Duration.ZERO;
                LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);

                Task task = new Task(manager.generateId(), "Instant Task",
                        "Task with zero duration", StatusTask.NEW, zeroDuration, startTime);
                int taskId = manager.createTask(task);

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);
                Optional<Task> loadedTask = loadedManager.getTaskById(taskId);

                // Then
                assertTrue(loadedTask.isPresent(), "Задача должна существовать");
                assertEquals(zeroDuration, loadedTask.get().getDuration());
                assertEquals(startTime, loadedTask.get().getEndTime());
            }
        }

        @Nested
        @DisplayName("Тесты производительности с большими данными")
        class PerformanceTest {

            @Test
            @DisplayName("Обработка большого количества задач со всеми полями")
            void testShouldHandleLargeNumberOfTasksWithAllFields() throws IOException {
                // Given
                LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 0, 0);

                for (int i = 0; i < 50; i++) {
                    LocalDateTime startTime = baseTime.plusHours(i * 2);
                    Duration duration = Duration.ofMinutes(30);

                    Task task = new Task(manager.generateId(), "Task " + i,
                            "Description " + i, StatusTask.NEW, duration, startTime);
                    manager.createTask(task);
                }

                // When
                FileBackedTasksManager loadedManager = FileBackedTasksManager.loadFromFile(testFile);

                // Then
                assertEquals(50, loadedManager.getAllTasks().size());

                for (int i = 0; i < 50; i++) {
                    int taskId = i + 1;
                    Optional<Task> task = loadedManager.getTaskById(taskId);
                    assertTrue(task.isPresent(), "Задача " + taskId + " должна существовать");
                    assertEquals("Task " + i, task.get().getName());
                    assertEquals("Description " + i, task.get().getDescription());
                    assertNotNull(task.get().getStartTime());
                    assertNotNull(task.get().getDuration());
                }
            }
        }
    }
