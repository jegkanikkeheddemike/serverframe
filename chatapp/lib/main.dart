import 'dart:async';
import 'dart:io';

import 'package:async/async.dart';
import 'package:chatapp/loginpage.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Chatapp',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String? connectedUsers;
  List<String> messages = [];
  bool loggedIn = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: connectedUsers == null || !loggedIn
          ? Center(child: LoginPage(submitName: (name) {
              conn!.writeEvent("Connect", name);
              setState(() {
                loggedIn = true;
              });
            }))
          : Center(
              child: Column(
                children: [
                  Text("Chat! with $connectedUsers"),
                  TextField(
                    onSubmitted: (value) => conn!.writeEvent("PostMsg", value),
                  ),
                  ...messages.map((e) => Text(e))
                ],
              ),
            ),
    );
  }

  ServerConnection? conn;

  void updateUsers(String newUsers) {
    setState(() {
      connectedUsers = newUsers;
    });
  }

  @override
  void initState() {
    super.initState();
    () async {
      conn = await ServerConnection.connect("koebstoffer.info", updateUsers);
      conn!.addHandler("UpdateUsers",
          (newUsers) => setState(() => connectedUsers = newUsers));
      conn!.addHandler("PostMsg", (msg) => setState(() => messages.add(msg)));
    }();
  }
}

class ServerConnection {
  Socket socket;
  Map<String, void Function(String)> handlers = {};

  ServerConnection({required this.socket});

  static Future<ServerConnection> connect(
      String addr, void Function(String) updateUsers) async {
    Socket socket = await Socket.connect(addr, 9977);

    ServerConnection conn = ServerConnection(socket: socket);
    conn.run();
    return conn;
  }

  void addHandler(String key, void Function(String) handler) {
    handlers[key] = handler;
  }

  void run() async {
    StreamController<int> bc = StreamController<int>();
    StreamQueue<int> queue = StreamQueue<int>(bc.stream);

    socket.listen((event) {
      for (var byte in event) {
        bc.add(byte);
      }
    });

    Future<String> readString() async {
      List<int> buffer = List.empty(growable: true);
      while (true) {
        int character = await queue.next;
        if (character == "\n".codeUnitAt(0)) {
          break;
        }
        buffer.add(character);
      }

      return String.fromCharCodes(buffer);
    }

    while (true) {
      String command = await readString();
      if (command == "PING") {
        await readString();
        continue;
      }
      void Function(String)? handler = handlers[command];
      if (handler == null) {
        print("INVALID COMMAND: $command");
        continue;
      }
      handler(await readString());
    }
  }

  void writeEvent(String name, String msg) {
    socket.writeln(name);
    socket.writeln(msg);
  }
}
