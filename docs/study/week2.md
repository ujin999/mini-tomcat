# 2주차
## Step 6. Servlet
### 1. Servlet의 필요성

```java
// HttpServer

while (true) {
    HttpRequest request = parser.parse(...);
    
    if (request.getUri().equals("/hello")) {
        ...
    }
}
```
위와 같이 uri가 `GET /hello` 한개만 있다면 상관 없다.

```java
GET /hello

GET /users

POST /users

DELETE /users

GET /login

POST /login

GET /board

...
```

다음과 같이 로직이 많아지게 되면 수없이 많은 if-else 문이 생겨나게 된다.
그래서 HttpServer는 클라이언트와 서버 애플리케이션 사이에서 HTTP 프로토콜 규격에 맞는 통신을 중재하는 역할만 하고,

비즈니스 로직은 다른 객체(Servlet)이 처리하도록 하여 역할을 명확히 분리할 필요가 있는 것이다.

### 2. Servlet 설계 과제

```java
interface HttpServlet {
    // 멤버 변수
    HttpResponse httpResponse; (x)
    HttpRequest httpRequest; (x)

    // 메서드
    public void init(Object config);

    public HttpResponse service();

    public void destroy();
    
    protected void doGet();
    
    protected void doPost();
    
    protected void doPut();
}
```
1. servlet은 하나의 서비스가 상속받아서 사용할 것이기 때문에 interface로 생성할 것 같다.
2. httpRequest는 생성자로 생성하여 service가 이해하게 할 것이기 때문에 하나의 멤버 변수로 두고
3. httpResponse는 반환값으로 반환하여 생성할 것이다.
4. 그리고 각 Http Method는 필요한 것만 오버라이딩 하여 사용할 수 있도록 하려고 한다.

### 3. Servlet 설계 과제 고칠점
1. `GET /hello` 요청이 1000번 들어온다면 HelloServlet은 1000개 생성할 수 없다. 싱글톤으로 하나만 생성해야 한다.
- `HttpRequest`와 `HttpResponse`를 상태로 저장하고 있다면 하나의 HelloServlet을 재활용할 수 없다.
2. `HttpResponse service()` -> `void service(...)`
- 빈 response를 생성하여 servlet이 채우는 것이 일반적이다.
3. service()는 Dispatcher(배차원) 역할
- service()의 `switch(method)`를 통해서 `doGet`, `doPost`, `doPut`을 고르는 문지기 역할을 한다.

### 4. 추천하는 최종 설계

```java
import java.net.http.HttpRequest;

public interface HttpServlet {

    void init(ServletConfig config);

    void service(HttpRequest request,
                 HttpResponse response);
    
    void destroy();
}
```

### 5. Servlet을 인터페이스가 아닌 추상 클래스로 만드는 이유
1. 기본적인 코드를 구현하여 405 에러를 기본적으로 제공한다.
- 사용자가 오버라이딩한 코드만 서비스를 할 수 있게 만든다.
2. protected를 강제하고 싶다. `doGet` `doPost`은 Servlet 내부에서만 사용하는 메서드이다.
- interface에서는 protected 설정을 할 수 없다.
3. Template Method Pattern
- HttpServlet의 `service()`는 절대 바뀌면 안되는 부분이고 `doGet()``doPost()` 부분은 변하는 부분이다.
- interface로 만들게 되면 개발자마다 `service()`의 구현은 변하게 된다. 그러나 Tomcat 입장에서는 service()의 동작은 모두 동일해야 한다.

## Step 7.HttpResponse

### 1. HttpResponse란
HTTP 응답 전체를 표현하는 개체이다.
`Status` `Headers` `Body` 등을 포함한다.

### 2. HttpResponse 설계
```java
class HttpResponse {

    String protocolVersion;

    int statusCode;

    String reasonPhrase;

    Map<String, String> headers;

    String body;

}
```

## Step 8. Router
### 1. Router란?
1. uri와 method를 확인한다.
2. 해당 uri와 method에 맞는 servlet을 호출한다.

### 2. Router 설계
```java
class Router {
    Map<String, Servlet> servletMap;
    
    public void route(Method method, String uri) {
    }
    
    public void register(Method method, String uri, Servlet servlet) {
    }
}
```

