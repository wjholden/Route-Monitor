package com.wjholden.routemonitor;

import java.awt.Color;
import java.time.Duration;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public interface Trie {
    boolean set(int ip, int mask, int metric);
    Trie subtrie(int ip, int mask);
    Trie find(int ip);
    Color find(int ip, Duration change);
    int[] color(int ip, int mask, Duration change);
    double population();
    void clear();
    void purge(Duration timeout);
}
