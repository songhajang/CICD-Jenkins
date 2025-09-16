# CI/CD Jenkins

## GitHub Webhook이란?

GitHub Webhook은 특정 이벤트가 발생 시 다른 URL로 API 호출을 할 수 있게 해주는 역할을 합니다.

## 자동화 적용 후 예상 과정

1. **CPU 바운드 애플리케이션을 로컬에서 수정한 후 Github에 push한다.**
2. **Github Webhook이 동작해 jenkins에게 API 요청을 날린다.**
3. **jenkins는 Github로부터 온 API 요청을 받아서 저장소의 소스코드를 다운받고 스프링 부트 프로젝트를 빌드하여 JAR파일로 만든다.**
4. **jenkins가 JAR파일을 CPU 워커 인스턴스들에 배포하고 실행시킨다.**

## 자동화 적용을 위한 작업 과정

1. **Github 에서 jenkins로 API요청이 오면 해당 리포지토리로 접근해 소스를 다운받고 JAR파일로 만들고 배포하도록 설정한다.**
2. **Github Webhook 기능을 활성화 시킨다.**
3. **애플리케이션의 포트를 8080포트로 변경시킨다.**
4. **테스트해보면서 삽질한다.**
5. **자동화 성공 이후 무중단 배포를 위해 추가 설정을 한다.**

## Jenkins에 Github Webhook 빌드 유발 설정

빌드에서 해야하는 일은 소스코드를 저장소에서 pull 받고 해당 소스코드의 의존성을 다운로드 받은 후, 애플리케이션으로 바로 실행할 수 있도록 jar파일로 묶는 행위가 포함됩니다.

위 과정을 빌드에서 수행해주면 배포될 인스턴스가 여러개 있어도 소스코드와 의존성을 다운로드 받는 과정은 오직 한 번만 이루어 지면 됩니다. (각 인스턴스마다 소스코드, 의존성을 다운로드 받게되면 배포시간이 늘어날 것입니다.)

### 2-1. 리포지토리 fork
해당 링크의 리포지토리를 fork한다. 내 리포지토리에 추가된다.

### 2-2. 리포지토리 등록

## ngrok 설정

ngrok은 로컬 서버를 인터넷에 노출시켜 외부에서 접근할 수 있게 해주는 터널링 도구입니다.

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
   ```bash
   # Jenkins가 8080 포트에서 실행 중일 때
   ngrok http 8080
   
   # 또는 특정 포트 지정
   ngrok http 8080 --subdomain=my-jenkins
   ```

4. **ngrok URL 확인**
   - ngrok 실행 후 제공되는 `https://xxxxx.ngrok.io` URL을 GitHub Webhook 설정에 사용

## Jenkins 자동화 파이프라인 스크립트

### 실제 운영 중인 Pipeline Script (Windows 환경)

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

### 참고용 Pipeline Script (Linux/Unix 환경)

```groovy
pipeline {
    agent any
    
    environment {
        GITHUB_REPO = 'your-username/your-repo'
        JAR_NAME = 'target/your-app.jar'
        DEPLOY_PATH = '/opt/app/'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '소스코드 체크아웃 중...'
                git branch: 'main', url: "https://github.com/${GITHUB_REPO}.git"
            }
        }
        
        stage('Build') {
            steps {
                echo 'Maven 빌드 시작...'
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                echo '테스트 실행 중...'
                sh 'mvn test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                echo 'JAR 파일 배포 중...'
                script {
                    // 기존 프로세스 종료
                    sh 'pkill -f "java.*your-app" || true'
                    
                    // 새 JAR 파일 배포
                    sh "cp ${JAR_NAME} ${DEPLOY_PATH}"
                    
                    // 애플리케이션 실행
                    sh "nohup java -jar ${DEPLOY_PATH}your-app.jar --server.port=8080 > app.log 2>&1 &"
                }
            }
        }
        
        stage('Health Check') {
            steps {
                echo '헬스 체크 수행 중...'
                script {
                    sleep(10) // 애플리케이션 시작 대기
                    sh 'curl -f http://localhost:8080/actuator/health || exit 1'
                }
            }
        }
    }
    
    post {
        success {
            echo '파이프라인 성공!'
            // Slack 알림 등 추가 가능
        }
        failure {
            echo '파이프라인 실패!'
            // 실패 알림 등 추가 가능
        }
        always {
            echo '파이프라인 완료'
        }
    }
}
```

