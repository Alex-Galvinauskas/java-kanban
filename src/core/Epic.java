package core;

import exceptions.TaskManagerExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Represents an Epic task that can contain multiple subtasks.
 * An Epic is a special type of Task that maintains a list of subtask IDs and provides methods
 * to manage them. It includes validation logic through an exception handler and ensures
 * thread-safe operations on subtask IDs.
 */
public final class Epic extends Task {
    private final List<Integer> subTaskIds;
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();

    /**
     * Constructs a new Epic task with the specified ID, name, and description.
     * Initializes the task with NEW status by default and validates the epic for creation.
     * Also initializes an empty list for storing subtask IDs.
     *
     * @param id          the unique identifier of the epic
     * @param name        the name of the epic
     * @param description the detailed description of the epic
     */
    public Epic(int id, String name, String description) {
        super(id, name,description, StatusTask.NEW);
        exceptionHandler.validateEpicForCreation(this);
        this.subTaskIds = new ArrayList<>();
    }

    /**
     * Creates a new Epic object by copying properties from another Epic.
     * Validates the source Epic for copying and ensures the new Epic has its own copy of subtask IDs.
     *
     * @param other the Epic object to copy from (must not be null)
     * @throws NullPointerException if the provided Epic is null
     */
    public Epic(Epic other) {
        super(Objects.requireNonNull(other, "Эпик не может быть null").getId(),
                other.getName(), other.getDescription(), other.getStatus());
        exceptionHandler.validateEpicForCopy(other);
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
    }

    /**
     * Gets an unmodifiable list of sub task IDs associated with this epic.
     *
     * @return an unmodifiable list of integer IDs representing the sub tasks
     */
    public List<Integer> getSubTaskIds() {
        return Collections.unmodifiableList(subTaskIds);
    }

    /**
     * Adds a subtask ID to this epic's list of subtasks.
     * Validates the subtask ID before adding to ensure it's not already present.
     *
     * @param subTaskId the ID of the subtask to add
     */
    public void addSubTaskId(int subTaskId) {
        exceptionHandler.validateSubTaskIdForAdd(subTaskId, subTaskIds, getId());
        if (!subTaskIds.contains(subTaskId)) {
            subTaskIds.add(subTaskId);
        }
    }

    /**
     * Removes a subTask ID from the list of subTask IDs associated with this Epic.
     * The method first validates the subTask ID using the exception handler before removal.
     *
     * @param subTaskId the ID of the subTask to be removed
     * @return true if the subTask ID was successfully removed, false otherwise
     */
    public boolean removeSubTaskId(int subTaskId) {
        exceptionHandler.validateSubTaskIdForRemove(subTaskId, subTaskIds, getId());
        return subTaskIds.remove(Integer.valueOf(subTaskId));
    }

    /**
     * Clears all subtask IDs associated with this epic.
     * This method removes all entries from the internal collection of sub task IDs.
     */
    public void clearSubTaskIds() {
        subTaskIds.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Epic epic)) return false;
        if (!super.equals(o)) return false;
        return subTaskIds.equals(epic.subTaskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subTaskIds);
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", subTaskIds=" + subTaskIds +
                '}';
    }
}