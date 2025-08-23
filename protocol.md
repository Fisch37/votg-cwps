# Network Protocol
## Preface
The data protocol for this project should have no problems dealing 
with multiple instances of the communication library on the same client.
Any changes should be kept as backward compatible as possible, 
but if an incompatibility exists, the library should be able to detect
it before any problems can arise.

The network protocol shall be embeddable as a library into other mods and not be installable as its own mod.
This means multiple versions of the library may be communicating over one connection.
An individual embedded library that starts with its own runtime shall henceforth be referred to as an "instance".

The protocol shall be designed to allow for arbitrarily many instances to communicate over one connection 
and each instance must decide whether to propagate a received packet to its host mod.

## Negotiation Packets
### Server Protocol version
```java
record ServerProtocolVersion (
    int majorVersion,
    int minorVersion
) { }
```

On connection the server sends a packet with the major and minor version of its protocol.
The library should then compare this information to its own version information.
Further communication from this instance of the library should _only_
occur if 1. the major versions are identical, and 2. the minor version of the client is 
less than or equal to the server's minor version.

This negotiation is deliberately over-protective. 
A major version change may not mean an incompatibility exists with any of the features requested 
by this instance (see below), but regardless, no further handling of packets should occur by this instance 
and an error notice should appear.

### Requested Notifications
```java
record RequestedNotifications(
    EnumSet<ChannelFlag> channelFlags
) { }
```
Once the instance has confirmed its compatibility, it shall send a packet signalling 
which channels it would like to be notified over. The server will send information on a channel if 1.
the client is permitted to receive information on this channel, and 2. any of the RequestedNotifications
packets it has received during this session from this client have set its channel flag.

Instances should only propagate notifications to their host mod that have been requested through that instance.

#### Channels
```java
enum ChannelFlag {
    WAYPOINTS,
    ADMIN_WAYPOINTS,
}
```
- **WAYPOINTS**: Sends a list of all waypoints visible for that player when this flag is first set.
    Subsequently sends update packets whenever that list changes (add, remove, visibility)
- **ADMIN_WAYPOINTS**: Sends a list of all waypoints whether normally visible or not and corresponding change packets.
    Should send different packets than WAYPOINTS.