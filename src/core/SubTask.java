package core;

import exceptions.TaskManagerExceptionHandler;

import java.util.Objects;

public class SubTask extends Task {
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();
    private int epicId;


    public SubTask(int id, String name, String description, StatusTask status, int epicId) {
        super(id, name, description, status);
        exceptionHandler.validateNonNull(id, name);
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
        if (!(obj instanceof SubTask)) return false;
        if (!super.equals(obj)) return false;
        SubTask subTask = (SubTask) obj;
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
                ", status=" + getStatus() +
                ", description='" + getDescription() + '\'' +
                ", epicId=" + epicId +
                '}';
    }
}
