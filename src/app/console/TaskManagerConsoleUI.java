package app.console;

import core.*;
import managers.Managers;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;

import java.util.List;
import java.util.Scanner;

public class TaskManagerConsoleUI {
    private final InMemoryTaskManager taskManager = Managers.getDefault();
    private final InMemoryHistoryManager historyManager = Managers.getDefaultHistory();
    private final Scanner scanner;
    private boolean isRunning;

    public TaskManagerConsoleUI(Scanner scanner, boolean isRunning) {
        this.scanner = scanner;
        this.isRunning = isRunning;
    }

    public void start() {
        while (isRunning) {
            showMenu(
                    "ГЛАВНОЕ МЕНЮ",
                    List.of(
                            new MenuItem("Операции с задачами", this::taskOperation),
                            new MenuItem("Операции с эпиками", this::epicOperation),
                            new MenuItem("Операции с подзадачами", this::subTaskOperation),
                            new MenuItem("Просмотр всех задач", () -> System.out.println(taskManager.getAllTasks())),
                            new MenuItem("Просмотр истории", () -> System.out.println(this.viewHistory()))

                    )
                    );
        }
    }


    private void showMenu(String title, List<MenuItem> items) {
        System.out.println("\n=== " + title + " ===");
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, items.get(i).getTitle());
        }
        System.out.println("0 " + (title.equals("ГЛАВНОЕ МЕНЮ") ? "Выход из программы" : "Вернуться в главное меню"));
        System.out.println("Выберите пункт меню:");

        int choice = readIntInput();
        if (choice == 0) return;

        if (choice > 0 && choice <= items.size()) {
            items.get(choice - 1).getAction().run();
        } else {
            System.out.println("Неизвестная команда. Попробуйте еще раз.");
        }
    }

    private void taskOperation() {
        showMenu(
                "МЕНЮ ЗАДАЧ",
                List.of(
                        new MenuItem("Создать задачу", () -> {
                            Task task = readTaskInput();
                            taskManager.createTask(task);
                        }),
                        new MenuItem("Обновить задачу", () -> {
                            Task task = readTaskInput();
                            taskManager.updateTask(task);
                        }),
                        new MenuItem("Удалить задачу", () -> {
                            int id = readIntInput("Введите ID задачи для удаления:");
                            taskManager.deleteTaskById(id);
                        }),
                        new MenuItem("Просмотр всех задач", () ->
                                System.out.println(taskManager.getAllTasks())),

                        new MenuItem("Найти задачу по ID", () -> {
                            int id = readIntInput("Введите ID задачи:");
                            System.out.println(taskManager.getTaskById(id));
                        })
                )
        );
    }

    private void epicOperation() {
        showMenu(
                "МЕНЮ ЭПИКОВ",
                List.of(
                        new MenuItem("Создать эпик", () -> {
                            Epic epic = readEpicInput();
                            taskManager.createEpic(epic);
                        }),
                        new MenuItem("Удалить эпик", () -> {
                            int id = readIntInput("Введите ID эпика для удаления:");
                            taskManager.deleteEpicById(id);
                        }),
                        new MenuItem("Просмотр всех эпиков", () ->
                                System.out.println(taskManager.getAllEpics())),
                        new MenuItem("Найти эпик по ID", () -> {
                            int id = readIntInput("Введите ID эпика:");
                            System.out.println(taskManager.getEpicById(id));
                        }),
                        new MenuItem("Просмотреть подзадачи эпика", () -> {
                            int id = readIntInput("Введите ID эпика:");
                            System.out.println(taskManager.getSubTasksByEpicId(id));
                        })
                )
        );
    }


    private void subTaskOperation() {
        showMenu(
                "МЕНЮ ПОДЗАДАЧ",
                List.of(
                        new MenuItem("Создать подзадачу", () -> {
                            SubTask subTask = readSubTaskInput();
                            taskManager.createSubTask(subTask);
                        }),
                        new MenuItem("Обновить подзадачу", () -> {
                            SubTask subTask = readSubTaskInput();
                            taskManager.updateSubTask(subTask);
                        }),
                        new MenuItem("Удалить подзадачу", () -> {
                            int id = readIntInput("Введите ID подзадачи для удаления:");
                            taskManager.deleteSubTaskById(id);
                        }),
                        new MenuItem("Посмотреть все подзадачи", () ->
                                System.out.println(taskManager.getAllSubTasks())),
                        new MenuItem("Найти подзадачу по ID", () -> {
                            int id = readIntInput("Введите ID подзадачи:");
                            System.out.println(taskManager.getSubTaskById(id));
                        })
                )
        );
    }

    private int readIntInput(String number) {
        while (true) {
            System.out.println(number);
            try {
                int id = Integer.parseInt(scanner.nextLine());
                if (id >= 0)
                    return id;
                System.out.println("ID должен быть положительным числом.");
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат ID. Введите число.");
            }
        }
    }

    private int readIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Пожалуйста введите число");
            }
        }
    }

    private Task readTaskInput() {
        System.out.println("Введите название задачи:");
        String title = scanner.nextLine();
        System.out.println("Введите описание задачи:");
        String description = scanner.nextLine();
        return new Task(title, description);
    }

    private Epic readEpicInput() {
        System.out.println("Введите название эпика:");
        String title = scanner.nextLine();
        System.out.println("Введите описание эпика:");
        String description = scanner.nextLine();
        return new Epic(title, description);
    }

    private SubTask readSubTaskInput() {
        System.out.println("Введите название подзадачи:");
        String title = scanner.nextLine();
        System.out.println("Введите описание подзадачи:");
        String description = scanner.nextLine();
        System.out.println("Введите ID родительского эпика:");
        int epicId = readIntInput("Введите ID эпика:");
        return new SubTask(title, description, epicId);
    }

    private String viewHistory() {
        return historyManager.getHistory().toString();
    }
}
