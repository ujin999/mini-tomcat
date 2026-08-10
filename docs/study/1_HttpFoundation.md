# HTTP Foundation

## Tomcat 이해
톰캣은 클라이언트의 HTTP 요청을 받아서 자바 코드를 실행하고, 그 결과를 다시 HTTP로 응해 주는 웹 애플리케이션 서버(WAS)

정적인 파일(HTML, CSS, 이미지)만 주고받는 일반적인 웹 사이트(Apach, Nginx 등)와 달리, 톰캣은 자바 실행 환경을 제공하여
동적인 데이터(DB 조회, 회원가입 등)를 처리할 수 있게 해준다.

> WAS(Web Application Server)는 HTTP 요청을 받아 비즈니스 로직을 수행하고, 동적인 웹 페이지나 데이터를 생성하여 응답하는 서버다. 정적 리소스를 제공하는 Web Server와는 차이가 있다.

### 1. HTTP 통신 및 파싱(Connector 역할)
브라우저와 톰캣 사이의 단순한 네트워크 연결과 데이터 통역을 담당한다.

1. 소켓 관리: 클라이언트의 소켓 연결(Accept)을 대기하고 연결을 수립한다.
   - 이때 톰캣은 클라이언트가 요청을 보낼 때 소켓 연결을 열고, HTML이나 JSON 데이터를 응답한 직후 소켓 연결을 바로 닫는다(Stateless).
2. HTTP 프로토콜 해석: 바이너리나 텍스트 상태로 들어온 HTTP 요청 메시지를 자바 객체(HttpServletRequest)로 변환한다. 반대로 자바가 처리한 결과는 HTTP 응답 메시지(HttpServletResponse)로 바꾸어 준다.
3. 스레드 풀 관리: 동시에 수많은 요청이 들어왔을 때, 서버가 다운되지 않도록 톰캣 내부적으로 스레드를 미리 만들어두고 효율적으로 담당한다.

### 2. 서블릿의 생명 주기 관리(Servlet Container / Catalina 역할)
개발자가 작성한 자바 클래스(서블릿)를 스스로 생성하고 소멸시키는 '관리자' 역할을 한다.
- 서블릿(servlet): 자바를 사용하여 웹 페이지를 동적으로 생성하고, 클라이언트의 HTTP 요청을 처리하는 자바 기반의 웹 컴포넌트
- 자바 언어 자체는 웹 서버가 아니기 때문에, 톰캣(Tomcat) 같은 웹 애플리케이션 서버(WAS)가 자바 코드를 실행해 웹 요청을 처리할 수 있도록 이어주는 규격

1. 서블릿 인스턴스 관리: 개발자가 매번 new MyServlet()을 호출하지 않는다. 톰캣이 켜질 때나 첫 요청이 올 때 서블릿 객체를 딱 한 번만 생성해서 메모리에 올려둔다.
2. 생명주기 메서드 호출: 서블릿이 태어날 때 init(), 요청이 올 때 service()(doGet/doPost), 서버가 꺼질 때 destroy() 메서드를 때에 맞춰 자동으로 실행해 준다.
3. 멀티스레딩 자원: 하나의 서블릿 객체를 두고, 요청이 들어올 때마다 새로운 스레드를 생성(혹은 스레드 풀에서 할당)하여 service() 메서드를 동시에 실행할 수 있도록 환경을 제공한다.

### 3. URL 라우팅 및 매핑(Context 역할)
클라이언트가 요청한 주소(URL)를 보고, 어떤 자바 코드를 실행해야 할지 올바른 길을 찾아준다.

- 예를 들어 사용자가 http://localhost:8080/login 으로 접속하면, 톰캣은 설정 정보(과거에는 web.xml, 현재는 @WebServlet 어노테이션)를 읽어두었다가 /login 주소와 연결된 LoginServlet 클래스를 매핑하여 실행한다.

## 개발할 톰캣의 구성
```text
                 Browser
                    │
                    ▼
            ServerSocket (8080)
                    │
                    ▼
               HttpServer
                    │
             accept(Socket)
                    │
                    ▼
               HttpParser
                    │
           HttpRequest 객체 생성
                    │
                    ▼
                  Router
          (/hello → HelloServlet)
                    │
                    ▼
            Servlet.service()
                    │
                    ▼
              HttpResponse
                    │
                    ▼
           HTTP 문자열 생성 후 전송
                    │
                    ▼
                 Browser
```

