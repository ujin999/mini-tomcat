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