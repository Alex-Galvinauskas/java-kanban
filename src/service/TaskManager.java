package service;

import core.Epic;
import core.StatusTask;
import core.SubTask;
import core.Task;
import exceptions.TaskManagerExceptionHandler;


import java.util.*;

public class TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();
    private int nextId = 1;


    public int createTask(Task task) {
        exceptionHandler.validateTaskForCreation(task, tasks.values());
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
        return id;
    }


    public int createEpic(Epic epic) {
        exceptionHandler.validateEpicForCreation(epic);
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
        return id;
    }

    public int createSubTask(SubTask subTask) {
        exceptionHandler.validateSubTaskForCreation(subTask, epics);

        int id = generateId();
        subTask.setId(id);
        subTasks.put(id, subTask);

        Epic epic = epics.get(subTask.getEpicId());
        epic.addSubTaskId(id);
        updateEpicStatus(subTask.getEpicId());

        return id;
    }

    public List<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        return task != null ? new Task(task) : null;
    }

    public List<Epic> getAllEpics() {
        return List.copyOf(epics.values());
    }

    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        return epic != null ? new Epic(epic) : null;
    }

    public List<SubTask> getSubTasksByEpicId(int epicId) {
        Epic epic = epics.get(epicId);
        exceptionHandler.validateEpicExists(epic, epicId);

        List<SubTask> result = new ArrayList<>();
        for (int subtaskId : epic.getSubTaskIds()) {
            SubTask subTask = subTasks.get(subtaskId);
            if (subTask != null) {
                result.add(new SubTask(subTask));
            }
        }

        return Collections.unmodifiableList(result);
    }

    public List<SubTask> getAllSubTasks() {
        return List.copyOf(subTasks.values());
    }

    public SubTask getSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        return subTask != null ? new SubTask(subTask) : null;
    }

    public void updateTask(Task task) {
        exceptionHandler.validateTaskForUpdate(task, tasks);
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

    public void updateSubTask(SubTask subTask) {
        exceptionHandler.validateSubTaskForUpdate(subTask, subTasks, epics);

        subTasks.put(subTask.getId(), subTask);
        updateEpicStatus(subTask.getEpicId());
    }

    public void deleteAllTasks() {
        tasks.clear();
    }

    public void deleteTaskById(int id) {
        tasks.remove(id);
    }

    public void deleteAllEpics() {
        epics.clear();
        subTasks.clear();
    }

    public void deleteEpicById(int id) {
        Epic epic = epics.get(id);
        exceptionHandler.validateEpicExists(epic, id);

        for (int subtaskId : epic.getSubTaskIds()) {
            subTasks.remove(subtaskId);
        }
        epics.remove(id);
    }

    public void deleteAllSubTasks() {
        subTasks.clear();

        for (Epic epic : epics.values()) {
            epic.getSubTaskIds().clear();
            updateEpicStatus(epic.getId());
        }
    }

    public void deleteSubTaskById(int id) {
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