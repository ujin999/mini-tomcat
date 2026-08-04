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

#### URL Connection
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