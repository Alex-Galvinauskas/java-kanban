package taskmanager.app.service.manager;

import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.exception.ValidationException;
import taskmanager.app.management.TaskManager;
import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.util.StatusCheckResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * TaskManager
 * Управляет задачами, эпиками и подзадачами, поддерживает историю просмотров.
 */
public class InMemoryTaskManager implements TaskManager {

    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private final ValidationException validator = new ValidationException();

    /**
     * Восстанавливает задачу напрямую в карту задач.
     * Используется для восстановления состояния менеджера.
     *
     * @param task задача для восстановления
     */
    protected void restoreTaskDirectly(Task task) {
        tasks.put(task.getId(), task);
    }

    /**
     * Восстанавливает эпик напрямую в карту эпиков.
     * Используется для восстановления состояния менеджера.
     *
     * @param epic эпик для восстановления
     */
    protected void restoreEpicDirectly(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    /**
     * Восстанавливает эпик напрямую в карту эпиков.
     * Используется для восстановления состояния менеджера.
     *
     * @param subTask подзадача для восстановления
     */
    protected void restoreSubTaskDirectly(SubTask subTask) {
        subTasks.put(subTask.getId(), subTask);
        Epic epic = epics.get(subTask.getEpicId());
        if (epic != null) {
            epic.addSubTaskId(subTask.getId());
        }
    }

    /**
     * Создает задачу
     *
     * @param task задача для создания (не может быть null)
     *
     * @return id созданной задачи
     */
    @Override
    public int createTask(Task task) {
        validator.validateNotNull(task, "Задача");
        if (task.getId() == 0) {
            task.setId(generateId());
        }
        validator.validateTaskForCreation(task, tasks.values());
        tasks.put(task.getId(), task);
        afterTaskCreation(task);
        return task.getId();
    }

    /**
     * Создает эпик
     *
     * @param epic эпик для создания (не может быть null)
     *
     * @return id созданного эпика
     */
    @Override
    public int createEpic(Epic epic) {
        if (epic.getId() == 0) {
            epic.setId(generateId());
        }
        validator.validateForEpicCreation(epic);
        epics.put(epic.getId(), epic);
        afterEpicCreation(epic);
        return epic.getId();
    }

    /**
     * Создает подзадачу
     *
     * @param subTask подзадача для создания (не может быть null)
     *
     * @return id созданной подзадачи
     */
    @Override
    public int createSubTask(SubTask subTask) {
        if (subTask.getId() == 0) {
            subTask.setId(generateId());
        }

        if (subTask.getId() == subTask.getEpicId()) {
            throw new IllegalArgumentException("Подзадача не может быть своим же эпиком");
        }

        validator.validateForSubTaskCreation(subTask, epics);

        subTasks.put(subTask.getId(), subTask);
        Epic epic = epics.get(subTask.getEpicId());
        epic.addSubTaskId(subTask.getId());
        updateEpicStatus(subTask.getEpicId());

        afterSubTaskCreation(subTask);
        return subTask.getId();
    }

    /**
     * @return неизменяемый список всех задач
     */
    @Override
    public List<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    /**
     * Возвращает задачу по id
     *
     * @param id задачи
     *
     * @return копию задачи или null если задача не найдена
     *
     * @throws IllegalArgumentException если id не положительный
     */
    @Override
    public Optional<Task> getTaskById(int id) {
        validator.validatePositiveId(id);
        Optional<Task> taskOptional = Optional.ofNullable(tasks.get(id));

        taskOptional.ifPresent(historyManager::add);
        return taskOptional.map(Task::new);
    }

    /**
     * @return неизменяемый список всех эпиков
     */
    @Override
    public List<Epic> getAllEpics() {
        return List.copyOf(epics.values());
    }

    /**
     * Возвращает эпик по id
     *
     * @param id эпика
     *
     * @return копию эпика или null если эпик не найден
     *
     * @throws IllegalArgumentException если id не положительный
     */
    @Override
    public Optional<Epic> getEpicById(int id) {
        validator.validatePositiveId(id);

        return Optional.ofNullable(epics.get(id))
                .map(epic -> {
                    historyManager.add(epic);
                    return new Epic(epic);
                });
    }

    /**
     * Возвращает список подзадач для указанного эпика.
     *
     * @param epicId id эпика
     *
     * @return неизменяемый список подзадач
     *
     * @throws IllegalArgumentException если id не положительный или эпик не существует
     */
    @Override
    public List<SubTask> getSubTasksByEpicId(int epicId) {
        validator.validatePositiveId(epicId);

        return Optional.ofNullable(epics.get(epicId))
                .map(epic -> {
                    validator.validateEpicExist(epics, epicId);
                    return epic.getSubTaskIds().stream()
                            .map(subTasks::get)
                            .filter(Objects::nonNull)
                            .map(SubTask::new)
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }

    /**
     * @return неизменяемый список всех подзадач
     */
    @Override
    public List<SubTask> getAllSubTasks() {
        return List.copyOf(subTasks.values());
    }

    /**
     * Возвращает подзадачу по id
     *
     * @param id подзадачи
     *
     * @return копию подзадачи или null если подзадача не найдена
     *
     * @throws IllegalArgumentException если id не положительный
     */
    @Override
    public SubTask getSubTaskById(int id) {
        validator.validatePositiveId(id);
        return Optional.ofNullable(subTasks.get(id))
                .map(subTask -> {
                    historyManager.add(subTask);
                    return new SubTask(subTask);
                })
                .orElse(null);
    }

    /**
     * @return неизменяемый список истории в порядке просмотра задач
     */
    @Override
    public Collection<Task> getHistory() {
        return historyManager.getHistory();
    }

    /**
     * Обновляет задачу
     *
     * @param task с обновленными данными
     */
    @Override
    public void updateTask(Task task) {
        validator.validateTaskForUpdate(task, tasks);
        tasks.put(task.getId(), new Task(task));
        afterTaskUpdate(task);
    }

    /**
     * Обновляет подзадачу
     *
     * @param subTask с обновленными данными
     */
    @Override
    public void updateSubTask(SubTask subTask) {
        validator.validateSubTaskForUpdate(subTask, subTasks, epics);
        subTasks.put(subTask.getId(), subTask);
        updateEpicStatus(subTask.getEpicId());
        afterSubTaskUpdate(subTask);
    }

    /**
     * Удаляет все задачи.
     * Очищает историю.
     */
    @Override
    public void deleteAllTasks() {
        Set<Integer> taskIds = new HashSet<>(tasks.keySet());
        tasks.clear();
        taskIds.forEach(historyManager::remove);
        afterAllTasksDeletion();
    }

    /**
     * Удаляет задачу по id
     *
     * @param id задачи
     */
    @Override
    public void deleteTaskById(int id) {
        validator.validatePositiveId(id);
        tasks.remove(id);
        historyManager.remove(id);
        afterTaskDeletion(id);
    }

    /**
     * Удаляет все эпики.
     * Очищает историю для эпиков и подзадач
     */
    @Override
    public void deleteAllEpics() {
        epics.keySet()
                .forEach(id -> {
                    historyManager.remove(id);
                    deleteAllSubTasks();
                });

        epics.clear();
        afterAllEpicsDeletion();
    }

    /**
     * Удаляет эпик по id
     *
     * @param id эпика
     */
    @Override
    public void deleteEpicById(int id) {
        validator.validatePositiveId(id);
        Epic epic = epics.get(id);
        validator.validateEpicExist(epics, id);

        for (int subtaskId : epic.getSubTaskIds()) {
            subTasks.remove(subtaskId);
            historyManager.remove(subtaskId);
        }
        historyManager.remove(id);
        epics.remove(id);
        afterEpicDeletion(id);
    }

    /**
     * Удаляет все подзадачи.
     * Обновляет статусы эпиков и очищает историю
     */
    @Override
    public void deleteAllSubTasks() {
        Set<Integer> subTaskIds = new HashSet<>(subTasks.keySet());

        subTasks.clear();

        for (Integer id : subTaskIds) {
            historyManager.remove(id);
        }

        for (Epic epic : epics.values()) {
            epic.clearSubTaskIds();
            updateEpicStatus(epic.getId());
        }
        afterAllSubTasksDeletion();
    }

    /**
     * Удаляет подзадачу по id
     *
     * @param id подзадачи
     */
    @Override
    public void deleteSubTaskById(int id) {
        validator.validatePositiveId(id);
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            subTasks.remove(id);

            Epic epic = epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.removeSubTaskId(id);
                updateEpicStatus(epic.getId());
            }
            historyManager.remove(id);
            afterSubTaskDeletion(id);
        }
    }

    /**
     * @return новый уникальный идентификатор
     */
    @Override
    public int generateId() {
        return nextId.getAndIncrement();
    }

    protected void afterSubTaskDeletion(int subTaskId) {
    }

    protected void afterAllSubTasksDeletion() {
    }

    protected void afterEpicDeletion(int epicId) {
    }

    protected void afterAllEpicsDeletion() {
    }

    protected void afterTaskDeletion(int taskId) {
    }

    protected void afterAllTasksDeletion() {
    }

    protected void afterSubTaskUpdate(SubTask subTask) {
    }

    protected void afterTaskUpdate(Task task) {
    }

    /**
     * Обновляет статус эпика
     *
     * @param epicId id эпика
     */
    private void updateEpicStatus(int epicId) {
        Optional.ofNullable(epics.get(epicId))
                .ifPresent(epic -> {
                    List<Integer> subtaskIds = epic.getSubTaskIds();
                    if (subtaskIds.isEmpty()) {
                        setEpicStatus(epic, StatusTask.NEW);
                        return;
                    }

                    StatusCheckResult statusCheck = checkSubTasksStatuses(subtaskIds);
                    determineEpicStatus(epic, statusCheck);
                });
    }

    /**
     * Устанавливает статусы эпика
     *
     * @param epic   для обновления
     * @param status новый статус
     */
    private void setEpicStatus(Epic epic, StatusTask status) {
        epic.setStatus(status);
    }

    /**
     * Проверяет статусы подзадач
     *
     * @param subtaskIds список id подзадач
     *
     * @return результат проверки
     */
    private StatusCheckResult checkSubTasksStatuses(List<Integer> subtaskIds) {
        List<StatusTask> statuses = subtaskIds.stream()
                .map(subTasks::get)
                .filter(Objects::nonNull)
                .map(SubTask::getStatus)
                .toList();

        boolean allNew = statuses.stream().allMatch(status -> status == StatusTask.NEW);
        boolean allDone = statuses.stream().allMatch(status -> status == StatusTask.DONE);
        boolean hasInProgress = statuses.stream().anyMatch(status -> status == StatusTask.IN_PROGRESS);

        StatusCheckResult result = new StatusCheckResult();
        result.setAllNew(allNew);
        result.setAllDone(allDone);
        result.setHasInProgress(hasInProgress);

        return result;
    }

    /**
     * Определяет статус эпика
     *
     * @param epic        для обновления
     * @param statusCheck результат проверки подзадач
     */
    private void determineEpicStatus(Epic epic, StatusCheckResult statusCheck) {
        if (statusCheck.isHasInProgress()) {
            setEpicStatus(epic, StatusTask.IN_PROGRESS);
        } else if (statusCheck.isAllNew()) {
            setEpicStatus(epic, StatusTask.NEW);
        } else if (statusCheck.isAllDone()) {
            setEpicStatus(epic, StatusTask.DONE);
        } else {
            setEpicStatus(epic, StatusTask.IN_PROGRESS);
        }
    }

    protected void afterSubTaskCreation(SubTask subTask) {
    }

    protected void afterEpicCreation(Epic epic) {
    }

    protected void afterTaskCreation(Task task) {
    }
}