package com.rcttabview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.transition.TransitionManager
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEachIndexed
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.svg.SvgDecoder
import coil3.size.Precision
import coil3.size.Size as CoilSize
import coil3.size.Scale
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.assets.ReactFontManager
import com.facebook.react.modules.core.ReactChoreographer
import com.facebook.react.views.text.ReactTypefaceUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_AUTO
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_UNLABELED
import com.google.android.material.transition.platform.MaterialFadeThrough

class ExtendedBottomNavigationView(context: Context) : BottomNavigationView(context) {
  override fun getMaxItemCount(): Int {
    return 100
  }
}

class ReactBottomNavigationView(context: Context) : LinearLayout(context) {
  private var bottomNavigation = ExtendedBottomNavigationView(context)
  val layoutHolder = FrameLayout(context)

  var onTabSelectedListener: ((key: String) -> Unit)? = null
  var onTabLongPressedListener: ((key: String) -> Unit)? = null
  var onNativeLayoutListener: ((width: Double, height: Double) -> Unit)? = null
  var onTabBarMeasuredListener: ((height: Int) -> Unit)? = null
  var disablePageAnimations = false
  var items: MutableList<TabInfo> = mutableListOf()
  private val iconSources: MutableMap<Int, ImageSource> = mutableMapOf()
  private val drawableCache: MutableMap<ImageSource, Drawable> = mutableMapOf()

  private var isLayoutEnqueued = false
  private var selectedItem: String? = null
  private var activeTintColor: Int? = null
  private var inactiveTintColor: Int? = null
  private val checkedStateSet = intArrayOf(android.R.attr.state_checked)
  private val uncheckedStateSet = intArrayOf(-android.R.attr.state_checked)
  private var hapticFeedbackEnabled = false
  private var fontSize: Int? = null
  private var fontFamily: String? = null
  private var fontWeight: Int? = null
  private var labeled: Boolean? = null
  private var lastReportedSize: Size? = null
  private var hasCustomAppearance = false
  private var uiModeConfiguration: Int = Configuration.UI_MODE_NIGHT_UNDEFINED

  private val imageLoader = ImageLoader.Builder(context)
    .components {
      add(SvgDecoder.Factory())
    }
    .build()

  init {
    orientation = VERTICAL

    addView(
      layoutHolder, LayoutParams(
        LayoutParams.MATCH_PARENT,
        0,
      ).apply { weight = 1f }
    )
    layoutHolder.isSaveEnabled = false

    addView(
      bottomNavigation, LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT
      )
    )
    uiModeConfiguration = resources.configuration.uiMode

