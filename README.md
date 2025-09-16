# CI/CD Jenkins
![Jenkins](https://img.shields.io/badge/Jenkins-Built%20with-blue?logo=jenkins&logoColor=white)
![ngrok](https://img.shields.io/badge/ngrok-Tunnel-orange?logo=ngrok&logoColor=white)
![Spring](https://img.shields.io/badge/Spring-Boot-green?logo=spring&logoColor=white)

<img width="318" height="159" alt="image" src="https://github.com/user-attachments/assets/782fbc1c-5c2f-49c9-9aae-4adb868e72dd" />


## ✅ 목표
- CI/CD 개념과 Jenkins 파이프라인 구조 이해
- 실제 레포를 이용해 Jenkins 파이프라인을 작성, 빌드, 배포를 수행

---

## 개념 정리

### 📌 Jenkins 이란?
오픈소스 자동화 서버로, 빌드(Build), 테스트(Test), 배포(Deploy) 등 소프트웨어 개발의 반복적인 과정을 **자동화할 수 있는 도구** <br>
개발자가 수동으로 하던 작업을 자동으로 연결하고 실행하게 해주는 플랫폼


### 📌 GitHub Webhook 이란?
**외부 서버로 이벤트 알림을 보내는 자동 연결 장치** <br>
GitHub 저장소에서 특정 이벤트(예: Push, Pull Request, Issue 등)가 발생했을 때, 설정된 URL(서버 또는 서비스)로 HTTP POST 요청을 보내 외부 시스템과 자동으로 통신하게 하는 역할



### 📌 ngrok 이란?
로컬(내 PC나 개발 서버)에서 실행 중인 애플리케이션을 공개 인터넷에서 접근할 수 있는 **임시 도메인(터널)을 제공하는 도구**
외부 네트워크에서 직접 접근할 수 없는 개발 환경을 **인터넷에 안전하게 노출시키는** 서비스



#### ngrok 설치 및 사용법

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


---

## 🛠 자동화 구축 시나리오

1. **GitHub Webhook 활성화**  
코드 변경 발생 시 Jenkins로 API 요청 전송  

2. **소스 다운로드 및 빌드**  
Jenkins가 해당 리포지토리에 접근하여 소스를 다운로드  
Gradle/Maven 빌드를 통해 **JAR 파일 생성**  

3. **자동 배포**  
생성된 JAR 파일을 서버에 자동 배포  

4. **환경 설정**  
애플리케이션 실행 포트를 **8083**으로 변경하여 실행 환경에 맞춤  

5. **테스트 및 모니터링**  
빌드/배포 과정을 실시간 모니터링  
  로그 검증 및 정상 동작 여부 확인  


---

# 초기 과정

## 1. github webhook 등록
   <img width="869" height="230" alt="image" src="https://github.com/user-attachments/assets/04760750-65ef-48bd-8498-1b67e134878b" />

- 빌드에서 해야하는 일은 소스코드를 저장소에서 pull 받고 해당 소스코드의 의존성을 다운로드 받은 후, 애플리케이션으로 바로 실행할 수 있도록 jar파일로 묶는 행위가 포함
- 위 과정을 빌드에서 수행해주면 배포될 인스턴스가 여러개 있어도 소스코드와 의존성을 다운로드 받는 과정은 오직 한 번만 이루어 지면 됨

## 2. 새로운 item 등록

| 2-1. 해당 링크의 리포지토리 복사 | 2-2. 복사한 리포지토리 링크<br> jenkins item 에 gitHub project 에 등록 | 2-3. 자동화 파이프라인 스크립트 추가 |
|---------|---------|---------|
| <img src="https://github.com/user-attachments/assets/abfc4b94-6fbc-45bd-8a7c-1a4b66f2f2b6" width="441" height="320" /> | <img src="https://github.com/user-attachments/assets/f759dd8f-121c-4689-9503-d0ff0b1cee2d" width="660" height="320" />  | <img width="1269" height="662" alt="image" src="https://github.com/user-attachments/assets/b502bf88-71d2-4cf0-8fda-e0d686a3baf4" /> |


# Maven 빌드 과정

## 1. 아키택처
<img width="718" height="451" alt="image" src="https://github.com/user-attachments/assets/88232e62-89a3-4fdb-ae0a-d1ab2d4f3b37" />


1. **Github 에서 jenkins로 API요청이 오면 해당 리포지토리로 접근해 소스를 다운받고 JAR파일로 만들고 배포하도록 설정한다.**
2. **Github Webhook 기능을 활성화 시킨다.**
3. **애플리케이션의 포트를 8080포트로 변경시킨다.**
4. **테스트해보면서 log 기록 확인한다.**

## 2. Pipeline Script 코드

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

### Stage 별 설명

**1. Checkout**
- `git branch: 'main'` → `main` 브랜치의 코드를 clone
- 성공적으로 pull되면 **"Pull 성공"** 메시지를 출력

**2. Build (Maven Wrapper)**
- `CI_CD_OS_maven` 디렉토리로 이동한 뒤, Maven Wrapper(`mvnw.cmd`)를 실행 
- 실행 명령어
  ```bash
  .\mvnw.cmd -B clean package -DskipTests
  ```
- B → 배치 모드(로그 최소화)
- clean → 이전 빌드 산출물 삭제
- package → 프로젝트 빌드 후 패키징(JAR 생성)
- DskipTests → 테스트 실행 생략

#### 3. Check JAR
- target 디렉토리 내에 JAR 파일이 존재하는지 확인

   ```bash
   dir /-C target\*.jar
   ```
   JAR 파일이 없으면 "JAR 파일이 없습니다!" 메시지를 출력

#### 4. Archive

<img width="400" height="133" alt="image" src="https://github.com/user-attachments/assets/713667b3-ef4b-4fc8-8f22-e307871d61f3" />

- target/*.jar 파일을 Jenkins 빌드 아티팩트로 저장
- fingerprint: true 옵션을 통해 추적 가능성을 보장


## 3. 빌드

<img width="764" height="269" alt="image" src="https://github.com/user-attachments/assets/af7302ce-d194-4c2d-84c9-81bd55a0125a" />


---

# Gradle 실행 과정

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

### 1. Docker 환경 설정

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

## 2. Pipeline Script 코드

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



### 3. 빌드

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

## 🚨 Troubleshooting

### 일반적인 문제와 해결책

| 문제 | 원인 | 해결책 |
|------|------|--------|
| `Permission denied` | 볼륨 마운트 권한 부족 | `sudo chown -R 1000:1000 /opt/builds` |
| `Java not found` | Java 17 미설치 | Jenkins 컨테이너에 Java 17 설치 |
| `JAR not found` | Gradle 빌드 실패 | `./gradlew build` 로그 확인 |
| `Directory not exists` | 볼륨 마운트 미설정 | Docker 컨테이너 재시작 시 `-v` 옵션 추가 |

---
