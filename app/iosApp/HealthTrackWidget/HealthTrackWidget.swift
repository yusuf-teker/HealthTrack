import WidgetKit
import SwiftUI
import AppIntents

// MARK: - Shared Telemetry Model & Pending Queues

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

struct PendingWidgetWorkoutPayload: Codable {
    var dateIso: String
    var rawText: String
    var durationMinutes: Int
    var caloriesBurned: Int
    var createdAtMs: Int64
}

struct PendingWidgetWeightPayload: Codable {
    var dateIso: String
    var weightKg: Double
    var createdAtMs: Int64
}

enum WidgetPage: Int, CaseIterable {
    case summary = 0
    case aiFood = 1
    case workout = 2
    case weight = 3

    func label(isTurkish: Bool) -> String {
        switch self {
        case .summary: return isTurkish ? "Özet" : "Stats"
        case .aiFood: return isTurkish ? "AI Öğün" : "AI Meal"
        case .workout: return isTurkish ? "Spor" : "Sport"
        case .weight: return isTurkish ? "Kilo" : "Weight"
        }
    }

    var systemImage: String {
        switch self {
        case .summary: return "flame.fill"
        case .aiFood: return "sparkles"
        case .workout: return "figure.run"
        case .weight: return "scalemass.fill"
        }
    }

    var deepLinkURL: URL {
        switch self {
        case .summary: return URL(string: "healthtrack://open?tab=food")!
        case .aiFood: return URL(string: "healthtrack://open?tab=food&action=ai_quick_add")!
        case .workout: return URL(string: "healthtrack://open?tab=workout&action=add_workout")!
        case .weight: return URL(string: "healthtrack://open?tab=weight&action=add_weight")!
        }
    }
}

// MARK: - App Group Shared Store

