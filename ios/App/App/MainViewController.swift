import UIKit
import Capacitor
import GoogleMobileAds
import AppTrackingTransparency
import AdSupport
import WebKit

class MainViewController: CAPBridgeViewController, BannerViewDelegate {

    private var bannerView: BannerView!
    private let bannerAdUnitID = "ca-app-pub-3403038737253823/5667820640"

    override func viewDidLoad() {
        super.viewDidLoad()
        setupJSBridge()
        setupAdMobBanner()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        requestATTAndLoadBanner()
    }

    private func setupJSBridge() {
        guard let webView = self.webView else { return }
        
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
                },
                setNotificationTime: function(hour) {
                    console.log("Notificación programada para la hora: " + hour);
                },
                updateWidgetData: function(edo, mun, nombre, magna, premium) {
                    console.log("Widget data updated", edo, mun, nombre, magna, premium);
                },
                requestIgnoreBatteryOptimization: function() {}
            };
        }
        """
        let userScript = WKUserScript(source: jsBridge, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        webView.configuration.userContentController.addUserScript(userScript)
    }

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
