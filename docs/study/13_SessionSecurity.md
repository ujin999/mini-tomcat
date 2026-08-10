# Session Security
## 개념
### 1. 함수형 인터페이스
추상 메서드를 딱 하나만 가지고 있는 인터페이스를 말한다.

자바에서 람다식이나 메서드 참조를 사용하려면 반드시 그 기반이 되는 함수형 인터페이스가 필요하다.

- 핵심 규칙: 다른 static 메서드나 default 메서드가 몇 개 있든 상관 없지만, 구현해야 하는 추상 메서드는
반드시 1개여야 한다.
- 표식: 인터페이스 위에 `@FunctionalInterface` 어노테이션을 붙여서 컴파일러가 검증하도록 만든다.

해당 프로젝트에서는 다음과 같이 사용되었다.

```java
interface AutoCloseable {
    void close() throws Exception;   // 파라미터 없음, 리턴 없음
}
```

```java
class Session {
    public AutoCloseable withLock() {
        lock.lock();
        return lock::unlock;
    }
}
```

```java
try (AutoCloseable ignored = session.withLock()) {
    ...
}
```

`try(이 부분)`에는 AutoCloseable(또는 그 하위 호환인 Closeable)을 구혀8ㄴ한 객체만 들어갈 수 있다.

내부적으로 close() 메서드가 반드시 존재하고 호출된다.

그래서 AutoCloseable 객체를 넣어야 하는데 함수형 인터페이스를 사용하여 AutoCloseable을 만든 것이다.

AutoCloseable 객체는 `void close()`단 하나밖에 없기 때문에 해당 메서드와 똑같이 생긴 메서드 참조를 넣어주면
```java
AutoCloseable c = new AutoCloseable() {
    @Override
    public void close() throws Exception {
        lock.unlock(); // close가 호출될 때 unlock이 실행됨
    }
};

```
다음과 같은 로직으로 코드가 실행된다.

### 2. Scheduler의 구현
`Executors.newSingleThreadScheduledExecutor()`는 정확히 스레드 풀 1짜리 executor를 만든다.

이 스레드는 평소에 그냥 잠들어 있다가(CPU 사용 안함, accept()가 블로킹될 때랑 똑같은 원리), 예약된 시간이
되면 깨어나서 등록해둔 작업을 실행하고, 끝나면 다시 잠든다.

해당 스레드는 데몬 스레드가 아니라 정상적으로 서버가 종료되도 이 스레드가 살아있으면 JVM이 꺼지지 않을 수도 있다.
    - 데몬 스레드: 별로 중요한 작업이 아니라 non daemon thread가 없다면 그냥 끝나는 스레드
    - 데몬이 아닌 스레드: 중요한 작업이라 해당 스레드가 돌고 있으면 JVM이 종료되지 않음.

- schedulteAtFixedRate(작업, 초기지연, 주기, 단위): 시작하는 시점 사이 간격을 일정하게 유지함
작업을 동일한 시간 동안 돌리겠다는 의미. (그 시간을 넘어도 실행이 되긴함. 근데, 쉬는 시간이 사라짐.)
- ScheduleWithFixedDelay: 한 실행이 끝난 뒤부터 다음 시작할 때까지의 간격을 유지함. 끝나고 쉰다는 개념.

### 3. ReentrantLock
같은 스레드가 Lock을 또 잡으려고 해도 재진입이 가능하다.

하나의 스레드가 어떤 객체에 대해 Lock을 잡고 들어갔다. 그런데, 그 class 내부에는
또 Lock(두 lock은 같은 lock 객체)을 잡는 코드가 들어있을 때, `ReentrantLock`은
막지 않고 통과시켜 준다. (내부적으로 몇 번 잡았는지 카운트는 한다.)

### 4. BiFunction<T, U, R>
```java
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u); // T와 U를 입력받아 R을 리턴함
}
```

