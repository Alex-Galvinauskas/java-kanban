package exceptions;

import core.Epic;
import core.SubTask;
import core.Task;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TaskManagerExceptionHandler {


    public void validateNonNull(Object obj, String entityName) {
        if (obj == null) {
            throw new IllegalArgumentException(entityName + " не может быть null");
        }
    }

    public void validateNonEmptyString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть null или пустым");
        }
    }

    public void validatePositiveId(int id, String entityName) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID " + entityName + " должен быть положительным");
        }
    }


    public void validateTaskForCreation(Task task, Collection<Task> existingTasks) {
        validateNonNull(task, "Задача");
        validateNonEmptyString(task.getName(), "");

        if (existingTasks.stream().anyMatch(t -> t.getName().equals(task.getName()))) {
            throw new IllegalArgumentException("Задача с таким именем уже существует");
        }
    }

    public void validateTaskForUpdate(Task task, Map<Integer, Task> tasks) {
        validateNonNull(task, "Задача");
        validatePositiveId(task.getId(), "задачи");
        if (!tasks.containsKey(task.getId())) {
            throw new IllegalArgumentException("Задача с ID " + task.getId() + " не существует");
        }
    }


    public void validateEpicForCreation(Epic epic) {
        validateNonNull(epic, "Эпик");
        validateNonEmptyString(epic.getName(), "Название эпика");
        validateNonEmptyString(epic.getDescription(), "Описание эпика");
    }

    public void validateEpicForCopy(Epic epic) {
        validateNonNull(epic, "Эпик");
        validatePositiveId(epic.getId(), "эпика");
        validateNonEmptyString(epic.getName(), "Название эпика");
        validateNonEmptyString(epic.getDescription(), "Описание эпика");
    }

    public void validateEpicExists(Epic epic, int epicId) {
        validateNonNull(epic, "Эпик");
        validatePositiveId(epicId, "эпика");
    }


    public void validateSubTaskForCreation(SubTask subTask, Map<Integer, Epic> epics) {
        validateNonNull(subTask, "Подзадача");
        validateNonEmptyString(subTask.getName(), "Название подзадачи");

        int epicId = subTask.getEpicId();
        validatePositiveId(epicId, "эпика");

        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
        }
    }

    public void validateSubTaskForUpdate(SubTask subTask,
                                         Map<Integer, SubTask> subTasks,
                                         Map<Integer, Epic> epics) {
        validateNonNull(subTask, "Подзадача");
        validatePositiveId(subTask.getId(), "подзадачи");

        if (!subTasks.containsKey(subTask.getId())) {
            throw new IllegalArgumentException("Подзадача с ID " + subTask.getId() + " не существует");
        }

        int epicId = subTask.getEpicId();
        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с ID " + epicId + " не найден");
        }
    }
    public void validateSubTaskIdForAdd(int subTaskId, List<Integer> existingSubTaskIds, int epicId) {
        validatePositiveId(subTaskId, "Подзадача");
        if (existingSubTaskIds.contains(subTaskId)) {
            throw new IllegalArgumentException(
                    "Подзадача с ID " + subTaskId + " уже существует в эпике " + epicId);
        }
    }

    public void validateSubTaskIdForRemove(int subTaskId, List<Integer> existingSubTaskIds, int epicId) {
        validatePositiveId(subTaskId, "Подзадача");
        if (!existingSubTaskIds.contains(subTaskId)) {
            throw new IllegalArgumentException(
                    "Подзадача с ID " + subTaskId + " не найдена в эпике " + epicId);
        }
    }

}