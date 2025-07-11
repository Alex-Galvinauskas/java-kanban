package core;

import exceptions.TaskManagerExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public final class Epic extends Task {
    private final List<Integer> subTaskIds;
    private final TaskManagerExceptionHandler exceptionHandler = new TaskManagerExceptionHandler();

    public Epic(int id, String name, String description) {
        super(id, name,description, StatusTask.NEW);
        exceptionHandler.validateEpicForCreation(this);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(Epic other) {
        super(Objects.requireNonNull(other, "Эпик не может быть null").getId(),
                other.getName(), other.getDescription(), other.getStatus());
        exceptionHandler.validateEpicForCopy(other);
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
    }

    public List<Integer> getSubTaskIds() {
        return Collections.unmodifiableList(subTaskIds);
    }

    public void addSubTaskId(int subTaskId) {
        exceptionHandler.validateSubTaskIdForAdd(subTaskId, subTaskIds, getId());
        if (!subTaskIds.contains(subTaskId)) {
            subTaskIds.add(subTaskId);
        }
    }

    public boolean removeSubTaskId(int subTaskId) {
        exceptionHandler.validateSubTaskIdForRemove(subTaskId, subTaskIds, getId());
        return subTaskIds.remove(Integer.valueOf(subTaskId));
    }

    public void clearSubTaskIds() {
        subTaskIds.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Epic)) return false;
        if (!super.equals(o)) return false;
        Epic epic = (Epic) o;
        return subTaskIds.equals(epic.subTaskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subTaskIds);
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", subTaskIds=" + subTaskIds +
                '}';
    }
}