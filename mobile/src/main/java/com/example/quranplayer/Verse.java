package com.example.quranplayer;

public class Verse{
    private int chapter;
    private int verseNumber;
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
        if(verseNumber < 10)
            verseNum = "00" + verseNumber;
        else if(verseNumber < 100)
            verseNum = "0" + verseNumber;
        offlineFileName = chapterNum + verseNum + ".mp3";
        url = "https://everyayah.com/data/AbdulSamad_64kbps_QuranExplorer.Com/" +
                chapterNum + verseNum + ".mp3";
    }
    public String getUrl(){
        return url;
    }
    public String getOfflineFileName() {
        return offlineFileName;
    }
}