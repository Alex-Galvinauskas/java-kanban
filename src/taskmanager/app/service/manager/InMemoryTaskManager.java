package taskmanager.app.service.manager;

import taskmanager.app.entity.Epic;
import taskmanager.app.entity.StatusTask;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.exception.ValidationException;
import taskmanager.app.management.TaskManager;
import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.service.time.TimeManagerService;
import taskmanager.app.util.StatusCheckResult;

import java.time.Duration;
import java.time.LocalDateTime;
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
    final Set<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(
            Task::getStartTime,
            Comparator.nullsLast(Comparator.naturalOrder())
    ).thenComparing(Task::getId));
    private final TimeManagerService timeManager;

    public InMemoryTaskManager(){
        this.timeManager = new TimeManagerService();
    }

    /**
     * Возвращает задачи в порядке приоритета (по startTime)
     */
    public List<Task> getPrioritizedTasks() {
        return prioritizedTasks.stream()
                .sorted(Comparator.comparing(
                        Task::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(Task::getId))
                .collect(Collectors.toList());
    }

    /**
     * Обновляет время эпика на основе его подзадач
     */
    public void updateEpicTime(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return;
        }

        prioritizedTasks.remove(epic);

        if (epic.getSubTaskIds().isEmpty()) {
            epic.setStartTime(null);
            epic.setDuration(null);
            epic.setEndTime(null);
            return;
        }

        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        Duration totalDuration = Duration.ZERO;
        boolean hasTimeData = false;

        for (int subtaskId : epic.getSubTaskIds()) {
            SubTask subtask = subTasks.get(subtaskId);
            if (subtask != null && subtask.getStartTime() != null && subtask.getDuration() != null) {
                hasTimeData = true;

                if (earliestStart == null || subtask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subtask.getStartTime();
                }

                LocalDateTime subtaskEnd = subtask.getEndTime();
                if (latestEnd == null || subtaskEnd.isAfter(latestEnd)) {
                    latestEnd = subtaskEnd;
                }

                totalDuration = totalDuration.plus(subtask.getDuration());
            }
        }

        if (hasTimeData) {
            epic.setStartTime(earliestStart);
            epic.setDuration(totalDuration);
            epic.setEndTime(latestEnd);
            if (epic.getStartTime() != null) {
                prioritizedTasks.add(epic);
            }
        } else {
            epic.setStartTime(null);
            epic.setDuration(null);
            epic.setEndTime(null);
        }
    }

    /**
     * Проверяет пересечение задачи с другими задачами по времени
     */
    private void checkForTimeOverlaps(Task taskToCheck) {
        if (taskToCheck.getStartTime() == null || taskToCheck.getDuration() == null) {
            return;
        }

        for (Task existingTask : prioritizedTasks) {
            if (existingTask.getId() != taskToCheck.getId() &&
                    existingTask.getStartTime() != null &&
                    existingTask.getDuration() != null &&
                    isTasksOverlap(taskToCheck, existingTask)) {
                throw new RuntimeException("Задача '" + taskToCheck.getName() +
                        "' пересекается по времени с задачей '" + existingTask.getName() + "'");
            }
        }
    }

    /**
     * Проверяет пересечение двух задач по времени
     */
    public boolean isTasksOverlap(Task task1, Task task2) {
        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime start2 = task2.getStartTime();
        LocalDateTime end2 = task2.getEndTime();

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        return !start1.isEqual(end2) && !start2.isEqual(end1) &&
                start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void validateTaskTime(Task task) {
        timeManager.validateTaskTime(task);
        checkForTimeOverlaps(task);
    }

    private void addTaskToTimeSlots(Task task) {
        timeManager.addTaskToTimeSlots(task);
    }

    private void removeTaskFromTimeSlots(Task task) {
        timeManager.removeTaskFromTimeSlots(task);
    }

    /**
     * Восстанавливает задачу напрямю в карту задач.
     *
     * @param task задача для восстановления
     */
    protected void restoreTaskDirectly(Task task) {
        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
            addTaskToTimeSlots(task);
        }
    }

    /**
     * Восстанавливает эпик напрямую в карту эпиков.
     *
     * @param epic эпик для восстановления
     */
    protected void restoreEpicDirectly(Epic epic) {
        epics.put(epic.getId(), epic);
        if (epic.getStartTime() != null) {
            prioritizedTasks.add(epic);
        }
    }

    /**
     * Восстанавливает подзадачу напрямую в карту подзадач.
     *
     * @param subTask подзадача для восстановления
     */
    protected void restoreSubTaskDirectly(SubTask subTask) {
        subTasks.put(subTask.getId(), subTask);
        Epic epic = epics.get(subTask.getEpicId());
        if (epic != null) {
            epic.addSubTaskId(subTask.getId());
            updateEpicStatus(epic.getId());
            updateEpicTime(epic.getId());
        }
        if (subTask.getStartTime() != null) {
            prioritizedTasks.add(subTask);
            addTaskToTimeSlots(subTask);
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

        try {
            validateTaskTime(task);
            validator.validateTaskForCreation(task, tasks.values());
            tasks.put(task.getId(), task);
            if (task.getStartTime() != null) {
                prioritizedTasks.add(task);
                addTaskToTimeSlots(task);
            }
            afterTaskCreation(task);
            return task.getId();
        } catch (RuntimeException e) {
            if (tasks.containsKey(task.getId())) {
                tasks.remove(task.getId());
                prioritizedTasks.remove(task);
                removeTaskFromTimeSlots(task);
            }
            throw e;
        }
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

        try {
            validator.validateForEpicCreation(epic);
            epics.put(epic.getId(), epic);
            afterEpicCreation(epic);
            return epic.getId();
        } catch (RuntimeException e) {
            epics.remove(epic.getId());
            throw e;
        }
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

        try {
            validateTaskTime(subTask);
            validator.validateForSubTaskCreation(subTask, epics);

            subTasks.put(subTask.getId(), subTask);
            Epic epic = epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.addSubTaskId(subTask.getId());
                updateEpicStatus(subTask.getEpicId());
                updateEpicTime(subTask.getEpicId());
            }

            if (subTask.getStartTime() != null) {
                prioritizedTasks.add(subTask);
                addTaskToTimeSlots(subTask);
            }

            afterSubTaskCreation(subTask);
            return subTask.getId();
        } catch (RuntimeException e) {
            if (subTasks.containsKey(subTask.getId())) {
                subTasks.remove(subTask.getId());
                prioritizedTasks.remove(subTask);
                removeTaskFromTimeSlots(subTask);

                Epic epic = epics.get(subTask.getEpicId());
                if (epic != null) {
                    epic.removeSubTaskId(subTask.getId());
                    updateEpicStatus(epic.getId());
                    updateEpicTime(epic.getId());
                }
            }
            throw e;
        }
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
    public Optional<SubTask> getSubTaskById(int id) {
        validator.validatePositiveId(id);
        return Optional.ofNullable(subTasks.get(id))
                .map(subTask -> {
                    historyManager.add(subTask);
                    return new SubTask(subTask);
                });
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
        Task oldTask = tasks.get(task.getId());

        if (oldTask != null) {
            prioritizedTasks.remove(oldTask);
            removeTaskFromTimeSlots(oldTask);
        }

        try {
            validateTaskTime(task);
            tasks.put(task.getId(), task);
            if (task.getStartTime() != null) {
                prioritizedTasks.add(task);
                addTaskToTimeSlots(task);
            }
        } catch (Exception e) {
            if (oldTask != null) {
                tasks.put(oldTask.getId(), oldTask);
                if (oldTask.getStartTime() != null) {
                    prioritizedTasks.add(oldTask);
                    addTaskToTimeSlots(oldTask);
                }
            }
            throw new RuntimeException("Ошибка при обновлении задачи " + task.getId(), e);
        } finally {
            try {
                afterTaskUpdate(task);
            } catch (Exception e) {
                System.err.println("Ошибка в afterTaskUpdate для задачи " + task.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Обновляет подзадачу
     *
     * @param subTask с обновленными данными
     */
    @Override
    public void updateSubTask(SubTask subTask) {
        validator.validateSubTaskForUpdate(subTask, subTasks, epics);

        SubTask oldSubTask = subTasks.get(subTask.getId());
        Epic oldEpic = oldSubTask != null ? epics.get(oldSubTask.getEpicId()) : null;

        if (oldSubTask != null) {
            prioritizedTasks.remove(oldSubTask);
            removeTaskFromTimeSlots(oldSubTask);

            if (oldSubTask.getEpicId() != subTask.getEpicId() && oldEpic != null) {
                oldEpic.removeSubTaskId(oldSubTask.getId());
                updateEpicStatus(oldEpic.getId());
                updateEpicTime(oldEpic.getId());
            }
        }

        try {
            validateTaskTime(subTask);
            subTasks.put(subTask.getId(), subTask);

            Epic newEpic = epics.get(subTask.getEpicId());
            if (newEpic != null) {
                if (oldSubTask == null || oldSubTask.getEpicId() != subTask.getEpicId()) {
                    newEpic.addSubTaskId(subTask.getId());
                }
                updateEpicStatus(newEpic.getId());
                updateEpicTime(newEpic.getId());
            }

            if (subTask.getStartTime() != null) {
                prioritizedTasks.add(subTask);
                addTaskToTimeSlots(subTask);
            }
        } catch (Exception e) {
            if (oldSubTask != null) {
                subTasks.put(oldSubTask.getId(), oldSubTask);
                if (oldSubTask.getStartTime() != null) {
                    prioritizedTasks.add(oldSubTask);
                    addTaskToTimeSlots(oldSubTask);
                }

                if (oldEpic != null && (oldSubTask.getEpicId() != subTask.getEpicId())) {
                    oldEpic.addSubTaskId(oldSubTask.getId());
                    updateEpicStatus(oldEpic.getId());
                    updateEpicTime(oldEpic.getId());
                }
            }
            throw new RuntimeException("Ошибка при обновлении подзадачи " + subTask.getId(), e);
        } finally {
            try {
                afterSubTaskUpdate(subTask);
            } catch (Exception e) {
                System.err.println("Ошибка в afterSubTaskUpdate для подзадачи " + subTask.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Удаляет все задачи.
     * Очищает историю.
     */
    @Override
    public void deleteAllTasks() {
        try {
            tasks.values().forEach(task -> {
                try {
                    prioritizedTasks.remove(task);
                    removeTaskFromTimeSlots(task);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении задачи из приоритетных/временных слотов: " + e.getMessage());
                }
            });

            Set<Integer> taskIds = new HashSet<>(tasks.keySet());
            tasks.clear();
            taskIds.forEach(id -> {
                try {
                    historyManager.remove(id);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении задачи " + id + " из истории: " + e.getMessage());
                }
            });
        } finally {
            try {
                afterAllTasksDeletion();
            } catch (Exception e) {
                System.err.println("Ошибка в afterAllTasksDeletion: " + e.getMessage());
            }
        }
    }

    /**
     * Удаляет задачу по id
     *
     * @param id задачи
     */
    @Override
    public void deleteTaskById(int id) {
        try {
            validator.validatePositiveId(id);
            Task task = tasks.get(id);
            if (task != null) {
                try {
                    prioritizedTasks.remove(task);
                    removeTaskFromTimeSlots(task);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении задачи из приоритетных/временных слотов: " + e.getMessage());
                }

                tasks.remove(id);

                try {
                    historyManager.remove(id);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении задачи " + id + " из истории: " + e.getMessage());
                }
            }
        } finally {
            try {
                afterTaskDeletion(id);
            } catch (Exception e) {
                System.err.println("Ошибка в afterTaskDeletion: " + e.getMessage());
            }
        }
    }

    /**
     * Удаляет все эпики.
     * Очищает историю для эпиков и подзадач
     */
    public void deleteAllEpics() {
        try {
            deleteAllSubTasks();

            epics.values().forEach(epic -> {
                try {
                    if (epic.getStartTime() != null) {
                        prioritizedTasks.remove(epic);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении эпика из приоритетных задач: " + e.getMessage());
                }
            });

            Set<Integer> epicIds = new HashSet<>(epics.keySet());
            epics.clear();
            epicIds.forEach(id -> {
                try {
                    historyManager.remove(id);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении эпика " + id + " из истории: " + e.getMessage());
                }
            });
        } finally {
            try {
                afterAllEpicsDeletion();
            } catch (Exception e) {
                System.err.println("Ошибка в afterAllEpicsDeletion: " + e.getMessage());
            }
        }
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

        if (epic == null) {
            throw new IllegalArgumentException("Эпик с id " + id + " не существует");
        }

        List<Integer> subTaskIds = new ArrayList<>(epic.getSubTaskIds());
        boolean deletionFailed = false;

        try {
            for (int subtaskId : subTaskIds) {
                try {
                    deleteSubTaskById(subtaskId);
                } catch (Exception e) {
                    deletionFailed = true;
                    System.err.println("Ошибка при удалении подзадачи " + subtaskId + ": " + e.getMessage());
                }
            }

            try {
                if (epic.getStartTime() != null) {
                    prioritizedTasks.remove(epic);
                }
            } catch (Exception e) {
                System.err.println("Ошибка при удалении эпика из приоритетных задач: " + e.getMessage());
            }

            try {
                historyManager.remove(id);
            } catch (Exception e) {
                System.err.println("Ошибка при удалении эпика " + id + " из истории: " + e.getMessage());
            }

            epics.remove(id);

        } finally {
            try {
                afterEpicDeletion(id);
            } catch (Exception e) {
                System.err.println("Ошибка в afterEpicDeletion: " + e.getMessage());
            }
        }

        if (deletionFailed) {
            throw new RuntimeException("Не все подзадачи были удалены успешно");
        }
    }

    /**
     * Удаляет все подзадачи.
     * Обновляет статусы эпиков и очищает историю
     */
    @Override
    public void deleteAllSubTasks() {
        try {
            subTasks.values().forEach(subTask -> {
                try {
                    prioritizedTasks.remove(subTask);
                    removeTaskFromTimeSlots(subTask);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении подзадачи из приоритетных/временных слотов: " + e.getMessage());
                }
            });

            Set<Integer> subTaskIds = new HashSet<>(subTasks.keySet());
            subTasks.clear();

            for (Integer id : subTaskIds) {
                try {
                    historyManager.remove(id);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении подзадачи " + id + " из истории: " + e.getMessage());
                }
            }

            for (Epic epic : epics.values()) {
                try {
                    epic.clearSubTaskIds();
                    updateEpicStatus(epic.getId());
                    updateEpicTime(epic.getId());
                } catch (Exception e) {
                    System.err.println("Ошибка при обновлении эпика " + epic.getId() + ": " + e.getMessage());
                }
            }
        } finally {
            try {
                afterAllSubTasksDeletion();
            } catch (Exception e) {
                System.err.println("Ошибка в afterAllSubTasksDeletion: " + e.getMessage());
            }
        }
    }

    /**
     * Удаляет подзадачу по id
     *
     * @param id подзадачи
     */
    @Override
    public void deleteSubTaskById(int id) {
        try {
            validator.validatePositiveId(id);
            SubTask subTask = subTasks.get(id);
            if (subTask != null) {
                try {
                    prioritizedTasks.remove(subTask);
                    removeTaskFromTimeSlots(subTask);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении подзадачи из приоритетных/временных слотов: " + e.getMessage());
                }

                subTasks.remove(id);

                Epic epic = epics.get(subTask.getEpicId());
                if (epic != null) {
                    try {
                        epic.removeSubTaskId(id);
                        updateEpicStatus(epic.getId());
                        updateEpicTime(epic.getId());
                    } catch (Exception e) {
                        System.err.println("Ошибка при обновлении эпика " + epic.getId() + ": " + e.getMessage());
                    }
                }

                try {
                    historyManager.remove(id);
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении подзадачи " + id + " из истории: " + e.getMessage());
                }
            }
        } finally {
            try {
                afterSubTaskDeletion(id);
            } catch (Exception e) {
                System.err.println("Ошибка в afterSubTaskDeletion: " + e.getMessage());
            }
        }
    }

    /**
     * @return новый уникальный идентификатор
     */
    @Override
    public int generateId() {
        try {
            return nextId.getAndIncrement();
        } catch (Exception e) {
            System.err.println("Ошибка при генерации ID: " + e.getMessage());
            throw new RuntimeException("Не удалось сгенерировать ID", e);
        }
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