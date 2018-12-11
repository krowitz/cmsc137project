import javax.swing.*;
import java.awt.*;

public class EndGamePanel extends JPanel{
    /*
    displays the end game view after the last level which
    consists of the name and score of the player
    */
    public EndGamePanel(String name, String score){

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(Color.WHITE);

        this.add(Box.createRigidArea(new Dimension(50,50)));
        JLabel endGameText = new JLabel("Great game, " + name + "!");
        endGameText.setFont(new Font("San-Serif", Font.PLAIN, 40));
        JLabel scoreText = new JLabel("Your final score is");
        scoreText.setFont(new Font("San-Serif", Font.PLAIN, 45));
        JLabel scoreValue = new JLabel(score);
        scoreValue.setFont(new Font("San-Serif", Font.BOLD, 100));
        scoreValue.setForeground(Color.ORANGE);

        endGameText.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        scoreText.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        scoreValue.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        this.add(endGameText);
        this.add(scoreText);
        this.add(scoreValue);
    }
}