## 프로젝트 구조 만들기
```text
mini-tomcat
│
├── src
│   ├── server
│   ├── http
│   ├── servlet
│   ├── routing
│   ├── examples
│   └── Main.java
│
└── README.md
```

## 설계도 그리기
```text
Browser
    │
HttpServer
    │
HttpParser
    │
HttpRequest
    │
Router
    │
Servlet.service(request, response)
    │
HttpResponse
    │
Browser
```
```text
Browser (클라이언트)
  │
┌─▼────────────────────────────────────────────────────────┐
│ 1. Connector (연결 및 프로토콜 파싱)                     │
│   - ServerSocket (8080): 지정된 포트로 소켓 연결 대기    │
│   - HttpServer/Acceptor: accept(Socket)으로 연결 수락    │
│   - HttpParser: HTTP 프로토콜 데이터 파싱                 │
│   - HttpRequest / HttpResponse 객체 생성                │
└─┬────────────────────────────────────────────────────────┘
  │ (Request/Response 객체를 엔진으로 전달)
┌─▼────────────────────────────────────────────────────────┐
│ 2. Engine (Catalina - 서블릿 컨테이너 엔진)               │
│   - 알맞은 가상 호스트(Host)를 찾아 요청 라우팅            │
└─┬────────────────────────────────────────────────────────┘
  │ 
┌─▼────────────────────────────────────────────────────────┐
│ 3. Host (가상 호스트 - 예: localhost)                     │
│   - 요청된 URL 경로를 보고 적절한 웹 앱(Context) 지정     │
└─┬────────────────────────────────────────────────────────┘
  │ 
┌─▼────────────────────────────────────────────────────────┐
│ 4. Context (웹 애플리케이션 / WAR)                        │
│   - Router (서블릿 매핑 설정 분석: /hello)               │
│   - Target 발견: HelloServlet 인스턴스 매핑              │
│   - Servlet.service(Request, Response) 메서드 호출       │
└─┬────────────────────────────────────────────────────────┘
  │ (비즈니스 로직 수행 및 HttpResponse 객체에 결과 기록)
┌─▼────────────────────────────────────────────────────────┐
│ 5. Connector (응답 전송)                                 │
│   - HttpResponse 객체를 기반으로 최종 HTTP 문자열 생성     │
│   - 소켓 파이프라인을 통해 클라이언트로 데이터 전송       │
└─┬────────────────────────────────────────────────────────┘
  │
  ▼
Browser (화면 렌더링)
```
### 1. HttpServer
ServerSocket을 생성한 후 accept를 통해 클라이언트의 연결을 기다린 후
클라이언트의 TCP/IP 연결이 성공적으로 완료되면 Socket을 생성한다.

Socket의 InputStream과 OutputStream을 이용하여 요청과 응답을 처리한다.

### 2. HttpParser
InputStream에서 HTTP 요청 문자열을 읽는다.

HTTP Method, URI, Version, Header, Body를 파싱한다.

### 3. HttpRequest
method, uri, queryString, headers, body를 보관하는 객체이다.

### 4. Router
url의 주소를 확인하여 실행해야 할 서블릿을 찾고 연결한다.

### 5. HttpServlet
사용자가 요청한 비즈니스 로직이 들어있다.

### 6. HttpResponse
최종 응답 정보가 들어있는 객체이다.

## 클래스 목록 작성

### 1. Main
서버를 실행시키는 메인 클래스이다.

### 2. HttpServer
ServerSocket을 생성한 후 accept를 통해 클라이언트의 연결을 기다린 후
클라이언트의 TCP/IP 연결이 성공적으로 완료되면 Socket을 생성한다.

Socket의 InputStream과 OutputStream을 이용하여 요청과 응답을 처리한다.

### 3. HttpParser
InputStream에서 HTTP 요청 문자열을 읽는다.

HTTP Method, URI, Version, Header, Body를 파싱한다.

