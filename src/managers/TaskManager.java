package managers;

import core.Epic;
import core.SubTask;
import core.Task;

import java.util.List;

public interface TaskManager {
    int createTask(Task task);

    int createEpic(Epic epic);

    int createSubTask(SubTask subTask);

    List<Task> getAllTasks();

    Task getTaskById(int id);

    List<Epic> getAllEpics();

    Epic getEpicById(int id);

    List<SubTask> getSubTasksByEpicId(int epicId);

    List<SubTask> getAllSubTasks();

    SubTask getSubTaskById(int id);

    void updateTask(Task task);

    void updateSubTask(SubTask subTask);

    void deleteAllTasks();

    void deleteTaskById(int id);

    void deleteAllEpics();

    void deleteEpicById(int id);

    void deleteAllSubTasks();

    void deleteSubTaskById(int id);

    int generateId();
}
