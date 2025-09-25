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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для обработчика подзадач")
class SubtasksHandlerTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private int epicId;
    private int subTaskId;
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        this.testInfo = testInfo;
        manager = new InMemoryTaskManager();
        taskServer = new HttpTaskServer(manager);
        gson = taskServer.getGson();

        // Given
        Epic epic = new Epic("Test Epic", "For subtasks testing");
        epicId = manager.createEpic(epic);

        SubTask subTask = new SubTask(manager.generateId(), "Existing SubTask",
                "Existing description", StatusTask.NEW,
                Duration.ofMinutes(10),
                LocalDateTime.now().plusMinutes(30), epicId);
        subTaskId = manager.createSubTask(subTask);

        System.out.printf("🚀 Запуск теста: %s%n", testInfo.getDisplayName());
        taskServer.start();
    }

    @AfterEach
    void tearDown() {
        taskServer.stop();
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Создание подзадачи")
    void testCreateSubTask() throws IOException, InterruptedException {
        // Given
        SubTask subTask = new SubTask(manager.generateId(), "Test SubTask",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now(), epicId);
        String subTaskJson = gson.toJson(subTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(201, response.statusCode());

        List<SubTask> subTasks = manager.getAllSubTasks();
        assertEquals(2, subTasks.size());
        assertEquals("Test SubTask", subTasks.get(1).getName());
        assertEquals(epicId, subTasks.get(1).getEpicId());
    }

    @Test
    @DisplayName("Получение всех подзадач")
    void testGetAllSubTasks() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        List<?> subTasks = gson.fromJson(response.body(), List.class);
        assertEquals(1, subTasks.size());
    }

    @Test
    @DisplayName("Получение подзадачи по ID")
    void testGetSubTaskById() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + subTaskId))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        SubTask subTask = gson.fromJson(response.body(), SubTask.class);
        assertEquals("Existing SubTask", subTask.getName());
        assertEquals(subTaskId, subTask.getId());
    }

    @Test
    @DisplayName("Получение несуществующей подзадачи")
    void testGetNonExistentSubTask() throws IOException, InterruptedException {
        // Given
        int nonExistentId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + nonExistentId))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("не найдена"));
    }

    @Test
    @DisplayName("Обновление подзадачи")
    void testUpdateSubTask() throws IOException, InterruptedException {
        // Given
        SubTask updatedSubTask = new SubTask(subTaskId, "Updated SubTask",
                "Updated description", StatusTask.IN_PROGRESS,
                Duration.ofMinutes(15), LocalDateTime.now().plusHours(1), epicId);
        String updatedJson = gson.toJson(updatedSubTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + subTaskId))
                .POST(HttpRequest.BodyPublishers.ofString(updatedJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        SubTask subTask = manager.getSubTaskById(subTaskId);
        assertNotNull(subTask, "Подзадача должна существовать");
        assertEquals("Updated SubTask", subTask.getName());
        assertEquals(StatusTask.IN_PROGRESS, subTask.getStatus());
    }

    @Test
    @DisplayName("Удаление подзадачи по ID")
    void testDeleteSubTaskById() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + subTaskId))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertThrows(NotFoundException.class, () -> manager.getSubTaskById(subTaskId));
        assertTrue(manager.getAllSubTasks().isEmpty());
    }

    @Test
    @DisplayName("Удаление всех подзадач")
    void testDeleteAllSubTasks() throws IOException, InterruptedException {
        // Given
        SubTask anotherSubTask = new SubTask(manager.generateId(), "Another SubTask",
                "Another description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now().plusHours(2), epicId);
        manager.createSubTask(anotherSubTask);

        assertEquals(2, manager.getAllSubTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertTrue(manager.getAllSubTasks().isEmpty());
    }

    @Test
    @DisplayName("Удаление несуществующей подзадачи")
    void testDeleteNonExistentSubTask() throws IOException, InterruptedException {
        // Given
        int nonExistentId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + nonExistentId))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(204, response.statusCode());
        assertEquals(1, manager.getAllSubTasks().size());
    }

    @Test
    @DisplayName("Создание подзадачи с неверным форматом JSON")
    void testCreateSubTaskWithInvalidJson() throws IOException, InterruptedException {
        // Given
        String invalidJson = "{ invalid json }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Неверный формат JSON"));
    }

    @Test
    @DisplayName("Создание подзадачи с пересечением времени")
    void testCreateSubTaskWithTimeOverlap() throws IOException, InterruptedException {
        // Given
        LocalDateTime sameTime = LocalDateTime.now().plusMinutes(30);
        SubTask overlappingSubTask = new SubTask(manager.generateId(), "Overlapping SubTask",
                "Overlapping description", StatusTask.NEW,
                Duration.ofMinutes(10), sameTime, epicId);
        String overlappingJson = gson.toJson(overlappingSubTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(overlappingJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(406, response.statusCode());
        assertTrue(response.body().contains("пересекается"));
    }

    @Test
    @DisplayName("Обновление подзадачи с неверным ID в пути")
    void testUpdateSubTaskWithMismatchedId() throws IOException, InterruptedException {
        // Given
        SubTask subTask = new SubTask(subTaskId, "Test SubTask",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now().plusHours(3), epicId);
        String subTaskJson = gson.toJson(subTask);

        int wrongId = subTaskId + 1;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + wrongId))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("Запрос с неподдерживаемым методом")
    void testUnsupportedMethod() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Метод не поддерживается"));
    }

    @Test
    @DisplayName("Запрос с неверным форматом ID")
    void testInvalidIdFormat() throws IOException, InterruptedException {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/invalid"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Неверный формат ID подзадачи"));
    }

    @Test
    @DisplayName("Создание подзадачи с несуществующим эпиком")
    void testCreateSubTaskWithNonExistentEpic() throws IOException, InterruptedException {
        // Given
        int nonExistentEpicId = 9999;
        SubTask subTask = new SubTask(manager.generateId(), "Test SubTask",
                "Description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now(), nonExistentEpicId);
        String subTaskJson = gson.toJson(subTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Эпик с id " + nonExistentEpicId + " не существует"));
    }

    @Test
    @DisplayName("Обновление подзадачи с изменением эпика")
    void testUpdateSubTaskChangeEpic() throws IOException, InterruptedException {
        // Given
        Epic newEpic = new Epic("New Epic", "For moving subtask");
        int newEpicId = manager.createEpic(newEpic);

        SubTask updatedSubTask = new SubTask(subTaskId, "Moved SubTask",
                "Moved description", StatusTask.DONE,
                Duration.ofMinutes(20),
                LocalDateTime.now().plusHours(2), newEpicId);
        String updatedJson = gson.toJson(updatedSubTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + subTaskId))
                .POST(HttpRequest.BodyPublishers.ofString(updatedJson))
                .build();

        // When
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        SubTask subTask = manager.getSubTaskById(subTaskId);
        assertNotNull(subTask, "Подзадача должна существовать");
        assertEquals(newEpicId, subTask.getEpicId());
        assertEquals(StatusTask.DONE, subTask.getStatus());
    }
}