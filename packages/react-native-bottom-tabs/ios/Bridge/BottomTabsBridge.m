#ifdef SWIFT_PACKAGE
#import "BottomTabsBridge.h"
@import BottomTabsSwift;

UIView<RNCTabViewProvider> *RNCCreateTabViewProvider(id<RNCTabViewProviderDelegate> delegate) {
  return (UIView<RNCTabViewProvider> *)[[TabViewProvider alloc] initWithDelegate:(id<TabViewProviderDelegate>)delegate];
}

NSObject *RNCCreateBottomAccessoryProvider(id<RNCBottomAccessoryProviderDelegate> delegate) {
  return [[BottomAccessoryProvider alloc] initWithDelegate:(id<BottomAccessoryProviderDelegate>)delegate];
}

@implementation RNCTabInfo
+ (NSObject *)createWithKey:(NSString *)key
                     title:(NSString *)title
                     badge:(NSString *)badge
                  sfSymbol:(NSString *)sfSymbol
           focusedSfSymbol:(NSString *)focusedSfSymbol
           activeTintColor:(UIColor *)activeTintColor
         iconRenderingMode:(NSString *)iconRenderingMode
                    hidden:(BOOL)hidden
                    testID:(NSString *)testID
                      role:(NSString *)role
           preventsDefault:(BOOL)preventsDefault
              labelVisible:(BOOL)labelVisible
                 avatarUri:(NSString *)avatarUri
            avatarInitials:(NSString *)avatarInitials
     avatarBackgroundColor:(NSString *)avatarBackgroundColor
                avatarSize:(CGFloat)avatarSize
         avatarStrokeColor:(NSString *)avatarStrokeColor
           avatarStrokeGap:(CGFloat)avatarStrokeGap
         avatarStrokeWidth:(CGFloat)avatarStrokeWidth {
  return [[TabInfo alloc] initWithKey:key title:title badge:badge sfSymbol:sfSymbol
                    focusedSfSymbol:focusedSfSymbol activeTintColor:activeTintColor
                  iconRenderingMode:iconRenderingMode hidden:hidden testID:testID
                               role:role preventsDefault:preventsDefault
                       labelVisible:labelVisible avatarUri:avatarUri
                     avatarInitials:avatarInitials avatarBackgroundColor:avatarBackgroundColor
                         avatarSize:avatarSize avatarStrokeColor:avatarStrokeColor
                    avatarStrokeGap:avatarStrokeGap avatarStrokeWidth:avatarStrokeWidth];
}
@end
#endif
