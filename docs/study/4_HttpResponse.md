# HttpResponse
## 1. 현재 방식의 문제점
### 1.1. headers 문제
```java
Map<String, String> headers = httpResponse.getHeaders();
```
현재 `HttpResponse`객체로부터 map 객체를 직접 받아와 쓰고 있다.

만약 여기서 개발자가 실수로 `headers.clear()`를 하거나 객체 headers 내부를 직접 수정하거나
`headers`에서 시스템이 수정하는 부분을 개발자가 건드려 오류가 나면 안된다.

이러한 시스템 설계를 막기 위해 `headers`를 밖으로 노출시키는 것을 제한하고
필요한 `header`만 제공하기 위해 setter를 활용하기로 한다.

```java
public void setHeader(String key, String value) {
    headers.put(key, value);
}
```
일단은 임시 방편으로 setter를 제작하긴 하였으나, 추후 시스템이 넣는 header들에
대해서는 개발자가 접근하지 못하도록 수정할 필요가 있다.

### 1.2. Content-Length 문제
현재는 `Content-Length`를 servlet을 만드는 개발자가 직접 넣도록 되어있다.

하지만, 우리는 문자를 넣으면 `Content-Length`를 자동으로 주입해주길 바라고
만약 실수로라도 Content-Length를 잘못 계산한다면 추후 클라이언트에서 논리 오류가 발생할 가능성이 높다.

또한 `Content-Length`를 `HttpResponse`의 `toByte()`부분으로 옮기면서
`Content-Length`가 완전히 시스템 내에서 작성되도록 하였다.