enum WidgetSharedStore {
    static let appGroupId = "group.com.yusufteker.healthtrack"
    static let snapshotKey = "ht_widget_snapshot"
    static let pendingWaterDeltaKey = "ht_widget_pending_water_delta_ml"
    static let pendingWaterDateKey = "ht_widget_pending_water_date"
    static let pendingWorkoutsKey = "ht_widget_pending_workouts_json"
    static let pendingWeightsKey = "ht_widget_pending_weights_json"
    static let activePageKey = "ht_widget_active_page"
    static let draftWeightKey = "ht_widget_draft_weight_kg"
    static let feedbackTextKey = "ht_widget_feedback_text"
    static let feedbackTimestampKey = "ht_widget_feedback_ts"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroupId) ?? .standard
    }

    static func isoDateString(for date: Date = Date()) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    static func loadSnapshot(for date: Date = Date()) -> WidgetTelemetrySnapshot {
        let store = defaults
        guard let jsonString = store.string(forKey: snapshotKey),
              let data = jsonString.data(using: .utf8),
              let decoded = try? JSONDecoder().decode(WidgetTelemetrySnapshot.self, from: data) else {
            return .sample
        }
        if !decoded.isLoggedIn {
            return .sample
        }
        let targetIso = isoDateString(for: date)
        if !decoded.dateIso.isEmpty && decoded.dateIso != targetIso {
            let pendingWater = (store.string(forKey: pendingWaterDateKey) == targetIso)
                ? max(0, store.integer(forKey: pendingWaterDeltaKey))
                : 0
            return WidgetTelemetrySnapshot(
                userId: decoded.userId,
                dateIso: targetIso,
                isLoggedIn: decoded.isLoggedIn,
                consumedCalories: 0,
                targetCalories: decoded.targetCalories,
                burnedCalories: 0,
                proteinG: 0,
                proteinTargetG: decoded.proteinTargetG,
                carbsG: 0,
                carbsTargetG: decoded.carbsTargetG,
                fatG: 0,
                fatTargetG: decoded.fatTargetG,
                waterMl: pendingWater,
                waterTargetMl: decoded.waterTargetMl,
                steps: 0,
                stepTarget: decoded.stepTarget,
                latestWeightKg: decoded.latestWeightKg,
                targetWeightKg: decoded.targetWeightKg,
                streakDays: decoded.streakDays,
                isActiveToday: false,
                themeAccentId: decoded.themeAccentId,
                isDarkTheme: decoded.isDarkTheme,
                languageCode: decoded.languageCode,
                updatedAtMs: decoded.updatedAtMs
            )
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

    static func loadActivePage() -> WidgetPage {
        let raw = defaults.integer(forKey: activePageKey)
        return WidgetPage(rawValue: raw) ?? .summary
    }

    static func setActivePage(_ page: WidgetPage) {
        let store = defaults
        store.set(page.rawValue, forKey: activePageKey)
        store.synchronize()
    }

    static func cyclePage(delta: Int) {
        let count = WidgetPage.allCases.count
        let current = loadActivePage().rawValue
        let next = ((current + delta) % count + count) % count
        setActivePage(WidgetPage(rawValue: next) ?? .summary)
    }

    static func loadDraftWeight(snapshot: WidgetTelemetrySnapshot) -> Double {
        let stored = defaults.double(forKey: draftWeightKey)
        if stored >= 25.0 && stored <= 280.0 {
            return (stored * 10.0).rounded() / 10.0
        }
        let base = snapshot.latestWeightKg ?? 72.0
        return (base * 10.0).rounded() / 10.0
    }

    static func adjustDraftWeight(deltaTenths: Int) {
        let snap = loadSnapshot()
        let current = loadDraftWeight(snapshot: snap)
        let updated = min(max(((current * 10.0).rounded() + Double(deltaTenths)) / 10.0, 25.0), 280.0)
        let store = defaults
        store.set(updated, forKey: draftWeightKey)
        store.synchronize()
    }

    static func setFeedback(_ message: String) {
        let store = defaults
        store.set(message, forKey: feedbackTextKey)
        store.set(Date().timeIntervalSince1970, forKey: feedbackTimestampKey)
        store.synchronize()
    }

    static func loadRecentFeedback(now: Date = Date()) -> String? {
        let store = defaults
        let ts = store.double(forKey: feedbackTimestampKey)
        guard ts > 0, now.timeIntervalSince1970 - ts <= 8.0 else { return nil }
        return store.string(forKey: feedbackTextKey)
    }

    static func addWater(amountMl: Int) {
        let now = Date()
        let todayIso = isoDateString(for: now)
        var snapshot = loadSnapshot(for: now)
        snapshot.dateIso = todayIso
        snapshot.waterMl = max(snapshot.waterMl + amountMl, 0)
        saveSnapshot(snapshot)

        let store = defaults
        let sameDayPending = store.string(forKey: pendingWaterDateKey) == todayIso
        let existingDelta = sameDayPending ? store.integer(forKey: pendingWaterDeltaKey) : 0
        store.set(existingDelta + amountMl, forKey: pendingWaterDeltaKey)
        store.set(todayIso, forKey: pendingWaterDateKey)
        store.synchronize()
    }

    static func logQuickWorkout(titleTr: String, titleEn: String, durationMin: Int, caloriesBurned: Int) {
        let now = Date()
        let todayIso = isoDateString(for: now)
        var snapshot = loadSnapshot(for: now)
        snapshot.dateIso = todayIso
        snapshot.burnedCalories = max(snapshot.burnedCalories + caloriesBurned, 0)
        snapshot.isActiveToday = true
        saveSnapshot(snapshot)

        let rawText = snapshot.isTurkish ? titleTr : titleEn
        let entry = PendingWidgetWorkoutPayload(
            dateIso: todayIso,
            rawText: rawText,
            durationMinutes: durationMin,
            caloriesBurned: caloriesBurned,
            createdAtMs: Int64(now.timeIntervalSince1970 * 1000.0)
        )

        let store = defaults
        var queue: [PendingWidgetWorkoutPayload] = []
        if let existing = store.string(forKey: pendingWorkoutsKey),
           let data = existing.data(using: .utf8),
           let decoded = try? JSONDecoder().decode([PendingWidgetWorkoutPayload].self, from: data) {
            queue = decoded
        }
        queue.append(entry)
        if let encoded = try? JSONEncoder().encode(queue),
           let jsonString = String(data: encoded, encoding: .utf8) {
            store.set(jsonString, forKey: pendingWorkoutsKey)
            store.synchronize()
        }

        let feedback = snapshot.isTurkish
            ? "✓ \(rawText) (-\(caloriesBurned) kcal)"
            : "✓ \(rawText) (-\(caloriesBurned) kcal)"
        setFeedback(feedback)
    }

    static func saveDraftWeight() {
        let now = Date()
        let todayIso = isoDateString(for: now)
        var snapshot = loadSnapshot(for: now)
        let weight = loadDraftWeight(snapshot: snapshot)
        snapshot.dateIso = todayIso
        snapshot.latestWeightKg = weight
        saveSnapshot(snapshot)

        let entry = PendingWidgetWeightPayload(
            dateIso: todayIso,
            weightKg: weight,
            createdAtMs: Int64(now.timeIntervalSince1970 * 1000.0)
        )

        let store = defaults
        var queue: [PendingWidgetWeightPayload] = []
        if let existing = store.string(forKey: pendingWeightsKey),
           let data = existing.data(using: .utf8),
           let decoded = try? JSONDecoder().decode([PendingWidgetWeightPayload].self, from: data) {
            queue = decoded
        }
        queue.append(entry)
        if let encoded = try? JSONEncoder().encode(queue),
           let jsonString = String(data: encoded, encoding: .utf8) {
            store.set(jsonString, forKey: pendingWeightsKey)
            store.synchronize()
        }

        let formatted = String(format: "%.1f", weight)
        let feedback = snapshot.isTurkish ? "✓ \(formatted) kg kaydedildi" : "✓ Saved \(formatted) kg"
        setFeedback(feedback)
    }
}

// MARK: - Interactive iOS 17+ AppIntents

struct AddWaterWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Su Ekle"
    static var description = IntentDescription("Günlük su tüketimine hızlıca su ekler.")

    @Parameter(title: "Miktar (ml)")
    var amountMl: Int

    init() { self.amountMl = 250 }
    init(amountMl: Int) { self.amountMl = amountMl }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.addWater(amountMl: amountMl)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct CycleWidgetPageIntent: AppIntent {
    static var title: LocalizedStringResource = "Widget Sayfası Değiştir"
    static var description = IntentDescription("Widget içinde Özet, AI Öğün, Spor ve Kilo sayfaları arasında geçiş yapar.")

    @Parameter(title: "Yön")
    var delta: Int

    init() { self.delta = 1 }
    init(delta: Int) { self.delta = delta }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.cyclePage(delta: delta)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct SelectWidgetPageIntent: AppIntent {
    static var title: LocalizedStringResource = "Widget Sayfası Seç"
    static var description = IntentDescription("Widget içinde belirli bir sayfayı açar.")

    @Parameter(title: "Sayfa")
    var pageIndex: Int

    init() { self.pageIndex = 0 }
    init(pageIndex: Int) { self.pageIndex = pageIndex }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.setActivePage(WidgetPage(rawValue: pageIndex) ?? .summary)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct LogQuickWorkoutWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Hızlı Spor Kaydet"
    static var description = IntentDescription("Uygulamayı açmadan doğrudan widget'tan hızlı spor ekler.")

    @Parameter(title: "Başlık TR")
    var titleTr: String

    @Parameter(title: "Başlık EN")
    var titleEn: String

    @Parameter(title: "Süre (dk)")
    var durationMin: Int

    @Parameter(title: "Kalori")
    var caloriesBurned: Int

    init() {
        self.titleTr = "30 dk Koşu"
        self.titleEn = "30 min Run"
        self.durationMin = 30
        self.caloriesBurned = 310
    }

    init(titleTr: String, titleEn: String, durationMin: Int, caloriesBurned: Int) {
        self.titleTr = titleTr
        self.titleEn = titleEn
        self.durationMin = durationMin
        self.caloriesBurned = caloriesBurned
    }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.logQuickWorkout(
            titleTr: titleTr,
            titleEn: titleEn,
            durationMin: durationMin,
            caloriesBurned: caloriesBurned
        )
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct AdjustDraftWeightWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Kilo Ayarla"
    static var description = IntentDescription("Widget üzerindeki taslak kiloyu artırır veya azaltır.")

    @Parameter(title: "Değişim (0.1 kg)")
    var deltaTenths: Int

    init() { self.deltaTenths = 1 }
    init(deltaTenths: Int) { self.deltaTenths = deltaTenths }

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.adjustDraftWeight(deltaTenths: deltaTenths)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct SaveDraftWeightWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Kilo Kaydet"
    static var description = IntentDescription("Seçilen kiloyu uygulamayı açmadan doğrudan kaydeder.")

    init() {}

    func perform() async throws -> some IntentResult {
        WidgetSharedStore.saveDraftWeight()
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

// MARK: - Timeline Provider

struct HealthTrackWidgetEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetTelemetrySnapshot
    let activePage: WidgetPage
    let draftWeightKg: Double
    let feedback: String?
}

struct HealthTrackWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> HealthTrackWidgetEntry {
        HealthTrackWidgetEntry(
            date: Date(),
            snapshot: .sample,
            activePage: .summary,
            draftWeightKg: 74.2,
            feedback: nil
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (HealthTrackWidgetEntry) -> Void) {
        let now = Date()
        let snap = context.isPreview ? WidgetTelemetrySnapshot.sample : WidgetSharedStore.loadSnapshot(for: now)
        let page = context.isPreview ? .summary : WidgetSharedStore.loadActivePage()
        let draft = context.isPreview ? (snap.latestWeightKg ?? 74.2) : WidgetSharedStore.loadDraftWeight(snapshot: snap)
        let feedback = context.isPreview ? nil : WidgetSharedStore.loadRecentFeedback(now: now)
        completion(HealthTrackWidgetEntry(date: now, snapshot: snap, activePage: page, draftWeightKg: draft, feedback: feedback))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HealthTrackWidgetEntry>) -> Void) {
        let now = Date()
        let calendar = Calendar.current
        let thirtyMins = calendar.date(byAdding: .minute, value: 30, to: now) ?? now.addingTimeInterval(1800)
        let startOfToday = calendar.startOfDay(for: now)
        let startOfTomorrow = calendar.date(byAdding: .day, value: 1, to: startOfToday) ?? now.addingTimeInterval(86400)
        let nextMidnight = startOfTomorrow.addingTimeInterval(2)

        let snapNow = WidgetSharedStore.loadSnapshot(for: now)
        let page = WidgetSharedStore.loadActivePage()
        let draft = WidgetSharedStore.loadDraftWeight(snapshot: snapNow)
        let feedback = WidgetSharedStore.loadRecentFeedback(now: now)

        var entries = [
            HealthTrackWidgetEntry(date: now, snapshot: snapNow, activePage: page, draftWeightKg: draft, feedback: feedback)
        ]

        // Clear transient feedback pill after 9 seconds automatically
        if feedback != nil {
            let clearDate = now.addingTimeInterval(9)
            entries.append(
                HealthTrackWidgetEntry(date: clearDate, snapshot: snapNow, activePage: page, draftWeightKg: draft, feedback: nil)
            )
        }

        let snapTomorrow = WidgetSharedStore.loadSnapshot(for: nextMidnight)
        entries.append(
            HealthTrackWidgetEntry(date: nextMidnight, snapshot: snapTomorrow, activePage: page, draftWeightKg: draft, feedback: nil)
        )

        completion(Timeline(entries: entries, policy: .after(min(thirtyMins, nextMidnight))))
    }
}

// MARK: - Design Tokens

private enum TelemetryPalette {
    static let primary = Color(red: 0.220, green: 0.741, blue: 0.973)   // #38BDF8
    static let primaryDeep = Color(red: 0.008, green: 0.518, blue: 0.780)
    static let protein = Color(red: 0.753, green: 0.518, blue: 0.988)   // #C084FC
    static let carbs = Color(red: 0.984, green: 0.573, blue: 0.235)     // #FB923C
    static let fat = Color(red: 0.992, green: 0.878, blue: 0.278)       // #FDE047
    static let waterCyan = Color(red: 0.220, green: 0.741, blue: 0.973)
    static let waterGreen = Color(red: 0.024, green: 0.839, blue: 0.627)
    static let streakAmber = Color(red: 0.984, green: 0.749, blue: 0.141)

    static func ringColors(for ratio: Double) -> [Color] {
        if ratio > 1.0 {
            return [Color(red: 0.976, green: 0.451, blue: 0.086), Color(red: 0.937, green: 0.267, blue: 0.267)]
        } else if ratio >= 0.75 {
            return [primaryDeep, Color(red: 0.918, green: 0.702, blue: 0.031), Color(red: 0.976, green: 0.451, blue: 0.086)]
        } else if ratio >= 0.50 {
            return [primaryDeep, Color(red: 0.918, green: 0.702, blue: 0.031)]
        } else {
            return [primaryDeep, waterCyan]
        }
    }

    static func statusColor(for ratio: Double) -> Color {
        if ratio > 1.0 { return Color(red: 0.937, green: 0.267, blue: 0.267) }
        if ratio >= 0.75 { return Color(red: 0.976, green: 0.451, blue: 0.086) }
        if ratio >= 0.50 { return Color(red: 0.918, green: 0.702, blue: 0.031) }
        return waterCyan
    }
}

// MARK: - Shared Subviews

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

struct BrandPagerHeaderView: View {
    let snapshot: WidgetTelemetrySnapshot
    let activePage: WidgetPage
    let showPager: Bool
    let compact: Bool
    let customTitle: String?

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let pillBg = isDark ? Color.white.opacity(0.09) : Color.black.opacity(0.06)

        HStack(spacing: 5) {
            ZStack {
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(LinearGradient(colors: [TelemetryPalette.primary, TelemetryPalette.primaryDeep], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: compact ? 18 : 20, height: compact ? 18 : 20)
                Text("H")
                    .font(.system(size: compact ? 10 : 11, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
            }

            if let customTitle {
                Text(customTitle)
                    .font(.system(size: compact ? 11 : 12, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                    .lineLimit(1)
            } else if !compact {
                Text(activePage == .summary ? "HealthTrack" : activePage.label(isTurkish: snapshot.isTurkish))
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                    .lineLimit(1)
            } else {
                Text(activePage.label(isTurkish: snapshot.isTurkish))
                    .font(.system(size: 10.5, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                    .lineLimit(1)
            }

            Spacer(minLength: 2)

            if showPager {
                if !compact {
                    // 4 direct page tabs
                    HStack(spacing: 3) {
                        ForEach(WidgetPage.allCases, id: \.rawValue) { page in
                            let isSelected = page == activePage
                            Button(intent: SelectWidgetPageIntent(pageIndex: page.rawValue)) {
                                Image(systemName: page.systemImage)
                                    .font(.system(size: 9, weight: .bold))
                                    .foregroundStyle(
                                        isSelected
                                        ? Color.white
                                        : (isDark ? Color.white.opacity(0.55) : Color.black.opacity(0.45))
                                    )
                                    .frame(width: 22, height: 20)
                                    .background(
                                        Capsule()
                                            .fill(isSelected ? TelemetryPalette.primary : pillBg)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                // Prev / Next arrow controls
                HStack(spacing: 3) {
                    Button(intent: CycleWidgetPageIntent(delta: -1)) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 9.5, weight: .heavy))
                            .foregroundStyle(isDark ? .white.opacity(0.85) : .black.opacity(0.75))
                            .frame(width: 20, height: 20)
                            .background(Circle().fill(pillBg))
                    }
                    .buttonStyle(.plain)

                    Button(intent: CycleWidgetPageIntent(delta: 1)) {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 9.5, weight: .heavy))
                            .foregroundStyle(isDark ? .white.opacity(0.85) : .black.opacity(0.75))
                            .frame(width: 20, height: 20)
                            .background(Circle().fill(pillBg))
                    }
                    .buttonStyle(.plain)
                }
            } else {
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
}

// MARK: - Page 2: AI Food Input Page (Medium & Small)

struct AiFoodPageView: View {
    let snapshot: WidgetTelemetrySnapshot
    let compact: Bool

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let isTr = snapshot.isTurkish
        let inputBg = isDark ? Color.white.opacity(0.08) : Color.black.opacity(0.05)
        let borderCol = TelemetryPalette.primary.opacity(isDark ? 0.42 : 0.35)

        VStack(alignment: .leading, spacing: compact ? 6 : 8) {
            // AI Input Field Trigger -> opens app directly with AI food keyboard focused
            Link(destination: URL(string: "healthtrack://open?tab=food&action=ai_quick_add")!) {
                HStack(spacing: 6) {
                    Image(systemName: "sparkles")
                        .font(.system(size: compact ? 11 : 13, weight: .bold))
                        .foregroundStyle(TelemetryPalette.primary)
                    Text(compact
                         ? (isTr ? "Ne yedin? Yaz..." : "What did you eat?")
                         : (isTr ? "Ne yedin? Örn: 2 yumurta, 1 dilim ekmek..." : "What did you eat? e.g. 2 eggs, toast..."))
                        .font(.system(size: compact ? 10 : 11.5, weight: .medium))
                        .foregroundStyle(isDark ? Color.white.opacity(0.65) : Color.black.opacity(0.55))
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, compact ? 8 : 10)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(inputBg)
                        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(borderCol, lineWidth: 1))
                )
            }

            // Primary AI Calculate & Add Button
            Link(destination: URL(string: "healthtrack://open?tab=food&action=ai_quick_add")!) {
                HStack(spacing: 5) {
                    Image(systemName: "wand.and.stars")
                        .font(.system(size: compact ? 10 : 12, weight: .bold))
                    Text(compact
                         ? (isTr ? "AI Hesapla" : "AI Log")
                         : (isTr ? "AI ile Hesapla & Öğüne Ekle" : "AI Calculate & Add Meal"))
                        .font(.system(size: compact ? 10.5 : 12, weight: .heavy))
                        .lineLimit(1)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, compact ? 7 : 9)
                .background(
                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                        .fill(LinearGradient(colors: [TelemetryPalette.primary, TelemetryPalette.primaryDeep], startPoint: .leading, endPoint: .trailing))
                )
            }

            if !compact {
                HStack(spacing: 8) {
                    Text(isTr
                         ? "Bugün: \(snapshot.consumedCalories) / \(snapshot.targetCalories) kcal"
                         : "Today: \(snapshot.consumedCalories) / \(snapshot.targetCalories) kcal")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundStyle(isDark ? Color.white.opacity(0.72) : Color.black.opacity(0.65))
                    Spacer()
                    Link(destination: URL(string: "healthtrack://open?tab=food&action=add_food")!) {
                        Text(isTr ? "+ Manuel Ekle" : "+ Manual")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(TelemetryPalette.waterGreen)
                    }
                }
            }
        }
    }
}

// MARK: - Page 3: Workout Quick-Log Page (In-Widget 1-Tap + AI Input)

struct WorkoutPageView: View {
    let snapshot: WidgetTelemetrySnapshot
    let feedback: String?
    let compact: Bool

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let isTr = snapshot.isTurkish
        let cardBg = isDark ? Color.white.opacity(0.08) : Color.black.opacity(0.05)

        VStack(alignment: .leading, spacing: compact ? 5 : 7) {
            if let feedback, !feedback.isEmpty {
                Text(feedback)
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundStyle(TelemetryPalette.waterGreen)
                    .lineLimit(1)
            } else {
                // AI Workout Input Trigger
                Link(destination: URL(string: "healthtrack://open?tab=workout&action=add_workout")!) {
                    HStack(spacing: 6) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: compact ? 10 : 12, weight: .bold))
                            .foregroundStyle(TelemetryPalette.carbs)
                        Text(compact
                             ? (isTr ? "AI Spor Yaz..." : "AI Workout...")
                             : (isTr ? "Spor yaz (AI hesaplar) • 🔥 \(snapshot.burnedCalories) kcal" : "Describe workout (AI) • 🔥 \(snapshot.burnedCalories) kcal"))
                            .font(.system(size: compact ? 10 : 11, weight: .semibold))
                            .foregroundStyle(isDark ? Color.white.opacity(0.75) : Color.black.opacity(0.65))
                            .lineLimit(1)
                        Spacer(minLength: 0)
                        Image(systemName: "sparkles")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(TelemetryPalette.carbs)
                    }
                    .padding(.horizontal, 9)
                    .padding(.vertical, compact ? 6 : 8)
                    .background(
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .fill(cardBg)
                            .overlay(RoundedRectangle(cornerRadius: 11, style: .continuous).stroke(TelemetryPalette.carbs.opacity(0.4), lineWidth: 0.9))
                    )
                }
            }

            // 1-Tap Direct In-Widget Workout Presets (zero app launch!)
            if compact {
                VStack(spacing: 4) {
                    WorkoutPresetButton(
                        emoji: "🏃",
                        label: isTr ? "30dk Koşu" : "30m Run",
                        kcal: 310,
                        titleTr: "30 dk Koşu",
                        titleEn: "30 min Run",
                        durationMin: 30,
                        isDark: isDark
                    )
                    WorkoutPresetButton(
                        emoji: "🏋️",
                        label: isTr ? "45dk Fitness" : "45m Gym",
                        kcal: 260,
                        titleTr: "45 dk Fitness",
                        titleEn: "45 min Fitness",
                        durationMin: 45,
                        isDark: isDark
                    )
                }
            } else {
                HStack(spacing: 6) {
                    WorkoutPresetButton(
                        emoji: "🏃",
                        label: isTr ? "30dk Koşu" : "30m Run",
                        kcal: 310,
                        titleTr: "30 dk Koşu",
                        titleEn: "30 min Run",
                        durationMin: 30,
                        isDark: isDark
                    )
                    WorkoutPresetButton(
                        emoji: "🏋️",
                        label: isTr ? "45dk Fitness" : "45m Gym",
                        kcal: 260,
                        titleTr: "45 dk Fitness",
                        titleEn: "45 min Fitness",
                        durationMin: 45,
                        isDark: isDark
                    )
                    WorkoutPresetButton(
                        emoji: "🚶",
                        label: isTr ? "30dk Yürüyüş" : "30m Walk",
                        kcal: 150,
                        titleTr: "30 dk Yürüyüş",
                        titleEn: "30 min Walk",
                        durationMin: 30,
                        isDark: isDark
                    )
                }
            }
        }
    }
}

struct WorkoutPresetButton: View {
    let emoji: String
    let label: String
    let kcal: Int
    let titleTr: String
    let titleEn: String
    let durationMin: Int
    let isDark: Bool

    var body: some View {
        Button(intent: LogQuickWorkoutWidgetIntent(
            titleTr: titleTr,
            titleEn: titleEn,
            durationMin: durationMin,
            caloriesBurned: kcal
        )) {
            VStack(spacing: 2) {
                Text("\(emoji) \(label)")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                    .lineLimit(1)
                Text("-\(kcal) kcal")
                    .font(.system(size: 9, weight: .heavy, design: .rounded))
                    .foregroundStyle(TelemetryPalette.carbs)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 7)
            .padding(.horizontal, 4)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(TelemetryPalette.carbs.opacity(isDark ? 0.15 : 0.12))
                    .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(TelemetryPalette.carbs.opacity(0.35), lineWidth: 0.75))
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Page 4: Weight Quick-Log Page (100% In-Widget Stepper + Save)

struct WeightPageView: View {
    let snapshot: WidgetTelemetrySnapshot
    let draftWeightKg: Double
    let feedback: String?
    let compact: Bool

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let isTr = snapshot.isTurkish
        let pillBg = isDark ? Color.white.opacity(0.09) : Color.black.opacity(0.06)

        VStack(spacing: compact ? 6 : 8) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Link(destination: URL(string: "healthtrack://open?tab=weight&action=add_weight")!) {
                    HStack(alignment: .firstTextBaseline, spacing: 3) {
                        Text(String(format: "%.1f", draftWeightKg))
                            .font(.system(size: compact ? 24 : 28, weight: .black, design: .rounded))
                            .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                        Text("kg")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(TelemetryPalette.protein)
                    }
                }

                Spacer(minLength: 2)

                if let feedback, !feedback.isEmpty {
                    Text(feedback)
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundStyle(TelemetryPalette.waterGreen)
                        .lineLimit(1)
                } else if let target = snapshot.targetWeightKg {
                    Text("\(isTr ? "Hedef" : "Goal"): \(String(format: "%.1f", target)) kg")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundStyle(isDark ? Color.white.opacity(0.6) : Color.black.opacity(0.55))
                        .lineLimit(1)
                }
            }

            HStack(spacing: 5) {
                if !compact {
                    WeightStepButton(label: "-0.5", deltaTenths: -5, bg: pillBg, isDark: isDark)
                }
                WeightStepButton(label: "-0.1", deltaTenths: -1, bg: pillBg, isDark: isDark)
                WeightStepButton(label: "+0.1", deltaTenths: 1, bg: pillBg, isDark: isDark)
                if !compact {
                    WeightStepButton(label: "+0.5", deltaTenths: 5, bg: pillBg, isDark: isDark)
                }

                Button(intent: SaveDraftWeightWidgetIntent()) {
                    Text(isTr ? "✓ Kaydet" : "✓ Save")
                        .font(.system(size: 10.5, weight: .heavy))
                        .foregroundStyle(.white)
                        .padding(.horizontal, compact ? 10 : 12)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(LinearGradient(colors: [TelemetryPalette.protein, TelemetryPalette.primary], startPoint: .leading, endPoint: .trailing))
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct WeightStepButton: View {
    let label: String
    let deltaTenths: Int
    let bg: Color
    let isDark: Bool

    var body: some View {
        Button(intent: AdjustDraftWeightWidgetIntent(deltaTenths: deltaTenths)) {
            Text(label)
                .font(.system(size: 10.5, weight: .heavy, design: .rounded))
                .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(bg))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Main Multi-Page Family Views

struct SmallWidgetView: View {
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let snapshot = entry.snapshot
        let isDark = snapshot.isDarkTheme
        let pillBg = isDark ? Color.white.opacity(0.09) : Color.black.opacity(0.06)
        let currentL = String(format: "%.1f", Double(snapshot.waterMl) / 1000.0)
        let targetL = String(format: "%.1f", Double(snapshot.waterTargetMl) / 1000.0)

        VStack(spacing: 6) {
            Spacer(minLength: 0)

            switch entry.activePage {
            case .summary:
                CalorieRingView(snapshot: snapshot, size: 74, lineWidth: 7.5)
                HStack(spacing: 4) {
                    HStack(spacing: 2) {
                        Image(systemName: "drop.fill").font(.system(size: 9))
                        Text("\(currentL)/\(targetL)L")
                            .font(.system(size: 9.5, weight: .bold, design: .rounded))
                            .lineLimit(1)
                    }
                    .foregroundStyle(snapshot.isWaterGoalMet ? TelemetryPalette.waterGreen : TelemetryPalette.waterCyan)
                    Spacer(minLength: 2)
                    Button(intent: AddWaterWidgetIntent(amountMl: 250)) {
                        Text("+250")
                            .font(.system(size: 9.5, weight: .heavy, design: .rounded))
                            .foregroundStyle(TelemetryPalette.waterCyan)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3.5)
                            .background(
                                Capsule()
                                    .fill(TelemetryPalette.waterCyan.opacity(0.18))
                                    .overlay(Capsule().stroke(TelemetryPalette.waterCyan.opacity(0.4), lineWidth: 0.75))
                            )
                    }
                    .buttonStyle(.plain)
                }
            case .aiFood:
                AiFoodPageView(snapshot: snapshot, compact: true)
            case .workout:
                WorkoutPageView(snapshot: snapshot, feedback: entry.feedback, compact: true)
            case .weight:
                WeightPageView(snapshot: snapshot, draftWeightKg: entry.draftWeightKg, feedback: entry.feedback, compact: true)
            }

            Spacer(minLength: 0)

            // Compact bottom switcher for Small widget
            HStack(spacing: 4) {
                Button(intent: CycleWidgetPageIntent(delta: -1)) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 9.5, weight: .heavy))
                        .foregroundStyle(isDark ? .white.opacity(0.85) : .black.opacity(0.75))
                        .frame(width: 24, height: 22)
                        .background(RoundedRectangle(cornerRadius: 7, style: .continuous).fill(pillBg))
                }
                .buttonStyle(.plain)

                Text(entry.activePage.label(isTurkish: snapshot.isTurkish))
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                    .frame(maxWidth: .infinity)
                    .lineLimit(1)

                Button(intent: CycleWidgetPageIntent(delta: 1)) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 9.5, weight: .heavy))
                        .foregroundStyle(isDark ? .white.opacity(0.85) : .black.opacity(0.75))
                        .frame(width: 24, height: 22)
                        .background(RoundedRectangle(cornerRadius: 7, style: .continuous).fill(pillBg))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(11)
        .widgetURL(entry.activePage.deepLinkURL)
    }
}

struct BottomPageTabBar: View {
    let snapshot: WidgetTelemetrySnapshot
    let activePage: WidgetPage

    var body: some View {
        let isDark = snapshot.isDarkTheme
        let inactiveBg = isDark ? Color.white.opacity(0.08) : Color.black.opacity(0.05)

        HStack(spacing: 6) {
            ForEach(WidgetPage.allCases, id: \.rawValue) { page in
                let selected = page == activePage
                let accent: Color = {
                    switch page {
                    case .summary: return isDark ? .white : Color(red: 0.06, green: 0.09, blue: 0.16)
                    case .aiFood: return TelemetryPalette.primary
                    case .workout: return TelemetryPalette.carbs
                    case .weight: return TelemetryPalette.protein
                    }
                }()

                Button(intent: SelectWidgetPageIntent(pageIndex: page.rawValue)) {
                    Text(page.label(isTurkish: snapshot.isTurkish))
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(selected ? Color.white : accent)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8.5)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(selected ? TelemetryPalette.primary : inactiveBg)
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct MediumWidgetView: View {
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let snapshot = entry.snapshot
        let isDark = snapshot.isDarkTheme

        VStack(spacing: 8) {
            Spacer(minLength: 0)

            switch entry.activePage {
            case .summary:
                HStack(spacing: 12) {
                    CalorieRingView(snapshot: snapshot, size: 84, lineWidth: 8.5)

                    VStack(spacing: 4.5) {
                        MacroProgressRow(label: "Protein", current: snapshot.proteinG, target: snapshot.proteinTargetG, color: TelemetryPalette.protein, isDark: isDark)
                        MacroProgressRow(label: snapshot.isTurkish ? "Karb" : "Carbs", current: snapshot.carbsG, target: snapshot.carbsTargetG, color: TelemetryPalette.carbs, isDark: isDark)
                        MacroProgressRow(label: snapshot.isTurkish ? "Yağ" : "Fat", current: snapshot.fatG, target: snapshot.fatTargetG, color: TelemetryPalette.fat, isDark: isDark)

                        HStack(spacing: 6) {
                            VStack(alignment: .leading, spacing: 2.5) {
                                HStack(spacing: 3) {
                                    Image(systemName: "drop.fill").font(.system(size: 9))
                                    Text("\(snapshot.waterMl)/\(snapshot.waterTargetMl) ml")
                                        .font(.system(size: 9.5, weight: .bold, design: .rounded))
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
                                    .font(.system(size: 9.5, weight: .heavy, design: .rounded))
                                    .foregroundStyle(TelemetryPalette.waterCyan)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4.5)
                                    .background(
                                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                                            .fill(TelemetryPalette.waterCyan.opacity(0.18))
                                            .overlay(RoundedRectangle(cornerRadius: 8, style: .continuous).stroke(TelemetryPalette.waterCyan.opacity(0.4), lineWidth: 0.75))
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.top, 2)
                    }
                }
            case .aiFood:
                AiFoodPageView(snapshot: snapshot, compact: false)
            case .workout:
                WorkoutPageView(snapshot: snapshot, feedback: entry.feedback, compact: false)
            case .weight:
                WeightPageView(snapshot: snapshot, draftWeightKg: entry.draftWeightKg, feedback: entry.feedback, compact: false)
            }

            Spacer(minLength: 0)

            BottomPageTabBar(snapshot: snapshot, activePage: entry.activePage)
        }
        .padding(12)
        .widgetURL(entry.activePage.deepLinkURL)
    }
}

struct LargeWidgetView: View {
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let snapshot = entry.snapshot
        let isDark = snapshot.isDarkTheme
        let cardBg = isDark ? Color.white.opacity(0.06) : Color.black.opacity(0.04)

        VStack(spacing: 12) {
            // Top Active Page Interactive Card
            Group {
                switch entry.activePage {
                case .summary:
                    HStack(spacing: 14) {
                        CalorieRingView(snapshot: snapshot, size: 108, lineWidth: 10)

                        VStack(alignment: .leading, spacing: 7) {
                            MacroProgressRow(label: "Protein", current: snapshot.proteinG, target: snapshot.proteinTargetG, color: TelemetryPalette.protein, isDark: isDark)
                            MacroProgressRow(label: snapshot.isTurkish ? "Karb" : "Carbs", current: snapshot.carbsG, target: snapshot.carbsTargetG, color: TelemetryPalette.carbs, isDark: isDark)
                            MacroProgressRow(label: snapshot.isTurkish ? "Yağ" : "Fat", current: snapshot.fatG, target: snapshot.fatTargetG, color: TelemetryPalette.fat, isDark: isDark)

                            HStack(spacing: 8) {
                                if let weight = snapshot.latestWeightKg {
                                    Label(String(format: "%.1f kg", weight), systemImage: "scalemass.fill")
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundStyle(TelemetryPalette.protein)
                                }
                                if snapshot.burnedCalories > 0 {
                                    Label("\(snapshot.burnedCalories) kcal", systemImage: "flame.fill")
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundStyle(TelemetryPalette.carbs)
                                }
                            }
                            .padding(.top, 2)
                        }
                    }
                case .aiFood:
                    AiFoodPageView(snapshot: snapshot, compact: false)
                case .workout:
                    WorkoutPageView(snapshot: snapshot, feedback: entry.feedback, compact: false)
                case .weight:
                    WeightPageView(snapshot: snapshot, draftWeightKg: entry.draftWeightKg, feedback: entry.feedback, compact: false)
                }
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(cardBg))

            // Hydration Interactive Card
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "drop.fill").font(.system(size: 11))
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

            Spacer(minLength: 0)

            BottomPageTabBar(snapshot: snapshot, activePage: entry.activePage)
        }
        .padding(14)
        .widgetURL(entry.activePage.deepLinkURL)
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
                Image(systemName: systemImage).font(.system(size: 11, weight: .bold))
                Text(title).font(.system(size: 11, weight: .bold))
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
                Text("HealthTrack").font(.system(size: 11, weight: .bold))
                Spacer()
                Text("⚡\(snapshot.streakDays)\(snapshot.isTurkish ? "g" : "d")").font(.system(size: 10, weight: .bold))
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

// MARK: - Shared Background Helper

private struct WidgetBackdrop: View {
    let isDark: Bool

    var body: some View {
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

// MARK: - 1. Main 4-Page Telemetry Widget

struct HealthTrackWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HealthTrackWidgetEntry

    var body: some View {
        switch family {
        case .systemSmall:
            SmallWidgetView(entry: entry)
        case .systemMedium:
            MediumWidgetView(entry: entry)
        case .systemLarge:
            LargeWidgetView(entry: entry)
        case .accessoryCircular:
            AccessoryCircularView(snapshot: entry.snapshot)
        case .accessoryRectangular:
            AccessoryRectangularView(snapshot: entry.snapshot)
        default:
            MediumWidgetView(entry: entry)
        }
    }
}

struct HealthTrackTelemetryWidget: Widget {
    let kind: String = "HealthTrackTelemetryWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HealthTrackWidgetProvider()) { entry in
            HealthTrackWidgetEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    WidgetBackdrop(isDark: entry.snapshot.isDarkTheme)
                }
        }
        .configurationDisplayName("HealthTrack 4'lü Bio-Telemetri")
        .description("Özet, AI Öğün Ekleme, Hızlı Spor ve Kilo girişi tek widget içinde 4 sayfada.")
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

// MARK: - 2. Standalone AI Food Widget (Smart Stack Ready)

struct HealthTrackAiFoodStandaloneView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let compact = family == .systemSmall
        VStack(spacing: compact ? 6 : 10) {
            BrandPagerHeaderView(
                snapshot: entry.snapshot,
                activePage: .aiFood,
                showPager: false,
                compact: compact,
                customTitle: entry.snapshot.isTurkish ? "AI Öğün Ekle" : "AI Meal Log"
            )
            Spacer(minLength: 0)
            AiFoodPageView(snapshot: entry.snapshot, compact: compact)
            Spacer(minLength: 0)
        }
        .padding(compact ? 11 : 13)
        .widgetURL(URL(string: "healthtrack://open?tab=food&action=ai_quick_add"))
    }
}

struct HealthTrackAiFoodWidget: Widget {
    let kind: String = "HealthTrackAiFoodWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HealthTrackWidgetProvider()) { entry in
            HealthTrackAiFoodStandaloneView(entry: entry)
                .containerBackground(for: .widget) {
                    WidgetBackdrop(isDark: entry.snapshot.isDarkTheme)
                }
        }
        .configurationDisplayName("HealthTrack • AI Öğün")
        .description("Smart Stack için doğrudan AI öğün girişi ve hesaplama widget'ı.")
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}

// MARK: - 3. Standalone Workout Widget (Smart Stack Ready)

struct HealthTrackWorkoutStandaloneView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let compact = family == .systemSmall
        VStack(spacing: compact ? 6 : 9) {
            BrandPagerHeaderView(
                snapshot: entry.snapshot,
                activePage: .workout,
                showPager: false,
                compact: compact,
                customTitle: entry.snapshot.isTurkish ? "Hızlı Spor" : "Quick Workout"
            )
            Spacer(minLength: 0)
            WorkoutPageView(snapshot: entry.snapshot, feedback: entry.feedback, compact: compact)
            Spacer(minLength: 0)
        }
        .padding(compact ? 11 : 13)
        .widgetURL(URL(string: "healthtrack://open?tab=workout&action=add_workout"))
    }
}

struct HealthTrackWorkoutWidget: Widget {
    let kind: String = "HealthTrackWorkoutWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HealthTrackWidgetProvider()) { entry in
            HealthTrackWorkoutStandaloneView(entry: entry)
                .containerBackground(for: .widget) {
                    WidgetBackdrop(isDark: entry.snapshot.isDarkTheme)
                }
        }
        .configurationDisplayName("HealthTrack • Hızlı Spor")
        .description("Smart Stack için tek dokunuşla spor kaydetme ve AI antrenman girişi.")
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}

// MARK: - 4. Standalone Weight Widget (Smart Stack Ready)

struct HealthTrackWeightStandaloneView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HealthTrackWidgetEntry

    var body: some View {
        let compact = family == .systemSmall
        VStack(spacing: compact ? 6 : 10) {
            BrandPagerHeaderView(
                snapshot: entry.snapshot,
                activePage: .weight,
                showPager: false,
                compact: compact,
                customTitle: entry.snapshot.isTurkish ? "Hızlı Kilo" : "Quick Weight"
            )
            Spacer(minLength: 0)
            WeightPageView(
                snapshot: entry.snapshot,
                draftWeightKg: entry.draftWeightKg,
                feedback: entry.feedback,
                compact: compact
            )
            Spacer(minLength: 0)
        }
        .padding(compact ? 11 : 13)
        .widgetURL(URL(string: "healthtrack://open?tab=weight&action=add_weight"))
    }
}

struct HealthTrackWeightWidget: Widget {
    let kind: String = "HealthTrackWeightWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: HealthTrackWidgetProvider()) { entry in
            HealthTrackWeightStandaloneView(entry: entry)
                .containerBackground(for: .widget) {
                    WidgetBackdrop(isDark: entry.snapshot.isDarkTheme)
                }
        }
        .configurationDisplayName("HealthTrack • Hızlı Kilo")
        .description("Smart Stack için uygulamayı açmadan doğrudan widget içinden tartım kaydetme.")
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}
