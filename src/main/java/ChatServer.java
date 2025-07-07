import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/* класс для реализации логики ожидания входящих соединений, регистрации пользователей и распространения
сообщений среди активных клиентов */
public class ChatServer {
    /* clients хранит список активных пользователей, где ключ — это имя пользователя, а значение — PrintWriter,
    используемый для отправки сообщений этому пользователю */
    private final Map<String, PrintWriter> clients = new HashMap<>();
    private final int PORT; // порт, на котором сервер будет ждать подключения
    private final String LOG_FILE_NAME = "file.log"; // имя файла журнала
    private ServerSocket serverSocket;

    public ChatServer(int port) {
        // конструктор принимает port и инициализирует
        this.PORT = port;
    }

    public void run() throws Exception {
        // ServerSocket позволяет организовать прием входящих TCP/IP соединений
        // создаем объект, ожидающий входящих соединений на заданном порту
        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setReuseAddress(true); // Разрешаем повторное использование адреса и порта
            System.out.println("Сервер запущен на порте " + PORT);

            while (!serverSocket.isClosed()) {
            /* Основное тело метода бесконечно ждёт поступления новых соединений от клиентов.
                Каждое соединение порождает новый поток для обслуживания конкретного клиента */
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    /* Метод для отправки сообщения всем зарегистрированным пользователям.
    Сначала формирует строковую запись для лога, содержащую метку времени, имя отправителя и само сообщение */
    private synchronized void broadcast(String sender, String msg) {
        LocalDateTime now = LocalDateTime.now();
        String logEntry = "[" + now.toString() + "] [" + sender + "]: " + msg + "\n";

        appendLog(logEntry); // добавляем сформированную запись в файл журнала

        /* Перебираем всех зарегистрированных пользователей и отправляем каждому сообщение через
         соответствующий PrintWriter. Метод flush() гарантирует немедленную передачу данных */
        clients.values().forEach(writer -> {
            writer.println(logEntry.trim());
            writer.flush();
        });
    }

    /* Метод handleClient() обслуживает одного конкретного клиента. Получает входящий поток данных
     от клиента и выходной поток для передачи сообщений обратно клиенту */
    private void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String username = reader.readLine().trim(); // Прочитайте строку и удалите пробелы
            if (username.isEmpty()) {
                System.out.println("Отказано в регистрации: Имя пользователя не предоставлено");
                return; // Закрываем соединение сразу
            }
            clients.put(username, writer);
            System.out.println("Пользователь '" + username + "' присоединился.");

            /* Циклически читаем сообщения от клиента, пока тот не пошлет команду /exit. Все принятые
            сообщения транслируются остальным участникам чата методом broadcast() */
            String line;
            while ((line = reader.readLine()) != null && !"/exit".equals(line)) {
                if ("".equals(line.trim())) continue;

                broadcast(username, line);
            }

            // удаляем клиента из активного списка и закрываем соединение, если произошла ошибка
            clients.remove(username);
            System.out.println("Пользователь '" + username + "' покинул чат.");
        } catch (IOException ex) {
            System.err.println("Ошибка при обработке клиента: " + ex.getMessage());
        }
    }
    // Метод добавляет запись в файл журнала, дополняя его существующими данными
    private void appendLog(String entry) {
        try {
            Path logDir = Paths.get("log");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir); // создаем директорию log, если её ещё нет
            }

            Path logPath = logDir.resolve("file.log");
            Files.writeString(logPath, entry + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Ошибка записи в лог-файл: " + ex.getMessage());
        }
    }

    public void stop() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addLogEntry(String entry) {
        appendLog(entry);
    }

    public void sendMessage(String sender, String message) {
        broadcast(sender, message);
    }

    // Главная точка входа приложения.
    // Читаем порт из файла настроек и запускаем сервер на указанном порту.
    public static void main(String[] args) throws Exception {
        String settingsFileName = "settings.txt"; // Файл конфигурации
        int port = readPortFromSettings(settingsFileName);
        new ChatServer(port).run();
    }

    // Метод читает число из файла настроек и возвращает его в виде целочисленного значения
    public static int readPortFromSettings(String filename) throws IOException {
        String content = Files.readString(Path.of(filename));
        return Integer.parseInt(content.trim()); // trim() уберёт пробелы и переводы строки
    }
}
