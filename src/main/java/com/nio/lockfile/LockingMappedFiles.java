package com.nio.lockfile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * 当多个线程加锁的区域有交集时，后者会抛出OverlappingFileLockException异常
 *
 * Created by dongchunxu on 2017/6/21.
 */
public class LockingMappedFiles {
    private static final int LENGTH = 0x8FFFFFF;

    static FileChannel fc;

    public static void main(String[] args) throws IOException {
        fc = new RandomAccessFile("LockingMappedFiles.txt", "rw").getChannel();

        MappedByteBuffer out =
                fc.map(FileChannel.MapMode.READ_WRITE, 0, LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            out.put((byte)'x');
        }

        new LockAndModify(out, 0, LENGTH/2);
        new LockAndModify(out, LENGTH/3, LENGTH/4 + LENGTH/2);
    }

    private static class LockAndModify extends Thread {
        private ByteBuffer buff;
        private int start, end;
        LockAndModify(ByteBuffer bb, int start, int end) {
            this.start = start;
            this.end = end;
            bb.limit(end);
            bb.position(start);
            buff = bb.slice();
            start();
        }

        public void run() {
            try {
                FileLock lock = fc.tryLock(start, end, false);
                System.out.println("Locked: " + start + " to " + end);
                while (buff.position() < buff.limit() - 1) {
                    buff.put((byte) (buff.get() + 1));
                }
                lock.release();
                System.out.println("Released: " + start + " to " + end);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

    }
}
