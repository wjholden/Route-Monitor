# Route-Monitor
This program uses RIPv2 to monitor the health of a network. The program is written in Java and does not require privileged access on Windows to run. (It may require privilege to listen on port 520 in UNIX).

![Supernet Monitor](https://pbs.twimg.com/media/DhAb8qDUcAAHdlm.jpg:large)

# Usage

Provide the supernets in dotted-decimal form as command-line arguments.

`java -jar Route_Monitor.jar [ip-address subnet-mask]*`

Example: `java -jar Route_Monitor.jar 10.0.0.0 255.255.0.0 172.16.0.0 255.255.0.0 192.168.0.0 255.255.0.0 10.128.0.0 255.255.0.0 172.31.0.0 255.255.0.0`

Any size supernet can be monitored but the design focuses on /16's.

Press 'f' to toggle fullscreen, 'q' to quit, and 'c' to clear the routing table.

# Configuration

The router must not advertise a default route. A 0.0.0.0/0 route will turn every pixel white. Disable automatic summarization. Use a [prefix list](https://www.cisco.com/c/en/us/support/docs/ip/interior-gateway-routing-protocol-igrp/9105-34.html) to deny the default route and permit everything else. This program silently ignores authentication. It might be a good idea to configure both [authentication](https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13719-50.html) and an inbound ACL to prevent malicious behavior.

`ip prefix-list NO-DEFAULT seq 5 deny 0.0.0.0/0
ip prefix-list NO-DEFAULT seq 10 permit 0.0.0.0/0 ge 1

access-list 1 deny any

router rip
 version 2
 no auto-summary
 distribute-list prefix NO-DEFAULT out
 distribute-list 1 in`
 
Authentication is silently 

# Gory details

The backing data structure is a custom binary trie.

The implementation includes a handful of non-obvious features to reduce resource usage. Foremost, the program memoizes the size of the trie and its subtries on each insertion. A Swing Timer is used to refresh the panels every 500 milliseconds. The panels are not ordinarily redrawn if the subtrie size has not changed, but there is a 1/10 random chance that it will be redrawn anyways. A callback-based architecture might improve performance even further but I wanted to keep the trie data structure untangled from the UI.

The second optimization is the use of subtries. Upon each panel refresh, the panel locates the subtrie holding the desired supernet, if any. If such a subtrie does not exist then the operation completes quickly. If it does then the program iterates over each IP address in the subtrie in in \Theta(m-p) where m is the IP address length and p is the supernet prefix length. This saves 2^(m-p) * (m-p) recursive calls. For a /16, that's 2^20 saved recursive calls for each panel repaint.

The RIP listener uses a basic thread. Methods in the binary trie are marked `synchronized`. I am not a concurrency expert; the `synchronized` keyword might not be an adequate safeguard, but since only one thread writes to the trie and only one thread reads from the tread the overall risk is low.

The RIP program purges routes in the trie learned >180 seconds ago every 10 seconds.
