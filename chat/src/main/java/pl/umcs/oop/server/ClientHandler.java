package pl.umcs.oop.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final List<ClientHandler> clients;
    private PrintWriter out;
    private String login;

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    private void broadcast(String message) {
        for (ClientHandler c : clients) {
            if (c.login != null) {
                c.sendMessage(message);
            }
        }
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Witaj w czacie! Podaj login (LOGIN:twojLogin):");
            String message;

            while ((message = in.readLine()) != null) {
                if (message.startsWith("LOGIN:")) {
                    this.login = message.substring(6).trim();
                    broadcast(login + " dolaczyl do czatu.");
                    break;
                } else {
                    out.println("Niepoprawny format logowania.");
                }
            }
            while((message = in.readLine()) != null) {
                if(message.equalsIgnoreCase("/online")) {
                    String users = clients.stream().filter(c -> c.login!= null)
                            .map(c -> "["+c.login+"]").collect(Collectors.joining("\n"));
                    sendMessage("Uzytkownicy online: "+users);
                } else {
                    System.out.println("Otrzymano wiadomosc: " + message);
                    broadcast(this.login + ": " + message);
                }
            }

        } catch (IOException e) {
            System.err.println("Blad komunikacji: "+ e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            clients.remove(this);
            if (login != null) {
                broadcast(login + " opuscil czat.");
            }
        }
    }
}
