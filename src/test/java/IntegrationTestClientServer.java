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

public class IntegrationTestClientServer {
    private static Process serverProcess;
    private static final String CLIENT_LOG_FILE = "file.log";
    private static final int PORT = 8080;

    @BeforeAll
    static void startServer() throws IOException {
        File file = new File("settings.txt");
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            bw.write(Integer.toString(PORT));
            bw.close();
        }

        serverProcess = new ProcessBuilder()
                .command("java", "-cp", ".", "ChatServer")
                .redirectErrorStream(true)
                .start();
    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        serverProcess.destroy();
        TimeUnit.SECONDS.sleep(1); // ожидание завершения процессов
    }

    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @Test
    void testClientServerCommunication() throws IOException, InterruptedException {
        // Создание экземпляра клиента
        Socket clientSocket = new Socket();
        InetSocketAddress addr = new InetSocketAddress("localhost", PORT);
        clientSocket.connect(addr);

        BufferedReader input = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

        // Регистрация пользователя
        output.println("Alice");
        assertEquals("[Alice joined the chat]", input.readLine());

        // Отправка сообщения
        output.println("Hello everyone!");
        assertEquals("[Alice]: Hello everyone!", input.readLine());

        // Выход из чата
        output.println("/exit");
        assertEquals("[Alice left the chat]", input.readLine());

        // Ожидание освобождения ресурсов
        TimeUnit.SECONDS.sleep(1);

        // Проверка наличия записей в файле лога
        File logFile = new File(CLIENT_LOG_FILE);
        assertTrue(logFile.exists());
        assertTrue(logFile.length() > 0);
    }

}
