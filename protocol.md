# Network Protocol
## Handshake
When a client running any version of [cwps-netlib](https://github.com/Fisch37/cwps-netlib) connects to a server,
it sends a `ClientHello` packet declaring which types of communications it is interested in.
These dictate solely what packets the server should send unprovoked (i.e. as a notify packet) and servers may receive
packets of any packet type and handle them without errors.
(Though they may opt to do nothing, if the client does not have the correct privileges for a packet.)

If the server does not support the networking protocol, it shall not send any further messages and the client should
consider this a handshake failure. 
If the server does support the protocol, it shall send a `ServerHello` packet, 
containing any information about the server state that the client is 1) entitled to receive, 
and 2) has requested via the `ChannelFlags`s set in its `ClientHello` packet.

```java
import java.util.SortedMap;

record ClientHello(EnumSet<ChannelFlag> channels) { }

record ServerHello(
        Optional<List<Waypoint>> accessibleWaypoints,
        Optional<List<Waypoint>> allWaypoints
) { }
```

- `ServerHello.accessibleWaypoints` is `Optional.empty()` if `ChannelFlags.WAYPOINTS` was not set in `ClientHello`, 
    otherwise it is an `Optional.of` with a list of all waypoints accessible to the user, 
    assuming they are not an admin.
- `ServerHello.allWaypoints` is `Optional.empty()` if `ChannelFlags.WAYPOINTS_ADMIN` was not set or the player does not
    have admin permission (in regard to waypoints), else it is an `Optional.of` 
    with a list of all waypoints on the server.

## Waypoints
```java
record WaypointUpdate(
        WaypointKey key, @Nullable Waypoint waypoint,
        boolean accessible
) { }
```
If either `ChannelFlags.WAYPOINTS` or `ChannelFlags.WAYPOINTS_ADMIN` are set for the client,
the server shall send a `WaypointUpdate` packet whenever a waypoint visible to the client as a result of either
`WAYPOINTS` or `WAYPOINTS_ADMIN` is added, renamed, changed, or removed.

`WaypointUpdate.key` and `WaypointUpdate.waypoint` behave in the following way to define the update that occurred:
- `waypoint == null`: The waypoint `key` has been removed.
- `waypoint != null && key.equals(waypoint.key)`: A normal update occurred.
    `accessible` may be false only if `ServerHello.allWaypoints` is not empty.
- `waypoint != null && !key.equals(key != waypoint.key)` The waypoint `key` has been renamed to `waypoint.key`

The packets `AllWaypoints` and `AccessibleWaypoints` exist, but remain unused.

## Teleport Requests
```java
record NewTeleportRequest(UUID from, UUID to, boolean isHere) { }
record TeleportEvent(UUID target, Action action) {
    enum Action { ACCEPT, DENY, CANCEL }
}
```
When a `tpa` or `tphere` occurs and the client has sent the `TELEPORT_REQUESTS` flag,
the server shall send a `NewTeleportRequest` packet. `to` will always be the UUID of the client.
`isHere` will be `true` if the request seeks to teleport `from` to `to`.

Clients may, at any time, send a `TeleportEvent` packet to respond to or manage their own requests.
An `action` of `ACCEPT` or `DENY` shall behave like `/tpaccept` or `/tpdeny` respectively 
if the current teleport request targeting the client, if any, originated from `target`.
The `CANCEL` action will behave like `/tpcancel` if the client has sent their own teleport request, and it is pending.