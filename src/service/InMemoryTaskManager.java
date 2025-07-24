package service;

import core.Epic;
import core.StatusTask;
import core.SubTask;
import core.Task;
import exceptions.TaskValidator;
import managers.TaskManagerInterface;


import java.util.*;

public class InMemoryTaskManager implements TaskManagerInterface {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private final TaskValidator validator = new TaskValidator();
    private int nextId = 1;


    @Override
    public int createTask(Task task) {
        validator.validateTaskForCreation(task, tasks.values());
        if (task.getId() == 0) {
            task.setId(generateId());
        }
        tasks.put(task.getId(), task);
        return task.getId();
    }

    @Override
    public int createEpic(Epic epic) {
        validator.validateForEpicCreation(epic);
        if(epic.getId() == 0) {
        epic.setId(generateId());
        }
        epics.put(epic.getId(), epic);
        return epic.getId();
    }

    @Override
    public int createSubTask(SubTask subTask) {
        if (subTask == null) {
            throw new IllegalArgumentException("Подзадача не может быть null");
        }
        if (subTask.getId() == subTask.getEpicId()) {
            throw new IllegalArgumentException("Подзадача не может быть своим же эпиком");
        }

        if(subTask.getId() == 0) {
        subTask.setId(generateId());
        }
        validator.validateForSubTaskCreation(subTask, epics);

        subTasks.put(subTask.getId(), subTask);
        Epic epic = epics.get(subTask.getEpicId());
        epic.addSubTaskId(subTask.getId());
        updateEpicStatus(subTask.getEpicId());

        return subTask.getId();
    }

    @Override
    public List<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    @Override
    public Task getTaskById(int id) {
        validator.validatePositiveId(id);
        Task task = tasks.get(id);
        historyManager.add(task);
        return task != null ? new Task(task) : null;
    }

    @Override
    public List<Epic> getAllEpics() {
        return List.copyOf(epics.values());
    }

    @Override
    public Epic getEpicById(int id) {
        validator.validatePositiveId(id);
        Epic epic = epics.get(id);
        historyManager.add(epic);
        return epic != null ? new Epic(epic) : null;
    }

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

        return Collections.unmodifiableList(result);
    }

    @Override
    public List<SubTask> getAllSubTasks() {
        return List.copyOf(subTasks.values());
    }

    @Override
    public SubTask getSubTaskById(int id) {
        validator.validatePositiveId(id);
        SubTask subTask = subTasks.get(id);
        historyManager.add(subTask);
        return subTask != null ? new SubTask(subTask) : null;
    }

    public Collection<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public void updateTask(Task task) {
        validator.validateTaskForUpdate(task, tasks);
        tasks.put(task.getId(), new Task(task));
    }

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

    @Override
    public void updateSubTask(SubTask subTask) {
        validator.validateSubTaskForUpdate(subTask, subTasks, epics);
        subTasks.put(subTask.getId(), subTask);
        updateEpicStatus(subTask.getEpicId());
    }

    @Override
    public void deleteAllTasks() {
        Set<Integer> taskIds = new HashSet<>(tasks.keySet());
        tasks.clear();
        for (Integer id : taskIds) {
            historyManager.removeIfExists(id);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Set<Integer> taskIds = new HashSet<>(tasks.keySet());
        validator.validatePositiveId(id);
        tasks.remove(id);
        for (Integer taskId : taskIds) {
            historyManager.removeIfExists(taskId);
        }
    }

    @Override
    public void deleteAllEpics() {
        Set<Integer> epicIds = new HashSet<>(epics.keySet());
        epics.clear();
        for (Integer id : epicIds) {
            historyManager.removeIfExists(id);
            deleteAllSubTasks();
        }
    }

    @Override
    public void deleteEpicById(int id) {
        validator.validatePositiveId(id);
        Epic epic = epics.get(id);
        validator.validateEpicExist(epics, id);

        for (int subtaskId : epic.getSubTaskIds()) {
            subTasks.remove(subtaskId);
        }
        epics.remove(id);
    }

    @Override
    public void deleteAllSubTasks() {
        Set<Integer> subTaskIds = new HashSet<>(subTasks.keySet());

        subTasks.clear();

        for (Integer id : subTaskIds) {
            historyManager.removeIfExists(id);
        }

        for (Epic epic : epics.values()) {
            epic.clearSubTaskIds();
            updateEpicStatus(epic.getId());
        }
    }

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
        }
    }

    @Override
    public int generateId() {
        return nextId++;
    }

    private void setEpicStatus(Epic epic, StatusTask status) {
        epic.setStatus(status);
    }

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