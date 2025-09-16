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
