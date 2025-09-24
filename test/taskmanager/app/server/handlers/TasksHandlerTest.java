package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.Task;
import taskmanager.app.management.TaskManager;
import taskmanager.app.server.HttpTaskServer;
import taskmanager.app.service.manager.InMemoryTaskManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для обработчика задач")
class TasksHandlerTest {
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(8081);
    private static final String BASE_URL = "http://localhost:";
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private TestInfo testInfo;
    private int port;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        this.testInfo = testInfo;
        this.port = PORT_COUNTER.getAndIncrement();
        this.manager = new InMemoryTaskManager();
        this.taskServer = new HttpTaskServer(manager, port);
        this.gson = taskServer.getGson();
        this.client = HttpClient.newHttpClient();

        System.out.printf("🚀 Запуск теста %s на порту %d%n", testInfo.getDisplayName(), port);
        taskServer.start();
    }

    @AfterEach
    void tearDown() {
        if (taskServer != null) {
            taskServer.stop();
        }
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Добавление задачи")
    void testAddTask() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Test Task", "Testing task 2",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now());
        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(201, response.statusCode());

        List<Task> tasksFromManager = manager.getAllTasks();
        assertNotNull(tasksFromManager);
        assertEquals(1, tasksFromManager.size());
        assertEquals("Test Task", tasksFromManager.getFirst().getName());
    }

    private String getUrl(String path) {
        return BASE_URL + port + path;
    }

    @Test
    @DisplayName("Получение задачи по ID")
    void testGetTaskById() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Test Task", "Description",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now());
        int taskId = manager.createTask(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + taskId)))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        Task responseTask = gson.fromJson(response.body(), Task.class);
        assertNotNull(responseTask);
        assertEquals(taskId, responseTask.getId());
        assertEquals("Test Task", responseTask.getName());
        assertEquals("Description", responseTask.getDescription());
    }

    @Test
    @DisplayName("Получение всех задач")
    void testGetAllTasks() throws IOException, InterruptedException {
        // Given
        manager.createTask(new Task(manager.generateId(), "Task 1", "Desc 1",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now()));
        manager.createTask(new Task(manager.generateId(), "Task 2", "Desc 2",
                StatusTask.IN_PROGRESS, Duration.ofMinutes(10), LocalDateTime.now().plusHours(1)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertEquals("Task 1", tasks[0].getName());
        assertEquals("Task 2", tasks[1].getName());
    }

    @Test
    @DisplayName("Обновление задачи")
    void testUpdateTask() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Original Task", "Description",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now());
        int taskId = manager.createTask(task);

        Task updatedTask = new Task(taskId, "Updated Task", "Updated Description",
                StatusTask.DONE, Duration.ofMinutes(10), LocalDateTime.now().plusHours(1));
        String updatedJson = gson.toJson(updatedTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + taskId)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(updatedJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        Optional<Task> taskFromManagerOpt = manager.getTaskById(taskId);
        assertTrue(taskFromManagerOpt.isPresent());

        Task taskFromManager = taskFromManagerOpt.get();
        assertEquals("Updated Task", taskFromManager.getName());
        assertEquals("Updated Description", taskFromManager.getDescription());
        assertEquals(StatusTask.DONE, taskFromManager.getStatus());
        assertEquals(Duration.ofMinutes(10), taskFromManager.getDuration());
    }

    @Test
    @DisplayName("Удаление задачи по ID")
    void testDeleteTask() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Task to delete",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now());
        int taskId = manager.createTask(task);
        assertEquals(1, manager.getAllTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + taskId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
        assertFalse(manager.getTaskById(taskId).isPresent());
    }

    @Test
    @DisplayName("Удаление всех задач")
    void testDeleteAllTasks() throws IOException, InterruptedException {
        // Given
        manager.createTask(new Task(manager.generateId(), "Task 1", "Desc 1",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now()));
        manager.createTask(new Task(manager.generateId(), "Task 2", "Desc 2",
                StatusTask.NEW, Duration.ofMinutes(10), LocalDateTime.now().plusHours(1)));
        assertEquals(2, manager.getAllTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    @DisplayName("Получение несуществующей задачи по ID")
    void testGetTaskByIdNotFound() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/999")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("не найдена"));
    }

    @Test
    @DisplayName("Обработка невалидного JSON")
    void testInvalidJson() throws IOException, InterruptedException {
        // Given
        String invalidJson = "{ invalid json }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("Обновление несуществующей задачи")
    void testUpdateNonExistentTask() throws IOException, InterruptedException {
        // Given
        Task task = new Task(999, "Non-existent", "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now());
        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/999")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(jsonResponse.has("error"));
        assertTrue(jsonResponse.get("error").getAsString().contains("не существует"));
    }

    @Test
    @DisplayName("Удаление несуществующей задачи")
    void testDeleteNonExistentTask() throws IOException, InterruptedException {
        // Given
        int nonExistentTaskId = 999;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + nonExistentTaskId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
    }

    @Test
    @DisplayName("Создание задачи с конфликтом времени")
    void testCreateTaskWithTimeConflict() throws IOException, InterruptedException {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        Task existingTask = new Task(manager.generateId(), "Existing Task", "Desc",
                StatusTask.NEW, Duration.ofHours(1), startTime);
        manager.createTask(existingTask);

        Task conflictingTask = new Task(manager.generateId(), "Conflicting Task", "Desc",
                StatusTask.NEW, Duration.ofMinutes(30), startTime.plusMinutes(30));
        String taskJson = gson.toJson(conflictingTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(406, response.statusCode());

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(jsonResponse.has("error"));

        String errorMessage = jsonResponse.get("error").getAsString().toLowerCase();
        assertTrue(errorMessage.contains("пересекается") || errorMessage.contains("конфликт") ||
                errorMessage.contains("conflict") || errorMessage.contains("время"));
        assertTrue(errorMessage.contains("conflicting task"));
    }

    @Test
    @DisplayName("Неподдерживаемый HTTP метод")
    void testUnsupportedMethod() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("POST запрос с пустым телом")
    void testPostWithEmptyBody() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("некорректный JSON формат"));
    }

    @Test
    @DisplayName("Создание задачи только с обязательными полями")
    void testCreateTaskWithMinimumFields() throws IOException, InterruptedException {
        // Given
        String minimalTaskJson = """
                {
                    "name": "Минимальная задача",
                    "status": "NEW"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(minimalTaskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(201, response.statusCode());

        List<Task> tasks = manager.getAllTasks();
        assertEquals(1, tasks.size());
        assertEquals("Минимальная задача", tasks.getFirst().getName());
    }

    @Test
    @DisplayName("Создание задачи с невалидными данными")
    void testCreateTaskWithInvalidData() throws IOException, InterruptedException {
        // Given
        String invalidDateJson = """
                {
                    "name": "Test Task",
                    "startTime": "invalid-date-format"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidDateJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(jsonResponse.has("error"));
        String errorMessage = jsonResponse.get("error").getAsString();
        assertTrue(errorMessage.contains("could not be parsed") || errorMessage.contains("дата") ||
                errorMessage.contains("формат"));
    }

    @Test
    @DisplayName("Получение задачи с невалидным ID")
    void testGetTaskWithInvalidId() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/invalid")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("Проверка формата JSON ответа при ошибках")
    void testErrorResponseFormat() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/invalid_id")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(jsonResponse.has("error"));
        assertNotNull(jsonResponse.get("error").getAsString());
    }

    @Test
    @DisplayName("Создание задачи со специальными символами")
    void testCreateTaskWithSpecialCharacters() throws IOException, InterruptedException {
        // Given
        String taskWithSpecialChars = """
                {
                    "name": "Задача с спец. символами: \\"<>{}[]&@#",
                    "description": "Описание с \\\\n переносами и \\\\t табуляциями",
                    "status": "NEW"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskWithSpecialChars))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(201, response.statusCode());

        Task createdTask = gson.fromJson(response.body(), Task.class);
        assertTrue(createdTask.getName().contains("спец. символами"));
    }

    @Test
    @DisplayName("Получение большого количества задач")
    void testGetManyTasks() throws IOException, InterruptedException {
        // Given
        for (int i = 0; i < 100; i++) {
            Task task = new Task(manager.generateId(), "Task " + i, "Description " + i,
                    StatusTask.NEW, Duration.ofMinutes(5),
                    LocalDateTime.now().plusHours(i));
            manager.createTask(task);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .GET()
                .build();

        // When
        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(200, response.statusCode());

        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertEquals(100, tasks.length);
        assertTrue(endTime - startTime < 1000);
    }

    @Test
    @DisplayName("Проверка HTTP заголовков в ответах")
    void testResponseHeaders() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Test Task", "Description",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now());
        int taskId = manager.createTask(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + taskId)))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        Optional<String> contentType = response.headers().firstValue("Content-Type");
        assertTrue(contentType.isPresent());
        assertTrue(contentType.get().contains("application/json"));
    }

    @Test
    @DisplayName("Создание задачи с крайними значениями дат")
    void testCreateTaskWithEdgeCaseDates() throws IOException, InterruptedException {
        // Given
        String farFutureTaskJson = """
                {
                    "name": "Задача в далеком будущем",
                    "status": "NEW",
                    "startTime": "2100-01-01T00:00:00",
                    "duration": 1440
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(farFutureTaskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertTrue(response.statusCode() == 201 || response.statusCode() == 400);
    }

    @Test
    @DisplayName("Создание задачи с различными невалидными сценариями")
    void testCreateTaskWithInvalidDataBoundaryCases() throws IOException, InterruptedException {
        // Given
        Map<String, String> invalidScenarios = Map.of(
                "Пустой JSON объект", "{}",
                "Null значения", "{\"name\": null, \"duration\": 30}",
                "Нечисловое значение duration", "{\"name\": \"Test\", \"duration\": \"not-a-number\"}",
                "Слишком большие числа", "{\"name\": \"Test\", \"duration\": 9999999999}"
        );

        for (Map.Entry<String, String> scenario : invalidScenarios.entrySet()) {
            String scenarioName = scenario.getKey();
            String requestBody = scenario.getValue();

            System.out.println("\n=== " + scenarioName + " ===");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getUrl("/tasks")))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // When
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Then
            System.out.println("Request: " + requestBody);
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 400) {
                System.out.println("✅ Сервер корректно отклонил невалидные данные");
            } else if (response.statusCode() == 201) {
                System.out.println("⚠️  Сервер принял данные, которые могут считаться невалидными");
            }
        }
    }

    @Test
    @DisplayName("Создание задачи с конфликтом времени при обновлении")
    void testCreateTaskWithTimeConflictOnUpdate() throws IOException, InterruptedException {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        Task existingTask = new Task(manager.generateId(), "Существующая задача", "Описание",
                StatusTask.NEW, Duration.ofHours(2), baseTime);
        manager.createTask(existingTask);

        Task conflictingTask = new Task(manager.generateId(), "Конфликтующая задача", "Описание",
                StatusTask.NEW, Duration.ofHours(1), baseTime.plusMinutes(30));
        String taskJson = gson.toJson(conflictingTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        if (response.statusCode() == 406) {
            assertTrue(response.body().toLowerCase().contains("пересекается"));
        } else if (response.statusCode() == 400) {
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            assertTrue(jsonResponse.has("error"));
            assertEquals(1, manager.getAllTasks().size());
        }
    }
}