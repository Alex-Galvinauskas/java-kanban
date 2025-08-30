package taskmanager.app.service.manager;

import taskmanager.app.entity.*;
import taskmanager.app.exception.ManagerSaveException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Менеджер задач с сохранением в CSV-файл.
 */
public class FileBackedTasksManager extends InMemoryTaskManager {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final Path filePath;
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public FileBackedTasksManager(final Path filePath) {
        this.filePath = filePath;
    }

    public FileBackedTasksManager(String fileName) {
        this.filePath = Path.of(fileName);
    }

    /**
     * Загружает менеджер задач из файла.
     *
     * @param filePath - путь к файлу
     *
     * @return менеджер задач
     */
    public static FileBackedTasksManager loadFromFile(Path filePath) {
        FileBackedTasksManager manager = new FileBackedTasksManager(filePath);
        try {
            if (!Files.exists(filePath)) {
                return manager;
            }

            List<String> lines = Files.readAllLines(filePath, CHARSET);
            if (lines.size() <= 1) {
                return manager;
            }

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    Task task = fromString(line);
                    addTaskToManager(manager, task);
                } catch (IllegalArgumentException e) {
                    System.err.println("Ошибка чтения строки: " + line + " - " + e.getMessage());
                }
            }

            restoreEpicRelationships(manager);
            manager.updateIdCounter();

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка загрузки файла: " + filePath.getFileName());
        }
        return manager;
    }

    /**
     * Преобразует строку CSV в задачу.
     *
     * @param value - строка CSV
     *
     * @return восстановленная задача
     *
     * @throws IllegalArgumentException если строка не соответствует формату
     */
    public static Task fromString(String value) {
        List<String> fields = parseCsvLine(value);

        if (fields.size() < 5) {
            throw new IllegalArgumentException("Недостаточно полей для создания задачи: " + value);
        }

        try {
            int id = Integer.parseInt(fields.get(0));
            TaskType type = TaskType.valueOf(fields.get(1));
            String name = unescapeCsvField(fields.get(2));
            StatusTask status = StatusTask.valueOf(fields.get(3));
            String description = unescapeCsvField(fields.get(4));

            switch (type) {
                case TASK:
                    return new Task(id, name, description, status);
                case EPIC:
                    return new Epic(id, name, description);
                case SUBTASK:
                    if (fields.size() < 6 || fields.get(5).isEmpty()) {
                        throw new IllegalArgumentException("Для подзадачи отсутствует epicId: " + value);
                    }
                    int epicId = Integer.parseInt(fields.get(5));
                    return new SubTask(id, name, description, status, epicId);
                default:
                    throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный числовой формат: " + value, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неверные данные в строке: " + value, e);
        }
    }

    /**
     * Генерирует уникальный идентификатор.
     *
     * @return следующий доступный идентификатор
     */
    @Override
    public int generateId() {
        return idCounter.incrementAndGet();
    }

    @Override
    protected void afterSubTaskDeletion(int subTaskId) {
        saveWithExceptionHandling("Ошибка удаления подзадачи");
    }

    @Override
    protected void afterAllSubTasksDeletion() {
        saveWithExceptionHandling("Ошибка удаления всех подзадач");
    }

    @Override
    protected void afterEpicDeletion(int epicId) {
        saveWithExceptionHandling("Ошибка удаления эпика");
    }

    @Override
    protected void afterAllEpicsDeletion() {
        saveWithExceptionHandling("Ошибка удаления всех эпиков");
    }

    @Override
    protected void afterTaskDeletion(int taskId) {
        saveWithExceptionHandling("Ошибка удаления задачи");
    }

    @Override
    protected void afterAllTasksDeletion() {
        saveWithExceptionHandling("Ошибка удаления всех задач");
    }

    @Override
    protected void afterSubTaskUpdate(SubTask subTask) {
        saveWithExceptionHandling("Ошибка обновления подзадачи");
    }

    @Override
    protected void afterTaskUpdate(Task task) {
        saveWithExceptionHandling("Ошибка обновления задачи");
    }

    @Override
    protected void afterSubTaskCreation(SubTask subTask) {
        saveWithExceptionHandling("Ошибка сохранения подзадачи");
    }

    @Override
    protected void afterEpicCreation(Epic epic) {
        saveWithExceptionHandling("Ошибка сохранения эпика");
    }

    @Override
    protected void afterTaskCreation(Task task) {
        saveWithExceptionHandling("Ошибка сохранения задачи");
    }

    /**
     * Сохраняет все задачи в файл.
     */
    protected void save() {
        try {
            ensureFileExists();

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, CHARSET)) {
                writeHeader(writer);
                writeAllTasks(writer);
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения файла");
        }
    }

    /**
     * Сохраняет в файл.
     *
     * @param message - сообщение об ошибке
     */
    private void saveWithExceptionHandling(String message) {
        try {
            save();
        } catch (RuntimeException e) {
            throw new ManagerSaveException(message, e);
        }
    }

    /**
     * Проверяет, существует ли файл и создает его при отсутствии.
     *
     * @throws IOException если возникла ошибка при создании файла
     */
    private void ensureFileExists() throws IOException {
        Path parentDir = filePath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }

    /**
     * Пишет заголовок в файл.
     *
     * @param writer - объект для записи в файл
     *
     * @throws IOException если возникла ошибка при записи
     */
    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("id,type,name,status,description,epic");
        writer.newLine();
    }

    /**
     * Пишет все задачи в файл.
     *
     * @param writer для записи
     *
     * @throws IOException - если возникла ошибка при записи
     */
    private void writeAllTasks(BufferedWriter writer) throws IOException {
        for (Task task : getAllTasks()) {
            writer.write(taskToString(task));
            writer.newLine();
        }
        for (Epic epic : getAllEpics()) {
            writer.write(taskToString(epic));
            writer.newLine();
        }
        for (SubTask subtask : getAllSubTasks()) {
            writer.write(taskToString(subtask));
            writer.newLine();
        }
    }

    /**
     * Преобразует задачу в строку для CSV.
     *
     * @param task - задача
     *
     * @return строка для CSV
     */
    private String taskToString(Task task) {
        return String.join(",",
                String.valueOf(task.getId()),
                task.getType().name(),
                escapeCsvField(task.getName()),
                task.getStatus().name(),
                escapeCsvField(task.getDescription()),
                task instanceof SubTask ? String.valueOf(((SubTask) task).getEpicId()) : ""
        );
    }

    /**
     * Экран полей для CSV.
     *
     * @param field - поле для экранирования
     *
     * @return экранированная строка
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        String escaped = field.replace("\n", "\\n").replace("\r", "\\r");

        if (escaped.contains("\"") || escaped.contains(",")) {
            return "\"" + escaped.replace("\"", "\"\"") + "\"";
        }
        return escaped;
    }

    /**
     * Добавляет задачу в менеджере задач и обновляет счетчик.
     *
     * @param manager - менеджер задач
     * @param task    - задача для добавления
     */
    private static void addTaskToManager(FileBackedTasksManager manager, Task task) {
        switch (task.getType()) {
            case TASK:
                manager.restoreTaskDirectly(task);
                break;
            case EPIC:
                manager.restoreEpicDirectly((Epic) task);
                break;
            case SUBTASK:
                manager.restoreSubTaskDirectly((SubTask) task);
                break;
        }
    }

    /**
     * Восстанавливает связи между epic и subTask.
     *
     * @param manager - менеджер задач
     */
    private static void restoreEpicRelationships(FileBackedTasksManager manager) {
        for (SubTask subTask : manager.subTasks.values()) {
            Epic epic = manager.epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.addSubTaskId(subTask.getId());
            }
        }

        for (Epic epic : manager.epics.values()) {
            calculateEpicStatus(epic, manager.subTasks);
        }
    }

    /**
     * Вычисляет статус epic на основе статусов его subTask.
     *
     * @param epic     - epic
     * @param subTasks - subTask
     */
    private static void calculateEpicStatus(Epic epic, Map<Integer, SubTask> subTasks) {
        if (epic.getSubTaskIds().isEmpty()) {
            epic.setStatus(StatusTask.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (int subTaskId : epic.getSubTaskIds()) {
            SubTask subTask = subTasks.get(subTaskId);
            if (subTask != null) {
                StatusTask status = subTask.getStatus();
                if (status != StatusTask.NEW) {
                    allNew = false;
                }
                if (status != StatusTask.DONE) {
                    allDone = false;
                }
            }
        }

        if (allNew) {
            epic.setStatus(StatusTask.NEW);
        } else if (allDone) {
            epic.setStatus(StatusTask.DONE);
        } else {
            epic.setStatus(StatusTask.IN_PROGRESS);
        }
    }

    /**
     * Парсит строку CSV, учитывая экранирование.
     *
     * @param line - строка CSV
     *
     * @return список полей
     */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        if (inQuotes) {
            throw new IllegalArgumentException("Незакрытые кавычки в строке: " + line);
        }
        return fields;
    }

    /**
     * Убирает экран CSV.
     *
     * @param field - экранированная поле
     *
     * @return обычная строка
     */
    private static String unescapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        String unescaped = field;
        if (field.startsWith("\"") && field.endsWith("\"")) {
            unescaped = field.substring(1, field.length() - 1);
            unescaped = unescaped.replace("\"\"", "\"");
        }
        return unescaped.replace("\\n", "\n").replace("\\r", "\r");
    }

    /**
     * Обновляет последовательность идентификаторов.
     */
    private void updateIdCounter() {
        int maxId = 0;

        for (Task task : getAllTasks()) {
            maxId = Math.max(maxId, task.getId());
        }
        for (Epic epic : getAllEpics()) {
            maxId = Math.max(maxId, epic.getId());
        }
        for (SubTask subTask : getAllSubTasks()) {
            maxId = Math.max(maxId, subTask.getId());
        }

        idCounter.set(maxId);
    }


    private static FileBackedTasksManager getFileBackedTaskManager(Path testPath) throws IOException {
        FileBackedTasksManager fileManager = new FileBackedTasksManager(testPath);

        Task task1 = new Task(fileManager.generateId(), "Простая задача 1",
                "Описание задачи 1", StatusTask.NEW);
        Task task2 = new Task(fileManager.generateId(), "Простая задача 2",
                "Описание задачи 2", StatusTask.IN_PROGRESS);

        Epic epic1 = new Epic(fileManager.generateId(), "Эпик 1", "Описание эпика 1");
        Epic epic2 = new Epic(fileManager.generateId(), "Эпик 2", "Описание эпика 2");

        SubTask subTask1 = new SubTask(fileManager.generateId(), "Подзадача 1",
                "Описание подзадачи 1", StatusTask.NEW, epic1.getId());
        SubTask subTask2 = new SubTask(fileManager.generateId(), "Подзадача 2",
                "Описание подзадачи 2", StatusTask.IN_PROGRESS, epic1.getId());
        SubTask subTask3 = new SubTask(fileManager.generateId(), "Подзадача 3",
                "Описание подзадачи 3", StatusTask.DONE, epic1.getId());

        fileManager.createTask(task1);
        fileManager.createTask(task2);
        fileManager.createEpic(epic1);
        fileManager.createEpic(epic2);
        fileManager.createSubTask(subTask1);
        fileManager.createSubTask(subTask2);
        fileManager.createSubTask(subTask3);

        fileManager.save();
        return fileManager;
    }

    public static void main(String[] args) throws IOException {
        Path testPath = Path.of("test_tasks.csv");

        try {
            FileBackedTasksManager fileManager = getFileBackedTaskManager(testPath);

            System.out.println("Первый менеджер создан. Задачи сохранены в файл.");
            System.out.println("Задачи в первом менеджере:");
            System.out.println("Простыe задачи: " + fileManager.getAllTasks().size());
            System.out.println("Эпики: " + fileManager.getAllEpics().size());
            System.out.println("Подзадачи: " + fileManager.getAllSubTasks().size());

            FileBackedTasksManager manager2 = FileBackedTasksManager.loadFromFile(testPath);

            System.out.println("\nВторой менеджер загружен из файла.");
            System.out.println("Задачи во втором менеджере:");
            System.out.println("Простыe задачи: " + manager2.getAllTasks().size());
            System.out.println("Эпики: " + manager2.getAllEpics().size());
            System.out.println("Подзадачи: " + manager2.getAllSubTasks().size());

            boolean allTasksMatch = fileManager.getAllTasks().equals(manager2.getAllTasks());
            boolean allEpicsMatch = fileManager.getAllEpics().equals(manager2.getAllEpics());
            boolean allSubTasksMatch = fileManager.getAllSubTasks().equals(manager2.getAllSubTasks());

            System.out.println("\nРезультаты проверки:");
            System.out.println("Простыe задачи совпадают: " + allTasksMatch);
            System.out.println("Эпики совпадают: " + allEpicsMatch);
            System.out.println("Подзадачи совпадают: " + allSubTasksMatch);

            if (allTasksMatch && allEpicsMatch && allSubTasksMatch) {
                System.out.println("✅ Все задачи успешно сохранены и загружены!");
            } else {
                System.out.println("❌ Обнаружено несоответствие в данных!");
            }

        } finally {
            if (Files.exists(testPath)) {
                try {
                    Files.delete(testPath);
                    System.out.println("✅ Временный файл удален: " + testPath.getFileName());
                } catch (IOException e) {
                    System.out.println("❌ Не удалось удалить временный файл: " + e.getMessage());
                }
            }
        }
    }

}