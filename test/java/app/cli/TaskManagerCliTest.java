package java.app.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.app.entity.Epic;
import java.app.entity.SubTask;
import java.app.entity.Task;
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
        outContent.reset();
    }

    @Test
    @DisplayName("Чтение корректного ввода")
    void testReadIntInput() {
        // Given
        String input = "5";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);
        //When
        int result = consoleUi.readIntInput();
        //Then
        assertEquals(5, result);
    }

    @Test
    @DisplayName("Некорректный ввод с последующим корректным")
    void testReadIntInputWithInvalidInput() {
        //Given
        String input = "abc\n10";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);
        //When
        int result = consoleUi.readIntInput();
        //Then
        assertEquals(10, result);
        assertTrue(outContent.toString().contains("Неверный ввод. Пожалуйста введите число"));
    }

    @Test
    @DisplayName("Создание задачи через консоль")
    void testReadTaskInput() {
        //Given
        String input = "Test Task\nTest Description";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //When
        Task task = consoleUi.readTaskInput();

        //Then
        assertEquals("Test Task", task.getName());
        assertEquals("Test Description", task.getDescription());
    }

    @Test
    @DisplayName("Создание эпика через консоль")
    void testReadEpicInput() {
        //Given
        String input = "Test Epic\nTest Description";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //When
        Epic epic = consoleUi.readEpicInput();

        //Then
        assertEquals("Test Epic", epic.getName());
        assertEquals("Test Description", epic.getDescription());
    }

    @Test
    @DisplayName("Создание подзадачи с указанием эпика через консоль")
    void testReadSubTaskInput() {
        //Given
        String input = "Test Subtask\nTest Description\n1";
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        consoleUi = new TaskManagerCLI(scanner, true);

        //When
        SubTask subtask = consoleUi.readSubTaskInput();

        //Then
        assertEquals("Test Subtask", subtask.getName());
        assertEquals("Test Description", subtask.getDescription());
        assertEquals(1, subtask.getEpicId());
    }
}