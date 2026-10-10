# XiaomiAds

`XiaomiAds` supports three paths in one module: a standalone BlastAds platform, AdMob mediation,
and AppLovin MAX mediation. The standalone path supports banner, interstitial, and rewarded ads and
can participate in Ivy's own waterfall independently of AdMob or MAX.

## Dependency

Add the `XiaomiAds` component. The module pins Columbus to `3.6.0.4`, which preserves the APIs used
by Xiaomi's currently published AdMob and MAX adapters. Do not also add Columbus 4.x to the same
application.

## Platform configuration

```json
{
  "platforms": {
    "xiaomi": {
      "appKey": "YOUR_APP_KEY",
      "appSecret": "YOUR_APP_SECRET",
      "gdprConsent": false,
      "staging": false,
      "lite": false
    }
  }
}
```

- `appKey` and `appSecret` are required.
- `gdprConsent` is optional. If omitted, the module reads the shared Google UMP/IAB result.
- `staging` must remain `false` in production.
- `lite` is optional and defaults to `false`.

The module waits for the shared `CMPUtil` UMP flow, calls `AdGlobalSdk.setGDPRConsent(...)`, and
only then initializes Columbus. Apps using another CMP can call
`XiaomiAds.setGDPRConsent(boolean)` before BlastAds setup.

## Waterfall configuration

Use `platform: "xiaomi"` in the normal BlastAds arrays. Add `fallback: true` when Xiaomi should
only be selected after all non-fallback sources are unavailable:

```json
{
  "inter": [
    {
      "platform": "xiaomi",
      "adUnit": "YOUR_INTERSTITIAL_PLACEMENT",
      "fallback": true
    }
  ],
  "reward": [
    {
      "platform": "xiaomi",
      "adUnit": "YOUR_REWARDED_PLACEMENT",
      "fallback": true
    }
  ],
  "banner": [
    {
      "platform": "xiaomi",
      "adUnit": "YOUR_BANNER_PLACEMENT",
      "fallback": true,
      "width": 320,
      "height": 50
    }
  ]
}
```

Columbus documents banner sizes `320x50` and `300x250`. The default is `320x50`.

Documented Columbus test credentials and placements:

- App key: `MISDK_DEMO`
- App secret: `ee60917ed59044e5dbf7f71fa366acd3`
- Banner: `1.305.2.1`
- MREC: `1.305.2.5`
- Interstitial image: `1.305.17.1`
- Interstitial video: `1.305.17.2`
- Rewarded: `1.305.29.1`

Xiaomi notes that these test placements do not load when the device language is Chinese.

## AdMob mediation

The module includes Xiaomi's AdMob adapter `23.2.0.2`. Configure an AdMob waterfall custom event
with this class:

`com.zeus.gmc.sdk.mobileads.columbus.mediationadapter.ColumbusAdmobMediationAdapter`

Use this server parameter:

```json
{"app_id":"MediaType#AppSecret","placement_id":"Columbus placement ID"}
```

If an explicit GDPR value is needed before AdMob initialization, put `xiaomiGdprConsent` under
`platforms.admob`; otherwise the shared UMP result is used.

## MAX mediation

The module includes Xiaomi's MAX adapter `13.2.0.1`. Configure a MAX custom SDK network with:

`com.zeus.gmc.sdk.mobileads.columbus.mediationadapter.ColumbusMaxMediationAdapter`

Set Custom Network App ID to `MediaType#AppSecret` and Placement ID to the Columbus placement ID.
If an explicit GDPR value is needed, put `xiaomiGdprConsent` under `platforms.max`.

Xiaomi's published adapters target Google Mobile Ads `23.2.0` and AppLovin `13.2.0`, while this
repository currently uses newer host SDKs. The host SDK dependencies are excluded to prevent
downgrades; validate both mediation paths on a real device before production rollout.
