# Concord
Console-based real-time messaging platform, inspired by Discord.

This platform is organized by many independent servers, each of which supports the following:
- Multiple message channels. By default, there's one `general` channel.
- Broadcasting itself on certain discovery servers for users to find. The server decides where it wants to be discovered, if at all.
- Starting threads as spin-offs of a source message (with infinite recursion, i.e. threads within threads).
- Private message between users in a server. **No support for private messaging users outside the context of a server.**
- Banning users from the server.

Each server uses a single [Nitrite](https://www.dizitart.org/nitrite-database/#what-is-nitrite) database to hold messages and other information.
