NetChess
========

#### Description

NetChess - a client-server chess application for Player vs Player games.
Clients are represented as Desktop UI apps which connects to the multiplayer server.

You can see screenshots in `/screenshots` directory.

The technical task is described in file `ТЗ 6.Game Server ru.doc`.

Castling = move king 2 cells at side.

#### Technologies

Key technologies: Java SE, JavaFX.

Also were used: Netty, JUnit, Mockito, Maven.

#### How to run

The project consists of 4 modules: server (`NetChessServer`), client (`NetChessClient`), chess (`NetChessGame`) and common library (`NetChessCommon`).
`NetChessParent` - is a parent Maven-project.

1. Open `NetChessParent` in IDE (e.g., NetBeans).
2. Build `NetChessParent` using your IDE or Maven CLI commands (e.g., `mvn clean install`).
3. Run server and several clients. By default, the server starts on 127.0.0.1 and port is 22222.

P.S.
Code Conventions http://www.oracle.com/technetwork/java/codeconvtoc-136057.html
The only difference is that the opening brace starts from a new line.

##### Dates

It was written in Spring of 2016.
