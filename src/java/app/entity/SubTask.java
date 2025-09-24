package java.app.entity;

import java.app.exception.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class SubTask extends Task {
    private final int epicId;


    public SubTask(int id, String name, String description, StatusTask status, int epicId) {
        super(id, name, description, status);
        ValidationException validator = new ValidationException();
        validator.validateNotNull(id, name);
        this.epicId = epicId;
    }

    public SubTask(int id, String name, String description, StatusTask status,
                   Duration duration, LocalDateTime startTime, int epicId) {
        super(id, name, description, status, duration, startTime);
        ValidationException validator = new ValidationException();
        validator.validateNotNull(id, name);
        this.epicId = epicId;
    }

    public SubTask(SubTask other) {
        super(Objects.requireNonNull(other, "Объект SubTask не может быть null"));
        this.epicId = other.epicId;
    }

    public SubTask(String name, String description, int epicId) {
        super(name, description);
        this.epicId = epicId;
        this.setStatus(StatusTask.NEW);
    }

    public int getEpicId() {
        return epicId;
    }

    public TaskType getType() {
        return TaskType.SUBTASK;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), epicId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SubTask subTask)) return false;
        if (!super.equals(obj)) return false;
        return epicId == subTask.epicId;
    }

    @Override
    public String toString() {
        return "Подзадача: " +
                "Id: " + getId() +
                ", Имя: " + getName() +
                ", Описание: " + getDescription() +
                ", Id эпика: " + epicId +
                ", Статус: " + getStatus();
    }
}
