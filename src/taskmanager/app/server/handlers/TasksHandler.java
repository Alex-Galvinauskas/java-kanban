package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import taskmanager.app.entity.Task;
import taskmanager.app.exception.NotFoundException;
import taskmanager.app.management.TaskManager;

import java.io.IOException;

/**
 * Обработчик HTTP запросов для работы с задачами.
 * Обрабатывает операции GET, POST, DELETE для endpoints /tasks и /tasks/{id}.
 * Наследует общую функциональность от BaseHttpHandler.
 */
public class TasksHandler extends BaseHttpHandler {

    /**
     * Создает новый обработчик задач.
     *
     * @param taskManager менеджер задач для выполнения операций с задачами
     * @param gson экземпляр Gson для сериализации и десериализации JSON
     */
    public TasksHandler(TaskManager taskManager, Gson gson) {
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
     * Обрабатывает GET запросы для задач.
     * Поддерживает два варианта:
     * - GET /tasks - возвращает все задачи
     * - GET /tasks/{id} - возвращает задачу по указанному ID
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            sendSuccess(exchange, taskManager.getAllTasks());
        } else {
            try {
                int id = Integer.parseInt(idParam);
                Task task = taskManager.getTaskById(id);
                sendSuccess(exchange, task);
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID задачи");
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            }
        }
    }

    /**
     * Обрабатывает POST запросы для задач.
     * Поддерживает два варианта:
     * - POST /tasks - создает новую задачу
     * - POST /tasks/{id} - обновляет существующую задачу
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            Task task = gson.fromJson(requestBody, Task.class);

            if (task == null) {
                sendBadRequest(exchange, "Неверный запрос: некорректный JSON формат");
                return;
            }

            String idParam = getPathParameter(exchange, 1);

            if (idParam == null) {
                int newId = taskManager.createTask(task);
                task.setId(newId);
                sendCreated(exchange, task);
            } else {
                int id = Integer.parseInt(idParam);
                task.setId(id);
                taskManager.updateTask(task);
                sendSuccess(exchange, task);
            }
        } catch (JsonSyntaxException e) {
            sendBadRequest(exchange, "Неверный запрос: некорректный JSON формат");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("пересекается")) {
                sendHasInteractions(exchange, e.getMessage());
            } else {
                sendBadRequest(exchange, e.getMessage());
            }
        } catch (Exception e) {
            sendInternalError(exchange, "Ошибка при сохранении задачи " + e.getMessage());
        }
    }

    /**
     * Обрабатывает DELETE запросы для задач.
     * Поддерживает два варианта:
     * - DELETE /tasks - удаляет все задачи
     * - DELETE /tasks/{id} - удаляет задачу по указанному ID
     *
     * @param exchange HTTP обмен для обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        String idParam = getPathParameter(exchange, 1);

        if (idParam == null) {
            taskManager.deleteAllTasks();
            sendNoContent(exchange);
        } else {
            try {
                int id = Integer.parseInt(idParam);
                taskManager.deleteTaskById(id);
                sendNoContent(exchange);
            } catch (NumberFormatException e) {
                sendBadRequest(exchange, "Неверный формат ID задачи");
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            }
        }
    }
}