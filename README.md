# Разработка Сетевого чата. 
Сетевой чат для обмена текстовыми сообщениями по сети с помощью консоли (терминала) между двумя и более пользователями (ссылка): [сетевой чат](https://github.com/KerillV/Coursework_Chat_Java).

Первое приложение - сервер чата (ChatServer) ожидает подключения пользователей.

Второе приложение - клиент чата (ChatClient) подключается к серверу чата и осуществляет доставку и получение новых сообщений.

Установка порта для подключения клиентов осуществляется через файл настроек settings.txt.

Сообщения, отправленные пользователем через сервер, логируются и записываются в file.log с указанием имени пользователя и времени отправки. За логирование отвечает метод appendLog в ChatServer.

### Пользование чатом:
- запускается сервер в ChatServer командой main;
- запускается клиент в ChatClient командой main;
- пользователь в терминале вводит свое имя;
- пользователь может писать сообщения (все сообщения логируются);
- выход из чата пользователя осуществляется командой /exit;
- при каждом запуске приложения файл логирования дополняется новыми логами.