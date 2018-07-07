package com.wjholden.routemonitor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public class SupernetFrame extends JFrame implements KeyListener {

    private final Trie trie = new BinaryRoutingTrie();
    private final static String USAGE = "java -jar (ip-address subnet-mask description)+\n"
            + "Example: java -jar 192.0.2.0 255.255.255.0 \"TEST-NET-1\" 198.51.100.0 255.255.255.0 \"TEST-NET-2\" 203.0.113.0 255.255.255.0 \"TEST-NET-3\"";
    private final static String HELP
            = "a: show about\n"
            + "c: clear routing table\n"
            + "f: toggle fullscreen\n"
            + "h: show this help\n"
            + "q: quit\n"
            + "r: refresh screen\n"
            + "+: enlarge panels\n"
            + "-: shrink panels\n"
            + "0: reset panel scale\n"
            + "<: half color change period\n"
            + ">: double color change period\n"
            + ".: reset color change period to 1 minute\n"
            + "";
    private static final String ABOUT = "William John Holden\n"
            + "https://github.com/wjholden/Route-Monitor/\n";
    
    private final List<SupernetPanel> panels = new ArrayList<>();
    
    private final AffineTransform transform;

    public SupernetFrame(String networks[]) {
        setLayout(new FlowLayout());
        
        transform = new AffineTransform();

        setTitle("Supernet Monitor");
        this.getContentPane().setBackground(Color.BLACK);

        for (int i = 0; i < networks.length; i += 3) {
            JPanel outer = new JPanel();
            outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));

            SupernetPanel panel = new SupernetPanel(trie, IP.toInteger(networks[i]),
                    IP.toInteger(networks[i + 1]), networks[i + 2], transform);
            outer.add(panel);
            outer.add(new JLabel(panel.prefix));
            panels.add(panel);

            add(outer);
        }

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        //new javax.swing.Timer(30000, e -> System.out.println(trie)).start();
    }

    private void startRip() {
        try {
            RIP rip = new RIP(trie);
            Thread thread = new Thread(rip);
            thread.start();
        } catch (IOException ex) {
            System.err.println(ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error in RIP thread", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setFullscreen() {
        this.dispose();
        if (isUndecorated()) {
            // clear the maximized bits (4 and 2) if fullscreen -> normal
            setExtendedState(getExtendedState() & (~JFrame.MAXIMIZED_BOTH));
            setUndecorated(false);
        } else {
            // set the maximized bits if normal -> fullscreen
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
            setUndecorated(true);
        }
        this.setLocation(0, 0);
        this.pack();
        this.setVisible(true);
    }

    public static void main(String args[]) {
        if (args.length == 0 || args[0].matches("^.*[a-zA-Z]+.*$") || args.length % 3 > 0) {
            System.out.println(USAGE);
            return;
        }

        SupernetFrame frame = new SupernetFrame(args);
        frame.startRip();
        frame.addKeyListener(frame);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 'f':
                setFullscreen();
                break;
            case 'q':
                System.exit(0);
                break;
            case 'c':
                trie.clear();
                break;
            case 'h':
                JOptionPane.showMessageDialog(this, HELP, "Help", JOptionPane.QUESTION_MESSAGE);
                break;
            case 'a':
                JOptionPane.showMessageDialog(this, ABOUT);
                break;
            case 'r':
                refresh();
                break;
            case '+':
                transform.scale(1.1, 1.1);
                refresh();
                this.dispose();
                this.pack();
                this.setVisible(true);
                break;
            case '-': 
                transform.scale(0.9, 0.9);
                refresh();
                this.dispose();
                this.pack();
                this.setVisible(true);
                break;
            case '0':
                transform.setToIdentity();
                refresh();
                this.dispose();
                this.pack();
                this.setVisible(true);
                break;
            case '>': 
                SupernetPanel.setColorChangeInteveral(SupernetPanel.getColorChangeInterval().multipliedBy(2L));
                break;
            case '<':
                SupernetPanel.setColorChangeInteveral(SupernetPanel.getColorChangeInterval().dividedBy(2L));
                break;
            case '.':
                SupernetPanel.setColorChangeInteveral(Duration.ofMinutes(1));
                break;
        }
    }
    
    private void refresh() {
        panels.forEach(p -> p.repaint());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // do nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // do nothing
    }
}
