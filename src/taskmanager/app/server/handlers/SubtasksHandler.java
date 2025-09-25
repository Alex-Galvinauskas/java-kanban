package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import taskmanager.app.entity.SubTask;
import taskmanager.app.exception.NotFoundException;
import taskmanager.app.management.TaskManager;

import java.io.IOException;

/**
 * Обработчик HTTP запросов для работы с подзадачами.
 * Обрабатывает операции GET, POST, DELETE для endpoints /subtasks и /subtasks/{id}.
 * Наследует общую функциональность от BaseHttpHandler.
 *
 * <p><b>Важно:</b> Подзадачи всегда связаны с родительским эпиком.
 * При создании подзадачи требуется указать epicId.
 */
public class SubtasksHandler extends BaseHttpHandler {

    /**
     * Создает новый обработчик подзадач.
     *
     * @param taskManager менеджер задач для выполнения операций с подзадачами
     * @param gson экземпляр Gson для сериализации и десериализации JSON
     */
    public SubtasksHandler(TaskManager taskManager, Gson gson) {
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
            sendInternalError(exchange, "Внутренняя ошибка сервера " + e.getMessage());
        }
    }

    /**
     * Обрабатывает GET запросы для подзадач.
     * Поддерживает два варианта:
     * - GET /subtasks - возвращает все подзадачи
     * - GET /subtasks/{id} - возвращает подзадачу по указанному ID
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            sendSuccess(exchange, taskManager.getAllSubTasks());
        } else {
            try {
                int id = Integer.parseInt(idParam);
                SubTask subtask = taskManager.getSubTaskById(id); // Теперь бросает исключение если не найден
                sendSuccess(exchange, subtask);
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID подзадачи");
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            }
        }
    }

    /**
     * Обрабатывает POST запросы для подзадач.
     * Поддерживает два варианта:
     * - POST /subtasks - создает новую подзадачу
     * - POST /subtasks/{id} - обновляет существующую подзадачу
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);

        try {
            SubTask subtask = gson.fromJson(requestBody, SubTask.class);

            if (subtask == null) {
                sendBadRequest(exchange, "Неверный формат JSON");
                return;
            }

            String idParam = getPathParameter(exchange, 1);

            if (idParam == null) {
                int newId = taskManager.createSubTask(subtask);
                subtask.setId(newId);
                sendCreated(exchange, subtask);
            } else {
                int id = Integer.parseInt(idParam);
                subtask.setId(id);
                taskManager.updateSubTask(subtask);
                sendSuccess(exchange, subtask);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            sendBadRequest(exchange, "Неверный формат JSON: " + e.getMessage());
        } catch (NumberFormatException e) {
            sendBadRequest(exchange, "Неверный формат ID подзадачи");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("пересекается")) {
                sendHasInteractions(exchange, e.getMessage());
            } else {
                sendBadRequest(exchange, e.getMessage());
            }
        } catch (Exception e) {
            sendInternalError(exchange, "Ошибка при создании подзадачи " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для подзадач.
     * Поддерживает два варианта:
     * - DELETE /subtasks - удаляет все подзадачи
     * - DELETE /subtasks/{id} - удаляет подзадачу по указанному ID
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            taskManager.deleteAllSubTasks();
            sendNoContent(exchange);
        } else {
            try {
                int id = Integer.parseInt(idParam);
                taskManager.deleteSubTaskById(id);
                sendNoContent(exchange);
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID подзадачи");
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            }
        }
    }
}