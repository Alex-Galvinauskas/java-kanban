package taskmanager.app.server.handlers;

import com.google.gson.Gson;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты для обработчика истории и приоритетных задач")
class HistoryAndPrioritizedHandlerTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private final HttpClient client = HttpClient.newHttpClient();
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        this.testInfo = testInfo;
        manager = new InMemoryTaskManager();
        taskServer = new HttpTaskServer(manager);
        gson = taskServer.getGson();

        System.out.printf("🚀 Запуск теста: %s%n", testInfo.getDisplayName());
        taskServer.start();
    }

    @AfterEach
    void tearDown() {
        taskServer.stop();
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Получение истории задач")
    void testGetHistory() throws IOException, InterruptedException {
        // Given
        Task task1 = new Task(manager.generateId(), "Task 1",
                "Desc 1", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now());
        int taskId1 = manager.createTask(task1);
        manager.getTaskById(taskId1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertThat(history).hasSize(1);
        assertThat(history[0].getName()).isEqualTo("Task 1");
    }

    @Test
    @DisplayName("Получение приоритетных задач")
    void testGetPrioritizedTasks() throws IOException, InterruptedException {
        // Given
        Task task1 = new Task(manager.generateId(), "Task 1",
                "Desc 1", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now().plusHours(2));
        Task task2 = new Task(manager.generateId(), "Task 2",
                "Desc 2", StatusTask.NEW,
                Duration.ofMinutes(10), LocalDateTime.now().plusHours(1));

        manager.createTask(task1);
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] prioritized = gson.fromJson(response.body(), Task[].class);
        assertThat(prioritized).hasSize(2);
        assertThat(prioritized[0].getName()).isEqualTo("Task 2");
        assertThat(prioritized[1].getName()).isEqualTo("Task 1");
    }

    @Test
    @DisplayName("Неподдерживаемый метод для истории")
    void testUnsupportedMethod() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Получение пустой истории задач")
    void testGetEmptyHistory() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("История содержит максимальное количество задач")
    void testHistoryWithMaxTasks() throws IOException, InterruptedException {
        // Given
        for (int i = 1; i <= 5; i++) {
            Task task = new Task(manager.generateId(), "Task " + i,
                    "Desc " + i, StatusTask.NEW,
                    Duration.ofMinutes(5), LocalDateTime.now().plusHours(i));
            int taskId = manager.createTask(task);
            manager.getTaskById(taskId); // Добавляем в историю
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertThat(history).hasSize(5);
        assertThat(history).extracting(Task::getName)
                .containsExactly("Task 1", "Task 2", "Task 3", "Task 4", "Task 5");
    }

    @Test
    @DisplayName("Получение пустого списка приоритетных задач")
    void testGetEmptyPrioritizedTasks() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] prioritized = gson.fromJson(response.body(), Task[].class);
        assertThat(prioritized).isEmpty();
    }

    @Test
    @DisplayName("Неподдерживаемый метод для приоритетных задач")
    void testUnsupportedMethodForPrioritized() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Некорректный endpoint")
    void testInvalidEndpoint() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/invalid"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @DisplayName("Проверка полей задачи в истории")
    void testTaskFieldsInHistory() throws IOException, InterruptedException {
        // Given
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Duration duration = Duration.ofMinutes(30);
        Task task = new Task(manager.generateId(), "Test Task",
                "Test Description", StatusTask.IN_PROGRESS,
                duration, startTime);

        int taskId = manager.createTask(task);
        manager.getTaskById(taskId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertThat(history).hasSize(1);

        Task returnedTask = history[0];
        assertThat(returnedTask.getName()).isEqualTo("Test Task");
        assertThat(returnedTask.getDescription()).isEqualTo("Test Description");
        assertThat(returnedTask.getStatus()).isEqualTo(StatusTask.IN_PROGRESS);
        assertThat(returnedTask.getDuration()).isEqualTo(duration);
        assertThat(returnedTask.getStartTime()).isEqualTo(startTime);
    }

    @Test
    @DisplayName("Приоритетные задачи сортируются по времени начала")
    void testPrioritizedTasksSorting() throws IOException, InterruptedException {
        // Given
        Task earlyTask = new Task(manager.generateId(), "Early Task",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(15), LocalDateTime.now().plusMinutes(30));

        Task middleTask = new Task(manager.generateId(), "Middle Task",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(20), LocalDateTime.now().plusHours(1));

        Task lateTask = new Task(manager.generateId(), "Late Task",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(25), LocalDateTime.now().plusHours(2));

        manager.createTask(lateTask);
        manager.createTask(earlyTask);
        manager.createTask(middleTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] prioritized = gson.fromJson(response.body(), Task[].class);
        assertThat(prioritized).hasSize(3);
        assertThat(prioritized).extracting(Task::getName)
                .containsExactly("Early Task", "Middle Task", "Late Task");
    }

    @Test
    @DisplayName("Задачи без времени начала не попадают в приоритетный список")
    void testTasksWithoutStartTimeNotInPrioritized() throws IOException, InterruptedException {
        // Given
        Task taskWithTime = new Task(manager.generateId(), "Task with time",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(15), LocalDateTime.now().plusHours(1));

        Task taskWithoutTime = new Task(manager.generateId(), "Task without time",
                "Description", StatusTask.NEW);

        manager.createTask(taskWithTime);
        manager.createTask(taskWithoutTime);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Task[] prioritized = gson.fromJson(response.body(), Task[].class);
        assertThat(prioritized).hasSize(1);
        assertThat(prioritized[0].getName()).isEqualTo("Task with time");
    }
}