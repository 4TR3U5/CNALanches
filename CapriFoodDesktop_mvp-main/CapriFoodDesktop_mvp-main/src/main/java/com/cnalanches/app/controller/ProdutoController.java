package com.cnalanches.app.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import com.cnalanches.app.model.CategoriaModel;  
import com.cnalanches.app.model.ProdutoModel;
import com.cnalanches.app.service.ProdutoService;
import com.cnalanches.app.service.MaskService;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

public class ProdutoController {

    @FXML private TextField txtNomeCategoria;
    @FXML private TextField txtNomeProduto;
    @FXML private TextField txtPrecoCompra;
    @FXML private TextField txtPrecoVenda;
    @FXML private TextField txtQuantidade;
    @FXML private ComboBox<CategoriaModel> cbCategoria;
    @FXML private VBox overlayLoading;
    @FXML private Button btnSalvarCategoria;
    @FXML private Button btnSalvarProduto;

    private final ProdutoService produtoService = new ProdutoService();
    private final MaskService maskService = new MaskService();
    private ObservableList<CategoriaModel> listaCategorias = FXCollections.observableArrayList();

    // Evita que o listener do filtro se chame recursivamente (StackOverflowError)
    private boolean atualizandoFiltro = false;



    @FXML
    public void initialize() {
        configurarStringConverterComboBox();
        carregarCategorias();
        configurarComboBoxDinamico();
        configurarMascaras();
        configurarValidacaoFormularios();
    }

    private void configurarMascaras() {
        maskService.aplicarMascaraMoeda(txtPrecoCompra);
        maskService.aplicarMascaraMoeda(txtPrecoVenda);
    }

