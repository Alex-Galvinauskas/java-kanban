package core;

import exceptions.TaskValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public final class Epic extends Task {
    private final List<Integer> subTaskIds;
    private final TaskValidator validator = new TaskValidator();

    public Epic(int id, String name, String description) {
        super(id, name,description, StatusTask.NEW);
        validator.validateForEpicCreation(this);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(Epic other) {
        super(Objects.requireNonNull(other, "Эпик не может быть null").getId(),
                other.getName(), other.getDescription(), other.getStatus());
        this.subTaskIds = new ArrayList<>(other.subTaskIds);
    }

    public Epic(String description, String name) {
        super(description, name);
        this.subTaskIds = new ArrayList<>();
        this.setStatus(StatusTask.NEW);
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

    public void removeSubTaskId(int subTaskId) {
        validator.validatePositiveId(subTaskId);
        subTaskIds.remove(Integer.valueOf(subTaskId));
    }

    public void clearSubTaskIds() {
        subTaskIds.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Epic epic)) return false;
        if (!super.equals(o)) return false;
        return subTaskIds.equals(epic.subTaskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subTaskIds);
    }

    @Override
    public String toString() {
        return "Эпик: " +
                "Id: " + getId() +
                ", Имя:'" + getName() +
                ", Описание: " + getDescription() +
                ", Статус: " + getStatus() +
                ", Id подзадач: " + subTaskIds;
    }
}