package service;

import core.Epic;
import core.StatusTask;
import core.SubTask;
import core.Task;
import exceptions.TaskManagerExceptionHandler;


import java.util.*;

/**
 * Manages tasks, epics, and subtasks, providing functionality for their creation, retrieval,
 * updating, and deletion. Maintains relationships between epics and their subtasks, and handles
 * status propagation from subtasks to their parent epics. Uses an exception handler for input
 * validation and maintains unique identifiers for all tasks.
 */

public class InMemoryTaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();
    private int nextId = 1;


    /**
     * Creates a new task with the given parameters and assigns it a unique ID.
     * Validates the task before creation and stores it in the task collection.
     *
     * @param task the task object to be created
     * @return the unique identifier assigned to the created task
     */

    public int createTask(Task task) {
        exceptionHandler.validateTaskForCreation(task, tasks.values());
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
        return id;
    }


    /**
     * Creates a new Epic task and adds it to the task manager.
     * Validates the epic before creation using the exception handler.
     * Generates a unique ID for the epic and stores it in the epics collection.
     *
     * @param epic the Epic object to be created and stored
     * @return the unique identifier assigned to the newly created epic
     */

    public int createEpic(Epic epic) {
        exceptionHandler.validateEpicForCreation(epic);
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
        return id;
    }

    /**
     * Creates a new subtask and adds it to the task manager.
     * Validates the subtask before creation, generates a unique ID for it,
     * adds it to the subtask collection, and updates the associated epic's status.
     *
     * @param subTask the subtask to be created
     * @return the ID of the newly created subtask
     */

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

    /**
     * Retrieves a list containing all tasks currently managed by the TaskManager.
     * The returned list is an unmodifiable copy of the internal task collection.
     *
     * @return an unmodifiable List containing all tasks
     */

    public List<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    /**
     * Retrieves a task by its unique identifier.
     * Returns a deep copy of the task if found, null otherwise.
     *
     * @param id the unique identifier of the task to retrieve
     * @return a deep copy of the task if found, null otherwise
     */

    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        return task != null ? new Task(task) : null;
    }

    /**
     * Returns a list containing all epics in the task manager.
     * The returned list is a defensive copy to prevent modification of the internal state.
     *
     * @return an unmodifiable list containing all epics
     */

    public List<Epic> getAllEpics() {
        return List.copyOf(epics.values());
    }

    /**
     * Retrieves an epic task by its unique identifier.
     * Returns a deep copy of the epic if found, or null if no epic exists with the given ID.
     *
     * @param id the unique identifier of the epic to retrieve
     * @return a deep copy of the {@link Epic} object if found, null otherwise
     */

    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        return epic != null ? new Epic(epic) : null;
    }

    /**
     * Retrieves all subtasks associated with a specific epic task.
     * Validates that the epic exists before retrieving subtasks.
     * Returns an unmodifiable list of subtasks cloned from the original objects.
     *
     * @param epicId the ID of the epic task to find subtasks for
     * @return an unmodifiable list of {@link SubTask} objects associated with the epic
     * @throws RuntimeException if the epic with specified ID doesn't exist
     */

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

    /**
     * Gets all subtasks.
     *
     * @return an unmodifiable list containing all subtasks
     */

    public List<SubTask> getAllSubTasks() {
        return List.copyOf(subTasks.values());
    }

    /**
     * Retrieves a subTask by its unique identifier.
     * Returns a deep copy of the subTask if found, otherwise returns null.
     *
     * @param id the unique identifier of the subTask to retrieve
     */

    public void getSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            new SubTask(subTask);
        }
    }


    /**
     * Updates the specified task in the task manager.
     * Validates the task before updating and replaces the existing task with a new instance.
     *
     * @param task the task to be updated
     */

    public void updateTask(Task task) {
        exceptionHandler.validateTaskForUpdate(task, tasks);
        tasks.put(task.getId(), new Task(task));
    }
    /**
     * Updates the status of an Epic task based on the statuses of its subtasks.
     * If the Epic has no subtasks, its status is set to NEW.
     * Otherwise, the status is determined by checking all subtasks' statuses.
     *
     * @param epicId the ID of the Epic task to update
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
     * Updates an existing subTask in the task manager.
     * Validates the subTask before updating and ensures the associated epic's status is updated.
     *
     * @param subTask the subTask to be updated
     */

    public void updateSubTask(SubTask subTask) {
        exceptionHandler.validateSubTaskForUpdate(subTask, subTasks, epics);

        subTasks.put(subTask.getId(), subTask);
        updateEpicStatus(subTask.getEpicId());
    }


    public void deleteAllTasks() {
        tasks.clear();
    }

    /**
     * Delete task by id.
     *
     * @param id the id
     */
    public void deleteTaskById(int id) {
        tasks.remove(id);
    }


    public void deleteAllEpics() {
        epics.clear();
        subTasks.clear();
    }

    /**
     * Deletes an epic task by its ID. All subtasks associated with the epic are also removed.
     * Validates that the epic exists before deletion using the exception handler.
     *
     * @param id the ID of the epic to be deleted
     * @throws RuntimeException if the epic with the specified ID does not exist (handled by exceptionHandler)
     */

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
            epic.clearSubTaskIds();
            updateEpicStatus(epic.getId());
        }
    }

    /**
     * Deletes a subtask with the specified ID from the task manager.
     * If the subtask exists, it is removed from the subtasks map and also from its associated epic's subtask list.
     * After removal, the status of the associated epic is updated.
     *
     * @param id the ID of the subtask to be deleted
     */

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

    /**
     * Generates and returns a unique identifier for tasks.
     * The identifier is auto-incremented each time this method is called.
     *
     * @return a unique integer identifier
     */

    public int generateId() {
        return nextId++;
    }

    private void setEpicStatus(Epic epic, StatusTask status) {
        epic.setStatus(status);
    }
    /**
     * Checks the statuses of subtasks in the given list and updates the result accordingly.
     * The method checks if any subtask is in progress, and if so, sets the corresponding flag in the result.
     * It also checks if all subtasks are either NEW or DONE and updates the flags accordingly.
     *
     * @param subtaskIds the list of subtask IDs to check
     * @return a {@link StatusCheckResult} object containing the status check results
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
     * Determines and sets the status of an Epic based on the provided status check result.
     * The status is set to IN_PROGRESS if there are any subtasks in progress.
     * If all subtasks are new, the status is set to NEW.
     * If all subtasks are done, the status is set to DONE.
     * Otherwise, defaults to setting the status to IN_PROGRESS.
     *
     * @param epic the Epic whose status needs to be determined
     * @param statusCheck the result of checking subtask statuses
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