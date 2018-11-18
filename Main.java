import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class Main{

    //constructor for main UI
    public Main(){

        JFrame window = new JFrame("Illus");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(new Dimension(500,500));
        window.setLayout(new BorderLayout());
        window.add(new MainWindow());
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    //main window contents
    public class MainWindow extends JPanel {

        private Canvas canvasPane;

        //set up main window contents
        public MainWindow() {
            setLayout(new BorderLayout());

            add(new UpperPane(), BorderLayout.NORTH);

            add((canvasPane = new Canvas()), BorderLayout.CENTER);

            add(new RightPane(), BorderLayout.EAST);

            add(new BrushOptions(canvasPane), BorderLayout.SOUTH);
        }
    }

    //upper portion
    public class UpperPane extends JPanel{

        //contains timer and word to guess
        public UpperPane() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            Timer timer = new Timer(60);
            timer.setPreferredSize(new Dimension(55,55));
            timer.setFont(new Font("San-Serif", Font.PLAIN, 20));

            add(timer, BorderLayout.WEST);

            Word word = new Word("WORD");
            word.setHorizontalAlignment(SwingConstants.CENTER);
            word.setFont(new Font("San-Serif", Font.PLAIN, 25));

            add(word, BorderLayout.CENTER);
        }
    }

    //timer component
    public class Timer extends JLabel {
        private int time;

        public Timer(int time) {
            this.time = time;
        }

        public void updateTime(){
            this.time--;
        }

        public int getTime(){
            return this.time;
        }

        //render time inside circle
        public void paintComponent( Graphics g ) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D)g;

            g2d.setColor(Color.RED);
            g2d.drawOval(0,0, 50, 50);

            g2d.setColor(Color.BLACK);
            g2d.drawString(Integer.toString(this.getTime()), 15, 32);
        }
    }

    //word-to-guess component
    public class Word extends JLabel{
        private String word;

        public Word(String word) {
            super(word);
            this.word = word;

        }

        public void updateWord(String word){
            this.word = word;
        }

        public String getTime(){
            return this.word;
        }
    }

    //right portion of UI
    public class RightPane extends JPanel {
        private TextArea chatArea;
        private TextArea scores;
        private TextField inputField;
        private JButton enterButton;

        //contains player score and chat room

        public RightPane(){
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            setPreferredSize(new Dimension(200,250));

            scores = new TextArea("SCORES:",5,1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            scores.setEditable(false);
            add(scores);

            chatArea = new TextArea("CHATROOM:",13,1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            chatArea.setEditable(false);

            add(chatArea);

            inputField = new TextField();
            add(inputField);

            enterButton = new JButton("Enter");
            enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(enterButton);
        }
    }

    //brush options component
    public class BrushOptions extends JPanel {

        //ROYGBV colors and erasers
        public BrushOptions(Canvas canvasPane) {
            JButton blackColor = new JButton(new ColorAction(canvasPane, null, Color.BLACK));
            blackColor.setBackground(Color.BLACK);
            blackColor.setPreferredSize(new Dimension(25,25));

            JButton redColor = new JButton(new ColorAction(canvasPane, null, new Color(209, 0, 0)));
            redColor.setBackground(new Color(209, 0, 0));
            redColor.setPreferredSize(new Dimension(25,25));

            JButton orangeColor = new JButton(new ColorAction(canvasPane, null, new Color(255, 102, 34)));
            orangeColor.setBackground(new Color(255, 102, 34));
            orangeColor.setPreferredSize(new Dimension(25,25));

            JButton yellowColor = new JButton(new ColorAction(canvasPane, null, new Color(255, 218, 33)));
            yellowColor.setBackground(new Color(255, 218, 33));
            yellowColor.setPreferredSize(new Dimension(25,25));

            JButton greenColor = new JButton(new ColorAction(canvasPane, null, new Color(51, 221, 0)));
            greenColor.setBackground(new Color(51, 221, 0));
            greenColor.setPreferredSize(new Dimension(25,25));

            JButton blueColor = new JButton(new ColorAction(canvasPane, null, new Color(17, 51, 204)));
            blueColor.setBackground(new Color(17, 51, 204));
            blueColor.setPreferredSize(new Dimension(25,25));

            JButton violetColor = new JButton(new ColorAction(canvasPane, null, new Color(51, 0, 68)));
            violetColor.setBackground(new Color(51, 0, 68));
            violetColor.setPreferredSize(new Dimension(25,25));


            JButton eraser = new JButton(new ColorAction(canvasPane, "Eraser", Color.WHITE));

            JButton clear = new JButton(new ClearAction(canvasPane, "Clear"));

            add(redColor);
            add(orangeColor);
            add(yellowColor);
            add(greenColor);
            add(blueColor);
            add(violetColor);
            add(eraser);
            add(clear);
        }

        public class ClearAction extends AbstractAction{
            private Canvas canvasPane;

            private ClearAction(Canvas canvasPane, String name){
                if(name != null) putValue(NAME, name);
                this.canvasPane = canvasPane;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                canvasPane.clearCanvas();
            }
        }

        public class ColorAction extends AbstractAction {

            private Canvas canvasPane;
            private Color color;

            private ColorAction(Canvas canvasPane, String name, Color color) {
                if(name != null) putValue(NAME, name);
                this.canvasPane = canvasPane;
                this.color = color;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                canvasPane.setForeground(color);
            }

        }

    }

    //canvas component
    public class Canvas extends JPanel {
        //contains drawing data
        private BufferedImage background;

        public Canvas() {
            this.setBackground(Color.WHITE);
            this.setForeground(Color.BLACK);

            //mouse event listeners
            MouseAdapter handler = new MouseAdapter() {

                //render brush drawings
                @Override
                public void mousePressed(MouseEvent e) {
                    drawDot(e.getPoint());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    drawDot(e.getPoint());
                }

            };
            //create action listeners
            addMouseListener(handler);
            addMouseMotionListener(handler);
        }

        protected void drawDot(Point pt) {
            if (background == null) {
                updateBuffer();
            }

            if (background != null) {
                //canvas
                Graphics2D g2d = background.createGraphics();

                //change color of brush
                g2d.setColor(getForeground());

                //change width and height to adjust thickness of brush
                g2d.fillOval(pt.x - 5, pt.y - 5, 10, 10);

                //release system resources of by graphics g2d
                g2d.dispose();
            }

            //update canvas
            repaint();
        }

        protected void clearCanvas(){
            background = null;
            updateBuffer();
            repaint();
        }

        protected void updateBuffer() {

            if (getWidth() > 0 && getHeight() > 0) {
                BufferedImage newBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

                //render canvas
                Graphics2D g2d = newBuffer.createGraphics();
                //set canvas BG
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                if (background != null) {
                    g2d.drawImage(background, 0, 0, this);
                }
                g2d.dispose();
                background = newBuffer;
            }

        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 200);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            if (background == null) {
                updateBuffer();
            }
            g2d.drawImage(background, 0, 0, this);
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        new Main();
    }

}