### 4. Router
URI를 기반으로 적절한 Servlet을 선택한다.

### 5. Servlet
요청을 받고 비즈니스 로직을 호출하고 Response를 작성한다.

### 6. HttpResponse
status, headers, body를 보관하는 객체이다.

## HttpServer 구현

### 1. 브라우저에서 localhost:8080을 입력하면 어떤 일이 일어날까?
http://localhost:8080/hello 를 입력하게 되면 HTTP 요청이 바로 이루어지지 않는다.

호스트(서버)와 클라이언트(브라우저)를 연결하는 TCP 연결이 이루어진 다음에 HTTP요청을 전송할 수 있다.
```text
브라우저

↓

TCP 연결 생성

↓

연결 성공

↓

HTTP 요청 전송
```

### 2. ServerSocket 이해
Tomcat은 `ServerSocket server = new ServerSocket(8080)`을 실행한다.

이 한줄은 서버는 8080 포트로 포트 바인딩 하는 것을 의미한다.
- 포트 바인딩: 애플리케이션이 특정 네트워크 포트와 연결하여 외부의 요청을 받아들일 수 있도록 대기(Listening)하는 상태
- 이제부터 8080 포트로 들어오는 연결 요청은 `ServerSocket server`로 들어오게 된다.

### 3. accept()
```java
while (true) {
    Socket socket = server.accept();
}
```
톰캣은 위 코드를 계속 실행하게 된다.

브라우저가 접속하면 요청이 올 때까지 기다렸다가 Socket을 생성한다.

### 4. Socket
소켓은 서버가 클라이언트 하나와의 연결을 의미한다.

브라우저와 서버가 연결되면 데이터를 주고받아야 한다.

Socket에는 `InputStream`과 `OutputStream`이 존재한다.
- Stream: 컴퓨터 프로그램은 파일, 네트워크, 메모리 등 다양한 대상과 데이터를 주고받아야 한다. 이때 대상의 종류와 상관없이 데이터를 주고받을 수 있도록 규격화한 가상의 연결 통로가 바로 스트림이다.
- OutputStream: 출력 버퍼에 쌓인 데이터를 패킷 쪼개기를 통해 잘개 나눈다. 그리고 쪼개진 패킷에 [출발지 IP/port, 목적지 IP/port, 순서 번호]가 적힌 헤더를 붙여 OS의 네트워크 카드(NIC)를 통해 밖으로 던진다.
- InputStream: 상대방이 보낸 패킷들이 내 컴퓨터의 네트워크 카드에 도착하고 뒤죽박죽 섞인 패킷들을 순서대로 조립하여 버퍼에 쌓는다. 그리고 입력 버퍼를 read하면 프로그램 메모리로 데이터가 올라온다.

### 5. HttpServer의 책임
```
1. ServerSocket 생성
2. 8080 포트 오픈
3. 무한 대기
4. accept()
5. Socket 획득
6. InputStream 생성
7. OutputStream 생성
8. Parser 호출
9. Router 호출
10. Servlet 실행
```

### 6. 과제
1. ServerSocket은 왜 하나만 존재해야 할까?
- ServerSocket은 특정 포트에서 연결을 기다리는 Listener로 하나만 존재할 수 있다. 하나의 포트에 여러개의 ServerSocket이 생성될 수 없다.
2. Socket은 왜 클라이언트마다 새로 생성될까?
- 클라이언트 마다 소켓을 연결해야 하기 때문에 클라이언트마다 생성된다.
3. accept()가 없다면 브라우저는 서버와 어떻게 연결될 수 있을까?
- accept()가 없으면 TCP 연결 자체가 성립되지 않는다. `accept()`를 하지 않으면 애플리케이션은 연결을 받아들이지 않는다.
4. InputStream과 OutputStream은 왜 ServerSocket이 아니라 Socket에 있을까?
- 클라이언트마다 데이터가 다르게 들어오고 나가야 되기 때문이다.

### 7. HttpServer 설계
```text
HttpServer

멤버 변수
- serverSocket
- port

메서드
- start()
    - HttpServer를 실행한다. serverSocket에서 accept 메서드를 실행한다.
- stop()
    - HttpServer를 중지한다. 종료에 필요한 작업을 진행한다.
```

