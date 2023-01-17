package com.willdev.openvpn.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.mopub.common.MoPub;
//import com.mopub.common.MoPubReward;
//import com.mopub.mobileads.MoPubErrorCode;
//import com.mopub.mobileads.MoPubRewardedAdListener;
//import com.mopub.mobileads.MoPubRewardedAds;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.RewardedVideoCallbacks;
import com.willdev.openvpn.R;
import com.willdev.openvpn.adapter.VipServerAdapter;
import com.willdev.openvpn.api.WebAPI;
import com.willdev.openvpn.model.Server;
import com.willdev.openvpn.utils.Config;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VipServersFragment extends Fragment implements VipServerAdapter.OnSelectListener {

    @BindView(R.id.rcv_servers)
    RecyclerView rcvServers;
    private RelativeLayout animationHolder;
    @BindView(R.id.purchase_layout)
    RelativeLayout mPurchaseLayout;
    @BindView(R.id.vip_unblock)
    ImageButton mUnblockButton;
    private VipServerAdapter serverAdapter;
    AlertDialog.Builder builder;
    private Context context;
    private ProgressDialog progressDialog;

    private RewardedVideoAd rewardedVideoAd;
    private String TAG = "RewarderVideoAd";
    private StartAppAd startAppAd;
    MaxRewardedAd rewardedAd;
    Server serverr;
    private RewardedAd mRewardedAd;
    private Boolean facebookAd = false, startAd = false, mopubAd = false, appLovin = false;
    private boolean isRewardVideoLoaded = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vip_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this,view);

        mPurchaseLayout.setVisibility(View.GONE);
        progressDialog = new ProgressDialog(getContext());

        progressDialog.show();
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.e("REWARDED INITIALIZ", initializationStatus.getAdapterStatusMap().toString());
                loadAds();
            }
        });

        context = getActivity();

        Log.e("rewardID", WebAPI.ADMOB_REWARD_ID);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false);
        serverAdapter = new VipServerAdapter(getActivity());
        serverAdapter.setOnSelectListener(this);
        rcvServers.setLayoutManager(layoutManager);
        rcvServers.setAdapter(serverAdapter);
        loadServers();
    }

    private void loadServers() {
        ArrayList<Server> servers = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(WebAPI.PREMIUM_SERVERS);
            for (int i=0; i < jsonArray.length();i++){
                JSONObject object = (JSONObject) jsonArray.get(i);
                servers.add(new Server(object.getString("serverName"),
                        object.getString("flagURL"),
                        object.getString("ovpnConfiguration"),
                        object.getString("vpnUserName"),
                        object.getString("vpnPassword")
                ));
                Log.v("Servers",object.getString("ovpnConfiguration"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        serverAdapter.setData(servers);
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @OnClick(R.id.vip_unblock)
    void openPurchase(){


    }

    @Override
    public void onSelected(Server server) {
        if (getActivity() != null)
        {
            if (Config.vip_subscription || Config.all_subscription) {
                Intent mIntent = new Intent();
                mIntent.putExtra("server", server);
                getActivity().setResult(getActivity().RESULT_OK, mIntent);
                getActivity().finish();

            } else
            {
                serverr = server;
                showDialog(getActivity());
            }

        }
    }

    public void showDialog(Activity activity) {

        builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ads_dialog, null);
        builder.setView(dialogView);
        Button purchase = dialogView.findViewById(R.id.purchase);
        Button watchAd = dialogView.findViewById(R.id.watchAd);
        purchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),PurchaseActivity.class);
                startActivity(intent);
            }
        });

        watchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(WebAPI.ADS_TYPE.equals(WebAPI.ADS_TYPE_ADMOB)) {
                    if (mRewardedAd != null) {
                        Activity activityContext = getActivity();
                        mRewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {

                                Log.d("TAG", "The user earned the reward.");
                                int rewardAmount = rewardItem.getAmount();
                                String rewardType = rewardItem.getType();
                            }
                        });
                    } else {
                        Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                    }
                }
                else if(WebAPI.ADS_TYPE.equals(WebAPI.ADS_TYPE_FACEBOOK_ADS)) {
                    if (facebookAd)
                        rewardedVideoAd.show();
                    else {
                        Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                    }

                } else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_STR)) {
                        if(startAd) {

                            startAd = false;
                            startAppAd.showAd();

                            Intent mIntent = new Intent();
                            mIntent.putExtra("server", serverr);
                            getActivity().setResult(getActivity().RESULT_OK, mIntent);
                            getActivity().finish();

                            startAppAd.setVideoListener(new VideoListener() {
                                @Override
                                public void onVideoCompleted() {
                                    Log.d("STARTAPP", ": Rewarded video completed!");

                                    startAppAd = null;
                                }
                            });
                        }
                        else {
                            Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                        }
                } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_MP)) {
/*
                    if (mopubAd)
                        MoPubRewardedAds.showRewardedAd(WebAPI.ADMOB_REWARD_ID);
                    else {
                        Intent mIntent = new Intent();
                        mIntent.putExtra("server", serverr);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();
                    }
*/
                    Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();

                } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_UT)) {

                    if (UnityAds.isReady (WebAPI.ADMOB_REWARD_ID)) {
                        UnityAds.show (activity, WebAPI.ADMOB_REWARD_ID);

                        UnityAds.addListener(new IUnityAdsListener() {
                            @Override
                            public void onUnityAdsReady(String s) {

                            }

                            @Override
                            public void onUnityAdsStart(String s) {

                            }

                            @Override
                            public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {

                                if(s.equals(WebAPI.ADMOB_REWARD_ID)) {
                                    Log.v("UNITY", " reward ad finished");

                                    Intent mIntent = new Intent();
                                    mIntent.putExtra("server", serverr);
                                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                                    getActivity().finish();
                                }
                            }

                            @Override
                            public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

                                if(s.equals(WebAPI.ADMOB_REWARD_ID)) {
                                    Log.e("UNITY", " reward ad error, " + unityAdsError.toString());

                                    Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                    }
                } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APV)) {
                    if (rewardedAd.isReady()) {
                        rewardedAd.showAd();
                    } else {
                        Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                    }
                } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APD)) {
                    if(isRewardVideoLoaded) {
                        Log.v("APPODEAL", "reward loaded");
                        Appodeal.show(getActivity(), Appodeal.REWARDED_VIDEO);
                        isRewardVideoLoaded = true;
                    }  else {
                        Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    void loadAds()
    {

        //ADMOB
        if(WebAPI.ADS_TYPE.equals(WebAPI.ADS_TYPE_ADMOB)) {
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(getActivity(), WebAPI.ADMOB_REWARD_ID,
                    adRequest, new RewardedAdLoadCallback(){
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                            mRewardedAd = null;
                            progressDialog.dismiss();
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
                            progressDialog.dismiss();

                            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdShowedFullScreenContent() {

                                    Log.d(TAG, "Ad was shown.");
                                    mRewardedAd = null;
                                    Intent mIntent = new Intent();
                                    mIntent.putExtra("server", serverr);
                                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                                    getActivity().finish();
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {

                                    Log.d(TAG, "Ad was dismissed.");
                                    mRewardedAd = null;
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    });
        }

        //FACEBOOK
        else if(WebAPI.ADS_TYPE.equals(WebAPI.ADS_TYPE_FACEBOOK_ADS)) {
            rewardedVideoAd = new RewardedVideoAd(getActivity(), WebAPI.ADMOB_REWARD_ID);
            RewardedVideoAdListener rewardedVideoAdListener = new RewardedVideoAdListener() {
                @Override
                public void onError(Ad ad, AdError error) {

                    Log.e(TAG, "FB Rewarded video ad failed to load: " + error.getErrorMessage());
                    progressDialog.dismiss();
                }

                @Override
                public void onAdLoaded(Ad ad) {

                    Log.d(TAG, "FB Rewarded video ad is loaded and ready to be displayed!");
                    facebookAd = true;
                    progressDialog.dismiss();
                }

                @Override
                public void onAdClicked(Ad ad) {

                    Log.d(TAG, "FB Rewarded video ad clicked!");
                }

                @Override
                public void onLoggingImpression(Ad ad) {

                    Log.d(TAG, "FB Rewarded video ad impression logged!");
                }

                @Override
                public void onRewardedVideoCompleted() {

                    Log.d(TAG, "FB Rewarded video completed!");
                    facebookAd = false;
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", serverr);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();

                }

                @Override
                public void onRewardedVideoClosed() {

                    Log.d(TAG, "FB Rewarded video ad closed!");
                }
            };
            rewardedVideoAd.loadAd(
                    rewardedVideoAd.buildLoadAdConfig()
                            .withAdListener(rewardedVideoAdListener)
                            .build());

        }

        //START APP
        else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_STR)) {
            startAppAd = new StartAppAd(getContext());


            startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
                @Override
                public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {

                    startAd = true;
                    progressDialog.dismiss();
                }

                @Override
                public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                    Log.d("StartApp Failed", ad.getErrorMessage());
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
                }
            });
        }

        //UNITY - PRELOADED IN MAINACTIVITY
        else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_UT)) {
            progressDialog.dismiss();
        }

        //APPLOVIN
        else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APV)) {

            rewardedAd = MaxRewardedAd.getInstance(WebAPI.ADMOB_REWARD_ID, getActivity());
            rewardedAd.setListener(new MaxRewardedAdListener() {
                @Override
                public void onRewardedVideoStarted(MaxAd ad) {

                }

                @Override
                public void onRewardedVideoCompleted(MaxAd ad) {
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", serverr);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();
                }

                @Override
                public void onUserRewarded(MaxAd ad, MaxReward reward) {

                }

                @Override
                public void onAdLoaded(MaxAd ad) {
                    Log.d("APPLOVINADSTATUS", " reward ad loaded");
                    progressDialog.dismiss();
                }

                @Override
                public void onAdDisplayed(MaxAd ad) {

                }

                @Override
                public void onAdHidden(MaxAd ad) {

                }

                @Override
                public void onAdClicked(MaxAd ad) {

                }

                @Override
                public void onAdLoadFailed(String adUnitId, MaxError error) {
                    Log.e("APPLOVINADSTATUS", " reward ad not loaded");
                    progressDialog.dismiss();
                }

                @Override
                public void onAdDisplayFailed(MaxAd ad, MaxError error) {

                }
            });

            rewardedAd.loadAd();
        }

        //MOPUB
        else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_MP)) {
            Toast.makeText(getActivity(), "ads Not Available Or Buy Subscription", Toast.LENGTH_SHORT).show();
/*
            MoPub.onCreate(getActivity());

            MoPubRewardedAds.loadRewardedAd(WebAPI.ADMOB_REWARD_ID);

            MoPubRewardedAdListener rewardedAdListener = new MoPubRewardedAdListener() {
                @Override
                public void onRewardedAdLoadFailure(@NonNull String s, @NonNull MoPubErrorCode moPubErrorCode) {

                }

                @Override
                public void onRewardedAdLoadSuccess(String adUnitId) {
                    // Called when the ad for the given adUnitId has loaded. At this point you should be able to call MoPubRewardedAds.showRewardedAd() to show the ad.
                    mopubAd = true;
                }

                @Override
                public void onRewardedAdStarted(String adUnitId) {
                    // Called when a rewarded ad starts playing.
                }

                @Override
                public void onRewardedAdShowError(String adUnitId, MoPubErrorCode errorCode) {
                    //  Called when there is an error while attempting to show the ad.

                    mopubAd = false;
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", serverr);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();
                }

                @Override
                public void onRewardedAdClicked(@NonNull String adUnitId) {
                    //  Called when a rewarded ad is clicked.
                }

                @Override
                public void onRewardedAdClosed(String adUnitId) {
                    // Called when a rewarded ad is closed. At this point your application should resume.
                }

                @Override
                public void onRewardedAdCompleted(Set<String> adUnitIds, MoPubReward reward) {
                    // Called when a rewarded ad is completed and the user should be rewarded.
                    // You can query the reward object with boolean isSuccessful(), String getLabel(), and int getAmount().

                    Log.d(TAG, "Mopub Rewarded video completed!");
                    mopubAd = false;
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", serverr);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();
                }
            };

            MoPubRewardedAds.setRewardedAdListener(rewardedAdListener);
*/
        } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APD)) {
            Appodeal.initialize(getActivity(), WebAPI.ADMOB_REWARD_ID, Appodeal.REWARDED_VIDEO);

            progressDialog.show();

            if(WebAPI.rewardedVideoLoaded) {
                progressDialog.dismiss();
                isRewardVideoLoaded = true;
            }


            Appodeal.setRewardedVideoCallbacks(new RewardedVideoCallbacks() {
                @Override
                public void onRewardedVideoLoaded(boolean isPrecache) {
                    // Called when rewarded video is loaded
                    isRewardVideoLoaded = true;
                    WebAPI.rewardedVideoLoaded = true;
                    progressDialog.dismiss();
                }
                @Override
                public void onRewardedVideoFailedToLoad() {
                    // Called when rewarded video failed to load
                    progressDialog.dismiss();
                    Log.v("APPODEAL", "reward not loaded");
                }
                @Override
                public void onRewardedVideoShown() {
                    // Called when rewarded video is shown
                    progressDialog.dismiss();
                    Log.v("APPODEAL", "reward shown");
                }
                @Override
                public void onRewardedVideoShowFailed() {
                    // Called when rewarded video show failed
                    progressDialog.dismiss();
                    Log.v("APPODEAL", "reward failed");
                }
                @Override
                public void onRewardedVideoClicked() {
                    // Called when rewarded video is clicked
                }
                @Override
                public void onRewardedVideoFinished(double amount, String name) {
                    // Called when rewarded video is viewed until the end
                    Log.v("APPODEAL", "reward finish");
                }
                @Override
                public void onRewardedVideoClosed(boolean finished) {
                    // Called when rewarded video is closed
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", serverr);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();

                    Log.v("APPODEAL", "reward closed");
                }
                @Override
                public void onRewardedVideoExpired() {
                    // Called when rewarded video is expired
                }
            });

        }
    }
}