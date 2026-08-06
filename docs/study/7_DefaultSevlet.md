# Default Servlet
## 문제점

### 정적 리소스 제공 문제
Router.route()는 현재 Servlet을 매칭해주는 역할을 하고 있다.

그러나 Servlet으로 정적 리소스를 제공하는 것은 그렇게 좋지 못한 설계이다.

개발자가 정적 리소스를 제공하기 위해 Servlet을 작성해야 하는 문제가 있다.

그래서 정적 리소스만 제공하는 Servlet이 필요하다. 개발자가 해당 폴더에 필요한 것을
넣기만 하면 resource를 제공해줄 수 있다면 훨씬 좋은 설계가 될 것이다.

### 어떻게 정적 리소스를 요청하는지 파악할 것인가?
리소스 요청이 온다면 어떻게 정적 리소스인지 파악하고 정적 리소스를 줄 것인가?

1. Header 요청(`GET /style.css`)을 파악한 뒤 요청에 따라 분기한다.
2. 라우팅이 실패한다면 해당 요청은 정적 리소스를 요청한다고 생각한다.

위의 두 가지 측면으로 생각해볼 수 있다.

### path traversal(경로 조작)
사용자가 만약 `GET /../../etc/passwd HTTP/1.1`과 같은 요청을 보냈을 때,
이러한 요청이 폴더 바깐의 시스템 파일을 읽어버리는 결과로 이어질 수 있다.

이러한 경우를 어떻게 막을 수 있을까?

## 해결
### 1. 정적 리소스 해결 문제
`DefaultServlet`이라는 핸들러를 제공하여 정적 리소스를 요청에는 해당 클래스가 담당하도록 한다.

일반 컨트롤러 라우팅은 사용자가 만든 Servlet으로 라우팅을 시키는 역할이고

정적 리소스만 제공하는 Servlet은 정적 리소스만을 제공하는 역할을 한다.

두 개의 역할 차이가 분명하기에 두 핸들러를 나누어 제작한다.

### 2. 정적 리소스를 요청하는지 어떻게 파악할 것인가?

2번을 선택하여 제작한다. 최종적으로는 URI 분석을 거쳐 정적 리소스 경로 패턴을 처리해야 하지만

현재 프로젝트에서는 라우팅이 실패했을 때 해당 요청을 정적 리소스를 요청한다고 생각한다.

### 3. path traversal 문제 해결
사용자가 경로 조작을 시도하려고 했을 때

우리는 사용자가 입력한 경로가 최종 접근할 수 있는 폴더 경로인지 최종적으로 확인해야 한다.

탈출을 시도한 최종 경로가 허용 경로 밖으로 나간다면 해당 요청에 대해 에러 코드를 응답한다.

```java
staticRoot = Path.of(uri).toAbsolutePath().normalize();

String uri = request.getUri();
Path requested = staticRoot.resolve(uri.substring(1)).normalize();

if (!requested.startsWith(staticRoot)) {
    throw new RouteNotFoundException(HttpStatus.NOT_FOUND);
}
```

허용하는 경로 `staticRoot`를 지정하고 상대방의 요청 경로를 절대 경로로 변환한다.
Path의 `normalize()`를 이용하면 `../..`과 같은 상대 경로 표시를 완벽히 변환하여 절대 경로로 변환해준다.

우리가 허용하는 경로와 다르다면 에러를 반환한다.

### 4. MimeType
MimeType이란 Multipurpose Internet Mail Extensions의 줄임말이다.

옛날에 이메일로 텍스트만 보내던 시절에 다양한 파일을 전송하려고 파일의 형식을 타나내는 식별자를 정하여
텍스트만 보내던 이메일의 한계를 극복하기 위해 나타내었다.

이때 개발자들이 이메일용으로 이미 잘 만들어진 MIME 표준 식별자를 웹(HTTP)에 그대로 가져다 쓰기 시작했다.

Content-Type 필드에 이 MIME 타입이 들어가 전송된다.

따라서 정해진 식별자를 따르고자 enum 타입으로 정의했다.