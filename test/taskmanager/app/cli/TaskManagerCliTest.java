package taskmanager.app.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import taskmanager.app.entity.Epic;
import taskmanager.app.entity.SubTask;
import taskmanager.app.entity.Task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тесты CLI для TaskManager")
class TaskManagerCliTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private TaskManagerCLI consoleUi;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(System.out);
    }

    @Test
    @DisplayName("Чтение корректного ввода")
    void testReadIntInput() {
        // вводим число 5
        String input = "5";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);
        //вызываем метод readIntInput()
        int result = consoleUi.readIntInput();
        //проверяем, что результат равен 5
        assertEquals(5, result);
    }

    @Test
    @DisplayName("Некорректный ввод с последующим корректным")
    void testReadIntInputWithInvalidInput() {
        // вводим некорректный ввод и затем корректный
        String input = "abc\n10";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);
        //вызываем метод readIntInput()
        int result = consoleUi.readIntInput();
        //проверяем, что результат равен 10
        assertEquals(10, result);
        assertTrue(outContent.toString().contains("Неверный ввод. Пожалуйста введите число"));
    }

    @Test
    @DisplayName("Создание задачи через консоль")
    void testReadTaskInput() {
        // вводим данные для создания задачи
        String input = "Test Task\nTest Description";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //Вызываем метод readTaskInput()
        Task task = consoleUi.readTaskInput();

        //Проверяем, что задача создана корректно
        assertEquals("Test Task", task.getName());
        assertEquals("Test Description", task.getDescription());
    }

    @Test
    @DisplayName("Создание эпика через консоль")
    void testReadEpicInput() {
        // вводим данные для создания задачи
        String input = "Test Epic\nTest Description";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //Вызываем метод readEpicInput()
        Epic epic = consoleUi.readEpicInput();

        //Проверяем, что задача создана корректно
        assertEquals("Test Epic", epic.getName());
        assertEquals("Test Description", epic.getDescription());
    }

    @Test
    @DisplayName("Создание подзадачи с указанием эпика через консоль")
    void testReadSubTaskInput() {
        // вводим данные для создания задачи
        String input = "Test Subtask\nTest Description\n1";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //Вызываем метод readSubtaskInput()
        SubTask subtask = consoleUi.readSubTaskInput();

        //Проверяем, что задача создана корректно
        assertEquals("Test Subtask", subtask.getName());
        assertEquals("Test Description", subtask.getDescription());
        assertEquals(1, subtask.getEpicId());
    }
}