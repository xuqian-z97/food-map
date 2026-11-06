import XCTest

/// B1 iOS UI 冒烟用例：验证 XCUITest 可以在模拟器里驱动 FoodMapApp。
///
/// 这两个用例**不依赖后端**：登录表单填写完成后，把服务地址指向一个必然拒绝连接的端口，
/// 用来验证「输入 → 点击 → 异步错误提示」整条 UI 链路。依赖真实后端的登录链路见
/// `harness/scenarios/b1-ui-login.yml`。
final class LoginFlowTests: XCTestCase {
    private let unreachableGateway = "http://127.0.0.1:9"

    override func setUp() {
        continueAfterFailure = false
    }

    /// 启动 App 并等待登录页出现；`-uiTestResetSession` 会清掉本地会话，保证从登录页开始。
    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestResetSession"]
        app.launch()
        XCTAssertTrue(
            app.textFields["login.identifier"].waitForExistence(timeout: 30),
            "登录页没有出现，检查 App 是否启动成功或本地是否残留会话"
        )
        return app
    }

    /// 清空输入框后再输入，避免依赖上一次运行的残留文本。
    private func replaceText(in element: XCUIElement, with text: String) {
        element.tap()
        if let current = element.value as? String, !current.isEmpty {
            element.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count))
        }
        element.typeText(text)
    }

    func testLoginFormIsVisible() {
        let app = launchApp()

        XCTAssertTrue(app.textFields["login.identifier"].exists, "缺少账号输入框")
        XCTAssertTrue(app.secureTextFields["login.password"].exists, "缺少密码输入框")
        XCTAssertTrue(app.textFields["login.baseURL"].exists, "缺少服务地址输入框")
        XCTAssertTrue(app.buttons["login.submit"].exists, "缺少登录按钮")
    }

    func testTypingIntoFieldsKeepsValue() {
        let app = launchApp()
        let identifierField = app.textFields["login.identifier"]

        replaceText(in: identifierField, with: "ui_probe_account")

        XCTAssertEqual(identifierField.value as? String, "ui_probe_account")
    }

    func testUnreachableGatewayShowsReadableError() {
        let app = launchApp()

        replaceText(in: app.textFields["login.identifier"], with: "ui_probe_account")
        replaceText(in: app.secureTextFields["login.password"], with: "ui_probe_password")
        replaceText(in: app.textFields["login.baseURL"], with: unreachableGateway)

        app.buttons["login.submit"].tap()

        let errorText = app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS %@", "无法连接后端服务")
        ).firstMatch
        XCTAssertTrue(errorText.waitForExistence(timeout: 30), "不可达网关没有给出可读的错误提示")
    }

    /// 真实链路用例：由 `harness/scenarios/b1-ui-login.yml` 注入账号、密码和网关地址。
    ///
    /// 环境变量通过 `TEST_RUNNER_` 前缀由 xcodebuild 透传给测试进程。
    func testLoginThroughGateway() {
        let environment = ProcessInfo.processInfo.environment
        guard
            let account = environment["UI_ACC"],
            let password = environment["UI_PW"],
            let gateway = environment["UI_GATEWAY"]
        else {
            XCTFail("缺少 UI_ACC / UI_PW / UI_GATEWAY，请通过 harness/scenarios/b1-ui-login.yml 运行")
            return
        }

        let app = launchApp()

        replaceText(in: app.textFields["login.identifier"], with: account)
        replaceText(in: app.secureTextFields["login.password"], with: password)
        replaceText(in: app.textFields["login.baseURL"], with: gateway)

        app.buttons["login.submit"].tap()

        XCTAssertTrue(
            app.otherElements["map.home"].waitForExistence(timeout: 30),
            "登录后没有进入地图页，检查网关是否可达、账号是否注册成功"
        )
    }
}
