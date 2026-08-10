# Socket Time out
## 문제 & 해결
Socket 연결 후 write()에서 데이터를 보내지 않아 무한 대기 상태에 빠지게 만드는 공격이나 클라이언트 측
오류를 피하기 위해 다음과 같은 설정을 할 수 있다.

`socket.setSoTimeOut(time)`

해당 설정을 통해서 socket time out 시간을 정해두고 `HttpStatus.TIME_OUT`과 함께 응답을 보낸다.

time out 설정은 서버 담당이라고 생각하기 때문에 서버 측에서 설정을 하고

time out 문제는 client 측의 문제이기 때문에 client 내부에서 에러 핸들링을 처리한다.

정확히는 parse 단계에서 `read()`에서 문제가 발생하기 때문에 Parse 안에서 에러 핸들링을 한다.