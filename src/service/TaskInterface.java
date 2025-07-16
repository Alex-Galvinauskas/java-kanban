package service;

import core.Task;

import java.util.List;


public interface TaskInterface {
    int createTask(Task task);
    List<Task> getAllTasks();
    Task getTaskById(int id);
    void updateTask(Task task);
    void deleteAllTask(int id);
    void deleteTaskById(int id);
}
