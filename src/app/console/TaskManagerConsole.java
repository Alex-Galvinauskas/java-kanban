package app.console;

import core.Epic;
import core.SubTask;
import core.Task;
import managers.Managers;
import service.InMemoryHistoryManager;
import service.InMemoryTaskManager;
import java.util.List;

import java.util.Scanner;

public class TaskManagerConsole {
    private final InMemoryTaskManager taskManager = Managers.getDefault();
    private final InMemoryHistoryManager historyManager = Managers.getDefaultHistory();
    private final Scanner scanner;

    /**
     * Флаг, указывающий, запущена ли программа
     */
    public boolean isRunning;

    public TaskManagerConsole(Scanner scanner, boolean isRunning) {
        this.scanner = scanner;
        this.isRunning = isRunning;
    }

    /**
     * Запускает консольный интерфейс
     */
    public void start() {
        while (isRunning) {
            showMenu(
                    "ГЛАВНОЕ МЕНЮ",
                    List.of(
                            new MenuItem("Операции с задачами", this::taskOperation),
                            new MenuItem("Операции с эпиками", this::epicOperation),
                            new MenuItem("Операции с подзадачами", this::subTaskOperation),
                            new MenuItem("Просмотр всех задач", () -> {
                                System.out.println(taskManager.getAllTasks());
                            }),
                            new MenuItem("Просмотр истории", () -> {
                                System.out.println(this.viewHistory());
                            }),
                            new MenuItem("Выход из программы", () -> {
                                System.out.println("Спасибо за использование.");
                                isRunning = false;
                            }

                            )
                    ));
        }
    }


    public void showMenu(String title, List<MenuItem> items) {
        System.out.println("\n=== " + title + " ===");
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, items.get(i).title());
        }
        System.out.println("0 " + (title.equals("ГЛАВНОЕ МЕНЮ")
                ? "Выход из программы"
                : "Вернуться в главное меню"));
        System.out.println("Выберите пункт меню:");

        int choice = readIntInput();
        if (choice == 0) {
            return;
        }

        if (choice > 0 && choice <= items.size()) {
            items.get(choice - 1).action().run();
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
                            int item = readIntInput("Введите ID задачи для удаления:");
                            taskManager.deleteTaskById(item);
                        }),
                        new MenuItem("Просмотр всех задач", () ->
                                System.out.println(taskManager.getAllTasks())),

                        new MenuItem("Найти задачу по ID", () -> {
                            int item = readIntInput("Введите ID задачи:");
                            System.out.println(taskManager.getTaskById(item));
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
                            int item = readIntInput("Введите ID эпика для удаления:");
                            taskManager.deleteEpicById(item);
                        }),
                        new MenuItem("Просмотр всех эпиков", () ->
                                System.out.println(taskManager.getAllEpics())),
                        new MenuItem("Найти эпик по ID", () -> {
                            int item = readIntInput("Введите ID эпика:");
                            System.out.println(taskManager.getEpicById(item));
                        }),
                        new MenuItem("Просмотреть подзадачи эпика", () -> {
                            int item = readIntInput("Введите ID эпика:");
                            System.out.println(taskManager.getSubTasksByEpicId(item));
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
                            int item = readIntInput("Введите ID подзадачи для удаления:");
                            taskManager.deleteSubTaskById(item);
                        }),
                        new MenuItem("Посмотреть все подзадачи", () ->
                                System.out.println(taskManager.getAllSubTasks())),
                        new MenuItem("Найти подзадачу по ID", () -> {
                            int item = readIntInput("Введите ID подзадачи:");
                            System.out.println(taskManager.getSubTaskById(item));
                        })
                )
        );
    }

    /**
     * Читает ввод пользователя
     *
     * @param number - сообщение для пользователя
     *
     * @return введенное число
     */
    private int readIntInput(String number) {
        while (true) {
            System.out.println(number);
            try {
                int item = Integer.parseInt(scanner.nextLine());
                if (item >= 0) {
                    return item;
                }
                System.out.println("Пункт меню должен быть положительным числом.");
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат. Введите число.");
            }
        }
    }

    /**
     * Читает ввод пользователя
     *
     * @return введенное число
     */
    public int readIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Пожалуйста введите число");
            }
        }
    }

    /**
     * Читает ввод данных для создания задачи
     *
     * @return объект задачи
     */
    public Task readTaskInput() {
        System.out.println("Введите название задачи:");
        String title = scanner.nextLine();
        System.out.println("Введите описание задачи:");
        String description = scanner.nextLine();
        return new Task(title, description);
    }

    /**
     * Читает ввод данных для создания эпика
     *
     * @return объект эпик
     */

    public Epic readEpicInput() {
        System.out.println("Введите название эпика:");
        String title = scanner.nextLine();
        System.out.println("Введите описание эпика:");
        String description = scanner.nextLine();
        return new Epic(title, description);
    }

    /**
     * Читает ввод данных для создания подзадачи
     *
     * @return объект подзадачи
     */
    public SubTask readSubTaskInput() {
        System.out.println("Введите название подзадачи:");
        String title = scanner.nextLine();
        System.out.println("Введите описание подзадачи:");
        String description = scanner.nextLine();
        System.out.println("Введите ID родительского эпика:");
        int epicId = readIntInput("Введите ID эпика:");
        return new SubTask(title, description, epicId);
    }

    /**
     * Выводит историю просмотров
     *
     * @return строку истории просмотров
     */
    private String viewHistory() {
        return historyManager.getHistory().toString();
    }
}