## *. Refactoring
### 1. Error Handling
```java
public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("HTTP Server started on port {}", port);
            while (true) {
                try (Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {
                    HttpRequest request = parser.parse(in);
                    log.info("Client connected:");

                    HttpResponse httpResponse = new HttpResponse();
                    router.route(request, httpResponse);

                    out.write(httpResponse.toBytes());
                    out.flush();
                } catch (IOException e) {
                    log.error("Failed to connect with client", e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to start HTTP server on port {}", port, e);
        } catch(HttpParseException e) {

            // 400 Bad Request 응답
            log.error("400 Bad Request: ", e);
        }


    }
```

두 가지 문제가 발생하게 된다.
1. 위와 같이 작성을 하게 되면 400 Bad Request에 대한 응답을 하지 못하게 된다.
2. 그리고 에러 발생 시 while(true) 문에서 튕겨 나가기 때문에 전체 서버가 다운되는 대참사가 일어난다.

```java
public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("HTTP Server started on port {}", port);
            while (true) {
                try (Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {
                    HttpRequest request;
                    HttpResponse response = new HttpResponse();

                    try {
                        request = parser.parse(in);
                        log.info("Client connected to: {}", request.getUri());

                        router.route(request, response);
                    } catch (HttpParseException e) {
                        log.error("400 Bad Request: Invalid HTTP Format", e);

                        response.setStatusCode(400);
                        response.setReasonPhrase("Bad Request");
                        response.getHeaders().put("Content-Type", "text/plain; charset=utf-8");
                        response.write("400 Bad Request");
                    }
                    out.write(response.toBytes());
                    out.flush();
                } catch (IOException e) {
                    log.error("Failed to process client socket connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Fatal: Failed to start HTTP server on port {}", port, e);
        }
    }
```

### 2. 변수를 사용하여야 하는 경우
```java
    elements.put("Method", tokens[0]);
    elements.put("Uri", tokens[1]);
    elements.put("ProtocolVersion", tokens[2]);
```
이 코드는 사용자가 `elements.get("method")`같이 오류에서 컴파일 에러가 발생하지 않아 오류를 잡기 어렵다.

컴파일 에러를 발생시키려면 다음과 같이 변수를 직접 사용하여 변수 사용자가 정확한 코드를 사용하게 하는 것이 중요하다.
```java
    String method, uri, protocolVersion;
    if (tokens.length == 3) {
        method = tokens[0];
        uri = tokens[1];
        protocolVersion = tokens[2];
```

## Step 8. Servlet Container
### 1. 현재의 문제점
```java
router.register(
    "GET",
    "/hello",
    new HelloServlet()
);
```
HttpServer가 직접 Servlet을 등록하는 과정을 겪고 있다.
- /hello를 알아야 한다.
- HelloServlet을 알아야 한다.
- Router 등록 방법을 알아야 한다.

HttpServer가 모든 사실을 알아야 한다. HttpServer의 책임이 점점 커지는 것이다. 

따라서 다음과 같은 ServletContainer가 등장하여 HttpServer는 ServletContainer만 알면 된다.
```java
                   Browser
                      │
                      ▼
                 HttpServer
                      │
                      ▼
             ServletContainer
                      │
             ┌────────┴────────┐
             ▼                 ▼
          Router         HelloServlet
```

### 2. ServletContainer 설계
```java
public class ServletContainer {

    private final Router router;

    public ServletContainer() {

    }

    /*
     * Servlet을 초기화하고 Router에 등록한다.
     */
    public void initialize() {

    }

    public Router getRouter() {

    }

}
```

### 3. 다음 수행 과제
ServletContainer를 아래 목표에 맞게 구현하기.
- Router를 생성한다.
- HelloServlet을 생성한다.
- HelloServlet의 init()을 호출한다. (아직 비어 있어도 괜찮습니다.)
- Router에 등록한다.
- getRouter()가 초기화된 Router를 반환한다.

## Step 9. Servlet 자동 등록
### 1. 필요한 이유
Spring에서 우리는 `@RestController` `@RequestMapping("/hello")`를 적지 register()를 등록하지 않는다.

하나하나 servlet을 등록하려면 시간이 오래 걸린다.

