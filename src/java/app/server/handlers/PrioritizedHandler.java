package java.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.app.management.TaskManager;
import java.io.IOException;

/**
 * Обработчик HTTP запросов для получения приоритетного списка задач.
 * Обрабатывает GET запросы к endpoint /prioritized.
 * Наследует общую функциональность от BaseHttpHandler.
 *
 * <p>Предоставляет доступ к списку задач, отсортированных по приоритету
 * (по времени начала выполнения). Задачи без времени начала выполняются в конце списка.
 */
public class PrioritizedHandler extends BaseHttpHandler {

    /**
     * Создает новый обработчик приоритетного списка задач.
     *
     * @param taskManager менеджер задач для получения приоритетного списка
     * @param gson экземпляр Gson для сериализации списка в JSON
     */
    public PrioritizedHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    /**
     * Обрабатывает входящий HTTP запрос.
     * Поддерживает только GET метод для получения приоритетного списка задач.
     *
     * @param exchange HTTP обмен для обработки запроса и отправки ответа
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendSuccess(exchange, taskManager.getPrioritizedTasks());
            } else {
                sendBadRequest(exchange, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendInternalError(exchange, "Внутренняя ошибка сервера " + e.getMessage());
        }
    }
}