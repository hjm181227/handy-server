# 장바구니 API 문서

## 개요
장바구니 관련 기능을 제공하는 REST API입니다. 모든 API는 인증이 필요하며, Authorization 헤더에 Bearer 토큰을 포함해야 합니다.

## API 엔드포인트

### 1. 장바구니에 상품 추가
**POST** `/api/cart/add`

상품을 장바구니에 추가합니다. 같은 상품, 사이즈, 모양이 이미 있는 경우 수량이 증가됩니다.

#### 요청 본문
```json
{
  "productId": 1,
  "quantity": 2,
  "size": "M",
  "shape": "ROUND"
}
```

#### 응답
```json
{
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "커스텀 반지",
      "productMainImageUrl": "https://example.com/image.jpg",
      "productPrice": 50000,
      "quantity": 2,
      "size": "M",
      "shape": "ROUND",
      "totalPrice": 100000
    }
  ],
  "totalAmount": 100000,
  "totalItems": 2
}
```

### 2. 장바구니 목록 조회
**GET** `/api/cart`

현재 사용자의 장바구니 상태를 조회합니다.

#### 응답
```json
{
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "커스텀 반지",
      "productMainImageUrl": "https://example.com/image.jpg",
      "productPrice": 50000,
      "quantity": 2,
      "size": "M",
      "shape": "ROUND",
      "totalPrice": 100000
    }
  ],
  "totalAmount": 100000,
  "totalItems": 2
}
```

### 3. 장바구니 상품 수량 변경
**PUT** `/api/cart/items/{itemId}`

특정 장바구니 아이템의 수량을 변경합니다.

#### 요청 본문
```json
{
  "quantity": 3
}
```

#### 응답
```json
{
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "커스텀 반지",
      "productMainImageUrl": "https://example.com/image.jpg",
      "productPrice": 50000,
      "quantity": 3,
      "size": "M",
      "shape": "ROUND",
      "totalPrice": 150000
    }
  ],
  "totalAmount": 150000,
  "totalItems": 3
}
```

### 4. 장바구니 상품 삭제
**DELETE** `/api/cart/items/{itemId}`

특정 장바구니 아이템을 삭제합니다.

#### 응답
```json
{
  "items": [],
  "totalAmount": 0,
  "totalItems": 0
}
```

### 5. 장바구니 비우기
**DELETE** `/api/cart`

사용자의 모든 장바구니 아이템을 삭제합니다.

#### 응답
```
"Cart cleared successfully"
```

## 데이터 구조

### CartStateResponse
```json
{
  "items": "CartItemResponse[]",
  "totalAmount": "BigDecimal",
  "totalItems": "Integer"
}
```

### CartItemResponse
```json
{
  "id": "Long",
  "productId": "Long",
  "productName": "String",
  "productMainImageUrl": "String",
  "productPrice": "BigDecimal",
  "quantity": "Integer",
  "size": "String",
  "shape": "String",
  "totalPrice": "BigDecimal"
}
```

### CartAddRequest
```json
{
  "productId": "Long",
  "quantity": "Integer",
  "size": "String",
  "shape": "String"
}
```

### CartItemUpdateRequest
```json
{
  "quantity": "Integer"
}
```

## 에러 처리

### 400 Bad Request
- 잘못된 요청 형식
- 필수 필드 누락

### 401 Unauthorized
- 인증 토큰 누락 또는 만료

### 403 Forbidden
- 다른 사용자의 장바구니 아이템에 접근 시도

### 404 Not Found
- 존재하지 않는 상품
- 존재하지 않는 장바구니 아이템
- 존재하지 않는 사용자

### 500 Internal Server Error
- 서버 내부 오류

## 인증

모든 API 요청에는 다음 헤더가 필요합니다:
```
Authorization: Bearer <JWT_TOKEN>
```

## 예시 사용법

### cURL 예시

#### 장바구니에 상품 추가
```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "size": "M",
    "shape": "ROUND"
  }'
```

#### 장바구니 조회
```bash
curl -X GET http://localhost:8080/api/cart \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### 수량 변경
```bash
curl -X PUT http://localhost:8080/api/cart/items/1 \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

#### 아이템 삭제
```bash
curl -X DELETE http://localhost:8080/api/cart/items/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### 장바구니 비우기
```bash
curl -X DELETE http://localhost:8080/api/cart \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 구현된 파일들

1. **DTO 클래스들**
   - `CartAddRequest.java` - 장바구니 추가 요청
   - `CartItemResponse.java` - 장바구니 아이템 응답
   - `CartStateResponse.java` - 장바구니 상태 응답
   - `CartItemUpdateRequest.java` - 수량 변경 요청

2. **Repository 클래스들**
   - `CartRepository.java` - Cart 엔티티 Repository
   - `CartItemRepository.java` - CartItem 엔티티 Repository

3. **Service 클래스**
   - `CartService.java` - 장바구니 비즈니스 로직

4. **Controller 클래스**
   - `CartController.java` - REST API 엔드포인트

5. **Exception 클래스**
   - `CartException.java` - 장바구니 관련 예외

## 주요 기능

- **중복 상품 처리**: 같은 상품, 사이즈, 모양이 추가되면 수량이 증가됩니다.
- **사용자별 장바구니**: 각 사용자는 자신만의 장바구니를 가집니다.
- **보안**: 다른 사용자의 장바구니에 접근할 수 없습니다.
- **상품 정보 포함**: 장바구니 아이템에 상품의 기본 정보(이름, 이미지, 가격)가 포함됩니다.
- **총액 계산**: 장바구니의 총 금액과 총 아이템 수가 자동으로 계산됩니다.
