package com.example.quranplayer;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputMethodSession;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quranplayer.shared.Quran;
import com.example.quranplayer.shared.Verse;

import java.util.ArrayList;

public class MainAutoActivity extends AppCompatActivity implements InputMethodSession{
    private MediaPlayer mp = new MediaPlayer();
    private boolean resume = true;//by default we assume that once a track is set, it should play
    private static int chapter = -1;//use negative one to confidentally conclude no valid chapter/verse has been given yet.
    private static int verse = -1;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private Button btn;
    private SpeechRecognizer speechRecognizer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void finishInput() {

    }

    @Override
    public void updateSelection(int i, int i1, int i2, int i3, int i4, int i5) {

    }

    @Override
    public void viewClicked(boolean b) {

    }

    @Override
    public void updateCursor(Rect rect) {

    }

    @Override
    public void displayCompletions(CompletionInfo[] completionInfos) {

    }

    @Override
    public void updateExtractedText(int i, ExtractedText extractedText) {

    }

    @Override
    public void dispatchKeyEvent(int i, KeyEvent event, InputMethodSession.EventCallback c){
        if(event.getAction() == event.ACTION_DOWN) {
            if(event.getKeyCode() == event.KEYCODE_MEDIA_PLAY_PAUSE) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle bundle) {

                    }

                    @Override
                    public void onBeginningOfSpeech() {

                    }

                    @Override
                    public void onRmsChanged(float v) {

                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {

                    }

                    @Override
                    public void onEndOfSpeech() {

                    }

                    @Override
                    public void onError(int i) {

                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        String userInput = data.get(0);
                        //check if mediaplayer is paused but user wants to resume
                        if(!mp.isPlaying()){
                            if(userInput.contains("Resume") || userInput.contains("resume")){
                                playTrack();
                            }
                        }
                        //check if the mediaplayer is currently in a playing state
                        if(mp.isPlaying()){
                            //if the request was to pause, then pause
                            if(userInput.contains("Pause") || userInput.contains("pause"))
                                resume = false;
                            //we assume that this "pause" is all this listening event is supposed to do
                            //return out of function to not cause unintended behavior.
                            return;
                        }
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

                    @Override
                    public void onPartialResults(Bundle bundle) {

                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {

                    }
                });

            }
        }
    }
    public void onResults(){

    }
    @Override
    public void dispatchTrackballEvent(int i, MotionEvent motionEvent, InputMethodSession.EventCallback eventCallback) {

    }

    @Override
    public void dispatchGenericMotionEvent(int i, MotionEvent motionEvent, InputMethodSession.EventCallback eventCallback) {

    }

    @Override
    public void appPrivateCommand(String s, Bundle bundle) {

    }

    @Override
    public void toggleSoftInput(int i, int i1) {

    }

    @Override
    public void updateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
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
            //calculate what the next track will be
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
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //recursively play the next verse with the updated chapter, verse numbers
                    if(resume)
                        playTrack();
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