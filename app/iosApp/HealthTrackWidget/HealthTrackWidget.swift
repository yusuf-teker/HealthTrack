import WidgetKit
import SwiftUI
import AppIntents

// MARK: - Shared Telemetry Model & App Group Store

struct WidgetTelemetrySnapshot: Codable {
    var userId: Int = 0
    var dateIso: String = ""
    var isLoggedIn: Bool = false
    var consumedCalories: Int = 0
    var targetCalories: Int = 2000
    var burnedCalories: Int = 0
    var proteinG: Int = 0
    var proteinTargetG: Int = 100
    var carbsG: Int = 0
    var carbsTargetG: Int = 250
    var fatG: Int = 0
    var fatTargetG: Int = 65
    var waterMl: Int = 0
    var waterTargetMl: Int = 2500
    var steps: Int = 0
    var stepTarget: Int = 10000
    var latestWeightKg: Double? = nil
    var targetWeightKg: Double? = nil
    var streakDays: Int = 0
    var isActiveToday: Bool = false
    var themeAccentId: String = "ocean"
    var isDarkTheme: Bool = true
    var languageCode: String = "tr"
    var updatedAtMs: Int64 = 0

    var safeTargetCalories: Int { max(targetCalories, 1) }
    var remainingCalories: Int { max(safeTargetCalories - consumedCalories, 0) }
    var overCalories: Int { max(consumedCalories - safeTargetCalories, 0) }
    var isOverCalories: Bool { consumedCalories > safeTargetCalories }
    var rawCalorieRatio: Double { Double(consumedCalories) / Double(safeTargetCalories) }
    var calorieProgress: Double { min(max(rawCalorieRatio, 0), 1) }
    var waterProgress: Double {
        guard waterTargetMl > 0 else { return 0 }
        return min(max(Double(waterMl) / Double(waterTargetMl), 0), 1)
    }
    var isWaterGoalMet: Bool { waterTargetMl > 0 && waterMl >= waterTargetMl }
    var isTurkish: Bool { languageCode.lowercased().hasPrefix("tr") }

    static var sample: WidgetTelemetrySnapshot {
        WidgetTelemetrySnapshot(
            userId: 1,
            dateIso: "2026-11-13",
            isLoggedIn: true,
            consumedCalories: 1480,
            targetCalories: 2150,
            burnedCalories: 360,
            proteinG: 104,
            proteinTargetG: 135,
            carbsG: 148,
            carbsTargetG: 230,
            fatG: 46,
            fatTargetG: 68,
            waterMl: 1500,
            waterTargetMl: 2500,
            steps: 7420,
            stepTarget: 10000,
            latestWeightKg: 74.2,
            targetWeightKg: 71.0,
            streakDays: 7,
            isActiveToday: true,
            themeAccentId: "ocean",
            isDarkTheme: true,
            languageCode: "tr",
            updatedAtMs: 0
        )
    }
}

enum WidgetSharedStore {
    static let appGroupId = "group.com.yusufteker.healthtrack"
    static let snapshotKey = "ht_widget_snapshot"
    static let pendingWaterDeltaKey = "ht_widget_pending_water_delta_ml"
    static let pendingWaterDateKey = "ht_widget_pending_water_date"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroupId) ?? .standard
    }

    static func loadSnapshot() -> WidgetTelemetrySnapshot {
        let store = defaults
        guard let jsonString = store.string(forKey: snapshotKey),
              let data = jsonString.data(using: .utf8),
              let decoded = try? JSONDecoder().decode(WidgetTelemetrySnapshot.self, from: data) else {
            return .sample
        }
        if !decoded.isLoggedIn {
            return .sample
        }
        return decoded
    }

    static func saveSnapshot(_ snapshot: WidgetTelemetrySnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot),
              let jsonString = String(data: data, encoding: .utf8) else { return }
        let store = defaults
        store.set(jsonString, forKey: snapshotKey)
        store.synchronize()
    }

    static func addWater(amountMl: Int) {
        var snapshot = loadSnapshot()
        snapshot.waterMl = max(snapshot.waterMl + amountMl, 0)
        saveSnapshot(snapshot)

        let store = defaults
        let existingDelta = store.integer(forKey: pendingWaterDeltaKey)
        store.set(existingDelta + amountMl, forKey: pendingWaterDeltaKey)
        if !snapshot.dateIso.isEmpty {
            store.set(snapshot.dateIso, forKey: pendingWaterDateKey)
        }
        store.synchronize()
    }
}

