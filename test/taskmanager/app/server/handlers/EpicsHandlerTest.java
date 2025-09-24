package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты для обработчика эпиков")
class EpicsHandlerTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;
    private TestInfo testInfo;
    private int port;

    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(8181);
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
    @DisplayName("Создание эпика")
    void testCreateEpic() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Test Epic", "Test Description");
        String epicJson = gson.toJson(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(201);

        List<Epic> epics = manager.getAllEpics();
        assertThat(epics).hasSize(1);

        Epic createdEpic = epics.getFirst();
        assertThat(createdEpic.getName()).isEqualTo("Test Epic");
        assertThat(createdEpic.getDescription()).isEqualTo("Test Description");
        assertThat(createdEpic.getStatus()).isEqualTo(StatusTask.NEW);
    }

    @Test
    @DisplayName("Получение эпика по ID")
    void testGetEpicById() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Epic with Subtasks", "Description");
        int epicId = manager.createEpic(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Epic responseEpic = gson.fromJson(response.body(), Epic.class);
        assertThat(responseEpic).isNotNull();
        assertThat(responseEpic.getId()).isEqualTo(epicId);
        assertThat(responseEpic.getName()).isEqualTo("Epic with Subtasks");
        assertThat(responseEpic.getDescription()).isEqualTo("Description");
        assertThat(responseEpic.getSubTaskIds()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Получение эпика с подзадачами")
    void testGetEpicWithSubtasks() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Epic with Subtasks", "Description");
        int epicId = manager.createEpic(epic);

        SubTask subTask1 = new SubTask(manager.generateId(), "SubTask 1", "Description 1",
                StatusTask.NEW, Duration.ofMinutes(30),
                LocalDateTime.now(), epicId);
        SubTask subTask2 = new SubTask(manager.generateId(), "SubTask 2", "Description 2",
                StatusTask.IN_PROGRESS, Duration.ofMinutes(45),
                LocalDateTime.now().plusHours(1), epicId);

        manager.createSubTask(subTask1);
        manager.createSubTask(subTask2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Epic responseEpic = gson.fromJson(response.body(), Epic.class);
        assertThat(responseEpic).isNotNull();
        assertThat(responseEpic.getId()).isEqualTo(epicId);
        assertThat(responseEpic.getName()).isEqualTo("Epic with Subtasks");
        assertThat(responseEpic.getSubTaskIds()).hasSize(2);
        assertThat(responseEpic.getStatus()).isEqualTo(StatusTask.IN_PROGRESS);
    }

    @Test
    @DisplayName("Получение всех эпиков")
    void testGetAllEpics() throws IOException, InterruptedException {
        // Given
        manager.createEpic(new Epic("Epic 1", "Description 1"));
        manager.createEpic(new Epic("Epic 2", "Description 2"));
        manager.createEpic(new Epic("Epic 3", "Description 3"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(200);

        Epic[] epics = gson.fromJson(response.body(), Epic[].class);
        assertThat(epics).hasSize(3);
        assertThat(epics).extracting(Epic::getName)
                .containsExactly("Epic 1", "Epic 2", "Epic 3");
    }

    @Test
    @DisplayName("Удаление эпика по ID")
    void testDeleteEpic() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Epic to delete", "Description");
        int epicId = manager.createEpic(epic);
        assertThat(manager.getAllEpics()).hasSize(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllEpics()).isEmpty();
        assertThat(manager.getEpicById(epicId)).isEmpty();
    }

    @Test
    @DisplayName("Удаление эпика с подзадачами")
    void testDeleteEpicWithSubtasks() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Epic with subtasks", "Description");
        int epicId = manager.createEpic(epic);

        SubTask subTask = new SubTask(manager.generateId(), "SubTask", "Description",
                StatusTask.NEW, Duration.ofMinutes(30),
                LocalDateTime.now(), epicId);
        manager.createSubTask(subTask);

        assertThat(manager.getAllEpics()).hasSize(1);
        assertThat(manager.getAllSubTasks()).hasSize(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllEpics()).isEmpty();
        assertThat(manager.getAllSubTasks()).isEmpty();
    }

    @Test
    @DisplayName("Удаление всех эпиков")
    void testDeleteAllEpics() throws IOException, InterruptedException {
        // Given
        manager.createEpic(new Epic("Epic 1", "Description 1"));
        manager.createEpic(new Epic("Epic 2", "Description 2"));

        int epicId = manager.createEpic(new Epic("Epic with subtasks", "Description"));
        SubTask subTask = new SubTask(manager.generateId(), "SubTask", "Description",
                StatusTask.NEW, Duration.ofMinutes(30),
                LocalDateTime.now(), epicId);
        manager.createSubTask(subTask);

        assertThat(manager.getAllEpics()).hasSize(3);
        assertThat(manager.getAllSubTasks()).hasSize(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllEpics()).isEmpty();
        assertThat(manager.getAllSubTasks()).isEmpty();
    }

    @Test
    @DisplayName("Получение несуществующего эпика по ID")
    void testGetEpicByIdNotFound() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/999")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("не найден");
    }

    @Test
    @DisplayName("Создание эпика с невалидным JSON")
    void testCreateEpicWithInvalidJson() throws IOException, InterruptedException {
        // Given
        String invalidJson = "{ invalid json }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Создание эпика с невалидными данными")
    void testCreateEpicWithInvalidData() throws IOException, InterruptedException {
        // Given
        String invalidEpicJson = """
            {
                "description": "Description without name"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidEpicJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Удаление несуществующего эпика")
    void testDeleteNonExistentEpic() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/999")))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Получение эпика с невалидным ID")
    void testGetEpicWithInvalidId() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/invalid")))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Неподдерживаемый HTTP метод для эпиков")
    void testUnsupportedMethod() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
    }
}