그리고 servlet이 늘어나면 늘어날수록 ServletContainer의 내부 코드에서 관리할 코드가 늘어난다. 

### 2. 필요 기술
이를 위해서 다음과 같은 기술이 필요하다.
- Reflection: String만으로 객체를 만들고 싶을 때 Reflection을 이용한다.
- Annotation: HelloServlet이 자신의 uri를 알고 있다면 훨씬 수월하게 코드를 수정할 수 있다.
- ClassLoader: Reflection은 클래스를 알아야 한다. 그런데 HelloServlet을 어떻게 찾을 수 있는지를 ClassLoader와 클래스 탐색을 이용한다.

### 3. java.lang.Class
클래스명 뒤에 붙는 `.class` 구문은 자바에서 클래스 자체의 메타 데이터를 담고 있는 "**객체**"를 뜻한다.

클래스 리터럴(Class Literal)이라고 부르며, JVM 내부의 java.lang.Class 객체를 참조하는 역할을 한다.

주요 역할과 용도
- 타입 정보 전달: 프로그램 실행 중(runtime)에 틀정 클래스의 구조 정보를 다른 메서드에 넘겨줄 때 사용한다.
- Reflection: 클래스의 이름, 생성자, 메서드, 필드 정보를 조회하고 제어할 때 사용한다.
- 제네릭 데이터 저장: 데이터 타입을 명시적으로 지정하여 컴파일러나 라이브러리가 올바른 타입을 인식하도록 돕는다.

### 4. 과제
HelloServlet.class

이 코드가 있다고 가정했을 때,

1. 왜 .class를 붙일 수 있을까요?
2. HelloServlet.class의 타입은 무엇일까요?
3. HelloServlet.class == String.class는 왜 가능한 비교일까요?
4. `String.class == String.class`는 true이고 `String.class == Integer.class`는 false인 이유

답변.
1. `.class`문법은 클래스의 메타데이터를 표현하는 Class 객체를 얻는 문법이다.
2. java.lang의 Class 타입이다.
3. 두 개는 결국 같은 Class 타입이라 비교가 가능하다.
4. 클래스 하나당 Class 객체가 단 하나만 존재한다. `String 클래스 정보`를 담고 있는 Class 객체는 단 하나뿐이다.

### 5. Reflection
컴파일러는 소스코드를 중간 언어(Bytecode)로 번역할 때, 프로그램 실행 코드뿐만 아니라 구조 정보도 함께 레코딩한다.
- 모든 클래스, 메서드, 필드, 오너테이션의 정보를 다 만들어 파일 내부의 특정 영역(Constant Pool)에 표 형태로 저장한다.
- Magic Number, Constant Pool, Access Flags, super/Interface, Fields / Method Table, Attributes 등의 정보를 파일에 저장한다.
- JVM은 클래스 파일을 읽어 메모리의 메서드 영역에 올린다. 이때 바이너리에 있던 메타데이터가 메모리 구조체로 변환된다.
- 개발자가 `Class.forName("User")` 등을 호출하면 런타임 엔진은 메모리에 이미 파싱되어 있는 해당 클래스의 메타데이터 주소를 찾아내어 Class객체 형태로 포장해 반환한다.

### 6. 런타임 중간에 객체를 생성하는 방법
```java
public HttpServlet createServletDynamically(String className) throws Exception {
    // 1. 문자열로 클래스 정보를 메모리로 로드
    Class<?> clazz = Class.forName(className);
    
    // 2. 런타임에 동적으로 객체 생성!
    Object instance = clazz.getDeclaredConstructor().newInstance();
    
    // 3. 앞서 배운 다형성을 활용해 부모 타입으로 캐스팅하여 반환
    return (HttpServlet) instance; 
}
```
1. Class.forName()
- 1. JVM 내부의 '클래스 로더(Class Loader)' 가동
- 2. 디스크의 classpath/bin 폴더에서 `HttpServlet.class` 파일 검색
- 3. 파일을 찾으면 바이트코드를 읽어 메모리에 적재

