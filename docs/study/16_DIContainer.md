# DI Container
## 개념
`ServletContainer`나 `HandlerMapping`은 둘 다 새로운 객체를 생성할 때
no-arg 생성자를 호출한다.

이건 스캔된 클래스가 아무 의존성도 없을 때만 통하는 기법이다.

그러나 `GreetingController`가 `GreetingService`를 필드로 들고 싶어지는 순간
그 의존성을 누가 담당할지 아무도 알지 못한다.

이때 `Container`가 그 역할을 담당한다.

### 역할
1. 발견 (Discover) - `@Component/@Controller 등`이 붙은 클래스를 스캔해서 관리 대상인 클래스를 알아냄
2. 생성 + 배선 (Create & Wire) - 생성자 파라미터 타입을 리플렉션으로 읽고, 각 타입을 재귀적으로 해결해서
생성자 주입으로 조립함
3. 소유 (Own the lifecycle) - 한 번 만든 인스턴스를 캐시해서 재사용함

### IoC (제어의 역전)
리팩토링 전엔 객체를 어떻게 만들지 결정을 각 소비자가 코드 안에 넣었다.

예를 들면 `GreetingController`가 `GreetingService`를 필드로 두고 싶다면, Controller는 new를 통해서
하나의 새로운 인스턴스를 만들 것이다.

하지만 만약 다른 컨트롤러도 같은 `GreetingService`가 필요하다면 또 하나의 `GreetingService`를
제작해야 한다.

이때 다른 클래스가 `GreetingController`와 같은 `GreetingService`를 필요로 한다면 어떨까?

즉, 싱글톤으로 만들어서 같은 Service 객체를 공유해야 하려면 서버 코드에서 부터 시작해서 계속 사용자가
객체를 생성자 혹은 setter 등으로 넣어줘야 한다.

**현재는 생성자 주입만 지원하고 있다.**

이제는 각각의 필요한 코드를 소비자가 직접 넣는 것이 아니라 컨테이너가 직접 필요한 객체를 찾아 넣어준다.

이것이 제어의 역전이다.

## 설계
### Container
```java
private final Map<Class<?>, ComponentStatus> states;
private final Map<Class<?>, Object> instances;
private final Deque<Class<?>> resolutionStack;
```
컨테이너는 기본적으로 3가지의 필드를 두고 있다. 
- states: 현재 객체가 만들어졌는지 확인하는 상태, 해당 상태로 순환 참조를 막을 수 있거나 캐싱을 확인할 수 있다.
- instances: 만들어진 인스턴스들
- Deque: 순환참조에 대한 에러를 표시하기 위한 스택

```java
public <T> T getBean(Class<T> clazz) {
    // 이미 만들어져있다면 캐시된 인스턴스를 반환
    
    // 만드는 중이라면 순환참조로 인식 <-- 순환참조는 금지되어 있음
    
    // 생성자에 필요한 인스턴스를 확인 후 getBean을 통해서 인스턴스 생성
    
    // 생성자에 필요한 모든 인스턴스를 통해 clazz 인스턴스를 생성
}
```

`getBean()`은 `HandlerMapping`에서 `newInstance()`로 컨트롤러를 직접 만들지 않고
`container.getBean(clazz)`로 공유 인스턴스를 받음으로써 이제 인자를 주입받을 수 있게 되었다.
-> IoC

### ClassScanner
`ClassScanner`가 Class를 스캔할 때 각자 자신이 속한 서브 패키지를 `basePackage`의 인자로 넣어주었다.

하지만, 이렇게 하면 `@Component`/`@Controller`가 달렸어도 패키지 경로가 달라 스캔하지 못하는 문제가 있었다.

이제는 기본 패키지 `com.example.minitomcat` 밑에 있는 모든 패키지를 확인하고 필요한 필터에 따라 List를
만들어 제공해준다.

```java
public List<Class<?>> scan(String basePackage, Predicate<Class<?>> filter) {
    if (classesByPackage.containsKey(basePackage)) {
        return classesByPackage.get(basePackage).stream()
                .filter(filter)
                .toList();
    }
    
    // ...

    return classes.stream().filter(filter).toList();
}
```

그리고 이미 `basePackage`를 전체로 한번 체크하기 때문에 다른 곳에서 한 번 더 체크하는 것을 방지하기 위해
`Map<String, List<Class<?>>> classesByPackage = new HashMap<>();`에서 캐싱해서 사용하기로 한다.

## 버그 개선
### ComponentState
처음엔 생성 실패 시 상태를 되돌리는 로직을 finally에 `CREATED`로 뒀다가,
실패한 경우에도 `CREATED`로 처리해서 싱글턴이 깨져버려서 오류가 발생해도 찾기 어려운 상황을 개선할 수 있도록
finally를 없애고 catch로 `FAILED`되는 상황에 서버가 작동 중지하도록 바꾸었다.

