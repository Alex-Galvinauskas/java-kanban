package taskmanager.app.management;

import taskmanager.app.entity.Epic;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;
import taskmanager.app.entity.TaskType;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface TaskManager {
    int createTask(Task task) throws IOException;

    int createEpic(Epic epic) throws IOException;

    int createSubTask(SubTask subTask) throws IOException;

    List<Task> getAllTasks();

    Task getTaskById(int id);

    List<Epic> getAllEpics();

    Epic getEpicById(int id);

    List<SubTask> getSubTasksByEpicId(int epicId);

    List<SubTask> getAllSubTasks();

    SubTask getSubTaskById(int id);

    default TaskType getType() {
        return null;
    }

    Collection<Task> getHistory();

    void updateTask(Task task) throws IOException;

    void updateSubTask(SubTask subTask) throws IOException;

    void deleteAllTasks() throws IOException;

    void deleteTaskById(int id) throws IOException;

    void deleteAllEpics() throws IOException;

    void deleteEpicById(int id) throws IOException;

    void deleteAllSubTasks() throws IOException;

    void deleteSubTaskById(int id) throws IOException;

    int generateId();
}
