package org.example;
//komentarze do uzytku wlasnego
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;

import java.io.*;
import java.net.Socket;
import java.util.Comparator;

public class Controller {
    @FXML private TextField filterField; // Pole do filtrowania słów w czasie rzeczywistym
    @FXML private ListView<Word> wordList; // Lista wyświetlanych słów
    @FXML private Label wordCountLabel; // Etykieta pokazująca liczbę widocznych słów

    private final ObservableList<Word> words = FXCollections.observableArrayList(); // Pełna lista otrzymanych słów
    private final FilteredList<Word> filteredWords = new FilteredList<>(words, w -> true); // Filtrowana widoczna lista

    public void initialize() {
        SortedList<Word> sortedWords = new SortedList<>(filteredWords, Comparator.comparing(w -> w.getWord().toLowerCase())); //lista posortowana alfabetycznie
        //wordList.setItems(words);//Ustawia dane do wyświetlenia w liście.
        //wordList.setItems(filteredWords);//widoczna lista filtrowana
        wordList.setItems(sortedWords);//widoczna lista posortowana po filtrowaniu

        wordList.setCellFactory(lv -> new ListCell<>() { //komórka listy słów
            @Override
            protected void updateItem(Word item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString()); //wyświetla wynik toString z Word
            }
        }); //modyfikuje sposób wyświetlania słowa

        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.trim().toLowerCase(); //filtr nie jest case sensitive
            filteredWords.setPredicate(w -> filter.isEmpty() || w.getWord().toLowerCase().contains(filter)); //dodaje słowo kiedy filtr pusty albo słowo zawiera filtr
            wordCountLabel.setText(String.valueOf(filteredWords.size())); //aktualizuj licznik
        });
        /*filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            wordList.setItems(words.filtered(w -> w.getWord().contains(newVal))); //pokazuje tylko słowa z wybranym tekstem
            wordList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Word item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            }); //ustawia nową wartość
        });*/ //reaguje na każdą zmianę tekstu

        connectToServer();
    }
    private void connectToServer() {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 12345); //połączenie z hostem
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                //odczyt linii
                String word;
                while ((word = in.readLine()) != null) {
                    final String finalWord = word;
                    Platform.runLater(() -> {
                        Word newWord = new Word(finalWord);
                        words.add(newWord);
                        wordCountLabel.setText(String.valueOf(filteredWords.size()));
                    }); //tylko główny wątek może modyfikować GUI
                } //czyta słowa do przerwania połączenia
            } catch (IOException e) {
                Platform.runLater(() -> words.add(new Word("Blad polaczenia: " + e.getMessage())));
            } //obsługa wyjątku
        }).start(); //nowy wątek, żeby aplikacja się nie zamroziła
    }
}
