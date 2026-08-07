# Filter
## 개념
### 1. Filter / Criteria 패턴
구조적 패턴의 일종으로, 특정 기준에 따라 객체 전체의 집합을 걸러내어 원하는 결과만 도출하는 패턴
```java
import java.util.ArrayList;
import java.util.List;

// 1. 대상 도메인 클래스
class Laptop {
    private String name;
    private String brand;
    private boolean isGaming;

    public Laptop(String name, String brand, boolean isGaming) {
        this.name = name;
        this.brand = brand;
        this.isGaming = isGaming;
    }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public boolean isGaming() { return isGaming; }
}

// 2. 필터 인터페이스 정의
interface Criteria {
    List<Laptop> meetCriteria(List<Laptop> laptops);
}

// 3. 구체적인 브랜드 필터
class BrandFilter implements Criteria {
    private String brand;

    public BrandFilter(String brand) {
        this.brand = brand;
    }

    @Override
    public List<Laptop> meetCriteria(List<Laptop> laptops) {
        List<Laptop> result = new ArrayList<>();
        for (Laptop laptop : laptops) {
            if (laptop.getBrand().equalsIgnoreCase(brand)) {
                result.add(laptop);
            }
        }
        return result;
    }
}

// 4. 구체적인 게이밍 필터
class GamingFilter implements Criteria {
    @Override
    public List<Laptop> meetCriteria(List<Laptop> laptops) {
        List<Laptop> result = new ArrayList<>();
        for (Laptop laptop : laptops) {
            if (laptop.isGaming()) {
                result.add(laptop);
            }
        }
        return result;
    }
}

// 5. 실행 테스트
public class FilterMain {
    public static void main(String[] args) {
        List<Laptop> laptops = new ArrayList<>();
        laptops.add(new Laptop("맥북프로", "Apple", false));
        laptops.add(new Laptop("그램", "LG", false));
        laptops.add(new Laptop("리전", "Lenovo", true));

        Criteria appleFilter = new BrandFilter("Apple");
        Criteria gamingFilter = new GamingFilter();

        // 필터 적용 및 결과 출력
        System.out.println("--- Apple 노트북 ---");
        for (Laptop l : appleFilter.meetCriteria(laptops)) {
            System.out.println(l.getName());
        }

        System.out.println("\n--- 게이밍 노트북 ---");
        for (Laptop l : gamingFilter.meetCriteria(laptops)) {
            System.out.println(l.getName());
        }
    }
}

```
조건을 두어서 해당 filter의 조건에 맞는 요소만 뽑거나 해당 조건에 맞는 것들만 어떤 로직을 적용하는
패턴이다.



### 2. Chain of Responsibility
```java
public static void main(String[] args) {
    Handler tech = new TechnicalHandler();
    Handler billing = new BillingHandler();

    // 사슬 연결: 기술 팀 -> 결제 팀
    tech.setNext(billing);

    tech.handle("로그인 에러가 발생해요");  // 기술 팀이 처리 후 종료
    tech.handle("환불 결제 문의합니다");    // 기술 팀이 패스 -> 결제 팀이 처리
    tech.handle("기타 건의 사항입니다");    // 모두 패스 -> 처리 불가 출력
}
```
techHandler -> billingHandler -> superHandler 로 연결시켜서
해당 handler가 처리할 수 있으면 처리하고 처리할 수 없다면 다음 handler에 넘겨서 요청한다.

### 3. filter + chain of responsibility
```java
request = parser.parse(in);
sessionHandler.handle(request, response);
loggingHandler.handle(request, response);      // 추가
authHandler.handle(request, response);          // 또 추가
try {
    router.route(request, response);
} ...
```
여기서 이제 handler가 계속 생길 때마다 모든 핸들러를 clientHandler에 계속해서 추가해야 한다.

하지만, 이러면 clientHandler는 서버에 어떤 로직이 있는지 모두 알아야만 한다.

그래서 우리는 chain of responsibility를 이용하여 filter에 적합한 로직만 처리하도록

### 4. Handler
특정 이벤트나 요청, 데이터가 발생했을 때, 이를 담당하여 처리하는 제어 객체나 함수를 뜻한다.

#### 4.1. 핵심 개념과 역할
- 이벤트 기반 작동: 평소에는 가만히 대기하다가, 특정 신호가 들어오는 순간 깨어나 작동한다.
- 관심사 분리: "무슨 일이 일어났는가(발행)"와 "그 일을 어떻게 처리할 것인가(핸들러)"를 분리하여 코드를 깔끔하게 만든다.

#### 4.2. 예시
- 웹 서버의 Request Handler
- Js Event Handler
- Exception Handler

## 어려웠던 점
### 1. Exception Handler를 같이 옮기기
어떤 Exception Handler가 어떤 메서드에 포함된건지 파악하기가 쉽지 않다.

예외 처리 할 때는 항상 주석을 같이 달아서 해당 예외 처리는 어떤 메서드에서 나온 것인지
파악하는게 중요할 듯 하다.