// MARK: - Interactive iOS 17+ AppIntent (+250ml / +500ml Water)

struct AddWaterWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Su Ekle"
    static var description = IntentDescription("Günlük su tüketimine hızlıca su ekler.")

    @Parameter(title: "Miktar (ml)")
    var amountMl: Int

    init() {
        self.amountMl = 250
    }

    init(amountMl: Int) {
        self.amountMl = amountMl
    }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.addWater(amountMl: amountMl)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

// MARK: - Timeline Provider

struct HealthTrackWidgetEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetTelemetrySnapshot
}

struct HealthTrackWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> HealthTrackWidgetEntry {
        HealthTrackWidgetEntry(date: Date(), snapshot: .sample)
    }

    func getSnapshot(in context: Context, completion: @escaping (HealthTrackWidgetEntry) -> Void) {
        let snap = context.isPreview ? WidgetTelemetrySnapshot.sample : WidgetSharedStore.loadSnapshot()
        completion(HealthTrackWidgetEntry(date: Date(), snapshot: snap))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HealthTrackWidgetEntry>) -> Void) {
        let snap = WidgetSharedStore.loadSnapshot()
        let entry = HealthTrackWidgetEntry(date: Date(), snapshot: snap)
        let nextRefresh = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }
}

// MARK: - Design Tokens

private enum TelemetryPalette {
    static let protein = Color(red: 0.753, green: 0.518, blue: 0.988) // #C084FC
    static let carbs = Color(red: 0.984, green: 0.573, blue: 0.235)   // #FB923C
    static let fat = Color(red: 0.992, green: 0.878, blue: 0.278)     // #FDE047
    static let waterCyan = Color(red: 0.220, green: 0.741, blue: 0.973)
    static let waterGreen = Color(red: 0.024, green: 0.839, blue: 0.627)
    static let streakAmber = Color(red: 0.984, green: 0.749, blue: 0.141)

    static func ringColors(for ratio: Double) -> [Color] {
        if ratio > 1.0 {
            return [Color(red: 0.976, green: 0.451, blue: 0.086), Color(red: 0.937, green: 0.267, blue: 0.267)]
        } else if ratio >= 0.75 {
            return [Color(red: 0.008, green: 0.518, blue: 0.780), Color(red: 0.918, green: 0.702, blue: 0.031), Color(red: 0.976, green: 0.451, blue: 0.086)]
        } else if ratio >= 0.50 {
            return [Color(red: 0.008, green: 0.518, blue: 0.780), Color(red: 0.918, green: 0.702, blue: 0.031)]
        } else {
            return [Color(red: 0.008, green: 0.518, blue: 0.780), Color(red: 0.220, green: 0.741, blue: 0.973)]
        }
    }

    static func statusColor(for ratio: Double) -> Color {
        if ratio > 1.0 { return Color(red: 0.937, green: 0.267, blue: 0.267) }
        if ratio >= 0.75 { return Color(red: 0.976, green: 0.451, blue: 0.086) }
        if ratio >= 0.50 { return Color(red: 0.918, green: 0.702, blue: 0.031) }
        return Color(red: 0.220, green: 0.741, blue: 0.973)
    }
}

// MARK: - Subviews

struct CalorieRingView: View {
    let snapshot: WidgetTelemetrySnapshot
    let size: CGFloat
    let lineWidth: CGFloat

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let trackColor = isDark ? Color.white.opacity(0.10) : Color.black.opacity(0.08)
        let gradColors = TelemetryPalette.ringColors(for: snapshot.rawCalorieRatio)
        let activeColor = TelemetryPalette.statusColor(for: snapshot.rawCalorieRatio)

        ZStack {
            Circle()
                .stroke(trackColor, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))

            if snapshot.isOverCalories {
                Circle()
                    .stroke(Color.red.opacity(0.22), style: StrokeStyle(lineWidth: lineWidth + 4, lineCap: .round))
            }

