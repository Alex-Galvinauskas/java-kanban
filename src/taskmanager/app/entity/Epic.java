package taskmanager.app.entity;

import taskmanager.app.exception.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Epic extends Task {
    private final List<Integer> subTaskIds;
    private LocalDateTime endTime;
    private final ValidationException validator = new ValidationException();
    private LocalDateTime startTime;
    private Duration duration;

    public Epic(int id, String name, String description) {
        super(id, name, description, StatusTask.NEW);
        validator.validateForEpicCreation(this);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(Epic other) {
        super(Objects.requireNonNull(other, "Эпик не может быть null").getId(),
                other.getName(),
                other.getDescription(),
                other.getStatus(),
                other.getDuration(),
                other.getStartTime());
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
        this.endTime = other.endTime;
        this.startTime = other.startTime;
        this.duration = other.duration;
    }

    public Epic(String name, String description) {
        super(name, description);
        this.subTaskIds = new ArrayList<>();
        this.setStatus(StatusTask.NEW);
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Integer> getSubTaskIds() {
        return Collections.unmodifiableList(subTaskIds);
    }

    public void addSubTaskId(int subTaskId) {
        validator.validatePositiveId(subTaskId);
        if (!subTaskIds.contains(subTaskId)) {
            subTaskIds.add(subTaskId);
        }
    }

    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subTaskIds, startTime, duration, endTime);
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
                ", Длительность: " + (duration != null ? duration.toMinutes() + "мин" : "Не указана") +
                ", Время начала: " + (startTime != null ? startTime : "Не указано") +
                ", Время окончания: " + (endTime != null ? endTime : "Не указано") +
                ", Id подзадач: " + subTaskIds;
    }

    public void removeSubTaskId(int subTaskId) {
        validator.validatePositiveId(subTaskId);
        subTaskIds.remove(Integer.valueOf(subTaskId));
    }

    public void clearSubTaskIds() {
        subTaskIds.clear();
    }
}