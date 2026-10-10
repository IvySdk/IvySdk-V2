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

import com.google.android.gms.ads.AdView
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Manages a cache of banner ad instances. */
class BannerCache {

  private val mutex = Mutex()
  /** List of banner ads that have started loading but have not yet finished loading. */
  private val unloadedBanners = mutableListOf<AdView>()
  /** List of banner ads that have successfully loaded. */
  private val loadedBanners = mutableListOf<AdView>()

  /**
   * Adds a banner ad to the loaded banners cache.
   *
   * @param banner The banner ad to be added.
   */
  suspend fun addLoaded(banner: AdView) {
    mutex.withLock {
      if (unloadedBanners.remove(banner)) {
        loadedBanners.add(banner)
      }
    }
  }

  /**
   * Adds a banner ad to the unloaded banners cache.
   *
   * @param banner The banner ad to be added.
   */
  suspend fun addUnloaded(banner: AdView) {
    mutex.withLock { unloadedBanners.add(banner) }
  }

  /**
   * Gets the size of the loaded banners cache.
   *
   * @return The size of the loaded banners cache.
   */
  suspend fun getLoadedSize(): Int {
    return mutex.withLock { loadedBanners.size }
  }

  /**
   * Gets the next banner ad from the loaded banners cache.
   *
   * @return A banner ad, or null if the loaded banners cache is empty.
   */
  suspend fun getNextLoadedBannerAd(): AdView? {
    return mutex.withLock {
      if (loadedBanners.isNotEmpty()) {
        loadedBanners.removeAt(0)
      } else {
        null
      }
    }
  }

  /** Cleans up resources held by the cache. */
  fun destroy() {
    for (banner in unloadedBanners) {
      banner.destroy()
    }
    for (banner in loadedBanners) {
      banner.destroy()
    }
    unloadedBanners.clear()
    loadedBanners.clear()
  }
}
