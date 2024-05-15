package com.example.quranplayer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ListView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.quranplayer.shared.Quran;
import com.example.quranplayer.shared.Verse;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MediaPlayer mp = new MediaPlayer();
    private boolean resume = false;
    private static int chapter = -1;//use negative one to confidentally conclude no valid chapter/verse has been given yet.
    private static int verse = -1;
    private ImageButton speakButton;
    private ImageButton resumePauseButton;
    private TextView playingWhat;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speakButton = (ImageButton) findViewById(R.id.listenButton);
        speakButton.setOnClickListener(this);
        resumePauseButton = (ImageButton) findViewById(R.id.pauseButton);
        resumePauseButton.setOnClickListener(this);
        playingWhat = (TextView) findViewById(R.id.nowPlaying);
        //we will assign behavior to this playingWhat textview in the playTrack() method.
    }
    public void onClick(View v) {
        if (v.getId() == R.id.listenButton) {
            if(!mp.isPlaying())//if no media playing handle this normally
            {
                //force the play button to be "on" and resume is in the true state regardless of previous states
                resumePauseButton.setImageResource(android.R.drawable.ic_media_pause);
                resume = true;
                startVoiceRecognitionActivity();
            }
            else{
                //create an alert for the user to be aware that he cant give the voice command
                //during the recitation of a verse
                new AlertDialog.Builder(this)
                        .setTitle("Can't take input during recitation")
                        .setMessage("I'm sorry. I can't take a voice input during the recitation")
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }

        else if(v.getId() == R.id.pauseButton){
            //we must check if the action we are doing is resuming or pausing.
            //This is implemented by assigning a boolean which will affect the "playTrack() loop"
            //Regardless of whether there is relevant voice input, pause/play icon change should always work
            if(resume){
                //if it was in the resume state, then we want to now "pause" and change the icon from
                //"pause" to "play".
                resumePauseButton.setImageResource(android.R.drawable.ic_media_play);
                resume = false;
                //Inform the user that it is being paused, we have to validate
                //that something is playing since this setText() is not like the ones used in playTrack()
                if(mp.isPlaying())//if it is in a state of playing, then this is relevant
                    playingWhat.setText("Pausing after completion of this ayah!");
            }
            else{
                //it was in the pause state, we want to now resume it
                //simply change boolean and set the new icon from "play" to "pause"
                resumePauseButton.setImageResource(android.R.drawable.ic_media_pause);
                resume = true;
                //if we are in this block when a recitation is not playing, then it means to move to next track
                if(!mp.isPlaying() && chapter != -1){
                    playTrack();
                }
                //if we are in this block during a recitation, it means we are in a
                //track that the user changed his mind about pausing
                if(mp.isPlaying() && chapter != -1)
                    playingWhat.setText("Now playing Chapter " + chapter + " verse " + verse + "!");
            }
        }

    }
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String userInput = matches.get(0);
            //validate this input by checking that the syntax "Chapter X verse Y" was used
            try{
                //if this is a pause command then check if track is playing,
                if(userInput.contains("Chapter")){
                    userInput = userInput.substring(userInput.indexOf("Chapter") + 7);
                }
                else if(userInput.contains("chapter")){
                    userInput = userInput.substring(userInput.indexOf("chapter") + 7);
                }
                else{
                    //Alert the user that it didn't understand the input
                    //!!!!
                    AssetFileDescriptor descriptor = getAssets().openFd("alertInvalidInput.wav");
                    mp = new MediaPlayer();
                    mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();//good convention since we dont need it past this point
                    mp.prepare();
                    mp.start();
                    return;//failed validating
                }

                if(userInput.contains("Verse")){
                    //at this point we need to get the chapter and verse number that the user intended
                    String chap = userInput.substring(0, userInput.indexOf("Verse"));// X Verse Y -> " X "
                    //convert it to a string array split by the spaces and convert it to the integer
                    chap = chap.replaceAll(" ", "");
                    chap = handleNum(chap);
                    chapter = Integer.parseInt(chap);
                    //To deal with any input after the verse number, we will split the string by space
                    //and only use the element right after "verse" to represent the number we are interested in
                    userInput = userInput.trim();
                    verse = Integer.parseInt(userInput.split(" ")[2]);
                }
                else if(userInput.contains("verse")){
                    //at this point we need to get the chapter and verse number that the user intended
                    String chap = userInput.substring(0, userInput.indexOf("verse"));// X Verse Y -> " X "
                    //remove the spaces and convert to Integer
                    chap = chap.replaceAll(" ", "");
                    chap = handleNum(chap);
                    chapter = Integer.parseInt(chap);
                    //To deal with any input after the verse number, we will split the string by space
                    //and only use the element right after "verse" to represent the number we are interested in
                    userInput = userInput.trim();
                    verse = Integer.parseInt(userInput.split(" ")[2]);
                }
                else if(userInput.contains(":")){
                    //sometimes google voice will use this syntax "Chapter X:Y"
                    String chap = userInput.substring(0, userInput.indexOf(":"));
                    //remove spaces and convert to Integer
                    chap = chap.replaceAll(" ", "");
                    chap = handleNum(chap);
                    chapter = Integer.parseInt(chap);
                    //To deal with any input after verse number, only take that first character to parse into Int
                    //all elements after are not of interest.
                    userInput = userInput.trim();
                    String verseNotParsed = userInput.substring(userInput.indexOf(":") + 1);
                    verse = Integer.parseInt(verseNotParsed.substring(0, 1));
                }
                else{
                    //Alert the user that it didn't understand the input
                    AssetFileDescriptor descriptor = getAssets().openFd("alertInvalidInput.wav");
                    mp = new MediaPlayer();
                    mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();//good convention since we dont need it past this point
                    mp.prepare();
                    mp.start();
                    return;//failed validating
                }
            }
            catch(Exception e){
                //Assuming that we tried parsing an integer where the parse failed
                //We should alert the user that he needs to try again
                try{
                    AssetFileDescriptor descriptor = getAssets().openFd("alertNeededNumber.wav");
                    mp = new MediaPlayer();
                    mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();//good convention since we dont need it past this point
                    mp.prepare();
                    mp.start();
                }
                catch(Exception f){
                    System.out.println("Issue getting media");
                }
                return;
            }
            //now that we have the character and verse. Validate the numbers are valid
            //this requires a try catch block since on invalid input we may be unable to
            //play the appropriate "media" response
            try{
                if(chapter < 1 || chapter > 114){
                    //Alert user that a chapter must be in range [1, 114]
                    AssetFileDescriptor descriptor = getAssets().openFd("alertChapterRange.wav");
                    mp = new MediaPlayer();
                    mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();//good convention since we dont need it past this point
                    mp.prepare();
                    mp.start();
                    return;
                }
                //check lookup table (uses hashing) if the verse number is smaller than or
                //equal to number of verses for that chapter
                Quran.fillTable();
                if(verse < 1 || verse > Quran.verselookup.get(chapter)){
                    //Alert user that this verse number doesnt exist in this chapter
                    AssetFileDescriptor descriptor = getAssets().openFd("alertVerseRange.wav");
                    mp = new MediaPlayer();
                    mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                    descriptor.close();//good convention since we dont need it past this point
                    mp.prepare();
                    mp.start();
                    return;
                }
            }
            catch(Exception e){
                System.out.println("Failed to play media");
            }
            //because we reached this point in the handler without issue we
            //will play track until finishes Quran or is "paused"
            playTrack();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void playTrack(){
        try{
            resume = true;
            //Validated, now get our media player to play the verse
            Verse theVerse = new Verse(chapter, verse);
            //AssetFileDescriptor afd = getAssets().openFd(theVerse.getUrl());
            //mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp = new MediaPlayer();
            mp.setDataSource(theVerse.getUrl());
            mp.prepare();
            mp.start();
            playingWhat.setText("Now playing Chapter " + chapter + " verse " + verse + "! ");
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //on the completion of a verse, the next verse in sequence should be
                    //figured out so we must use logic to get where the next track will be
                    if(chapter == 114 && verse == Quran.verselookup.get(chapter)){
                        //this means we finished the Quran, get out of the function
                        return;
                    }
                    else if(verse == Quran.verselookup.get(chapter)){
                        //that means we finished the chapter
                        chapter++;
                        verse = 1;
                    }
                    else{
                        //all that happened was a verse got completed
                        verse++;
                    }
                    //recursively play the next verse with the updated chapter, verse numbers
                    if(resume)
                        playTrack();
                    else{
                        //We want to inform the user where they stopped.
                        playingWhat.setText("Resume to play chapter " + chapter + " verse " + verse + "!");
                    }
                }
            });
        }
        catch(Exception e){
            //print play error
            System.out.println("Error playing track at chapter: " + chapter + " verse: " + verse);
        }

    }
    public String handleNum(String arg){
        switch(arg){
            case "zero":
                arg = "0";
                break;
            case "one":
                arg = "1";
                break;
            case "two":
                arg = "2";
                break;
            case "three":
                arg = "3";
                break;
            case "four":
                arg = "4";
                break;
            case "five":
                arg = "5";
                break;
            case "six":
                arg = "6";
                break;
            case "seven":
                arg = "7";
                break;
            case "eight":
                arg = "8";
                break;
            case "nine":
                arg = "2";
                break;
            default://do nothing, we want the numeral to stay the same
                break;
        }
        return arg;
    }
}