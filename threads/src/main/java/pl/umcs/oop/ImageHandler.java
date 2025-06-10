package pl.umcs.oop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class ImageHandler {
    private BufferedImage image;

    public void loadImage(String filePath) {
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            System.err.println("Bład wczytywania pliku: " + e.getMessage());
        }
    }

    public void saveImage(String filePath) {
        try {
            String format = filePath.substring(filePath.lastIndexOf('.') + 1);
            ImageIO.write(image, format, new File(filePath));
        } catch (IOException e) {
            System.err.println("Bład zapisu: " + e.getMessage());
        }
    }

    public void increaseBrightness(int value) {
        for (int y=0; y < image.getHeight(); y++) {
            for(int x=0; x< image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                int r = Math.clamp(color.getRed() + value, 0, 255);
                int g = Math.clamp(color.getGreen() + value, 0, 255);
                int b = Math.clamp(color.getBlue() + value, 0, 255);
                Color brighter = new Color(r, g, b);

                image.setRGB(x, y, brighter.getRGB());
            }
        }
    }

    public void increaseBrightnessMultiThreaded(int value) {
        int cores = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[cores];

        System.out.println("Dostepne rdzenie: "+cores);

        int totalSize = image.getHeight() * image.getWidth();
        int chunkSize = totalSize / cores;

        for (int i = 0; i < cores; i++) {
            int start = i*chunkSize;
            int end = (i == cores - 1) ? totalSize : (start + chunkSize);

            threads[i] = new Thread(() -> {
                for (int index = start; index < end; index++) {
                    int x = index % image.getWidth();
                    int y = index / image.getWidth();

                    int rgb = image.getRGB(x, y);
                    Color color = new Color(rgb);
                    int r = Math.clamp(color.getRed() + value, 0, 255);
                    int g = Math.clamp(color.getGreen() + value, 0, 255);
                    int b = Math.clamp(color.getBlue() + value, 0, 255);
                    Color brighter = new Color(r, g, b);

                    image.setRGB(x, y, brighter.getRGB());
                }
            });
        }

        for (Thread t: threads) {
            t.start();
        }

        for (Thread t: threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void increaseBrightnessThreadPool(int value) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int y = 0; y < image.getHeight(); y++) {
            final int row = y;
            executor.execute(() -> {
                for(int x=0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, row);
                    Color color = new Color(rgb);
                    int r = Math.clamp(color.getRed() + value, 0, 255);
                    int g = Math.clamp(color.getGreen() + value, 0, 255);
                    int b = Math.clamp(color.getBlue() + value, 0, 255);
                    Color brighter = new Color(r, g, b);

                    image.setRGB(x, row, brighter.getRGB());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public int[][] computeHistogramThreadPool() {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int cores = Runtime.getRuntime().availableProcessors();

        final int[][] histograms = new int[3][256]; // 0 - R, 1 - G, 2 - B

        ExecutorService executor = Executors.newFixedThreadPool(cores);

        for (int y = 0; y < height; y++) {
            final int row = y;
            executor.execute(() -> {
                int[] localRed = new int[256];
                int[] localGreen = new int[256];
                int[] localBlue = new int[256];

                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, row);
                    Color color = new Color(rgb);
                    localRed[color.getRed()]++;
                    localGreen[color.getGreen()]++;
                    localBlue[color.getBlue()]++;
                }

                synchronized (histograms[0]) {
                    for (int i = 0; i < 256; i++) {
                        histograms[0][i] += localRed[i];
                        histograms[1][i] += localGreen[i];
                        histograms[2][i] += localBlue[i];
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return histograms;
    }
    public BufferedImage generateHistogramImage(int[][] histogram) {
        int width = 256;
        int height = 100;
        BufferedImage histImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = histImage.createGraphics();
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        int max = 0;
        for (int i = 0; i < 256; i++) {
            max = Math.max(max, Math.max(histogram[0][i], Math.max(histogram[1][i], histogram[2][i])));
        }
        if (max == 0) max = 1;

        for (int i = 0; i < 256; i++) {
            int rHeight = histogram[0][i] * height / max;
            int gHeight = histogram[1][i] * height / max;
            int bHeight = histogram[2][i] * height / max;

            g.setColor(Color.RED);
            g.drawLine(i, height, i, height - rHeight);

            g.setColor(Color.GREEN);
            g.drawLine(i, height, i, height - gHeight);

            g.setColor(Color.BLUE);
            g.drawLine(i, height, i, height - bHeight);
        }

        g.dispose();
        return histImage;
    }
}
