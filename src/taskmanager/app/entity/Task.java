package taskmanager.app.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private int id;
    private final String name;
    private final String description;
    private StatusTask status;
    private LocalDateTime startTime;
    private Duration duration;

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = StatusTask.NEW;
    }

    public Task(int id, String name, String description, StatusTask status,
                Duration duration, LocalDateTime startTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.startTime = startTime;
        this.duration = duration;
    }

    public Task(Task task) {
        this.id = task.id;
        this.name = task.name;
        this.description = task.description;
        this.status = task.status;
        this.startTime = task.startTime;
        this.duration = task.duration;
    }

    public Task(int id, String name, String description, StatusTask statusTask) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = statusTask;
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

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        if(startTime == null || duration == null) {
            return null;
        }
        return startTime.plus(duration);
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
                ", Статус: " + status +
                ", Длительность: " + (duration != null ? duration.toMinutes() : "Не указана") +
                ", Время начала: " + (startTime != null ? startTime : "Не указано") +
                ", Время окончания: " + getEndTime();
    }
}
