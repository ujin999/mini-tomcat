"""
Opens many TCP connections to the server and sends nothing on any of them,
holding them open — simulates a Slowloris-style attack that ties up worker
threads blocked in HttpParser.parse()'s read() call.

Usage: python3 bench/slowloris.py [num_connections] [hold_seconds]
"""
import socket
import sys
import time

HOST = "localhost"
PORT = 18080
NUM_CONNECTIONS = int(sys.argv[1]) if len(sys.argv) > 1 else 320
HOLD_SECONDS = int(sys.argv[2]) if len(sys.argv) > 2 else 40

sockets = []
for i in range(NUM_CONNECTIONS):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, PORT))
        sockets.append(s)
    except OSError as e:
        print(f"connection {i} failed: {e}")
        break

print(f"[slowloris] opened {len(sockets)} silent connections, holding for {HOLD_SECONDS}s")
time.sleep(HOLD_SECONDS)

for s in sockets:
    try:
        s.close()
    except OSError:
        pass
print("[slowloris] closed all connections")
