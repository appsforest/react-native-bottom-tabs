import SwiftUI

struct TabItem: View {
  var title: String?
  var icon: PlatformImage?
  var sfSymbol: String?
  var labeled: Bool?
  var tabData: TabInfo?
  var fontFamily: String?
  var fontWeight: String?

  var body: some View {
#if os(macOS)
    if let icon {
      Image(nsImage: icon)
    } else if let sfSymbol, !sfSymbol.isEmpty {
      Image(systemName: sfSymbol).noneSymbolVariant()
    }
#else
    if let tabData, tabData.isAvatar {
      Image(uiImage: UIImage.avatar(
        from: icon,
        initials: tabData.avatarInitials,
        backgroundColor: tabData.avatarBackgroundColor.flatMap(UIColor.fromHex) ?? .systemGray,
        size: tabData.avatarSize,
        strokeColor: tabData.avatarStrokeColor.flatMap(UIColor.fromHex),
        strokeGap: tabData.avatarStrokeGap,
        strokeWidth: tabData.avatarStrokeWidth,
        fontFamily: fontFamily,
        fontWeight: fontWeight
      ))
    } else if let icon {
      Image(uiImage: icon)
    } else if let sfSymbol, !sfSymbol.isEmpty {
      Image(systemName: sfSymbol).noneSymbolVariant()
    }
#endif
    
    if labeled != false {
      Text(title ?? "")
    }
  }
}
