import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* Класс реализует клиентскую сторону чата. Он соединяется с сервером, устанавливает имя пользователя
 и участвует в процессе обмена сообщениями */
public class ChatClient {
    // инициализация полей: адрес сервера, порт, имя пользователя и имя файла журнала
    private final String SERVER_ADDRESS;
    private final int SERVER_PORT;
    private final String USERNAME;
    private final String LOG_FILE_NAME = "file.log";

    // через конструктор примем аргументы для адреса сервера, порта и имени пользователя
    public ChatClient(String address, int port, String username) {
        this.SERVER_ADDRESS = address;
        this.SERVER_PORT = port;
        this.USERNAME = username;
    }

    // Метод устанавливает соединение с сервером и запускает обработку сообщений в отдельном потоке
    public void run() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            // Получаем потоки для чтения и записи данных от сервера
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            writer.println(USERNAME); // Отправляем серверу наше имя для авторизации

            executor.submit(() -> receiveMessages(reader)); // создаётся фоновый поток для приёма сообщений от сервера

            /* Циклично читает пользовательские сообщения и отправляет их на сервер. Цикл продолжается до
             тех пор, пока пользователь не ввёл пустую строку или команду /exit. */
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (!(line = userInput.readLine()).trim().isEmpty() && !"/exit".equals(line)) {
                writer.println(line);
            }

            // закрываем соединение и останавливаем обработчики сообщений
            System.out.println("Вы вышли из чата.");
        } finally {
            executor.shutdown();
        }
    }

    /* Методом receiveMessages() непрерывно принимается каждое входящее сообщение от сервера и выводится
     на экран. Каждая принятая строка также сохраняется в журнале */
    private void receiveMessages(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                appendLog(line); // Добавляем входящее сообщение в лог
            }
        } catch (IOException ex) {
            System.err.println("Ошибка при чтении сообщений: " + ex.getMessage());
        }
    }

    // Метод дополняет файл журнала новой записью
    private void appendLog(String entry) {
        try {
            Files.writeString(Path.of(LOG_FILE_NAME), entry + "\n", StandardOpenOption.APPEND);
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
    private static String askForUsername() {
        System.out.print("Ваше имя: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            return br.readLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
