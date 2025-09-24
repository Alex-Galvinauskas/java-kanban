package java.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.app.management.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Абстрактный базовый класс для обработчиков HTTP запросов.
 * Предоставляет общую функциональность для работы с HTTP запросами и ответами,
 * включая чтение тела запроса, отправку ответов с различными статус-кодами
 * и извлечение параметров из пути URL.
 * Все конкретные обработчики должны наследоваться от этого класса.
 */
public abstract class BaseHttpHandler implements HttpHandler {
    protected final TaskManager taskManager;
    protected final Gson gson;

    /**
     * Конструктор базового обработчика.
     *
     * @param taskManager менеджер задач для выполнения операций
     * @param gson экземпляр Gson для сериализации и десериализации JSON
     */
    protected BaseHttpHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    /**
     * Читает тело HTTP запроса и возвращает его в виде строки.
     *
     * @param exchange HTTP обмен для чтения тела запроса
     * @return содержимое тела запроса в виде строки
     * @throws IOException если произошла ошибка ввода-вывода при чтении тела запроса
     */
    public String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Отправляет текстовый ответ с указанным статус-кодом.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param response текст ответа для отправки
     * @param statusCode HTTP статус-код ответа
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    public void sendText(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

        if (exchange.getResponseHeaders() != null) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        }

        if (responseBytes.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
        }

        OutputStream os = exchange.getResponseBody();
        if (os != null) {
            try (os) {
                if (responseBytes.length > 0) {
                    os.write(responseBytes);
                }
            }
        }
    }

    /**
     * Отправляет успешный ответ со статусом 200 OK.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param responseObject объект для сериализации в JSON и отправки в теле ответа
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendSuccess(HttpExchange exchange, Object responseObject) throws IOException {
        String response = gson.toJson(responseObject);
        sendText(exchange, response, 200);
    }

    /**
     * Отправляет ответ со статусом 201 Created.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param responseObject объект для сериализации в JSON и отправки в теле ответа
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendCreated(HttpExchange exchange, Object responseObject) throws IOException {
        String response = gson.toJson(responseObject);
        sendText(exchange, response, 201);
    }

    /**
     * Отправляет ответ со статусом 404 Not Found.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param message сообщение об ошибке
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendNotFound(HttpExchange exchange, String message) throws IOException {
        String response = gson.toJson(new ErrorResponse(message));
        sendText(exchange, response, 404);
    }

    /**
     * Отправляет ответ со статусом 400 Bad Request.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param message сообщение об ошибке
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        String response = gson.toJson(new ErrorResponse(message));
        sendText(exchange, response, 400);
    }

    /**
     * Отправляет ответ со статусом 406 Not Acceptable.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param message сообщение об ошибке
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendHasInteractions(HttpExchange exchange, String message) throws IOException {
        String response = gson.toJson(new ErrorResponse(message));
        sendText(exchange, response, 406);
    }

    /**
     * Отправляет ответ со статусом 500 Internal Server Error.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @param message сообщение об ошибке
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendInternalError(HttpExchange exchange, String message) throws IOException {
        String response = gson.toJson(new ErrorResponse(message));
        sendText(exchange, response, 500);
    }

    /**
     * Отправляет ответ со статусом 204 No Content.
     *
     * @param exchange HTTP обмен для отправки ответа
     * @throws IOException если произошла ошибка ввода-вывода при отправке ответа
     */
    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    /**
     * Извлекает параметр из пути URL по указанному индексу.
     *
     * @param exchange HTTP обмен для получения пути запроса
     * @param index индекс параметра в пути (начиная с 0 для первого параметра после корня)
     * @return значение параметра пути или null, если параметр не существует
     */
    public String getPathParameter(HttpExchange exchange, int index) {
        String[] pathParts = exchange.getRequestURI().getPath().split("/");
        if (pathParts.length > index + 1) {
            return pathParts[index + 1];
        }
        return null;
    }

    /**
     * Запись для представления ответа с ошибкой.
     * Используется для сериализации сообщений об ошибках в JSON.
     *
     * @param error сообщение об ошибке
     */
    protected record ErrorResponse(String error) {}
}