2. 생성자 주소 확보
- 1. `Constructor<?> ctor = clazz.getDeclaredConstructor()`를 통하여 객체를 조립할 "스위치(생성자)"를 찾아야 한다.
- 2. Metaspace에 올라간 클래스 정보 안에는 필드 목록, 메서드 목록과 함께 생성자 테이블이 배열 형태로 존재한다.
- 3. .getDeclaredConstructor() 함수를 실행하면, JVM 내부 C++ 코드로 작성된 테이블 탐색 로직을 돌려 매개변수 정보가 null인 생성자의 메모리 시작 주소와 메타데이터를 빼내어 `Constructor`라는 자바 객체로 포장해 리턴한다.

3. newInstance(): 실제 힙 메모리 할당
- 1. 클래스 설계도에 적힌 필드 크기를 모두 더해 정확히 이 객체가 차지할 바이트 크기만큼 Heap 영역의 빈 공간을 쪼갠다.
- 2. 객체 헤더 세팅: `HelloServlet` 설계도를 따른다는 마크 워드를 강제로 주입한다.
- 3. 생성자 코드 실행: 2번째 Constructor에서 찾아둔 생성자의 바이트코드 주소로 실행 흐름을 점프 시킨 후 생성자 코드를 실행한다.
- 4. 모든 조립이 끝난 힙 메모리의 시작 주소값을 자바 레이어로 던져준다.

### 7. 문제
```java
// 1. 문자열로 클래스 정보를 메모리로 로드
Class<?> clazz = Class.forName("java.lang.String");

// 2. 런타임에 동적으로 객체 생성!
Object instance = clazz.getDeclaredConstructor().newInstance();

// 3. 앞서 배운 다형성을 활용해 부모 타입으로 캐스팅하여 반환
return (HttpServlet) instance; 
```

### 8. 직접 객체 생성 없애기
```java
public void initialize() {

    HttpServlet helloServlet = new HelloServlet();

    helloServlet.init();

    router.register(GET, "/hello", helloServlet);

}
```

```java
new HelloServlet()
```
을 없애는 것이 목표다.

### 9. "com.example.minitomcat.HelloServlet" 문자 조차 없애자
이제 Reflection을 통해서 객체를 자동 생성할 수 있게 되었다. 하지만, 개발자는 여전히 `"com.example.minitomcat.HelloServlet"` 문자열을 입력해줘야 한다.

Reflection은 이미 알고 있는 클래스를 분석하는 기술이지, 자동으로 찾는 기능까지 존재하지는 않는다.

그래서 우리는 `Annotation`을 사용하여 해당 클래스가 가지고 있는 `uri`나 해당 클래스는 `servlet`이라는 정보를 저장한다.

그런 후에 ClassLoader를 사용하여 Servlet Container에 등록되어야 할 객체가 무엇인지 Annotation을 기반으로 파악한다.

### 10. Annotation
Annotation은 클래스에 붙이는 메모이다. 간단한 메모를 통해서 해당 클래스가 어떤 역할을 하는지, 책임은 무엇인지 등을 메모할 수 있다.

Annotation은 `.java`파일에서 컴파일 될 때 `.class`파일에 Annotation 정보를 포함하게 된다.
```java
@WebServlet("/hello")
public class HelloServlet {}
```
해당 코드가 컴파일되게 되면 `HelloServlet.class`에 클래스 정보, 메서드 정보, 필드 정보, Annotation 정보 까지 모두 저장되게 된다.

JVM이 해당 클래스를 읽으면 메모리로 로드하고 `Class<HelloServlet>` 객체를 생성한다.

Reflection은 Class 객체를 읽고 Annotation 목록을 확인한 후 WebServlet을 반견하면 ServletContainer에 등록한다.

Annotation은 하나의 인터페이스의 한 종류이다. (자바의 특별한 문법이 아님)

@WebServlet("/hello")을 생성하기 위해서 우리는 어떻게 만들었을까?
```java
class WebServlet {

    private String value;

    public WebServlet(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```
원래는 이렇게 생성자, 멤버 변수, 메서드를 만들어서 제공했을 것이다.

하지만, Annotation은 개발자가 생성하는게 아니다. 우리는 Annotation을 만들 때 `new WebServlet(...)`같은 행위를 한 적이 없다.
Annotation 객체는 컴파일러와 JVM이 대신 만들어 준다.
```text
.java

↓

컴파일

↓

.class

↓

JVM 로드

↓

Annotation 객체 생성
```
---
> Annotation은 데이터를 저장하는 객체가 아니라 속성(attribute)을 선언하는 인터페이스이다.
```java
public @interface WebServlet {

    String value();

    HttpMethod method();

}
```

