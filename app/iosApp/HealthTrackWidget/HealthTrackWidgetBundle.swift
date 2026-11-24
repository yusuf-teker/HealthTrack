import WidgetKit
import SwiftUI

@main
struct HealthTrackWidgetBundle: WidgetBundle {
    var body: some Widget {
        HealthTrackTelemetryWidget()
        HealthTrackAiFoodWidget()
        HealthTrackWorkoutWidget()
        HealthTrackWeightWidget()
    }
}
