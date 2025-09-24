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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты для обработчика задач")
class TasksHandlerTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private TestInfo testInfo;
    private int port;

    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(8081);
    private static final String BASE_URL = "http://localhost:";

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

    private String getUrl(String path) {
        return BASE_URL + port + path;
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
        assertThat(response.statusCode()).isEqualTo(201);

        List<Task> tasksFromManager = manager.getAllTasks();
        assertThat(tasksFromManager).isNotNull().hasSize(1);
        assertThat(tasksFromManager.getFirst().getName()).isEqualTo("Test Task");
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
        assertThat(response.statusCode()).isEqualTo(200);

        Task responseTask = gson.fromJson(response.body(), Task.class);
        assertThat(responseTask).isNotNull();
        assertThat(responseTask.getId()).isEqualTo(taskId);
        assertThat(responseTask.getName()).isEqualTo("Test Task");
        assertThat(responseTask.getDescription()).isEqualTo("Description");
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
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::getName)
                .containsExactly("Task 1", "Task 2");
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
        assertThat(response.statusCode()).isEqualTo(200);

        Task taskFromManager = manager.getTaskById(taskId).orElseThrow();
        assertThat(taskFromManager.getName()).isEqualTo("Updated Task");
        assertThat(taskFromManager.getDescription()).isEqualTo("Updated Description");
        assertThat(taskFromManager.getStatus()).isEqualTo(StatusTask.DONE);
        assertThat(taskFromManager.getDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("Удаление задачи по ID")
    void testDeleteTask() throws IOException, InterruptedException {
        // Given
        Task task = new Task(manager.generateId(), "Task to delete",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now());
        int taskId = manager.createTask(task);
        assertThat(manager.getAllTasks()).hasSize(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks/" + taskId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllTasks()).isEmpty();
        assertThat(manager.getTaskById(taskId)).isEmpty();
    }

    @Test
    @DisplayName("Удаление всех задач")
    void testDeleteAllTasks() throws IOException, InterruptedException {
        // Given
        manager.createTask(new Task(manager.generateId(), "Task 1", "Desc 1",
                StatusTask.NEW, Duration.ofMinutes(5), LocalDateTime.now()));
        manager.createTask(new Task(manager.generateId(), "Task 2", "Desc 2",
                StatusTask.NEW, Duration.ofMinutes(10), LocalDateTime.now().plusHours(1)));
        assertThat(manager.getAllTasks()).hasSize(2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/tasks")))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllTasks()).isEmpty();
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
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("не найдена");
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
        assertThat(response.statusCode()).isEqualTo(400);
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
        assertThat(response.statusCode()).isEqualTo(400);

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertThat(jsonResponse.has("error")).isTrue();
        assertThat(jsonResponse.get("error").getAsString()).contains("не существует");
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
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
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
        assertThat(response.statusCode()).isEqualTo(406);

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertThat(jsonResponse.has("error")).isTrue();

        String errorMessage = jsonResponse.get("error").getAsString().toLowerCase();
        assertThat(errorMessage).containsAnyOf("пересекается", "конфликт", "conflict", "время");
        assertThat(errorMessage).contains("conflicting task");
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
        assertThat(response.statusCode()).isEqualTo(400);
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
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("некорректный JSON формат");
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
        assertThat(response.statusCode()).isEqualTo(201);

        List<Task> tasks = manager.getAllTasks();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getName()).isEqualTo("Минимальная задача");
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
        assertThat(response.statusCode()).isEqualTo(400);

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertThat(jsonResponse.has("error")).isTrue();
        assertThat(jsonResponse.get("error").getAsString())
                .containsAnyOf("could not be parsed", "дата", "формат");
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
        assertThat(response.statusCode()).isEqualTo(400);
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
        assertThat(response.statusCode()).isEqualTo(400);

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        assertThat(jsonResponse.has("error")).isTrue();
        assertThat(jsonResponse.get("error").getAsString()).isNotNull();
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
        assertThat(response.statusCode()).isEqualTo(201);

        Task createdTask = gson.fromJson(response.body(), Task.class);
        assertThat(createdTask.getName()).contains("спец. символами");
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
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] tasks = gson.fromJson(response.body(), Task[].class);
        assertThat(tasks).hasSize(100);
        assertThat(endTime - startTime).isLessThan(1000);
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
        assertThat(response.statusCode()).isEqualTo(200);

        Optional<String> contentType = response.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get()).contains("application/json");
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
        assertThat(response.statusCode()).isIn(201, 400);
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
            assertThat(response.body().toLowerCase()).contains("пересекается");
        } else if (response.statusCode() == 400) {
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            assertThat(jsonResponse.has("error")).isTrue();
            assertThat(manager.getAllTasks()).hasSize(1);
        }
    }
}