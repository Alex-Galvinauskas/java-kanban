package taskmanager.app.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import taskmanager.app.management.Managers;
import taskmanager.app.management.TaskManager;
import taskmanager.app.server.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * HTTP сервер для управления задачами.
 * Предоставляет REST API для работы с задачами, эпиками, подзадачами,
 * историей просмотров и приоритетным списком задач.
 *
 * <p>Сервер использует стандартный порт 8080 и поддерживает следующие endpoints:
 * <ul>
 *   <li>{@code /tasks} - управление задачами</li>
 *   <li>{@code /epics} - управление эпиками</li>
 *   <li>{@code /subtasks} - управление подзадачами</li>
 *   <li>{@code /history} - получение истории просмотров</li>
 *   <li>{@code /prioritized} - получение приоритетного списка задач</li>
 * </ul>
 *
 * <p>Сервер автоматически регистрирует адаптеры для сериализации/десериализации
 * объектов {@link LocalDateTime} и {@link Duration}.
 */
public class HttpTaskServer {
    private static final int DEFAULT_PORT = 8080;
    private final HttpServer server;
    private final TaskManager taskManager;
    private final Gson gson;
    private final int port;

    /**
     * Создает новый экземпляр HTTP сервера задач на порту по умолчанию.
     */
    public HttpTaskServer(TaskManager taskManager) throws IOException {
        this(taskManager, DEFAULT_PORT);
    }

    /**
     * Создает новый экземпляр HTTP сервера задач.
     * Инициализирует менеджер задач, настраивает Gson и конфигурирует маршруты.
     *
     * @throws IOException если произошла ошибка при создании HTTP сервера
     */
    public HttpTaskServer(TaskManager taskManager, int port) throws IOException {
        this.taskManager = taskManager;
        this.gson = createGson();
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        configureRoutes();
    }

    /**
     * Конструктор по умолчанию - использует Managers.getDefault() и порт по умолчанию.
     */
    public HttpTaskServer() throws IOException {
        this(Managers.getDefault());
    }

    /**
     * Конструктор по умолчанию с указанием порта
     */
    public HttpTaskServer(int port) throws IOException {
        this(Managers.getDefault(), port);
    }

    /**
     * Создает и настраивает экземпляр Gson с адаптерами для специальных типов.
     *
     * @return настроенный экземпляр Gson с поддержкой LocalDateTime и Duration
     */
    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Настраивает маршруты HTTP сервера.
     * Регистрирует обработчики для различных endpoints API.
     */
    private void configureRoutes() {
        server.createContext("/tasks", new TasksHandler(taskManager, gson));
        server.createContext("/epics", new EpicsHandler(taskManager, gson));
        server.createContext("/subtasks", new SubtasksHandler(taskManager, gson));
        server.createContext("/history", new HistoryHandler(taskManager, gson));
        server.createContext("/prioritized", new PrioritizedHandler(taskManager, gson));
    }

    /**
     * Запускает HTTP сервер.
     * После запуска сервер начинает принимать входящие подключения на указанном порту.
     */
    public void start() {
        server.start();
        System.out.println("HTTP менеджера задач запущен на порту " + port);
    }

    /**
     * Останавливает HTTP сервер.
     * Сервер прекращает принимать новые подключения и завершает работу.
     */
    public void stop() {
        server.stop(0);
        System.out.println("HTTP менеджера задач остановлен");
    }

    /**
     * Возвращает экземпляр Gson для использования в тестах.
     */
    public Gson getGson() {
        return gson;
    }

    public int getPort() {
        return port;
    }

    /**
     * Основной метод для запуска HTTP сервера задач.
     * Создает экземпляр сервера, запускает его и регистрирует хуки для graceful shutdown.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        try {
            HttpTaskServer taskServer = new HttpTaskServer();
            taskServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nПолучен сигнал завершения...");
                taskServer.stop();
            }));

            System.out.println("Нажмите Ctrl+C для остановки сервера");
            Thread.currentThread().join();

        } catch (IOException e) {
            System.err.println("Ошибка запуска HTTP сервера: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Сервер завершает работу...");
        }
    }
}