# 🎸 기타등등 - 기타 입문자를 위한 학습용 어플리케이션

## 📖 1. 프로젝트 소개

- 기존의 기타 학습용 어플리케이션은, 사용자가 연주한 기타 코드를 판단해주지만, 박자를 감지하지는 못합니다.
- 혹은 음향 파일에서 기타 코드를 추출해주지만, 사용자가 연주한 기타 코드에 대한 판단은 하지 않습니다.
- 그래서 코드와 박자 추정이 가능하며, 사용자에게 피드백이 가능한 어플리케이션을 개발했습니다.

## 👤 2. 팀원 구성 및 역할

### 1) 팀원 구성

| 도소라 | 앱 UI |
|---|---|
| 김예은 | 알고리즘 |

### 2) 본인 역할

#### 🐚 도소라 | 앱 UI

- hps 알고리즘 레퍼런스 코드 탐색
- 파이썬과 코틀린 연동 가능한 chaoqupy 라이브러리 적용
- Compose를 이용한 악보/노트 뷰 개발, 진행바 애니메이션 구현
- WAV로 변환 후 알고리즘에 입력

## 🛠️ 3. 개발 환경

### 🔍 1) 프레임워크 및 언어
- Front-end: Kotlin (1.9.0)
- Back-end: None

### 🔧 2) 개발 도구
- Android Studio: Hedgehog (2023.1.1)
- Gradle: 8.4
- Android Gradle Plugin: 8.2.1
- JDK: 17 (JetBrains Runtime)

### 📱 3) 테스트 환경
- Android 에뮬레이터: API 레벨 34 (Android 14)
- 갤럭시탭 S7: API 레벨 34 (Android 14)

### 📚 4) 주요 라이브러리 및 API
- tarsosDSP: 2.5

### 🔖 5) 버전 및 이슈 관리
- Git: 2.43.0

### 👥 6) 협업 툴
- 커뮤니케이션: Slack

## ▶️ 4. 프로젝트 실행 방법

### ⬇️ 1) 필수 설정 사항

#### ① 기본 환경
- Android Studio (최신 버전)
- Java JDK (Java 8 이상)
- Android SDK (minSdk 24, targetSdk 34, compileSdk 34)
- Python (Python 3.12 - Chaquopy 사용)

#### ② 필수 의존성 패키지
- androidx.core:core-ktx: 1.13.1
- androidx.lifecycle:lifecycle-runtime-ktx: 2.7.0
- androidx.activity:activity-compose: 1.9.0
- androidx.compose: compose-bom: 2024.05.00
- androidx.lifecycle:lifecycle-viewmodel-compose: 2.7.0
- Python 패키지: numpy, matplotlib, wave

### ⿻ 2) 프로젝트 클론 및 설정
- 프로젝트 클론
```bash
git clone https://github.com/sorongosdev/GuitarApp.git
```
- 의존성 설치
```bash
# Mac
./gradlew --refresh-dependencies

# Window
gradlew.bat --refresh-dependencies
```

### 🌐 3) 앱 빌드
```bash
./gradlew build
```

## 📁 5. 프로젝트 구조
```
tarsos_example
├─ MainActivity.kt  # 앱의 메인 액티비티, UI 구성, 녹음 권한 요청, 레이아웃 관리
├─ MyApplication.kt  # 앱의 전역 컨텍스트 제공 Application 클래스
├─ consts
│  ├─ AnswerTypes.kt  # 사용자 연주 정답 상태 코드 및 색상 매핑 정의
│  ├─ ChordTypes.kt  # 기타 코드 관련 매핑 및 탭 위치 정보
│  ├─ NoteTypes.kt  # 악보 노트 패턴 및 정답 판정을 위한 패턴 정의
│  └─ WavConsts.kt  # 녹음 및 타이밍 관련 상수 정의
├─ model
│  └─ MyViewModel.kt  # 앱 데이터 상태 관리, 박자/코드 정확도 판정, 피드백 생성 로직
└─ utils
   ├─ AudioProcessorHandler.kt  # 오디오 녹음 및 처리
   ├─ CanvasDraw.kt  # Canvas를 사용한 악보, 코드, 탭, 진행 바 등 그래픽 요소 그리기
   └─ RMSProcessor.kt  # 오디오 신호의 RMS(Root Mean Square) 값 계산 프로세서
```

## 📅 6. 개발 기간
- 전체 프로젝트 기간: 2023.10 ~ 2024.05
- 기획 및 디자인: 2023.10 ~ 2023.12
- 개발: 2024.01 ~ 2024.05

## 📜 7. 흐름별 기능 설명

### 📄 1) 연주 전 악보

- 랜덤한 기타 코드 운지법과 기타 악보를 보여줍니다.

| 기타 코드와 악보 표출 |
| :-----: |
| ![image](https://github.com/user-attachments/assets/155d7c3f-07ae-43f5-85a0-79dc3534f603) |

### 📄 2) 카운트 다운 시작

- 화면 중앙 상단의 **연주시작** 버튼을 누르면 메트로놈 카운트다운이 4박자동안 진행됩니다.

| 카운트 다운 |
| :-----: |
| ![카운트다운](https://github.com/user-attachments/assets/77e2a099-89cd-4c8e-bea0-fe369a083ced) |

### 📄 3) 사용자 연주

- 사용자는 진행바에 따라 연주합니다.

| 사용자 연주 |
| :-----: |
| ![진행바](https://github.com/user-attachments/assets/aeef6c6c-2986-4861-8fc3-eeff323c8507) |

### 📄 4) 사용자에게 피드백

- 박자 피드백: 사용자가 연주한 곳에 음표를 표시합니다.
- 코드 피드백: 음표의 색으로 사용자가 정확한 기타 코드를 쳤는지에 대한 피드백을 해줍니다.

| 박자 O, 코드 O | 박자 O, 코드 X |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/420474ab-243e-415d-9315-d460ec371809) | ![image](https://github.com/user-attachments/assets/7d4deda5-2e96-46ff-9ca3-8f520d8ae010) |

| 박자 X, 코드 O | 박자 X, 코드 X |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/e39d7d20-c51c-446f-b4e7-acd11e39edb7) | ![image](https://github.com/user-attachments/assets/49c73f81-a582-4168-b41c-089f0c1ac07f) |
