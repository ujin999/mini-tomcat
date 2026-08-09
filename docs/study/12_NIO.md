# NIO
## 개념
### 1. ServerSocketChannel
클래식|NIO|
---|-------------------|
ServerSocket(듣고 있는 소켓)|ServerSocketChannel |
Socket|SocketChannel|
serverSocket.accept() -> Socket|serverSocketChannel.accept() -> SocketChannel|

ServerSocket과 ServerSocketChannel의 역할은 같다.

둘의 차이는 논블로킹 모드를 쓸 수 있는지 없는지와 같다.

`serverSocketChannel.configureBlocking(boolean default true)`로 논블로킹/블로킹 모드를 전환할 수 있다.

`serverSocketChannel.accept -> SocketChannel`이고

`SocketChannel.socket()`을 통해 클래식 Socket 객체를 꺼낼 수도 있다.

### 2. SocketChannel에 OutputStream이 없는 이유
`OutputStream`은 내가 요청한 만큼은 어떻게든 다 처리해준다.
- 모든 데이터를 다보냈거나
- 아니면 예외가 발생했거나
- 두 가지 상태밖에 존재하지 않는다.

그러나 SocketChannel(nonblocking 모드)에 write(ByteBuffer)를 호출한 후 해당 API는
중간 단계를 표현할 수 있다.
- 몇 바이트를 현재 보냈다.
- 내가 보낸 데이터랑 비교해서 다 보냈는지 확인할 수도 있고 아직 버퍼에 남아있는지 확인할 수 있다.

### 3. SocketChannel.write(ByteBuffer)
`outputStream.write(byte[])`랑 차이가 존재한다.

byte[]는 그냥 byte 배열이므로 내부적으로 지금까지 얼마나 처리되었는지 기억하는 기능이 없다.

ByteBuffer는 position(어디까지 처리했는지)이랑 limit(어디까지 처리해야 하는지)를 기억하고 있어서
부를 때마다 실제로 전송된 만큼 position이 자동으로 앞으로 이동한다.

그래서 만약 재시도 로직을 짰다면, 같은 buffer 객체를 그대로 다시 넘기기만 하면 직전에 보낸 position을
버퍼가 기억하고 있어서 이어서 보낼 수 있다.

## 문제점
### 1. ExceptionHandler()가 응답을 할 때까지 accept()할 수 없다.
```java
 try {
    new HttpThreadPoolExceptionHandler().handle(clientSocket);
    clientSocket.close();
} 
```

`handle()` 안에는 다음과 같은 코드가 들어있다.

```java
out.write(response.toBytes());
out.flush();
```

- out.write(): 데이터를 메모리 버퍼에 복사하는 과정이다. <- 리소스가 별로 들지 않는다. response 고작 몇 백 바이트
- out.flush(): 메모리 버퍼에 쌓여있던 데이터를 OS의 네트워크 소켓 버퍼로 밀어내고, 물리적인 네트워크 선을 통해 전송을 시작한다.
  - 여기서 위의 `new HttpThreadPoolExceptionHandler().handle(clientSocket)`에서 블로킹이 일어난다.
  - 버퍼의 데이터가 실제 네트워크로 완전히 출발(혹은 OS 버퍼로 이동)할 때까지 프로그램이 블로킹 상태가 된다.
  - 근데 공격자 입장에서 receive widow 크기를 0으로 설정해버리면 TCP 송신 버퍼과 꽉차면서 서버도 마비된다.
  - 위 상황을 막기 위해 NIO 넌블로킹으로 그냥 받든 말든 보내고 끝내버린다.
  - 통신이 연결된 소켓만 빠르게 ClientHandler로 넘겨주기 위해 ExceptionHandler는 그냥 NIO로 처리한다.

## 해결
### 1. java.nio.channels.ServerSocketChannel / SocketChannel
```java
try {
    new HttpThreadPoolExceptionHandler().handle(clientSocket);
    clientSocket.close();
} 
```
위 상황은 최대 스레드 수를 넘고 큐까지 예약이 꽉찼을 때 지금 서버에 접근을 못하는 것을 알리는 코드이다.

이 상황에서 제대로 된 응답을 할 필요는 없다.

그래서 클라이언트가 응답을 제대로 받든 말든 한번 NIO로 보내주고 `HttpServer`는 계속해서 accept()하고
받는 역할을 해서 ThreadPool에 넘겨주기만 하면 된다.