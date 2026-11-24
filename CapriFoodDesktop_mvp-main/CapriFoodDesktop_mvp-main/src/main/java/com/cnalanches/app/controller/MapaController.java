package com.cnalanches.app.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.cnalanches.app.model.MesaAbertaModel;
import com.cnalanches.app.service.VendaService;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MapaController {

    @FXML private FlowPane painelMesas;
    @FXML private Button btnAtualizar;
    @FXML private Label lblStatus;

    private final VendaService vendaService = new VendaService();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private final DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        carregarMesas();
    }

    @FXML
    public void carregarMesas() {
        btnAtualizar.setDisable(true);
        lblStatus.setText("Carregando...");

        Task<List<MesaAbertaModel>> task = new Task<>() {
            @Override
            protected List<MesaAbertaModel> call() throws Exception {
                return vendaService.listarMesasAbertas();
            }
        };

        task.setOnSucceeded(e -> {
            montarMesas(task.getValue());
            btnAtualizar.setDisable(false);
            lblStatus.setText(task.getValue().size() + " mesa(s) aberta(s)");
        });

        task.setOnFailed(e -> {
            btnAtualizar.setDisable(false);
            lblStatus.setText("Erro ao carregar mesas");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void montarMesas(List<MesaAbertaModel> mesas) {
        painelMesas.getChildren().clear();

        if (mesas.isEmpty()) {
            Label vazio = new Label("Nenhuma mesa aberta. Clique em \"+ Nova mesa\".");
            vazio.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
            painelMesas.getChildren().add(vazio);
            return;
        }

        for (MesaAbertaModel m : mesas) {
            painelMesas.getChildren().add(criarCard(m));
        }
    }

    private VBox criarCard(MesaAbertaModel m) {
        boolean vazia = m.getQtdItens() == 0;

        Label numero = new Label("Mesa " + m.getMesa());
        numero.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        Label total = new Label(moeda.format(m.getTotal()));
        total.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                + (vazia ? "#999999" : "#28a745") + ";");

        Label detalhe = new Label(vazia
                ? "Sem itens • aberta às " + m.getAbertura().format(hora)
                : m.getQtdItens() + " item(ns) • aberta às " + m.getAbertura().format(hora));
        detalhe.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");

        VBox card = new VBox(6, numero, total, detalhe);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.setPrefSize(210, 110);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: " + (vazia ? "#e0e0e0" : "#007bff") + "; "
                + "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand;");
        card.setOnMouseClicked(e -> abrirVenda((Node) e.getSource(), m.getComandaId(), m.getMesa()));
        return card;
    }

    @FXML
    private void novaMesa(ActionEvent event) {
        abrirVenda((Node) event.getSource(), null, null);
    }

    /** Abre a tela de venda em modo mesa: nova (ids nulos) ou uma comanda já aberta. */
    private void abrirVenda(Node origem, Integer comandaId, Integer mesa) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cnalanches/app/pdv-view.fxml"));
            Parent root = loader.load();

            VendaController venda = loader.getController();
            if (comandaId == null) {
                venda.iniciarModoMesa();
            } else {
                venda.iniciarMesaExistente(comandaId, mesa);
            }

            Stage stage = (Stage) origem.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CnaLanches - Mesa");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Não foi possível abrir a tela de venda.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    @FXML
    private void voltarParaHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cnalanches/app/pdv-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CnaLanches - Home");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}