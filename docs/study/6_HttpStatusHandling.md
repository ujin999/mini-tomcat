# Http Status Handling
## Exception
```text
Throwable
 ├── Error (시스템 레벨 심각한 문제, 보통 안 잡음)
 └── Exception
      ├── RuntimeException (= Unchecked Exception)
      │    ├── NullPointerException
      │    ├── ArrayIndexOutOfBoundsException
      │    ├── IllegalArgumentException
      │    └── ClassCastException 등
      └── 그 외 Exception (= Checked Exception)
           ├── IOException
           ├── SQLException
           └── ClassNotFoundException 등
```
두 Exception의 차이는 컴파일러의 강제성에 달려있다.

### 1. Exception: Checked Exception
- 컴파일러가 `throws` 선언이나 `try-catch` 처리를 반드시 강제함
- `IOException`, `SQLException`과 같이 "일어날 수 있는" 외부 요인 문제
### 2. RuntimeException: Unchecked Exception
- 컴파일러가 처리를 강제하지 않는다. (`try-catch`/`throws` 없어도 컴파일 됨)
- 대게 프로그래머의 버그를 나타냄 (null 체크 누락, 배열 범위 초과, 잘못된 형변환 등)

## 현재의 문제
### 1. RouteException
`Router.rout()`에는 다음과 같은 코드가 있다.
```java
if (routingTable.containsKey(key)) {
    throw new RouteException("This routing address is already registered: " + key);
}
```
해당 RouteException을 클라이언트가 알아야 할까?(에러 상태 코드를 받음)

이 부분은 사실 서버의 `initialize()` 문제이기 때문에 이 부분은 서버를 종료시켜
서버가 완벽하게 구동하기 전 시작을 막아 로그를 남기는 것이 좋다.
-> 이런 것을 "fail fast"라고 부른다.

### 2. Exception Handler 부재
클라이언트에게 오류를 전달하기 위해 매번 같은 일을 처리하고 있다.

```java
httpResponse.setStatusCode(404);
httpResponse.setReasonPhrase("Not Found");
httpResponse.write("Page Not Found");
```

이렇게 직접 httpResponse를 만들어 넘겨주고 있다.

하지만 실패 코드를 사용자에게 보여준다는 것은 시스템이 정상 작동하지 않았거나
사용자가 잘못된 Request를 보내어 발생한 오류로도 볼 수 있다.

문제는 다음과 같이 두 가지다.
1. 실패한 응답에도 개발자가 직접 코드를 작성하고 있다는 것이다.
2. 매 오류마다 if 문으로 오류 응답을 직접 주입하고 있다.

로직이 확대되면서 이런 오류 코드도 계속해서 증가할텐데 개발자가 직접 이러한 코드를
작성하는 것은 굉장히 비효율 적이다.

### 3. 서버가 구동되서는 안되는 오류와 계속 작동되도 이상 없는 오류

#### 3.1. 서버가 구동되서는 안되는 오류
```java
log.error("Fail to create servlet instance", e);
throw new RuntimeException("Fatal error: Server failure", e);
```
다음과 같은 오류는 서버가 구동되서는 안되고 바로 종료되어야 한다.

서버에서 servlet 객체를 생성할 수 없을 때 나타나는 오류이다.

이건 명백히 서버 로직 자체의 문제이기 때문에 오류 발생 즉시 서버가 종료되어야 하는 상황이다.

이럴 때는 서버 가동을 멈추고 바로 로직을 수정해야 하는 상황이다.

#### 3.2. 계속 작동되어도 이상 없는 오류
```java
throw new RouteNotFoundException(HttpStatus.NOT_FOUND, "Page Not Found");
```

이건 서버가 잘못 구동된 것이 아니라 클라이언트가 잘못된 URL로 검색되어 있는 오류이기 때문에
서버 자체의 문제라고 할 수 없다.

이럴 때는 서버 작동을 멈춰서는 안되고 클라이언트 측에 오류를 전송하는 것이 맞다.

오류 상황에 따라 에러를 처리하는 방식이 달라져야 함을 인지해야 한다.

## 문제 해결
### 1. 클라이언트가 상황을 명확히 알아야 될 오류와 그렇지 않은 오류

```java
catch (HttpException e) {
    httpExceptionHandler.handle(e, response);
} catch (Exception e) {
    httpExceptionHandler.handleUnexpected(e, response);
}
```

#### 1.1. 클라이언트가 상황을 명확히 알아야 될 오류
예를 들어 `400(Bad Request)`의 경우에 그 설명을 써줘야 사용자가 원인을 파악하여 수정할 수 있다.

400 오류에는 다양한 원인 상황이 존재한다.
- 요청 라인 형식 오류
- 헤더 파싱 실패

그래야 사용자가 해당 오류를 수정할 수 있다.

```java
String body = status.getStatusCode() + " ";
if (e.getMessage() == null) {
    body += status.getResponsePhrase();
} else {
    body += e.getMessage();
}
```
다음과 같이 `httpExceptionHandler.handle(e, response)`에서는
`e.getMessage()`를 body에 함께 제공하여 사용자가 명확한 오류를 파악할 수 있도록 하였다.

#### 1.2. 클라이언트가 알면 안되는 오류
서버에서 문제가 발생하여 클라이언트에게 `500(Interval Server Error)`에러를 제공했다면
서버에 어떤 문제가 발생했다는 이야기이다.

여기서 server의 오류 메시지 `e.getMessage()`를 통해서 서버 내부 자체의 문제를 사용자에게
제공한다면 추후에 해당 문제로 공격을 시도하거나 하는 등의 보안 문제가 발생할 수 있기 때문에
절대로 외부로 오류 메시지를 밖으로 유출해서는 안된다.

### 2. 계속 반복되는 오류 처리
계속 반복되는 오류 처리를 매번 response에 사용자가 직접 상태를 주입하여 클라이언트에게 제공하고 있는데
매번 같은 처리에 대하여 코드를 작성하는 것은 비효율적인 일이다.

그래서 Exception Handler를 제공하여 사용자가 `HttpException`을 발생시키면 자동으로 클라이언트에게
상태 코드와 오류를 응답해주는 형식으로 변경하였다.

```java
public void handle(HttpException e, HttpResponse response) {
    HttpStatus status = e.getStatus();

    logError(status, e);

    response.setStatusCode(status.getStatusCode());
    response.setReasonPhrase(status.getResponsePhrase());
    response.setContentType("text/plain; charset=utf-8");

    String body = status.getStatusCode() + " ";
    if (e.getMessage() == null) {
        body += status.getResponsePhrase();
    } else {
        body += e.getMessage();
    }

    response.write(body);
}
```
### 3. 상황에 따라 달라지는 에러 메시지
밖으로 명확한 설명을 요구하는 에러와 그렇지 않은 500번 대 에러를 구분하기 위해
`handle()`메서드와 `handleUnexpected()`메서드를 구분하여 클라이언트에게 각각 다른 응답을 제공한다.

### 4. 상태 코드
Http 상태 코드에는 정해진 규칙이 있다.

개발자가 직접 상태 코드를 주입하면 정해진 규칙을 벗어날 수도 있고 오류를 발생할 확률이 높아진다.

그래서 `HttpStatus`객체를 만들어 개발자가 정해진 규칙 안에서 상태 코드를 입력할 수 있게 만들었다.