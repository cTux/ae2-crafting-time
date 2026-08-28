import argparse
import socket
import struct
import time
from pathlib import Path

from PIL import Image


HOST = "127.0.0.1"
PORT = 5905
KEYS = {
    "backspace": 0xFF08,
    "enter": 0xFF0D,
    "escape": 0xFF1B,
    "space": 0x20,
    "tab": 0xFF09,
}
SHIFTED = dict(zip('~!@#$%^&*()_+{}|:"<>?', "`1234567890-=[]\\;'.,/"))


def receive(sock, count):
    data = b""
    while len(data) < count:
        chunk = sock.recv(count - len(data))
        if not chunk:
            raise ConnectionError("VNC connection closed")
        data += chunk
    return data


def connect():
    sock = socket.create_connection((HOST, PORT), timeout=10)
    receive(sock, 12)
    sock.sendall(b"RFB 003.008\n")
    security_types = receive(sock, receive(sock, 1)[0])
    if 1 not in security_types:
        raise RuntimeError("VNC no-auth mode unavailable")
    sock.sendall(b"\x01")
    if receive(sock, 4) != b"\0\0\0\0":
        raise RuntimeError("VNC security handshake failed")
    sock.sendall(b"\x01")
    header = receive(sock, 24)
    width, height, name_length = struct.unpack(">HH16xI", header)
    receive(sock, name_length)
    return sock, width, height


def send_key(sock, keysym, down):
    sock.sendall(struct.pack(">BBHI", 4, down, 0, keysym))


def tap(sock, keysym):
    send_key(sock, keysym, 1)
    send_key(sock, keysym, 0)


def capture(sock, width, height, output):
    pixel_format = struct.pack(">BBBBHHHBBB3x", 32, 24, 0, 1, 255, 255, 255, 16, 8, 0)
    sock.sendall(b"\x00\x00\x00\x00" + pixel_format)
    sock.sendall(struct.pack(">BBHi", 2, 0, 1, 0))
    sock.sendall(struct.pack(">BBHHHH", 3, 0, 0, 0, width, height))
    if receive(sock, 1) != b"\x00":
        raise RuntimeError("Unexpected VNC message")
    rectangles = struct.unpack(">xH", receive(sock, 3))[0]
    image = Image.new("RGB", (width, height))
    for _ in range(rectangles):
        x, y, rect_width, rect_height, encoding = struct.unpack(">HHHHi", receive(sock, 12))
        if encoding != 0:
            raise RuntimeError(f"Unexpected VNC encoding {encoding}")
        pixels = receive(sock, rect_width * rect_height * 4)
        rectangle = Image.frombytes("RGB", (rect_width, rect_height), pixels, "raw", "BGRX")
        image.paste(rectangle, (x, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output)
    print(f"{output} {width}x{height}")


def click(sock, x, y, count):
    for index in range(count):
        sock.sendall(struct.pack(">BBHH", 5, 0, x, y))
        sock.sendall(struct.pack(">BBHH", 5, 1, x, y))
        time.sleep(0.05)
        sock.sendall(struct.pack(">BBHH", 5, 0, x, y))
        if index + 1 < count:
            time.sleep(0.2)


def type_text(sock, value):
    for character in value:
        base = SHIFTED.get(character, character.lower())
        shifted = character in SHIFTED or character.isupper()
        if shifted:
            send_key(sock, 0xFFE1, 1)
        tap(sock, ord(base))
        if shifted:
            send_key(sock, 0xFFE1, 0)


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="action", required=True)
    capture_parser = subparsers.add_parser("capture")
    capture_parser.add_argument("output", type=Path)
    click_parser = subparsers.add_parser("click")
    click_parser.add_argument("x", type=int)
    click_parser.add_argument("y", type=int)
    click_parser.add_argument("--count", type=int, choices=(1, 2), default=1)
    key_parser = subparsers.add_parser("key")
    key_parser.add_argument("name", choices=KEYS)
    text_parser = subparsers.add_parser("text")
    text_parser.add_argument("value")
    args = parser.parse_args()

    sock, width, height = connect()
    with sock:
        if args.action == "capture":
            capture(sock, width, height, args.output)
        elif args.action == "click":
            click(sock, args.x, args.y, args.count)
        elif args.action == "key":
            tap(sock, KEYS[args.name])
        else:
            type_text(sock, args.value)


if __name__ == "__main__":
    main()
