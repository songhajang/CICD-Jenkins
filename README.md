# CI/CD Jenkins
![Jenkins](https://img.shields.io/badge/Jenkins-Built%20with-blue?logo=jenkins&logoColor=white)
![ngrok](https://img.shields.io/badge/ngrok-Tunnel-orange?logo=ngrok&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-Boot-green?logo=spring&logoColor=white)

<img width="318" height="159" alt="image" src="https://github.com/user-attachments/assets/782fbc1c-5c2f-49c9-9aae-4adb868e72dd" />


## 목표
- CI/CD 개념과 Jenkins 파이프라인 구조 이해
- 실제 레포를 이용해 Jenkins 파이프라인을 작성, 빌드, 배포를 수행

---

## 개념 정리

### Jenkins 이란?
오픈소스 자동화 서버로, 빌드(Build), 테스트(Test), 배포(Deploy) 등 소프트웨어 개발의 반복적인 과정을 **자동화할 수 있는 도구** <br>
개발자가 수동으로 하던 작업을 자동으로 연결하고 실행하게 해주는 플랫폼


### GitHub Webhook 이란?
**외부 서버로 이벤트 알림을 보내는 자동 연결 장치** <br>
GitHub 저장소에서 특정 이벤트(예: Push, Pull Request, Issue 등)가 발생했을 때, 설정된 URL(서버 또는 서비스)로 HTTP POST 요청을 보내 외부 시스템과 자동으로 통신하게 하는 역할



### ngrok 이란?
로컬(내 PC나 개발 서버)에서 실행 중인 애플리케이션을 공개 인터넷에서 접근할 수 있는 **임시 도메인(터널)을 제공하는 도구**
외부 네트워크에서 직접 접근할 수 없는 개발 환경을 **인터넷에 안전하게 노출시키는** 서비스



#### ngrok 설치 및 사용법

1. **ngrok 다운로드 및 설치**
   ```bash
   # Windows (C포 파이프라인 |
|------------------|--------------|
| ![jenkins_build](https://github.com/user-attachments/assets/c4ad2d96-f1c7-4e9a-b4d9-50189ea046c1) | ![deploy (1)](https://github.com/user-attachments/assets/f4281031-73fd-4c12-8eb8-9475f2514a5f)|




## 1. 아키택처

<img width="827" height="444" alt="image" src="https://github.com/user-attachments/assets/a3928a0f-6696-4dc7-a328-abae1e5d2aea" />

1. **CPU 바운드 애플리케이션을 로컬에서 수정한 후 Github에 push한다.**
2. **Github Webhook이 동작해 jenkins에게 API 요청을 날린다.**
3. **jenkins는 Github로부터 온 API 요청을 받아서 저장소의 소스코드를 다운받고 스프링 부트 프로젝트를 빌드하여 JAR파일로 만든다.**
4. **jenkins가 JAR파일을 CPU 워커 인스턴스들에 배포하고 실행시킨다.**


### bind mount 파일 구조

빌드 완료 후 `/opt/builds/` 디렉토리 구조

```
/opt/builds/
├── app_20240916_133045.jar    # 첫 번째 빌드 (보관됨)
├── app_20240916_151512.jar    # 최신 빌드
└── app_latest.jar → app_20240916_151512.jar  # 최신 버전 링크
```

## 2. Docker 환경 설정

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

## 3. Pipeline Script 코드

### 환경변수 설정

| Variable | Value | Description |
|----------|-------|-------------|
| `DEST_DIR` | `/appjardir` | 빌드 결과물이 저장되는 호스트 디렉토리 |
| `TZ` | `Asia/Seoul` | 한국 표준시 설정 |
| `JAVA_HOME` | `/opt/java/openjdk` | Java 17 설치 경로 |
| `PATH` | `${JAVA_HOME}/bin:${PATH}` | Java 17을 PATH에 추가 |

```groovy
pipeline {
  agent any
  options {
    timestamps()
  }
  environment {
    // 볼륨 마운트된 경로 사용: -v $(pwd)/appjardir:/var/jenkins_home/appjar
    DEST_DIR = '/var/jenkins_home/appjar'  // 호스트의 appjardir와 연결됨
    TZ       = 'Asia/Seoul'
    // Java 17 경로 설정
    JAVA_HOME = '/opt/java/openjdk'
    PATH = "${JAVA_HOME}/bin:${PATH}"
  }
  stages {
    stage('Checkout') {
      steps {
        echo 'Git 저장소에서 코드 가져오기'
        git branch: 'main', url:  'https://github.com/yunkihong-dev/CI-CD-Study.git'
      }
    }
    stage('Build with Gradle') {
      steps {
        echo 'Gradle 빌드 시작'
        dir('step04_gradleBuild') {
          sh '''#!/bin/bash
            set -euo pipefail
            chmod +x ./gradlew
            ./gradlew clean build -x test
          '''
        }
      }
    }
    stage('Copy Jar to Build Directory') {
      steps {
        echo '빌드된 JAR을 날짜별 파일명으로 DEST_DIR에 저장'
        sh '''#!/bin/bash
          set -euo pipefail
          ts="$(date +%Y%m%d_%H%M%S)"
          # DEST_DIR 디렉토리 생성 (볼륨 마운트된 디렉토리이므로 권한 문제 없음)
          echo ":파일_폴더: Creating directory: ${DEST_DIR}"
          mkdir -p "${DEST_DIR}"
          # 가장 대표 JAR 1개 선택(plain/sources/javadoc 제외)
          jar="$(ls -1 step04_gradleBuild/build/libs/*.jar \
                 | grep -vE '(-plain|sources|javadoc)\\.jar$' \
                 | head -n 1 || true)"
          if [ -z "${jar}" ]; then
            echo ":x: step04_gradleBuild/build/libs 에서 Jar를 찾지 못했습니다."
            exit 1
          fi
          echo ":포장: Found JAR: ${jar}"
          # 권한 지정하며 복사
          cp "${jar}" "${DEST_DIR}/app_${ts}.jar"
          chmod 644 "${DEST_DIR}/app_${ts}.jar"
          # 최신 링크 업데이트(원자적 교체)
          ln -sfn "app_${ts}.jar" "${DEST_DIR}/app_latest.jar"
          echo ":흰색_확인_표시: Copied: ${DEST_DIR}/app_${ts}.jar"
          echo ":링크: Updated link: ${DEST_DIR}/app_latest.jar"
          # 빌드 결과 확인
          echo ":클립보드: Build artifacts in ${DEST_DIR}:"
          ls -la "${DEST_DIR}/"
          # 호스트에서도 확인할 수 있음을 알림
          echo ":집: 호스트에서는 appjardir 디렉토리에서 확인 가능합니다."
        '''
      }
    }
    stage('Archive Artifacts') {
      steps {
        archiveArtifacts artifacts: 'step04_gradleBuild/build/libs/*.jar',
                         fingerprint: true,
                         onlyIfSuccessful: true
      }
    }
    stage('Run Application') {
      steps {
        echo 'JAR 파일 실행'
        sh '''
          chmod 755 "${DEST_DIR}/app_latest.jar"
        '''
        }
    }
}
  post {
    success {
      echo ':흰색_확인_표시: Build & copy complete.'
      echo ":열린_파일_폴더: JAR files are available in: ${env.DEST_DIR}"
      echo ":집: 호스트에서는 appjardir 디렉토리에서 확인 가능합니다."
    }
    failure { echo ':x: Build failed — logs를 확인하세요.' }
  }
}
```
### Stage 별 설명

**1. Checkout**
- GitHub 저장소에서 소스코드를 Jenkins 워크스페이스로 cone
- Repository: `https://github.com/ChatHongPT/ci-cd-jenkins.git`
- Branch: `main`

 **2.Build with Gradle**
- Java 17 환경에서 Gradle 빌드를 실행
- 테스트를 제외하고 빌드하여 시간을 단축
- `./gradlew clean build -x test`

 **3. Copy Jar to Build Directory**
- 빌드된 JAR 파일을 호스트 볼륨으로 복사
- 타임스탬프 기반 버전 관리를 적용
- 최신 버전에 대한 심볼릭 링크를 생성

**4. Archive Artifacts**
- Jenkins 웹 UI에서 다운로드 가능하도록 아티팩트를 보관



## 4. 빌드

```bash
# 호스트에서 빌드 결과 확인
ls -la /appjardir

# Jenkins 컨테이너에서 확인
docker exec jenkins ls -la /appjardir
```

#### 서비스 연동 예시

<img width="408" height="77" alt="image" src="https://github.com/user-attachments/assets/7a4f2d43-e2e7-40f5-a1d7-c56a221d3095" />

```bash
# 최신 JAR로 서비스 실행
java -jar /appjardir/app_latest.jar

# 특정 버전으로 롤백
java -jar /appjardir/app_20240916_133045.jar
```

## 5. 자동 배포
서버에서 Java 애플리케이션(`app_latest.jar`)을 자동으로 감시하고 배포하는 스크립트

```sh
ubuntu@myserver00:~$ cat deploy.sh
#!/bin/bash
cd /appjardir
echo "자동 배포 서비스 시작..."
LAST_CHECK=0
kill_app() {
    echo "기존 앱 완전 종료 시도..."
    PID_PORT=$(lsof -t -i:8084 2>/dev/null)
    if [ ! -z "$PID_PORT" ]; then
        echo "포트 8084 사용 중인 PID: $PID_PORT 종료"
        kill $PID_PORT
        sleep 2
        kill -9 $PID_PORT 2>/dev/null
    fi
    pkill -f "java.*app_latest.jar"
    rm -f app.pid
    echo ":흰색_확인_표시: 종료 완료"
}
start_app() {
    echo "새 앱 시작 중..."
    java -jar app_latest.jar > app.log 2>&1 &
    NEW_PID=$!
    echo $NEW_PID > app.pid
    echo ":흰색_확인_표시: 앱 시작됨 (PID: $NEW_PID)"
}
while true; do
    if [ -f app_latest.jar ]; then
        # 파일 수정 시간 가져오기 (초 단위)
        CURRENT_TIME=$(stat -c %Y app_latest.jar)
        if [ $CURRENT_TIME -gt $LAST_CHECK ]; then
            echo "새로운 JAR 감지됨! ($(date -d @$CURRENT_TIME))"
            kill_app
            sleep 3
            start_app
            LAST_CHECK=$CURRENT_TIME
        fi
    fi
    sleep 5  # 5초마다 체크
done
~

```
### 기능 설명
**1. 자동 JAR 감지**
- `/appjardir` 폴더 내 `app_latest.jar` 파일의 변경 여부를 주기적으로 확인
- 새 버전이 올라오면 자동으로 배포 프로세스 시작

**2. 기존 앱 안전 종료**
- 포트 8084에서 실행 중인 기존 프로세스를 확인하고 종료
- 이전 버전 JAR를 실행 중인 Java 프로세스도 종료
- 기존 앱 PID 기록(`app.pid`) 삭제

**3. 새 앱 자동 시작**
- 새 JAR 파일을 백그라운드에서 실행
- 실행된 프로세스 PID를 `app.pid`에 기록
- 표준 출력 및 오류 로그를 `app.log`에 기록

**4. 주기적 감시**
- 5초마다 JAR 파일 상태를 체크하여 변경 감지 시 자동 배포
- 서버에 수동 배포 없이 항상 최신 버전 실행 유지

---


## Troubleshooting

### 일반적인 문제와 해결책

| 문제 | 원인 | 해결책 |
|------|------|--------|
| `Permission denied` | 볼륨 마운트 권한 부족 | `sudo chown -R 1000:1000 /opt/builds` |
| `Java not found` | Java 17 미설치 | Jenkins 컨테이너에 Java 17 설치 |
| `JAR not found` | Gradle 빌드 실패 | `./gradlew build` 로그 확인 |
| `Directory not exists` | 볼륨 마운트 미설정 | Docker 컨테이너 재시작 시 `-v` 옵션 추가 |

---
