import java.io.*;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Dictionary{
    private final String filename = "words.txt";
    private ArrayList<String> randomWords;
    private String word;

    // reads the contents of the file
    public void readFile(){
        try{
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            int i=0, randomIndex=1, numberOfWords; //total number of words in file
            int[] wordIndexArr = new int[3];
            this.randomWords = new ArrayList<String>();
            
            line = br.readLine(); //read total number of words in file
            numberOfWords = Integer.parseInt(line);

            while(i<wordIndexArr.length){
                boolean existing = false;
                randomIndex = getRandomWordIndex(numberOfWords);
                
                for(int j=0; j<wordIndexArr.length; j++){
                    if(randomIndex == wordIndexArr[j]) existing = true;
                }

                if(!existing){
                    wordIndexArr[i] = randomIndex;
                    i++;
                }
            }

            Arrays.sort(wordIndexArr);

            i = 0;
            int currLine = 0;
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

    private int getRandomWordIndex(int max){
        Random rand = new Random();
        int index = rand.nextInt(max);
        return index;
    }

    //Allows the user to choose a word from the three random words
    public void chooseWord(){
        String chosenWord;
        Scanner sc = new Scanner(System.in);
        boolean valid;

        System.out.println("Select a word to be guessed from the pool of words ");
        
            for(String word : this.randomWords){
                System.out.print("[" + word + "] ");
            }
            System.out.println();
        do{
            chosenWord = sc.nextLine();
            valid = false;
            if(this.randomWords.contains(chosenWord)){
                valid = true;
            }else{
                System.out.println("Word typed is not in the pool of words. Try again.");
            }
        }while(!valid);

        System.out.println("Chosen word is: " + chosenWord);
        this.setWord(chosenWord);
    }

    public boolean validateWord(String answer, String guess){
        boolean valid;
        return valid = answer.equals(guess) ? true : false;
    }

    public void setWord(String word){
        this.word = word;
    }

    public String getWord(){
        return this.word;
    }
    
}