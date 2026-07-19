# ChatApp

A Discord-inspired desktop chat application written in **Java 25**, using **JavaFX**
for the UI, **MySQL** for persistence, and raw **Java Sockets** (newline-delimited
JSON) for real-time communication. Built as a **Maven multi-module** project with a
clean client/server/shared split.

## Features

- User registration and login (BCrypt password hashing)
- Friends system: send/accept/decline requests, remove friends, live friends list
- Presence: Online, Idle, Do Not Disturb, Offline — broadcast to friends in real time
- Direct messages: real-time delivery, timestamps, typing indicators, persisted history
- Servers ("guilds"): create, join via invite code, leave, custom icon/name
- Text channels inside servers, with real-time channel chat and history
- Discord-style layout: server rail, channel/DM sidebar, chat area, members panel
- Modern dark theme (JavaFX CSS) with rounded corners and hover states

## Architecture

```
chatapp/
├── pom.xml                # parent POM (dependency management)
├── shared/                # protocol + DTOs used by both client and server
│   └── src/main/java/com/chatapp/shared/
│       ├── protocol/      # Envelope, MessageType, payload classes
│       ├── model/         # UserDto, ServerDto, ChannelDto, ChatMessageDto, ...
│       └── util/          # JsonUtil (Gson wiring)
├── server/                # socket server + MySQL backend
│   └── src/main/java/com/chatapp/server/
│       ├── config/        # ServerConfig (reads config.properties)
│       ├── db/            # ConnectionPool + dao/ (UserDao, FriendDao, ServerDao, ...)
│       ├── service/       # business logic (AuthService, FriendService, GuildService, ...)
│       ├── net/           # ChatServer, ClientHandler, SessionManager
│       └── Main.java      # composition root
└── client/                # JavaFX desktop client
    └── src/main/java/com/chatapp/client/
        ├── controller/    # LoginController, RegisterController, MainController
        ├── net/           # SocketClient
        ├── model/         # AppState (shared observable session state)
        ├── ui/             # UiFactory (avatars, presence dots)
        ├── util/          # ClientConfig, SceneManager
        └── Main.java       # JavaFX Application entry point
```

This follows an **MVC** split: the `model` (DTOs in `shared`, `AppState` in the
client), the `controller` layer (JavaFX controllers on the client, `ClientHandler`
+ services on the server), and the `view` (FXML + CSS). The server itself is
further layered into **DAO → Service → Net**, so persistence, business rules,
and protocol handling stay independent and testable.

### Wire protocol

Every socket message is a single line of JSON (newline-delimited), deserialized
into an `Envelope { type: MessageType, payload: <json>, requestId }`. Client
requests and server responses/pushes share the same `MessageType` enum
(see `shared/protocol/MessageType.java`) so the whole protocol is discoverable
from one file.

## Prerequisites

- **JDK 25** or newer
- **Maven 3.9+**
- **MySQL 8.0+** (a local server or Docker container)

## 1. Set up the database

```bash
mysql -u root -p < server/src/main/resources/schema.sql
```

This creates the `chatapp` database and all tables. Then create an application
user (or reuse an existing one) and grant it access:

```sql
CREATE USER 'chatapp_user'@'localhost' IDENTIFIED BY 'chatapp_password';
GRANT ALL PRIVILEGES ON chatapp.* TO 'chatapp_user'@'localhost';
FLUSH PRIVILEGES;
```

## 2. Configure the server

Edit `server/src/main/resources/config.properties` (or copy it elsewhere and
pass its path via `-Dchatapp.config=/path/to/config.properties` at runtime):

```properties
db.url=jdbc:mysql://localhost:3306/chatapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user=chatapp_user
db.password=chatapp_password
db.pool.size=10

server.port=5555
```

## 3. Build the project

From the repository root:

```bash
mvn clean install
```

This builds `shared`, `server`, and `client` in order (the parent `pom.xml`
declares the module order).

## 4. Run the server

```bash
./run-server.sh
# or directly:
java -jar server/target/chatapp-server.jar
```

You should see `ChatApp server listening on port 5555` in the console.

## 5. Run the client

Edit `client/src/main/resources/config.properties` if the server isn't on
`localhost:5555`, then:

```bash
./run-client.sh
# or directly:
mvn -pl client -am javafx:run
```

Launch multiple client instances (just run the command again in another
terminal) to chat between two accounts.

## Using the app

1. **Register** a couple of accounts from the login screen.
2. Log in as one user, click the **+** above the friends list to send a friend
   request by username, then log in as the other user (a second client
   instance) to accept it.
3. Click a friend to open a **direct message** — try typing in both windows to
   see the typing indicator.
4. Click the **+** at the bottom of the server rail to **create a server**;
   you'll get an invite code. Use **Join a server** from another account to
   join it with that code.
5. Inside a server, use the **+** next to the channel list to add more text
   channels.

## Notes on design choices

- **Connection pooling** is a small hand-rolled `ArrayBlockingQueue`-based pool
  (`ConnectionPool`) rather than a third-party library, to keep the server's
  dependency footprint minimal.
- **Symmetric friendship** rows (both `A→B` and `B→A` in the `friends` table)
  keep "get my friends" a single simple `SELECT`, at the cost of a little
  write-side duplication when a request is accepted or a friendship removed.
- **One thread per client connection** (via `Executors.newVirtualThreadPerTaskExecutor()`)
  keeps `ClientHandler` code straightforward (blocking reads) while scaling
  comfortably thanks to Java 25 virtual threads.
- Messages for both **direct messages and channel messages** share one
  `messages` table (`receiver_id` xor `channel_id`), avoiding duplicated
  history/pagination logic in `MessageDao`.

## Troubleshooting

- **`Unable to connect to MySQL`** on server startup: double-check
  `db.url` / `db.user` / `db.password` in `config.properties` and that MySQL is
  running and reachable on that host/port.
- **Client shows "Could not reach server"**: make sure the server is running
  and that `client/src/main/resources/config.properties` points at the right
  host/port.
- **JavaFX runtime errors when running the built jar directly**: use
  `mvn -pl client -am javafx:run` (or the `run-client.sh` script), which wires
  up the JavaFX modules for you; running `client/target/chatapp-client.jar`
  directly with plain `java -jar` requires a JavaFX runtime on the module path.
