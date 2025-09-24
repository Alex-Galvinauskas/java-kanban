package java.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.app.entity.Epic;
import java.app.entity.SubTask;
import java.app.management.TaskManager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Обработчик HTTP запросов для работы с эпиками.
 * Обрабатывает операции GET, POST, DELETE для endpoints /epics, /epics/{id} и /epics/{id}/subtasks.
 * Наследует общую функциональность от BaseHttpHandler.
 */
public class EpicsHandler extends BaseHttpHandler {

    /**
     * Создает новый обработчик эпиков.
     *
     * @param taskManager менеджер задач для выполнения операций с эпиками
     * @param gson экземпляр Gson для сериализации и десериализации JSON
     */
    public EpicsHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    /**
     * Обрабатывает входящий HTTP запрос.
     * Определяет метод запроса и делегирует обработку соответствующему методу.
     *
     * @param exchange HTTP обмен для обработки запроса и отправки ответа
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.matches("/epics/\\d+/subtasks") && "GET".equals(method)) {
                handleGetEpicSubtasks(exchange);
                return;
            }

            switch (method) {
                case "GET":
                    handleGet(exchange);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendBadRequest(exchange, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendInternalError(exchange, "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает GET запросы для эпиков.
     * Поддерживает три варианта:
     * - GET /epics - возвращает все эпики
     * - GET /epics/{id} - возвращает эпик по указанному ID
     * - GET /epics/{id}/subtasks - возвращает список подзадач указанного эпика
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            sendSuccess(exchange, taskManager.getAllEpics());
        } else {
            try {
                int id = Integer.parseInt(idParam);
                Optional<Epic> epic = taskManager.getEpicById(id);

                if (epic.isPresent()) {
                    sendSuccess(exchange, epic.get());
                } else {
                    sendNotFound(exchange, "Эпик с ID " + id + " не найден");
                }
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID эпика");
            }
        }
    }

    /**
     * Обрабатывает GET запросы для получения подзадач конкретного эпика.
     * Endpoint: GET /epics/{id}/subtasks
     * Возвращает список всех подзадач, принадлежащих указанному эпику.
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleGetEpicSubtasks(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            if (pathParts.length < 4) {
                sendBadRequest(exchange, "Неверный формат URL. Ожидается: /epics/{id}/subtasks");
                return;
            }

            int epicId = Integer.parseInt(pathParts[2]);
            Optional<Epic> epic = taskManager.getEpicById(epicId);

            if (epic.isEmpty()) {
                sendNotFound(exchange, "Эпик с ID " + epicId + " не найден");
                return;
            }

            List<SubTask> subtasks = taskManager.getEpicSubtasks(epicId);
            sendSuccess(exchange, subtasks);

        } catch (NumberFormatException e) {
            sendBadRequest(exchange, "Неверный формат ID эпика");
        } catch (Exception e) {
            sendInternalError(exchange, "Ошибка при получении подзадач эпика: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает POST запросы для эпиков.
     * Поддерживает создание нового эпика через POST /epics
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     * @throws IllegalArgumentException если переданные данные эпика невалидны
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        if (requestBody == null || requestBody.trim().isEmpty()) {
            sendBadRequest(exchange, "Тело запроса не может быть пустым");
            return;
        }
        try {
            Epic epic = gson.fromJson(requestBody, Epic.class);

            if (epic.getName() == null || epic.getName().trim().isEmpty()) {
                sendBadRequest(exchange, "Поле 'name' является обязательным");
                return;
            }
            epic.setId(0);
            String idParam = getPathParameter(exchange, 1);

            if (idParam == null) {
                int newId = taskManager.createEpic(epic);
                epic.setId(newId);
                sendCreated(exchange, epic);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            sendBadRequest(exchange, "Невалидный JSON: " + e.getMessage());
        } catch (RuntimeException e) {
            sendBadRequest(exchange, e.getMessage());
        } catch (Exception e) {
            sendInternalError(exchange, "Ошибка при обработке запроса: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для эпиков.
     * Поддерживает два варианта:
     * - DELETE /epics - удаляет все эпики
     * - DELETE /epics/{id} - удаляет эпик по указанному ID
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            taskManager.deleteAllEpics();
            sendNoContent(exchange);
        } else {
            try {
                int id = Integer.parseInt(idParam);
                taskManager.deleteEpicById(id);
                sendNoContent(exchange);
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID эпика");
            } catch (IllegalArgumentException e) {
                sendNotFound(exchange, e.getMessage());
            }
        }
    }
}