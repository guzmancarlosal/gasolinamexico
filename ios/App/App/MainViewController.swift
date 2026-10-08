import UIKit
import Capacitor
import GoogleMobileAds
import AppTrackingTransparency
import AdSupport
import WebKit
import UserNotifications

class MainViewController: CAPBridgeViewController, BannerViewDelegate, WKScriptMessageHandler, UNUserNotificationCenterDelegate {

    private var bannerView: BannerView!
    private let bannerAdUnitID = "ca-app-pub-3403038737253823/5667820640"

    override func viewDidLoad() {
        super.viewDidLoad()
        configureAppearance()
        UNUserNotificationCenter.current().delegate = self
        setupJSBridge()
        setupAdMobBanner()
    }

    private func configureAppearance() {
        // Fondo nativo dinámico adaptado al modo de la interfaz (Midnight Slate #080c14 o Soft Gray #f0f2f5)
        view.backgroundColor = UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 8/255.0, green: 12/255.0, blue: 20/255.0, alpha: 1.0)
                : UIColor(red: 240/255.0, green: 242/255.0, blue: 245/255.0, alpha: 1.0)
        }
        guard let webView = self.webView else { return }
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        requestATTAndLoadBanner()
    }

    private func setupJSBridge() {
        guard let webView = self.webView else { return }
        webView.configuration.userContentController.removeScriptMessageHandler(forName: "nativeAppBridge")
        webView.configuration.userContentController.add(self, name: "nativeAppBridge")
        
        let jsBridge = """
        if (!window.app) {
            window.app = {
                openMap: function(address) {
                    var encoded = encodeURIComponent(address);
                    var url = "http://maps.apple.com/?q=" + encoded;
                    window.location.href = url;
                },
                addMyMun: function(mun, edo) {
                    try {
                        localStorage.setItem("gasApp_municipioId", mun);
                        localStorage.setItem("gasApp_estadoId", edo);
                    } catch(e) {}
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.nativeAppBridge) {
                        window.webkit.messageHandlers.nativeAppBridge.postMessage({
                            action: "addMyMun",
                            mun: String(mun),
                            edo: String(edo)
                        });
                    }
                },
                setNotificationTime: function(hour) {
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.nativeAppBridge) {
                        window.webkit.messageHandlers.nativeAppBridge.postMessage({
                            action: "setNotificationTime",
                            hour: Number(hour)
                        });
                    }
                },
                updateWidgetData: function(edo, mun, nombre, magna, premium) {
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.nativeAppBridge) {
                        window.webkit.messageHandlers.nativeAppBridge.postMessage({
                            action: "updateWidgetData",
                            edo: String(edo),
                            mun: String(mun),
                            nombre: String(nombre),
                            magna: String(magna),
                            premium: String(premium)
                        });
                    }
                },
                requestIgnoreBatteryOptimization: function() {}
            };
        }
        """
        let userScript = WKUserScript(source: jsBridge, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        webView.configuration.userContentController.addUserScript(userScript)
    }

    // MARK: - WKScriptMessageHandler
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "nativeAppBridge",
              let body = message.body as? [String: Any],
              let action = body["action"] as? String else { return }
        
        switch action {
        case "setNotificationTime":
            let hour = body["hour"] as? Int ?? -1
            handleSetNotificationTime(hour: hour)
            
        case "updateWidgetData":
            if let mun = body["mun"] as? String { UserDefaults.standard.set(mun, forKey: "gasApp_municipioId") }
            if let edo = body["edo"] as? String { UserDefaults.standard.set(edo, forKey: "gasApp_estadoId") }
            if let nombre = body["nombre"] as? String { UserDefaults.standard.set(nombre, forKey: "gasApp_municipioNombre") }
            if let magna = body["magna"] as? String { UserDefaults.standard.set(magna, forKey: "gasApp_cheapestMagna") }
            if let premium = body["premium"] as? String { UserDefaults.standard.set(premium, forKey: "gasApp_cheapestPremium") }
            let scheduledHour = UserDefaults.standard.object(forKey: "gasApp_notificationHour") as? Int ?? -1
            if scheduledHour != -1 {
                scheduleDailyNotification(hour: scheduledHour)
            }
            
        case "addMyMun":
            if let mun = body["mun"] as? String { UserDefaults.standard.set(mun, forKey: "gasApp_municipioId") }
            if let edo = body["edo"] as? String { UserDefaults.standard.set(edo, forKey: "gasApp_estadoId") }
            
        default:
            break
        }
    }

    // MARK: - Local Notifications Scheduling
    private func handleSetNotificationTime(hour: Int) {
        UserDefaults.standard.set(hour, forKey: "gasApp_notificationHour")
        let center = UNUserNotificationCenter.current()
        
        if hour == -1 {
            center.removePendingNotificationRequests(withIdentifiers: ["daily_gas_notification"])
            print("[GasolinaMexico] Notificaciones diarias canceladas por el usuario.")
            return
        }
        
        center.requestAuthorization(options: [.alert, .sound, .badge]) { [weak self] granted, error in
            DispatchQueue.main.async {
                if granted {
                    print("[GasolinaMexico] Permiso de notificaciones concedido.")
                    self?.scheduleDailyNotification(hour: hour)
                } else {
                    print("[GasolinaMexico] Permiso de notificaciones denegado o error: \(String(describing: error))")
                }
            }
        }
    }

    private func scheduleDailyNotification(hour: Int) {
        guard hour >= 0 && hour <= 23 else { return }
        let center = UNUserNotificationCenter.current()
        let content = UNMutableNotificationContent()
        let munNombre = UserDefaults.standard.string(forKey: "gasApp_municipioNombre") ?? "tu ciudad"
        let magna = UserDefaults.standard.string(forKey: "gasApp_cheapestMagna") ?? ""
        
        content.title = "⛽ Gasolina más barata hoy en \(munNombre)"
        if !magna.isEmpty {
            content.body = "Magna disponible desde \(magna) hoy. Toca para ver las gasolineras recomendadas."
        } else {
            content.body = "Consulta los precios de hoy en \(munNombre) y ahorra en tu tanque."
        }
        content.sound = .default
        content.userInfo = ["fromNotification": true]
        
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = 0
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: "daily_gas_notification", content: content, trigger: trigger)
        
        center.removePendingNotificationRequests(withIdentifiers: ["daily_gas_notification"])
        center.add(request) { error in
            if let error = error {
                print("[GasolinaMexico] Error al programar notificación: \(error.localizedDescription)")
            } else {
                print("[GasolinaMexico] Notificación diaria programada con éxito para las \(hour):00 hrs.")
            }
        }
    }

    // MARK: - UNUserNotificationCenterDelegate
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }

    // MARK: - AdMob
    private func setupAdMobBanner() {
        bannerView = BannerView()
        bannerView.adUnitID = bannerAdUnitID
        bannerView.rootViewController = self
        bannerView.delegate = self
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(bannerView)

        NSLayoutConstraint.activate([
            bannerView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            bannerView.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
    }

    private func requestATTAndLoadBanner() {
        if #available(iOS 14, *) {
            ATTrackingManager.requestTrackingAuthorization { [weak self] _ in
                DispatchQueue.main.async {
                    self?.loadBannerAd()
                }
            }
        } else {
            loadBannerAd()
        }
    }

    private func loadBannerAd() {
        let frame = view.frame.inset(by: view.safeAreaInsets)
        let viewWidth = frame.size.width
        bannerView.adSize = currentOrientationAnchoredAdaptiveBanner(width: viewWidth)
        bannerView.load(Request())
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        bannerView.isHidden = false
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        print("AdMob iOS banner failed to load: \(error.localizedDescription)")
    }
}
