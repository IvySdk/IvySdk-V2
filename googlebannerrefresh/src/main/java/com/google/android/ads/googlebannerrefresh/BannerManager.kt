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

import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the loading and display of banner ads, including caching and refreshing.
 *
 * This class is responsible for handling the AdView instance, making ad requests, caching ads, and
 * rotating them at specified refresh rates. It's tied to a specific BannerRefreshView.
 */
class BannerManager(
  private val bannerRefreshView: BannerRefreshView,
  private val configuration: BannerRefreshConfiguration,
) {

  private enum class BannerState {
    /** The banner view is ready to rotate in a new banner ad. */
    READY_TO_ROTATE,
    /** The banner view is displaying a banner ad but is not on screen. */
    WAITING_FOR_IMPRESSION,
    /** The banner view was on screen and its rotation timer has started. */
    ACTIVE,
  }

  // Uses SupervisorJob to make sure if one ad loading coroutine fails, the entire ad refresh
  // process won't be canceled.
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val bannerCache = BannerCache()
  private val refreshBackoff =
    BannerRefreshBackoff(configuration.adsBufferSize, configuration.defaultRefreshRateInSeconds)
  private var bannerState: BannerState = BannerState.READY_TO_ROTATE
  private var currentAdView: AdView? = null
  private var rotationJob: Job? = null

  /**
   * Gets the currently displayed [AdView].
   *
   * @return The currently displayed [AdView], or null if no ad is currently displayed.
   */
  fun getCurrentAdView(): AdView? {
    return currentAdView
  }

  /** Loads banner ads into the cache. */
  fun load(bannersToLoad: Int = 1) {
    repeat(bannersToLoad) {
      val adView =
        AdView(bannerRefreshView.context).apply {
          adUnitId = bannerRefreshView.adUnitId
          bannerRefreshView.getAdSize()?.let { setAdSize(it) }
          adListener = createAdListener(this@apply)
        }

      adView.loadAd(bannerRefreshView.adRequest)
      scope.launch { bannerCache.addUnloaded(adView) }
    }
  }

  /** Rotates the displayed banner ad by retrieving a cached ad and loads a new one. */
  private fun tryRotateBannerView() {
    scope.launch {
      bannerState = BannerState.READY_TO_ROTATE

      // If there is no next loaded banner ad, keep the current banner ad on screen and return.
      val nextBannerAd = bannerCache.getNextLoadedBannerAd() ?: return@launch

      // Load a new banner ad after pulling one from the cache.
      load()

      bannerRefreshView.removeAllViews()
      currentAdView = nextBannerAd
      bannerRefreshView.addView(nextBannerAd)
      bannerState = BannerState.WAITING_FOR_IMPRESSION
    }
  }

  /**
   * Creates an [AdListener] for an [AdView] to handle ad events.
   *
   * This listener forwards events to the [bannerRefreshView]'s listener and also calls the
   * corresponding methods within the [BannerManager] to handle internal logic like caching and
   * refreshing.
   *
   * @param adView The [AdView] to create the listener for.
   * @return An [AdListener] instance.
   */
  private fun createAdListener(adView: AdView): AdListener {
    return object : AdListener() {
      override fun onAdLoaded() {
        scope.launch {
          refreshBackoff.resetBackoff()

          val isCacheEmpty = bannerCache.getLoadedSize() == 0
          // Marking a banner as loaded that was previously marked as unloaded adds to the cache.
          bannerCache.addLoaded(adView)
          val isCacheEmptyChanged = isCacheEmpty && bannerCache.getLoadedSize() == 1

          bannerRefreshView.adListener?.onAdLoaded()

          if (isCacheEmptyChanged && bannerState == BannerState.READY_TO_ROTATE) {
            tryRotateBannerView()
          }
        }
      }

      override fun onAdFailedToLoad(adError: LoadAdError) {
        scope.launch {
          bannerRefreshView.adListener?.onAdFailedToLoad(adError)

          val currentBackoffInSeconds = refreshBackoff.getCurrentBackoffInSeconds()
          refreshBackoff.increaseBackoff()
          delay(currentBackoffInSeconds * 1000L)
          adView.loadAd(bannerRefreshView.adRequest)
        }
      }

      override fun onAdImpression() {
        scope.launch {
          // It's a no-op if an impression is recorded before the ad is placed on screen.
          if (adView !== currentAdView) {
            Log.d(Constants.TAG, "An impression was recorded before the ad was placed on screen.")
            bannerRefreshView.adListener?.onAdImpression()
            return@launch
          }

          // Prevent autorefresh impressions from triggering a scheduled job when a rotation is
          // already scheduled.
          if (rotationJob?.isActive == true) {
            bannerRefreshView.adListener?.onAdImpression()
            return@launch
          }

          val adSourceId = adView.responseInfo?.loadedAdapterResponseInfo?.adSourceId

          val refreshRateInSeconds: Int =
            if (adSourceId != null) {
              configuration.adSourceRefreshRatesInSeconds[adSourceId]
                ?: configuration.defaultRefreshRateInSeconds
            } else {
              configuration.defaultRefreshRateInSeconds
            }

          Log.d(Constants.TAG, "Refresh interval is set to $refreshRateInSeconds seconds.")

          bannerState = BannerState.ACTIVE
          // Schedule the next ad rotation.
          rotationJob =
            scope.launch {
              delay(refreshRateInSeconds * 1000L)
              tryRotateBannerView()
              rotationJob = null
            }

          bannerRefreshView.adListener?.onAdImpression()
        }
      }

      override fun onAdOpened() {
        bannerRefreshView.adListener?.onAdOpened()
      }

      override fun onAdClicked() {
        bannerRefreshView.adListener?.onAdClicked()
      }

      override fun onAdClosed() {
        bannerRefreshView.adListener?.onAdClosed()
      }

      override fun onAdSwipeGestureClicked() {
        bannerRefreshView.adListener?.onAdSwipeGestureClicked()
      }
    }
  }

  fun destroy() {
    bannerCache.destroy()
    scope.cancel()
    rotationJob = null
  }
}
