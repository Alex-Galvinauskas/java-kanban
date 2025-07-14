package core;

import exceptions.TaskManagerExceptionHandler;

import java.util.Objects;

/**
 * A subtask that is part of an epic in a task management system.
 * Extends the base Task class and includes a reference to its parent epic.
 * Provides validation and exception handling through TaskManagerExceptionHandler.
 *
 * @see Task
 */

public class SubTask extends Task {
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();
    private final int epicId;


    /**
     * Constructs a new SubTask with the specified id, name, description, status and epic id.
     * Validates that id and name are not null.
     *
     * @param id          the unique identifier of the subtask
     * @param name        the name of the subtask
     * @param description the description of the subtask
     * @param status      the current status of the subtask
     * @param epicId      the id of the epic this subtask belongs to
     */

    public SubTask(int id, String name, String description, StatusTask status, int epicId) {
        super(id, name, description, status);
        exceptionHandler.validateNonNull(id, name);
        this.epicId = epicId;
    }

    /**
     * Creates a new SubTask instance by copying the fields from another SubTask.
     * The constructor performs a deep copy of all fields except epicId which is directly assigned.
     *
     * @param other the SubTask instance to copy from (must not be null)
     * @throws NullPointerException if the other SubTask is null
     */
    public SubTask(SubTask other) {
        super(Objects.requireNonNull(other, "Объект SubTask не может быть null"));
        this.epicId = other.epicId;
    }


    public int getEpicId() {
        return epicId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SubTask subTask)) return false;
        if (!super.equals(obj)) return false;
        return epicId == subTask.epicId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), epicId);
    }

    @Override
    public String toString() {
        return "Core.model.SubTask{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", epicId=" + epicId +
                ", status=" + getStatus() +
                '}';
    }
}
