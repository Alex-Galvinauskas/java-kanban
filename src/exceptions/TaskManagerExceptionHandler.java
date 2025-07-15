package exceptions;

import core.Epic;
import core.SubTask;
import core.Task;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * A utility class for validating various Task Manager entities and operations.
 * Provides validation methods for tasks, epics, and subtasks to ensure they meet required constraints
 * before operations like creation, update, or deletion.
 */
public class TaskManagerExceptionHandler {


    /**
     * Validates that the specified object is not null.
     * Throws an {@link IllegalArgumentException} with a descriptive message if the object is null.
     *
     * @param obj the object to be validated
     * @param entityName the name of the entity to include in the exception message
     * @throws IllegalArgumentException if the specified object is null
     */
    public void validateNonNull(Object obj, String entityName) {
        if (obj == null) {
            throw new IllegalArgumentException(entityName + " не может быть null");
        }
    }

    /**
     * Validates that a string is not null or empty (after trimming).
     * Throws an IllegalArgumentException with a descriptive message if validation fails.
     *
     * @param value the string value to validate
     * @param fieldName the name of the field being validated (used in error message)
     * @throws IllegalArgumentException if the value is null or empty after trimming
     */
    public void validateNonEmptyString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть null или пустым");
        }
    }

    /**
     * Validates that the given ID is a positive number.
     * Throws an IllegalArgumentException if the ID is zero or negative.
     *
     * @param id the ID to validate
     * @param entityName the name of the entity associated with the ID (used in error message)
     * @throws IllegalArgumentException if the ID is not positive
     */
    public void validatePositiveId(int id, String entityName) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID " + entityName + " должен быть положительным");
        }
    }


    /**
     * Validates a task for creation by checking if it is non-null, has a non-empty name,
     * and does not duplicate an existing task's name in the provided collection.
     *
     * @param task the task to validate
     * @param existingTasks the collection of existing tasks to check for duplicates
     * @throws IllegalArgumentException if the task is null, has an empty name, or duplicates an existing task's name
     */

    public void validateTaskForCreation(Task task, Collection<Task> existingTasks) {
        validateNonNull(task, "Задача");
        validateNonEmptyString(task.getName(), "");

        if (existingTasks.stream().anyMatch(t -> t.getName().equals(task.getName()))) {
            throw new IllegalArgumentException("Задача с таким именем уже существует");
        }
    }

    /**
     * Validates a task before updating it.
     * Checks if the task is non-null, has a positive ID, and exists in the provided task map.
     * Throws an IllegalArgumentException if any of the validations fail.
     *
     * @param task the task to validate
     * @param tasks the map of existing tasks to check against
     * @throws IllegalArgumentException if the task is null, has non-positive ID, or doesn't exist in the map
     */
    public void validateTaskForUpdate(Task task, Map<Integer, Task> tasks) {
        validateNonNull(task, "Задача");
        validatePositiveId(task.getId(), "задачи");
        if (!tasks.containsKey(task.getId())) {
            throw new IllegalArgumentException("Задача с ID " + task.getId() + " не существует");
        }
    }


    /**
     * Validates an epic for creation by checking that it is not null and that its name and description are not empty.
     * Throws an exception if any of the validations fail.
     *
     * @param epic the epic to validate
     * @throws IllegalArgumentException if the epic is null, or if its name or description is empty
     */

    public void validateEpicForCreation(Epic epic) {
        validateNonNull(epic, "Эпик");
        validateNonEmptyString(epic.getName(), "Название эпика");
        validateNonEmptyString(epic.getDescription(), "Описание эпика");
    }

    /**
     * Validates an Epic object for copying by checking for non-null, positive ID, and non-empty name and description.
     * Throws appropriate exceptions if any validation fails.
     *
     * @param epic the Epic object to validate
     * @throws NullPointerException if the epic is null
     * @throws IllegalArgumentException if the epic's ID is not positive or if name/description are empty
     */

    public void validateEpicForCopy(Epic epic) {
        validateNonNull(epic, "Эпик");
        validatePositiveId(epic.getId(), "эпика");
        validateNonEmptyString(epic.getName(), "Название эпика");
        validateNonEmptyString(epic.getDescription(), "Описание эпика");
    }

    /**
     * Validates that an epic exists by checking if it is non-null and has a positive ID.
     * Throws an exception if the epic is null or if the ID is not positive.
     *
     * @param epic the epic object to validate
     * @param epicId the ID of the epic to validate
     * @throws IllegalArgumentException if the epic is null or the ID is not positive
     */

    public void validateEpicExists(Epic epic, int epicId) {
        validateNonNull(epic, "Эпик");
        validatePositiveId(epicId, "эпика");
    }


    /**
     * Validates a subtask for creation by checking if it is non-null, has a non-empty name,
     * and if the associated epic exists in the provided map of epics.
     * Throws an IllegalArgumentException if any validation fails.
     *
     * @param subTask the subtask to validate
     * @param epics   the map of existing epics to validate against
     * @throws IllegalArgumentException if the subtask is null, has an empty name,
     *                                  has an invalid epic ID, or the epic doesn't exist
     */

    public void validateSubTaskForCreation(SubTask subTask, Map<Integer, Epic> epics) {
        validateNonNull(subTask, "Подзадача");
        validateNonEmptyString(subTask.getName(), "Название подзадачи");

        int epicId = subTask.getEpicId();
        validatePositiveId(epicId, "эпика");

        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
        }
    }

    /**
     * Validates a subtask before updating it in the task manager.
     * Checks if the subtask and its ID are valid, if the subtask exists in the provided map,
     * and if its associated epic exists in the provided epic map.
     *
     * @param subTask  the subtask to validate
     * @param subTasks map of existing subtasks
     * @param epics    map of existing epics
     * @throws IllegalArgumentException if the subtask is invalid, doesn't exist, or its epic doesn't exist
     */

    public void validateSubTaskForUpdate(SubTask subTask,
                                         Map<Integer, SubTask> subTasks,
                                         Map<Integer, Epic> epics) {
        validateNonNull(subTask, "Подзадача");
        validatePositiveId(subTask.getId(), "подзадачи");

        if (!subTasks.containsKey(subTask.getId())) {
            throw new IllegalArgumentException("Подзадача с ID " + subTask.getId() + " не существует");
        }

        int epicId = subTask.getEpicId();
        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
        }
    }

    /**
     * Validates a sub-task ID for addition to an epic.
     * Checks if the ID is positive and ensures it doesn't already exist in the list of existing sub-task IDs.
     * Throws an IllegalArgumentException if the ID is invalid or already exists.
     *
     * @param subTaskId the ID of the sub-task to validate
     * @param existingSubTaskIds list of existing sub-task IDs in the epic
     * @param epicId the ID of the epic to which the sub-task belongs
     * @throws IllegalArgumentException if the sub-task ID is invalid or already exists
     */

    public void validateSubTaskIdForAdd(int subTaskId, List<Integer> existingSubTaskIds, int epicId) {
        validatePositiveId(subTaskId, "Подзадача");
        if (existingSubTaskIds.contains(subTaskId)) {
            throw new IllegalArgumentException(
                    "Подзадача с ID " + subTaskId + " уже существует в эпике " + epicId);
        }
    }

    /**
     * Validates the subTask ID for removal by checking if it is a positive ID and if it exists in the list of existing subTask IDs.
     * Throws an {@link IllegalArgumentException} if the subTask ID is not found in the epic.
     *
     * @param subTaskId the ID of the subTask to validate
     * @param existingSubTaskIds the list of existing subTask IDs
     * @param epicId the ID of the epic to which the subTask belongs
     * @throws IllegalArgumentException if the subTask ID is not found in the epic
     */

    public void validateSubTaskIdForRemove(int subTaskId, List<Integer> existingSubTaskIds, int epicId) {
        validatePositiveId(subTaskId, "Подзадача");
        if (!existingSubTaskIds.contains(subTaskId)) {
            throw new IllegalArgumentException(
                    "Подзадача с ID " + subTaskId + " не найдена в эпике " + epicId);
        }
    }
}