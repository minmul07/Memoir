> 작업 전 `docs/PROJECT.md`를 읽고 프로젝트 사실/컨벤션을 확인한다.

---

## Android 도구 및 Skill 사용

- 일반적인 코드 확인과 검증은 파일 검색, shell, Gradle 등 가장 가벼운 방법을 우선한다.
- 단순 파일 확인을 위해 IDE/MCP나 런타임 도구를 불필요하게 사용하지 않는다.
- Compose의 복잡한 state, side-effect, recomposition 또는 performance 문제는 `compose-expert`를 사용한다.
- ADB, logcat, dumpsys, crash, ANR 분석이 필요한 경우 `android-adb-debugging`을 사용한다.
- 실제 디바이스/에뮬레이터 화면 또는 사용자 플로우 검증이 필요한 경우 `android-mobile-mcp`를 사용한다.
- Android Studio / JetBrains MCP는 IDE inspection, run configuration, 프로젝트 구조 등 IDE 관점의 정보가 실제로 필요할 때 사용한다.
- 앱 데이터 삭제, uninstall, `pm clear` 등 파괴적인 작업은 사용자의 명시적인 요청 없이 수행하지 않는다.
- 동일한 목적의 검증을 여러 도구로 불필요하게 반복하지 않는다.

---

## 완료 기준

작업은 다음 조건을 만족해야 완료된 것으로 본다.

1. 요청된 동작 또는 변경이 구현되어 있다.
2. 요청과 무관한 파일이나 동작을 변경하지 않았다.
3. `docs/TESTING.md` 기준에 맞는 필요한 검증을 수행했다.
4. 검증이 실패하거나 수행할 수 없는 경우 원인과 영향 범위를 명확히 보고했다.
5. 외부 API, 백엔드 또는 불명확한 동작에 대한 중요한 가정을 명시했다.
6. 위험 요소나 남은 작업이 있다면 숨기지 않고 보고했다.

---

## 작업 완료 응답 형식

작업 완료 후 필요한 항목만 간결히 보고한다.

### 변경 사항

* 변경한 내용과 주요 파일

### 검증

* 실행한 검증과 결과

### 가정 / 남은 작업

* 중요한 가정, 미검증 사항, 남은 TODO가 있는 경우에만 작성

사용하지 않은 도구를 모두 나열하지 않는다.

수행하지 않은 검증은 결과의 신뢰성에 영향을 주는 경우에만 이유를 보고한다.

Android Studio / JetBrains MCP, ADB, mobile-mcp 등 런타임 또는 IDE 기반 도구를 실제로 사용한 경우에는 수행한 조작과 핵심 결과를 간결히 명시한다.

위험하거나 불확실한 부분은 조용히 추측하지 말고 명시적으로 보고한다.

---

## Documentation

After implementing a change, inspect relevant project documentation.

Update documentation in the same change when the implementation changes:
- externally observable behavior
- architecture or module responsibilities
- APIs or data contracts
- setup, build, or development workflows
- non-obvious technical constraints

Do not update documentation for implementation-only refactors that do not
change documented behavior.

Keep documentation changes minimal and scoped to the affected area.
Do not rewrite unrelated documentation.

Do not modify AGENTS.md automatically.
If you discover a recurring project rule, constraint, or workflow that should
be preserved for future agents, suggest an AGENTS.md change to the user.

---

## Gradle

에이전트에서 `./gradlew`를 실행할 때 배포본을 다시 받거나 데몬을 새로 띄우면, `GRADLE_USER_HOME`을 호스트 사용자 홈의 `.gradle`(
`C:\Users\ddddd\.gradle`)로 명시한다. Android Studio/일반 터미널과 같은 캐시·데몬을 쓰고, 시스템 PATH의 `gradle`로 우회하지 않는다.