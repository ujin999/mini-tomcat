# ThreadPool

## 개념
### 1. ServerSocket
`ServerSocket serverSocket = new ServerSocket(8080)`을 실행하게 되면 다음과 같은 일이 발생한다.

1. socket(): 통신을 위한 소켓 창구를 생성한다.
2. bind(): 생성한 소켓에 특정 포트 번호를 바인딩한다.
3. listen(): 클라이언트 접속 요청을 받을 수 있는 대기 상태(Listening)로 전환하고,
연결 요청을 저장할 Queue(백로그 큐) 공간을 커널에 생성한다.

이후에 `Socket socket = serverSocket.accept()`를 통해서 큐에 쌓여 있는 연결 중
맨 앞의 것을 꺼내와(FIFO 방식) 실제 통신할 소켓을 반환한다.

`serverSocket.accept()`는 큐에서 꺼내오는 역할만 하므로 리소스를 많이 잡지 않는다.

### 2. Queue의 종류

ThreadPoolExecutor 의 작업 순서는 다음과 같다.
1. 현재 스레드 수가 corePoolSize보다 적다 → 무조건 새 스레드부터 만든다 (큐를 보지도 않음)
2. corePoolSize를 채웠다 → 큐에 넣어본다 (offer())
- 성공하면 그 작업은 큐에서 대기, 스레드가 비면 그때 처리
- 실패하면(큐가 꽉 찼거나, 애초에 못 받는 큐거나) 3번으로
3. 큐가 거부했다 → maximumPoolSize까지 새 스레드를 만들어본다
4. 그것도 꽉 찼다 → 거절 (RejectedExecutionException)

따라서 큐의 종류 선택을 신중히 해야 한다.

나 같은 경우에는 `SynchronousQueue`를 선택하여 큐를 사용하지 않는 경우가 발생하였다. 

```java
         큐 종류                       │ 큐 용량 │                       실제 동작                       │
├────────────────────────────────────────────────────┼─────────┼───────────────────────────────────────────────────────┤
│ SynchronousQueue                                   │ 0       │ 대기 없이 곧장 스레드 증설 → max 도달 시 거절         │
├────────────────────────────────────────────────────┼─────────┼───────────────────────────────────────────────────────┤
│ LinkedBlockingQueue<>()                            │ 무제한  │ 스레드는 core에서 안 늘고, 큐에 무한정 쌓임           │
├────────────────────────────────────────────────────┼─────────┼───────────────────────────────────────────────────────┤
│ LinkedBlockingQueue<>(N) / ArrayBlockingQueue<>(N) │ N       │ core로 처리 → 큐에 N개까지 대기 → max까지 증설 → 거절 │
└────────────────────────────────────────────────────┴─────────┴────────────────────────────────────────────────────
```
1. SynchronousQueue<>()
- 저장 용량이 0이라 넣고 바로 나오는 "즉시 전달"만 가능하다. 그래서 큐에 넣는 것 자체가 불가능하다.
그래서 사용 시 주의를 해야한다.
2. LinkedBlockingQueue<>()
- 기본 용량이 Integer.MAX_VLAUE라 maximumPoolSize를 설정했어도 큐에 무한대로 작업이 쌓이기 때문에
큐에 무한정 쌓인 메로리를 다 막아버릴 수 있따.
3. LinkedBlockingQueue<>(N) / ArrayBlockingQueue<>(N)
- 큐의 용량이 N으로 정해져있기 때문에 메모리에 작업들이 계속 쌓이지 않고
ThreadPoolExecutor에서 해당 스레드와 큐의 지정한 크기를 다 사용하였다면
사용자와의 요청을 받을 수 없다고 반환한다.


## 문제점
### 1. 단일 스레드
현재는 단일 스레드로 다음과 같은 작동 방식을 띄고 있다.
```text
Accept -> Parsing -> Routing -> Response -> Accept
```
즉 요청이 오면 해당 요청을 응답해줄 때까지 다른 소켓을 받지 못한다.

## 해결
### 1. 여러 스레드가 요청을 처리하도록 한다.
```java
this.threadPool = new ThreadPoolExecutor(
    10, 200, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(N)
);
```
현재 스레드 풀을 생성하여 각 요청처리를 스레드에서 응답하도록 한다.

`Executors.newFixedThreadPool()`로 고정된 스레드 풀을 생성할 수 도 있지만,
위와 같이 기본 스레드 10개 그리고 최대 스레드 200개로 스레드 풀을 생성하여
요청의 수에 따라 스레드 수를 늘렸다 줄였다 할 수 있다.

해당 방식은 현재 톰켓에서 구현하고 있는 방식이다.

### 2. Socket이 닫히는 문제점
```java
while (true) {
    try (Socket clientSocket = serverSocket.accept();){
        log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

        threadPool.execute(new ClientHandler(clientSocket, parser, router, defaultServlet, httpExceptionHandler));
    } catch (IOException e) {
        log.error("Failed to process client socket connection", e);
    }
}
```

현재 while 문 안에서 다음과 같이 Socket을 try-catch-resources를 이용하여 스레드에 넘겨주면
다음 while 문이 시작됨과 동시에 해당 socket은 닫히게 된다.

따라서 resources에서 자동으로 소켓을 닫지 않고 해당 소켓은 thread 내의 핸들러에서 닫도록 한다.

```java
while (true) {
        try {
            Socket clientSocket = serverSocket.accept();
            log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

            threadPool.execute(new ClientHandler(clientSocket, parser, router, defaultServlet, httpExceptionHandler));
        } catch (IOException e) {
            log.error("Failed to process client socket connection", e);
        }
    }
```

```java
// class ClientHandler
@Override
public void run() {

    try (
        Socket socket = this.clientSocket;
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
    ) {
```
클라이언트 핸들러에서 소켓을 닫도록 한다.
다음과 같이 주입 받은 clientSocket을 통해서도 try-catch-resources에 넣을 수 있다.