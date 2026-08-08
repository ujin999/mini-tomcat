# Dispatcher Servlet
## 개념
### 1. Dispatcher Servlet
Spring 프레임워크에서 웹 요청을 가장 먼저 받아 적절한 컨트롤러에게 나누어주는 중앙 컨트롤러

- 디스패처(Dispatcher): '운행 관리원' 또는 '배차원'이라는 뜻, 요청을 목적지까지 보내주는 역할
- Servlet: 자바를 이용해 웹 페이지를 동적으로 생성하는 서버측 프로그램

HTTP 요청의 진입점이 되어 보안, 로그, 인터셉터 등 공통 작업을 한번에 처리한다.
URL 주소에 따라 개발자가 작성한 비즈니스 로직으로 요청을 토스한다.

### 2. Method Reflection

Reflection API에서 `Method` 클래스가 동작하는 원리를 설명하는 개발 문서의 일부이다.

핵심은 "메스드를 실행할 때, 넣으려는 데이터 타입이 실제 메서드가 정의한 타입보다 작으면 크기를 키워주고,
크면 에러를 낸다."

-> int를 인자로 받는 파라미터에서 byte나 short는 확장되지만, long과 같은 것은 에러를 반환한다.

### 3. RequestMapping / Controller
지금까지는 `HelloServlet`을 만들어서 그 서블릿 안에서 `HttpResponse`의 요소를 직접 넣어 만들었다.

하지만 개발자 입장에서는 하나의 `controller`를 만들고 `@RequestMapping`과 `@Controller`만
붙여서 간단하게 html 페이지나 DTO, String 등을 간단하게 return 하기만 한다면 코드 생성하기가 너무
편할 것이다.

이전에는 `GET`, `POST` 등 HttpMethod에 따라서 서블릿을 하나씩 만들어주어야 하지만,
같은 비즈니스 로직에서 여러 `HttpMethod`에 대한 메서드를 하나의 컨트롤러 안에서 관리할 수 있다.
`@Controller`가 붙은 메서드는 HttpMapping에서 사용자가 원하는 요청 key로 관리 된다.

Spring에서 MVC 패턴을 만들 때 주로 사용하는 어노테이션이다.

### 4. HandlerMethod
두 가지 필드로 구성된다.
- Method: 개발자가 `@Controller`를 붙인 메서드(리플렉션의 메서드, 메서드 메타데이터)가 주입된다.
- Controller: 개발자가 제작한 Controller가 주입된다. 컨트롤러를 하나 주입하여 객체를 한번만
생성하여 꺼내기 쉬운 형태로 만든 것이다.

### 5. HandlerMapping
개발자가 생성한 컨트롤러를 등록/관리하는 객체가 하나 필요하다.

ServletContainer에서 구현한 것처럼 이번에도 리플렉션을 사용하여 어노테이션을 분석한 후
클라이언트의 `HttpMethod`와 `URI`를 조합하여 키를 만든 후에 Map<key, handlerMethod>으로
해당 요청에 맞는 메서드에 대한 정보를 관리한다.

역할
- initialize()에서 리플렉션을 통해 `HttpMethod:URI`의 키와 그에 맞는 컨트롤러 메서드를 value로 하여
handlers로 여러 핸들러(컨트롤러의 메서드들)를 관리한다.
- getHandler(HttpRequest)를 통해 요청에 맞는 핸들러를 반환한다.

### 6. HandlerAdapter
클라이언트의 request에 따라 적절한 handler를 직접 실행하고 해당 값을 통해서 response를 구성한다.

이제 여기서는 클래스를 분석하는게 아니라 컨트롤러 내에 들어있는 메서드를 분석해야 한다.
- 파라미터의 개수
- 파라미터의 타입

`handlerMethod.getMethod().getParameterTypes()`를 통해서 파라미터 타입과 개수를 파악하여
해당 파라미터를 request와 response 내에서 찾아내어 메서드에 전달하면 된다.

## 어려웠던 점
### 1. 인터페이스 설계
인터페이스를 설계하는 것이 어려웠다.

객체의 역할을 알아도 어떤 메서드가 어떤 일을 하는지 파악하기가 쉽지 않았다.

- 어떤 메서드가 필요한지 한번에 파악하기 힘들다.
- 메서드 내에서 어떤 파라미터가 필요한지 파악하기 힘들다.

해결 방법
1. 거꾸로 설계
    - 객체의 역할부터 찾지 말고 호출되는 순서대로 코드를 작성해본다.
2. 구체적인 예시 하나를 따라가보기
    - `GET /greet` 예시처럼 이 URL에 맞는 핸들러에 필요한 것을 찾아보기
3. 메서드 몸통을 먼저 작성하기
    - 메서드 본문을 먼저 작성하고 필요한 값들을 파라미터로 추가해보기
4. 많이 틀리기
    - 틀리고 고치는 과정에서 더욱 정교한 코드가 완성된다. 글쓰기 쓰기와 비슷하다. 초안 -> 수정 -> 발견 -> 재수정
    - 설계 -> 구현 -> 이상한 부분 발견 -> 재설계
    - 많이 틀려봐야 틀린 부분을 찾을 수 있고 고칠 수 있다.

### 2. 리플렉션 다루기
처음 리플렉션을 구현해보면서 리플렉션에 필요한 정보들과 메서드들을 처음 접해서 리플렉션을 다루기가
만만치 않았다.

이번 리플렉션을 다룰 때 배웠던 점
- `Method[] methods = clazz.getDeclaredMethods()`를 통해서 메서드에 대한 정보를 가져올 수 있다.
- 메서드도 마찬가지로 어노테이션 정보를 가져올 수 있다. `RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);`
- `method.getParameterTypes()`를 통해서 메서드에 달린 파라미터 정보를 얻을 수 있다.
- `method.invoke(controllerInstance, args)`는 메서드를 실제로 실행시킬 수 있는 방법이다.