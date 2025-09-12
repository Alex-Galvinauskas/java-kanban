package taskmanager.app.service.manager;

import taskmanager.app.entity.*;
import taskmanager.app.exception.ManagerSaveException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Реализация менеджера задач с сохранением состояния в CSV-файл.
 * Автоматически сохраняет все изменения в файл после каждой операции.
 * Поддерживает загрузку состояния из файла при старте.
 */
public class FileBackedTasksManager extends InMemoryTaskManager {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int MIN_FIELDS_FOR_TASK = 5;
    private static final int MIN_FIELDS_FOR_SUBTASK = 6;
    private static final int FIELD_INDEX_TYPE = 1;
    private static final int FIELD_INDEX_NAME = 2;
    private static final int FIELD_INDEX_STATUS = 3;
    private static final int FIELD_INDEX_DESCRIPTION = 4;
    private static final int FIELD_INDEX_EPIC_ID = 5;
    private static final int FIELD_INDEX_START_TIME = 6;
    private static final int FIELD_INDEX_DURATION = 7;
    private static final int FIELD_INDEX_END_TIME = 8;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Path filePath;
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private boolean isLoading = false;

    public FileBackedTasksManager(final Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Восстанавливает состояние менеджера из CSV-файла.
     * Если файл не существует или пуст - возвращает пустой менеджер.
     *
     * @throws ManagerSaveException если возникли проблемы с чтением файла
     */
    public static FileBackedTasksManager loadFromFile(Path filePath) {
        FileBackedTasksManager manager = new FileBackedTasksManager(filePath);
        manager.isLoading = true;

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
                    System.err.println("Пропускаем некорректную строку: " + line + " - " + e.getMessage());
                }
            }