여기서 BiFunction에 위에서 말한 함수형 인터페이스를 채우는 방법을 통해 사용할 수 있다.

```java
public void computeAttribute(String name, BiFunction<String, Object, Object> remappingFunction) {
    try (LockGuard ignored = withLock()) {
        lastAccessedTime = System.currentTimeMillis();
        Object current = attributes.get(name);
        Object updated = remappingFunction.apply(name, current);
        attributes.put(name, updated);
    }
}
```
```java
// 사용
session.computeAttribute("count", (name, current) -> (current == null ? 0 : (int) current) + 1);
```
다음과 같이 사용하면 BiFunction 객체는 `R apply(T t, U u)`의 내용을 채운다.

## 문제 & 해결
### 1. session의 만료 설정
```java
public HttpSession getSession(String sessionId) {
    HttpSession session = sessions.get(sessionId);
    if (session == null) {
        return null;
    }
    if (session.isExpired(maxInactiveIntervalSeconds)) {
        return null;   // 만료됐으면 "못 찾은 것"처럼 취급
    }
    session.updateExpirationTime();
    return session;
}
```

클라이언트는 이제 만료된 세션에 접근하지 못하고 새로운 세션 ID를 받게 된다.

### 2. 메모리의 세션을 주기적으로 청소하기
사용되지 않는 세션을 메모리에 남겨두고 있으면 메모리 누수가 발생한다.

더 이상 사용하지 않는 세션(만료된 세션)은 삭제시켜 GC 대상에 놓이게 한다.

### 3. 경쟁상태 발생
`HttpSessionManager` 내에서 `getSession()`과 `cleanUpExpiredSessions()`
사이에 경쟁 상태가 발생할 수 있다.

두 메서드 내에서 비슷한 시간에 `isExpired()`를 통해 세션이 만료되었는지 확인했을 때
요청 스레드가 getSession()으로 세션을 찾아 얻은 순간 정리 스레드가 세션을 맵에서 지워버리게 되면
요청 스레드는 이미 손에 쥔 세션 객체를 그대로 사용하게 된다.

하지만, 이미 맵에서 지워버린 세션은 다음에 찾을 수 없게 된다.

- 요청 스레드: isExpired() -> 아직 만료 안됨. 그래서 세션을 들고 있음
- 요청 스레드 <- 잠쉬 쉬게 됨
- 약간의 시간이 정리 스레드가 실행되면서 시간이 지나서 만료로 상태가 바뀜
- 그러면 요청 스레드는 맵에서 지워진 스레드를 그대로 들고 작업을 계속하게 되어 오류를 발생함

해당 문제를 맵 전체에 lock을 걸고 정리 시간 동안은 세션을 사용할 수 없게 막을 수도 있다.

하지만 정리는 하나씩 하는데 정리하는 시간 동안 다른 스레드가 세션을 사용할 수 없다는 것은
크게 문제가 된다고 생각하여 세션 하나하나에 락을 거는 방식을 채택했다.

```java
@FunctionalInterface
public interface LockGuard extends AutoCloseable {
    @Override
    void close();   // AutoCloseable의 "throws Exception"을 없애 좁힘
}

public LockGuard withLock() {
    lock.lock();
    return lock::unlock;
}
```

- `withLock()` 메서드를 만들어 try-with-resource 구문을 사용하려고 하였다.
- `LockGuard`: // AutoCloseable의 "throws Exception"을 없애 에러가 발생했을 때
ClientHandler가 처리하도록 두었다.

각 락은 `private final`을 제외하고 변경되는 모든 변수를 사용하는 부분에 적용하였다.

### 4. ScheduledExecutorService의 문제
`ScheduledExecutorService`로 등록한 작업이 예외를 던지면, 그 순간부터 다시는 실행되지 않는다.
아무 로그도 없이 조용히 멈춘다.

따라서 예외 처리를 통해 이러한 문제를 해결시켜 주어야 한다.