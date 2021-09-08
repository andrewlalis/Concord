# Concord Messaging Protocol
This document describes the messaging protocol that the Concord server will use to communicate with clients on persistent TCP connections, and over UDP for any lossy communications such as voice or video streaming.

## Messages
The basis of Concord's TCP messaging protocol is the concept of a **message**, whose only uniform aspect is that every message MUST begin with a single byte that defines the type of message. This is shown below as a `type id`.

There are some conventions which are generally observed when communicating certain complex data types in messages.

- Any `String` value is written as a 4-byte integer value defining the length of the string, followed by exactly that many bytes of content. A `null` string is written as a length of `-1`, without any bytes following it.
- Any enumeration value is written as the 4-byte integer ordinal value of the enumeration value, or `-1` if the value is `null`.
- Any `UUID` is written as two consecutive 8-byte long integers; the most significant bits are followed by the least significant bits. If the value is `null`, two consecutive `-1` values are written.
- A list of zero or more messages is written as a 4-byte integer value denoting the number of messages, followed by exactly that many messages. Only uniform lists of messages of the same type are supported.

The following sections provide information on the currently supported message types. Each section begins with a specification of the message payload's contents, including the message's `type id`, followed by a description of the message's purpose and usage.

### Identification
```
type id: 0
nickname: String
```
This message is sent by the client to the server immediately upon opening a TCP socket to the server.

### ServerWelcome
```
type id: 1
clientId: UUID
currentChannelId: UUID
currentChannelName: String
metaData: ServerMetaData
```
This message is sent by the server in response to an `Identification` message from a new client and after the client has been successfully registered as connected to the server. See the **ServerMetaData** message type for information about its structure.

### Chat
```
type id: 2
id: UUID
senderId: UUID
senderNickname: String
timestamp: long
message: string
```
This message can be sent by both the client and server. When sent by the client, it indicates that the client wishes to send a message in its current channel. The server may accept or reject this request. If accepted, the message will be added to the channel's history, and broadcast to all other clients in the channel. When sent by the server, this indicates that another client has sent a message in the channel that the receiving client is in, and that we should append this chat message to our local representation of the chat history. Additionally, chats are sent as part of a **ChatHistoryResponse**.

TODO: Add more message types.
