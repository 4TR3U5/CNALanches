package com.cnalanches.app.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HomeController {

  
    public Parent carregarView(String caminhoView) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoView));
            return loader.load();
        } catch (IOException e) {
            System.err.println("Erro ao carregar a view: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

     
    @FXML
    private void abrirCadastro(ActionEvent event) {
        String caminho = "/com/cnalanches/app/cadastro-view.fxml";
        Parent root = carregarView(caminho);

        if (root != null) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Cadastro de Itens");
            stage.show();
        }
    }

    @FXML
    private void abrirEstoque(ActionEvent event) {
        Parent root = carregarView("/com/cnalanches/app/estoque-view.fxml");

        if (root != null) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Controle de Estoque");
            stage.show();
        }
    }

    @FXML
    private void abrirPdv(ActionEvent event) {
        Parent root = carregarView("/com/cnalanches/app/pdv-view.fxml");

        if (root != null) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("PDV");
            stage.show();
        }
    }


    @FXML
    private void abrirRelatorio(ActionEvent event) {
        Parent root = carregarView("/com/cnalanches/app/relatorio-view.fxml");

        if (root != null) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Relatórios");
            stage.show();
        }

    }
}