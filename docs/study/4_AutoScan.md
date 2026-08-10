# Auto Scan

## ClassLoader
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

## ClassLoader 만으로는 자동 완성을 할 수 없다.
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

## Reflection 먼저
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

## Tomcat이 Router에 HelloServlet.class를 저장하지 않고, HelloServlet 객체를 저장하는 이유

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

## Scanner는 왜 String을 인자로 받을까?
Spring은 `@ComponentScan("com.exampele")`을 인자로 받는다.

왜 인자로 받을까?
1. Java는 "**패키지**"를 기준으로 동작한다.
2. 빌드 후 `.jar` 또는 `.war` 형태의 압축 파일로 패키징되어 실행된다.
  - 이 상태에서는 OS 수준의 파일 경로(/usr/...)로 개별 파일에 접근할 수 없다.
3. 자바에서 클래스를 로딩하고 찾는 주체는 OS나 파일 시스템이 아니라 ClassLoader이다.
  - 클래스로더는 패키지 경로를 찾을 때 문자열을 기반으로 탐색한다.
4. File을 넘기면 확장성이 떨어진다.
5. 타입 안정성을 위해 클래스 타입을 인자로 받는 방식도 존재하여 해당 클래스가 위치한 패키지를 기준점으로 삼아 하위 패키지를 스캔한다.

## ClassLoader 종류
자바의 클래스로더는 여러 종류가 존재한다.

그 중에 우리는 개발자가 작성한 클래스를 읽는 `Application ClassLoader`를 읽을 것이다.

`Application ClassLoader`는 외부 라이브러리나 현재 프로젝트에 있는 클래스들을 로드하고 읽을 수 있다.

## ClassLoader 설계
```java
class ClassLoader {
    public List<Class<?>> scan(String basePackage) {
      ...
    }
}
```

## JAR 파일 안의 클래스 스캔
```java
private void findClassesInJar(URL url, String path, List<Class<?>> classes) throws IOException, ClassNotFoundException {

    // jar:file:/... 주소에서 실제 JarFile 객체를 뽑아냅니다.  
    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();  
    try (JarFile jarFile = jarURLConnection.getJarFile()) {  
        Enumeration<JarEntry> entries = jarFile.entries();  
  
        while (entries.hasMoreElements()) {  
            JarEntry entry = entries.nextElement();  
            String entryName = entry.getName(); // 예: com/example/servlet/Hello.class  
  
            // 내가 찾는 패키지 경로로 시작하면서 .class 파일인 것만 필터링  
            if (entryName.startsWith(path) && entryName.endsWith(".class")) {  
                // 파일 경로 형태( / )를 패키지 형태( . )로 변환  
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);  
                classes.add(Class.forName(className));  
            }  
        }  
    }  
}
```

`URL url`: "jar:file:/app.jar!/com/example/servlet"같은 주소를 띄고 있다.

### URL Connection
```java
JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();  
```
URL Connection을 통해 url에 해당하는 jar 파일(! 이전의 file:/app.jar) 전체를 확인하고 내부 파일 지도를 확인한다. 그리고 "/com/example/servlet" 밑의 Entry들을 확인하여 JarFile, JarEntry 등을 만들어낸다.

URL Connection은 Java 애플리케이션과 URL 간의 연결 관련한 모든 클래스의 수퍼클래스이다. 리소스(HTTP, FTP)가 있는 서버 또는 로컬 경러와의 하드웨어적 연결을 맺고 끊는 역할을 한다.

URLConnection의 클래스는 일반적인 URL에 대한 API를 제공한다.
일반적으로 클라이언트 프로그램은 URL을 통해 서버와 통신할 때 다음 단계를 따른다.
- URL 객체 만들기
- URL에서 URLConnection 객체 획득
- URL 연결 구성
- 헤더 필드 읽기
- 입력 스트림 가져오기 및 데이터 읽기
- 출력 스트림 가져오기 및 데이터 쓰기
- 연결 닫기