            restoreEpicRelationships(manager);
            restorePrioritizedTasks(manager);
            manager.updateIdCounter();

        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось прочитать файл: " + filePath.getFileName(), e);
        } finally {
            manager.isLoading = false;
        }
        return manager;
    }

    /**
     * Восстанавливает приоритетные задачи в менеджер
     */
    private static void restorePrioritizedTasks(FileBackedTasksManager manager) {
        manager.prioritizedTasks.clear();

        manager.tasks.values().stream()
                .filter(task -> task.getStartTime() != null)
                .forEach(manager.prioritizedTasks::add);

        manager.subTasks.values().stream()
                .filter(subTask -> subTask.getStartTime() != null)
                .forEach(manager.prioritizedTasks::add);

        manager.epics.values().stream()
                .filter(epic -> epic.getStartTime() != null)
                .forEach(manager.prioritizedTasks::add);
    }

    /**
     * Парсит CSV строку в объект задачи.
     * Формат: id,type,name,status,description,epic,start_time,duration,end_time
     *
     * @throws IllegalArgumentException если строка имеет неверный формат или недостаточно полей
     */
    public static Task fromString(String value) {
        List<String> fields = parseCsvLine(value);

        if (fields.size() < MIN_FIELDS_FOR_TASK) {
            throw new IllegalArgumentException("Недостаточно полей для создания задачи: " + value);
        }

        try {
            int id = Integer.parseInt(fields.getFirst());
            TaskType type = TaskType.valueOf(fields.get(FIELD_INDEX_TYPE));
            String name = unescapeCsvField(fields.get(FIELD_INDEX_NAME));
            StatusTask status = StatusTask.valueOf(fields.get(FIELD_INDEX_STATUS));
            String description = unescapeCsvField(fields.get(FIELD_INDEX_DESCRIPTION));
            LocalDateTime startTime = parseOptionalDateTime(fields, FIELD_INDEX_START_TIME);
            Duration duration = parseOptionalDuration(fields, FIELD_INDEX_DURATION);
            LocalDateTime endTime = parseOptionalDateTime(fields, FIELD_INDEX_END_TIME);

            switch (type) {
                case TASK:
                    return new Task(id, name, description, status, duration, startTime);
                case EPIC:
                    Epic epic = new Epic(id, name, description);
                    epic.setStartTime(startTime);
                    epic.setDuration(duration);
                    epic.setEndTime(endTime);
                    return epic;
                case SUBTASK:
                    if (fields.size() < MIN_FIELDS_FOR_SUBTASK || fields.get(FIELD_INDEX_EPIC_ID).isEmpty()) {
                        throw new IllegalArgumentException("Для подзадачи должен быть указан ID эпика");
                    }
                    int epicId = Integer.parseInt(fields.get(FIELD_INDEX_EPIC_ID));
                    return new SubTask(id, name, description, status, duration, startTime, epicId);
                default:
                    throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный числовой формат в данных: " + value, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректные данные в строке: " + value, e);
        }
    }

    @Override
    public int generateId() {
        return idCounter.incrementAndGet();
    }

    /**
     * Автоматически сохраняет состояние в файл после операций.
     * Не срабатывает во время загрузки данных из файла.
     */
    private void autoSave() {
        if (isLoading) {
            return;
        }
        try {
            save();
        } catch (RuntimeException e) {
            throw new ManagerSaveException("Ошибка автоматического сохранения", e);
        }
    }

    @Override
    protected void afterSubTaskDeletion(int subTaskId) {
        autoSave();
    }

    @Override
    protected void afterAllSubTasksDeletion() {
        autoSave();
    }

    @Override
    protected void afterEpicDeletion(int epicId) {
        autoSave();
    }

    @Override
    protected void afterAllEpicsDeletion() {
        autoSave();
    }

    @Override
    protected void afterTaskDeletion(int taskId) {
        autoSave();
    }

    @Override
    protected void afterAllTasksDeletion() {
        autoSave();
    }

    @Override
    protected void afterSubTaskUpdate(SubTask subTask) {
        autoSave();
    }

    @Override
    protected void afterTaskUpdate(Task task) {
        autoSave();
    }

    @Override
    protected void afterSubTaskCreation(SubTask subTask) {
        autoSave();
    }

    @Override
    protected void afterEpicCreation(Epic epic) {
        autoSave();
    }

    @Override
    protected void afterTaskCreation(Task task) {
        autoSave();
    }

    /**
     * Сохраняет текущее состояние всех задач в CSV-файл.
     *
     * @throws ManagerSaveException если не удалось записать в файл
     */
    protected void save() {
        try {
            ensureFileExists();

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, CHARSET)) {
                writeHeader(writer);
                writeAllTasks(writer);
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка записи в файл: " + filePath, e);
        }
    }

    /**
     * Гарантирует, что файл и родительская директория существуют.
     * Создает их при необходимости.
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
     * Записывает заголовок CSV с названиями полей.
     */
    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("id,type,name,status,description,epic,start_time,duration,end_time");
        writer.newLine();
    }

    /**
     * Записывает все задачи в файл в порядке: обычные задачи, эпики, подзадачи.
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
     * Преобразует задачу в CSV строку с экранированием специальных символов.
     */
    private String taskToString(Task task) {
        List<String> fields = new ArrayList<>();
        fields.add(String.valueOf(task.getId()));
        fields.add(task.getType().name());
        fields.add(escapeCsvField(task.getName()));
        fields.add(task.getStatus().name());
        fields.add(escapeCsvField(task.getDescription()));

        if (task instanceof SubTask) {
            fields.add(String.valueOf(((SubTask) task).getEpicId()));
        } else {
            fields.add("");
        }

        fields.add(formatOptionalDateTime(task.getStartTime()));
        fields.add(formatOptionalDuration(task.getDuration()));

        if (task instanceof Epic) {
            fields.add(formatOptionalDateTime(task.getEndTime()));
        } else {
            fields.add("");
        }

        return String.join(",", fields);
    }

    /**
     * Экранирует строку для корректного сохранения в CSV.
     * Обрабатывает кавычки, запятые и переносы строк.
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
     * Добавляет задачу в менеджер, пропуская дубликаты.
     */
    private static void addTaskToManager(FileBackedTasksManager manager, Task task) {
        switch (task.getType()) {
            case TASK:
                if (!manager.tasks.containsKey(task.getId())) {
                    manager.restoreTaskDirectly(task);
                }
                break;
            case EPIC:
                if (!manager.epics.containsKey(task.getId())) {
                    manager.restoreEpicDirectly((Epic) task);
                }
                break;
            case SUBTASK:
                if (!manager.subTasks.containsKey(task.getId())) {
                    manager.restoreSubTaskDirectly((SubTask) task);
                }
                break;
        }
    }

    /**
     * Восстанавливает связи между эпиками и их подзадачами после загрузки.
     */
    private static void restoreEpicRelationships(FileBackedTasksManager manager) {
        manager.subTasks.values().forEach(subTask -> {
            Epic epic = manager.epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.addSubTaskId(subTask.getId());
            }
        });

        manager.epics.values().forEach(epic -> {
            calculateEpicStatus(epic, manager.subTasks);
            manager.updateEpicTime(epic.getId());
        });
    }

    /**
     * Вычисляет статус эпика на основе статусов его подзадач.
     */
    private static void calculateEpicStatus(Epic epic, Map<Integer, SubTask> subTasks) {
        if (epic.getSubTaskIds().isEmpty()) {
            epic.setStatus(StatusTask.NEW);
            return;
        }

        List<StatusTask> statuses = epic.getSubTaskIds().stream()
                .map(subTasks::get)
                .filter(Objects::nonNull)
                .map(SubTask::getStatus)
                .toList();

        boolean allNew = statuses.stream().allMatch(status -> status == StatusTask.NEW);
        boolean allDone = statuses.stream().allMatch(status -> status == StatusTask.DONE);

        if (allNew) {
            epic.setStatus(StatusTask.NEW);
        } else if (allDone) {
            epic.setStatus(StatusTask.DONE);
        } else {
            epic.setStatus(StatusTask.IN_PROGRESS);
        }
    }

    /**
     * Парсит CSV строку с учетом экранирования кавычек.
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
            throw new IllegalArgumentException("Незакрытые кавычки в CSV строке");
        }

        return fields;
    }

    /**
     * Убирает экранирование из CSV поля.
     */
    private static String unescapeCsvField(String field) {
        if (field == null) return "";

        String unescaped = field;
        if (field.startsWith("\"") && field.endsWith("\"")) {
            unescaped = field.substring(1, field.length() - 1).replace("\"\"", "\"");
        }
        return unescaped.replace("\\n", "\n").replace("\\r", "\r");
    }

    /**
     * Обновляет счетчик ID на основе максимального ID в загруженных задачах.
     */
    private void updateIdCounter() {
        int maxId = Stream.of(tasks, epics, subTasks)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        idCounter.set(maxId);
    }

    private static LocalDateTime parseOptionalDateTime(List<String> fields, int index) {
        return (fields.size() > index && !fields.get(index).isEmpty()) ?
                LocalDateTime.parse(fields.get(index), DATE_TIME_FORMATTER) : null;
    }

    private static Duration parseOptionalDuration(List<String> fields, int index) {
        return (fields.size() > index && !fields.get(index).isEmpty()) ?
                Duration.ofMinutes(Long.parseLong(fields.get(index))) : null;
    }

    private static String formatOptionalDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }

    private static String formatOptionalDuration(Duration duration) {
        return duration != null ? String.valueOf(duration.toMinutes()) : "";
    }

    /**
     * Переопределяем методы восстановления для добавления в prioritizedTasks
     */
    @Override
    protected void restoreTaskDirectly(Task task) {
        super.restoreTaskDirectly(task);
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    @Override
    protected void restoreEpicDirectly(Epic epic) {
        super.restoreEpicDirectly(epic);
        if (epic.getStartTime() != null) {
            prioritizedTasks.add(epic);
        }
    }

    @Override
    protected void restoreSubTaskDirectly(SubTask subTask) {
        super.restoreSubTaskDirectly(subTask);
        if (subTask.getStartTime() != null) {
            prioritizedTasks.add(subTask);
        }
    }
}