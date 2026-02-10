# Reliable File Transfer over UDP (TCP-like Features) — Java

📌 **Português:** veja [README_PT-BR.md](README_PT-BR.md)

---

# Reliable File Transfer over UDP (TCP-like Features) — Java

This project was developed for a **Computer Networks** course. It implements a simple **reliable file transfer** protocol **on top of UDP**, adding TCP-like ideas such as **sequencing, loss detection, and retransmission**.

The server splits a file into fixed-size chunks, adds a small header with a **sequence number**, and sends the packets via UDP. The client stores the packets, detects missing sequence numbers, requests retransmission, and finally rebuilds the file.

---

## Features (TCP-like over UDP)

- **Packet sequencing** (each data packet contains a sequence number)
- **Loss detection** (client checks which sequence numbers are missing)
- **Selective retransmission** (client requests only the missing packets)
- **Reassembly in order** (client writes the file in the correct sequence)
- **Timeout handling** (client stops waiting after a configured timeout)
- **Simple flow control** (server uses a small delay between sends to avoid bursts)

---

## Protocol (Application Layer)

### Message types

Client → Server
- `MEN` : request available files (menu)
- `GET<filename>` : request file download (examples: `GETfoto1`, `GETfoto2`)
- `FAL<seq>` : request a missing packet (seq formatted as 32 digits)

Server → Client
- `Disponíveis: ...` : response to `MEN`
- `ARQ<seq><data>` : data packet
- `FIM` : end of transfer
- `ERR` : file not found

### Packet format

- **Header size:** 35 bytes  
  - `ARQ` (3 bytes)
  - sequence counter (32 bytes, formatted)
- **Payload size:** 512 bytes
- **Total packet size:** 547 bytes

Sequence numbers are **1-based** in the header (the client stores them as 0-based indexes internally).

---

## Project Structure

- `UDPServer.java` — loads files, splits into packets, sends via UDP, and resends missing packets
- `UDPClient.java` — requests the file, receives packets, requests missing ones, and saves the result

---

## How to Run

### Requirements
- Java 8+ (works best with Java 11/17 as well)

### 1) Configure file paths

This project uses **hardcoded Windows paths**. Update them if needed:

**Server**
- Input folder (files to send):
  - `C:\Users\Afonso\Desktop\enviar\foto1.txt`
  - `C:\Users\Afonso\Desktop\enviar\foto2.txt`

**Client**
- Output file (download result):
  - `C:\Users\Afonso\Desktop\receber\download.txt`

> Tip: ensure the output folder exists, or the client will fail to save the file.

### 2) Compile

```bash
javac UDPServer.java UDPClient.java
```

### 3) Start the server

```bash
java UDPServer
```

### 4) Start the client (another terminal)

```bash
java UDPClient
```

The client will:
1. request the menu (`MEN`)
2. ask which file to download (`foto1` or `foto2`)
3. download, detect missing packets, request retransmission, and save to `download.txt`

---

## Testing Packet Loss

The server intentionally simulates packet loss by skipping packets **4 and 5** during the first send.
This helps validate the retransmission logic.

---

## Limitations / Possible Improvements

- Add **ACKs** and a **sliding window** (closer to real TCP)
- Add **checksums/CRC** for data integrity validation
- Improve the end-of-transfer logic (e.g., send total packet count)
- Support arbitrary filenames (not only `foto1` and `foto2`)
- Make paths configurable via CLI arguments instead of hardcoding

---

## License

Educational use.
