# Memoir

스크린샷, 웹 링크, PDF처럼 저장해 둔 정보를 기기 안의 Gemma 4로 분석·구조화하고, 필요할 때 다시 찾는 Android 개인 정보 데이터베이스입니다. 지금은 이미지
수집이 연결되어 있고, 링크와 PDF는 같은 큐·분석 흐름으로 이어갈 예정입니다.

수집한 원본과 AI 분석은 기기 안에서 처리합니다. 인터넷은 모델 다운로드와 Crashlytics 같은 앱 운영에만 쓰고, 분석할 사용자 콘텐츠는 외부 AI 서버로 보내지
않습니다. 저장과 분석은 분리되어 있어서, 항목은 바로 큐에 넣고 요약은 이후에 순차 실행합니다.

## 현재 동작

지금 받는 입력은 이미지입니다. Share와 시스템 Photo Picker로 확인한 뒤 Item과 분석 작업(`queued`)을 저장합니다. 큐는 Foreground
Service에서 순서대로 처리합니다. ML Kit 문자 인식(라틴·한국어·일본어·중국어·데바나가리)으로 텍스트를 읽고, 설치·선택한 Gemma 4 E2B 또는 E4B가 GPU에서
요약합니다. 결과는 로컬 DB에 남고, 기록과 항목 상세에서 원문 텍스트로 볼 수 있습니다.

분석 시작은 두 가지입니다.

- **수동 분석** (기본): 큐 화면 Play FAB로 시작·재개
- **즉시 분석**: 활성 큐가 있으면 메인 화면이 분석을 바로 시작

설정에서 고를 수 있는 **지정된 시간에 분석**은 아직 스케줄이 없고, 시작 동작은 수동 분석과 같습니다. 로드 실패는 즉시 실패로 끝나고, OCR과 추론 실패는 한 번
재시도합니다. 작업은 개별·전체 삭제할 수 있습니다.

모델은 크기에 따라 받습니다. OCR은 ML Kit, Gemma 4 E2B/E4B `.litertlm`은 Hugging Face Hub에서 `DownloadManager`로 앱
전용 `files/models`에 저장합니다. 설정 > 모델 관리에서 Gemma 설치·선택과 OCR 활성화를 바꿉니다.

아직 없는 것: 온보딩 게이트(권한·Crashlytics·모델 화면은 placeholder), 홈 최근 항목, 자연어 검색, 웹 링크·PDF, 일정·알림 등 제안 행동, 태블릿
레이아웃.

## 이후 범위

| 단계        | 내용                                                         |
|-----------|------------------------------------------------------------|
| P0 (진행 중) | 온보딩 완료 조건(진행도 + OCR + 멀티모달 모델 1개), 홈·큐·설정 UI, 엔티티 추출       |
| P1        | 시멘틱 검색, URL·PDF 수집, 분석 엔티티 기반 제안 행동(일정, 알림, 지도, 전화, 계좌 복사) |
| P2        | 태블릿 master-detail, 하드웨어 키보드 단축키                            |

## 스택

- Kotlin, Jetpack Compose, Material 3 (`MemoirTheme`). 다크/라이트는 시스템을 따른다
- minSdk 30, compileSdk / targetSdk 37, `applicationId` `minmul.memoir`
- JDK 17, Android Gradle Plugin 9.4, Gradle 9.8 (wrapper)
- Hilt + KSP, Navigation 3, Room 3 + BundledSQLite
- ML Kit Text Recognition, LiteRT-LM Gemma GPU
- Timber (debug 빌드만)

## 모듈

feature 모듈은 서로 의존하지 않습니다. 탭과 Activity 조립은 `:app`이 합니다.

| 모듈                     | 역할                                                   |
|------------------------|------------------------------------------------------|
| `:app`                 | Application, MainActivity, IntakeActivity, 탭·온보딩 게이트 |
| `:feature:onboarding`  | 온보딩                                                  |
| `:feature:intake`      | 가져오기 확인                                              |
| `:feature:main`        | 홈, 보관함, 항목 상세                                        |
| `:feature:queue`       | 분석 큐, 분석 기록                                          |
| `:feature:settings`    | 설정, 모델 관리                                            |
| `:feature:search`      | 검색 (P0에서 미연결)                                        |
| `:background:analysis` | 순차 OCR / Gemma 분석 Foreground Service                 |
| `:data:content`        | 원본·항목·분석 작업 저장과 조회                                   |
| `:data:model`          | Gemma `.litertlm` 다운로드                               |
| `:data:preferences`    | 온보딩, OCR, 모델, 큐 모드, Crashlytics 설정                   |
| `:core:model`          | 공유 도메인 모델                                            |
| `:core:storage`        | Room, 파일, DataStore                                  |
| `:core:ai`             | OCR, Gemma GPU 엔진                                    |
| `:core:design`         | 테마와 공통 UI                                            |

## 빌드와 테스트

Android Studio에서 열거나 wrapper로 실행합니다.

```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
```

계측 테스트와 스크린샷 테스트를 바꾼 경우에만 각각 `connectedDebugAndroidTest`, `validateDebugScreenshotTest`를 실행합니다.

디버그 빌드의 분석 진단 로그는 Logcat 태그 `MemoirAnalysis` (Verbose)입니다. 릴리스 빌드에서는 출력하지 않습니다.

## 문서

제품·아키텍처·테스트 기준은 `docs/`에 있습니다.

| 문서                | 내용                |
|-------------------|-------------------|
| `docs/PRD.md`     | 제품 요구사항과 P0–P2 범위 |
| `docs/PROJECT.md` | 스택, 모듈 책임, 의존성    |
| `docs/TESTING.md` | 테스트 작성·실행         |
| `docs/BACKLOG.md` | 현재 구현 단계와 후속 작업   |
