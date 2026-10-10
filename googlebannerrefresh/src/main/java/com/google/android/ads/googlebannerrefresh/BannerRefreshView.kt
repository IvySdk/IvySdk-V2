// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.ads.googlebannerrefresh

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.ResponseInfo

/** A view that displays and refreshes a banner ad. */
class BannerRefreshView
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
  private var _adUnitId: String? = null
  private var adSize: AdSize? = null
  private var configuration: BannerRefreshConfiguration? = null
  private var bannerManager: BannerManager? = null
  internal lateinit var adRequest: AdRequest
  var adListener: AdListener? = null

  var adUnitId: String
    get() =
      try {
        checkNotNull(_adUnitId)
      } catch (e: NullPointerException) {
        Log.e(Constants.TAG, "getAdUnitId(...) must not be null")
        throw e
      }
    set(value) {
      if (_adUnitId == null) {
        _adUnitId = value
      } else {
        Log.w(
          Constants.TAG,
          "Ignoring new ad unit ID: $value. Ad unit ID remains at: $_adUnitId. " +
            "Ad unit ID should only be set once.",
        )
      }
    }

  /**
   * Loads and starts refreshing banner ads.
   *
   * @param builder The [AdRequest.Builder] to use for loading ads.
   */
  fun load(builder: AdRequest.Builder) {
    if (_adUnitId == null || adSize == null || configuration == null) {
      Log.e(
        Constants.TAG,
        "Cannot load ads: Required refresh parameters (adUnitId, adSize, configuration) are not all set.",
      )
      return
    }

    val extras = builder.build().getNetworkExtrasBundle(AdMobAdapter::class.java) ?: Bundle()
    // Disable autorefresh.
    extras.putInt("_noRefresh", 1)
    // Indicate requests are using this banner refresh plugin.
    extras.putString("google-banner-refresh", "1.0.0")
    builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)

    // Indicate through request agent that requests are using this banner refresh plugin.
    builder.setRequestAgent("google-banner-refresh-1.0.0")

    adRequest = builder.build()

    bannerManager = BannerManager(this, configuration!!)
    bannerManager?.load(configuration!!.adsBufferSize)
  }

  fun setConfiguration(configuration: BannerRefreshConfiguration) {
    if (this.configuration == null) {
      this.configuration = configuration
    } else {
      Log.w(
        Constants.TAG,
        "Ignoring new refresh configuration: ${configuration}. " +
          "setConfiguration() should only be called once.",
      )
    }
  }

  fun getConfiguration(): BannerRefreshConfiguration? {
    return configuration
  }

  fun setAdSize(adSize: AdSize) {
    if (this.adSize == null) {
      this.adSize = adSize
    } else {
      Log.w(
        Constants.TAG,
        "Ignoring new ad size: ${adSize}. Ad size remains at: ${this.adSize}. " +
          "setAdSize() should only be called once.",
      )
    }
  }

  fun getAdSize(): AdSize? {
    return adSize
  }

  fun getResponseInfo(): ResponseInfo? {
    return bannerManager?.getCurrentAdView()?.responseInfo
  }

  /** Cleans up resources held by the view. */
  fun destroy() {
    bannerManager?.destroy()
    bannerManager = null
  }

  override fun onDetachedFromWindow() {
    destroy()
    super.onDetachedFromWindow()
  }
}
