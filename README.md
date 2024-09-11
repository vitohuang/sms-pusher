# SMS Pusher

## Description

SMS Pusher is an Android application designed to read incoming SMS messages and forward them to a specified remote server via HTTP POST requests. This app is useful for situation when you want to get notified about incoming SMS messages on your old phone (special if need two factor authentication code, having a different phone number, etc). The app ensures that SMS permissions are handled properly and provides a user-friendly interface for configuration.

## Features

- Reads incoming SMS messages.
- Forwards SMS data (sender and message body) to a specified endpoint, the server implementation is available here: https://github.com/vitohuang/SMS-Receiver.
- User-friendly interface for setting the endpoint URL and authorization token.
- Logs the status of sent messages and any errors encountered.

## Prerequisites

- Android Studio installed on your machine.
- Basic knowledge of Android development and Kotlin.

## Building the APK

To build the APK for the SMS Pusher app, follow these steps:

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd SMSPusher
   ```

2. **Open the Project**:
   Open the project in Android Studio.

3. **Configure the Project**:
   Ensure that the project is configured with the correct SDK versions. You can check this in the `build.gradle` files.

4. **Build the APK**:
   - In Android Studio, go to the menu bar and select `Build`.
   - Click on `Build Bundle(s) / APK(s)`.
   - Select `Build APK(s)`.
   - Once the build is complete, you will see a notification with a link to locate the APK.

5. **Install the APK**:
   You can install the APK on your device by transferring it via USB or using an emulator.

## Permissions

The app requires the following permissions to function correctly:

- `SEND_SMS`: To send SMS messages (if needed).
- `RECEIVE_SMS`: To receive incoming SMS messages.
- `READ_SMS`: To read the content of incoming SMS messages.
- `INTERNET`: To make HTTP POST requests to the remote server.

Make sure to grant these permissions when prompted.

## Usage

1. Launch the app and enter the endpoint URL where you want to send the SMS data.
2. Provide the authorization token if required by the server.
3. The app will automatically listen for incoming SMS messages and forward them to the specified endpoint.

## Logging

The app maintains a log of all sent messages and any errors encountered during the process. You can view these logs within the app interface.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.
