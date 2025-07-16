package service;

import core.Epic;
import core.SubTask;

import java.util.List;

public interface EpicInterface {
    int createEpic(Epic epic);
    List<Epic> getAllEpics();
    Epic getEpicById(int id);
    void deleteAllEpics();
    void deleteEpicById(int id);
    List<SubTask> getSubTasksByEpicId(int id);
}
