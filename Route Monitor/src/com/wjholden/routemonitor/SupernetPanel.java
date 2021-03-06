package com.wjholden.routemonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
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
    private double lastSupernetPopulation;
    private final int height, width;
    private final AffineTransform transform;
    private static Duration colorChangeInterval = Duration.ofMinutes(1);
    
    public SupernetPanel(Trie trie, int ip, int mask, String description, AffineTransform transform) {
        this.trie = trie;
        this.ip = ip;
        this.mask = mask;
        this.transform = transform;
        prefixLength = Integer.bitCount(mask);
        prefix = IP.toString(ip) + "/" + prefixLength + " (" + description + ")";
        height = (32 - prefixLength) / 2;
        width = (32 - prefixLength) - height;
        lastSupernetPopulation = -1;
        this.setBackground(Color.BLACK);
        
        Timer timer = new javax.swing.Timer(0, this::updateScreen);
        timer.setRepeats(true);
        timer.setDelay(100);
        timer.start();
    }
    
    private void updateScreen(ActionEvent e) {
        double n = trie.population();
        
        // This mess is all about helping the environment by using less energy.
        // We want a low-latency user interface, but we don't need to waste
        // unnecessary CPU cycles spinlocking to get it.
        // Update the screen when the size of the trie changed or 1/10 chance.
        // This seemingly minor tweak considerably reduced CPU usage in testing
        // from 10% to about 1-2%.
        //
        // Change: now that the population is a double we can tell the difference
        // between when a route gets poised. No need for random repaints.
        // Also allows for faster swing timer since this method is effectively
        // free if no painting needs to be done.
        if (n != lastSupernetPopulation) {
            this.repaint();
            lastSupernetPopulation = n;
        }
        
    }
    

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) ((1 << width) * transform.getScaleX()),
                (int) ((1 << height) * transform.getScaleY()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //paintIterateAndRecurse(g);
        paintRecurseAndIterate(g);
    }
    
    private void paintIterateAndRecurse(Graphics g) {
        BufferedImage img = new BufferedImage(1 << width, 1 << height, BufferedImage.TYPE_INT_RGB);
        Trie subtrie = trie.subtrie(ip, mask);
        
        if (subtrie == null) {
            return;
        }
        
        for (int x = 0 ; x < (1 << width) ; x++) {
            for (int y = 0 ; y < (1 << height) ; y++) {
                img.setRGB(x, y, subtrie.find(((1 << width) * y + x) << prefixLength, colorChangeInterval).getRGB());
            }
        }
        ((Graphics2D) g).drawImage(img, transform, null);
    }
    
    private void paintRecurseAndIterate(Graphics g) {
        BufferedImage img = new BufferedImage(1 << width, 1 << height, BufferedImage.TYPE_INT_ARGB);
        int colors[] = trie.color(ip, mask, colorChangeInterval);
        img.setRGB(0, 0, 1 << width, 1 << height, colors, 0, 1 << width);
        ((Graphics2D) g).drawImage(img, transform, null);
    }
    
    public static void setColorChangeInteveral(final Duration duration) {
        SupernetPanel.colorChangeInterval = duration;
    }
    
    public static Duration getColorChangeInterval() {
        return SupernetPanel.colorChangeInterval;
    }
    
    private static void createAndShowGUI() {
        JFrame f = new JFrame("SupernetPanel");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Trie trie = new BinaryRoutingTrie();
        trie.set(IP.toInteger("192.168.64.0"), 0xfffff000, 1);
        trie.set(IP.toInteger("192.168.160.0"), 0xfffffff0, 1);
        trie.set(IP.toInteger("192.168.128.0"), 0xffffff00, 1);
        trie.set(IP.toInteger("192.168.32.127"), 0xffffffff, 1);
        trie.set(IP.toInteger("192.168.40.128"), 0xfffffff0, 16);
        SupernetPanel panel = new SupernetPanel(trie, IP.toInteger("192.168.0.0"), 0xffff0000, "test", new AffineTransform());
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