## HttpParser

### 1. HttpRequest 설계
HttpRequest Class
```text
HttpRequest
1. member
- http method
- uri
- protocolVersion
- headers
- body

- user(브라우저의 요청에서는 user는 절대 알 수 없다. -> XXX)
```

### 2. 연습 문제
```text
POST /users?id=10 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
User-Agent: Chrome

{
  "name": "kim"
}
```

위의 String을 method, uri, protocolVersion, headers, body 부분으로 나누어 HttpRequest 객체 안에 들어갈 멤버 변수의 값을 작성해 보아라.
```text
method = "POST"
uri = "/users?id=10"
protocolVersion = "HTTP/1.1"
headers.contentType = "application/json"
headers.userAgent = "Chrome"

body = "
{
    "name": "kim"
}
"

```

### 3. HttpRequest 클래스
```java
public class HttpRequest {
   private String method;
   private String uri;
   private String protocolVersion;
   private Map<String, String> headers;
   private String body;
}
```
위의 형태보다 다음과 같이 method를 enum으로 적는 것이 더 좋다.

```java
public class HttpRequest {
   private HttpMethod method;
   private String uri;
   private String protocolVersion;
   private Map<String, String> headers;
   private String body;
}
```
enum을 사용하는 세 가지 이유가 존재한다.
1. 가장 중요한 이유는 컴파일 타임에 오류를 잡을 수 있다.
- HttpMethod.Get, HttpMethod.POST 등은 허용이 되고 HttpMethod.GEET과 같이 잘못된 입력을 하면 컴파일 자체가 되지 않아 오류를 잡을 수 있다.
- 문자열을 비교하지 않아도 된다. request.getMethod() == HttpMethod.GET이 허용이 된다.
- IDE의 도움을 받아 자동 완성이 가능하다.

### 4. HttpParser 설계
```java
class HttpParser {
    public HttpRequest parse(InputStream inputStream) {
       ...
    }
}
```
궁금했던 점
1. parse 메서드는 static이면 안될까?
  - parse가 static이 된다면 최대 Header 크기, 최대 Body 크기, 지원하는 HTTP Version, 문자 인코딩 같은 설정이 필요해질 수 있는데 해당 설정이 어렵게 된다.
  - static 메서드는 상대가 없고 설정도 필요 없고 확장할 일이 거의 없을 때 사용한다.
2. InputStream inputStream 을 생성자로 받으면 안될까?
   - 굳이 생성자로 받아서 HttpParser를 state하게 만들 필요가 없다. HttpParser는 단순히 InputStream을 HttpRequest라는 객체로 바꾸는 역할만 할 뿐이다.

### 5. HttpMethod
```java
public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
   ...;

   private final String value;
   private static final Map<String, HttpMethod> METHODS = new HashMap<>();
   HttpMethod(String value) {
      this.value = value;
   }

   static {
      for (HttpMethod httpMethod : HttpMethod.values()) {
         METHODS.put(httpMethod.getValue(), httpMethod);
      }
   }

   public String getValue() {
      return value;
   }

   // text와 일치하는 value에 맞는 enum으로 바꾸기 위한 함수
   public static HttpMethod fromString(String text) {
      return METHODS.get(text);
   }
}
```
1. static으로 클래스 메모리에 처음 로딩될 때 딱 한 번만 실행되는 특별한 코드 구역을 만들 수 있다. static 블록은 모든 상수가 이미 실행된 이후에 실행된다.
2. value 값으로 enum을 얻기 위해서는 새로운 함수나 Map이 필요하다.

### 6. HttpParser의 이해
Http Parser는 InputStream을 HttpRequest로 변환하는 역할을 한다.
즉, HTTP 문법을 읽어서 객체를 생성해내는 역할을 한다.
여기서 객체지향 설계에서 중요한 점은 역할을 잘 분리하는 것이다.
Http Parser의 역할은 문법이 틀렸는지 확인하고 HttpRequest를 정확히 생성해내는 것이다.
틀린 부분이 있다면 에러를 발생시켜 Server가 어떤 에러가 발생했는지 알려주는 역할을 한다.
