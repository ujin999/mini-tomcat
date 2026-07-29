# Mini Tomcat

## 목표

Tomcat의 핵심 동작을 직접 구현하여 HTTP 요청 처리 과정을 이해한다.

## 요청 처리 흐름

Browser
→ HttpServer
→ HttpParser
→ HttpRequest
→ Router
→ Servlet
→ HttpResponse
→ Browser

## 클래스

- HttpServer : 요청 수신
- HttpParser : HTTP 문자열 파싱
- HttpRequest : 요청 객체
- HttpResponse : 응답 객체
- Router : URL 매핑
- Servlet : 요청 처리