            Circle()
                .trim(from: 0, to: snapshot.calorieProgress)
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: gradColors + [gradColors.first ?? .blue]),
                        center: .center
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            VStack(spacing: 0) {
                Text(snapshot.isOverCalories ? (snapshot.isTurkish ? "AŞILDI" : "OVER") : (snapshot.isTurkish ? "ALINAN" : "INTAKE"))
                    .font(.system(size: size * 0.095, weight: .bold))
                    .foregroundStyle(snapshot.isOverCalories ? Color.red : (isDark ? Color.white.opacity(0.55) : Color.black.opacity(0.5)))

                Text("\(snapshot.consumedCalories)")
                    .font(.system(size: size * 0.22, weight: .black, design: .rounded))
                    .foregroundStyle(snapshot.isOverCalories ? Color.red : (isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16)))
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)

                Text(snapshot.isOverCalories
                     ? "+\(snapshot.overCalories)"
                     : "\(snapshot.remainingCalories) \(snapshot.isTurkish ? "kaldı" : "left")")
                    .font(.system(size: size * 0.10, weight: .bold))
                    .foregroundStyle(activeColor)
                    .lineLimit(1)
            }
            .padding(.horizontal, 6)
        }
        .frame(width: size, height: size)
    }
}

struct MacroProgressRow: View {
    let label: String
    let current: Int
    let target: Int
    let color: Color
    let isDark: Bool

    var body: some View {
        let ratio = target > 0 ? min(max(Double(current) / Double(target), 0), 1) : 0
        HStack(spacing: 6) {
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(isDark ? Color.white.opacity(0.68) : Color.black.opacity(0.6))
                .frame(width: 40, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(isDark ? Color.white.opacity(0.10) : Color.black.opacity(0.08))
                    Capsule()
                        .fill(color)
                        .frame(width: max(geo.size.width * ratio, ratio > 0 ? 4 : 0))
                }
            }
            .frame(height: 5)

            Text("\(current)/\(target)g")
                .font(.system(size: 9.5, weight: .bold, design: .rounded))
                .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                .lineLimit(1)
        }
    }
}

struct BrandHeaderView: View {
    let snapshot: WidgetTelemetrySnapshot
    let compact: Bool

