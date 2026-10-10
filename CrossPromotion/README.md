# CrossPromotion

交叉推广只有一个业务对象：`Ad`。每条广告包含目标 App、文字和三类可选资源：

```text
Ad
├── icon
├── banner
├── image
├── video
├── image2
└── video2
```

模块没有 Campaign、Creative 或 Placement。调用方决定请求 Banner、Interstitial 或
Rewarded，SDK 在展示前从已经就绪的广告中统一选择。

## 配置

```json
{
  "version": "20260807-001",
  "enabled": true,
  "ads": [
    {
      "id": "game_a_ad_1",
      "enabled": true,
      "weight": 100,
      "targetPackage": "com.example.game.a",
      "title": "Example Game",
      "description": "Play this amazing game",
      "callToAction": "Install",
      "icon": "asset:///crosspromotion/game_a/icon.webp",
      "banner": "asset:///crosspromotion/game_a/banner.webp",
      "image": "https://cdn.example.com/game_a/interstitial.webp",
      "video": "https://cdn.example.com/game_a/video.mp4",
      "image2": "https://cdn.example.com/game_a/interstitial-landscape.webp",
      "video2": "https://cdn.example.com/game_a/video-landscape.mp4"
    }
  ]
}
```

`weight` 可省略，默认值为 `1`。没有流量权重需求时，所有可用广告等概率选择。
`icon` 用于 Banner、播放页浮标和详情落地页，需提供方形应用图标。
`image` / `video` 是第一套全屏素材。只有横竖屏游戏的配置才包含第二套：第一套固定为竖屏，
`image2` / `video2` 固定为横屏。固定方向游戏由配置生成端把对应方向压平到第一套，SDK 不需要知道该游戏的固定方向。

资源字段支持：

- `asset:///path`：应用 `src/main/assets` 中的 APK 内置文件；
- `https://...`：下载到应用缓存的远程文件；
- `http://...`：仅建议本地测试使用。

例如 `asset:///crosspromotion/banner.webp` 对应：

```text
app/src/main/assets/crosspromotion/banner.webp
```

APK 与远程文件最后都会转换成本地文件，渲染层不区分来源。

## 启动时检查配置服务器

传入 `configUrl` 后，每次进程启动、Promotion 初始化时都会在后台请求一次服务器：

```java
CrossPromotion.getInstance().initialize(
        context,
        bundledFallbackConfig,
        "https://config.example.com/cross-promotion/config.json",
        listener);
```

启动顺序是：

```text
APK/Grid 默认配置 -> 上次远端有效配置 -> 本次服务器检查
```

本地配置或上次有效配置会立即用于缓存素材，不等待网络。服务器返回的新 JSON 只有在完整下载并通过字段校验后才会整体替换当前配置；请求失败、超时或 JSON 非法时继续使用当前配置。

SDK 会持久化最后一次有效的远端 JSON 和 ETag。即使首次启动尚无远端缓存，也会把 APK
配置中的 `version` 作为 `If-None-Match` 发送；服务器返回 HTTP 304 时直接继续使用当前已加载
的 APK 配置。后续启动优先发送远端缓存的 ETag。配置最大为 1 MiB，生产环境应使用 HTTPS。
也可以主动调用 `refreshConfig()` 再检查一次。

监听服务器检查结果：

```java
@Override
public void onConfigChecked(String version, boolean updated) {
}

@Override
public void onConfigCheckFailed(String reason) {
    // 当前配置仍然有效，无需停止广告流程。
}
```

## 展示规则

- Banner：只选择 `banner` 和 `icon` 都已就绪的广告；
- Interstitial：横竖屏配置按 Activity 当前方向选择素材组；组内优先使用已就绪的 `video`，否则使用 `image`，5 秒后允许关闭；
- Rewarded：横竖屏配置按 Activity 当前方向选择素材组；组内配置了视频时等待视频就绪并在完整播放后发奖，没有视频时使用图片，展示 5 秒后发奖。

全屏素材按比例完整展示，不裁切内容。播放层的 `Ad` 标签固定在屏幕左上角；竖屏使用包含图标、标题和 CTA 的展开胶囊，横屏只显示应用图标。视频播放完成后进入统一的详情落地页，展示图标、标题、描述和 CTA。
插屏视频在 5 秒后允许点击关闭；如果视频尚未播放完成，第一次关闭会先进入详情落地页，再次关闭才退出。图片插屏展示 5 秒后进入同一详情落地页，竖屏和横屏使用同一套落地页结构。
图片 Rewarded 的 5 秒只累计广告处于前台的时间；进入后台或跳转商店时暂停，返回后继续。

`show()` 不会等待网络下载。没有已缓存资源时立即返回未就绪，并继续在后台缓存。

## 直接使用

```java
CrossPromotionConfig config = CrossPromotionConfig.fromJson(json);
CrossPromotion.getInstance().initialize(context, config, listener);
CrossPromotion.getInstance().preload();

if (CrossPromotion.getInstance().isReady(CrossPromotionConfig.Format.INTERSTITIAL)) {
    CrossPromotion.getInstance().showInterstitial(activity);
}

if (CrossPromotion.getInstance().isReady(CrossPromotionConfig.Format.REWARDED)) {
    CrossPromotion.getInstance().showRewarded(activity);
}

CrossPromoBannerView banner = CrossPromotion.getInstance().createBanner(context);
if (banner != null) {
    container.addView(banner);
}
```

