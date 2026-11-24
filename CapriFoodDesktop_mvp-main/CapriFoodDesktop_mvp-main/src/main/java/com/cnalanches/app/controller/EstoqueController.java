package com.cnalanches.app.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.cnalanches.app.helper.AcaoProduto;
import com.cnalanches.app.model.CategoriaModel;
import com.cnalanches.app.model.ProdutoModel;
import com.cnalanches.app.service.ProdutoService;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EstoqueController {

    private static final String TODAS = "Todas";
    private static final String SEM_CATEGORIA = "Sem categoria";
    private static final int ESTOQUE_BAIXO = 5;

    @FXML private ListView<String> listaCategorias;
    @FXML private VBox conteudoMenu;
    @FXML private Button btnAtualizar;
    @FXML private Label lblStatus;
    @FXML private Label lblTitulo;
    @FXML private ToggleGroup grupoVisao;
    @FXML private ToggleButton btnAtivos;
    @FXML private ToggleButton btnInativos;

    private final ProdutoService produtoService = new ProdutoService();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private List<ProdutoModel> todos = List.of();
    private boolean modoInativos = false;

    @FXML
    public void initialize() {
        listaCategorias.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                montarMenu();
            }
        });

        // Impede ficar sem nenhuma opção selecionada (Ativos/Inativos)
        grupoVisao.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null && antigo != null) {
                antigo.setSelected(true);
            }
        });

        carregarEstoque();
    }

    public List<ProdutoModel> buscaItensInativos() {
        return produtoService.buscaItensInativos();
    }

    @FXML
    private void mostrarAtivos() {
        if (!modoInativos) return;
        modoInativos = false;
        lblTitulo.setText("Cardápio / Estoque");
        listaCategorias.getSelectionModel().clearSelection();
        carregarEstoque();
    }

    @FXML
    private void mostrarInativos() {
        if (modoInativos) return;
        modoInativos = true;
        lblTitulo.setText("Itens inativos");
        listaCategorias.getSelectionModel().clearSelection();
        carregarEstoque();
    }

    @FXML
    public void carregarEstoque() {
        btnAtualizar.setDisable(true);
        lblStatus.setText("Carregando...");

        final boolean inativos = modoInativos;

        Task<List<ProdutoModel>> task = new Task<>() {
            @Override
            protected List<ProdutoModel> call() {
                return inativos ? buscaItensInativos() : produtoService.buscaEstoque();
            }
        };

        task.setOnSucceeded(e -> {
            todos = task.getValue();

            String anterior = listaCategorias.getSelectionModel().getSelectedItem();
            Map<String, List<ProdutoModel>> grupos = agruparPorCategoria();

            listaCategorias.getItems().clear();
            listaCategorias.getItems().add(TODAS);
            listaCategorias.getItems().addAll(grupos.keySet());

            if (anterior != null && listaCategorias.getItems().contains(anterior)) {
                listaCategorias.getSelectionModel().select(anterior);
            } else {
                listaCategorias.getSelectionModel().select(TODAS);
            }
            montarMenu();

            btnAtualizar.setDisable(false);
            lblStatus.setText(todos.size() + (inativos ? " inativo(s)" : " produto(s)"));
        });

        task.setOnFailed(e -> {
            btnAtualizar.setDisable(false);
            lblStatus.setText("Erro ao carregar estoque");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Função única de ação: pede o valor ao usuário conforme a ação,
     * chama o service e recarrega a tela.
     */
    private void executarAcao(AcaoProduto acao, ProdutoModel produto) {
        Object valor = null;

        if (acao == AcaoProduto.ALTERAR_PRECO) {
            Optional<String> r = pedirTexto("Alterar preço", produto.getNome(), "Novo preço de venda:",
                    produto.getPrecoVenda() == null ? "" : String.valueOf(produto.getPrecoVenda()));
            if (r.isEmpty()) return;
            try {
                valor = Float.parseFloat(r.get().trim().replace("R$", "").replace(",", ".").trim());
            } catch (NumberFormatException ex) {
                erro("Preço inválido.");
                return;
            }
            if ((Float) valor < 0) {
                erro("O preço não pode ser negativo.");
                return;
            }

        } else if (acao == AcaoProduto.ALTERAR_QUANTIDADE) {
            Optional<String> r = pedirTexto("Alterar quantidade", produto.getNome(), "Nova quantidade:",
                    String.valueOf(produto.getQuantidade() == null ? 0 : produto.getQuantidade()));
            if (r.isEmpty()) return;
            try {
                valor = Integer.parseInt(r.get().trim());
            } catch (NumberFormatException ex) {
                erro("Quantidade inválida.");
                return;
            }
            if ((Integer) valor < 0) {
                erro("A quantidade não pode ser negativa.");
                return;
            }

        } else if (acao == AcaoProduto.ALTERAR_CATEGORIA) {
            Map<String, Integer> categorias = new LinkedHashMap<>();
            for (CategoriaModel c : produtoService.obterCategorias()) {
                categorias.put(c.getNome(), c.getId());
            }
            if (categorias.isEmpty()) {
                erro("Nenhuma categoria cadastrada.");
                return;
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(produto.getNomeCategoria(), categorias.keySet());
            dialog.setTitle("Alterar categoria");
            dialog.setHeaderText(produto.getNome());
            dialog.setContentText("Nova categoria:");
            Optional<String> r = dialog.showAndWait();
            if (r.isEmpty()) return;
            valor = categorias.get(r.get());

        } else if (acao == AcaoProduto.INATIVAR) {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Inativar \"" + produto.getNome() + "\"? Ele some do estoque.",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setTitle("Inativar produto");
            confirmacao.setHeaderText(null);
            if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }else if (acao == AcaoProduto.ATIVAR) {
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Ativar \"" + produto.getNome() + "\"? Ele volta para o estoque.",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setTitle("Ativar produto");
            confirmacao.setHeaderText(null);
            if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        final Object valorFinal = valor;
        lblStatus.setText("Salvando...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return produtoService.atualizarProduto(acao, produto.getId(), valorFinal);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                carregarEstoque();
            } else {
                lblStatus.setText("Não foi possível salvar");
                erro("Não foi possível salvar a alteração.");
            }
        });

        task.setOnFailed(e -> {
            lblStatus.setText("Erro ao salvar");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private Optional<String> pedirTexto(String titulo, String cabecalho, String conteudo, String padrao) {
        TextInputDialog dialog = new TextInputDialog(padrao);
        dialog.setTitle(titulo);
        dialog.setHeaderText(cabecalho);
        dialog.setContentText(conteudo);
        return dialog.showAndWait();
    }

    private void erro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private Map<String, List<ProdutoModel>> agruparPorCategoria() {
        return todos.stream().collect(Collectors.groupingBy(
                p -> p.getNomeCategoria() == null ? SEM_CATEGORIA : p.getNomeCategoria(),
                TreeMap::new,
                Collectors.toList()));
    }

    private void montarMenu() {
        conteudoMenu.getChildren().clear();

        String selecionada = listaCategorias.getSelectionModel().getSelectedItem();
        boolean mostrarTodas = selecionada == null || TODAS.equals(selecionada);

        for (Map.Entry<String, List<ProdutoModel>> grupo : agruparPorCategoria().entrySet()) {
            if (!mostrarTodas && !grupo.getKey().equals(selecionada)) {
                continue;
            }

            Label titulo = new Label(grupo.getKey());
            titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (modoInativos ? "#6c757d" : "#007bff") + ";");
            titulo.setPadding(new Insets(10, 0, 2, 0));
            conteudoMenu.getChildren().add(titulo);

            for (ProdutoModel produto : grupo.getValue()) {
                conteudoMenu.getChildren().add(criarLinhaProduto(produto));
            }
        }

        if (conteudoMenu.getChildren().isEmpty()) {
            conteudoMenu.getChildren().add(new Label(
                    modoInativos ? "Nenhum item inativo" : "Nenhum produto encontrado"));
        }
    }

    private HBox criarLinhaProduto(ProdutoModel produto) {
        Label nome = new Label(produto.getNome());
        nome.setStyle("-fx-font-size: 15px; -fx-text-fill: " + (modoInativos ? "#888888" : "#333333") + ";");
        HBox.setHgrow(nome, Priority.ALWAYS);
        nome.setMaxWidth(Double.MAX_VALUE);

        int qtd = produto.getQuantidade() == null ? 0 : produto.getQuantidade();
        Label estoque = new Label(qtd == 0 ? "Esgotado" : qtd + " un.");
        String cor = modoInativos ? "#888888"
                : (qtd == 0 ? "#dc3545" : (qtd <= ESTOQUE_BAIXO ? "#e69500" : "#28a745"));
        estoque.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");
        estoque.setMinWidth(80);
        estoque.setAlignment(Pos.CENTER_RIGHT);

        Float venda = produto.getPrecoVenda();
        Label preco = new Label(venda == null ? "-" : moeda.format(venda));
        preco.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                + (modoInativos ? "#888888" : "#333333") + ";");
        preco.setMinWidth(90);
        preco.setAlignment(Pos.CENTER_RIGHT);

        HBox linha;
        if (modoInativos) {
           Label selo = new Label("Inativo");
           selo.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #6c757d; -fx-font-size: 12px; "
                    + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 10 2 10;");
            Button btnAtivar = botaoAcao("Ativar", "#28a745", AcaoProduto.ATIVAR, produto);
            linha = new HBox(10, nome, estoque, preco, selo, btnAtivar);
        } else {
            Button btnPreco = botaoAcao("Preço", "#007bff", AcaoProduto.ALTERAR_PRECO, produto);
            Button btnQtd = botaoAcao("Qtd", "#17a2b8", AcaoProduto.ALTERAR_QUANTIDADE, produto);
            Button btnCat = botaoAcao("Categoria", "#6c757d", AcaoProduto.ALTERAR_CATEGORIA, produto);
            Button btnInativar = botaoAcao("Inativar", "#dc3545", AcaoProduto.INATIVAR, produto);
            linha = new HBox(10, nome, estoque, preco, btnPreco, btnQtd, btnCat, btnInativar);
        }

        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(10, 15, 10, 15));
        linha.setStyle("-fx-background-color: " + (modoInativos ? "#fafafa" : "white") + "; "
                + "-fx-background-radius: 8; -fx-border-color: #e6e6e6; -fx-border-radius: 8;");
        return linha;
    }

    private Button botaoAcao(String texto, String cor, AcaoProduto acao, ProdutoModel produto) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + cor + "; -fx-text-fill: white; "
                + "-fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> executarAcao(acao, produto));
        return btn;
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
}