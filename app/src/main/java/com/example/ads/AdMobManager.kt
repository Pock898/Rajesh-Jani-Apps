package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.StitchBlueContainer
import com.example.ui.theme.StitchBlue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob Management Utility & Ad Unit IDs provided by the user.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"

    // User provided Ad Unit IDs:
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Fixed Size Banner
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Interstitial
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Rewarded Ads

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    /**
     * Initialize Mobile Ads SDK. Safe to call multiple times.
     */
    fun initialize(context: Context) {
        try {
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(config)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: $status")
            }
            loadInterstitialAd(context)
            loadRewardedAd(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds", e)
        }
    }

    /**
     * Preload Interstitial Ad
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Failed to load interstitial ad: ${error.message}")
                }
            }
        )
    }

    /**
     * Show Interstitial Ad if available, then invokes onAdDismissed.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            // If ad not ready, proceed immediately and try loading
            loadInterstitialAd(activity)
            onAdDismissed()
        }
    }

    /**
     * Preload Rewarded Ad
     */
    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Failed to load rewarded ad: ${error.message}")
                }
            }
        )
    }

    /**
     * Show Rewarded Ad.
     * @param activity Context activity
     * @param onUserEarnedReward Callback when reward is earned
     * @param onAdClosed Callback when ad is finished or fails
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (Int, String) -> Unit,
        onAdClosed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    onAdClosed()
                }
            }
            ad.show(activity) { rewardItem ->
                rewardEarned = true
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
        } else {
            loadRewardedAd(activity)
            onAdClosed()
        }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null
    fun isInterstitialReady(): Boolean = interstitialAd != null
}

/**
 * Modern Jetpack Compose AdMob Banner Composable.
 * Adapts gracefully and displays a placeholder in Compose preview/inspection mode.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.BANNER_AD_UNIT_ID,
    adSize: AdSize = AdSize.BANNER
) {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StitchBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Banner Preview",
                color = StitchBlue,
                fontSize = 12.sp
            )
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(adSize)
                        setAdUnitId(adUnitId)
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
