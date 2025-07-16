package service;

import core.SubTask;

import java.util.List;

public interface SubTaskInterface {
    int createSubTask(SubTask subTask);
    List<SubTask> getAllSubTasks();
    SubTask getSubTaskById(int id);
    void updateSubTask(SubTask subTask);
    void deleteAllSubTasks(int id);
    void deleteSubTaskById(int id);

}
