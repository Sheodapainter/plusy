package pl.umcs.oop.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable { //runnable pozwala na odpalenie w osobnym watku
    private final Socket socket; //polaczenie z tym konkretnym klientem
    private final List<ClientHandler> clients; //lista klientow
    private PrintWriter out; //strumien do wysylania wiadomosci
    private String login;

    public ClientHandler(Socket socket, List<ClientHandler> clients) { //konstruktor, zapisuje dana przekazane przez serwer
        this.socket = socket;
        this.clients = clients;
    }
    public String getLogin() {
        return login;
    }

    public boolean loginExists(String loginToCheck) {
        for (ClientHandler c : clients) {
            if (loginToCheck.equals(c.getLogin())) { //sprawdza, czy login istnieje w liscie
                return true;
            }
        }
        return false;
    }

    private void broadcast(String message) { //rozsyla wiadomosc do innych klientow
        for (ClientHandler c : clients) { //wykonuje dla kazdego polaczonego klienta
            if (c.login != null) {
                c.sendMessage(message); //wysyla wiadomosc
            }
        }
    }
    
    private void sendMessage(String message) { //wysyla wiadomosc do konkretnego klienta
        if (out != null) {
            out.println(message);
        }
    }
    
    public ClientHandler getClientByLogin(String login) {
        for (ClientHandler c : clients) {
            if (login.equals(c.getLogin())) {
                return c; //zwraca klienta ze zgodnym loginem
            }
        }
        return null;
    }

    @Override
    public void run() { //wykonywana po odpaleniu watku, obsluguje komunikacje z klientem
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){ //try gwarantuje zamkniecie po zakonczeniu, BReader "in" odczytuje wiadomosc od klienta
            out = new PrintWriter(socket.getOutputStream(), true); //strumien do wysylania wiadomosci
            out.println("Witaj w czacie! Podaj login (LOGIN:twojLogin):");
            String message; //przechowuje wiadomosci

            while ((message = in.readLine()) != null) { //logowanie
                if (message.startsWith("LOGIN:")) { //akceptuje tylko podany format
                    if(message.contains(" ")) {
                        out.println("Login nie moze zawierac spacji.");
                    } else {
                        this.login = message.substring(6).trim();
                        broadcast(login + " dolaczyl do czatu.");
                        break; //konczy petle przy udanym logowaniu
                    }
                } else {
                    out.println("Niepoprawny format logowania.");
                }
            }
            while((message = in.readLine()) != null) { //glowna komunikacja
                if(message.equalsIgnoreCase("/online")) { //obsluga komendy /online
                    String users = clients.stream().filter(c -> c.login!= null) //wybiera tylko zalogowanych klientow
                            .map(c -> "["+c.login+"]").collect(Collectors.joining("\n")); //tworzy tekst z filtrowanej listy uzytkownikow
                    sendMessage("Uzytkownicy online: "+users); //wysyla wiadomosc z lista tylko uzytkownikowi ktory wyslal komende
                } else if(message.startsWith("/w ")) { // "/w login wiadomosc" wysle wiadomosc prywatna
                    String[] parts = message.trim().split(" "); //dzieli na "/w", login i fragmenty wiadomosci
                    if(parts.length < 3) { //nie akceptuje pustej wiadomosci
                        out.println("Wiadomosc jest pusta, nie wyslano");
                    } else if(loginExists(parts[1])) { //wykonuje jezeli login istnieje
                        String msg2 = parts[2]; //ustawia wiadomosc na pierwszy segment
                        for(int i=3; i<parts.length; i++) { //petla dodaje wszystkie segmenty, tworzac pelna wiadomosc
                            msg2+=" "+parts[i];
                        }
                        ClientHandler c = getClientByLogin(parts[1]); //klient c zwraca uzytkownika o podanym loginie
                        sendMessage("Ja->"+parts[1]+": "+msg2); //wyswietlone u nas
                        c.sendMessage(this.login+"->Ja: "+msg2); //wyswietlone u odbiorcy
                    } else {
                        out.println("Podany uzytkownik nie istnieje lub nie jest zalogowany");
                    }
                } else {
                    System.out.println("Otrzymano wiadomosc: " + message); //komunikat o poprawnym wyslaniu wiadomosci
                    broadcast(this.login + ": " + message); //rozeslanie wiadomosci do wszystkich
                }
            }

        } catch (IOException e) { //obsluga bledu
            System.err.println("Blad komunikacji: "+ e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } //zamkniecie gniazda

            clients.remove(this);
            if (login != null) {
                broadcast(login + " opuscil czat.");
            } //usuniecie klienta z listy online
        }
    }
}
