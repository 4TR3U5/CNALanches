package com.cnalanches.app.controller;

import java.io.IOException;
import com.cnalanches.app.service.LoginService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    private final LoginService loginService;

    public LoginController() {
        this.loginService = new LoginService();
    }

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtSenha;

    @FXML
    private Label lblMensagem;

    @FXML
    private void validarLogin() {
        String usuario = txtUsuario.getText();
        String senha = txtSenha.getText();

        boolean retorno = this.loginService.validarLogin(usuario, senha);

        if (retorno) {
            abrirTelaHome();
        } else {
            lblMensagem.setText("Usuário ou senha inválidos.");
            lblMensagem.setStyle("-fx-text-fill: red;");
        }
    }

    private void abrirTelaHome() {
        try {
           
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cnalanches/app/home-view.fxml"));
            Parent root = loader.load();

           
            Stage stage = (Stage) txtUsuario.getScene().getWindow();

  
            stage.setScene(new Scene(root));
            stage.setTitle("Home");
            stage.show();

        } catch (IOException e) {
            System.err.println("Erro ao carregar a tela home: " + e.getMessage());
            e.printStackTrace();
            lblMensagem.setText("Erro ao abrir a tela principal.");
            lblMensagem.setStyle("-fx-text-fill: red;");
        }
    }
}