package com.wjholden.routemonitor;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public class BinaryRoutingTrie implements Trie {

    private final BinaryRoutingTrie children[];
    private int metric;
    private double population;
    private final long id;
    private static long counter = 0;
    private Instant modified, lastSeen;

    public BinaryRoutingTrie() {
        children = new BinaryRoutingTrie[2];
        metric = -1;
        id = counter;
        counter++;
        modified = lastSeen = Instant.EPOCH;
        population = 0;
    }

    @Override
    public synchronized boolean set(int ip, int mask, int metric) {
        if (mask == 0) {
            // always update the last seen time, even if we don't change anything
            lastSeen = Instant.now();
            
            if (this.metric != metric) {
                this.metric = metric;
                this.modified = lastSeen;
                this.setPopulation();
                return true; // change made, allows for logging
            }
            
            return false; // no change made
        } else {
            int i = (ip >>> 31);
            if (children[i] == null) {
                children[i] = new BinaryRoutingTrie();
            }
            boolean childChanged = children[i].set(ip << 1, mask << 1, metric);
            if (childChanged) {
                this.setPopulation();
            }
            return childChanged;
        }
    }

    private void setPopulation() {
        // I can't help myself. Memoization improves performance and
        // the use of Dynamic Programming is a nod to Richard Bellman.
        // Count this route only if it has a reachable metric.
        //
        // Poisoned routes are worth 1/1,000,000 of a size.
        this.population = (metric > 0 && metric < 16 ? 1 : 0)
                + (metric == 16 ? 1e-6 : 0)
                + (children[0] == null ? 0 : children[0].population)
                + (children[1] == null ? 0 : children[1].population);
    }

    @Override
    public synchronized Trie subtrie(int ip, int mask) {
        if (mask == 0) {
            return this;
        }

        int i = (ip >>> 31);
        if (children[i] == null) {
            return null;
        } else {
            return children[i].subtrie(ip << 1, mask << 1);
        }
    }

    @Override
    public synchronized BinaryRoutingTrie find(int ip) {
        int i = (ip >>> 31);
        if (children[i] == null) {
            return this;
        }

        // Catch the corner case of following a subtrie that does not match.
        // For example, a routing table contains 10/8 and 10.16/16.
        // Then lookups to 10.0.0.1 will continue down the 10.16/16 subtrie
        // and miss, so we need to match the 10/8 route.
        BinaryRoutingTrie r = children[i].find(ip << 1);
        return r.metric == -1 ? this : r;
    }

    @Override
    public synchronized Color find(int ip, Duration change) {
        BinaryRoutingTrie r = this.find(ip);
        Color c;
        if (r.metric == -1) { // no matching route in trie
            c = Color.BLACK;
        } else if (r.metric == 16) {
            // this route was poisoned recently
            Duration mdiff = Duration.between(r.modified, Instant.now());
            if (mdiff.compareTo(change) < 0) {
                c = Color.RED;
            } else {
                c = Color.BLACK;
            }
        } else if (r.metric == 0 || r.metric < -1 || r.metric > 16) {
            // none of these conditions should ever happen
            c = Color.PINK;
        } else {
            // this route was learned recently
            Duration mdiff = Duration.between(r.modified, Instant.now());
            if (mdiff.compareTo(change) < 0) {
                c = Color.BLUE;
            } else {
                c = Color.WHITE;
            }
        }
        return c;
    }

    @Override
    public double population() {
        return this.population;
    }

    @Override
    public synchronized void clear() {
        children[0] = children[1] = null;
        metric = -1;
        population = 0;
        modified = Instant.now();
    }

    @Override
    public synchronized void purge(final Duration timeout) {
        // We do not purge poisoned routes. This is to make sure the network
        // operator notices the red line.
        if (metric < 16 && Duration.between(this.lastSeen, Instant.now()).compareTo(timeout) > 0) {
            this.metric = -1;
        }
        for (int i = 0; i < 2; i++) {
            if (children[i] != null) {
                children[i].purge(timeout);
                if (children[i].population == 0) {
                    children[i] = null;
                }
            }
        }
        this.setPopulation();
    }

    @Override
    public String toString() {
        String s = String.format("  %d [label=\"%d\"];%n", id, metric);
        for (BinaryRoutingTrie child : children) {
            if (child != null) {
                s += String.format("  %d -> %d;%n", this.id, child.id);
                s += child.toString();
            }
        }
        return s;
    }

    public static void main(String args[]) {
        BinaryRoutingTrie t = new BinaryRoutingTrie();
        //t.set(IP.stringToInt("0.0.0.0"), 0, (byte) 15);

        t.set(IP.toInteger("192.168.0.0"), 0xffffff00, (byte) 5);
        /*
         t.set(IP.stringToInt("192.168.1.0"), 0xffffff00, (byte)13);
         t.set(IP.stringToInt("10.0.0.0"), 0xff000000, (byte)7);
         t.set(IP.stringToInt("10.16.0.0"), 0xffff0000, (byte)8);
         t.set(IP.stringToInt("172.16.34.12"), 0xffffc000, (byte)11);
         t.set(IP.stringToInt("172.16.34.12"), 0xffffffff, (byte)16);
         */

        System.out.println(t.find(IP.toInteger("192.168.0.0")).metric);
        t.set(IP.toInteger("192.168.0.0"), 0xffffff00, (byte) 5);
        System.out.println(t.find(IP.toInteger("192.168.0.0")).metric);

        /*
         System.out.println("digraph {");
         System.out.print(t);
         System.out.println("}");
         */
        /*
         System.out.println("Basic Tests");
         String[] basicTests = new String[] { "192.168.0.5", "192.168.1.5", 
         "192.168.2.5", "10.1.1.1", "10.16.0.1", "0.0.0.0", "255.255.255.255",
         "172.16.34.11", "172.16.34.12" };
         for (String test : basicTests) {
         System.out.printf("%s: %d%n", test, t.find(IP.stringToInt(test)).metric);
         }
         */
        /*
         System.out.println("Subtrie tests");
         Trie subtrie = t.subtrie(IP.stringToInt("172.16.0.0"), 0xffff0000);
        
         for (int i = 0 ; i < 16 ; i++) {
         System.out.println(subtrie.find(IP.stringToInt("34.0.0.0") | (i << 16), Duration.ZERO));
         }
         */
        /*
         System.out.println("digraph {");
         System.out.print(subtrie);
         System.out.println("}");
         */
        //System.out.println(subtrie.find(IP.stringToInt("34.11.0.0")));
        //System.out.println(subtrie.find(IP.stringToInt("34.12.0.0")));
    }

    @Override
    public int[] color(int ip, int mask, Duration change) {
        final BinaryRoutingTrie root = (BinaryRoutingTrie) this.subtrie(ip, mask);
        final int size = 1 << (32 - Integer.bitCount(mask));
        final int color[] = new int[size];
        if (root != null) {
            color(root, color, 0, size, change);
        } else {
            Arrays.fill(color, Color.BLACK.getRGB());
        }
        return color;
    }

    private static void color(BinaryRoutingTrie trie, int color[], int position, int count, Duration change) {
        if (count == 0) {
            return;
        }
        Color c = getColor(trie, change);
        if (c != Color.BLACK) {
            Arrays.fill(color, position, position + count, c.getRGB());
        }
        if (trie.children[0] != null) {
            color(trie.children[0], color, position, count >> 1, change);
        }
        if (trie.children[1] != null) {
            color(trie.children[1], color, position + (count >> 1), count >> 1, change);
        }
    }

    private static Color getColor(BinaryRoutingTrie trie, Duration change) {
        Color c;
        if (trie.metric == 16
                && Duration.between(trie.modified, Instant.now()).compareTo(change) < 0) {
            c = Color.RED;
        } else if (trie.metric > 0
                && trie.metric < 16
                && Duration.between(trie.modified, Instant.now()).compareTo(change) < 0) {
            c = Color.BLUE;
        } else if (trie.metric == 0 || trie.metric > 17) {
            c = Color.PINK; // this should be impossible. Pink is an error.
        } else if (trie.metric > 0 && trie.metric < 16) {
            c = Color.WHITE;
        } else {
            c = Color.BLACK;
        }
        return c;
    }
}
