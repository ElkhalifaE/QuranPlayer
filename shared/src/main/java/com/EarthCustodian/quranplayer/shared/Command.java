package com.EarthCustodian.quranplayer.shared;

public class Command {

    //The command should store these attributes regardless of type of command (play, pause, resume)
    private Verse start = new Verse(0, 0);
    private Verse end = new Verse(0, 0);
    private int loopRetain;//purpose to store initial loop value due to decrementing other loop data during recitation
    private int loop;

    //The Constructor should call a helper method to
    //get the correct attributes for this instance of the command
    public Command(Verse start, Verse end, int loopRetain){
        this.start = start;
        this.end = end;
        this.loopRetain = loopRetain;
        this.loop = loopRetain;
    }
    public Verse getStart(){
        return start;
    }
    public Verse getEnd(){
        return end;
    }
    public int getLoop(){
        return loop;
    }
    public void decrementLoop(){
        //used when we have done one full loop so we keep track of loop times
        loop--;
    }
    public void resetLoop(){
        //set the loop value back to its initial state
        //this is assuming it has been decremented all the way to 1
        this.loop = this.loopRetain;
    }
    public void validateStart(String startC){
        //given the input, check if there is a number that can be parsed out of the string
        //if there is and it exists in the domain of Quran chapters, then it is a valid chapter
        String numChapter = "";
        if(startC.contains("Chapter")){
            int indexOfChapterNumber = startC.indexOf("Chapter") + 8;
            numChapter = startC.substring(indexOfChapterNumber);
        }
        else if(startC.contains("chapter")){
            int indexOfChapterNumber = startC.indexOf("chapter") + 8;
            numChapter = startC.substring(indexOfChapterNumber);
        }
        else{
            //-1 represents not detecting keyword chapter or verse
            start.setChapterNumber(-1);
            return;
        }
        //If there is no keyword verse, same logic
        if(!startC.contains("verse") && !startC.contains("Verse") && !startC.contains(":")){
            start.setChapterNumber(-1);
            return;
        }
        //given this numChapter, get the parseable number, then keep the rest of the
        //string to be used to get the verse
        String parseableChapter = "";
        if(numChapter.contains(":")){
            //then the parseableChapter is just from 0 till the ":"
            parseableChapter = numChapter.substring(0, numChapter.indexOf(":"));
        }
        else{
            //then it is until the " "
            parseableChapter = numChapter.substring(0, numChapter.indexOf(" "));
        }
        int startChapter = 0;
        try{
            startChapter = Integer.parseInt(parseableChapter);
            if(startChapter < 1 && startChapter > 114){
                //-3 represents that the chapter doesnt exist
                start.setChapterNumber(-3);
                return;
            }
            start.setChapterNumber(startChapter);
        }
        catch(Exception e){
            //-2 represents there was no number recognized in the voice input
            start.setChapterNumber(-2);
            start.setVerseNumber(-2);
            return;
        }
        //get the parseableNumber, if it is in "chapter X verse Y" and in "Chapter X:Y" notation
        String parseableVerse = "";
        if(numChapter.contains(":")){
            //then the verse is right after the index of the colon
            parseableVerse = numChapter.substring(numChapter.indexOf(":") + 1);
        }
        else{
            //then verse is 5 characters after "erse" (makes it easier then handling for capital or not capital
            parseableVerse = numChapter.substring(numChapter.indexOf("erse") + 5);
        }
        try{
            int verse = Integer.parseInt(parseableVerse);
            if(verse < 1 && verse > Quran.verselookup.get(startChapter)){
                //-3 represents that the verse doesnt exist in the chapter
                start.setVerseNumber(-2);
                return;
            }
            Verse temp = new Verse(startChapter, verse);

            start = new Verse(startChapter, verse);
            end = new Verse(startChapter, Quran.verselookup.get(startChapter));

            MyMusicService.resetVerse();
        }
        catch(Exception e){
            //-2 represents that there was no number recognized in the voice input
            start.setChapterNumber(-2);
        }
    }
    public void validateEnd(String endC){
        //given the input, check if there is a number that can be parsed out of the string
        //if there is and it exists in the domain of Quran chapters, then it is a valid chapter
        String numChapter = "";
        if(endC.contains("Chapter")){
            int indexOfChapterNumber = endC.indexOf("Chapter") + 8;
            numChapter = endC.substring(indexOfChapterNumber);
        }
        else if(endC.contains("chapter")){
            int indexOfChapterNumber = endC.indexOf("chapter") + 8;
            numChapter = endC.substring(indexOfChapterNumber);
        }
        else{
            //-1 represents not detecting keyword chapter or verse
            end.setChapterNumber(-1);
            return;
        }
        //If there is no keyword verse, same logic
        if(!endC.contains("verse") && !endC.contains("Verse") && !endC.contains(":")){
            end.setChapterNumber(-1);
            return;
        }
        //given this numChapter, get the parseable number, then keep the rest of the
        //string to be used to get the verse
        String parseableChapter = "";
        if(numChapter.contains(":")){
            //then the parseableChapter is just from 0 till the ":"
            parseableChapter = numChapter.substring(0, numChapter.indexOf(":"));
        }
        else{
            //then it is until the " "
            parseableChapter = numChapter.substring(0, numChapter.indexOf(" "));
        }
        int endChapter = 0;
        try{
            endChapter = Integer.parseInt(parseableChapter);
            if(1 < endChapter  && endChapter > 114){
                //-3 represents that the chapter doesnt exist in range
                end.setChapterNumber(-3);
                return;
            }
            end.setChapterNumber(endChapter);
        }
        catch(Exception e){
            //-2 represents there was no number recognized in the voice input
            end.setChapterNumber(-2);
            return;
        }
        //get the parseableNumber, if it is in "chapter X verse Y" and in "Chapter X:Y" notation
        String parseableVerse = "";
        if(numChapter.contains(":")){
            //then the verse is right after the index of the colon
            parseableVerse = numChapter.substring(numChapter.indexOf(":") + 1);
        }
        else{
            //then verse is 5 characters after "erse" (makes it easier then handling for capital or not capital
            parseableVerse = numChapter.substring(numChapter.indexOf("erse") + 5);
        }
        //now that I have the parseable number, I want to attempt to convert it to an int
        //handle the exception if its not a real number, and return -1 to indicate failure
        try{
            int verse = Integer.parseInt(parseableVerse);
            if(verse < 1 && verse > Quran.verselookup.get(endChapter)){
                //-2 represents that the verse doesnt exist in the chapter
                end.setVerseNumber(-2);
                return;
            }
            //we need to check if the verse is before or after start (before start indicates error)
            // using the "line" property of a Verse object
            Verse temp = new Verse(endChapter, verse);
            if(start.getLine() > temp.getLine()){
                //-3 represents that the verse can't be reached due to being before the start
                end.setVerseNumber(-3);
                return;
            }
            //it succeeded, return the chapter
            end = new Verse(endChapter, verse);
            MyMusicService.resetVerse();
        }
        catch(Exception e){
            //-2 represents that there was no number recognized in the voice input
            end.setChapterNumber(-2);
        }
    }

