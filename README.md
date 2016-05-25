NetChess
========
NetChess - шахматное приложение для сетевой игры между несколькими игроками.

(NetChess - a client-server application for Player vs Player games)

Картинки игры можно посмотреть в папке `/screenshots`.

Техническое задание описано в файле `ТЗ 6.Game Server.doc`.

Рокировка = короля на две клетки в сторону.

####Технические детали:
Программа написана на Java 1.8.

Использованы следующие технологии: Netty, JavaFX, Maven, JUnit4, Slf4J.

Применены следующие шаблоны: Observer (`classes InvitationManager`, `GameChangedObservable`), Decorator (`class ConnectionManager`), Singleton.

####Для запуска:
Программа состоит из трёх проектов: серверной части (`NetChessServer`), клиентской части (`NetChessClient`) и общей библиотеки (`NetChessCommon`). Для удобства добавлен `NetChessParent` - мультимодульный Maven-проект.

1) Откройте проект `NetChessParent` в IDE (например, NetBeans).

2) Соберите `NetChessParent` средствами IDE для работы с Maven. Другой вариант - собрать из командной строки - перейдите в папку, где находится нужный pom.xml (`.../NetChess/NetChessParent/`), затем выполните команду `mvn clean install`.
При этом все три проекта соберутся в нужной последовательности (сначала соберётся общая библиотека).

3) Запустите сервер и несколько клиентов. Если потребуется - укажите main классы. По умолчанию ip-адрес настроен на 127.0.0.1 и порт 22222.

P.S.
Code Conventions http://www.oracle.com/technetwork/java/codeconvtoc-136057.html , единственное отличие, что открывающая скобка с новой строки.
