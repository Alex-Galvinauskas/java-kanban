package taskmanager.app.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import taskmanager.app.management.TaskManager;
import taskmanager.app.service.manager.InMemoryTaskManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpTaskServerTest {

    private HttpTaskServer server;
    private static final AtomicInteger portCounter = new AtomicInteger(8081);
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Test
    void testServerCreationWithCustomManager() throws IOException {
        // Given
        TaskManager manager = new InMemoryTaskManager();
        int port = portCounter.getAndIncrement();

        // When
        server = new HttpTaskServer(manager, port);

        // Then
        assertNotNull(server);
        assertNotNull(server.getGson());
        assertEquals(port, server.getPort());
    }

    @Test
    void testServerCreationWithDefaultManager() throws IOException {
        // Given
        int port = portCounter.getAndIncrement();

        // When
        server = new HttpTaskServer(port);

        // Then
        assertNotNull(server);
        assertNotNull(server.getGson());
        assertEquals(port, server.getPort());
    }

    @Test
    void testServerCreationWithDefaultConstructor() throws IOException {
        // When
        server = new HttpTaskServer(new InMemoryTaskManager(), portCounter.getAndIncrement());

        // Then
        assertNotNull(server);
        assertNotNull(server.getGson());
    }
}