    var body: some View {
        let isDark = snapshot.isDarkTheme
        HStack(spacing: 6) {
            ZStack {
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(LinearGradient(colors: [Color(red: 0.22, green: 0.74, blue: 0.97), Color(red: 0.01, green: 0.52, blue: 0.78)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: compact ? 18 : 20, height: compact ? 18 : 20)
                Text("H")
                    .font(.system(size: compact ? 10 : 11, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
            }

            if !compact {
                Text("HealthTrack")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
            } else {
                Text(snapshot.isOverCalories
                     ? "+\(snapshot.overCalories) kcal"
                     : "\(snapshot.remainingCalories) \(snapshot.isTurkish ? "kaldı" : "left")")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(isDark ? .white.opacity(0.85) : Color.black.opacity(0.75))
                    .lineLimit(1)
            }

            Spacer(minLength: 4)

            if !compact && snapshot.burnedCalories > 0 {
                HStack(spacing: 2) {
                    Image(systemName: "flame.fill")
                        .font(.system(size: 9))
                    Text("\(snapshot.burnedCalories) kcal")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                }
                .foregroundStyle(TelemetryPalette.carbs)
            }

            HStack(spacing: 2) {
                Image(systemName: "bolt.fill")
                    .font(.system(size: 9))
                Text("\(snapshot.streakDays)\(snapshot.isTurkish ? "g" : "d")")
                    .font(.system(size: 10, weight: .bold, design: .rounded))
            }
            .foregroundStyle(TelemetryPalette.streakAmber)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(
                Capsule()
                    .fill(TelemetryPalette.streakAmber.opacity(0.14))
                    .overlay(Capsule().stroke(TelemetryPalette.streakAmber.opacity(0.35), lineWidth: 0.6))
            )
        }
    }
}

// MARK: - Family Views

struct SmallWidgetView: View {
    let snapshot: WidgetTelemetrySnapshot

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let currentL = String(format: "%.1f", Double(snapshot.waterMl) / 1000.0)
        let targetL = String(format: "%.1f", Double(snapshot.waterTargetMl) / 1000.0)

        VStack(spacing: 6) {
            BrandHeaderView(snapshot: snapshot, compact: true)

            Spacer(minLength: 0)

            CalorieRingView(snapshot: snapshot, size: 70, lineWidth: 7.5)

            Spacer(minLength: 0)

            HStack(spacing: 6) {
                HStack(spacing: 2) {
                    Image(systemName: "drop.fill")
                        .font(.system(size: 9))
                    Text("\(currentL)/\(targetL)L")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .lineLimit(1)
                }
                .foregroundStyle(snapshot.isWaterGoalMet ? TelemetryPalette.waterGreen : TelemetryPalette.waterCyan)

                Spacer(minLength: 2)

                Button(intent: AddWaterWidgetIntent(amountMl: 250)) {
                    Text("+250ml")
                        .font(.system(size: 10, weight: .heavy, design: .rounded))
                        .foregroundStyle(TelemetryPalette.waterCyan)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 4)
                        .background(
                            Capsule()
                                .fill(TelemetryPalette.waterCyan.opacity(0.18))
                                .overlay(Capsule().stroke(TelemetryPalette.waterCyan.opacity(0.4), lineWidth: 0.75))
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .widgetURL(URL(string: "healthtrack://open?tab=food"))
    }
}

struct MediumWidgetView: View {
    let snapshot: WidgetTelemetrySnapshot

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let actionBg = isDark ? Color.white.opacity(0.08) : Color.black.opacity(0.05)

        VStack(spacing: 8) {
            BrandHeaderView(snapshot: snapshot, compact: false)

            HStack(spacing: 12) {
                CalorieRingView(snapshot: snapshot, size: 80, lineWidth: 8.5)

                VStack(spacing: 4.5) {
                    MacroProgressRow(
                        label: "Protein",
                        current: snapshot.proteinG,
                        target: snapshot.proteinTargetG,
                        color: TelemetryPalette.protein,
                        isDark: isDark
                    )
                    MacroProgressRow(
                        label: snapshot.isTurkish ? "Karb" : "Carbs",
                        current: snapshot.carbsG,
                        target: snapshot.carbsTargetG,
                        color: TelemetryPalette.carbs,
                        isDark: isDark
                    )
                    MacroProgressRow(
                        label: snapshot.isTurkish ? "Yağ" : "Fat",
                        current: snapshot.fatG,
                        target: snapshot.fatTargetG,
                        color: TelemetryPalette.fat,
                        isDark: isDark
                    )

                    HStack(spacing: 6) {
                        VStack(alignment: .leading, spacing: 2.5) {
                            HStack(spacing: 3) {
                                Image(systemName: "drop.fill")
                                    .font(.system(size: 9))
                                Text("\(snapshot.waterMl)/\(snapshot.waterTargetMl) ml")
                                    .font(.system(size: 10, weight: .bold, design: .rounded))
                                    .lineLimit(1)
                            }
                            .foregroundStyle(snapshot.isWaterGoalMet ? TelemetryPalette.waterGreen : TelemetryPalette.waterCyan)

                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(isDark ? Color.white.opacity(0.10) : Color.black.opacity(0.08))
                                    Capsule()
                                        .fill(LinearGradient(colors: [TelemetryPalette.waterCyan, TelemetryPalette.waterGreen], startPoint: .leading, endPoint: .trailing))
                                        .frame(width: max(geo.size.width * snapshot.waterProgress, snapshot.waterProgress > 0 ? 4 : 0))
                                }
                            }
                            .frame(height: 5)
                        }

                        Button(intent: AddWaterWidgetIntent(amountMl: 250)) {
                            Text("+250ml")
                                .font(.system(size: 10, weight: .heavy, design: .rounded))
                                .foregroundStyle(TelemetryPalette.waterCyan)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .fill(TelemetryPalette.waterCyan.opacity(0.18))
                                        .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(TelemetryPalette.waterCyan.opacity(0.4), lineWidth: 0.75))
                                )
                        }
                        .buttonStyle(.plain)

                        Link(destination: URL(string: "healthtrack://open?tab=food&action=add_food")!) {
                            Text(snapshot.isTurkish ? "+ Öğün" : "+ Meal")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                                .padding(.horizontal, 7)
                                .padding(.vertical, 4)
                                .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(actionBg))
                        }
                    }
                    .padding(.top, 2)
                }
            }
        }
        .padding(13)
        .widgetURL(URL(string: "healthtrack://open?tab=food"))
    }
}

