package java.app.service.time;

import org.junit.jupiter.api.*;
import java.app.entity.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotServiceTest {

    private TimeManagerService timeManagerService;
    private Task testTask;
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());

        timeManagerService = new TimeManagerService();
        testTask = new Task(1, "Test Task", "Description", null,
                Duration.ofHours(2),
                LocalDateTime.of(2025, 9, 8, 10, 0));
    }

    @AfterEach
    void tearDown() {
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("Тесты метода getTimeSlotsForInterval")
    class GetTimeSlotsForIntervalTest {

        @Test
        @DisplayName("Должен возвращать пустой набор при null параметрах")
        void testShouldReturnEmptySetForNullParameters() {
            // When
            Set<LocalDateTime> result1 = timeManagerService.getTimeSlotsForInterval(null, null);
            Set<LocalDateTime> result2 = timeManagerService.getTimeSlotsForInterval(
                    LocalDateTime.of(2025, 9, 8, 10, 0), null);
            Set<LocalDateTime> result3 = timeManagerService.getTimeSlotsForInterval(
                    null, LocalDateTime.of(2025, 9, 8, 12, 0));

            // Then
            assertTrue(result1.isEmpty());
            assertTrue(result2.isEmpty());
            assertTrue(result3.isEmpty());
        }

        @Test
        @DisplayName("Должен возвращать пустой набор при start после end")
        void testShouldReturnEmptySetWhenStartAfterEnd() {
            // Given
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 12, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 10, 0);

            // When
            Set<LocalDateTime> result = timeManagerService.getTimeSlotsForInterval(start, end);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Должен правильно рассчитывать слоты для интервала в 1 час")
        void testShouldCalculateSlotsForOneHourInterval() {
            // Given
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 11, 0);

            // When
            Set<LocalDateTime> result = timeManagerService.getTimeSlotsForInterval(start, end);

            // Then
            assertEquals(5, result.size()); // 10:00, 10:15, 10:30, 10:45, 11:00
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 0)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 15)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 30)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 45)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 11, 0)));
        }

        @Test
        @DisplayName("Должен правильно рассчитывать слоты для интервала менее 15 минут")
        void shouldCalculateSlotsForIntervalLessThan15Minutes() {
            // Given
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 10, 10);

            // When
            Set<LocalDateTime> result = timeManagerService.getTimeSlotsForInterval(start, end);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.contains(LocalDateTime.of(2025, 9,
                    8, 10, 0)));
        }

        @Test
        @DisplayName("Должен обрезать время до минут при расчете слотов")
        void testShouldTruncateToMinutesWhenCalculatingSlots() {
            // Given
            LocalDateTime start = LocalDateTime.of
                    (2025, 9, 8, 10, 0, 30);
            LocalDateTime end = LocalDateTime.of
                    (2025, 9, 8, 10, 30, 45);

            // When
            Set<LocalDateTime> result = timeManagerService.getTimeSlotsForInterval(start, end);

            // Then
            assertEquals(3, result.size());
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 0)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 15)));
            assertTrue(result.contains(LocalDateTime.of
                    (2025, 9, 8, 10, 30)));
        }
    }

    @Nested
    @DisplayName("Тесты метода isTimeSlotAvailable")
    class IsTimeSlotAvailableTest {

        @Test
        @DisplayName("Должен возвращать true для null параметров")
        void testShouldReturnTrueForNullParameters() {
            // When & Then
            assertTrue(timeManagerService.isTimeSlotAvailable(null, null));
            assertTrue(timeManagerService.isTimeSlotAvailable(LocalDateTime.now(), null));
            assertTrue(timeManagerService.isTimeSlotAvailable(null, LocalDateTime.now()));
        }

        @Test
        @DisplayName("Должен возвращать true для свободного интервала")
        void testShouldReturnTrueForAvailableInterval() {
            // Given
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 12, 0);

            // When
            boolean result = timeManagerService.isTimeSlotAvailable(start, end);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Должен возвращать false для занятого интервала")
        void testShouldReturnFalseForOccupiedInterval() {
            // Given
            timeManagerService.addTaskToTimeSlots(testTask);
            LocalDateTime overlappingStart = LocalDateTime.of(2025, 9, 8, 11, 0);
            LocalDateTime overlappingEnd = LocalDateTime.of(2025, 9, 8, 13, 0);

            // When
            boolean result = timeManagerService.isTimeSlotAvailable(overlappingStart, overlappingEnd);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Должен возвращать true для интервала после удаления задачи")
        void testShouldReturnTrueAfterRemovingTask() {
            // Given
            timeManagerService.addTaskToTimeSlots(testTask);
            timeManagerService.removeTaskFromTimeSlots(testTask);
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 12, 0);

            // When
            boolean result = timeManagerService.isTimeSlotAvailable(start, end);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Тесты метода validateTaskTime")
    class ValidateTaskTimeTest {

        @Test
        @DisplayName("Должен пропускать задачу без времени")
        void testShouldSkipTaskWithoutTime() {
            // Given
            Task taskWithoutTime = new Task(2, "No Time Task",
                    "Description", null, null, null);

            // When & Then
            assertDoesNotThrow(() -> timeManagerService.validateTaskTime(taskWithoutTime));
        }

        @Test
        @DisplayName("Должен пропускать задачу с null временем начала")
        void testShouldSkipTaskWithNullStartTime() {
            // Given
            Task task = new Task(2, "Task", "Description",
                    null, Duration.ofHours(1), null);

            // When & Then
            assertDoesNotThrow(() -> timeManagerService.validateTaskTime(task));
        }

        @Test
        @DisplayName("Должен пропускать задачу с null длительностью")
        void testShouldSkipTaskWithNullDuration() {
            // Given
            Task task = new Task(2, "Task", "Description",
                    null, null, LocalDateTime.now());

            // When & Then
            assertDoesNotThrow(() -> timeManagerService.validateTaskTime(task));
        }

        @Test
        @DisplayName("Должен выбрасывать исключение при пересечении времени")
        void testShouldThrowExceptionForTimeOverlap() {
            // Given
            timeManagerService.addTaskToTimeSlots(testTask);
            Task overlappingTask = new Task(2, "Overlapping Task", "Description", null,
                    Duration.ofHours(1),
                    LocalDateTime.of(2025, 9, 8, 11, 0));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> timeManagerService.validateTaskTime(overlappingTask));
            assertTrue(exception.getMessage().contains("пересекается по времени"));
        }

        @Test
        @DisplayName("Должен пропускать задачу без пересечения времени")
        void testShouldAllowTaskWithoutTimeOverlap() {
            // Given
            timeManagerService.addTaskToTimeSlots(testTask);
            Task nonOverlappingTask = new Task(2, "Non-Overlapping Task",
                    "Description", null, Duration.ofHours(1),
                    LocalDateTime.of(2025, 9, 8, 13, 0));

            // When & Then
            assertDoesNotThrow(() -> timeManagerService.validateTaskTime(nonOverlappingTask));
        }
    }

    @Nested
    @DisplayName("Тесты методов addTaskToTimeSlots и removeTaskFromTimeSlots")
    class AddRemoveTaskFromTimeSlotsTest {

        @Test
        @DisplayName("Должен игнорировать задачу без времени при добавлении")
        void testShouldIgnoreTaskWithoutTimeWhenAdding() {
            // Given
            Task taskWithoutTime = new Task(2, "No Time Task",
                    "Description", null, null, null);

            // When
            assertDoesNotThrow(() -> timeManagerService.addTaskToTimeSlots(taskWithoutTime));

            // Then
            assertTrue(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 12, 0)
            ));
        }

        @Test
        @DisplayName("Должен добавлять и удалять временные слоты задачи")
        void testShouldAddAndRemoveTaskTimeSlots() {
            // Given
            LocalDateTime start = LocalDateTime.of(2025, 9, 8, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 9, 8, 12, 0);

            // When
            timeManagerService.addTaskToTimeSlots(testTask);
            boolean afterAdd = timeManagerService.isTimeSlotAvailable(start, end);

            // Then
            assertFalse(afterAdd);

            // When
            timeManagerService.removeTaskFromTimeSlots(testTask);
            boolean afterRemove = timeManagerService.isTimeSlotAvailable(start, end);

            // Then
            assertTrue(afterRemove);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать несколько задач в одних слотах")
        void shouldHandleMultipleTasksInSameSlots() {
            // Given
            Task task1 = new Task(1, "Task 1", "Description", null,
                    Duration.ofHours(1),
                    LocalDateTime.of(2025, 9, 8, 10, 0));

            Task task2 = new Task(2, "Task 2", "Description", null,
                    Duration.ofHours(1),
                    LocalDateTime.of(2025, 9, 8, 10, 30));

            // When
            timeManagerService.addTaskToTimeSlots(task1);
            timeManagerService.addTaskToTimeSlots(task2);

            // Then
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 11, 30)
            ));

            // When
            timeManagerService.removeTaskFromTimeSlots(task1);

            // Then
            assertTrue(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 10, 29)
            ));

            //
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 30),
                    LocalDateTime.of(2025, 9, 8, 10, 31)
            ));

            //
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 30),
                    LocalDateTime.of(2025, 9, 8, 11, 30)
            ));
        }

        @Test
        @DisplayName("Должен игнорировать задачу без времени при удалении")
        void testShouldIgnoreTaskWithoutTimeWhenRemoving() {
            // Given
            Task taskWithoutTime = new Task(2, "No Time Task",
                    "Description", null, null, null);

            // When & Then
            assertDoesNotThrow(() -> timeManagerService.removeTaskFromTimeSlots(taskWithoutTime));
        }
    }

    @Nested
    @DisplayName("Интеграционные тесты")
    class IntegrationTests {

        @Test
        @DisplayName("Полный цикл: добавление, проверка, удаление задачи")
        void testFullCycleAddValidateRemove() {
            // Given
            Task task = new Task(1, "Test Task", "Description", null,
                    Duration.ofHours(2),
                    LocalDateTime.of(2025, 9, 8, 10, 0));

            // When & Then 1
            assertTrue(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 12, 0)
            ));

            // When & Then 2
            assertDoesNotThrow(() -> timeManagerService.validateTaskTime(task));

            // When & Then 3
            timeManagerService.addTaskToTimeSlots(task);
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 12, 0)
            ));

            // When & Then 4
            Task overlappingTask = new Task(2, "Overlapping", "Description", null,
                    Duration.ofHours(1),
                    LocalDateTime.of(2025, 9, 8, 11, 0));
            assertThrows(RuntimeException.class,
                    () -> timeManagerService.validateTaskTime(overlappingTask));

            // When & Then 5
            timeManagerService.removeTaskFromTimeSlots(task);
            assertTrue(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 12, 0)
            ));
        }

        @Test
        @DisplayName("Проверка граничных случаев временных интервалов")
        void testEdgeCaseTimeIntervals() {
            // Given
            Task task = new Task(1, "Edge Task", "Description", null,
                    Duration.ofMinutes(1), // Минимальная длительность
                    LocalDateTime.of(2025, 9, 8, 10, 0));

            // When
            timeManagerService.addTaskToTimeSlots(task);

            // Then
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 10, 1)
            ));
            assertFalse(timeManagerService.isTimeSlotAvailable(
                    LocalDateTime.of(2025, 9, 8, 10, 0),
                    LocalDateTime.of(2025, 9, 8, 10, 15)
            ));
        }
    }
}