package taskmanager.app.service.manager;

import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.exception.ValidationException;
import taskmanager.app.management.TaskManager;
import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.util.StatusCheckResult;

import java.io.IOException;
import java.util.*;

/**
 * TaskManager
 * Управляет задачами, эпиками и подзадачами, поддерживает историю просмотров.
 */
public class InMemoryTaskManager implements TaskManager {

    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private final ValidationException validator = new ValidationException();
    private int nextId = 1;

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
     *
     * @throws IllegalArgumentException если задача не прошла валидацию
     */
    @Override
    public int createTask(Task task) throws IOException {
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
     *
     * @throws IllegalArgumentException если эпик не прошел валидацию
     */
    @Override
    public int createEpic(Epic epic) throws IOException {
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
     *
     * @throws IllegalArgumentException если подзадача не прошла валидацию
     */
    @Override
    public int createSubTask(SubTask subTask) throws IOException {
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
    public Task getTaskById(int id) {
        validator.validatePositiveId(id);
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return new Task(task);
        }
        return null;
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
    public Epic getEpicById(int id) {
        validator.validatePositiveId(id);
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic != null ? new Epic(epic) : null;
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
        Epic epic = epics.get(epicId);
        validator.validateEpicExist(epics, epicId);

        List<SubTask> result = new ArrayList<>();
        for (int subtaskId : epic.getSubTaskIds()) {
            SubTask subTask = subTasks.get(subtaskId);
            if (subTask != null) {
                result.add(new SubTask(subTask));
            }
        }
        return List.copyOf(result);
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
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.add(subTask);
        }
        return subTask != null ? new SubTask(subTask) : null;
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
     *
     * @throws IllegalArgumentException если задача не прошла валидацию
     */
    @Override
    public void updateTask(Task task) throws IOException {
        validator.validateTaskForUpdate(task, tasks);
        tasks.put(task.getId(), new Task(task));
        afterTaskUpdate(task);
    }

    /**
     * Обновляет подзадачу
     *
     * @param subTask с обновленными данными
     *
     * @throws IllegalArgumentException если подзадача не прошла валидацию
     */
    @Override
    public void updateSubTask(SubTask subTask) throws IOException {
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
    public void deleteAllTasks() throws IOException {
        Set<Integer> taskIds = new HashSet<>(tasks.keySet());
        tasks.clear();
        for (Integer id : taskIds) {
            historyManager.remove(id);
        }
        afterAllTasksDeletion();
    }

    /**
     * Удаляет задачу по id
     *
     * @param id задачи
     */
    @Override
    public void deleteTaskById(int id) throws IOException {
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
    public void deleteAllEpics() throws IOException {
        Set<Integer> epicIds = new HashSet<>(epics.keySet());
        epics.clear();
        for (Integer id : epicIds) {
            historyManager.remove(id);
            deleteAllSubTasks();
        }
        afterAllEpicsDeletion();
    }

    /**
     * Удаляет эпик по id
     *
     * @param id эпика
     */
    @Override
    public void deleteEpicById(int id) throws IOException {
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
    public void deleteAllSubTasks() throws IOException {
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
    public void deleteSubTaskById(int id) throws IOException {
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
        return nextId++;
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
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return;
        }

        List<Integer> subtaskIds = epic.getSubTaskIds();
        if (subtaskIds.isEmpty()) {
            setEpicStatus(epic, StatusTask.NEW);
            return;
        }

        StatusCheckResult statusCheck = checkSubTasksStatuses(subtaskIds);
        determineEpicStatus(epic, statusCheck);
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
        StatusCheckResult result = new StatusCheckResult();

        for (int subtaskId : subtaskIds) {
            SubTask subTask = subTasks.get(subtaskId);
            if (subTask == null) {
                continue;
            }

            StatusTask status = subTask.getStatus();
            if (status == StatusTask.IN_PROGRESS) {
                result.setHasInProgress(true);
                return result;
            }
            result.setAllNew(result.isAllNew() && (status == StatusTask.NEW));
            result.setAllDone(result.isAllDone() && (status == StatusTask.DONE));
        }

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