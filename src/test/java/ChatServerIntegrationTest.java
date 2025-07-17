import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerIntegrationTest {

    private static ChatServer chatServer;
    //private static ServerSocket serverSocket;
    private static int TEST_PORT = 8080;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeAll
    static void setup() throws Exception {
        // Проверяем доступность порта перед запуском сервера
        try (ServerSocket tempSocket = new ServerSocket(TEST_PORT)) {
        }
        chatServer = new ChatServer(TEST_PORT);
        new Thread(() -> {
            try {
                chatServer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        // Дожидаемся старта сервера
        Thread.sleep(1000);
    }

    @Test
    void testRunServer() throws Exception {
        // Попробуем создать новое соединение с сервером
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            assertTrue(socket.isConnected(), "Соединение с сервером не установлено");
        }
    }

    @Test
    void testBroadcastMessage() throws Exception {
        // Проверка логирования сообщения
        String message = "Тестовое сообщение";
        chatServer.sendMessage("testUser", message);

        // Проверяем наличие записи в журнале
        var fileContent = Files.readString(Paths.get("src/main/resources/file.log"));
        assertTrue(fileContent.contains(message), "Сообщение не было зарегистрировано в логе!");
    }

    @Test
    void testHandleClientConnection() throws Exception {
        // Подключение к серверу
        Socket clientSocket = new Socket("localhost", TEST_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        // Регистрация пользователя
        out.println("MockUser");

        // Отправляем сообщение
        out.println("Hello from mock client");

        // Небольшое ожидание, чтобы сервер успел обработать сообщение
        TimeUnit.SECONDS.sleep(1);

        // Прочитываем ответ
        String response = in.readLine();

        // Проверяем, содержится ли наше сообщение в отклике
        assertTrue(response.contains("Hello from mock client"),
                "Ожидалось получение своего же сообщения обратно");

        // Завершение сессии
        out.println("/exit");
        clientSocket.close();
    }

    @Test
    void testAppendLog() throws Exception {
        // Удалим старый лог-файл перед добавлением новой строки
        File logFile = new File("src/main/resources/file.log");
        if (logFile.exists()) {
            logFile.delete();
        }

        // Добавляем запись
        String expectedLogEntry = "Тестовая запись в лог";
        chatServer.addLogEntry(expectedLogEntry);

        // Читаем содержимое файла
        var fileContent = Files.readString(Paths.get("src/main/resources/file.log"));
        assertTrue(fileContent.contains(expectedLogEntry), "Запись в лог не была сделана!");
    }

    @Test
    void testReadPortFromSettings() throws Exception {
        // Настроим файл настроек вручную
        Path path = Paths.get("src/main/resources/settings.txt");
        Files.write(path, List.of(Integer.toString(TEST_PORT)), StandardCharsets.UTF_8);

        // Чтение порта
        int port = ChatServer.readPortFromSettings("src/main/resources/settings.txt");
        assertEquals(TEST_PORT, port, "Порт не соответствует ожиданию");
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        // Остановка сервера
        chatServer.stop();
    }
}
