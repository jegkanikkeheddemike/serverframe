import 'package:flutter/material.dart';

class LoginPage extends StatelessWidget {
  final void Function(String) submitName;
  const LoginPage({super.key, required this.submitName});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        TextField(
          onSubmitted: submitName,
        ),
      ],
    );
  }
}
