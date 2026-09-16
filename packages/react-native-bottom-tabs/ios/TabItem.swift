import SwiftUI

struct TabItem: View {
  var title: String?
  var icon: PlatformImage?
  var sfSymbol: String?
  var labeled: Bool?
  var iconRenderingMode: String?
  var tabData: TabInfo?
  var fontFamily: String?
  var fontWeight: String?

  var body: some View {
#if !os(macOS)
    if let tabData, tabData.isAvatar {
      Image(uiImage: UIImage.avatar(
        from: icon,
        tabData: tabData,
        fontFamily: fontFamily,
        fontWeight: fontWeight
      ))
    } else if let icon {
      Image(uiImage: renderedIcon(icon))
    } else if let sfSymbol, !sfSymbol.isEmpty {
      Image(systemName: sfSymbol)
        .noneSymbolVariant()
    }
#else
    if let icon {
      Image(nsImage: icon)
    } else if let sfSymbol, !sfSymbol.isEmpty {
      Image(systemName: sfSymbol)
        .noneSymbolVariant()
    }
#endif
    if labeled != false && tabData?.labelVisible != false {
      Text(title ?? "")
    }
  }

#if !os(macOS)
  private var preservesOriginalIconColors: Bool {
    iconRenderingMode == "original"
  }

  private func renderedIcon(_ icon: UIImage) -> UIImage {
    preservesOriginalIconColors ? icon.withRenderingMode(.alwaysOriginal) : icon
  }
#endif
}
