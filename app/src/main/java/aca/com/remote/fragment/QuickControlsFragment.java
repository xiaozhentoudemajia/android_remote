package aca.com.remote.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import aca.com.magicasakura.utils.ThemeUtils;
import aca.com.magicasakura.widgets.TintImageView;
import aca.com.magicasakura.widgets.TintProgressBar;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import aca.com.nanohttpd.HttpService;
import aca.com.remote.MainApplication;
import aca.com.remote.R;
import aca.com.remote.activity.PlayingActivity;
import aca.com.remote.handler.HandlerUtil;
import aca.com.remote.service.MusicPlayer;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.daap.Status;

public class QuickControlsFragment extends BaseFragment {


    private TintProgressBar mProgress;
    public Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {

            long position = MusicPlayer.position();
            long duration = MusicPlayer.duration();
            if (duration > 0 && duration < 627080716){
                mProgress.setProgress((int) (1000 * position / duration));
            }

            if (MusicPlayer.isPlaying()) {
                mProgress.postDelayed(mUpdateProgress, 50);
            }else {
                mProgress.removeCallbacks(mUpdateProgress);
            }

        }
    };
    private TintImageView mPlayPause;
    private TextView mTitle;
    private TextView mArtist;
    private SimpleDraweeView mAlbumArt;
    private View rootView;
    private ImageView playQueue, next;
    private String TAG = "QuickControlsFragment";
    private static QuickControlsFragment fragment;
    private Session mSession;
    private HttpService.httpBinder binder;
    private static Status status;
    protected boolean dragging = false;

    public static QuickControlsFragment newInstance() {
        return new QuickControlsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_nav, container, false);
        this.rootView = rootView;
        mPlayPause = (TintImageView) rootView.findViewById(R.id.control);
        mProgress = (TintProgressBar) rootView.findViewById(R.id.song_progress_normal);
        mTitle = (TextView) rootView.findViewById(R.id.playbar_info);
        mArtist = (TextView) rootView.findViewById(R.id.playbar_singer);
        mAlbumArt = (SimpleDraweeView) rootView.findViewById(R.id.playbar_img);
        next = (ImageView) rootView.findViewById(R.id.play_next);
        playQueue = (ImageView) rootView.findViewById(R.id.play_list);

        mProgress.setProgressTintList(ThemeUtils.getThemeColorStateList(mContext, R.color.theme_color_primary));
        mProgress.postDelayed(mUpdateProgress,0);
        this.mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                dragging = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                dragging = false;
                if (mSession == null || seekBar == null || status == null) {
                    return;
                }

                // scan to location in song
                mSession.controlProgress(seekBar.getProgress());
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*wwj

                mPlayPause.setImageResource(MusicPlayer.isPlaying() ? R.drawable.playbar_btn_pause
                        : R.drawable.playbar_btn_play);
                mPlayPause.setImageTintList(R.color.theme_color_primary);

                if (MusicPlayer.getQueueSize() == 0) {
                    Toast.makeText(MainApplication.context, getResources().getString(R.string.queue_is_empty),
                            Toast.LENGTH_SHORT).show();
                } else {
                    HandlerUtil.getInstance(MainApplication.context).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MusicPlayer.playOrPause();
                        }
                    }, 60);
                }
                 */
                if (mSession != null) {
                    if (status.getPlayStatus() == Status.STATE_PLAYING) {
                        mSession.controlPause();
                    } else {
                        mSession.controlPlay();
                    }
                } else {
                    mPlayPause.setImageResource(MusicPlayer.isPlaying() ? R.drawable.playbar_btn_pause
                            : R.drawable.playbar_btn_play);
                    mPlayPause.setImageTintList(R.color.theme_color_primary);

                    if (MusicPlayer.getQueueSize() == 0) {
                        Toast.makeText(MainApplication.context, getResources().getString(R.string.queue_is_empty),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        HandlerUtil.getInstance(MainApplication.context).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MusicPlayer.playOrPause();
                            }
                        }, 60);
                    }
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MusicPlayer.next();
                    }
                }, 60);

            }
        });

        playQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PlayQueueFragment playQueueFragment = new PlayQueueFragment();
                        playQueueFragment.show(getFragmentManager(), "playqueueframent");
                    }
                }, 60);

            }
        });

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainApplication.context, PlayingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //wwj MainApplication.context.startActivity(intent);
            }
        });




        return rootView;
    }

    public void updateNowplayingCard() {
        mTitle.setText(MusicPlayer.getTrackName());
        mArtist.setText(MusicPlayer.getArtistName());
            ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                    if (imageInfo == null) {
                        return;
                    }
                    QualityInfo qualityInfo = imageInfo.getQualityInfo();
                    FLog.d("Final image received! " +
                                    "Size %d x %d",
                            "Quality level %d, good enough: %s, full quality: %s",
                            imageInfo.getWidth(),
                            imageInfo.getHeight(),
                            qualityInfo.getQuality(),
                            qualityInfo.isOfGoodEnoughQuality(),
                            qualityInfo.isOfFullQuality());
                }

                @Override
                public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
                    //FLog.d("Intermediate image received");
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    mAlbumArt.setImageURI(Uri.parse("res:/" + R.drawable.placeholder_disk_210));
                }
            };
            Uri uri = null;
            try{
                uri = Uri.parse(MusicPlayer.getAlbumPath());
            }catch (Exception e){
                e.printStackTrace();
            }
            if (uri != null) {
                ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri).build();

                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(mAlbumArt.getController())
                        .setImageRequest(request)
                        .setControllerListener(controllerListener)
                        .build();

                mAlbumArt.setController(controller);
            } else {
                mAlbumArt.setImageURI(Uri.parse("content://" + MusicPlayer.getAlbumPath()));
            }

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
        mProgress.removeCallbacks(mUpdateProgress);
    }

    @Override
    public void onResume() {
        super.onResume();
        /*wwj
        mProgress.setMax(1000);
        mProgress.removeCallbacks(mUpdateProgress);
        mProgress.postDelayed(mUpdateProgress,0);
        updateNowplayingCard();
        */
        if (mSession == null) {
            mProgress.setMax(1000);
            mProgress.removeCallbacks(mUpdateProgress);
            mProgress.postDelayed(mUpdateProgress, 0);
            updateNowplayingCard();
        } else {
            Log.i("wwj", "onResume:"+mContext.toString());
            statusUpdate.sendEmptyMessage(Status.UPDATE_TRACK);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void updateState() {
        if (MusicPlayer.isPlaying()) {
            mPlayPause.setImageResource(R.drawable.playbar_btn_pause);
            mPlayPause.setImageTintList(R.color.theme_color_primary);
            mProgress.removeCallbacks(mUpdateProgress);
            mProgress.postDelayed(mUpdateProgress,50);
        } else {
            mPlayPause.setImageResource(R.drawable.playbar_btn_play);
            mPlayPause.setImageTintList(R.color.theme_color_primary);
            mProgress.removeCallbacks(mUpdateProgress);
        }
    }


    public void updateTrackInfo() {
        updateNowplayingCard();
        updateState();
    }


    @Override
    public void changeTheme() {
        super.changeTheme();
        mProgress.setProgressTintList(ThemeUtils.getThemeColorStateList(mContext, R.color.theme_color_primary));
    }

    public void updateTrackInfo(Session session) {
        mSession = session;
        if (mSession != null) {
            status = session.singletonStatus(statusUpdate);
            status.updateHandler(statusUpdate);
            statusUpdate.sendEmptyMessage(Status.UPDATE_TRACK);
        } else {
            status.updateHandler(null);
            status = null;
        }
    }

    public void updateHttpBinder(HttpService.httpBinder ibinder) {
        binder = ibinder;
    }

    protected Handler statusUpdate = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // update gui based on severity
            switch (msg.what) {
                case Status.UPDATE_TRACK:
                    Log.i("wwj", "UPDATE_TRACK");
                    mTitle.setText(status.getTrackName());
                    mArtist.setText(status.getTrackArtist());

                case Status.UPDATE_COVER:
                    Log.i("wwj", "UPDATE_COVER");
                    if (status.coverEmpty) {
                        // fade down if no coverart
                        mAlbumArt.setImageDrawable(new ColorDrawable(Color.BLACK));
                    } else if (status.coverCache != null) {
                        // fade over to new coverart
                        Drawable one = mAlbumArt.getDrawable();
                        if (one != null) {
                            TransitionDrawable trans = new TransitionDrawable(new Drawable[] { one,
                                    new BitmapDrawable(getResources(), status.coverCache) });
                            mAlbumArt.setImageDrawable(trans);
                            trans.startTransition(1000);
                        } else {
                            mAlbumArt.setImageDrawable(new BitmapDrawable(getResources(), status.coverCache));
                        }
                        one = null;
                    }

                case Status.UPDATE_STATE:
                    Log.i("wwj", "UPDATE_STATE");
                    mPlayPause.setImageResource((status.getPlayStatus() == Status.STATE_PLAYING) ? R.drawable.playbar_btn_pause
                            : R.drawable.playbar_btn_play);
                    mPlayPause.setImageTintList(R.color.theme_color_primary);
                    mProgress.setMax(status.getProgressTotal());

                case Status.UPDATE_PROGRESS:
                    Log.i("wwj", "UPDATE_PROGRESS: "+status.getProgress());
                    if (!dragging) {
                        mProgress.setProgress(status.getProgress());
                    }
                    break;

                // This one is triggered by a thread, so should not be used to
                // update progress, etc...
                case Status.UPDATE_RATING:
                    Log.i("wwj", "UPDATE_RATING");
                    break;

                case Status.UPDATE_SPEAKERS:
                    Log.i("wwj", "UPDATE_SPEAKERS");
                    break;
            }
        }
    };
}