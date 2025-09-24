package java.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.app.management.TaskManager;
import java.io.IOException;

/**
 * Обработчик HTTP запросов для получения истории просмотров задач.
 * Обрабатывает GET запросы к endpoint /history.
 * Наследует общую функциональность от BaseHttpHandler.
 *
 * <p>Предоставляет доступ к истории последних просмотренных задач,
 * отсортированных в порядке их последнего обращения.
 */
public class HistoryHandler extends BaseHttpHandler {

    /**
     * Создает новый обработчик истории задач.
     *
     * @param taskManager менеджер задач для получения истории просмотров
     * @param gson экземпляр Gson для сериализации истории в JSON
     */
    public HistoryHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    /**
     * Обрабатывает входящий HTTP запрос.
     * Поддерживает только GET метод для получения истории просмотров.
     *
     * <p>Возвращает JSON-массив задач в порядке их последнего просмотра.
     *
     * @param exchange HTTP обмен для обработки запроса и отправки ответа
     * @throws IOException если произошла ошибка ввода-вывода при обработке запроса
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendSuccess(exchange, taskManager.getHistory());
            } else {
                sendBadRequest(exchange, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendInternalError(exchange, "Внутренняя ошибка сервера " + e.getMessage());
        }
    }
}