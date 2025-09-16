# CI/CD Jenkins
![Jenkins](https://img.shields.io/badge/Jenkins-Built%20with-blue?logo=jenkins&logoColor=white)
![ngrok](https://img.shields.io/badge/ngrok-Tunnel-orange?logo=ngrok&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-Boot-green?logo=spring&logoColor=white)

<img width="318" height="159" alt="image" src="https://github.com/user-attachments/assets/782fbc1c-5c2f-49c9-9aae-4adb868e72dd" />

## GitHub Webhook

GitHub Webhook은 특정 이벤트가 발생 시 다른 URL로 API 호출을 할 수 있게 해주는 역할

## Jenkins 자동화
<img width="558" height="558" alt="image" src="https://github.com/user-attachments/assets/d36ad68b-af1d-419a-bc58-b6f6be50ac0f" />

1. **CPU 바운드 애플리케이션을 로컬에서 수정한 후 Github에 push한다.**
2. **Github Webhook이 동작해 jenkins에게 API 요청을 날린다.**
3. **jenkins는 Github로부터 온 API 요청을 받아서 저장소의 소스코드를 다운받고 스프링 부트 프로젝트를 빌드하여 JAR파일로 만든다.**
4. **jenkins가 JAR파일을 CPU 워커 인스턴스들에 배포하고 실행시킨다.**

## 자동화 적용을 위한 작업

1. **Github 에서 jenkins로 API요청이 오면 해당 리포지토리로 접근해 소스를 다운받고 JAR파일로 만들고 배포하도록 설정한다.**
2. **Github Webhook 기능을 활성화 시킨다.**
3. **애플리케이션의 포트를 8080포트로 변경시킨다.**
4. **테스트를 하며 빌드과정을 지켜본다.**
5. **자동화 성공 이후 무중단 배포를 위해 추가 설정을 한다.**

## Jenkins에 Github Webhook 빌드 설정
   <img width="869" height="230" alt="image" src="https://github.com/user-attachments/assets/04760750-65ef-48bd-8498-1b67e134878b" />

- 빌드에서 해야하는 일은 소스코드를 저장소에서 pull 받고 해당 소스코드의 의존성을 다운로드 받은 후, 애플리케이션으로 바로 실행할 수 있도록 jar파일로 묶는 행위가 포함

- 위 과정을 빌드에서 수행해주면 배포될 인스턴스가 여러개 있어도 소스코드와 의존성을 다운로드 받는 과정은 오직 한 번만 이루어 지면 됨

### 2-1. 레포지토리 fork
   <img width="441" height="320" alt="image" src="https://github.com/user-attachments/assets/abfc4b94-6fbc-45bd-8a7c-1a4b66f2f2b6" />

- 해당 링크의 리포지토리를 fork한다.

### 2-2. 레포지토리 등록

## ngrok 설정
   <img width="331" height="152" alt="image" src="https://github.com/user-attachments/assets/111bd03c-9e77-4f53-ad58-9c269ee67c1a" />

- ngrok은 로컬 서버를 인터넷에 노출시켜 외부에서 접근할 수 있게 해주는 터널링 도구

### ngrok 설치 및 사용법

1. **ngrok 다운로드 및 설치**
   ```bash
   # Windows (Chocolatey 사용)
   choco install ngrok
   
   # 또는 공식 사이트에서 다운로드
   # https://ngrok.com/download
   ```

2. **ngrok 계정 생성 및 인증**
   ```bash
   # ngrok 계정 생성 후 authtoken 설정
   ngrok config add-authtoken YOUR_AUTHTOKEN
   ```

3. **로컬 서버 터널링**
   <img width="1457" height="697" alt="image" src="https://github.com/user-attachments/assets/45133c13-cdca-4397-aebc-7edeb8884b25" />

   ```bash
   # Jenkins가 8080 포트에서 실행 중일 때
   ngrok http 8080
   
   # 또는 특정 포트 지정
   ngrok http 8080 --subdomain=my-jenkins
   ```

5. **ngrok URL 확인**
   - ngrok 실행 후 제공되는 `https://xxxxx.ngrok.io` URL을 GitHub Webhook 설정에 사용

## Jenkins 자동화 파이프라인 스크립트
   <img width="1269" height="662" alt="image" src="https://github.com/user-attachments/assets/b502bf88-71d2-4cf0-8fda-e0d686a3baf4" />

### Pipeline Script

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ChatHongPT/ci-cd-test.git'
                echo 'Pull 성공'
            }
        }

        stage('Build (Maven Wrapper)') {
            steps {
                dir('CI_CD_OS_maven') {
                    bat '.\\mvnw.cmd -B clean package -DskipTests'
                }
            }
        }

        stage('Check JAR') {
            steps {
                dir('CI_CD_OS_maven') {
                    echo '✅ 빌드된 JAR 파일 확인 (Windows dir)'
                    bat 'dir /-C target\\*.jar || echo JAR 파일이 없습니다!'
                }
            }
        }

        stage('Archive') {
            steps {
                dir('CI_CD_OS_maven') {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }
}
```
### Stage별 설명

<img width="764" height="269" alt="image" src="https://github.com/user-attachments/assets/af7302ce-d194-4c2d-84c9-81bd55a0125a" />

#### 1. Checkout
- `git branch: 'main'` → `main` 브랜치의 코드를 가져옵니다.  
- 성공적으로 pull되면 **"Pull 성공"** 메시지를 출력합니다.  

#### 2. Build (Maven Wrapper)
- `CI_CD_OS_maven` 디렉토리로 이동한 뒤, Maven Wrapper(`mvnw.cmd`)를 실행합니다.  
- 실행 명령어
  ```bash
  .\mvnw.cmd -B clean package -DskipTests
  ```
- B → 배치 모드(로그 최소화)
- clean → 이전 빌드 산출물 삭제
- package → 프로젝트 빌드 후 패키징(JAR 생성)
- DskipTests → 테스트 실행 생략

#### 3. Check JAR
target 디렉토리 내에 JAR 파일이 존재하는지 확인합니다.

```bash
dir /-C target\*.jar
```
JAR 파일이 없으면 "JAR 파일이 없습니다!" 메시지를 출력합니다.

#### 4. Archive

<img width="400" height="133" alt="image" src="https://github.com/user-attachments/assets/713667b3-ef4b-4fc8-8f22-e307871d61f3" />

- target/*.jar 파일을 Jenkins 빌드 아티팩트로 저장합니다.
- fingerprint: true 옵션을 통해 추적 가능성을 보장합니다.
---
# Jenkins CI/CD Pipeline for Gradle Project

## Pipeline Stages

### 1. **Checkout**
- GitHub 저장소에서 소스코드를 Jenkins 워크스페이스로 가져옵니다
- Repository: `https://github.com/ChatHongPT/ci-cd-jenkins.git`
- Branch: `main`

### 2. **Build with Gradle**
- Java 17 환경에서 Gradle 빌드를 실행합니다
- 테스트를 제외하고 빌드하여 시간을 단축합니다
- `./gradlew clean build -x test`

### 3. **Copy Jar to Build Directory**
- 빌드된 JAR 파일을 호스트 볼륨으로 복사합니다
- 타임스탬프 기반 버전 관리를 적용합니다
- 최신 버전에 대한 심볼릭 링크를 생성합니다

### 4. **Archive Artifacts**
- Jenkins 웹 UI에서 다운로드 가능하도록 아티팩트를 보관합니다

## Environment Configuration

| Variable | Value | Description |
|----------|-------|-------------|
| `DEST_DIR` | `/opt/builds` | 빌드 결과물이 저장되는 호스트 디렉토리 |
| `TZ` | `Asia/Seoul` | 한국 표준시 설정 |
| `JAVA_HOME` | `/usr/lib/jvm/java-17-openjdk-amd64` | Java 17 설치 경로 |
| `PATH` | `${JAVA_HOME}/bin:${PATH}` | Java 17을 PATH에 추가 |

## File Structure

빌드 완료 후 `/opt/builds/` 디렉토리 구조

```
/opt/builds/
├── app_20240916_133045.jar    # 첫 번째 빌드 (보관됨)
├── app_20240916_151512.jar    # 최신 빌드
└── app_latest.jar → app_20240916_151512.jar  # 최신 버전 링크
```

## Prerequisites

### Docker 환경 설정

1. **Java 17이 포함된 Jenkins 컨테이너 실행**:
```bash
# Jenkins 컨테이너에 Java 17 설치
docker exec -u root jenkins apt-get update
docker exec -u root jenkins apt-get install -y openjdk-17-jdk
```

2. **볼륨 마운트 설정**:
```bash
# 호스트 디렉토리 생성 및 권한 설정
sudo mkdir -p /opt/builds
sudo chown -R 1000:1000 /opt/builds
sudo chmod 755 /opt/builds

# Jenkins 컨테이너 재시작 (볼륨 마운트 포함)
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /opt/builds:/opt/builds \
  jenkins/jenkins:lts
```

## Key Features

### **자동 버전 관리**
- 각 빌드마다 고유한 타임스탬프 파일명 생성
- 과거 버전들이 자동으로 보관되어 롤백 지원

### **무중단 배포**
- 심볼릭 링크(`app_latest.jar`)를 통한 원자적 업데이트
- 서비스 중단 없이 새 버전으로 전환

### **지능적 JAR 선택**
- `-plain`, `sources`, `javadoc` JAR 파일 자동 제외
- 실제 실행 가능한 JAR 파일만 선택

## Monitoring & Verification

### 빌드 상태 확인
```bash
# 호스트에서 빌드 결과 확인
ls -la /opt/builds/

# Jenkins 컨테이너에서 확인
docker exec jenkins ls -la /opt/builds/
```

### 서비스 연동 예시
<img width="755" height="43" alt="image" src="https://github.com/user-attachments/assets/f8af692a-15f7-4d30-8f32-5f401afa6851" />

```bash
# 최신 JAR로 서비스 실행
java -jar /opt/builds/app_latest.jar

# 특정 버전으로 롤백
java -jar /opt/builds/app_20240916_133045.jar
```

## 🚨 Troubleshooting

### 일반적인 문제와 해결책

| 문제 | 원인 | 해결책 |
|------|------|--------|
| `Permission denied` | 볼륨 마운트 권한 부족 | `sudo chown -R 1000:1000 /opt/builds` |
| `Java not found` | Java 17 미설치 | Jenkins 컨테이너에 Java 17 설치 |
| `JAR not found` | Gradle 빌드 실패 | `./gradlew build` 로그 확인 |
| `Directory not exists` | 볼륨 마운트 미설정 | Docker 컨테이너 재시작 시 `-v` 옵션 추가 |

---
