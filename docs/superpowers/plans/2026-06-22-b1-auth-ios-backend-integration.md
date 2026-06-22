# B1 Auth iOS Backend Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring B1 authentication from backend-only L2 verification to complete iOS-to-Gateway local integration covering register, login, current user, error states, and sensitive-log safety.

**Architecture:** iOS must use Gateway as the only external backend entry point. Gateway authenticates Access Tokens, overwrites trusted FoodMap identity headers, and routes to Auth/User services; iOS stores tokens in Keychain and validates restored sessions through `/api/users/me`.

**Tech Stack:** SwiftUI, MVVM, URLSession, Keychain, Spring Cloud Gateway, Spring Boot, OpenFeign, PostgreSQL, Flyway, MyBatis, Maven.

---

## File Structure

Frontend files to modify:

- `front/FoodMapApp/FoodMapApp/Core/Networking/APIClient.swift`: shared request builder, GET/POST support, auth header injection, response parsing.
- `front/FoodMapApp/FoodMapApp/Core/Networking/APIResponse.swift`: unified response envelope with `status`.
- `front/FoodMapApp/FoodMapApp/Core/Networking/NetworkError.swift`: typed errors for HTTP status, business code/message, decoding and network failures.
- `front/FoodMapApp/FoodMapApp/Features/Auth/AuthModels.swift`: login/register/current-user DTOs.
- `front/FoodMapApp/FoodMapApp/Core/Auth/AuthSessionStore.swift`: login, token persistence, current-user validation, invalid-session cleanup.
- `front/FoodMapApp/FoodMapApp/Features/Auth/LoginViewModel.swift`: Gateway default Base URL and error display source.
- `front/FoodMapApp/FoodMapApp/Features/Auth/RegisterView.swift`: keep Gateway Base URL in sync with login page.
- `front/FoodMapApp/README.md`: local Gateway-first integration instructions.
- `front/FoodMapApp/FoodMapApp.xcodeproj/project.pbxproj`: only if adding a test target.
- `front/FoodMapApp/FoodMapAppTests/*`: URLSession mock and ViewModel/session tests.

Backend and documentation files to modify or verify:

- `docs/api/auth-user.md`: final API contract for register, login, current user, and internal provisioning.
- `docs/integration/B1-auth-ios-backend/integration-plan.md`: safety point, scenarios, evidence.
- `docs/integration/B1-auth-ios-backend/issue-log.md`: blockers and retest results.
- `after/foodmap-gateway-service`: regression only unless Gateway behavior changes.
- `after/foodmap-auth-service`: regression only unless auth response or rollback behavior changes.
- `after/foodmap-user-service`: regression only unless current-user response changes.

## Task 1: Contract And Gateway Baseline

**Files:**
- Modify: `docs/api/auth-user.md`
- Modify: `front/FoodMapApp/README.md`
- Verify: `after/foodmap-gateway-service`
- Verify: `after/foodmap-auth-service`
- Verify: `after/foodmap-user-service`

- [ ] **Step 1: Confirm the external B1 API contract**

