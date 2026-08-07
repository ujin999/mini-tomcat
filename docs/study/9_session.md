# Session
## 개념
### 1. Session
로그인 상태, 장바구니처럼 "여러 요청에 걸쳐 유지돼야 하는 정보"를 위해 클라이언트에게
고유한 식별자(세션 ID)를 하나 발급해주고, 클라이언트가 그 이후 요청마다 그 ID를 계속 다시 보내주면,
서버는 그 ID로 클라이언트에 대한 정보를 식별한다.

자바에서는 식별자를 담는 쿠키 이름의 관례가 "JESSIONID"이다.

### 2. 쿠키
브라우저는 서버가 보낸 쿠키를 저장해 두었다가, 이후 해당 서버로 요청을 보낼 때마다 자동으로
HTTP 요청 헤더에 담아서 그대로 돌려주는 역할을 한다.

## 설계
### 1. Request의 Cookie
Request 요청에서 Cookie는 다음과 같은 형태로 온다.

`Cookie: a=1; b=2; c=3;`

`headers.get("Cookie")`를 매번 파싱해서 a값은 1이구나 b값은 2구나를 매번 파악할 수 없다.

그래서 Request에 `Map<String, String> cookies` 값을 추가해서 쿠키를 쉽게 쓸 수 있도록 하였다.

### 2. Request의 Session
쿠키 안에 `Session Id(JESSIONID)`가 포함되어 있다.

Session Id를 분서하여 해당 요청이
어떤 클라이언트가 보낸 것인지 파악하기 위하여 `Session session` 필드를 하나 두어 Request에서
세션을 읽기 쉽게 만들었다.

매번 Session Id를 통해 세션 정보를 얻어오는 방법도 있지만, Request에 `Session` 필드를 두어
요청에 대한 세션을 개발자가 쉽게 사용할 수 있도록 설계하였다.

### 3. 세션 데이터의 동시성
같은 클라이언트는 계속 같은 Session을 보내는데 2개의 요청이 한번에 들어와서 다른 스레드에서 요청 처리를
하고 있다고 가정을 하자.

세션 데이터는 매 요청마다 여러 스레드가 동시에 읽고 쓰게 된다. 그래서 동시성 문제가 발생할 수 있기 때문에
일반적인 `HashMap<>`을 사용할 수 없고 `ConcurrentHashMap<>()`을 사용하여 `Race condition`을
예방해야 한다.

`ConcurrentHashMap<>()`은 `java.util.concurrent` 패키지 내에 있다.

### 4. 처음 방문에 대한 고려, 새로운 세션 생성
클라이언트가 서버에 처음 접속하면 Cookie 헤더 자체가 없을 수 있다. 그러면 서버는 새로운 세션을 생성하여
해당 클라이언트에게 Session Id를 제공하여 세션 정보를 가질 수 있게 해야 한다.

### 5. 세션 생성 시점과 ID 발급
```text
요청 파싱 (쿠키도 파싱하여 Request에 넣어야 함)
    ↓
세션 조회/생성  ← 라우팅보다 먼저!, 해당 세션이 있는지 조회 -> 있다면 발급 없다면 생성 후 기억
    ↓
router.route() / defaultServlet.service()  ← 이 안에서 세션 사용 가능해야 함
    ↓
(새 세션이었다면) 응답에 Set-Cookie 실어주기
```

### 6. 여러 세션들을 관리하는 Session Manager
Session은 요청이 사라지거나 끝나도 사라지면 안되고 다음 요청에도 이용해야 한다.

같은 클라이언트의 요청에 대해서는 같은 Session 정보를 조회해서 다음에도 이용해야 한다.

추후에는 만료된 세션에 대해서는 주기적으로 삭제처리 하여 메모리에 계속 쌓이는 것을 방지하면 된다.

근데 Session을 보관하는 저장소도 여러 스레드가 `map.put()`, `map.get()`, `map.remove()`를
이용하기 때문에 race condition이 발생할 수 있다. 따라서, 여기서 세션을 저장하는 저장소도
동시성에 대한 고려를 해야 한다.

`ConcurrentHashMap<String sessionId, Session>`

동시성 클래스를 이용하여 여러 스레드가 세션에 접근할 때 발생할 수 있는 동시성 문제를 해결할 수 있다.

### 7. 세션 오케스트레이션

#### 오케스트레이션
> OOP 클래스에서 오케스트레이션은 여러 하위 객체들을 가져와 전체 비즈니스 로직의 실행 순서와 흐름을 제어하는 역할

계산이나 데이터 처리는 하위 객체에 맡기고, 자신은 조율에만 집중한다.

역할
- 흐름제어
- 낮은 결합도
- 책임 분할
- 트랜잭션 관리

## 문제점
### 1. Null Pointer Exception 주의
```java
// HttpParser.parse()
Map<String, String> cookies = null;   // 기본값이 null
while (...) {
    if (tokens[0].equals("Cookie")) {
        cookies = parseCookie(requestLine);   // Cookie 헤더가 있을 때만 값이 채워짐
    }
    ...
}
```
브라우저가 처음 접속하면 Cookie 헤더 자체를 보내지 않는다.

그러면 cookies는 null인 채로 HttpReqeust에 저장된다.

```java
Map<String, String> cookies = request.getCookies();
String sessionId = cookies.getOrDefault("JSESSIONID", "");   // cookies가 null이면 여기서 NPE
``` 

### 2. 쿠키 이름 설정
요청에서 찾는 쿠키 이름과 응답에서 보내는 쿠키 이름이 서로 같아야 한다.

예를 들어 `JESSIONID=123`라고 응답을 보냈으면 클라이언트 측도 `JESSIONID=123`이라는 응답을 똑같이 보낸다.

### 3. 한번 보낸 쿠키는 기한이 만료되지 않는 이상 응답으로 보내지 않아도 된다.

### 4. 쿠키에 대하여 응답의 헤더 구조와 요청 헤더 구조가 다른다
요청에 대한 쿠키 헤더 구조는 다음과 같다.
`Cookie: a=1; b=2; c=3;`

응답에 대한 쿠키 헤더 구조는 다음과 같다.
```text
Set-Cookie: a=1
Set-Cookie: b=2
Set-Cookie: b=3
```