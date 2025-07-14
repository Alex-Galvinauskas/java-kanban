package core;

import java.util.Objects;

/**
 * Represents a task with an identifier, name, description, and status.
 * Provides methods to access and modify task properties and implements standard
 * Java object methods (equals, hashCode, toString).
 */

public class Task {
    private int id;
    private final String name;
    private final String description;
    private StatusTask status;

    /**
     * Constructs a new Task with the specified id, name, description, and status.
     *
     * @param id          the unique identifier for the task
     * @param name        the name of the task
     * @param description the detailed description of the task
     * @param status      the current status of the task (e.g., NEW, IN_PROGRESS, DONE)
     */
    public Task(int id, String name, String description, StatusTask status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    /**
     * Constructs a new Task by copying all fields from another Task instance (copy constructor).
     *
     * @param other the Task instance to copy fields from
     */
    public Task(Task other) {
        this.id = other.id;
        this.name = other.name;
        this.description = other.description;
        this.status = other.status;
    }

    public String getName() {
        return name;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }


    public StatusTask getStatus() {
        return status;
    }

    public void setStatus(StatusTask status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Task task)) return false;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Core.model.Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                '}';
    }
}