#### 어노테이션이 멤버 변수를 가지지 않는 이유 

"이 Annotation은 value와 method라는 <**속성**>을 가진다"라고 선언한다.
- Annotation은 `인터페이스`의 일종이기 때문에 내부적으로 인터페이스를 상속받는 특별한 종류의 인터페이스로 컴파일된다.
- 자바의 인터페이스 내부에는 일반 멤버 변수를 둘 수가 없다.
- 따라서 규격을 정의하는 인터페이스 특성상, 값을 표현하는 수단으로 메서드(추상 메서드) 형식을 빌려 쓰게 되었다.


- Annotation은 상태(State)를 가지지 않는 순수 메타데이터이다. (런타임에 값을 동적으로 바꾸지 않는다.)
  - 상태란 멤버 변수의 값에 대한 변화를 의미한다.
- 컴파일러나 리플렉션 기술이 값을 읽어갈 때, 마치 객체의 Getter 메서드를 호출하듯이 값을 편하게 꺼내 쓰도록 유도하기 위해 메서드 형태로 호출 규격을 맞췄다.

- 컴파일 시점의 '기본값(default)' 매핑 메커니즘 때문이다.

#### JVM이 자동으로 만들어주는 것
```java
class WebServletProxy implements WebServlet {

    @Override
    public String value() {
        return "/hello";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return WebServlet.class;
    }
}
```
우리가 작성한 `@WebServlet("/hello")`를 읽어서 내부적으로 어노테이션 정보를 객체로 다룰 수 있게 도와준다.

다음과 같이 어노테이션을 단순한 메타데이터 텍스트로 보지 않고, 코드로 다룰 수 있는 하나의 객체로 만들어서 개발자에게 제공해야 한다.

이때 사용자가 작성하신 형태의 클래스를 매번 하드코딩으로 컴파일러가 만들어두면 파일 용량이 낭비되므로, JVM은 런타임에 `java.lang.reflect.Proxy`기술을 사용하여 메모리에 실시간으로 프록시 클래스를 띄운다.

#### Annotation의 세 가지 보관 정책
일반적인 어노테이션을 설정하면 런타임까지 어노테이션 정보를 보관하지 않아 일반적인 리플렉션으로 읽을 수 없다.

따라서 세 가지 보관 정책을 잘 설정해야 한다.
1. SOURCE
- `@Retention(RetentionPolicy.SOURCE)`
- .java -> 컴파일 -> 사라짐

2. CLASS
- `@REtention(RetentionPolicy.CLASS)`
- .java -> 컴파일 -> .class 파일 저장 -> JVM 로드 -> 사라짐

3. RUNTIME
- `@Retention(RetentionPolicy.RUNTIME)`
- .java -> 컴파일 -> .class -> JVM 로드 -> 메모리에 유지

### @Target
어노테이션을 직접 만들 때, 해당 어노테이션을 어디에 적용할 수 있는지 그 대상을 지정하는 메타 어노테이션이다.

- ElementType.Type: 클래스, 인터페이스, 열거형, 레코드 등에 붙일 수 있다.
- ElementType.METHOD: 메서드에만 적용 가능 (예: @Override, 스프링의 @GetMapping)
- ElementType.FIELD: 클래스의 멤버 변수(필드)에만 적용 가능 (예: 스프링의 @Autowired)
- ElementType.PARAMETER: 메서드의 매개변수에만 적용 가능 (예: @RequestParam)

### Annotation 사용
```java
Class<?> clazz = HelloServlet.class;

if (clazz.isAnnotationPresent(WebServlet.class)) {

    WebServlet annotation = clazz.getAnnotation(WebServlet.class);

    uri = annotation.value();

    log.info("Servlet container found uri: {}", uri);
}
```
위와 같이 어노테이션을 이용하여 uri 정보를 가져온다.

하지만, 아직도 `HelloServlet.class`라는 구현체를 알아야 한다.

아직까지는 자동등록이 아니다. 자동 등록이 되려면 모든 Servlet을 검색해서 등록해줄 수 있어야 한다.