    post {
      addOnLayoutChangeListener { _, left, top, right, bottom,
                                  _, _, _, _ ->
        val newWidth = right - left
        val newHeight = bottom - top

        // Notify about tab bar height.
        onTabBarMeasuredListener?.invoke(
          Utils.convertPixelsToDp(context, bottomNavigation.height).toInt()
        )

        if (newWidth != lastReportedSize?.width || newHeight != lastReportedSize?.height) {
          val dpWidth = Utils.convertPixelsToDp(context, layoutHolder.width)
          val dpHeight = Utils.convertPixelsToDp(context, layoutHolder.height)

          onNativeLayoutListener?.invoke(dpWidth, dpHeight)
          lastReportedSize = Size(newWidth, newHeight)
        }
      }
    }
  }

  private val layoutCallback = Choreographer.FrameCallback {
    isLayoutEnqueued = false
    refreshLayout()
  }

  private fun refreshLayout() {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
    )
    layout(left, top, right, bottom)
  }

  fun applyDirection(dir: Int) {
    bottomNavigation.layoutDirection = dir
  }

  override fun requestLayout() {
    super.requestLayout()
    @Suppress("SENSELESS_COMPARISON") // layoutCallback can be null here since this method can be called in init

    if (!isLayoutEnqueued && layoutCallback != null) {
      isLayoutEnqueued = true
      // we use NATIVE_ANIMATED_MODULE choreographer queue because it allows us to catch the current
      // looper loop instead of enqueueing the update in the next loop causing a one frame delay.
      ReactChoreographer
        .getInstance()
        .postFrameCallback(
          ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE,
          layoutCallback,
        )
    }
  }

  fun setSelectedItem(value: String) {
    selectedItem = value
    setSelectedIndex(items.indexOfFirst { it.key == value })
  }

  override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
    if (child === layoutHolder || child === bottomNavigation) {
      super.addView(child, index, params)
      return
    }

    val container = createContainer()
    container.addView(child, params)
    layoutHolder.addView(container, index)

    val itemKey = items[index].key
    if (selectedItem == itemKey) {
      setSelectedIndex(index)
      refreshLayout()
    }
  }

  private fun createContainer(): FrameLayout {
    val container = FrameLayout(context).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      isSaveEnabled = false
      visibility = GONE
      isEnabled = false
    }
    return container
  }

  private fun setSelectedIndex(itemId: Int) {
    bottomNavigation.selectedItemId = itemId
    if (!disablePageAnimations) {
      val fadeThrough = MaterialFadeThrough()
      TransitionManager.beginDelayedTransition(layoutHolder, fadeThrough)
    }

    layoutHolder.forEachIndexed { index, view ->
      if (itemId == index) {
        toggleViewVisibility(view, true)
      } else {
        toggleViewVisibility(view, false)
      }
    }

    layoutHolder.requestLayout()
    layoutHolder.invalidate()
  }

  private fun toggleViewVisibility(view: View, isVisible: Boolean) {
    check(view is ViewGroup) { "Native component tree is corrupted." }

    view.visibility = if (isVisible) VISIBLE else GONE
    view.isEnabled = isVisible
  }

  private fun onTabSelected(item: MenuItem) {
    val selectedItem = items[item.itemId]
    selectedItem.let {
      onTabSelectedListener?.invoke(selectedItem.key)
      emitHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
  }

  private fun onTabLongPressed(item: MenuItem) {
    val longPressedItem = items[item.itemId]
    longPressedItem.let {
      onTabLongPressedListener?.invoke(longPressedItem.key)
      emitHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
  }

  fun setTabBarHidden(isHidden: Boolean) {
    if (isHidden) {
      bottomNavigation.visibility = GONE
    } else {
      bottomNavigation.visibility = VISIBLE
    }
  }

  fun updateItems(items: MutableList<TabInfo>) {
    // If an item got removed, let's re-add all items
    if (items.size < this.items.size) {
      bottomNavigation.menu.clear()
    }

    this.items = items

    items.forEachIndexed { index, item ->
      val menuItem = getOrCreateItem(index, item.title)

      menuItem.title = if (item.labelVisible) item.title else ""
      menuItem.isVisible = !item.hidden

      // Handle avatar initials fallback when no icon source
      if (!iconSources.containsKey(index) && item.avatarInitials != null) {
        val sizePx = bottomNavigation.itemIconSize

        menuItem.icon = createAvatarDrawable(item, sizePx)
      } else if (iconSources.containsKey(index)) {
        getDrawable(iconSources[index]!!, index, 0.0, 0.0) {
          menuItem.icon = it
        }
      }

      if (item.badge?.isNotEmpty() == true) {
        val badge = bottomNavigation.getOrCreateBadge(index)
        badge.isVisible = true
        // Set the badge text only if it's different than an empty space to show a small badge.
        // More context: https://github.com/callstackincubator/react-native-bottom-tabs/issues/422
        if (item.badge != " ") {
          badge.text = item.badge
        }
        // Apply badge colors if provided (Material will use its default theme colors otherwise)
        item.badgeBackgroundColor?.let { badge.backgroundColor = it }
        item.badgeTextColor?.let { badge.badgeTextColor = it }
      } else {
        bottomNavigation.removeBadge(index)
      }
      post {
        val itemView = bottomNavigation.findViewById<View>(menuItem.itemId)
        itemView?.let { view ->
          view.setOnLongClickListener {
            onTabLongPressed(menuItem)
            true
          }
          view.setOnClickListener {
            onTabSelected(menuItem)
          }

          item.testID?.let { testId ->
            view.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_content_container)
              ?.apply {
                tag = testId
              }
          }
        }
      }
    }

    // Update tint colors and text appearance after updating all items.
    post {
      updateTextAppearance()
      updateTintColors()

      val menuView = bottomNavigation.getChildAt(0) as? ViewGroup ?: return@post

      for (i in 0 until menuView.childCount) {
        val item = items.getOrNull(i) ?: continue

        // skip, let default behavior apply
        if (item.labelVisible) {
          continue
        }

        val itemView = menuView.getChildAt(i) ?: continue
        val iconView = itemView.findViewById<android.widget.ImageView>(
          com.google.android.material.R.id.navigation_bar_item_icon_view
        )

        iconView?.layoutParams = (iconView?.layoutParams as? ViewGroup.LayoutParams)?.apply {
          height = ViewGroup.LayoutParams.MATCH_PARENT
          width = ViewGroup.LayoutParams.MATCH_PARENT
        }

        iconView?.requestLayout()
      }
    }
  }

  private fun getOrCreateItem(index: Int, title: String): MenuItem {
    return bottomNavigation.menu.findItem(index) ?: bottomNavigation.menu.add(0, index, 0, title)
  }

  fun setIcons(icons: ReadableArray?) {
    if (icons == null || icons.size() == 0) {
      return
    }

    for (idx in 0 until icons.size()) {
      val source = icons.getMap(idx)
      val uri = source?.getString("uri")

      if (uri.isNullOrEmpty()) {
        val item = items.getOrNull(idx)

        if (item?.avatarInitials != null) {
          val sizePx = bottomNavigation.itemIconSize

          bottomNavigation.menu.findItem(idx)?.icon = createAvatarDrawable(item, sizePx)
        }

        continue
      }

      // Read explicit width/height if provided (in dp), fallback to 0 meaning "use default"
      val widthDp = if (source.hasKey("width")) source.getDouble("width") else 0.0
      val heightDp = if (source.hasKey("height")) source.getDouble("height") else 0.0
      val imageSource = ImageSource(context, uri)

      this.iconSources[idx] = imageSource

      bottomNavigation.menu.findItem(idx)?.let { menuItem ->
        getDrawable(imageSource, idx, widthDp, heightDp) {
          menuItem.icon = it
        }
      }
    }
  }

  fun setLabeled(labeled: Boolean?) {
    this.labeled = labeled
    bottomNavigation.labelVisibilityMode = when (labeled) {
      false -> {
        LABEL_VISIBILITY_UNLABELED
      }
      true -> {
        LABEL_VISIBILITY_LABELED
      }
      else -> {
        LABEL_VISIBILITY_AUTO
      }
    }
  }

  fun setRippleColor(color: ColorStateList) {
    bottomNavigation.itemRippleColor = color
  }

  @SuppressLint("CheckResult")
  private fun getDrawable(
    imageSource: ImageSource,
    index: Int,
    widthDp: Double = 0.0,
    heightDp: Double = 0.0,
    onDrawableReady: (Drawable?) -> Unit
  ) {
    drawableCache[imageSource]?.let {
      onDrawableReady(applyRenderingMode(it, index))
      return
    }

    val defaultSizePx = bottomNavigation.itemIconSize

    val widthPx = if (widthDp > 0) TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, widthDp.toFloat(), context.resources.displayMetrics
    ).toInt() else defaultSizePx

    val heightPx = if (heightDp > 0) TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), context.resources.displayMetrics
    ).toInt() else defaultSizePx

    val request = ImageRequest.Builder(context)
      .data(imageSource.getUri(context))
      .size(CoilSize(widthPx, heightPx))
      .scale(Scale.FILL)
      .precision(Precision.EXACT)
      .allowHardware(false)
      .target { drawable ->
        post {
          val stateDrawable = drawable.asDrawable(context.resources)
          drawableCache[imageSource] = stateDrawable
          onDrawableReady(applyRenderingMode(stateDrawable, index))
        }
      }
      .listener(
        onError = { _, result ->
          Log.e("RCTTabView", "Error loading image: ${imageSource.uri}", result.throwable)
        }
      )
      .build()

    imageLoader.enqueue(request)
  }

  private fun applyRenderingMode(drawable: Drawable, index: Int): Drawable {
    val item = items.getOrNull(index) ?: return drawable

    return when {
      item.isAvatar && item.avatarUri != null -> {
        val bitmap = drawableToBitmap(drawable)
        val sizePx = bottomNavigation.itemIconSize
        val croppedBitmap = createCircularBitmap(bitmap, sizePx, item)
        val roundedDrawable = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
          .create(context.resources, croppedBitmap)

        // Don't set isCircular=true here since we already handle the circle manually
        // and the total bitmap may be larger than sizePx due to padding
        roundedDrawable.mutate().apply { setTintList(null) }
      }
      item.iconRenderingMode == "alwaysOriginal" -> {
        drawable.mutate().apply { setTintList(null) }
      }
      item.iconRenderingMode == "alwaysTemplate" -> drawable.mutate()
      else -> drawable
    }
  }

  private fun drawableToBitmap(drawable: Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) return drawable.bitmap

    val bitmap = android.graphics.Bitmap.createBitmap(
      drawable.intrinsicWidth.takeIf { it > 0 } ?: bottomNavigation.itemIconSize,
      drawable.intrinsicHeight.takeIf { it > 0 } ?: bottomNavigation.itemIconSize,
      android.graphics.Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)

    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
  }

  private fun createCircularBitmap(
    source: android.graphics.Bitmap,
    sizePx: Int,
    item: TabInfo
  ): android.graphics.Bitmap {
    val strokeColor = item.avatarStrokeColor?.let { parseHexColor(it) }

    val strokeWidthPx = if (strokeColor != null) TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      item.avatarStrokeWidth.toFloat(),
      context.resources.displayMetrics
    ) else 0f

    val gapPx = if (strokeColor != null) TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, item.avatarStrokeGap.toFloat(), context.resources.displayMetrics
    ) else 0f

    val padding = if (strokeColor != null) gapPx + strokeWidthPx else 0f
    val totalSizePx = (sizePx + padding * 2).toInt()

    val output = android.graphics.Bitmap.createBitmap(
      totalSizePx,
      totalSizePx,
      android.graphics.Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val radius = sizePx / 2f

    // Avatar rect starts after padding
    val avatarLeft = padding
    val avatarTop = padding

    // Save and clip to circle within padded area
    canvas.save()

    val path = android.graphics.Path()

    path.addCircle(
      avatarLeft + radius,
      avatarTop + radius,
      radius,
      android.graphics.Path.Direction.CW
    )

    canvas.clipPath(path)

    // Draw scaled source image into avatar rect
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(source, sizePx, sizePx, true)
    canvas.drawBitmap(scaledBitmap, avatarLeft, avatarTop, paint)
    canvas.restore()

    // Stroke — drawn outside the avatar circle, inside the padding
    if (strokeColor != null) {
      paint.color = strokeColor
      paint.style = android.graphics.Paint.Style.STROKE
      paint.strokeWidth = strokeWidthPx

      val strokeRadius = radius + gapPx + strokeWidthPx / 2f

      canvas.drawCircle(avatarLeft + radius, avatarTop + radius, strokeRadius, paint)
    }

    return output
  }

  fun setBarTintColor(color: Int?) {
    // Set the color, either using the active background color or a default color.
    val backgroundColor =
      color ?: Utils.getDefaultColorFor(context, android.R.attr.colorPrimary) ?: return

    // Apply the same color to both active and inactive states
    val colorDrawable = ColorDrawable(backgroundColor)

    bottomNavigation.itemBackground = colorDrawable
    bottomNavigation.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    hasCustomAppearance = true
  }

  fun setActiveTintColor(color: Int?) {
    activeTintColor = color
    updateTintColors()
  }

  fun setInactiveTintColor(color: Int?) {
    inactiveTintColor = color
    updateTintColors()
  }

  fun setActiveIndicatorColor(color: ColorStateList) {
    bottomNavigation.itemActiveIndicatorColor = color
  }

  fun setFontSize(size: Int) {
    fontSize = size
    updateTextAppearance()
  }

  fun setFontFamily(family: String?) {
    fontFamily = family
    updateTextAppearance()
  }

  fun setFontWeight(weight: String?) {
    val fontWeight = ReactTypefaceUtils.parseFontWeight(weight)
    this.fontWeight = fontWeight
    updateTextAppearance()
  }

  fun onDropViewInstance() {
    imageLoader.shutdown()
  }

  private fun updateTextAppearance() {
    // Early return if there is no custom text appearance
    if (fontSize == null && fontFamily == null && fontWeight == null) {
      return
    }

    val typeface = if (fontFamily != null || fontWeight != null) {
      ReactFontManager.getInstance().getTypeface(
        fontFamily ?: "",
        Utils.getTypefaceStyle(fontWeight),
        context.assets
      )
    } else null
    val size = fontSize?.toFloat()?.takeIf { it > 0 }

    val menuView = bottomNavigation.getChildAt(0) as? ViewGroup ?: return
    for (i in 0 until menuView.childCount) {
      val item = menuView.getChildAt(i)
      val largeLabel =
        item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_large_label_view)
      val smallLabel =
        item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_small_label_view)

      listOf(largeLabel, smallLabel).forEach { label ->
        label?.apply {
          size?.let { size ->
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
          }
          typeface?.let { setTypeface(it) }
        }
      }
    }
  }

  private fun emitHapticFeedback(feedbackConstants: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hapticFeedbackEnabled) {
      this.performHapticFeedback(feedbackConstants)
    }
  }

  private fun updateTintColors() {
    val currentItemTintColor = items.firstOrNull { it.key == selectedItem }?.activeTintColor

    val colorPrimary = currentItemTintColor ?: activeTintColor ?: Utils.getDefaultColorFor(
      context, android.R.attr.colorPrimary
    ) ?: return

    val colorSecondary = inactiveTintColor ?: Utils.getDefaultColorFor(
      context, android.R.attr.textColorSecondary
    ) ?: return

    val states = arrayOf(uncheckedStateSet, checkedStateSet)
    val colors = intArrayOf(colorSecondary, colorPrimary)
    val tintList = ColorStateList(states, colors)

    bottomNavigation.itemTextColor = tintList
    bottomNavigation.itemIconTintList = tintList

    // After applying global tint, remove tint for alwaysOriginal items
    post {
      val menuView = bottomNavigation.getChildAt(0) as? ViewGroup ?: return@post

      for (i in 0 until menuView.childCount) {
        val item = items.getOrNull(i) ?: continue

        if (item.iconRenderingMode == "alwaysOriginal" || item.isAvatar) {
          val itemView = menuView.getChildAt(i) ?: continue

          val iconView = itemView.findViewById<android.widget.ImageView>(
            com.google.android.material.R.id.navigation_bar_item_icon_view
          )

          iconView?.imageTintList = null
        }
      }
    }
  }

  private fun createAvatarDrawable(item: TabInfo, sizePx: Int): Drawable {
    val bitmap =
      android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)

    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val radius = sizePx / 2f

    // Save before clipping so we can restore for stroke
    canvas.save()

    // Clip to circle
    val path = android.graphics.Path()

    path.addCircle(radius, radius, radius, android.graphics.Path.Direction.CW)
    canvas.clipPath(path)

    // Background
    val bgColor =
      item.avatarBackgroundColor?.let { parseHexColor(it) } ?: android.graphics.Color.GRAY

    paint.color = bgColor
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(radius, radius, radius, paint)

    // Initials
    val initials = item.avatarInitials ?: ""

    paint.color = android.graphics.Color.WHITE
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.textSize = sizePx * 0.42f

    paint.typeface = if (fontFamily != null || fontWeight != null) {
      ReactFontManager.getInstance().getTypeface(
        fontFamily ?: "",
        Utils.getTypefaceStyle(fontWeight),
        context.assets
      )
    } else {
      android.graphics.Typeface.create(
        android.graphics.Typeface.DEFAULT,
        android.graphics.Typeface.BOLD
      )
    }

    val textBounds = android.graphics.Rect()

    paint.getTextBounds(initials, 0, initials.length, textBounds)
    canvas.drawText(initials, radius, radius - textBounds.exactCenterY(), paint)

    // Restore clip so stroke draws outside the circle boundary
    canvas.restore()

    // Stroke
    val strokeColor = item.avatarStrokeColor?.let { parseHexColor(it) }

    if (strokeColor != null) {
      val strokeWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        item.avatarStrokeWidth.toFloat(),
        context.resources.displayMetrics
      )

      val gapPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        item.avatarStrokeGap.toFloat(),
        context.resources.displayMetrics
      )

      paint.color = strokeColor
      paint.style = android.graphics.Paint.Style.STROKE
      paint.strokeWidth = strokeWidthPx
      canvas.drawCircle(radius, radius, radius - gapPx - strokeWidthPx / 2f, paint)
    }

    val roundedDrawable =
      androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(context.resources, bitmap)

    roundedDrawable.isCircular = true

    return roundedDrawable.mutate().apply { setTintList(null) }
  }

  private fun parseHexColor(hex: String): Int? {
    return try {
      android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    } catch (e: Exception) {
      null
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
    if (uiModeConfiguration == newConfig?.uiMode || hasCustomAppearance) {
      return
    }

    // User has hidden the bottom navigation bar, don't re-attach it.
    if (bottomNavigation.visibility == GONE) {
      return
    }

    // If appearance wasn't changed re-create the bottom navigation view when configuration changes.
    // React Native opts out ouf Activity re-creation when configuration changes, this workarounds that.
    // We also opt-out of this recreation when custom styles are used.
    removeView(bottomNavigation)
    bottomNavigation = ExtendedBottomNavigationView(context)
    addView(bottomNavigation)
    updateItems(items)
    setLabeled(this.labeled)
    this.selectedItem?.let { setSelectedItem(it) }
    uiModeConfiguration = newConfig?.uiMode ?: uiModeConfiguration
  }
}
