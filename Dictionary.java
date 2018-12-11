import java.io.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
public class Dictionary{
    private final String filename = "words.txt";
    private ArrayList<String> randomWords;
    private String answer;

    public Dictionary(){
        readFile();
    }

    // reads the contents of the file
    public void readFile(){
        try{
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            int i=0, randomIndex=1, numberOfWords;
            int[] wordIndexArr = new int[3];
            this.randomWords = new ArrayList<String>();
            
            //read first line in file and store it as the total number of words in file
            line = br.readLine(); 
            numberOfWords = Integer.parseInt(line);

            //get three unique random words from file
            while(i<wordIndexArr.length){
                Boolean existing = false;
                randomIndex = getRandomWordIndex(numberOfWords);
                
                for(int j=0; j<wordIndexArr.length; j++){
                    if(randomIndex == wordIndexArr[j]) existing = true;
                }

                if(!existing){
                    wordIndexArr[i] = randomIndex;
                    i++;
                }
            }

            // sort the array of indices
            Arrays.sort(wordIndexArr);

            i = 0;
            int currLine = 0;
            // proceed to reading the rest of the file and getting the words of the generated indices
            while((line = br.readLine()) != null){
                if(currLine == wordIndexArr[i]){  
                    this.randomWords.add(line);
                    i++;
                }
                currLine++;
            }
            
            br.close();
        }catch(Exception e){}
    }

    /*
    accepts an integer 'max' as parameter and returns a random
    value within the range [1-max]
    */
    private int getRandomWordIndex(int max){
        Random rand = new Random();
        int index = rand.nextInt(max);
        return index;
    }

    /*
    accepts a string 'guess' and checks if 'guess' is valid by
    comparing it to 'this.answer'
    */
    public Boolean validateWord(String guess){
        Boolean valid = this.answer.equals(guess) ? true : false;
        return valid;
    }

    /*
    sets 'this.answer' to the parameter string 'word'
    */
    public void setAnswer(String word){
        this.answer = word;
    }

    /*
    returns value of 'this.answer'
    */
    public String getAnswer(){
        return this.answer;
    }

    /*
    returns the random words chosen from file
    */
    public String getWords(){
        String options = String.join(" ", this.randomWords);
        return options;
    }
}