    public void validateLoop(String loop){
        //given the input, check if there is a number that can be parsed out of the string
        //if there is and it exists in the domain of Quran chapters, then it is a valid chapter
        String numLoop;
        if(loop.contains("Loop")){
            int indexOfVerseNumber = loop.indexOf("Loop") + 5;
            numLoop = loop.substring(indexOfVerseNumber);
        }
        else if(loop.contains("loop")){
            int indexOfVerseNumber = loop.indexOf("loop") + 5;
            numLoop = loop.substring(indexOfVerseNumber);
        }
        else{
            //assume he just gave the number, then simply get the full string
            numLoop = loop;
        }
        //Given this string we can assume that the first word from splitting by whitespace
        //as a delimiter ("5 times" for example) should be a parseable number
        String potentialNum = numLoop.split(" ")[0];
        String parseableNum = handleNum(potentialNum.trim());
        //now that I have the parseable number, I want to attempt to convert it to an int
        //handle the exception if its not a real number, and return -1 to indicate failure
        try{
            int loopTimes = Integer.parseInt(parseableNum);
            if(loopTimes < 1 && loopTimes > 1000){
                //-2 represents that looping over 1000 times or less than 1 time is not allowed
                this.loop = -2;
                this.loopRetain = -2;
                return;
            }
            //we succeeded to get a valid loop
            this.loop = loopTimes;
            this.loopRetain = loopTimes;
        }
        catch(Exception e){
            //-1 represents that there was no number recognized in the voice input
            this.loopRetain = -1;
            this.loop = -1;
        }
    }
    //This function takes a number in unparseable string form such as "zero" and converts it to parseable
    //string such as "0"
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
