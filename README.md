# SDIS Project

SDIS Project for group T4G23.

Group members:

1. Carlos Lousada (up201806302@up.pt)
2. João Lemos (up201000660@up.pt)
3. José Rocha (up201806371@up.pt)
4. Tomás Mendes (up201806522@up.pt)

## How to Run

### Compile and start RMI

```
javac $(find . -name '*.java')
```
```
start rmiregistry
```

### Run

#### Peer

```
java peers.Peer <protocol_version> <peer_ap> <IP_address> <port> <Boot_Peer_IP_address> <Boot_Peer_port>
```

Eg.: java peers.Peer 1.0 peer1 localhost 8080 localhost 8080

#### Backup Protocol

```
java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>
```

Eg.: java TestApp localhost/peer1 BACKUP testfiles/ric2.png 3

#### Restore Protocol

```
java TestApp <peer_ap> RESTORE <file_path>
```

Eg.: java TestApp localhost/peer2 RECLAIM 0

#### Delete Protocol

```
java TestApp <peer_ap> DELETE <file_path>
```

Eg.: java TestApp localhost/peer1 DELETE testfiles/ric2.png 

#### Reclaim Protocol

```
java TestApp <peer_ap> RECLAIM <space>
```

Eg.: java TestApp localhost/peer2 RECLAIM 0

#### State

```
java TestApp <peer_ap> STATE
```

Eg.: java TestApp localhost/peer2 STATE
