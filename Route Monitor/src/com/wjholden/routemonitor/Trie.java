package com.wjholden.routemonitor;

import java.awt.Color;
import java.time.Duration;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public interface Trie {
    void set(int ip, int mask, int metric, String prefix);
    void set(int ip, int mask, int metric);
    Trie subtrie(int ip, int mask);
    Trie find(int ip);
    Color find(int ip, Duration change);
    int size();
    void clear();
    boolean purge(Duration timeout);
}
