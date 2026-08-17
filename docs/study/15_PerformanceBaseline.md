# 15. Performance Baseline (PORTFOLIO.md Stage 1.5)

> ROADMAP.md의 Phase 15(Graceful Shutdown, 미착수)와는 다른 내용이다. 이 문서는 파일
> 번호 순서상 15번일 뿐, 성능 베이스라인 작업을 정리한 것이다.

## 왜 이 단계를 했는가

Phase 8(스레드풀), Phase 13(세션 락) 모두 "이렇게 설계하면 더 안전/빠를 것"이라는
논리는 있었지만, 실제로 측정해본 적은 없었다.

방법론(가설 → 변경 → 재측정 → 설명)을 실제로 처음 적용해본 단계.

---

## wrk 기초

`wrk`는 HTTP 부하테스트 도구. 핵심 개념 세 가지:

### 1) 논블로킹 이벤트 루프로 동작한다

`-t`(스레드 수)만큼만 OS 스레드를 만들고, 그 각 스레드가 kqueue(맥)/epoll(리눅스)로
자기 몫의 연결(`-c`÷`-t`개)을 동시에 감시한다. 한 연결이 응답을 기다리는 동안 그
스레드가 멈추지 않고 다른 연결에 요청을 계속 보낼 수 있다 — Phase 12에서 배운
`Selector` 패턴과 같은 원리다. 그래서 `-t4 -c200`이어도 실제로 200개 요청이 거의
동시에 서버에 가 있을 수 있다.

이걸 우리 서버의 `ClientHandler`(블로킹, 스레드 1개당 연결 1개)와 대조해보면 차이가
명확해진다 — `ClientHandler` 방식이었다면 스레드 4개는 정말 동시에 4개까지만 처리
가능했을 것이다.

### 2) closed-loop 모델이라는 한계가 있다

