package pl.umcs.oop.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_HOST = "localhost"; //adres serwera
    private static final int SERVER_PORT = 12345; //numer portu
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT); //tworzenie polaczenia z serwerem (gniazdo)
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //strumien wejsciowy z gniazda, odbieranie wiadomosci z serwera
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); //strumien wyjsciowy, wysyla dane do serwera. Argument true automatyczne oproznianie bufora, czyli wysylanie od razu
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in)); //strumien wejsciowy z klawiatury, odbieranie wiadomosci od uzytkownika

            System.out.println("Połączono z serwerem!"); //wiadomosc polaczenia
            Thread receiver = new Thread(() -> { //nasluchuje wiadomosci w tle
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    } //petla wypisuje wiadomosci od serwera az do momentu odebrania null, czyli rozlaczenia
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } //obsluga bledu
                System.exit(0); //klient konczy dzialanie
            });
            receiver.setDaemon(true); //watek ustawia na pomocniczy, czyli nie blokuje zakonczenia programu
            receiver.start(); //uruchom watek

            System.out.print("Podaj login: ");
            String login = consoleIn.readLine(); //uzywa strumienia z klawiatury
            out.println("LOGIN:" + login);

            String input;
            while (!(input = consoleIn.readLine()).equals("exit")) {
                out.println(input); //wysyla wiadomosc wpisana z klawiatury
            } //petla dziala dopoki z klawiatury nie wypiszemy exit

            socket.close(); //zamyka polaczenie
        } catch (IOException e) {
            throw new RuntimeException(e);
        } //obsluga bledu
    }
}
