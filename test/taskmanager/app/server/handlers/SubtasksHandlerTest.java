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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты для обработчика подзадач")
class SubtasksHandlerTest {
    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private final HttpClient client = HttpClient.newHttpClient();
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
        assertThat(response.statusCode()).isEqualTo(201);

        List<SubTask> subTasks = manager.getAllSubTasks();
        assertThat(subTasks).hasSize(2);
        assertThat(subTasks.get(1).getName()).isEqualTo("Test SubTask");
        assertThat(subTasks.get(1).getEpicId()).isEqualTo(epicId);
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
        assertThat(response.statusCode()).isEqualTo(200);

        List<?> subTasks = gson.fromJson(response.body(), List.class);
        assertThat(subTasks).hasSize(1);
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
        assertThat(response.statusCode()).isEqualTo(200);

        SubTask subTask = gson.fromJson(response.body(), SubTask.class);
        assertThat(subTask.getName()).isEqualTo("Existing SubTask");
        assertThat(subTask.getId()).isEqualTo(subTaskId);
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
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("не найдена");
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
        assertThat(response.statusCode()).isEqualTo(200);

        SubTask subTask = manager.getSubTaskById(subTaskId).orElseThrow();
        assertThat(subTask.getName()).isEqualTo("Updated SubTask");
        assertThat(subTask.getStatus()).isEqualTo(StatusTask.IN_PROGRESS);
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
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getSubTaskById(subTaskId)).isEmpty();
        assertThat(manager.getAllSubTasks()).isEmpty();
    }

    @Test
    @DisplayName("Удаление всех подзадач")
    void testDeleteAllSubTasks() throws IOException, InterruptedException {
        // Given
        SubTask anotherSubTask = new SubTask(manager.generateId(), "Another SubTask",
                "Another description", StatusTask.NEW,
                Duration.ofMinutes(5), LocalDateTime.now().plusHours(2), epicId);
        manager.createSubTask(anotherSubTask);

        assertThat(manager.getAllSubTasks()).hasSize(2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .DELETE()
                .build();

        // When
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllSubTasks()).isEmpty();
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
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(manager.getAllSubTasks()).hasSize(1);
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
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Неверный формат JSON");
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
        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(response.body()).contains("пересекается");
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
        assertThat(response.statusCode()).isEqualTo(400);
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
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Метод не поддерживается");
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
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Неверный формат ID подзадачи");
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
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Эпик с id " + nonExistentEpicId + " не существует"); // Исправлено
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
        assertThat(response.statusCode()).isEqualTo(200);

        SubTask subTask = manager.getSubTaskById(subTaskId).orElseThrow();
        assertThat(subTask.getEpicId()).isEqualTo(newEpicId);
        assertThat(subTask.getStatus()).isEqualTo(StatusTask.DONE);
    }
}