package service;

import core.Epic;
import core.StatusTask;
import core.SubTask;
import core.Task;
import exceptions.TaskValidator;
import managers.TaskManager;


import java.util.*;

/**
 * TaskManager
 * Управляет задачами, эпиками и подзадачами, поддерживает историю просмотров.
 */
public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private final TaskValidator validator = new TaskValidator();
    private int nextId = 1;

    /**
     * Создает задачу
     * @param task задача для создания (не может быть null)
     * @return id созданной задачи
     * @throws IllegalArgumentException если задача не прошла валидацию
     */
    @Override
    public int createTask(Task task) {
        validator.validateNotNull(task, "Задача");
        if (task.getId() == 0) {
            task.setId(generateId());
        }
        validator.validateTaskForCreation(task, tasks.values());
        tasks.put(task.getId(), task);
        return task.getId();
    }

    /**
     * Создает эпик
     * @param epic эпик для создания (не может быть null)
     * @return id созданного эпика
     * @throws IllegalArgumentException если эпик не прошел валидацию
     */
    @Override
    public int createEpic(Epic epic) {
        if (epic.getId() == 0) {
            epic.setId(generateId());
        }
        validator.validateForEpicCreation(epic);
        epics.put(epic.getId(), epic);
        return epic.getId();
    }

    /**
     * Создает подзадачу
     * @param subTask подзадача для создания (не может быть null)
     * @return id созданной подзадачи
     * @throws IllegalArgumentException если подзадача не прошла валидацию
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
     * @param id задачи
     * @return копию задачи или null если задача не найдена
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
     * @param id эпика
     * @return копию эпика или null если эпик не найден
     * @throws IllegalArgumentException если id не положительный
     */
    @Override
    public Epic getEpicById(int id) {
        validator.validatePositiveId(id);
        Epic epic = epics.get(id);
        historyManager.add(epic);
        return epic != null ? new Epic(epic) : null;
    }

    /**
     * Возвращает список подзадач для указанного эпика.
     * @param epicId id эпика
     * @return неизменяемый список подзадач
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
     * @param id подзадачи
     * @return копию подзадачи или null если подзадача не найдена
     * @throws IllegalArgumentException если id не положительный
     */
    @Override
    public SubTask getSubTaskById(int id) {
        validator.validatePositiveId(id);
        SubTask subTask = subTasks.get(id);
        historyManager.add(subTask);
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
     * @param task с обновленными данными
     * @throws IllegalArgumentException если задача не прошла валидацию
     */
    @Override
    public void updateTask(Task task) {
        validator.validateTaskForUpdate(task, tasks);
        tasks.put(task.getId(), new Task(task));
    }

    /**
     * Обновляет статус эпика
     * @param epicId id эпика
     */
    private void updateEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return;

        List<Integer> subtaskIds = epic.getSubTaskIds();
        if (subtaskIds.isEmpty()) {
            setEpicStatus(epic, StatusTask.NEW);
            return;
        }

        StatusCheckResult statusCheck = checkSubTasksStatuses(subtaskIds);
        determineEpicStatus(epic, statusCheck);
    }

    /**
     * Обновляет подзадачу
     * @param subTask с обновленными данными
     * @throws IllegalArgumentException если подзадача не прошла валидацию
     */
    @Override
    public void updateSubTask(SubTask subTask) {
        validator.validateSubTaskForUpdate(subTask, subTasks, epics);
        subTasks.put(subTask.getId(), subTask);
        updateEpicStatus(subTask.getEpicId());
    }

    /**
     * Удаляет все задачи.
     * Очищает историю.
     */
    @Override
    public void deleteAllTasks() {
        Set<Integer> taskIds = new HashSet<>(tasks.keySet());
        tasks.clear();
        for (Integer id : taskIds) {
            historyManager.remove(id);
        }
    }

    /**
     * Удаляет задачу по id
     * @param id задачи
     */
    @Override
    public void deleteTaskById(int id) {
        validator.validatePositiveId(id);
        tasks.remove(id);
        historyManager.remove(id);
    }

    /**
     * Удаляет все эпики.
     * Очищает историю для эпиков и подзадач
     */
    @Override
    public void deleteAllEpics() {
        Set<Integer> epicIds = new HashSet<>(epics.keySet());
        epics.clear();
        for (Integer id : epicIds) {
            historyManager.remove(id);
            deleteAllSubTasks();
        }
    }

    /**
     * Удаляет эпик по id
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
    }

    /**
     * Удаляет подзадачу по id
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
        }
    }

    /**
     * @return новый уникальный идентификатор
     */
    @Override
    public int generateId() {
        return nextId++;
    }

    /**
     * Устанавливает статусы эпика
     * @param epic для обновления
     * @param status новый статус
     */
    private void setEpicStatus(Epic epic, StatusTask status) {
        epic.setStatus(status);
    }

    /**
     * Проверяет статусы подзадач
     * @param subtaskIds список id подзадач
     * @return результат проверки
     */
    private StatusCheckResult checkSubTasksStatuses(List<Integer> subtaskIds) {
        StatusCheckResult result = new StatusCheckResult();

        for (int subtaskId : subtaskIds) {
            SubTask subTask = subTasks.get(subtaskId);
            if (subTask == null) continue;

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
     * @param epic для обновления
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
}