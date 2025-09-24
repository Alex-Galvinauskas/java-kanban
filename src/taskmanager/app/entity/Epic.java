package taskmanager.app.entity;

import taskmanager.app.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Epic extends Task {
    private final List<Integer> subTaskIds;
    private LocalDateTime endTime;
    private ValidationException validator;

    public Epic(int id, String name, String description) {
        super(id, name, description, StatusTask.NEW);
        this.validator = new ValidationException();
        validator.validateForEpicCreation(this);
        this.subTaskIds = new ArrayList<>();
        this.endTime = null;
    }

    public Epic(Epic other) {
        super(Objects.requireNonNull(other, "Эпик не может быть null").getId(),
                other.getName(),
                other.getDescription(),
                other.getStatus(),
                other.getDuration(),
                other.getStartTime());
        this.validator = new ValidationException();
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
        this.endTime = other.getEndTime();
    }

    public Epic(String name, String description) {
        super(name, description);
        this.validator = new ValidationException();
        this.subTaskIds = new ArrayList<>();
        this.setStatus(StatusTask.NEW);
        this.endTime = null;
    }

    private ValidationException getValidator() {
        if (validator == null) {
            validator = new ValidationException();
        }
        return validator;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Integer> getSubTaskIds() {
        return Collections.unmodifiableList(subTaskIds);
    }

    public void addSubTaskId(int subTaskId) {
        getValidator().validatePositiveId(subTaskId);
        if (!subTaskIds.contains(subTaskId)) {
            subTaskIds.add(subTaskId);
        }
    }

    public void removeSubTaskId(int subTaskId) {
        getValidator().validatePositiveId(subTaskId);
        subTaskIds.remove(Integer.valueOf(subTaskId));
    }

    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subTaskIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Epic epic)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(subTaskIds, epic.subTaskIds);
    }

    @Override
    public String toString() {
        return "Эпик: " +
                "Id: " + getId() +
                ", Имя:'" + getName() +
                ", Описание: " + getDescription() +
                ", Статус: " + getStatus() +
                ", Длительность: " + (getDuration() != null ? getDuration().toMinutes() + "мин" : "Не указана") +
                ", Время начала: " + (getStartTime() != null ? getStartTime() : "Не указано") +
                ", Время окончания: " + (getEndTime() != null ? getEndTime() : "Не указано") +
                ", Id подзадач: " + subTaskIds;
    }

    public void clearSubTaskIds() {
        subTaskIds.clear();
    }
}