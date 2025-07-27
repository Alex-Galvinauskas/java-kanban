package core;

import exceptions.TaskValidator;

import java.util.Objects;

public class SubTask extends Task {
    private final int epicId;


    public SubTask(int id, String name, String description, StatusTask status, int epicId) {
        super(id, name, description, status);
        TaskValidator validator = new TaskValidator();
        validator.validateNotNull(id, name);
        this.epicId = epicId;
    }

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
        return "Подзадача: " +
                "Id: " + getId() +
                ", Имя: " + getName() +
                ", Описание: " + getDescription() +
                ", Id эпика: " + epicId +
                ", Статус: " + getStatus();
    }
}
