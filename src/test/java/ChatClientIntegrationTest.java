import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatClientIntegrationTest {

    @InjectMocks
    private ChatClient client;

    @Mock
    private Socket socket;

    @Captor
    ArgumentCaptor<byte[]> byteCaptor;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testClientConnectToServer() throws Exception {
        // Устанавливаем поведение мока для сокета
        when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("connected".getBytes(StandardCharsets.UTF_8)));

        // Запускаем клиента
        client.run();

        // Проверяем, что сокет был установлен
        assertNotNull(client.getSocket(), "Соединение с сервером не установлено");
    }

    @Test
    void testSendAndReceiveMessage() throws Exception {
        // Подготовим фиктивное сообщение
        String testMessage = "Hello from integration test!";

        // Моком OutputStream, чтобы перехватить отправленное сообщение
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(baos);

        // Получим BufferedReader для симуляции приема сообщения
        ByteArrayInputStream bis = new ByteArrayInputStream("Your data here".getBytes());
        when(socket.getInputStream()).thenReturn(bis);

        // Создание BufferedReader
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Имитация ввода пользователя
        ByteArrayInputStream inputStream = new ByteArrayInputStream((testMessage + "\n").getBytes(StandardCharsets.UTF_8));
        BufferedReader mockReader = new BufferedReader(new InputStreamReader(inputStream));

        // Переопределим конструктор BufferedReader для захвата чтения
        PowerMockito.whenNew(BufferedReader.class).withArguments(any(InputStreamReader.class)).thenReturn(mockReader);

        // Начало работы клиента
        client.run();

        // Прочитаем сообщение, отправленное через сокет
        String sentMessage = baos.toString(StandardCharsets.UTF_8.name());
        assertTrue(sentMessage.contains(testMessage), "Сообщение не было отправлено!");
    }

    @Test
    void testStopClient() throws Exception {
        // Задействуем mock OutputStream для наблюдения за печатью сообщений
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PowerMockito.when(client.sysW.captureOutput()).thenReturn(outputStream);

        // Останавливаем клиента
        client.stop();

        // Проверяем вывод в консоль
        String output = outputStream.toString();
        assertTrue(output.contains("Вы вышли из чата."));
        assertFalse(client.isRunning(), "Клиент продолжает выполняться после команды exit");
    }

    @Test
    void testAppendLog() throws Exception {
        // Симулируем запись в лог
        String expectedLogEntry = "Тестовая запись в лог";
        client.logEntry(expectedLogEntry);

        // Проверяем факт наличия записи в файле
        var fileContent = Files.readString(Paths.get("file.log"));
        assertTrue(fileContent.contains(expectedLogEntry), "Запись в лог не была сделана!");
    }

    @Test
    void testAskForUsername() throws Exception {
        // Готовим входящий поток
        ByteArrayInputStream inputStream = new ByteArrayInputStream("TestUser\n".getBytes(StandardCharsets.UTF_8));
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        // Получить имя пользователя
        String username = ChatClient.askForUsername();
        assertEquals("TestUser", username, "Имя пользователя неверно");
    }

}