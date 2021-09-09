# Concord
Console-based real-time messaging platform, inspired by Discord.

## Vision

This platform will be organized by many independent servers, each of which will support the following:
- [x] Multiple message channels. By default, there's one `general` channel.
- [x] Broadcasting itself on certain discovery servers for users to find. The server decides where it wants to be discovered, if at all.
- [ ] Starting threads as spin-offs of a source message (with infinite recursion, i.e. threads within threads).
- [x] Private message between users in a server. **No support for private messaging users outside the context of a server.**
- [ ] Banning users from the server.
- [ ] Voice channels.
- [x] Persistent users. Connect and disconnect from a server multiple times, while keeping your information intact.


Here's a short demonstration of its current features:

https://user-images.githubusercontent.com/9953867/131097344-27cddf74-0cda-44e7-95d0-c9dc607291d6.mp4

# Concord Client

To use the client, simply download the latest `concord-client.jar` JAR file from [the releases page](https://github.com/andrewlalis/Concord/releases) and run it with Java (version 16 or higher) from anywhere on your machine.

Once you've started it, press **Enter** to click the button "Connect to Server". You will be prompted for the server IP address, and then a nickname for yourself, before you join the server. To disconnect, press **CTRL+C** at any time.

![concord_client_server_panel](https://user-images.githubusercontent.com/9953867/131096996-c9eec7e0-e8f0-4755-a14c-b81c727fba43.PNG)

Your console should look something like the above image. On the left, you have a list of channels, with a `*` next to the channel you're currently in. On the right, you have a list of all the other people that are in your channel. And of course in the middle you've got your chat, and a chatbox you can type messages in.

# Concord Server

To start up your own server, download the latest `concord-server.jar` JAR file from [the releases page](https://github.com/andrewlalis/Concord/releases) and run it with Java (version 16 or higher). The first time you run the server with `java -jar concord-server.jar`, it will generate a `server-config.json` configuration file, and a `concord-server.db` database file.

## Configuring the Server

You probably want to customize your server a bit. To do so, first stop your server by typing `stop` in the console where you started the server initially. Now you can edit `server-config.json` and restart the server once you're done. A description of the attributes is given below:

- `name` The name of the server.
- `description` A short description of what this server is for, or who it's run by.
- `port` The port on which the server accepts client connections.
- `chatHistoryMaxCount` The maximum amount of chat messages that a client can request from the server at any given time. Decrease this to improve performance.
- `chatHistoryDefaultCount` The default number of chat messages that are provided to clients when they join a channel, if they don't explicitly request a certain amount. Decrease this to improve performance.
- `maxMessageLength` The maximum length of a message. Messages longer than this will be rejected.
- `channels` Contains a list of all channels that the server uses. Each channel has an `id`, `name`, and `description`. **It is advised that you do not add or remove channels manually!** Instead, use the `add-channel` and `remove-channel` CLI commands that are available while the server is running.
- `discoveryServers` A list of URLs to which this server should send its metadata for publishing. Keep this empty if you don't want your server to be publicly visible.

## Server CLI

As mentioned briefly, the server supports a basic command-line-interface with some commands. You can show which commands are available via the `help` command. The following is a list of some of the most useful commands and a description of their functionality:

- `add-channel <name>` Adds a new channel to the server with the given name. Channel names cannot be blank, and they cannot be duplicates of an existing channel name.
- `remove-channel <name>` Removes a channel.
- `list-clients` Shows a list of all connected clients.
- `stop` Stops the server, disconnecting all clients.

Each server uses a single [Nitrite](https://www.dizitart.org/nitrite-database/#what-is-nitrite) database to hold messages and other information.
