NetChess
========
NetChess - шахматное приложение для сетевой игры между несколькими игроками.

NetChess - a client-server application for Player vs Player games.

Картинки игры можно посмотреть в папке `/screenshots`.

ТЗ описано в файле `ТЗ 6.Game Server.doc`.

Рокировка = короля на две клетки в сторону.

####Технические детали:
Программа написана на Java 1.8.

Использованы следующие технологии: Netty, JavaFX, Maven, JUnit4, Slf4J.

Применены следующие шаблоны: Observer (classes InvitationManager, GameChangedObservable), Decorator (class ConnectionManager), Singleton.

####Для запуска:
Программа состоит из трёх проектов: серверной части (NetChessServer), клиентской части (NetChessClient) и общей библиотеки (NetChessCommon).

1) Откройте все 3 проекта в NetBeans (или другом IDE).

2) Общая библиотека (NetChessCommon project) подключается как Maven dependency. Поэтому соберите его командой `mvn clean install` из командной строки или средствами IDE для Maven.

3) Таким же образом соберите серверный и клиентский проекты. Если потребуется - укажите main классы.

Затем запустите сервер и несколько клиентов. По умолчанию ip-адрес настроен на 127.0.0.1 и порт 22222.

P.S.
Code Conventions http://www.oracle.com/technetwork/java/codeconvtoc-136057.html , единственное отличие, что открывающая скобка c новой строки.