Check that `docs/api/auth-user.md` defines:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me
```

Each response must use:

```json
{
  "success": true,
  "status": 200,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

- [ ] **Step 2: Confirm Gateway is the iOS entry point**

Document the local URL:

```text
http://127.0.0.1:18080
```

Do not instruct iOS to call `http://127.0.0.1:18081` except as a backend-only diagnostic path.

- [ ] **Step 3: Run backend regression**

Run:

```bash
cd /Users/zhangxuqian/myPrjDev/food-map/after
mvn -pl foodmap-gateway-service test
mvn -pl foodmap-auth-service test
mvn -pl foodmap-user-service test
```

Expected: all three commands succeed.

- [ ] **Step 4: Commit if only contract/documentation changed**

```bash
git add docs/api/auth-user.md front/FoodMapApp/README.md
git commit -m "docs: align B1 auth gateway contract"
```

## Task 2: Frontend Gateway Base URL

**Files:**
- Modify: `front/FoodMapApp/FoodMapApp/Features/Auth/LoginViewModel.swift`
- Modify: `front/FoodMapApp/FoodMapApp/Features/Auth/RegisterView.swift`
- Modify: `front/FoodMapApp/README.md`

- [ ] **Step 1: Change the default local Base URL**

Set the default B1 local value to:

```swift
"http://127.0.0.1:18080"
```

Keep the text field editable so developers can point to a different local or LAN Gateway.

- [ ] **Step 2: Keep register using the same Base URL as login**

Verify `LoginView` passes the current Base URL into `RegisterView`, and `RegisterView` persists the same value after a registration attempt.

- [ ] **Step 3: Build the app**

Run:

```bash
/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild \
  -project front/FoodMapApp/FoodMapApp.xcodeproj \
  -scheme FoodMapApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath /private/tmp/foodmap-derived \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 4: Commit**

```bash
git add front/FoodMapApp/FoodMapApp/Features/Auth/LoginViewModel.swift front/FoodMapApp/FoodMapApp/Features/Auth/RegisterView.swift front/FoodMapApp/README.md
git commit -m "fix: default iOS auth integration to gateway"
```

## Task 3: APIClient Request And Error Foundation

**Files:**
- Modify: `front/FoodMapApp/FoodMapApp/Core/Networking/APIClient.swift`
- Modify: `front/FoodMapApp/FoodMapApp/Core/Networking/APIResponse.swift`
- Modify: `front/FoodMapApp/FoodMapApp/Core/Networking/NetworkError.swift`
- Modify: `front/FoodMapApp/FoodMapApp/Core/Networking/APIRequestTimeout.swift` if query timeout is reused.
- Test: `front/FoodMapApp/FoodMapAppTests/APIClientTests.swift`

- [ ] **Step 1: Add `status` to the response envelope**

`APIResponse` must decode:

```swift
let success: Bool
let status: Int
let code: String
let message: String
let data: T?
```

- [ ] **Step 2: Support authenticated GET requests**

`APIClient` must support:

```text
GET /api/users/me
Authorization: Bearer <access-token>
X-Request-Id: <uuid-or-traceable-id>
X-Trace-Id: <uuid-or-traceable-id>
```

- [ ] **Step 3: Parse non-2xx error envelopes**

When the backend returns a unified error body, prefer backend `status/code/message` over a generic HTTP status message.

- [ ] **Step 4: Add mock network tests**

Cover these cases with a mock `URLProtocol`:

- 200 success with data.
- 400 validation error with backend message.
- 401 invalid token.
- 403 forbidden.
- 409 duplicate account.
- 500 internal error with sanitized message.
- URLSession transport failure.

- [ ] **Step 5: Run build and tests**

Run the iOS build command from Task 2. If a test target exists, also run:

```bash
/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild \
  -project front/FoodMapApp/FoodMapApp.xcodeproj \
  -scheme FoodMapApp \
  -sdk iphonesimulator \
  -derivedDataPath /private/tmp/foodmap-derived \
  CODE_SIGNING_ALLOWED=NO \
  test
```

Expected: build succeeds; tests pass if the target exists.

- [ ] **Step 6: Commit**

```bash
git add front/FoodMapApp/FoodMapApp/Core/Networking front/FoodMapApp/FoodMapAppTests
git commit -m "feat: add authenticated API client error handling"
```

## Task 4: Current User Session Validation

**Files:**
- Modify: `front/FoodMapApp/FoodMapApp/Features/Auth/AuthModels.swift`
- Modify: `front/FoodMapApp/FoodMapApp/Core/Auth/AuthSessionStore.swift`
- Modify: `front/FoodMapApp/FoodMapApp/App/AppRouter.swift` if a startup loading state is required.
- Test: `front/FoodMapApp/FoodMapAppTests/AuthSessionStoreTests.swift`

- [ ] **Step 1: Add current-user model**

Create a Decodable model matching `/api/users/me`:

```swift
let userId: Int64
let accountId: Int64
let accountName: String
let nickname: String
let avatarMediaId: Int64?
let userStatus: String
```

- [ ] **Step 2: Fetch current user after login**

After saving tokens, call `/api/users/me` through Gateway and build `AuthSession` from the backend response.

- [ ] **Step 3: Validate restored sessions**

When Keychain has tokens on app startup:

1. Show a loading or neutral startup state.
2. Call `/api/users/me`.
3. If it succeeds, restore a real session.
4. If it returns 401/403 or decoding fails, clear Keychain and show `LoginView`.

- [ ] **Step 4: Remove `accountId/userId = 0` as a logged-in session**

Zero identifiers may be used only as preview data or explicit loading placeholders, not as an authenticated runtime session.

- [ ] **Step 5: Run build/tests**

Use the commands from Task 3.

- [ ] **Step 6: Commit**

```bash
git add front/FoodMapApp/FoodMapApp/Features/Auth/AuthModels.swift front/FoodMapApp/FoodMapApp/Core/Auth/AuthSessionStore.swift front/FoodMapApp/FoodMapApp/App/AppRouter.swift front/FoodMapApp/FoodMapAppTests
git commit -m "feat: validate iOS auth sessions with current user"
```

## Task 5: Full Local L2 Integration

**Files:**
- Modify: `docs/integration/B1-auth-ios-backend/integration-plan.md`
- Modify: `docs/integration/B1-auth-ios-backend/issue-log.md`
- Add evidence under: `docs/integration/B1-auth-ios-backend/screenshots`, `network`, `logs`, `tests` when useful.

- [ ] **Step 1: Start backend services**

Start PostgreSQL and services according to the latest backend local scripts. Gateway must be reachable at:

```text
http://127.0.0.1:18080
```

- [ ] **Step 2: Register through iOS**

Use a fresh account name:

```text
codex_auth_it_YYYYMMDD_<short>
```

Expected: register succeeds, response includes `accountId` and `userId`, no password or token is recorded in evidence.

- [ ] **Step 3: Login through iOS**

Expected:

- Login succeeds through Gateway.
- Token is saved in Keychain.
- App enters `MapHomeView`.
- No token is printed in Xcode output or saved evidence.

- [ ] **Step 4: Query current user through iOS**

Expected:

- `GET /api/users/me` returns the same `accountId/userId` as registration/login.
- Gateway overwrites any forged FoodMap identity headers.
- `AuthSessionStore.session` uses real identifiers.

- [ ] **Step 5: Run failure scenarios**

Execute:

- Wrong password.
- Duplicate account.
- Missing required registration field.
- Expired or invalid Access Token if easy to create.
- Backend unavailable or wrong port.

Expected: UI shows clear messages and keeps sensitive values out of logs.

- [ ] **Step 6: Update integration docs**

Update `integration-plan.md` scenario statuses and `issue-log.md` with every blocker or high-risk issue.

- [ ] **Step 7: Run verification**

Run:

```bash
./harness/scripts/validate-integration.sh
./harness/scripts/run-all.sh
```

Expected: scripts pass or any skipped checks are explained in the integration plan.

- [ ] **Step 8: Commit final integration evidence**

```bash
git add docs/integration/B1-auth-ios-backend
git commit -m "docs: record B1 auth ios integration evidence"
```

## Safety Point Checklist

Do not start full B1 iOS L2 integration until all P0 items are complete:

- [ ] iOS default/recommended Base URL is Gateway.
- [ ] `APIClient` supports GET, POST, Bearer Token, requestId and traceId.
- [ ] `APIResponse` includes `status`.
- [ ] Non-2xx backend error envelopes are parsed.
- [ ] Login success calls `/api/users/me`.
- [ ] Token restore calls `/api/users/me`.
- [ ] 401/403 clears invalid local session.
- [ ] `/internal/users/provision` remains blocked from external Gateway callers.
- [ ] Auth register rollback remains verified when user provisioning fails.
- [ ] User service accountId mismatch remains 403.
- [ ] iOS Debug build succeeds.
- [ ] No Token, password, full phone or full email appears in evidence.

## Self-Review

Spec coverage:

- Register, login and current-user flows are covered by Tasks 2 through 5.
- Gateway-only external access is covered by Tasks 1, 2 and 5.
- Error response parsing is covered by Task 3.
- Token persistence and restored-session validation are covered by Task 4.
- Backend regression and evidence are covered by Tasks 1 and 5.

Remaining known gap:

- Refresh Token auto-renewal is intentionally out of B1 full L2 scope and should be tracked as a later auth hardening task.
