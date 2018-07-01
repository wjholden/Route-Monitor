package com.wjholden.routemonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.time.Duration;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public class SupernetPanel extends JPanel {
    
    private final Trie trie;
    private final int ip, mask;
    private final int prefixLength;
    protected final String prefix;
    private final Dimension dimension;
    private int lastSupernetPopulation;
    private final int height, width;
    
    public SupernetPanel(Trie trie, int ip, int mask) {
        this.trie = trie;
        this.ip = ip;
        this.mask = mask;
        prefixLength = Integer.bitCount(mask);
        prefix = IP.intToString(ip) + "/" + prefixLength;
        height = (32 - prefixLength) / 2;
        width = (32 - prefixLength) - height;
        dimension = new Dimension(1 << width, 1 << height);
        lastSupernetPopulation = -1;
        this.setBackground(Color.BLACK);
        
        Timer timer = new javax.swing.Timer(0, this::updateScreen);
        timer.setRepeats(true);
        timer.setDelay(500);
        timer.start();
    }
    
    private void updateScreen(ActionEvent e) {
        int n = trie.size();
        
        // This mess is all about helping the environment by using less energy.
        // We want a low-latency user interface, but we don't need to waste
        // unnecessary CPU cycles spinlocking to get it.
        // Update the screen when the size of the trie changed or 1/10 chance.
        // This seemingly minor tweak considerably reduced CPU usage in testing
        // from 10% to about 1-2%.
        if (n != lastSupernetPopulation || Math.random() < 0.1) {
            this.repaint();
            lastSupernetPopulation = n;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return dimension;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        BufferedImage img = new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_RGB);
        Trie subtrie = trie.subtrie(ip, mask);
        
        if (subtrie == null) {
            return;
        }
        
        for (int x = 0 ; x < dimension.width ; x++) {
            for (int y = 0 ; y < dimension.height ; y++) {
                img.setRGB(x, y, subtrie.find((dimension.width * y + x) << prefixLength, Duration.ofSeconds(30)).getRGB());
            }
        }
        g.drawImage(img, 0, 0, null);
    }
    
    private static void createAndShowGUI() {
        JFrame f = new JFrame("SupernetPanel");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Trie trie = new BinaryRoutingTrie();
        trie.set(IP.stringToInt("192.168.64.0"), 0xfffff000, 1);
        trie.set(IP.stringToInt("192.168.160.0"), 0xfffffff0, 1);
        trie.set(IP.stringToInt("192.168.128.0"), 0xffffff00, 1);
        trie.set(IP.stringToInt("192.168.32.127"), 0xffffffff, 1);
        trie.set(IP.stringToInt("192.168.40.128"), 0xfffffff0, 16);
        SupernetPanel panel = new SupernetPanel(trie, IP.stringToInt("192.168.0.0"), 0xffff0000);
        f.add(panel);
        f.pack();
        f.setVisible(true);
    }
    
    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
}
