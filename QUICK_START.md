# 🚀 Handy Server 배포 퀵스타트 가이드

## 1️⃣ 첫 배포 (데이터베이스 설정)

```bash
# 1. 데이터베이스 환경 설정 (최초 1회만)
./setup-databases.sh both

# 2. 스테이지 환경 배포
./deploy-local.sh stage

# 3. 프로덕션 환경 배포  
./deploy-local.sh prod
```

## 2️⃣ 일반적인 배포

```bash
# 스테이지 배포 후 테스트
./deploy-local.sh stage

# 프로덕션 배포
./deploy-local.sh prod
```

## 3️⃣ 상태 확인

```bash
# 전체 상태 확인
./deployment-status.sh

# 서비스 관리
./manage-services.sh status
./manage-services.sh monitor    # 실시간 모니터링
```

## 4️⃣ 문제 발생 시

```bash
# 롤백 (이전 상태로 복원)
./rollback-deployment.sh stage
./rollback-deployment.sh prod

# 서비스 재시작
./manage-services.sh restart stage
./manage-services.sh restart prod
```

## 🔗 주요 URL

- **스테이지**: http://15.165.22.118:8081
- **프로덕션**: http://15.165.22.118:8080
- **헬스체크**: `/actuator/health` 추가

## 📚 상세 가이드

더 자세한 사용법은 [DEPLOYMENT_README.md](./DEPLOYMENT_README.md)를 참조하세요.