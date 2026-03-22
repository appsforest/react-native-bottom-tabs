import React
import UIKit

// MARK: - Public API

extension UIImage {
  static func avatar(
    from image: UIImage?,
    initials: String?,
    backgroundColor: UIColor,
    size: CGFloat,
    strokeColor: UIColor?,
    strokeGap: CGFloat,
    strokeWidth: CGFloat,
    fontFamily: String? = nil,
    fontWeight: String? = nil
  ) -> UIImage {
    let cgSize = CGSize(width: size, height: size)

    if let image {
      return image.circularCropped(size: cgSize, strokeColor: strokeColor, strokeGap: strokeGap, strokeWidth: strokeWidth)
    }

    return UIImage.initialsImage(
      initials: initials ?? "",
      backgroundColor: backgroundColor,
      size: cgSize,
      strokeColor: strokeColor,
      strokeGap: strokeGap,
      strokeWidth: strokeWidth,
      fontFamily: fontFamily,
      fontWeight: fontWeight
    )
  }

  func circularCropped(size: CGSize, strokeColor: UIColor?, strokeGap: CGFloat, strokeWidth: CGFloat) -> UIImage {
    UIImage.renderCircular(size: size, strokeColor: strokeColor, strokeGap: strokeGap, strokeWidth: strokeWidth) { ctx, avatarRect in
      UIBezierPath(ovalIn: avatarRect).addClip()

      let scale = max(size.width / self.size.width, size.height / self.size.height)
      let scaledSize = CGSize(width: self.size.width * scale, height: self.size.height * scale)
      
      let drawRect = CGRect(
        x: avatarRect.minX + (size.width - scaledSize.width) / 2,
        y: avatarRect.minY + (size.height - scaledSize.height) / 2,
        width: scaledSize.width,
        height: scaledSize.height
      )
      
      self.draw(in: drawRect)
      ctx.cgContext.resetClip()
    }
  }

  static func initialsImage(
    initials: String,
    backgroundColor: UIColor,
    size: CGSize,
    strokeColor: UIColor?,
    strokeGap: CGFloat,
    strokeWidth: CGFloat,
    fontFamily: String? = nil,
    fontWeight: String? = nil
  ) -> UIImage {
    UIImage.renderCircular(size: size, strokeColor: strokeColor, strokeGap: strokeGap, strokeWidth: strokeWidth) { _, avatarRect in
      backgroundColor.setFill()
      UIBezierPath(ovalIn: avatarRect).fill()

      let font: UIFont = fontFamily != nil || fontWeight != nil
        ? RCTFont.update(nil, withFamily: fontFamily, size: NSNumber(value: Double(size.width * 0.5)), weight: fontWeight, style: nil, variant: nil, scaleMultiplier: 1.0)
        : UIFont.systemFont(ofSize: size.width * 0.5, weight: .semibold)

      let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: UIColor.white]
      let str = NSAttributedString(string: initials, attributes: attrs)
      let strSize = str.size()
      
      let strRect = CGRect(
        x: avatarRect.minX + (size.width - strSize.width) / 2,
        y: avatarRect.minY + (size.height - strSize.height) / 2,
        width: strSize.width,
        height: strSize.height
      )
      
      str.draw(in: strRect)
    }
  }
}

// MARK: - Shared rendering

private extension UIImage {
  static func renderCircular(
    size: CGSize,
    strokeColor: UIColor?,
    strokeGap: CGFloat,
    strokeWidth: CGFloat,
    draw: (UIGraphicsImageRendererContext, CGRect) -> Void
  ) -> UIImage {
    let padding = strokeColor != nil ? strokeGap + strokeWidth : 0
    let totalSize = CGSize(width: size.width + padding * 2, height: size.height + padding * 2)
    let avatarRect = CGRect(x: padding, y: padding, width: size.width, height: size.height)

    let result = UIGraphicsImageRenderer(size: totalSize).image { ctx in
      draw(ctx, avatarRect)

      if let strokeColor {
        let borderRect = avatarRect.insetBy(dx: -(strokeGap + strokeWidth / 2), dy: -(strokeGap + strokeWidth / 2))
        let path = UIBezierPath(ovalIn: borderRect)
        
        path.lineWidth = strokeWidth
        strokeColor.setStroke()
        path.stroke()
      }
    }

    return result.withRenderingMode(.alwaysOriginal)
  }
}

// MARK: - Color

extension UIColor {
  static func fromHex(_ hex: String) -> UIColor? {
    let hex = hex.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: "#", with: "")
    
    guard hex.count == 6, let rgb = UInt64(hex, radix: 16) else { return nil }
    
    return UIColor(
      red:   CGFloat((rgb >> 16) & 0xFF) / 255,
      green: CGFloat((rgb >> 8)  & 0xFF) / 255,
      blue:  CGFloat( rgb        & 0xFF) / 255,
      alpha: 1.0
    )
  }
}
