package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view.xml")); //ładuje plik z katalogu resources
        Scene scene = new Scene(loader.load()); //wczytuje strukturę z view, tworzy komponenty i kontroler
        primaryStage.setTitle("Klient"); //tytuł okna
        primaryStage.setScene(scene); //przypisuje scenę do okna
        primaryStage.show(); //wyświetla okno
    }
    public static void main(String[] args) {
        launch(args);
    }

} //aplikacja graficzna