struct LargeWidgetView: View {
    let snapshot: WidgetTelemetrySnapshot

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let cardBg = isDark ? Color.white.opacity(0.06) : Color.black.opacity(0.04)

        VStack(spacing: 12) {
            BrandHeaderView(snapshot: snapshot, compact: false)

            // Telemetry Hero Row
            HStack(spacing: 14) {
                CalorieRingView(snapshot: snapshot, size: 108, lineWidth: 10)

                VStack(alignment: .leading, spacing: 7) {
                    MacroProgressRow(
                        label: "Protein",
                        current: snapshot.proteinG,
                        target: snapshot.proteinTargetG,
                        color: TelemetryPalette.protein,
                        isDark: isDark
                    )
                    MacroProgressRow(
                        label: snapshot.isTurkish ? "Karb" : "Carbs",
                        current: snapshot.carbsG,
                        target: snapshot.carbsTargetG,
                        color: TelemetryPalette.carbs,
                        isDark: isDark
                    )
                    MacroProgressRow(
                        label: snapshot.isTurkish ? "Yağ" : "Fat",
                        current: snapshot.fatG,
                        target: snapshot.fatTargetG,
                        color: TelemetryPalette.fat,
                        isDark: isDark
                    )

                    HStack(spacing: 8) {
                        if let weight = snapshot.latestWeightKg {
                            Label(String(format: "%.1f kg", weight), systemImage: "scalemass.fill")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(TelemetryPalette.protein)
                        }
                        if snapshot.steps > 0 {
                            Label("\(snapshot.steps)", systemImage: "figure.walk")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(TelemetryPalette.waterGreen)
                        }
                    }
                    .padding(.top, 2)
                }
            }
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(cardBg))

            // Hydration Interactive Card
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "drop.fill")
                            .font(.system(size: 11))
                        Text(snapshot.isTurkish ? "Günlük Hidrasyon" : "Daily Hydration")
                            .font(.system(size: 11, weight: .bold))
                        Spacer()
                        Text("\(snapshot.waterMl) / \(snapshot.waterTargetMl) ml")
                            .font(.system(size: 11, weight: .heavy, design: .rounded))
                    }
                    .foregroundStyle(snapshot.isWaterGoalMet ? TelemetryPalette.waterGreen : TelemetryPalette.waterCyan)

                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(isDark ? Color.white.opacity(0.10) : Color.black.opacity(0.08))
                            Capsule()
                                .fill(LinearGradient(colors: [TelemetryPalette.waterCyan, TelemetryPalette.waterGreen], startPoint: .leading, endPoint: .trailing))
                                .frame(width: max(geo.size.width * snapshot.waterProgress, snapshot.waterProgress > 0 ? 5 : 0))
                        }
                    }
                    .frame(height: 6)
                }

                Button(intent: AddWaterWidgetIntent(amountMl: 250)) {
                    Text("+250")
                        .font(.system(size: 11, weight: .heavy, design: .rounded))
                        .foregroundStyle(TelemetryPalette.waterCyan)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 6)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(TelemetryPalette.waterCyan.opacity(0.18)))
                }
                .buttonStyle(.plain)

                Button(intent: AddWaterWidgetIntent(amountMl: 500)) {
                    Text("+500")
                        .font(.system(size: 11, weight: .heavy, design: .rounded))
                        .foregroundStyle(TelemetryPalette.waterGreen)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 6)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(TelemetryPalette.waterGreen.opacity(0.18)))
                }
                .buttonStyle(.plain)
            }
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(cardBg))

            // Quick Action Shortcuts
            HStack(spacing: 8) {
                QuickLinkPill(
                    title: snapshot.isTurkish ? "+ Öğün" : "+ Meal",
                    systemImage: "fork.knife",
                    tint: TelemetryPalette.waterCyan,
                    bg: cardBg,
                    urlString: "healthtrack://open?tab=food&action=add_food"
                )
                QuickLinkPill(
                    title: snapshot.isTurkish ? "Spor" : "Workout",
                    systemImage: "dumbbell.fill",
                    tint: TelemetryPalette.carbs,
                    bg: cardBg,
                    urlString: "healthtrack://open?tab=workout"
                )
                QuickLinkPill(
                    title: snapshot.isTurkish ? "Tartım" : "Weight",
                    systemImage: "scalemass.fill",
                    tint: TelemetryPalette.protein,
                    bg: cardBg,
                    urlString: "healthtrack://open?tab=weight"
                )
            }
        }
        .padding(14)
        .widgetURL(URL(string: "healthtrack://open?tab=food"))
    }
}