### Pipeline Script (Scripted)

```groovy
node {
    def mavenHome = tool name: 'Maven-3.8.6', type: 'maven'
    def javaHome = tool name: 'JDK-11', type: 'jdk'
    
    env.PATH = "${mavenHome}/bin:${javaHome}/bin:${env.PATH}"
    
    stage('Checkout') {
        checkout scm
    }
    
    stage('Build') {
        sh 'mvn clean package -DskipTests'
    }
    
    stage('Test') {
        sh 'mvn test'
        publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
    }
    
    stage('Deploy') {
        sh '''
            pkill -f "java.*your-app" || true
            cp target/your-app.jar /opt/app/
            nohup java -jar /opt/app/your-app.jar --server.port=8080 > app.log 2>&1 &
        '''
    }
    
    stage('Health Check') {
        sleep(10)
        sh 'curl -f http://localhost:8080/actuator/health'
    }
}
```

### GitHub Webhook 설정

1. **GitHub 저장소 설정**
   - Settings → Webhooks → Add webhook
   - Payload URL: `https://your-ngrok-url.ngrok.io/github-webhook/`
   - Content type: `application/json`
   - Events: `Just the push event`

2. **Jenkins Webhook 설정**
   - Jenkins → Manage Jenkins → Configure System
   - GitHub Web Hook 설정 활성화
   - Jenkins URL: `https://your-ngrok-url.ngrok.io`

## 실제 운영 환경 성능 데이터

### Jenkins 파이프라인 실행 시간 분석

**평균 단계별 실행 시간:**
- **전체 실행 시간**: 약 30초
- **Checkout**: 3초 (소스코드 다운로드)
- **Build (Maven Wrapper)**: 21초 (가장 오래 걸리는 단계)
- **Check JAR**: 671ms (JAR 파일 검증)
- **Archive**: 542ms (아티팩트 아카이브)

**최근 빌드 성능:**
- Build #16: 전체 30초 (Build 단계 33초)
- Build #15: 전체 30초 (Build 단계 33초) 
- Build #14: 전체 30초 (Build 단계 38초)
- Build #13: 전체 30초 (Build 단계 37초)

**생성되는 아티팩트:**
- `CI_CD_OS_maven-0.0.1-SNAPSHOT.jar` (20.01 MiB)

### 성능 최적화 팁

1. **Maven 빌드 최적화**
   ```bash
   # 병렬 빌드 활성화
   mvn clean package -T 4 -DskipTests
   
   # 의존성 캐시 활용
   mvn clean package -o -DskipTests
   ```

2. **Docker를 활용한 빌드 환경**
   ```dockerfile
   FROM maven:3.8.6-openjdk-11
   WORKDIR /app
   COPY pom.xml .
   RUN mvn dependency:go-offline
   COPY src ./src
   RUN mvn clean package -DskipTests
   ```

## CI/CD 파이프라인 워크플로우

```
Local Development → GitHub → Webhook → Jenkins → Build → Deploy to CPU Worker Instance
```

1. **Local Development**: 개발자가 로컬에서 코드를 수정
2. **GitHub**: 코드를 GitHub 저장소에 push
3. **Webhook**: GitHub에서 Jenkins로 API 요청 전송
4. **Jenkins**: 소스코드 다운로드 및 JAR 파일 빌드
5. **Deploy**: JAR 파일을 CPU 워커 인스턴스에 배포 및 실행

## Jenkins 파이프라인 모니터링

### Stage View 활용법

1. **빌드 상태 확인**
   - 초록색: 성공
   - 빨간색: 실패
   - 파란색: 진행 중

2. **성능 분석**
   - 각 단계별 실행 시간 비교
   - 병목 지점 식별 (Build 단계가 가장 오래 걸림)
   - 평균 실행 시간 추적

3. **변경사항 추적**
   - 각 빌드별 커밋 정보 확인
   - 변경사항과 빌드 결과 연관성 분석

### 알림 설정

```groovy
post {
    success {
        slackSend (
            channel: '#dev-notifications',
            color: 'good',
            message: "✅ 빌드 성공: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
        )
    }
    failure {
        slackSend (
            channel: '#dev-notifications',
            color: 'danger',
            message: "❌ 빌드 실패: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
        )
    }
}
```