wrk는 "응답이 와야 다음 요청을 보낸다." 그래서 `-c`가 실제 동시 요청 수와 비슷해지려면,
서버 처리 시간이 네트워크 왕복/재요청 오버헤드보다 충분히 커야 한다(처리 시간이 0에
가까우면 스레드가 너무 빨리 반납돼서 `-c`만큼 실제로 안 쌓인다). 실제 사용자 트래픽은
서버가 느려져도 계속 요청이 들어오는 open-loop에 가까운데, closed-loop 도구는 서버가
심하게 느려지는 구간에서 상황을 실제보다 덜 나쁘게 보여줄 수 있다 ("Coordinated
Omission"). 이번 실험 규모에선 문제 안 됐지만 알아둘 것.

### 3) 핵심 옵션

- `-t`: wrk 자신의 스레드 수 (클라이언트 머신 코어 수에 맞추는 게 정석)
- `-c`: 유지할 동시 연결 수
- `-d`: 테스트 지속 시간 (`-d8s`, `-d 8s` 둘 다 동일하게 동작 — 짧은 옵션은 값을 붙여
  쓰든 띄어 쓰든 상관없음)
- `--latency`: 50/75/90/99 percentile까지 출력
- `-s <script.lua>`: 매 요청마다 정적으로 같은 요청을 반복하는 대신, 스크립트가 만든
  요청을 보냄

**wrk는 "요청을 몇 개 보내라"는 옵션이 없다.** `-d` 동안 `-c`개의 연결이 각자
"보내고 → 기다리고 → 다음 걸 보내고"를 반복할 뿐이고, 총 요청 수는 그 결과로 나오는
값이다 (`총 요청 수 ≈ 달성된 req/s × duration`).

### 4) 출력 읽는 법 (실제 c=200 예시로)

```
4 threads and 200 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
  Latency   118.44ms   10.46ms 204.63ms   75.18%
  Req/Sec   418.07     37.37   484.00     84.09%
Latency Distribution
   50%  116.92ms
   75%  122.62ms
   90%  130.97ms
   99%  150.31ms
16608 requests in 10.04s, 2.28MB read
Socket errors: connect 0, read 16608, write 0, timeout 0
Requests/sec:   1654.91
```

- `Latency` 행: 이 실행 동안 **모든 개별 요청**의 응답시간 통계
- `Req/Sec` 행: **스레드 1개당 평균**이다(전체가 아님!) — 418.07 × 4스레드 ≈ 1672,
  맨 아래 `Requests/sec: 1654.91`과 거의 일치. **진짜 전체 처리량은 맨 아래 줄.**
- `Latency Distribution`: 평균보다 훨씬 중요 — p99가 평균보다 훨씬 크면 "대부분은
  빠르지만 일부는 느리다"는 뜻. SLA는 보통 p95/p99 기준.
- `Socket errors: read`: 이 프로젝트에서 요청 수만큼 정확히 찍히는 걸 발견 →
  `ClientHandler`가 요청 하나 처리 후 소켓을 무조건 닫는 것(Keep-Alive 미지원)이 원인.
  wrk는 서버의 일방적 연결 종료를 "에러"로 잡고 재연결한다. 요청 자체는 성공하므로
  수치는 유효하지만, 매 요청이 TCP connect/close 비용을 추가로 문다는 뜻 —
  ROADMAP.md Phase 17로 기록.

---

## Experiment 1 — 스레드풀 포화 곡선

**가설**: core(10) + queue(100) + (max-core)(190) = 300을 넘으면 503이 뜨기 시작한다.

**설계**: `/bench`에 `Thread.sleep(100)`을 넣은 전용 엔드포인트(`ThreadPoolBench`)를
만들어서 처리 시간을 인위적으로 부여(안 그러면 closed-loop 모델 특성상 `-c`가 실제
동시성을 반영 못 함). 진행 중 `@RequestMapping` 누락 버그 발견 — 어노테이션이 없으면
`HandlerMapping`이 조용히 라우팅 등록을 건너뛰어서 404가 남.

**결과**: `-c 300`까지 거절 0%, `-c 350`부터 급격히 거절 시작(계산과 실측 일치). 더
중요한 발견 — 성공 응답만 떼어보면 `-c 300`부터 `-c 500`까지 처리량이 계속
~1870~1900 req/s로 일정 — 풀이 넘쳐도 이미 받아들인 요청의 처리 속도는 안 떨어지고
초과분만 명시적으로 거절된다는 Phase 8의 설계 의도가 실측으로 확인됨. 거절 비율이
완만하지 않고 급격한 이유: 거절 응답(~1ms)이 성공 응답(100ms+)보다 훨씬 빨리 재시도를
돌기 때문.

자세한 수치는 `docs/PROJECT_OVERVIEW.md`의 "성능 실험 — 스레드풀 포화 곡선" 참고.

---

## Experiment 2 — 세션 락 세분화

**가설**: Phase 13에서 세션마다 별도 `ReentrantLock`을 쓴 게, 전체를 lock 하나로
감싸는 것보다 여러 세션 동시 접근 시 더 빠르다.

### 설계 과정에서 마주친 것들

1. **여러 세션을 어떻게 동시에 만들지** — 쿼리파라미터(`?sessionId=`)를 쓰려 했는데,
   `HttpParser`가 URI에서 쿼리스트링을 아예 안 잘라내고(`uri = tokens[1]` 그대로),
   `HandlerMapping`이 완전 문자열 일치로 라우팅해서 쿼리스트링이 붙으면 무조건 404가
   난다는 걸 발견. 대신 **HTTP 헤더**(`X-Session-Id`)를 씀 — 헤더는 이미 파싱돼 있고
   `HandlerAdapter`가 `HttpRequest` 파라미터를 이미 지원해서 새 인프라가 필요 없음.
2. **전체-lock 버전**: `GlobalLockBenchManager`/`GlobalLockBenchSession`을 새로 작성
   (기존 코드 안 건드림). `GlobalLockBenchSession`은 스스로 락을 안 잡고, 호출부
   (`globalLockBench.bench()`)가 `manager.withLock()`으로 조회+수정 전체를 감싸는 구조
   — "모든 접근 경로가 락을 통과해야 lock이 의미 있다"는 Phase 13 교훈의 재적용.
3. **세션별-lock 버전**: 실제 `HttpSession`/`HttpSessionManager`를 그대로 재사용.
   `HttpSession`의 생성자가 `protected`라 직접 못 만드니, `PerSessionLockBenchAdapter`가
   "테스트 키(0~49) → 실제 UUID" 매핑을 들고 있다가 매 요청마다 진짜
   `HttpSessionManager.getSession(uuid)`를 호출 — production 코드를 그대로, 온전하게
   타는 방식.
4. **세션 만료 버그**: 50개 테스트 세션을 서버 시작 시 한 번만 만들고 이후 트래픽이
   없다가, `maxInactiveIntervalSeconds`(30초)를 넘겨 전부 만료 → per-session 쪽이 100%
   실패(NPE via `InvocationTargetException`). 한 번 만료되면 다시 안 살아남. 프로덕션
   코드(타임아웃 설정 등)는 안 건드리고, **측정 직전 50개 세션을 전부 한 번씩 깨우는
   워밍업 curl 루프**로 해결.
   - (참고: `maxInactiveIntervalSeconds`를 설정 가능하게 만들자는 아이디어도 나왔지만,
     Phase 13에서 이미 "지금 그걸 필요로 하는 호출자가 없다"는 이유로 per-call 오버로드를
     거절했던 것과 같은 이유로 보류. 워밍업으로 이미 해결됐고, 실제 제품 요구가 생기면
     그때 다시 논의.)

### 1차 결과 — 차이 없음 (가벼운 critical section)

`computeAttribute`가 하는 일(값 읽고 +1, 다시 쓰기)이 수십~수백 나노초 수준이라, 250개
스레드가 전체 lock을 놓고 경쟁해도 대기 시간 총합이 마이크로초 단위. 요청 전체
latency(1~2ms, Keep-Alive 미지원 + HTTP 파싱 + 리플렉션 디스패치가 지배적)에 완전히
묻혀서 두 버전 사이 차이가 1~5% 오차 범위 안에 있었다. **가설이 틀렸다는 게 아니라,
critical section이 너무 가벼워서 그 이점이 측정 불가능했다는 것.**

### 2차 결과 — 극적인 차이 (무거운 critical section)

`computeAttribute`에 넘기는 람다 안(`HttpSession`/`GlobalLockBenchSession` 자체는 안
건드림)에 `Thread.sleep(1)`을 추가해 lock 보유 시간을 1ms로 늘림:

| `-c` | Global req/s | Global latency | Per-session req/s | Per-session latency |
|---|---|---|---|---|
| 50 | 788 | 60.5ms | 6,854 | 6.7ms |
| 100 | 787 | 125.1ms | 6,878 | 13.8ms |
| 150 | 787 | 184.9ms | 19,416 | 5.6ms |
| 200 | 786 | 247.7ms | 18,736 | 5.4ms |
| 250 | 786 | 304.8ms | 18,671 | 4.4ms |

전체-lock은 동시성을 아무리 올려도 처리량이 ~786~788 req/s에 고정(1ms짜리 작업을 lock
하나가 순서대로 처리하니 이론적 최대치 1000 req/s에 근접한 곳에서 막힘), 지연시간은
동시성에 거의 정비례해서 증가 — 하나의 줄에 다 같이 서서 기다리는 전형적인 큐잉 패턴.
세션별-lock은 동시성이 늘어도 처리량이 계속 따라 올라가 최대 **~24배 높은 처리량**,
**~69배 낮은 지연시간**(`-c 250` 기준). 1차와 2차를 나란히 보면 "세션별 lock의 이점은
critical section이 무거울수록 커진다"를 실측으로 증명한 셈.

(`-c` 100→150 구간에서 per-session 처리량이 한 번 점프하는 특이점 있음 — JVM JIT
워밍업 영향으로 추정, 결론 자체엔 영향 없음.)

### wrk 명령어 / Lua 스크립트

```bash
for i in $(seq 0 49); do
  curl -s -o /dev/null -H "X-Session-Id: $i" http://localhost:18080/bench/session/global
done
wrk -t4 -c$c -d8s --latency -s bench/session_lock.lua http://localhost:18080/bench/session/global
```

- 워밍업 curl 루프: 측정 직전 0~49번 세션을 전부 한 번씩 건드려서 만료 타이머 리셋
- `bench/session_lock.lua`:
  ```lua
  request = function()
      wrk.headers["X-Session-Id"] = tostring(math.random(0, 49))
      return wrk.format(nil, nil, wrk.headers)
  end
  ```
  wrk는 전역 함수 이름이 정확히 `request`면 **매 요청마다** 이 함수를 호출해서 뭘
  보낼지 물어본다. `wrk.headers`는 wrk가 들고 있는 헤더 테이블(맵)이고, 여기에
  `math.random(0, 49)`로 뽑은 값을 문자열로(`tostring`) 넣어 매번 다른 세션을 가리키게
  한다. `wrk.format(nil, nil, wrk.headers)`의 `nil, nil`은 "method/path는 커맨드라인
  기본값 그대로, 헤더만 이걸로 교체"라는 뜻이고, 리턴값이 그대로 전송된다.

---

## Experiment 3 — Slowloris 전/후

**가설**: Phase 14의 `setSoTimeout(10_000)`이 없으면 Slowloris(연결만 하고 데이터를 안
보내는 공격)로 스레드풀을 영구히 마비시킬 수 있고, 있으면 공격이 계속돼도 서버가
스스로 회복한다.

### 스크립트 설계

wrk/k6는 항상 완전한 HTTP 요청을 보내도록 만들어져 있어서 "연결만 하고 침묵"을 못
시킨다 — 그래서 **Python 표준 `socket` 모듈**로 직접 만들었다(`bench/slowloris.py`).
핵심은 딱 이거다:

```python
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))
# s.send()를 아예 호출하지 않음 — 0바이트도 안 보냄
```

TCP 3-way handshake는 완료되니까 서버는 "연결됨"으로 보고 워커 스레드에 그 소켓을
넘기는데, `HttpParser.parse()`의 `read()`가 데이터를 영원히 기다리게 된다 — Phase 14의
타임아웃이 막으려던 바로 그 상황.

**몇 개나 열어야 하는가**: Experiment 1과 같은 계산 — core(10) + queue(100) +
(max-core)(190) = 300. 이 숫자보다 넉넉하게 320개를 열면 정상 요청이 들어갈 자리가
없어져 즉시 503을 받는다.

**관찰 방법**: 공격이 진행되는 동안 별도로 `bench/observe.sh`(curl을 1초 간격으로
반복, 상태코드+시각 기록)를 돌려서 언제 503이 뜨고 언제 정상으로 돌아오는지 타임라인을
남겼다.

**전/후 비교 방법**: `HttpServer.java:82`의 `clientSocket.setSoTimeout(10_000)` 한
줄을 측정 직전에 잠깐 주석 처리 → 재빌드/재시작 → "before" 측정 → 원상복구 →
재빌드/재시작 → "after" 측정. 두 측정 다 끝난 뒤 `git diff`로 원본과 완전히 동일함을
확인했다 — Experiment 1·2와 마찬가지로 실측이 끝나면 프로덕션 코드는 손댄 흔적 없이
원래대로.

### 결과

- **Before (타임아웃 없음)**: 공격 시작 즉시 503, 공격 스크립트가 25초 뒤 스스로
  연결을 끊을 때까지 **단 한 번도 회복되지 않음**. 서버 자체 방어력이 0이고 회복이
  전적으로 공격자의 의지에 달려있었다.
- **After (10초 타임아웃)**: 공격 시작 즉시 503이지만, 공격 스크립트가 여전히 30초
  동안 연결을 붙잡고 있는 **도중에** 약 9~10초 만에 자동으로 회복됨. 공격자가 손을
  떼지 않아도 서버 스스로 살아났다.

Before/after를 나란히 보면 "타임아웃 하나가 '공격자가 멈춰야 끝나는 장애'를 '10초짜리
자동 복구'로 바꿔놨다"는 걸 실측으로 확인한 것 — Phase 14 설계 당시의 추론이 그대로
증명됐다.
ㅇ