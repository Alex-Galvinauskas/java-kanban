package taskmanager.app.entity;

import java.util.Objects;

public class Task {
    private final String name;
    private final String description;
    private int id;
    private StatusTask status;

    public Task(int id, String name, String description, StatusTask status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(Task task) {
        this.id = task.id;
        this.name = task.name;
        this.description = task.description;
        this.status = task.status;
    }

    public Task(String name, String description) {
        this.description = description;
        this.name = name;
        this.status = StatusTask.NEW;
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

    public TaskType getType() {
        return TaskType.TASK;
    }


    public StatusTask getStatus() {
        return status;
    }

    public void setStatus(StatusTask status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Task task)) return false;
        return id == task.id;
    }

    @Override
    public String toString() {
        return "Задача: " +
                "Id: " + id +
                ", Имя: " + name +
                ", Описание: " + description +
                ", Статус: " + status;
    }
}
