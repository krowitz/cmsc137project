import javax.swing.*;
import java.awt.*;

public class EndGamePanel extends JPanel{
    JPanel subPanel;
    public EndGamePanel(String name, String score){
        // subPanel = new JPanel(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        // subPanel.setOpaque(false);
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
        //this.add(Box.createVerticalGlue());

        endGameText.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        scoreText.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        scoreValue.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        this.add(endGameText);
        this.add(scoreText);
        this.add(scoreValue);

    }
}