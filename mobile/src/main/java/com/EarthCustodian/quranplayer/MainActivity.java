package com.EarthCustodian.quranplayer;


import static com.EarthCustodian.quranplayer.shared.MyMusicService.mp;
import static com.EarthCustodian.quranplayer.shared.MyMusicService.resume;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.EarthCustodian.quranplayer.shared.MyMusicService;
import com.EarthCustodian.quranplayer.shared.Quran;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView playingWhat;
    private TextView sCV;
    private TextView sVV;
    private TextView eCV;
    private TextView eVV;
    private TextView lV;

    private ImageButton resumePauseButton;
    private ImageButton speakButtonStart;
    private ImageButton speakButtonEnd;
    private ImageButton speakButtonLoop;

    public static final int VOICE_RECOGNITION_STARTVERSE_CODE = 1111;
    public static final int VOICE_RECOGNITION_ENDVERSE_CODE = 2222;
    public static final int VOICE_RECOGNITION_LOOP_CODE = 3333;
    public String userInput;
    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(MainActivity.this, mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
            } catch(Exception e ) {
                System.out.println("Not supposed to happen");
            }
        }
    };
    //We need another callback for changing UI elements from the mediaPlayer
    private void startVoiceRecognitionActivity(int callingButtonID) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //Allows user to take longer to begin voice command
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        if(callingButtonID == R.id.listenStart){
            //if voice recognition was began by the startButton, then invoke voice recognition
            //for a start chapter input
            startActivityForResult(intent, VOICE_RECOGNITION_STARTVERSE_CODE);
        }
        else if(callingButtonID == R.id.listenEnd){
            //if voice recognition was began by endButton, then invoke voice recognition
            //for an end chapter input
            startActivityForResult(intent, VOICE_RECOGNITION_ENDVERSE_CODE);
        }
        else{
            //we assume the invoke was began by the loopButton, assume voice recognition
            //for a loop input
            startActivityForResult(intent, VOICE_RECOGNITION_LOOP_CODE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if the listening is for a startVerse, the behavior of how we treat the read string is different
        if (requestCode == VOICE_RECOGNITION_STARTVERSE_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            userInput = matches.get(0);
            //now that I have my userInput. Validate it for startVerse
            MyMusicService.command.validateStart(userInput);

            //command should have attributes that have been validated
            //We dont play media here, including the media that is used
            //to inform if the validation failed. Use this data in MediaService to handle media playing

            //handle if the input was invalid
            if(MyMusicService.command.getStart().getChapterNumber() < 0
                    || MyMusicService.command.getStart().getVerseNumber() < 0)//then we assigned it an error code{
            {
                mMediaControllerCompat.getTransportControls().sendCustomAction("Play Response for bad start", null);
                return;//get out of here, we shouldnt be setting the text to anything
            }

            //if the input ended up being valid, we want to set the new Text values
            sCV.setText(Integer.toString(MyMusicService.command.getStart().getChapterNumber()));
            sVV.setText(Integer.toString(MyMusicService.command.getStart().getVerseNumber()));

            //Assume that the end chapter and end verse is end of start chapter
            eCV.setText(Integer.toString(MyMusicService.command.getEnd().getChapterNumber()));
            eVV.setText(Integer.toString(MyMusicService.command.getEnd().getVerseNumber()));
        }
        //if the listening is for an endVerse, the behavior of how we treat the input is different
        else if(requestCode == VOICE_RECOGNITION_ENDVERSE_CODE && resultCode == RESULT_OK){
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            userInput = matches.get(0);
            //now that I have my userInput. Validate it for startVerse
            MyMusicService.command.validateEnd(userInput);

            //command should have attributes that have been validated
            //We dont play media here, including the media that is used
            //to inform if the validation failed. Use this data in MediaService to handle media playing

            //handle if the input was invalid
            if(MyMusicService.command.getEnd().getChapterNumber() < 0
                    || MyMusicService.command.getEnd().getVerseNumber() < 0)//then we assigned it an error code{
            {
                mMediaControllerCompat.getTransportControls().sendCustomAction("Play Response for bad end", null);
                return;//get out of here, we shouldnt be setting the text to anything
            }

            //if the input ended up being valid, we want to set the new Text values
            eCV.setText(Integer.toString(MyMusicService.command.getEnd().getChapterNumber()));
            eVV.setText(Integer.toString(MyMusicService.command.getEnd().getVerseNumber()));
        }
        //if the listening is for loop, the behavior of how we treat the input is different
        else if(requestCode == VOICE_RECOGNITION_LOOP_CODE && resultCode == RESULT_OK){
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            userInput = matches.get(0);
            //now that I have my userInput. Validate it for startVerse
            MyMusicService.command.validateLoop(userInput);
            //command should have attributes that have been validated
            //We dont play media here, including the media that is used
            //to inform if the validation failed. Use this data in MediaService to handle media playing
            if(MyMusicService.command.getLoop() < 0)//then we assigned it an error code
            {
                mMediaControllerCompat.getTransportControls().sendCustomAction("Play Response for bad loop", null);
                return;
            }
            //if we reached here it means we had a valid loop input, we can change the text
            lV.setText(Integer.toString(MyMusicService.command.getLoop()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }
        }
        @Override
        public void onSessionEvent(String event, Bundle extras) {
            //super.onSessionEvent(event, extras);
            //if the event is a changeNowPlayingText event, then its simply setting the text to the content of "extras"
            if(event.equals("changeNowPlayingTextToPlay")){
                playingWhat.setText(extras.get("0").toString());
            }
            else if(event.equals("changeNowPlayingTextToPause")){
                playingWhat.setText(extras.get("0").toString());
            }
            else if(event.equals("ChangeNowPlayingTextToCompletedPause")){
                playingWhat.setText(extras.get("0").toString());
            }
            else if(event.equals("changeNowPlayingTextToFinished")){
                playingWhat.setText("Finished playing!");
            }
            else if(event.equals("ERRStart")){
                sCV.setText("ERR");
                sVV.setText("ERR");
            }
            else if(event.equals("ERREnd")){
                eCV.setText("ERR");
                eVV.setText("ERR");
            }
            else{//event is an Error of loop
                lV.setText("ERR");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speakButtonStart = (ImageButton) findViewById(R.id.listenStart);
        speakButtonStart.setOnClickListener(this);
        speakButtonEnd = (ImageButton) findViewById(R.id.listenEnd);
        speakButtonEnd.setOnClickListener(this);
        speakButtonLoop = (ImageButton) findViewById(R.id.listenLoop);
        speakButtonLoop.setOnClickListener(this);

        resumePauseButton = (ImageButton) findViewById(R.id.pauseButton);
        resumePauseButton.setOnClickListener(this);

        playingWhat = (TextView) findViewById(R.id.nowPlaying);
        sCV = (TextView) findViewById(R.id.SCV);
        sVV = (TextView) findViewById(R.id.SVV);
        eCV = (TextView) findViewById(R.id.ECV);
        eVV = (TextView) findViewById(R.id.EVV);
        lV = (TextView) findViewById(R.id.LV);

        Quran.fillTable();//important to do this early in program before the hashtable is called

        mMediaBrowserCompat = new MediaBrowserCompat(getApplicationContext(), new ComponentName(getApplicationContext(), MyMusicService.class),
                mMediaBrowserCompatConnectionCallback, getIntent().getExtras());
        mMediaBrowserCompat.connect();

        MyMusicService.notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        Intent i = new Intent(this, MyMusicService.class);
        ContextCompat.startForegroundService(this, i);
    }
    @Override
    public void onClick(View view) {
        //For all buttons, we are only changing data, we are NOT invoking media
        //data such as boolean resume, start verse, etc
        //This is the only functionality of the buttons, it is the media service
        //that will handle playing media based on the data changed by the button clicks
        //This means the click was triggered by one of the three microphone buttons
        //first check if it was in resume state, we are assuming that media is playing if resume is true
        if(view == findViewById(R.id.pauseButton)){
            //Can only be entered if the view was the pausebutton anyways
            if(resume){
                //it is in a playing state, we should set the icon to the "pause" icon
                //we should also set resume to false;
                resume = false;
                resumePauseButton.setImageIcon(Icon.createWithResource(this, android.R.drawable.ic_media_play));
                //only if media is playing, should this be accessible
                if (mp.isPlaying())
                {
                    playingWhat.setText("Pausing recitation after completing this verse!");
                    mMediaControllerCompat.getTransportControls().pause();
                }
            }
            else{
                //it is in a paused state, we should set the icon to the "resume" icon
                //we should also set resume to true
                resume = true;
                resumePauseButton.setImageIcon(Icon.createWithResource(this, android.R.drawable.ic_media_pause));
                //We only want to use the media service if we know that the media is not playing already
                if(!mp.isPlaying()){
                    mMediaControllerCompat.getTransportControls().play();
                }
            }
        }
        else{
            if(mp.isPlaying()){
                //alert the user it cant listen during recitation
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Cannot listen")
                        .setMessage("During a recitation, no input can be taken")
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            }
            else {
                //it should be able to take input. begin the "voice listening"
                //with an argument that tells us which button started the event
                if (view == findViewById(R.id.listenStart))
                    startVoiceRecognitionActivity(R.id.listenStart);
                else if (view == findViewById(R.id.listenEnd))
                    startVoiceRecognitionActivity(R.id.listenEnd);
                else
                    startVoiceRecognitionActivity(R.id.listenLoop);
            }
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaBrowserCompat.disconnect();
    }
}
