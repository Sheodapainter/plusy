package pl.umcs.oop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class ImageHandler {
    private BufferedImage image; //obraz

    public void loadImage(String filePath) {
        try {
            image = ImageIO.read(new File(filePath)); //odczytuje obraz z pliku i zapisuje go w polu
        } catch (IOException e) {
            System.err.println("Bład wczytywania pliku: " + e.getMessage());
        }
    }

    public void saveImage(String filePath) {
        try {
            String format = filePath.substring(filePath.lastIndexOf('.') + 1); //odczyt formatu, podciąg zaczynający się od znaku po kropce
            ImageIO.write(image, format, new File(filePath)); //zapisuje obraz do pliku, w tym wypadku nowego
        } catch (IOException e) {
            System.err.println("Bład zapisu: " + e.getMessage());
        }
    }

    public void increaseBrightness(int value) {
        for (int y=0; y < image.getHeight(); y++) { //kazdy rzad pikseli
            for(int x=0; x< image.getWidth(); x++) { //kazdy piksel z rzedu
                int rgb = image.getRGB(x, y); //odczytuje rgb piksela
                Color color = new Color(rgb); //tworzy kolor z wartosci rgb
                int r = Math.clamp(color.getRed() + value, 0, 255);
                int g = Math.clamp(color.getGreen() + value, 0, 255);
                int b = Math.clamp(color.getBlue() + value, 0, 255); //kazda wartosc rozjasnia o wartosc z argumentu
                Color brighter = new Color(r, g, b); //tworzy nowy kolor z rozjasnionych wartosci

                image.setRGB(x, y, brighter.getRGB()); //ustawia piksel na rozjasniony kolor
            }
        }
    }

    public void increaseBrightnessMultiThreaded(int value) {
        int cores = Runtime.getRuntime().availableProcessors(); //odczyt ilosc rdzeni komputera
        Thread[] threads = new Thread[cores]; //utworzenie tablicy watkow

        System.out.println("Dostepne rdzenie: "+cores);

        int totalSize = image.getHeight() * image.getWidth(); //calkowita ilosc pikseli
        int chunkSize = totalSize / cores;

        for (int i = 0; i < cores; i++) { //operacja wykonana dla kazdego rdzenia
            int start = i*chunkSize; //poczatkowy piksel to jeden po koncowym ostatniego chunku
            int end = (i == cores - 1) ? totalSize : (start + chunkSize); //ostatni watek idzie do konca, wszystkie inne tylko do konca chunku

            threads[i] = new Thread(() -> { //kazda petla robi nowy watek
                for (int index = start; index < end; index++) { //watek dziala od poczatku do konca swojego chunku
                    int x = index % image.getWidth(); //% z szerokosci, kazdy kolejny piksel przesunie sie o 1, resetujac sie do 0 po osiagnieciu maks szer
                    int y = index / image.getWidth(); //iloraz z szerokosci, po osiagnieciu maks szer zwiekszy sie o 1, zaczynajac nowy rzad

                    int rgb = image.getRGB(x, y);
                    Color color = new Color(rgb);
                    int r = Math.clamp(color.getRed() + value, 0, 255);
                    int g = Math.clamp(color.getGreen() + value, 0, 255);
                    int b = Math.clamp(color.getBlue() + value, 0, 255);
                    Color brighter = new Color(r, g, b);

                    image.setRGB(x, y, brighter.getRGB()); //patrz metoda powyzej
                }
            });
        }

        for (Thread t: threads) { //rozpoczyna kazdy watek
            t.start();
        }

        for (Thread t: threads) { //???
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void increaseBrightnessThreadPool(int value) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //nowa pula watkow na bazie ilosci rdzeni

        for (int y = 0; y < image.getHeight(); y++) { //wykonuje dla kazdego rzedu
            final int row = y;
            executor.execute(() -> { //pula watkow wykonuje kod, kazdy watek osobny rzad
                for(int x=0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, row);
                    Color color = new Color(rgb);
                    int r = Math.clamp(color.getRed() + value, 0, 255);
                    int g = Math.clamp(color.getGreen() + value, 0, 255);
                    int b = Math.clamp(color.getBlue() + value, 0, 255);
                    Color brighter = new Color(r, g, b);

                    image.setRGB(x, row, brighter.getRGB());
                } //patrz metody powyzej
            });
        }
        executor.shutdown(); //zamknac watki
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES); //czeka na zakonczenie zadan
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public int[][] computeHistogramThreadPool() {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int cores = Runtime.getRuntime().availableProcessors();

        final int[][] histograms = new int[3][256]; // 0 - R, 1 - G, 2 - B

        ExecutorService executor = Executors.newFixedThreadPool(cores); //pula watkow

        for (int y = 0; y < height; y++) {
            final int row = y;
            executor.execute(() -> { //kazdy watek wykonuje osobny rzad
                int[] localRed = new int[256];
                int[] localGreen = new int[256];
                int[] localBlue = new int[256]; //kazde pole odpowiada wartosci koloru

                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, row);
                    Color color = new Color(rgb);
                    localRed[color.getRed()]++;
                    localGreen[color.getGreen()]++;
                    localBlue[color.getBlue()]++; //dodaje 1 do pola odpowiadajacego okreslonej wartosci koloru
                }

                synchronized (histograms[0]) { //???
                    for (int i = 0; i < 256; i++) {
                        histograms[0][i] += localRed[i];
                        histograms[1][i] += localGreen[i];
                        histograms[2][i] += localBlue[i]; //pelny histogram
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
        BufferedImage histImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); //kazdy piksel ma kolor rgb
        Graphics2D g = histImage.createGraphics(); //obiekt pozwalajacy rysowac na obrazie
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height); //ustawia cale tlo na czarne

        int max = 0;
        for (int i = 0; i < 256; i++) {
            max = Math.max(max, Math.max(histogram[0][i], Math.max(histogram[1][i], histogram[2][i]))); //szuka najwyzszej wartosci, zeby przeskalowac slupki do konca obrazu
        }
        if (max == 0) max = 1;

        for (int i = 0; i < 256; i++) {
            int rHeight = histogram[0][i] * height / max;
            int gHeight = histogram[1][i] * height / max;
            int bHeight = histogram[2][i] * height / max; //skaluje slupki aby najwyzszy mial wysokosc 100

            g.setColor(Color.RED);
            g.drawLine(i, height, i, height - rHeight); //dla kazdego koloru tworzy linie od dolu do wysokosci maks w tej samej kolumnie

            g.setColor(Color.GREEN);
            g.drawLine(i, height, i, height - gHeight);

            g.setColor(Color.BLUE);
            g.drawLine(i, height, i, height - bHeight);
        }

        g.dispose();
        return histImage;
    }
}