struct QuickLinkPill: View {
    let title: String
    let systemImage: String
    let tint: Color
    let bg: Color
    let urlString: String

    var body: some View {
        Link(destination: URL(string: urlString)!) {
            HStack(spacing: 4) {
                Image(systemName: systemImage)
                    .font(.system(size: 11, weight: .bold))
                Text(title)
                    .font(.system(size: 11, weight: .bold))
            }
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(bg))
        }
    }
}

struct AccessoryCircularView: View {
    let snapshot: WidgetTelemetrySnapshot

    var body: some View {
        Gauge(value: snapshot.calorieProgress) {
            Image(systemName: "flame.fill")
        } currentValueLabel: {
            Text("\(snapshot.consumedCalories)")
                .font(.system(size: 11, weight: .bold, design: .rounded))
        }
        .gaugeStyle(.accessoryCircular)
        .widgetURL(URL(string: "healthtrack://open?tab=food"))
    }
}

struct AccessoryRectangularView: View {
    let snapshot: WidgetTelemetrySnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text("HealthTrack")
                    .font(.system(size: 11, weight: .bold))
                Spacer()
                Text("⚡\(snapshot.streakDays)\(snapshot.isTurkish ? "g" : "d")")
                    .font(.system(size: 10, weight: .bold))
            }
            Text("\(snapshot.consumedCalories) / \(snapshot.targetCalories) kcal")
                .font(.system(size: 12, weight: .heavy, design: .rounded))
            Text("💧 \(snapshot.waterMl)/\(snapshot.waterTargetMl) ml")
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .widgetURL(URL(string: "healthtrack://open?tab=food"))
    }
}

// MARK: - Main Widget Entry View & Configuration

struct HealthTrackWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HealthTrackWidgetEntry

    var body: some View {
        switch family {
        case .systemSmall:
            SmallWidgetView(snapshot: entry.snapshot)
        case .systemMedium:
            MediumWidgetView(snapshot: entry.snapshot)
        case .systemLarge:
            LargeWidgetView(snapshot: entry.snapshot)
        case .accessoryCircular:
            AccessoryCircularView(snapshot: entry.snapshot)
        case .accessoryRectangular:
            AccessoryRectangularView(snapshot: entry.snapshot)
        default:
            MediumWidgetView(snapshot: entry.snapshot)
        }
    }
}

struct HealthTrackTelemetryWidget: Widget {
    let kind: String = "HealthTrackTelemetryWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HealthTrackWidgetProvider()) { entry in
            let isDark = entry.snapshot.isDarkTheme
            HealthTrackWidgetEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    if isDark {
                        LinearGradient(
                            colors: [
                                Color(red: 0.043, green: 0.059, blue: 0.098),
                                Color(red: 0.082, green: 0.118, blue: 0.196)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    } else {
                        LinearGradient(
                            colors: [Color.white, Color(red: 0.945, green: 0.965, blue: 0.992)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    }
                }
        }
        .configurationDisplayName("HealthTrack Bio-Telemetri")
        .description("Günlük kalori halkası, makrolar, tek dokunuşla su ekleme ve hızlı öğün/spor kısayolları.")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .systemLarge,
            .accessoryCircular,
            .accessoryRectangular
        ])
        .contentMarginsDisabled()
    }
}

