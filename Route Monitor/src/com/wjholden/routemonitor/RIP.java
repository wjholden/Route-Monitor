package com.wjholden.routemonitor;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author William John Holden (wjholden@gmail.com)
 */
public class RIP implements Runnable, Closeable {

    private final Trie trie;
    private final InetAddress GROUP;
    private static final int PORT = 520;
    private static final int MTU = 1500;
    private MulticastSocket socket;
    private static final Duration TIMEOUT = Duration.ofSeconds(180);
    
    public RIP(Trie trie) throws UnknownHostException {
        this.trie = trie;
        GROUP = InetAddress.getByName("224.0.0.9");
        
        // a timer to try to purge the trie of any routes learned more than
        // 180 seconds ago.
        Timer timer = new Timer("RIP Timeout", true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                trie.purge(TIMEOUT);
            }
            
        }, 10000, 10000); // every 10 seconds
    }
    
    private static void parse(Trie trie, ByteBuffer buffer) {
        byte command = buffer.get();
        byte version = buffer.get();
        short mustBeZero = buffer.getShort();
        
        if (command == 2) { // response code
            while (buffer.hasRemaining()) {
                short addressFamily = buffer.getShort();
                
                if (addressFamily == 0xffff) { // authentication
                    short authenticationType = buffer.getShort();
                    byte[] authentication = new byte[16];
                    buffer.get(authentication, 0, 16);
                } else if (addressFamily == 2 && buffer.remaining() >= 20) { // ipv4
                    short tag = buffer.getShort();
                    int ip = buffer.getInt();
                    int mask = buffer.getInt();
                    int nextHop = buffer.getInt();
                    int metric = buffer.getInt();
                    if (trie.set(ip, mask, metric)) {                    
                        System.out.printf("%s\t%-19s\t%2d%n", Instant.now(), IP.toString(ip) + "/" + Integer.bitCount(mask), metric);
                    }
                } else if (addressFamily == 2 && buffer.remaining() < 20) {
                    throw new RuntimeException("Not enough bytes in buffer!");
                }
            }
        }
    }
    
    @Override
    public void run() {
        try {
            socket = new MulticastSocket(PORT);
            socket.joinGroup(GROUP);
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(MTU);
                DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity());
                socket.receive(packet);
                RIP.parse(trie, buffer);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
    
    public static void main(String[] args) {
        Trie trie = new BinaryRoutingTrie();
        try (RIP rip = new RIP(trie)) {
            Thread thread = new Thread(rip);
            thread.start();
            System.out.println("Press any key to continue...");
            System.in.read();
            System.out.println("digraph {\n" + trie + "}");
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    @Override
    public void close() throws IOException {
        socket.leaveGroup(GROUP);
        socket.close();
    }
}
