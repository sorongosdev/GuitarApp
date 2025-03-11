# 🎸 기타등등 - 기타 입문자를 위한 학습용 어플리케이션

## 📖 1. 프로젝트 소개

- 기존의 기타 학습용 어플리케이션은, 사용자가 연주한 기타 코드를 판단해주지만, 박자를 감지하지는 못합니다.
- 혹은 음향 파일에서 기타 코드를 추출해주지만, 사용자가 연주한 기타 코드에 대한 판단은 하지 않습니다.
- 그래서 코드와 박자 추정이 가능하며, 사용자에게 피드백이 가능한 어플리케이션을 개발했습니다.

## 👤 2. 팀원 구성 및 역할

### 1) 팀원 구성

|   |   |
|---|---|
| 도소라 | 앱 UI |
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

## 📜 7. 기능 설명

### 📄 1) 손님 입장

- 손님이 입장하면 웰컴드링크를 선택하도록 합니다.
- 회원카드를 RFID 센서에 태그해 대기 등록을 도와줍니다.
- 현재 대기 상황을 알려줍니다.
- 웰컴드링크가 준비되면, 빈 자리로 손님을 안내합니다.

| 웰컴드링크 선택 | 대기 등록 | 대기 안내 |
| :-----: | :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/2bb175b7-2615-4b0d-9f60-56052b90e5fa) | ![image](https://github.com/user-attachments/assets/12661d60-d29d-440b-abc2-a123efa5c4a1) | ![image](https://github.com/user-attachments/assets/36930b2b-f3ae-4944-930f-5ca2fa93dc99) |

| 웰컴드링크와 함께 빈자리 안내 전 알림 | 자리 안내 중 알림 |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/35cbc623-6292-45dd-8441-ca1b988ac288) | ![image](https://github.com/user-attachments/assets/1fd7ea61-1c01-4ff8-9707-cd36fa0e08ba) |

### 📄 2) 시술 동의서

- 대기하는 손님이 시술에 동의한다는 시술 동의서를 작성할 수 있도록 합니다.
- 시술 시작을 알립니다.

| 시술 동의 | 시술 시작 안내 |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/5618c14b-3ffe-47f5-9081-15c15b9cc710) | ![image](https://github.com/user-attachments/assets/7b701af5-2aa6-437f-946d-403cfb0dd794) |

### 📄 3) 시술 진행 정도 확인

- 시술 진행 정도를 보여주는 화면입니다.
- 헤어 디자이너는 진행 정도를 실시간으로 손님에게 테미를 통해 보여주어, 시술에 집중할 수 있습니다.

| 시술 진행 시작 | 시술 진행 1단계 |
| :-----: | :-----: | 
| ![image](https://github.com/user-attachments/assets/9c550404-f35e-4932-84ee-c1b3aae232b9) | ![image](https://github.com/user-attachments/assets/c4958d8b-e68f-4af5-8fb9-93a361ab3825) |

| 시술 진행 2단계 | 시술 완료 |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/82bcf4e2-3da6-46cb-9cbf-df77d8b1f0da) | ![image](https://github.com/user-attachments/assets/d74f8bc7-04be-4e13-b6f0-36a71a8863f1) |

### 📄 4) 시술중 지원 서비스

- 뒤쪽에서 테미의 카메라로 실시간으로 두피 상태를 보여주는 서비스를 제공합니다.
- 원하는 헤어 시술만 직접 선택함으로써, 의도하지 않은 추가 서비스에 대한 요금이 발생하는 것을 미리 방지할 수 있습니다.

| 두피 상태 확인 | 원하는 머리 시술 선택 |
| :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/c894efc2-ffd7-4cc0-afe8-281d5c193ff8) | ![image](https://github.com/user-attachments/assets/275a03a3-16cc-499b-8fde-397d5e480c71) |

### 📄 5) 기타 서비스

- 테미를 통해 데스크의 직원을 호출할 수 있습니다.
- 원하는 헤어 제품을 직접 선택할 수 있어 부담 없이 고르고, 결제까지 가능합니다.
- 테미의 선반 위에 휴대폰을 올려두면 충전이 가능합니다.

| 도움 요청 | 상품 구매 | 휴대폰 충전 안내 |
| :-----: | :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/09998cca-31b9-41a5-b1d2-64b7a6eeb6df) | ![image](https://github.com/user-attachments/assets/be59393b-98b9-4eda-99f0-7426d15eb15e) | ![image](https://github.com/user-attachments/assets/45cd6925-cf37-411b-9ee6-ade8eb4ef423) |

### 📄 6) 결제

- 앞서 선택한 머리 시술과, 헤어 제품에 관한 결제를 확인할 수 있습니다.
- RFID 센서가 회원카드를 읽어 결제할 수 있도록 합니다.
- 남은 멤버십 포인트를 알려줍니다.

| 결제 확인 | 회원카드 결제창 | 남은 멤버십 포인트 안내 |
| :-----: | :-----: | :-----: |
| ![image](https://github.com/user-attachments/assets/c068c342-eca1-4346-bf61-198683bf849b) | ![image](https://github.com/user-attachments/assets/62cffd80-caf7-4a6c-aabe-4b252694a994) | ![image](https://github.com/user-attachments/assets/b3d23a2a-da98-491e-b673-0575f9ac0bc9) |
