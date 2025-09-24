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

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(201, response.statusCode(), "Неверный статус код при создании");
        List<Epic> epics = manager.getAllEpics();
        assertEquals(1, epics.size(), "Некорректное количество эпиков");
        assertEquals("Test Epic", epics.getFirst().getName(), "Некорректное имя эпика");
        assertEquals("Test Description", epics.getFirst().getDescription(),
                "Некорректное описание эпика");
        assertEquals(StatusTask.NEW, epics.getFirst().getStatus(),
                "Некорректный статус эпика");
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
        assertNotNull(responseEpic, "Эпик не должен быть null");
        assertEquals(epicId, responseEpic.getId(), "Некорректный ID эпика");
        assertEquals("Epic with Subtasks", responseEpic.getName(), "Некорректное имя эпика");
        assertEquals("Description", responseEpic.getDescription(),
                "Некорректное описание эпика");
        assertNotNull(responseEpic.getSubTaskIds(), "Список подзадач не должен быть null");
        assertTrue(responseEpic.getSubTaskIds().isEmpty(), "Список подзадач должен быть пустым");
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
        assertEquals(2, responseEpic.getSubTaskIds().size(), "Должно быть 2 подзадачи");
        assertEquals(StatusTask.IN_PROGRESS, responseEpic.getStatus(),
                "Статус эпика должен обновиться");
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
        assertEquals(3, epics.length, "Должно быть 3 эпика");
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
        assertEquals(0, manager.getAllEpics().size());
        assertTrue(manager.getEpicById(epicId).isEmpty(), "Эпик должен быть удален");
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
        assertEquals(0, manager.getAllEpics().size());
        assertEquals(0, manager.getAllSubTasks().size(),
                "Подзадачи должны быть удалены вместе с эпиком");
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
        assertEquals(0, manager.getAllEpics().size());
        assertEquals(0, manager.getAllSubTasks().size(),
                "Все подзадачи должны быть удалены");
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
        assertTrue(response.body().contains("не найден"),
                "Сообщение об ошибке должно содержать описание");
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
        assertEquals(400, response.statusCode(), "Должна быть ошибка валидации");
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
        assertEquals(400, response.statusCode(), "Должна быть ошибка валидации ID");
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
        assertEquals(400, response.statusCode(),
                "Должна быть ошибка неподдерживаемого метода");
    }
}