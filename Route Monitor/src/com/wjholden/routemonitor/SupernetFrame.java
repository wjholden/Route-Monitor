package com.wjholden.routemonitor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public class SupernetFrame extends JFrame implements KeyListener {
    
    final Trie trie = new BinaryRoutingTrie();
    
    public SupernetFrame(String networks[]) {
        setLayout(new FlowLayout());
        
        setTitle("Supernet Monitor");
        this.getContentPane().setBackground(Color.BLACK);
        
        for (int i = 0 ; i < networks.length ; i+= 2) {
            JPanel outer = new JPanel();
            outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
            
            SupernetPanel panel = new SupernetPanel(trie, IP.stringToInt(networks[i]), 
                IP.stringToInt(networks[i + 1]));
            outer.add(panel);
            outer.add(new JLabel(panel.prefix));
            
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
        }
    }
    
    private void setFullscreen() {
        this.dispose();
        setExtendedState(getExtendedState() ^ JFrame.MAXIMIZED_BOTH);
        setUndecorated(!isUndecorated());
        this.setLocation(0, 0);
        this.pack();
        this.setVisible(true);
    }
    
    public static void main(String args[]) {
        SupernetFrame frame = new SupernetFrame(args);
        frame.startRip();
        frame.addKeyListener(frame);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 'f': setFullscreen(); break;
            case 'q': System.exit(0); break;
            case 'c': trie.clear(); break;
            case 'h': System.out.println("hello"); break;
        }
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
