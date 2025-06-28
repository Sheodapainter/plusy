package pl.umcs.oop.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 12345; //port serwera
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); //lista polaczonych klientow
    private static final ExecutorService threadPool = Executors.newCachedThreadPool(); //pula watkow, dynamicznie tworzaca nowe watki i wykorzystujaca te zakonczone
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT)){ //tworzy serwer na porcie. Uzywajac "try" automatycznie zamyka serwer po zakonczeniu
            System.out.println("Serwer uruchomiony na porcie "+PORT);
            while (true) {
                System.out.println("Oczekiwanie na połączenie..."); //serwer caly czas czeka na polaczenia od klientow
                Socket socket = serverSocket.accept(); //zatrzymuje dzialanie do czasu polaczenia klienta, reszta kodu wykonuje sie po polaczeniu
                System.out.println("Połączono "+socket);
                ClientHandler clientHandler = new ClientHandler(socket, clients); //tworzy nowy obiekt klienta, przekazujac mu gniazdo polaczenia i liste klientow (aby rozsylac im wiadomosci)
                clients.add(clientHandler); //dodaje obiekt do listy aktywnych klientow
                threadPool.submit(clientHandler); //uruchamia klienta w osobnym watku
            } //petla dalej gotowa na klientow
        } catch (IOException e) {
            System.err.println("Błąd serwera: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        } //zamyka pule watkow (serwer) po tym jak sie wszystko skonczy

    }
}
