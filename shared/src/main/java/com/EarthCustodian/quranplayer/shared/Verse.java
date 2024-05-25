package com.EarthCustodian.quranplayer.shared;

public class Verse{
    private int chapter;
    private int verseNumber;
    private int line;//represents verse of Quran rather than verse of Chapter (ch 2 v 1 is line 8)
    private String offlineFileName;
    private String url;
    public Verse(int c, int v){
        this.chapter = c;
        this.verseNumber = v;
        String chapterNum = "";
        String verseNum = "";
        //the syntax is "00X" for chapters and verses less than 10, "0XX" for less than 100
        if(chapter < 10)
            chapterNum = "00" + chapter;
        else if(chapter < 100)
            chapterNum = "0" + chapter;
        else
            chapterNum = "" + chapter;
        if(verseNumber < 10)
            verseNum = "00" + verseNumber;
        else if(verseNumber < 100)
            verseNum = "0" + verseNumber;
        else{
            verseNum = "" + verseNumber;
        }
        offlineFileName = chapterNum + verseNum + ".mp3";
        url = "https://everyayah.com/data/Alafasy_128kbps/" +
                chapterNum + verseNum + ".mp3";
        //finally, use a loop to instantiate what line we are on
        line = 0;
        for(int ch = 1; ch < c; ch++)
            line += Quran.verselookup.get(ch);
        //finally add the number of verses of wherever we are in the chapter
        line += verseNumber;
    }
    public int getLine(){return line;}
    public String getUrl(){
        return url;
    }
    public void setUrl(String url){this.url = url;}
    public String getOfflineFileName() {
        return offlineFileName;
    }

    public void setOfflineFileName(String offlineName){this.offlineFileName = offlineName;}

    public int getChapterNumber(){ return chapter;}
    public int getVerseNumber(){ return verseNumber;}

    public void setChapterNumber(int chapter) { this.chapter = chapter;}
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber;}

}