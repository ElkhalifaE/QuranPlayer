package com.EarthCustodian.quranplayer.shared;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 *
 * <ul>
 *
 * <li> Extend {@link MediaBrowserServiceCompat}, implementing the media browsing
 *      related methods {@link MediaBrowserServiceCompat#onGetRoot} and
 *      {@link MediaBrowserServiceCompat#onLoadChildren};
 * <li> In onCreate, start a new {@link MediaSessionCompat} and notify its parent
 *      with the session"s token {@link MediaBrowserServiceCompat#setSessionToken};
 *
 * <li> Set a callback on the {@link MediaSessionCompat#setCallback(MediaSessionCompat.Callback)}.
 *      The callback will receive all the user"s actions, like play, pause, etc;
 *
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 *      {@link android.media.MediaPlayer})
 *
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 *      {@link MediaSessionCompat#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat)}
 *      {@link MediaSessionCompat#setMetadata(android.support.v4.media.MediaMetadataCompat)} and
 *      {@link MediaSessionCompat#setQueue(java.util.List)})
 *
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 *      android.media.browse.MediaBrowserService
 *
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 *
 * <ul>
 *
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
 *      an &lt;uses name="media"/&gt; element as a child.
 *      For example, in AndroidManifest.xml:
 *          &lt;meta-data android:name="com.google.android.gms.car.application"
 *              android:resource="@xml/automotive_app_desc"/&gt;
 *      And in res/values/automotive_app_desc.xml:
 *          &lt;automotiveApp&gt;
 *              &lt;uses name="media"/&gt;
 *          &lt;/automotiveApp&gt;
 *
 * </ul>
 */
public class MyMusicService extends MediaBrowserServiceCompat {
    private static final int NOTIFICATION_ID = 1337;
    public static Intent notificationIntent;
    public NotificationChannel channel;
    public static Command command = new Command(
            new Verse(1, 1), new Verse(114, 4), 1);//contains start verse, end verse, and loop times
    public static boolean resume = false;
    private static Verse current = new Verse(1, 1);
    private Verse next = new Verse(1, 2);//used for multithreading
    public static MediaPlayer mp = new MediaPlayer();
    private MediaPlayer mpE = new MediaPlayer();//Informing user of Error rather than recitation
    public MediaSessionCompat mSession;
    //Make the token public but not the session for "Encapsulation"

    //We need to create 2 methods that will be used frequently, one for resetting "current" to the new start verse
    //This will be called everytime a piece of data in the command changes due to new mic input
    public static void resetVerse(){current = command.getStart();}
    //This second method will be for assigning next to be at the correct spot given a currentVerse,
    //its a helper method that should only be needed in this context
    private void nextVerseSpot(){
        //First check if the start and end verse are same in command, this means next verse is always at start
        if(command.getStart().getLine() == command.getEnd().getLine())
            next = command.getStart();
        else{
            //Now check 3 conditions, if current is at the end spot, check if current is at end of respective chapter
            //and check if its anywhere else
            if(current.getLine() == command.getEnd().getLine()){
                //if it is at the end, this means next should be at the start (for looping
                next = command.getStart();
            }
            else if(current.getVerseNumber() == Quran.verselookup.get(current.getChapterNumber())){
                //if it is at the end of chapter, this means next should be at the following chapter
                next = new Verse(current.getChapterNumber() + 1, 1);
            }
            else{
                //it is in a normal spot of chapter, just get the next verse of the same chapter
                next = new Verse(current.getChapterNumber(), current.getVerseNumber() + 1);
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        channel = new NotificationChannel("CHANNEL_ID", "Channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Required to do channel, but I dont intend to update the notification");
        NotificationManager nM = getSystemService(NotificationManager.class);
        nM.createNotificationChannel(channel);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder foregroundNotification = new NotificationCompat.Builder(this, "CHANNEL_ID");
        foregroundNotification.setOngoing(true);
        Notification notification = foregroundNotification
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setContentTitle("My Awesome App")
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mSession = new MediaSessionCompat(getApplicationContext(), "MyMusicService", mediaButtonReceiver, null);

        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        /*
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mSession.setMediaButtonReceiver(pendingIntent);*/
        setSessionToken(mSession.getSessionToken());
    }
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
        result.sendResult(new ArrayList<MediaItem>());
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback{

        @Override
        public void onPlay() {
            if(command.getStart().getLine() == current.getLine())
                playFirst();
            else
                helpPlay();
        }
        private void playFirst(){
            try{
                //since this is first, we know that current verse starts at Start Verse
                current = command.getStart();
                nextVerseSpot();
                mp = new MediaPlayer();
                mp.setDataSource(current.getUrl());
                mp.prepare();
                mp.start();
            }
            catch(Exception e){
                //issue finding data source, this shouldnt ever happen
                System.out.println("Issue finding data source");
            }
            //Assuming it found it and started playing, we can inform the user what is happening
            //This can be done by editing the hardcoded string value, so that the UI updates even though
            //from the MusicService we dont have access to the UI

            //Use serializable data to "broadcast" to my mediaController to successfully change the text of UI element
            Bundle bundle = new Bundle();
            bundle.putSerializable("0", "Now playing Chapter " +  current.getChapterNumber() + " verse " + current.getVerseNumber() + "! ");
            mSession.sendSessionEvent("changeNowPlayingTextToPlay", bundle);

            //Assuming all this succeeded, now have a listener that onCompletion will call helpPlay()
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                @Override
                public void onCompletion(MediaPlayer mp) {
                    current = next;
                    if(resume){
                        helpPlay();
                    }
                    else{
                        //We want to inform the user where they paused.
                        //Note that we are using next rather than current
                        Bundle bundle2 = new Bundle();
                        bundle2.putSerializable("0", "Resume to play chapter " + next.getChapterNumber() + " verse " + next.getVerseNumber() + "!");
                        mSession.sendSessionEvent("ChangeNowPlayingTextToCompletedPause", bundle2);
                    }
                }
            });
        }
        private void helpPlay(){
            //if we are at end, we have to know the loop counter to know if we play from beginning again or not
            if(current.getLine() == command.getEnd().getLine()){
                //if loop has not been decremented to 1 yet. We should start the next loop
                if(command.getLoop() > 1){
                    command.decrementLoop();
                }
                else{
                    //media finished, bring all the values back to initial
                    Bundle bundle1 = new Bundle();
                    bundle1.putSerializable("0", "Finished Playing!");
                    mSession.sendSessionEvent("changeNowPlayingTextToFinished", bundle1);
                    command.resetLoop();
                    return;//make sure to stop playing at this point
                }
            }
            //on the completion of a verse, the next verse in sequence should be
            //figured out so we must use logic to get where the next track will be
            nextVerseSpot();
            try{
                mp = new MediaPlayer();
                mp.setDataSource(current.getUrl());
                mp.prepare();
                mp.start();
            }
            catch(Exception e){
                //issue finding data source, this shouldnt ever happen
                System.out.println("Issue finding data source");
            }
            //Use serializable data to "broadcast" to my mediaController to successfully change the text of UI element
            Bundle bundle = new Bundle();
            bundle.putSerializable("0", "Now playing Chapter " +  current.getChapterNumber() + " verse " + current.getVerseNumber() + "! ");
            mSession.sendSessionEvent("changeNowPlayingTextToPlay", bundle);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    current = next;
                    if(resume){
                        helpPlay();
                    }
                    else{
                        //We want to inform the user where they paused.
                        //Note that we are using next rather than current
                        Bundle bundle2 = new Bundle();
                        bundle2.putSerializable("0", "Resume to play chapter " + next.getChapterNumber() + " verse " + next.getVerseNumber() + "!");
                        mSession.sendSessionEvent("ChangeNowPlayingTextToCompletedPause", bundle2);
                    }
                }
            });
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
        }

        @Override
        public void onSeekTo(long position) {
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        @Override
        public void onPause() {
            //Use serializable data to "broadcast" to my mediaController to successfully change the text of UI element
            Bundle bundle = new Bundle();
            bundle.putSerializable("0", "Pausing recitation after completing this verse!");
            mSession.sendSessionEvent("changeNowPlayingTextToPause", bundle);

        }

        @Override
        public void onStop() {
            MyMusicService.resume = false;
            mSession.release();
            ServiceCompat.stopForeground(MyMusicService.this,ServiceCompat.STOP_FOREGROUND_REMOVE);
        }

        @Override
        public void onSkipToNext() {
        }
        @Override
        public void onSkipToPrevious() {
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            try{
                mpE = new MediaPlayer();
                mpE.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                if(action.equals("Play Response for bad start")){
                    /*
                    MainActivity.sCV.setText("ERR");
                    MainActivity.sVV.setText("ERR");*/
                    Verse temp = command.getStart();
                    //we want to check which code went wrong
                    if(temp.getChapterNumber() == -1){//if -1, no keyword "chapter" and no keyword "verse"
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertInvalidInput.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getChapterNumber() == -2){//if -2, no number was detected
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertNeededNumber.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getChapterNumber() == -3){//if -3, chapter doesnt exist in range
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertChapterRange.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getVerseNumber() == -2){//
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertVerseRange.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else{//getVerseNumber() == -3, means that end chapter is before beginning chapter
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertBadOrderVerse.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                }
                else if(action.equals("Play Response for bad end")){
                    /*
                    MainActivity.eCV.setText("ERR");
                    MainActivity.eVV.setText("ERR");*/
                    Verse temp = command.getEnd();
                    //we want to check which code went wrong
                    if(temp.getChapterNumber() == -1){//if -1, no keyword "chapter" and no keyword "verse"
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertInvalidInput.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getChapterNumber() == -2){//if -2, no number was detected
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertNeededNumber.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getChapterNumber() == -3){//if -3, chapter doesnt exist in range
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertChapterRange.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else if(temp.getVerseNumber() == -2){//
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertVerseRange.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                    else{//getVerseNumber() == -3, means that end chapter is before beginning chapter
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertBadOrderVerse.wav");
                        mpE.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mpE.prepare();
                        mpE.start();
                    }
                }
                else{//Assume that this is handling for an invalid loop
                    //use the values in command to figure out if an error occured
                    /*
                    MainActivity.lV.setText("ERR");*/
                    if(command.getLoop() == -1){
                        //then the error that happened was a number wasnt found
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertNeededNumberForLoop.wav");
                        mpE.setDataSource(new AssetFileDescriptor(afd.getParcelFileDescriptor(), afd.getStartOffset(), afd.getLength()));
                        mpE.prepare();
                        mpE.start();
                    }
                    else{
                        //the code was -2 and therefore it was less than 1 loop or it was over 1000
                        AssetFileDescriptor afd = getResources().getAssets().openFd("alertLoopRange.wav");
                        mpE.setDataSource(new AssetFileDescriptor(afd.getParcelFileDescriptor(), afd.getStartOffset(), afd.getLength()));
                        mpE.prepare();
                        mpE.start();
                    }
                }
            }
            catch(Exception e){
                System.out.println("Not supposed to fail");
            }
            mpE.release();
            mpE = null;
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
        }
    }
}