package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.exception.NotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для обработчика эпиков")
class EpicsHandlerTest {
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(8181);
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
        assertEquals(201, response.statusCode());

        List<Epic> epics = manager.getAllEpics();
        assertEquals(1, epics.size());

        Epic createdEpic = epics.get(0);
        assertEquals("Test Epic", createdEpic.getName());
        assertEquals("Test Description", createdEpic.getDescription());
        assertEquals(StatusTask.NEW, createdEpic.getStatus());
    }

    private String getUrl(String path) {
        return BASE_URL + port + path;
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
        assertEquals(200, response.statusCode());

        Epic responseEpic = gson.fromJson(response.body(), Epic.class);
        assertNotNull(responseEpic);
        assertEquals(epicId, responseEpic.getId());
        assertEquals("Epic with Subtasks", responseEpic.getName());
        assertEquals("Description", responseEpic.getDescription());
        assertNotNull(responseEpic.getSubTaskIds());
        assertTrue(responseEpic.getSubTaskIds().isEmpty());
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
        assertEquals(200, response.statusCode());

        Epic responseEpic = gson.fromJson(response.body(), Epic.class);
        assertNotNull(responseEpic);
        assertEquals(epicId, responseEpic.getId());
        assertEquals("Epic with Subtasks", responseEpic.getName());
        assertEquals(2, responseEpic.getSubTaskIds().size());
        assertEquals(StatusTask.IN_PROGRESS, responseEpic.getStatus());
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
        assertEquals(200, response.statusCode());

        Epic[] epics = gson.fromJson(response.body(), Epic[].class);
        assertEquals(3, epics.length);
        assertEquals("Epic 1", epics[0].getName());
        assertEquals("Epic 2", epics[1].getName());
        assertEquals("Epic 3", epics[2].getName());
    }

    @Test
    @DisplayName("Удаление эпика по ID")
    void testDeleteEpic() throws IOException, InterruptedException {
        // Given
        Epic epic = new Epic("Epic to delete", "Description");
        int epicId = manager.createEpic(epic);
        assertEquals(1, manager.getAllEpics().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllEpics().isEmpty());
        assertThrows(NotFoundException.class, () -> manager.getEpicById(epicId));
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

        assertEquals(1, manager.getAllEpics().size());
        assertEquals(1, manager.getAllSubTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics/" + epicId)))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubTasks().isEmpty());
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

        assertEquals(3, manager.getAllEpics().size());
        assertEquals(1, manager.getAllSubTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl("/epics")))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubTasks().isEmpty());
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
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("не найден"));
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
        assertEquals(400, response.statusCode());
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
        assertEquals(400, response.statusCode());
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
        assertEquals(404, response.statusCode());
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
        assertEquals(400, response.statusCode());
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
        assertEquals(400, response.statusCode());
    }
}