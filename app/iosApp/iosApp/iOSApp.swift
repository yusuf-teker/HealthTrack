import SwiftUI
import FirebaseCore
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

@main
struct iOSApp: App {
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
