package core;

import exceptions.TaskManagerExceptionHandler;

import java.util.Objects;

public class Task {
    private int id;
    private final String name;
    private final String description;
    private StatusTask status;

    public Task(int id, String name, String description, StatusTask status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

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
