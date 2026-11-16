import SwiftUI
import UIKit
import FirebaseCore
import UserNotifications
import WidgetKit
import Shared

final class SwiftWidgetPlatformDelegate: NSObject, IosWidgetPlatformDelegate {
    func reloadAllTimelines() {
        WidgetCenter.shared.reloadAllTimelines()
    }

    func checkWidgetInstalled(callback: any IosWidgetCheckCallback) {
        WidgetCenter.shared.getCurrentConfigurations { result in
            let isInstalled: Bool
            switch result {
            case .success(let infos):
                isInstalled = !infos.isEmpty
            case .failure:
                isInstalled = false
            }
            DispatchQueue.main.async {
                callback.onResult(installed: isInstalled)
            }
        }
    }
}

final class SwiftAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        DispatchQueue.main.async {
            application.registerForRemoteNotifications()
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        IosPushSwiftBridge.shared.onPushTokenUpdated(token: token)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        forwardNotification(userInfo: notification.request.content.userInfo, content: notification.request.content, isForeground: true)
        completionHandler([.sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        forwardNotification(userInfo: response.notification.request.content.userInfo, content: response.notification.request.content, isForeground: false)
        if let tab = response.notification.request.content.userInfo["open_tab"] as? String {
            WidgetDeepLinkBus.shared.emit(tabName: tab, action: nil)
        }
        completionHandler()
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        let isForeground = application.applicationState == .active
        forwardNotification(userInfo: userInfo, content: nil, isForeground: isForeground)
        completionHandler(.newData)
    }

    private func forwardNotification(userInfo: [AnyHashable: Any], content: UNNotificationContent?, isForeground: Bool) {
        let aps = userInfo["aps"] as? [AnyHashable: Any]
        let alert = aps?["alert"] as? [AnyHashable: Any]
        let title = (userInfo["title"] as? String) ?? (alert?["title"] as? String) ?? content?.title
        let body = (userInfo["body"] as? String) ?? (alert?["body"] as? String) ?? content?.body
        let typeRaw = (userInfo["activityType"] as? String) ?? (userInfo["type"] as? String)
        let sportType = userInfo["sportType"] as? String
        let actorName = (userInfo["actorName"] as? String) ?? title
        let actorUsername = userInfo["actorUsername"] as? String
        let id = userInfo["notificationId"] as? String
        let actorUserId: Int32 = {
            if let num = userInfo["actorUserId"] as? NSNumber { return num.int32Value }
            if let str = userInfo["actorUserId"] as? String, let parsed = Int32(str) { return parsed }
            return 0
        }()

        // Only forward remote/friend pushes or explicit payload notifications (skip local self-reminders unless friend)
        if title != nil || body != nil || actorName != nil {
            IosPushSwiftBridge.shared.onPushReceived(
                id: id,
                actorName: actorName,
                actorUsername: actorUsername,
                actorUserId: actorUserId,
                typeRaw: typeRaw,
                sportType: sportType,
                title: title,
                body: body,
                isForeground: isForeground
            )
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(SwiftAppDelegate.self) private var appDelegate
    private let widgetDelegate = SwiftWidgetPlatformDelegate()

    init() {
        FirebaseApp.configure()
        IosWidgetSwiftBridge.shared.register(delegate: widgetDelegate)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    IosWidgetSwiftBridge.shared.handleDeepLink(urlString: url.absoluteString)
                }
        }
    }
}