监听器的第一个参数是 `banner`、`interstitial` 或 `rewarded`，第二个参数是广告 `id`。

## AppsFlyer 安装归因

曝光时调用：

```java
CrossPromotionHelper.logCrossPromoteImpression(
        context, ad.targetPackage, ad.id, parameters);
```

点击时调用：

```java
CrossPromotionHelper.logAndOpenStore(
        context, ad.targetPackage, ad.id, parameters);
```

AppsFlyer 的 promoted App ID 使用 `targetPackage`，Campaign 和 `af_ad` 都直接使用广告
`id`，JSON 不需要额外的 Campaign 或 click URL。来源 App 与目标 App 都必须正确初始化
AppsFlyer，目标 App 安装并首次启动后才会产生安装归因。

上报参数包括：

- `af_ad`：广告 `id`；
- `af_ad_type`：`banner`、`interstitial` 或 `rewarded_video`；
- `af_ad_format`：`image` 或 `video`；
- `af_sub1`：配置版本；
- `is_incentivized=true`：Rewarded。

点击前检查系统是否有应用可以处理 `market://`。没有时，在点击参数中设置
`af_r` 为目标 App 的 Google Play HTTPS 页面，让 AppsFlyer 归因后打开网页。
Manifest 已声明 `market` 查询，支持 Android 11 及以上的应用可见性检查。

AppsFlyer 调用同步抛出异常时，点击会回退到目标 Package 的 Google Play 页面。
AF 6.17.2 在内部异步处理网络请求和页面跳转，没有提供失败回调；网络失败等异步错误
不会触发外层的异常兜底。

全屏广告点击后显示加载遮罩，等待期间不重复发起点击。10 秒内未发起外部页面跳转，
提示 `Opening failed` 并自动关闭广告页；返回键也可以取消等待并关闭广告。
断网或 AF 没有返回跳转结果时，也走同一条 10 秒超时路径。页面退到后台或销毁时，取消等待。
传给 AF 的 Context 包装器会在主线程检查本次点击是否仍有效及是否超过截止时间，
避免超时、取消或退出后，迟到的 AF 响应再次打开商店。此处理只阻止页面跳转，不取消
AF 的网络请求或已上报事件。该行为已核对 AF 6.17.2 使用传入 Context 启动页面的实现，
升级 AF SDK 时应重新验证。Banner 不受全屏页面的自动关闭逻辑影响。

## 接入 BlastAds

`CrossPromotion` AAR 内置 BlastAds 的初始化、Banner、Interstitial 和 Rewarded 适配器。
CrossPromotion 不使用 `adUnit` 做内部选择，三个广告类型可以使用同一个固定值：

```json
{
  "default": {
    "inter": [{
      "platform": "promotion",
      "adUnit": "cross_promotion",
      "fallback": true,
      "ecpm": 0.1
    }],
    "reward": [{
      "platform": "promotion",
      "adUnit": "cross_promotion",
      "fallback": true,
      "ecpm": 0.1
    }],
    "banner": [{
      "platform": "promotion",
      "adUnit": "cross_promotion",
      "fallback": true,
      "ecpm": 0.1
    }]
  },
  "platforms": {
    "promotion": {
      "configUrl": "https://config.example.com/cross-promotion/config.json",
      "config": {
        "version": "apk-fallback-1",
        "enabled": false,
        "ads": []
      }
    }
  }
}
```

交叉推广 Banner 展示期间会定时重新选择内部广告。Banner 条目配置了
`refreshSeconds` 时使用该间隔；未配置或配置为 `0` 时默认每 30 秒轮换一次。
Banner、插屏和激励视频分别使用平滑加权轮询：长期展示比例遵循 `weight`，同时尽量把同一条广告均匀分散，
避免普通加权随机在短时间内连续命中同一条。每次只对当前已就绪且符合条件的广告进行轮询。
如果其他非 `promotion` Banner 在交叉推广展示期间加载成功，BlastAds 会立即按照
现有优先级选择并替换交叉推广；切换后由新平台继续负责自身的 Banner 刷新。

`fallback=true` 保证正常变现广告优先，只有没有正常广告可展示时才选择交叉推广。

`PromotionInit` 每次启动都会请求 `configUrl`。优先使用平台配置中的地址；未配置时读取
APK 内置 `crosspromotion.json` 的 `configUrl`。`config` 是网络失败时的启动兜底；如果它和
Grid 配置都不存在，则尝试读取：

```text
app/src/main/assets/crosspromotion.json
```

内置文件即使没有广告也必须包含数字版本号、`enabled: false`、空 `ads` 和 `configUrl`，
以便客户端用该版本发起 ETag 条件请求。

配置只接受本文档定义的标准字段、枚举值和结构，不解析字段别名或旧结构。