    /**
     * Desabilita os botões de salvar enquanto algum campo obrigatório estiver vazio.
     * Preço com máscara zerada (R$ 0,00) fica com texto vazio, então também conta como não preenchido.
     */
    private void configurarValidacaoFormularios() {
        btnSalvarCategoria.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> vazio(txtNomeCategoria),
                        txtNomeCategoria.textProperty()
                )
        );

        btnSalvarProduto.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> vazio(txtNomeProduto)
                                || vazio(cbCategoria.getEditor())
                                || vazio(txtPrecoCompra)
                                || vazio(txtPrecoVenda)
                                || vazio(txtQuantidade),
                        txtNomeProduto.textProperty(),
                        cbCategoria.getEditor().textProperty(),
                        txtPrecoCompra.textProperty(),
                        txtPrecoVenda.textProperty(),
                        txtQuantidade.textProperty()
                )
        );
    }

    private boolean vazio(TextField campo) {
        return campo.getText() == null || campo.getText().trim().isEmpty();
    }

    private void mostrarSucesso(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    @FXML
    private void voltarParaHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cnalanches/app/home-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CnaLanches - Home");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     
    public void carregarCategorias() {
        overlayLoading.setVisible(true);

        Task<List<CategoriaModel>> task = new Task<>() {
            @Override
            protected List<CategoriaModel> call() throws Exception {
                return produtoService.obterCategorias();
            }
        };

        task.setOnSucceeded(e -> {
            listaCategorias = FXCollections.observableArrayList(task.getValue());
            atualizandoFiltro = true;
            try {
                cbCategoria.setItems(listaCategorias);
            } finally {
                atualizandoFiltro = false;
            }
            overlayLoading.setVisible(false);
        });

        task.setOnFailed(e -> {
            overlayLoading.setVisible(false);
            System.err.println("Erro ao carregar categorias.");
        });

        new Thread(task).start();
    }

    @FXML
    public void novaCategoria() {
        String nome = txtNomeCategoria.getText();

        if (nome == null || nome.trim().isEmpty()) {
            return;
        }

        overlayLoading.setVisible(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return produtoService.novaCategoria(nome);
            }
        };

        task.setOnSucceeded(e -> {
            overlayLoading.setVisible(false);
            if (task.getValue()) {
                txtNomeCategoria.clear();
                carregarCategorias();
                mostrarSucesso("Categoria \"" + nome.trim() + "\" cadastrada com sucesso!");
            }
        });

        task.setOnFailed(e -> overlayLoading.setVisible(false));

        new Thread(task).start();
    }

    /**
     * Define como o CategoriaModel será exibido no ComboBox e como o texto digitado 
     * é mapeado de volta para um objeto CategoriaModel existente.
     */
    private void configurarStringConverterComboBox() {
        cbCategoria.setConverter(new StringConverter<CategoriaModel>() {
            @Override
            public String toString(CategoriaModel cat) {
                return cat != null ? cat.getNome() : "";
            }

            @Override
            public CategoriaModel fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                return listaCategorias.stream()
                        .filter(c -> c.getNome().equalsIgnoreCase(string.trim()))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void configurarComboBoxDinamico() {
        cbCategoria.setEditable(true);

        TextField editor = cbCategoria.getEditor();
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (atualizandoFiltro) {
                return;
            }

            atualizandoFiltro = true;
            try {
                if (newValue == null || newValue.isEmpty()) {
                    if (cbCategoria.getItems() != listaCategorias) {
                        cbCategoria.setItems(listaCategorias);
                    }
                    return;
                }

                // Filtra com base no nome da CategoriaModel
                ObservableList<CategoriaModel> filtrados = FXCollections.observableArrayList();
                for (CategoriaModel cat : listaCategorias) {
                    if (cat.getNome() != null && cat.getNome().toLowerCase().contains(newValue.toLowerCase())) {
                        filtrados.add(cat);
                    }
                }

                cbCategoria.setItems(filtrados);
                if (!cbCategoria.isShowing() && editor.isFocused()) {
                    cbCategoria.show();
                }
            } finally {
                atualizandoFiltro = false;
            }
        });
    }

    @FXML
    public void novoProduto() {
        try {
            String nomeProduto = txtNomeProduto.getText();
            
         
            Integer categoriaId = null;
            Object valorSelecionado = cbCategoria.getValue();

            if (valorSelecionado instanceof CategoriaModel) {
                
                categoriaId = ((CategoriaModel) valorSelecionado).getId();
            } else if (valorSelecionado instanceof String) {
             
                String nomeDigitado = (String) valorSelecionado;
                categoriaId = listaCategorias.stream()
                        .filter(c -> c.getNome().equalsIgnoreCase(nomeDigitado.trim()))
                        .map(CategoriaModel::getId)
                        .findFirst()
                        .orElse(null);
            }

            if (categoriaId == null) {
                System.err.println("Selecione ou digite uma categoria válida.");
                return;
            }

            Integer quantidade = Integer.parseInt(txtQuantidade.getText().trim());
            Float precoCompra = maskService.moedaParaBigDecimal(txtPrecoCompra.getText()).floatValue();
            Float precoVenda = maskService.moedaParaBigDecimal(txtPrecoVenda.getText()).floatValue();

            boolean sucesso = produtoService.novo(nomeProduto, categoriaId, quantidade, precoCompra, precoVenda);

            if (sucesso) {
                limparFormularioProduto();
                mostrarSucesso("Produto \"" + nomeProduto.trim() + "\" cadastrado com sucesso!");
            }

        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter valores numéricos: " + e.getMessage());
        }
    }

    private void limparFormularioProduto() {
        txtNomeProduto.clear();

        atualizandoFiltro = true;
        try {
            cbCategoria.getSelectionModel().clearSelection();
            cbCategoria.setValue(null);
            cbCategoria.getEditor().clear();
            cbCategoria.setItems(listaCategorias);
        } finally {
            atualizandoFiltro = false;
        }

        txtPrecoCompra.clear();
        txtPrecoVenda.clear();
        txtQuantidade.clear();
    }

  


    public List<ProdutoModel> buscaEstoque() {
        return produtoService.buscaEstoque();
    } 

 
    @FXML public void voltarParaHome() { 
        
    }
}