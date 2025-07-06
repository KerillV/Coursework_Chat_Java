import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTestServer {

    private static Process serverProcess;
    private static final String SERVER_LOG_FILE = "file.log"; // журнал
    private static final int PORT = 8080; // порт, на котором будет запущен сервер

    // метод @BeforeAll выполняется один раз до начала любых тестов, используется для подготовки общего
    // окружения, которое применяется ко всем последующим тестовым сценариям
    @BeforeAll
    static void startServer() throws IOException { // метод стартует сервер
        File file = new File("settings.txt");
        /* Проверяем существование файла settings.txt, в котором хранится информация о порте. Если файл отсутствует, он создается,
        и туда записывается значение нашего порта (8080) */
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            bw.write(Integer.toString(PORT));
            bw.close();
        }

        serverProcess = new ProcessBuilder()
                .command("java", // команда запуска виртуальной машины Java
                        "-cp", // путь к классу, указывающий на текущую директорию
                        ".",
                        "ChatServer") // название класса, который будет исполняться
                .redirectErrorStream(true) // перенаправление вывода ошибок в стандартный поток, чтобы облегчить диагностику возможных проблем
                .start();
    }

    // метод @AfterAll выполняется после завершения всех тестов
    @AfterAll
    // Метод stopServer(): очищает окружение, разрушая запущенный серверный процесс
    static void stopServer() throws InterruptedException {
        serverProcess.destroy();
        TimeUnit.SECONDS.sleep(1); // Время сна (sleep) позволяет процессу завершиться окончательно
    }

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Test // единичный тестовый кейс
    void testServerConnectionAndMessaging() throws IOException, InterruptedException { // основной тестовый сценарий
        // создаем сокеты для двух клиентов
        Socket client1 = new Socket();
        Socket client2 = new Socket();

        InetSocketAddress addr = new InetSocketAddress("localhost", PORT);
        client1.connect(addr);
        client2.connect(addr);

        // оба клиента устанавливают соединение с сервером
        // Потоки ввода-вывода: создаются каналы для чтения и записи сообщений с клиентов
        // Буферизованные читатели: используются для удобного чтения поступающих сообщений.
        BufferedReader in1 = new BufferedReader(
                new InputStreamReader(client1.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader in2 = new BufferedReader(
                new InputStreamReader(client2.getInputStream(), StandardCharsets.UTF_8));

        // Потоки вывода объекты PrintWriter нужны для отправки сообщений серверу
        PrintWriter out1 = new PrintWriter(client1.getOutputStream(), true);
        PrintWriter out2 = new PrintWriter(client2.getOutputStream(), true);

        // Регистрируемся на сервере, отправляем имена
        out1.println("User1");
        out2.println("User2");

        // Ожидается подтверждение регистрации в виде сообщения вида
        assertEquals("[User1 присоединился к чату]", in1.readLine());
        assertEquals("[User2 присоединился к чату]", in2.readLine());

        // Отправка сообщений, клиент отправляет сообщение другому участнику чата
        out1.println("Hello from User1!");
        out2.println("Hi there, User2 here!");

        // Проверка доставки сообщений, проверяется, что оба клиента получили сообщение другого участника
        assertEquals("[User1]: Hello from User1!", in2.readLine());
        assertEquals("[User2]: Hi there, User2 here!", in1.readLine());

        // Прекращаем сеанс
        out1.println("/exit");
        out2.println("/exit");

        // ожидается соответствующее уведомление от сервера о выходе пользователя
        assertEquals("[User1 покинул чат]", in1.readLine());
        assertEquals("[User2 покинул чат]", in2.readLine());

        // Ожидаем некоторое время, чтобы сервер завершил операции
        TimeUnit.SECONDS.sleep(1);

        // Проверяем наличие записей в файле лога
        File logFile = new File(SERVER_LOG_FILE);
        assertTrue(logFile.exists());
        assertTrue(logFile.length() > 0);
    }

}
