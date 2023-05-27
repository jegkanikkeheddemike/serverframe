import 'dart:async';
import 'dart:io';

import 'package:async/async.dart';
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
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String connectedUsers = "";
  List<String> messages = [];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            TextField(
              onSubmitted: (value) {
                conn!.writeEvent("Connect", value);
              },
            ),
            const Text("Connected users:"),
            Text(connectedUsers)
          ],
        ),
      ),
    );
  }

  _ServerConnection? conn;

  void updateUsers(String newUsers) {
    setState(() {
      connectedUsers = newUsers;
    });
  }

  @override
  void initState() {
    super.initState();
    () async {
      conn = await _ServerConnection.connect("10.0.0.28", updateUsers);
      conn!.addHandler("UpdateUsers", (newUsers) {
        setState(() {
          connectedUsers = newUsers;
        });
      });
    }();
  }
}

class _ServerConnection {
  Socket socket;
  Map<String, void Function(String)> handlers = {};

  _ServerConnection({required this.socket});

  static Future<_ServerConnection> connect(
      String addr, void Function(String) updateUsers) async {
    Socket socket = await Socket.connect(addr, 9977);

    _ServerConnection conn = _ServerConnection(socket: socket);
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