### ClassLoader
`Class<?> clazz = UserServlet.class;` 이런 코드를 계속 추가하는 것이 아니라,
우리는 다음과 같은 코드를 만들고 싶다.
```java
for (Class<?> clazz : ???) {
    if (clazz.isAnnotationPresent(WebServlet.class)) {
        ...
    }
}
```

위의 코드에서 JVM이 `target/classes`를 어떻게 읽을까?

*ClassLoader*는 `.class`파일을 읽어서 `JVM`에 등록하고 `Class` 객체를 만든다.

그러면 ClassLoader는 프로젝트 안의 모든 클래스를 알 수 있을까?

이것은 틀린 대답니다.

왜냐하면 ClassLoader는 필요한 클래스만 로딩한다.

결국 `new HelloServlet()`을 한 적이 없다면 HelloServlet.class는 아직 JVM에 없을 수도 있다.

### ClassLoader 만으로는 자동 완성을 할 수 없다.
Spring은 config 파일에 있는 .class 파일을 먼저 탐색한다.

그리고 다음과 같은 방식으로 Class를 획득할 수 있다.
```java
프로젝트 디렉토리 탐색

↓

.class 파일 발견

↓

클래스 이름 생성

↓

Class.forName()

↓

Class 객체 생성

↓

Annotation 검사
```

### Reflection 먼저
처음 단계에서는 Reflection을 먼저 탐색하기 위해 ClassLoader는 나중에 구현한다.

다음과 같은 클래스를 사용해 일단은 리플렉션을 활용하여 Servlet을 추가하는 코드를 만든다.
```java
List<Class<?>> servletClasses = List.of(
        HelloServlet.class
);
```

```java
List<Class<?>> servletClasses = List.of(
    HelloServlet.class
);

for (Class<?> clazz : servletClasses) {
    if (clazz.isAnnotationPresent(WebServlet.class)) {

        try {
            // Read Annotation
            WebServlet annotation = clazz.getAnnotation(WebServlet.class);
            String uri = annotation.value();
            HttpMethod method = annotation.method();

            // Create Reflection
            if (!HttpServlet.class.isAssignableFrom(clazz)) {
                continue;
            }

            Object instance = clazz.getDeclaredConstructor().newInstance();
            HttpServlet servlet = (HttpServlet) instance;

            // init()
            servlet.init();

            // register()
            register(method, uri, servlet);

        } catch (Exception e) {
            log.error("Fail to create servlet instance", e);
            throw new RuntimeException("Fatal error: Server failure", e);
        }
    }
}
```
일단 오토 스캔은 하지 않고 Annotation 기반으로 서블릿을 등록하는 과정이다.

해당 과정 이후 다음에 오토 스캔을 도입할 예정이다.

### Tomcat이 Router에 HelloServlet.class를 저장하지 않고, HelloServlet 객체를 저장하는 이유

```java
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(...) {

    }

}
```
위와 같이 Tomcat 내부의 `Servlet`은 무상태이다. 즉, 멤버 변수가 존재하지 않는다.

톰캣은 멀티스레딩 방식으로 많은 Servlet을 한번에 돌리는데 상태가 있다면 경쟁 상태가 발생하게 된다.
> Servlet은 절대적으로 상태를 가지면 안 된다.
 
Router 내부에 `Map<String, Class<?>>`가 아닌 `Map<String, HttpServlet>`를 가지는 이유는
Class<?>로 저장되어 있다면 매번 객체를 생성해야 한다.

근데 Servlet은 애플리케이션 시작 시 한 번 생성되고, 모든 요청에서 재사용되어야 하기 때문에
HttpServlet으로 저장되는게 맞다.

그 이유는
1. 객체 생성 비용 감소(cold start, 매 요청 시 객체 생성 비용 감소)
2. init()을 한 번만 호출하면 됨
3. Servlet은 상태를 가지지 않는 객체로 설계되어 있기 때문
4. 요청마다 HttpRequest, HttpResponse를 전달받으므로 내부 상태를 저장할 필요가 없음
=> 굳이 Servlet을 매 요청마다 만들 필요가 없는 구조이다.

여기서 굉장히 중요한 말이 하나 있다.
> 상태를 가지지 않도록 설계한다면 싱글톤을 고려해라.