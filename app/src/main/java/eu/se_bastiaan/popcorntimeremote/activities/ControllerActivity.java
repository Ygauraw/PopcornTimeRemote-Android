package eu.se_bastiaan.popcorntimeremote.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.gson.internal.LinkedTreeMap;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.future.ResponseFuture;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import eu.se_bastiaan.popcorntimeremote.R;
import eu.se_bastiaan.popcorntimeremote.fragments.ConnectionLostFragment;
import eu.se_bastiaan.popcorntimeremote.fragments.JoystickMainControllerFragment;
import eu.se_bastiaan.popcorntimeremote.fragments.JoystickMovieControllerFragment;
import eu.se_bastiaan.popcorntimeremote.fragments.JoystickPlayerControllerFragment;
import eu.se_bastiaan.popcorntimeremote.fragments.JoystickSeriesControllerFragment;
import eu.se_bastiaan.popcorntimeremote.fragments.SubtitleSelectorDialogFragment;
import eu.se_bastiaan.popcorntimeremote.models.Instance;
import eu.se_bastiaan.popcorntimeremote.network.PopcornTimeRpcClient;

public class ControllerActivity extends ActionBarActivity {

    private Bundle mExtras;
    private PopcornTimeRpcClient mRpc;
    private Handler mHandler = new Handler();
    private String mCurrentFragment;
    private ResponseFuture<PopcornTimeRpcClient.RpcResponse> mViewstackFuture;

    @InjectView(R.id.progressBar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_framelayout);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        mExtras = intent.getExtras();

        if(mExtras != null && mExtras.containsKey("instance")) {
            Instance instance = (Instance) mExtras.get("instance");
            mRpc = new PopcornTimeRpcClient(this, instance.ip, instance.port, instance.username, instance.password);
            getSupportActionBar().setTitle(getString(R.string.app_name) + ": " + instance.name);
        } else {
            finish();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_logo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        runViewstackRunnable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportFragmentManager().popBackStack();
        if(mViewstackFuture != null)
            mViewstackFuture.cancel(true);
        mHandler.removeCallbacksAndMessages(null);
    }

    public void setFragment(Fragment fragment, boolean fade) {
        try {
            progressBar.setVisibility(View.GONE);

            SubtitleSelectorDialogFragment subsFragment = (SubtitleSelectorDialogFragment) getSupportFragmentManager().findFragmentByTag("subtitle_fragment");
            if (subsFragment != null) subsFragment.dismiss();

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (fade)
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            fragmentTransaction.replace(R.id.frameLayout, fragment);
            fragmentTransaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PopcornTimeRpcClient getClient() {
        if(mRpc == null && mExtras != null) mRpc = new PopcornTimeRpcClient(this, mExtras.getString(Instance.COLUMN_NAME_IP), mExtras.getString(Instance.COLUMN_NAME_PORT), mExtras.getString(Instance.COLUMN_NAME_USERNAME), mExtras.getString(Instance.COLUMN_NAME_PASSWORD));
        return mRpc;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private Runnable mGetViewstackRunnable = new Runnable() {
        @Override
        public void run() {
        mViewstackFuture = mRpc.getViewstack(new FutureCallback<PopcornTimeRpcClient.RpcResponse>() {
            @Override
            public void onCompleted(Exception e, PopcornTimeRpcClient.RpcResponse result) {
                if (e == null && result != null && result.result != null) {
                    LinkedTreeMap<String, Object> map = result.getMapResult();
                    if(map.containsKey("viewstack")) {
                        ArrayList<String> resultList = (ArrayList<String>) map.get("viewstack");
                        String topView = resultList.get(resultList.size() - 1);

                        if (topView.equals("player") && (mCurrentFragment == null || !mCurrentFragment.equals("player"))) {
                            setFragment(new JoystickPlayerControllerFragment(), true);
                            mCurrentFragment = topView;
                        } else if (topView.equals("shows-container-contain") && (mCurrentFragment == null || !mCurrentFragment.equals("shows-container-contain"))) {
                            setFragment(new JoystickSeriesControllerFragment(), true);
                            mCurrentFragment = topView;
                        } else if (topView.equals("movie-detail") && (mCurrentFragment == null || !mCurrentFragment.equals("movie-detail"))) {
                            setFragment(new JoystickMovieControllerFragment(), true);
                            mCurrentFragment = topView;
                        } else if (!(topView.equals("player") || topView.equals("shows-container-contain") || topView.equals("movie-detail")) && (mCurrentFragment == null || !mCurrentFragment.equals("main"))) {
                            setFragment(new JoystickMainControllerFragment(), true);
                            mCurrentFragment = "main";
                        }
                    }

                    mHandler.postDelayed(mGetViewstackRunnable, 500);
                } else if (e != null) {
                    e.printStackTrace();
                    setFragment(new ConnectionLostFragment(), true);
                    mCurrentFragment = "no-connection";
                }
            }
        });
        }
    };

    public void runViewstackRunnable() {
        try {
            getSupportFragmentManager().popBackStack();
            mRpc.ping(new FutureCallback<PopcornTimeRpcClient.RpcResponse>() {
                @Override
                public void onCompleted(Exception e, PopcornTimeRpcClient.RpcResponse result) {
                    if (e == null) {
                        mGetViewstackRunnable.run();
                    } else {
                        e.printStackTrace();
                        setFragment(new ConnectionLostFragment(), true);
                        mCurrentFragment = "no-connection";
                    }
                }
            });
        } catch (Exception e) { }
    }
}
