import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
Класс реализует клиентскую сторону чата. Он соединяется с сервером, устанавливает имя пользователя
и участвует в процессе обмена сообщениями
*/
public class ChatClient {
    // Поля: адрес сервера, порт, имя пользователя и имя файла журнала
    private final String SERVER_ADDRESS;
    private final int SERVER_PORT;
    private final String USERNAME;
    private final String LOG_FILE_NAME = "file.log";
    private Socket socket;

    // Булевская переменная для контроля активности клиента
    private volatile boolean running = true;

    // Конструктор принимает адрес сервера, порт и имя пользователя
    public ChatClient(String address, int port, String username) {
        this.SERVER_ADDRESS = address;
        this.SERVER_PORT = port;
        this.USERNAME = username;
    }
    public Socket getSocket() {
        return socket;
    }

    public boolean isRunning() {
        return running;
    }

    public void logEntry(String entry) {
        appendLog(entry);
    }

    /*
    Основная точка входа для запуска клиента: соединение с сервером, авторизация и последующий обмен сообщениями
    */
    public void run() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) { // Правильно определяем socket здесь
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // Авторизуемся на сервере
            writer.println(USERNAME);

            // Запускаем поток для приёма сообщений от сервера
            executor.submit(() -> receiveMessages(reader));

            // Основное циклическое чтение сообщений пользователя
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (running) {
                line = userInput.readLine();
                if ("/exit".equalsIgnoreCase(line)) {
                    stop(); // Останавливаем клиента
                } else {
                    writer.println(line); // Отправляем сообщение на сервер
                }
            }
        }
    }

    // Останавливает клиента и закрывает соединение
    public void stop() {
        running = false; // Сигнал для остановки приёма сообщений
        System.out.println("Вы вышли из чата.");
    }

    /*
    Непрерывно принимает входящие сообщения от сервера и выводит их на экран.
    Каждая принятая строка также сохраняется в файле журнала.
    */
    private void receiveMessages(BufferedReader reader) {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                System.out.println(line);
                appendLog(line); // Добавляем входящее сообщение в лог
            }
        } catch (IOException ex) {
            if (!running) {
                System.out.println("Связь закрыта."); // Нормально завершилась связь
            } else {
                System.err.println("Ошибка при чтении сообщений: " + ex.getMessage());
            }
        }
    }

    // Дополняет файл журнала новой записью
    private void appendLog(String entry) {
        try {
            Path logPath = Paths.get(LOG_FILE_NAME);
            Files.writeString(logPath, entry + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Ошибка записи в лог-файл: " + ex.getMessage());
        }
    }

    /* Главная точка входа клиента. Чтение порта из файла настроек, установка имени пользователя и запуск
     процесса взаимодействия с сервером */
    public static void main(String[] args) throws Exception {
        String settingsFileName = "settings.txt"; // Файл конфигурации
        int port = readPortFromSettings(settingsFileName);
        String serverAddress = "localhost";
        String username = askForUsername();

        new ChatClient(serverAddress, port, username).run();
    }

    // Метод читает значение порта из файла настроек
    private static int readPortFromSettings(String filename) throws IOException {
        return Integer.parseInt(Files.readString(Path.of(filename)));
    }

    // Метод запрашивает имя пользователя, предназначенное для идентификации в чате
    public static String askForUsername() {
        System.out.print("Ваше имя: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            return br.readLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}