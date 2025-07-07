import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class ChatClientIntegrationTest {

    private static ChatClient client;
    private static MockedStatic<System> systemMock;
    private static ExecutorService executorService;
    private static int TEST_PORT = 8080;

    @BeforeAll
    static void setUp() throws Exception {
        systemMock = Mockito.mockStatic(System.class);
        executorService = Executors.newSingleThreadExecutor();

        // Имитация вывода в консоль и ввода пользователя
        systemMock.when(() -> System.out.println(Optional.ofNullable(Mockito.any()))).thenReturn(null);
        systemMock.when(() -> System.in.read()).thenReturn((byte)'\n');

        // Создаем экземпляр клиента и стартуем его
        client = new ChatClient("localhost", TEST_PORT, "TestUser");
        new Thread(() -> {
            try {
                client.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        client.stop();
        executorService.shutdown();
        systemMock.close();
    }

    @Test
    void testClientConnectToServer() throws Exception {
        // Проверка успешного подключения клиента к серверу
        assertDoesNotThrow(() -> client.run());
    }

    @Test
    void testSendAndReceiveMessage() throws Exception {
        // Можем отправить сообщение и удостовериться, что оно дошло до другого клиента
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
        PrintWriter writer = new PrintWriter(client.getSocket().getOutputStream(), true);

        String testMessage = "Hello from integration test!";
        writer.println(testMessage);
        writer.flush();

        // Ждем подтверждения получения сообщения
        String receivedMsg = reader.readLine();
        assertTrue(receivedMsg.contains(testMessage), "Сообщение не было получено!");
    }

    @Test
    void testStopClient() throws Exception {
        // Тестируем отключение клиента
        client.stop();
        assertFalse(client.isRunning(), "Клиент продолжает выполняться после команды exit");
    }

    @Test
    void testAppendLog() throws Exception {
        // Проверка логирования действий клиента
        String expectedLogEntry = "Тестовая запись в лог";
        client.logEntry(expectedLogEntry);

        var fileContent = Files.readString(Paths.get("file.log"));
        assertTrue(fileContent.contains(expectedLogEntry), "Запись в лог не была сделана!");
    }

    @Test
    void testAskForUsername() throws Exception {
        // Проверка правильности запроса имени пользователя
        String username = ChatClient.askForUsername();
        assertEquals("TestUser", username, "Имя пользователя неверно");
    }
}