```java
try (JarFile jarFile = jarURLConnection.getJarFile())
```
JarFile 객체는 로컬 디스크나 메모리에 존재하는 독립된 JAR(Java Archive) 파일 전체를 자바 코드로 제어할 수 있게 추상화한 인스턴스이다. JarFile이 가리키는 실제 물리적 대상은 디스크에 있는 단 하나의 파일 `app.jar`이다. (그래서 클래스가 File인 것이다.)
이때 JarFile 안의 JarEntry를 통해서 JAR 파일의 내부를 확인할 수 있다.

JarFile은 이 압축 파일 자체를 하나의 큰 디렉터리처럼 다룰 수 있게 해주는 역할을 한다.

자바 코드를 빌드하면 컴파일된 .class 파일들과 이미지 XML 같은 리소스 파일들이 폴더 구조 그대로 압축된다.

JAR은 JIP 압축 포맷과 완전히 동일한데 ZIP 파일의 가장 뒷 부분에는 내부 파일들의 지도 역할을 하는 중앙 디렉터리 정보가 저장되어 있다.

예를 들어, "A.class 파일은 JAR 파일의 시작점으로부터 정확히 1,024바이트 떨어진 곳부터 시작되고, 압축된 길이는 500바이트다." 즉 offset = 1024, length = 500 이 된다.

이러한 정보를 갖고 해당 부분의 바이트만 가져와 압축을 해제하여 `A.clasa`파일의 정보를 가져올 수 있다.

```java
Enumeration<JarEntry> entries = jarFile.entries();
```
jarFile 내부에 있는 파일들을 entry라고 한다.

JarEntry는 정확히 각 파일들에 대한 메타데이터를 포함한 객체로 파일 자체를 의미하지는 않는다.

JavaFile은 url 주소를 폴더로 설정하면 `jar:file:/app.jar!/com/example/servlet`처럼 설정하면 entries는 jar파일 내에 있는 모든 entry를 얻는다. `/com/example/servlet`밑에 있는 entry만 뽑는 것이 아니다.

이때 아래와 같이 특정 파일을 딱 하나만 뽑겠다고 선언하면 바로 접근하는 것은 가능하다.
```java
URL url = new URL("jar:file:/app.jar!/config.xml");
JarURLConnection conn = (JarURLConnection) url.openConnection();
InputStream is = conn.getInputStream();
```
---

```java
while (entries.hasMoreElements()) {  
            JarEntry entry = entries.nextElement();  
            String entryName = entry.getName(); // 예: com/example/servlet/Hello.class  
  
            // 내가 찾는 패키지 경로로 시작하면서 .class 파일인 것만 필터링  
            if (entryName.startsWith(path) && entryName.endsWith(".class"))
```
이때 entries에는 JAR 파일의 모든 entry가 포함되어 있으므로 우리가 원하는 entry를 조건문으로 뽑아내야 하는 작업이 필요하다.

모든 entry를 매번 검사하는 것은 비효율적인 작업이므로 이에 대한 대책이 필요하다. 캐싱, 색인 파일, 멀티 스레딩을 활용한 고속 스캔을 이용하여 최적화하는 작업이 필요하다.

## Scanner
### Class.forName()
`Class.forName()`은 다음과 같이 동작한다.
```text
Class.forName()
    ↓
클래스 로딩
    ↓
클래스 초기화(static)
```

ClassScanner에서 사용한 forName()을 통해 Class 객체를 `List<Class<?>>`에 주입했다.

Class.forName()을 진행하면 클래스 초기화까지 진행한다.

근데 이건 좋은 설계가 되지 못한다.

Tomcat이나 Spring은 스캔과 객체 생성을 절대적으로 분리한다.
#### 1. 스캔 단계
- 클래스 찾기
- Annotation 읽기
- 초기화 하지 않음

#### 2. 생성 단계
- 필요한 클래스만 객체 생성
- 이때 static 초기화가 일어난다.

다음과 같이 클래스를 초기화하지 않고 메타데이터만 읽을 수 있다.
```java
Class.forName(className)
        ↓
Class.forName(className, false, classLoader)
```
