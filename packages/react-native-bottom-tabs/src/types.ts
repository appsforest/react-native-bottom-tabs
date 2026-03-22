import type { ImageSourcePropType, StyleProp, ViewStyle } from 'react-native';
import type { SFSymbol } from 'sf-symbols-typescript';

export type IconRenderingMode =
  | 'automatic'
  | 'alwaysOriginal'
  | 'alwaysTemplate';

export type IconSource = string | ImageSourcePropType;

export type AppleIcon = { sfSymbol: SFSymbol };

/**
 * Avatar icon configuration for displaying a user avatar as a tab icon.
 * When `uri` is provided, the image will be displayed as a circular cropped avatar.
 * When `uri` is empty or fails to load, `initials` will be displayed instead.
 */
export type AvatarIcon = {
  avatar: {
    /**
     * Remote URI for the avatar image.
     */
    uri?: string;
    /**
     * Fallback initials to display when `uri` is not provided or fails to load.
     * Maximum 2 characters.
     */
    initials?: string;
    /**
     * Background color for the initials circle. Accepts hex color string.
     * Defaults to the tab's active tint color.
     */
    backgroundColor?: string;
    /**
     * Width and height of the avatar circle in points.
     * Defaults to 26.
     */
    size?: number;
    /**
     * Stroke color for the border ring around the avatar. Accepts hex color string.
     * When undefined, no border is drawn.
     */
    strokeColor?: string;
    /**
     * Gap between the avatar circle and the stroke border in points.
     * Defaults to 1.
     */
    strokeGap?: number;
    /**
     * Width of the stroke border in points.
     * Defaults to 1.
     */
    strokeWidth?: number;
  };
};

export type TabRole = 'search';

export type LayoutDirection = 'ltr' | 'rtl' | 'locale';

export type BaseRoute = {
  key: string;
  title?: string;
  badge?: string;
  badgeBackgroundColor?: string;
  badgeTextColor?: string;
  lazy?: boolean;
  focusedIcon?: ImageSourcePropType | AppleIcon;
  unfocusedIcon?: ImageSourcePropType | AppleIcon;
  activeTintColor?: string;
  hidden?: boolean;
  testID?: string;
  role?: TabRole;
  freezeOnBlur?: boolean;
  style?: StyleProp<ViewStyle>;
  preventsDefault?: boolean;
  iconRenderingMode?: IconRenderingMode;
};

export type NavigationState<Route extends BaseRoute> = {
  index: number;
  routes: Route[];
};
