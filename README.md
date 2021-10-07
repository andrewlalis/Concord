# Concord
**NOTE: To improve organization, Concord has been moved to the [Concord Communication](https://github.com/Concord-Communication) GitHub organization.**

Open-source, independent, real-time messaging platform inspired by Discord. Here's what it can do:

- Host your own server, on your own hardware (no "cloud" servers). All the server's data is yours; do whatever you want with it, or nothing at all.
- Join someone else's server (with encryption between you and the server).
- Send and receive messages in real time, in different "channels" on a server.
- Send and receive private messages with other server members.

And here's what we have planned for the future:
- [ ] Threads: create a spin-off thread of messages from a single source message, to keep the main chat tidy.
- [ ] Voice communication, both in channels, and in private messages. The plan is to give all channels a configurable set of *capabilities*: by default all channels support text messages, but you can add voice chat support, streaming support, and more.
- [ ] Comprehensive server permissions system. We want to make it as easy as possible to configure the server's permissions to give admins fine-grained control over what users on their server can do. To this end, we plan on adding *roles* that can be assigned to particular users to grant them certain permissions.
- [ ] Emoji and formatted text support. React to a message with an emoji, and use markdown in your messages.
- [ ] File uploads: Allow users on your server to share files with each other as attachments to messages, with a high degree of configurability, so that you can decide how to deal with all the uploaded data.
- [ ] Plugins, bots, and automation! We want to make Concord as extensible as possible, so we plan to add first-class support for third-party plugins, and an API for developers to create bots that can participate in a server. We also want to provide admins with utilities for automating tasks without needing to know how to code (like auto-moderation, old data pruning, spam filters, etc.).
- [ ] Opt-in discovery services, so that users can find servers without needing an invitation.

# Concord Client

*Client application is currently work-in-progress.*

# Concord Server
To start up your own server, download the latest `concord-server.jar` JAR file from [the releases page](https://github.com/andrewlalis/Concord/releases) and run it with Java (version 16 or higher). The first time you run the server with `java -jar concord-server.jar`, it will generate a `server-config.json` configuration file, and a `concord-server.db` database file.

## Configuring the Server

You probably want to customize your server a bit. To do so, first stop your server by typing `stop` in the console where you started the server initially. Now you can edit `server-config.json` and restart the server once you're done. A description of the attributes is given below:

- `name` The name of the server.
- `description` A short description of what this server is for, or who it's run by.
- `port` The port on which the server accepts client connections.
- `acceptAllNewClients` Whether to automatically accept any new client that registers to this server. Set to false by default, meaning an administrator needs to approve any pending registration before it is complete.
- `chatHistoryMaxCount` The maximum amount of chat messages that a client can request from the server at any given time. Decrease this to improve performance.
- `chatHistoryDefaultCount` The default number of chat messages that are provided to clients when they join a channel, if they don't explicitly request a certain amount. Decrease this to improve performance.
- `maxMessageLength` The maximum length of a message. Messages longer than this will be rejected.
- `channels` Contains a list of all channels that the server uses. Each channel has an `id`, `name`, and `description`. **It is advised that you do not add or remove channels manually!** Instead, use the `add-channel` and `remove-channel` CLI commands that are available while the server is running.
- `discoveryServers` A list of URLs to which this server should send its metadata for publishing. Keep this empty if you don't want your server to be publicly visible.

## Server CLI

As mentioned briefly, the server supports a basic command-line-interface with some commands. You can show the commands that are available via the `help` command.

Each server uses a single [Nitrite](https://www.dizitart.org/nitrite-database/#what-is-nitrite) database to hold messages and other information.
