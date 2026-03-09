# API 테스트 예시 (Swagger UI / Postman용)

Swagger UI (`http://localhost:8080/swagger-ui.html`) 또는 Postman에서 테스트할 때 복사해서 사용하세요.

## POST /api/interaction/check

- `interaction-check-request.json` — Request body 예시
- `drugId`는 DB에 존재하는 `drugs.id` 값으로 변경하세요. (예: `SELECT id FROM drugs LIMIT 1;`)
