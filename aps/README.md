# Amazon Publisher Services Android SDK

## Summary

Amazon Publisher Services (APS) provides a suite of cloud-based services that enable web, mobile app, and advanced TV publishers to grow their businesses. Used by thousands of leading publishers throughout the world, our Transparent Ad Marketplace (TAM) service enables you to increase monetization by adding many leading sources of advertising demand, through one server-to-server integration. Learn more at: https://aps.amazon.com/aps/solutions-for-mobile-app-developers/

## Installation

To get started:

Open your project's `build.gradle` file, and add the following to the repositories and dependencies objects:

```
repositories {
    ...
    jcenter()
}

dependencies {
    ...
    implementation 'com.amazon.android:aps-sdk:8.2.1@aar'
}
```
If you are using AdMob, include the AdMob adapter as a dependency:

```
dependencies {
    ...
    implementation 'com.amazon.android:aps-sdk:8.2.1@aar'
    implementation 'com.amazon.android:aps-admob-adapter:1.2.0@aar'
}
```

## License
This software is licensed as "Program Materials" under the Amazon Publisher Services Agreement, which is available at https://ams.amazon.com/webpublisher/apsmanaged/apsagreement.html (the "License"). See the License for the specific language governing permissions and limitations under the License.

These materials may include third party software that is copyrighted by other parties and is subject to separate license terms. Information on that software and the applicable copyright notices and license terms are included in the separate NOTICE.txt accompanying the applicable materials. You acknowledge and agree that your license to use and distribute these materials is conditioned on your providing to your users the notices and disclaimers that are specified by such license terms.

These materials are distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

## Notice
Licenses to the Google Mobile Ads SDK are governed by the Google Terms of Service at https://policies.google.com/terms?gl=US&hl=en and the Google API Terms of Service at https://developers.google.com/terms/

Information about the Google Mobile Ads SDK for iOS is available here: https://developers.google.com/mobile-ads-sdk/docs/dfp/ios/download

Information about the Google Mobile Ads SDK for Android is available here: https://developers.google.com/mobile-ads-sdk/docs/dfp/android/sdk

License to the 5.0 MoPub SDK is governed by the following terms:

MoPub's terms are available here: https://www.mopub.com/legal/sdk-license-agreement/

MOAT's terms are available here: https://moat.com/terms and here: https://moat.com/third_party_software_attribution

IAS's terms are available here: https://integralads.com/sdk-license-agreement/

Information about the MoPub iOS SDK is available here: https://developers.mopub.com/docs/ios/getting-started/

Information about the MoPub Android SDK is available here: https://bintray.com/mopub/mopub-android-sdk/mopub-android-sdk/5.0.0#read

