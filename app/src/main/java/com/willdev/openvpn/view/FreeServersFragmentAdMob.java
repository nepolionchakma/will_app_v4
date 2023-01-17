package com.willdev.openvpn.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.NativeCallbacks;
import com.appodeal.ads.RewardedVideoCallbacks;
import com.willdev.openvpn.R;
import com.willdev.openvpn.adapter.FreeServerAdapter;
import com.willdev.openvpn.api.WebAPI;
import com.willdev.openvpn.model.Server;
import com.willdev.openvpn.utils.Config;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FreeServersFragmentAdMob extends Fragment implements FreeServerAdapter.OnSelectListener {
    @BindView(R.id.rcv_servers)
    RecyclerView rcvServers;

    @BindView(R.id.btnRandom)
    LinearLayout btnRandom;

    private FreeServerAdapter serverAdapter;
    private ArrayList<Server> servers;
    private ArrayList<Server> randomServers;
    private int arrLength = 0;

    private boolean isBannerLoaded = false, isRewardVideoLoaded = false;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_free_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        if(WebAPI.nativeAd == null) {
            Appodeal.initialize(getActivity(), WebAPI.ADMOB_NATIVE, Appodeal.NATIVE);

            if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APD)) {

                Appodeal.setNativeCallbacks(new NativeCallbacks() {
                    @Override
                    public void onNativeLoaded() {

                        if(!isBannerLoaded) {
                            Log.v("APPODEAL", "list LOADED");
                            List<com.appodeal.ads.NativeAd> loadedNativeAds = Appodeal.getNativeAds(1);
                            if (loadedNativeAds.isEmpty()){
                                Log.v("APPODEAL", "EMPTY");
                            }

                            WebAPI.nativeAd = loadedNativeAds.get(0);

                            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                            serverAdapter = new FreeServerAdapter(getActivity(), WebAPI.nativeAd);
                            serverAdapter.setOnSelectListener(server -> selectServer(server));
                            rcvServers.setLayoutManager(layoutManager);
                            rcvServers.setAdapter(serverAdapter);
                            loadServers();
                            isBannerLoaded = true;
                        }
                    }

                    @Override
                    public void onNativeFailedToLoad() {
                        Log.v("APPODEAL", "FAILED");

                        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                        serverAdapter = new FreeServerAdapter(getActivity(), null);
                        serverAdapter.setOnSelectListener(server -> selectServer(server));
                        rcvServers.setLayoutManager(layoutManager);
                        rcvServers.setAdapter(serverAdapter);
                        loadServers();
                        isBannerLoaded = true;
                    }

                    @Override
                    public void onNativeShown(com.appodeal.ads.NativeAd nativeAd) {
                        Log.v("APPODEAL", "shown");
                    }

                    @Override
                    public void onNativeShowFailed(com.appodeal.ads.NativeAd nativeAd) {
                        Log.v("APPODEAL", "NOT SHOWN");
                    }

                    @Override
                    public void onNativeClicked(com.appodeal.ads.NativeAd nativeAd) {
                        Log.v("APPODEAL", "clicked");
                    }

                    @Override
                    public void onNativeExpired() {
                        Log.v("APPODEAL", "expired");
                    }
                });
            } else {
                LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                serverAdapter = new FreeServerAdapter(getActivity(), null);
                serverAdapter.setOnSelectListener(this);
                rcvServers.setLayoutManager(layoutManager);
                rcvServers.setAdapter(serverAdapter);
                loadServers();
            }
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            serverAdapter = new FreeServerAdapter(getActivity(), WebAPI.nativeAd);
            serverAdapter.setOnSelectListener(server -> selectServer(server));
            rcvServers.setLayoutManager(layoutManager);
            rcvServers.setAdapter(serverAdapter);
            loadServers();
            isBannerLoaded = true;
        }

        btnRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                randomServer();
            }
        });

    }

    private void loadServers() {
        servers = new ArrayList<>();
        randomServers = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(WebAPI.FREE_SERVERS);

            arrLength = jsonArray.length() - 1;

            for (int i=0; i < jsonArray.length();i++){
                JSONObject object = (JSONObject) jsonArray.get(i);
                servers.add(new Server(object.getString("serverName"),
                        object.getString("flagURL"),
                        object.getString("ovpnConfiguration"),
                        object.getString("vpnUserName"),
                        object.getString("vpnPassword")
                ));

                randomServers.add(new Server(object.getString("serverName"),
                        object.getString("flagURL"),
                        object.getString("ovpnConfiguration"),
                        object.getString("vpnUserName"),
                        object.getString("vpnPassword")
                ));

                Log.v("Servers",object.getString("vpnUserName"));
                Log.v("Servers",object.getString("vpnPassword"));
                Log.v("Servers",object.getString("serverName"));
                Log.v("Servers",object.getString("flagURL"));
                Log.v("Servers",object.getString("ovpnConfiguration"));
                if((i % 2 == 0)&&(i > 0)){
                    if (!Config.vip_subscription && !Config.all_subscription) {
                        servers.add(null);
                    }
                }
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

    @Override
    public void onSelected(Server server) {
        selectServer(server);
    }

    public void randomServer(){

        int random = new Random().nextInt(arrLength);

        Intent mIntent = new Intent();
        mIntent.putExtra("server", randomServers.get(random));
        getActivity().setResult(getActivity().RESULT_OK, mIntent);
        getActivity().finish();
    }

    private void selectServer(Server server) {
        if (getActivity() != null)
        {
            if (Config.vip_subscription || Config.all_subscription) {
                Intent mIntent = new Intent();
                mIntent.putExtra("server", server);
                getActivity().setResult(getActivity().RESULT_OK, mIntent);
                getActivity().finish();

            } else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_STR)) {

                StartAppAd startAppAd = new StartAppAd(getContext());

                startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
                    @Override
                    public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {

                        startAppAd.showAd();

                        Intent mIntent = new Intent();
                        mIntent.putExtra("server", server);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();

                        startAppAd.setVideoListener(new VideoListener() {
                            @Override
                            public void onVideoCompleted() {
                                Log.d("STARTAPP", "Rewarded video completed!");

                            }
                        });
                    }

                    @Override
                    public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                        //Toast.makeText(mContext, "Ad Not Available or Buy Subscription", Toast.LENGTH_SHORT).show();
                        Log.d("STARTAPP", "StartApp Failed, " + ad.getErrorMessage());

                        Intent mIntent = new Intent();
                        mIntent.putExtra("server", server);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();
                    }
                });
            } else if(WebAPI.ADS_TYPE.equals(WebAPI.TYPE_UT)) {

                if (UnityAds.isReady (WebAPI.ADMOB_REWARD_ID)) {
                    UnityAds.show (getActivity(), WebAPI.ADMOB_REWARD_ID);

                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", server);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();

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
                                Log.v("UNITY", "Reward ad finished");
                            }
                        }

                        @Override
                        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

                            if(s.equals(WebAPI.ADMOB_REWARD_ID)) {
                                Log.e("UNITY", "Reward ad error, " + unityAdsError.toString());
                            }
                        }
                    });
                } else {
                    Intent mIntent = new Intent();
                    mIntent.putExtra("server", server);
                    getActivity().setResult(getActivity().RESULT_OK, mIntent);
                    getActivity().finish();
                }

            } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APV)) {

                MaxRewardedAd rewardedAd = MaxRewardedAd.getInstance(WebAPI.ADMOB_REWARD_ID, getActivity());
                rewardedAd.setListener(new MaxRewardedAdListener() {
                    @Override
                    public void onRewardedVideoStarted(MaxAd ad) {

                    }

                    @Override
                    public void onRewardedVideoCompleted(MaxAd ad) {
                        Intent mIntent = new Intent();
                        mIntent.putExtra("server", server);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();
                    }

                    @Override
                    public void onUserRewarded(MaxAd ad, MaxReward reward) {

                    }

                    @Override
                    public void onAdLoaded(MaxAd ad) {
                        rewardedAd.showAd();
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
                        Intent mIntent = new Intent();
                        mIntent.putExtra("server", server);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();
                    }

                    @Override
                    public void onAdDisplayFailed(MaxAd ad, MaxError error) {

                    }
                });

                rewardedAd.loadAd();

            } else if (WebAPI.ADS_TYPE.equals(WebAPI.TYPE_APD)) {

                if(!WebAPI.rewardedVideoLoaded) {
                    Appodeal.initialize(getActivity(), WebAPI.ADMOB_REWARD_ID, Appodeal.REWARDED_VIDEO);
                    WebAPI.rewardedVideoLoaded = true;
                    progressDialog.show();
                } else {
                    Appodeal.show(getActivity(), Appodeal.REWARDED_VIDEO);
                    isRewardVideoLoaded = true;
                }

                Appodeal.setRewardedVideoCallbacks(new RewardedVideoCallbacks() {
                    @Override
                    public void onRewardedVideoLoaded(boolean isPrecache) {
                        // Called when rewarded video is loaded
                        if(!isRewardVideoLoaded) {
                            Log.v("APPODEAL", "reward loaded");
                            Appodeal.show(getActivity(), Appodeal.REWARDED_VIDEO);
                            isRewardVideoLoaded = true;
                        }

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
                        mIntent.putExtra("server", server);
                        getActivity().setResult(getActivity().RESULT_OK, mIntent);
                        getActivity().finish();

                        Log.v("APPODEAL", "reward closed");
                    }
                    @Override
                    public void onRewardedVideoExpired() {
                        // Called when rewarded video is expired
                    }
                });

            } else {

                Intent mIntent = new Intent();
                mIntent.putExtra("server", server);
                getActivity().setResult(getActivity().RESULT_OK, mIntent);
                getActivity().finish();
            }
        }
    }
}