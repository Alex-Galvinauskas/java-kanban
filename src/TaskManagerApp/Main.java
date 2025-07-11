package TaskManagerApp;

import core.Epic;
import core.StatusTask;
import core.SubTask;
import core.Task;
import service.TaskManager;

public class Main {

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        Task task1 = new Task(manager.generateId(),"Задача 1", "Описание задачи",
                StatusTask.NEW);
        int taskId1 = manager.createTask(task1);
        System.out.println("Создана задача с ID: " + taskId1);
        Task savedTask1 = manager.getTaskById(taskId1);
        System.out.println("Статус задачи: " + savedTask1.getStatus());
        task1.setStatus(StatusTask.IN_PROGRESS);
        manager.updateTask(task1);
        System.out.println("Статус задачи после изменения " + task1.getStatus());

        Task task2 = new Task(manager.generateId(), "Задача 2", "Описание задачи",
                StatusTask.IN_PROGRESS);
        int taskId2 = manager.createTask(task2);
        System.out.println("Создана задача с ID: " + taskId2);
        Task savedTask2 = manager.getTaskById(taskId2);
        System.out.println("Статус задачи: " + savedTask2.getStatus());
        manager.deleteTaskById(taskId2);


        Epic epic = new Epic(manager.generateId(), "Эпик 1", "Описание эпика");
        int epicId = manager.createEpic(epic);
        System.out.println("Создан эпик с ID: " + epicId);

        SubTask subTask1 = new SubTask(epicId, "Подзадача 1", "Описание 1", StatusTask.NEW, epicId);
        SubTask subTask2 = new SubTask(epicId, "Подзадача 2", "Описание 2", StatusTask.NEW, epicId);

        int subTaskId1 = manager.createSubTask(subTask1);
        int subTaskId2 = manager.createSubTask(subTask2);
        System.out.println("Созданы подзадачи с ID: " + subTaskId1 + ", " + subTaskId2);

        int getSubTaskId = subTask1.getId();
        manager.getSubTaskById(getSubTaskId);
        System.out.println("ID подзадачи " + getSubTaskId);

        Epic savedEpic = manager.getEpicById(epicId);
        System.out.println("Статус эпика: " + savedEpic.getStatus());


        subTask1.setStatus(StatusTask.DONE);
        manager.updateSubTask(subTask1);


        savedEpic = manager.getEpicById(epicId);
        System.out.println("Статус эпика после изменения: " + savedEpic.getStatus());


        subTask2.setStatus(StatusTask.DONE);
        manager.updateSubTask(subTask2);


        savedEpic = manager.getEpicById(epicId);
        System.out.println("Статус эпика после всех изменений: " + savedEpic.getStatus());

    Epic epic1 = new Epic(manager.generateId(), "Второй эпик", "Описание эпика");
    int epicId1 = manager.createEpic(epic);
    System.out.println("Создан эпик с ID: " + epicId1);

        SubTask subTask3 = new SubTask(manager.generateId(), "Подзадача 3", "Описание 3", StatusTask.NEW, epicId);
        int subTaskId3 = subTask3.getId();
        System.out.println("ID подзадачи " + subTaskId3);
        manager.deleteSubTaskById(subTaskId3);
        System.out.println("Подзадача с ID " + subTaskId3 + " удалена");

        manager.getSubTasksByEpicId(epicId);
        System.out.println("ID подзадач по ID эпика " + epicId + " " + epicId1);

        String allSubTasks = manager.getAllSubTasks().toString();
        System.out.println(allSubTasks);

        String allTasks = manager.getAllTasks().toString();
        System.out.println(allTasks);

        manager.deleteEpicById(epicId1);
        System.out.println("Эпик с ID " + epicId1 + " удалён");
        String allEpics = manager.getAllEpics().toString();
        System.out.println(allEpics);




    }
}


