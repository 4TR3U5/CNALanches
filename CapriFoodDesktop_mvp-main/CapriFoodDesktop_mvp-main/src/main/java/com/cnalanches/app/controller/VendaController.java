package com.cnalanches.app.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.cnalanches.app.model.ComandaItemModel;
import com.cnalanches.app.model.ItemVendaModel;
import com.cnalanches.app.model.ProdutoModel;
import com.cnalanches.app.service.ProdutoService;
import com.cnalanches.app.service.VendaService;

import javafx.beans.binding.Bindings;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VendaController {

    private static final String SEM_CATEGORIA = "Sem categoria";
    private static final int ESTOQUE_BAIXO = 5;
    private static final int MAX_DIGITOS_MESA = 4;

    /** Item do carrinho (ainda não lançado no banco). */
    private static class ItemCarrinho {
        final ProdutoModel produto;
        int qtd;

        ItemCarrinho(ProdutoModel produto, int qtd) {
            this.produto = produto;
            this.qtd = qtd;
        }

        double subtotal() {
            Float preco = produto.getPrecoVenda();
            return (preco == null ? 0 : preco) * qtd;
        }
    }

    @FXML private ToggleGroup grupoModo;
    @FXML private ToggleButton btnCaixa;
    @FXML private ToggleButton btnMesa;
    @FXML private Button btnMapa;
    @FXML private TextField txtBusca;
    @FXML private VBox conteudoProdutos;
    @FXML private Label lblTituloCarrinho;
    @FXML private HBox boxMesa;
    @FXML private TextField txtMesa;
    @FXML private Button btnAbrirMesa;
    @FXML private Label lblMesaAtual;
    @FXML private VBox conteudoCarrinho;
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cbPagamento;
    @FXML private HBox boxRecebido;
    @FXML private TextField txtRecebido;
    @FXML private Label lblTroco;
    @FXML private Button btnLancar;
    @FXML private Button btnFinalizar;
    @FXML private Button btnLimpar;

    private final ProdutoService produtoService = new ProdutoService();
    private final VendaService vendaService = new VendaService();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // ---- Produtos vindos do banco ----
    private final List<ProdutoModel> produtos = new ArrayList<>();

    // ---- Estado da venda ----
    private final List<ItemCarrinho> carrinhoCaixa = new ArrayList<>();
    private final Map<Integer, List<ItemCarrinho>> mesasAbertas = new TreeMap<>();
    private final List<ComandaItemModel> lancados = new ArrayList<>(); // itens já gravados na comanda
    private List<ItemCarrinho> carrinhoAtual = carrinhoCaixa;           // itens novos (não lançados)
    private boolean modoMesa = false;
    private Integer mesaAtual = null;
    private Integer mesaComandaId = null;
    private boolean fechandoConta = false;

    @FXML
    public void initialize() {
        cbPagamento.getItems().addAll("Dinheiro", "Pix", "Cartão");
        cbPagamento.valueProperty().addListener((obs, a, n) -> atualizarPagamento());

        configurarCampoMesa();

        // Espaço entre as categorias
        conteudoProdutos.setSpacing(18);
        conteudoProdutos.setPadding(new Insets(0, 4, 10, 0));

        txtBusca.textProperty().addListener((obs, a, n) -> montarProdutos());
        txtRecebido.textProperty().addListener((obs, a, n) -> atualizarTroco());

        // Impede ficar sem modo selecionado
        grupoModo.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null && antigo != null) antigo.setSelected(true);
        });

        carregarProdutos();
        atualizarCarrinho();
    }

    public List<ProdutoModel> buscaEstoque() {
        return produtoService.buscaEstoque();
    }

    /** Busca os produtos do banco fora da thread da tela. */
    private void carregarProdutos() {
        conteudoProdutos.getChildren().setAll(new Label("Carregando produtos..."));

        Task<List<ProdutoModel>> task = new Task<>() {
            @Override
            protected List<ProdutoModel> call() {
                return buscaEstoque();
            }
        };

        task.setOnSucceeded(e -> {
            produtos.clear();
            produtos.addAll(task.getValue());
            montarProdutos();
        });

        task.setOnFailed(e -> {
            conteudoProdutos.getChildren().setAll(new Label("Erro ao carregar produtos"));
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Busca os itens já lançados da comanda e atualiza a tela. */
    private void carregarLancados(int comandaId) {
        Task<List<ComandaItemModel>> task = new Task<>() {
            @Override
            protected List<ComandaItemModel> call() throws Exception {
                return vendaService.listarItensComanda(comandaId);
            }
        };

        task.setOnSucceeded(e -> {
            lancados.clear();
            lancados.addAll(task.getValue());
            atualizarTela();
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            erro("Não foi possível carregar os itens da mesa.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Campo da mesa: só aceita dígitos (digitando ou colando) e não pode ficar vazio.
     * O botão Abrir fica desativado com o campo vazio, zero, ou com mesa já aberta.
     */
    private void configurarCampoMesa() {
        UnaryOperator<TextFormatter.Change> somenteNumeros = change -> {
            String novoTexto = change.getControlNewText();
            return novoTexto.matches("\\d{0," + MAX_DIGITOS_MESA + "}") ? change : null;
        };
        txtMesa.setTextFormatter(new TextFormatter<>(somenteNumeros));

        btnAbrirMesa.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> mesaAtual != null || !mesaDigitadaValida(),
                        txtMesa.textProperty(), txtMesa.disableProperty()));
    }

    /** True se o campo tem um número maior que zero. */
    private boolean mesaDigitadaValida() {
        String txt = txtMesa.getText();
        if (txt == null || txt.isBlank()) return false;
        try {
            return Integer.parseInt(txt.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // =========================================================
    // MODOS
    // =========================================================

    /** True se existe uma venda em aberto (itens no caixa ou mesa aberta). */
    private boolean vendaEmAndamento() {
        return modoMesa ? mesaAtual != null : !carrinhoCaixa.isEmpty();
    }

    @FXML
    private void modoCaixa() {
        if (!modoMesa) return;

        // Itens já lançados ficam no banco; só bloqueia se houver itens novos sem lançar
        if (!carrinhoAtual.isEmpty()) {
            grupoModo.selectToggle(btnMesa);
            erro("Há itens novos que ainda não foram lançados. Lance ou remova antes de sair da mesa.");
            return;
        }

        modoMesa = false;
        mesaAtual = null;
        mesaComandaId = null;
        fechandoConta = false;
        mesasAbertas.clear();
        lancados.clear();
        boxMesa.setVisible(false);
        boxMesa.setManaged(false);
        lblMesaAtual.setVisible(false);
        lblMesaAtual.setManaged(false);
        txtMesa.clear();
        txtMesa.setDisable(false);
        cbPagamento.setValue(null);
        lblTituloCarrinho.setText("Venda no caixa");
        btnFinalizar.setText("Finalizar venda");
        btnLimpar.setText("Limpar carrinho");
        carrinhoAtual = carrinhoCaixa;
        atualizarTela();
    }

    @FXML
    private void modoMesa() {
        if (modoMesa) return;

        if (vendaEmAndamento()) {
            grupoModo.selectToggle(btnCaixa); // volta o botão pro modo atual
            erro("Há uma venda em andamento no caixa. Finalize ou limpe o carrinho antes de abrir uma mesa.");
            return;
        }

        entrarModoMesa();
        txtMesa.requestFocus();
    }

    /** Ajusta a tela para o modo mesa (sem mesa selecionada). */
    private void entrarModoMesa() {
        modoMesa = true;
        boxMesa.setVisible(true);
        boxMesa.setManaged(true);
        lblTituloCarrinho.setText("Venda na mesa");
        btnFinalizar.setText("Fechar conta");
        btnLimpar.setText("Cancelar mesa");
        selecionarMesa(null);
    }

    /** Chamado pelo mapa: abre a tela já em modo mesa, pronta pra digitar o número. */
    public void iniciarModoMesa() {
        grupoModo.selectToggle(btnMesa);
        entrarModoMesa();
        txtMesa.requestFocus();
    }

    /** Chamado pelo mapa: abre uma comanda que já existe no banco. */
    public void iniciarMesaExistente(int comandaId, int numeroMesa) {
        grupoModo.selectToggle(btnMesa);
        entrarModoMesa();

        mesaComandaId = comandaId;
        mesasAbertas.put(numeroMesa, new ArrayList<>());
        selecionarMesa(numeroMesa);
        carregarLancados(comandaId);
    }

    /** Abre a mesa digitada. Só uma mesa por vez e só se não houver comanda aberta no banco. */
    @FXML
    private void abrirMesa() {
        if (!mesaDigitadaValida()) {
            erro("Digite um número de mesa válido (maior que zero).");
            txtMesa.requestFocus();
            return;
        }

        if (mesaAtual != null) {
            erro("A mesa " + mesaAtual + " já está aberta. Feche a conta ou cancele antes de abrir outra.");
            return;
        }

        final int numero = Integer.parseInt(txtMesa.getText().trim());
        txtMesa.setDisable(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                if (vendaService.verificaMesa(numero)) return -1; // já existe comanda aberta
                return vendaService.abrirComandaMesa(numero);
            }
        };

        task.setOnSucceeded(e -> {
            txtMesa.setDisable(false);

            if (task.getValue() == -1) {
                // Só avisa e para: não abre nada, não mostra "mesa aberta"
                txtMesa.clear();
                erro("A mesa " + numero + " já possui uma comanda aberta.");
                txtMesa.requestFocus();
                return;
            }

            mesaComandaId = task.getValue();
            mesasAbertas.computeIfAbsent(numero, k -> new ArrayList<>());
            selecionarMesa(numero);
        });

        task.setOnFailed(e -> {
            txtMesa.setDisable(false);
            task.getException().printStackTrace();
            erro("Não foi possível abrir a mesa: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void selecionarMesa(Integer mesa) {
        fechandoConta = false;
        mesaAtual = mesa;
        carrinhoAtual = (mesa != null) ? mesasAbertas.get(mesa) : new ArrayList<>();
        if (mesa == null) lancados.clear();

        boolean aberta = mesa != null;
        lblMesaAtual.setText(aberta ? "Mesa " + mesa + " aberta" : "");
        lblMesaAtual.setVisible(aberta);
        lblMesaAtual.setManaged(aberta);
        if (aberta) txtMesa.clear();
        txtMesa.setDisable(aberta);

        atualizarTela();
    }

    @FXML
    private void abrirMapa(ActionEvent event) {
        if (modoMesa && !carrinhoAtual.isEmpty()) {
            Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                    "Há itens novos que ainda não foram lançados. Sair do mapa descarta esses itens. Continuar?",
                    ButtonType.YES, ButtonType.NO);
            c.setTitle("Itens não lançados");
            c.setHeaderText(null);
            if (c.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cnalanches/app/mapa-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CnaLanches - Mapa de salão");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // PRODUTOS
    // =========================================================
    private void montarProdutos() {
        conteudoProdutos.getChildren().clear();

        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().trim().toLowerCase();

        Map<String, List<ProdutoModel>> grupos = produtos.stream()
                .filter(p -> busca.isEmpty()
                        || (p.getNome() != null && p.getNome().toLowerCase().contains(busca)))
                .collect(Collectors.groupingBy(
                        p -> p.getNomeCategoria() == null ? SEM_CATEGORIA : p.getNomeCategoria(),
                        TreeMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<ProdutoModel>> grupo : grupos.entrySet()) {
            List<ProdutoModel> lista = grupo.getValue();

            // Cabeçalho da categoria: barra de destaque + nome + contador
            Region barra = new Region();
            barra.setMinSize(4, 20);
            barra.setPrefSize(4, 20);
            barra.setStyle("-fx-background-color: #007bff; -fx-background-radius: 2;");

            Label nomeCategoria = new Label(grupo.getKey());
            nomeCategoria.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2d3d;");

            Label contador = new Label(lista.size() + (lista.size() == 1 ? " item" : " itens"));
            contador.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #6c757d; -fx-font-size: 11px; "
                    + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 9 2 9;");

            HBox cabecalho = new HBox(10, barra, nomeCategoria, contador);
            cabecalho.setAlignment(Pos.CENTER_LEFT);
            cabecalho.setPadding(new Insets(0, 4, 0, 2));

            // Card único com as linhas da categoria
            VBox card = new VBox();
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                    + "-fx-border-color: #dfe3e8; -fx-border-radius: 10;");
            for (int i = 0; i < lista.size(); i++) {
                card.getChildren().add(criarLinhaProduto(lista.get(i), i == lista.size() - 1));
            }

            VBox secao = new VBox(8, cabecalho, card);
            conteudoProdutos.getChildren().add(secao);
        }

        if (conteudoProdutos.getChildren().isEmpty()) {
            Label vazio = new Label("Nenhum produto encontrado");
            vazio.setStyle("-fx-text-fill: #999999;");
            conteudoProdutos.getChildren().add(vazio);
        }
    }

    private HBox criarLinhaProduto(ProdutoModel p, boolean ultima) {
        int disponivel = disponivel(p);
        boolean indisponivel = disponivel <= 0;

        String textoNome = (p.getNome() == null || p.getNome().isBlank()) ? "(sem nome)" : p.getNome();
        Label nome = new Label(textoNome);
        nome.setStyle("-fx-font-size: 15px; -fx-text-fill: " + (indisponivel ? "#aaaaaa" : "#2b2f33") + ";");
        HBox.setHgrow(nome, Priority.ALWAYS);
        nome.setMaxWidth(Double.MAX_VALUE);

        // Selo de estoque
        String fundo;
        String texto;
        if (indisponivel) {
            fundo = "#fdecea";
            texto = "#c62828";
        } else if (disponivel <= ESTOQUE_BAIXO) {
            fundo = "#fff4e0";
            texto = "#b36b00";
        } else {
            fundo = "#e6f4ea";
            texto = "#1e7e34";
        }
        Label estoque = new Label(indisponivel ? "Esgotado" : disponivel + " un.");
        estoque.setStyle("-fx-background-color: " + fundo + "; -fx-text-fill: " + texto + "; "
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 3 10 3 10;");
        estoque.setMinWidth(80);
        estoque.setAlignment(Pos.CENTER);

        Label preco = new Label(p.getPrecoVenda() == null ? "-" : moeda.format(p.getPrecoVenda()));
        preco.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                + (indisponivel ? "#aaaaaa" : "#1f2d3d") + ";");
        preco.setMinWidth(100);
        preco.setAlignment(Pos.CENTER_RIGHT);

        Button add = new Button("+");
        add.setDisable(indisponivel);
        add.setMinWidth(32);
        add.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        add.setOnAction(e -> adicionar(p));

        HBox linha = new HBox(12, nome, estoque, preco, add);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(10, 16, 10, 16));

        // Divisória entre itens (a última linha não tem)
        String borda = ultima ? "transparent" : "#eef0f2";
        String estiloBase = "-fx-background-color: transparent; "
                + "-fx-border-color: transparent transparent " + borda + " transparent;";
        linha.setStyle(estiloBase);

        if (!indisponivel) {
            String estiloHover = "-fx-background-color: #f3f8ff; "
                    + "-fx-border-color: transparent transparent " + borda + " transparent; -fx-cursor: hand;";
            linha.setOnMouseEntered(e -> linha.setStyle(estiloHover));
            linha.setOnMouseExited(e -> linha.setStyle(estiloBase));
            linha.setOnMouseClicked(e -> adicionar(p));
        }
        return linha;
    }

    /** Estoque do produto menos o que já está lançado na comanda e no carrinho atual. */
    private int disponivel(ProdutoModel p) {
        int emEstoque = p.getQuantidade() == null ? 0 : p.getQuantidade();
        return emEstoque - qtdLancada(p) - qtdNoCarrinho(p);
    }

    private int qtdLancada(ProdutoModel p) {
        int soma = 0;
        for (ComandaItemModel i : lancados) {
            if (i.getProdutoId().equals(p.getId())) soma += i.getQuantidade();
        }
        return soma;
    }

    private int qtdNoCarrinho(ProdutoModel p) {
        for (ItemCarrinho i : carrinhoAtual) {
            if (i.produto.getId().equals(p.getId())) return i.qtd;
        }
        return 0;
    }

    // =========================================================
    // CARRINHO
    // =========================================================
    private void adicionar(ProdutoModel p) {
        if (modoMesa && mesaAtual == null) {
            erro("Digite o número da mesa e clique em Abrir antes de adicionar itens.");
            txtMesa.requestFocus();
            return;
        }
        if (disponivel(p) <= 0) {
            erro("Sem estoque disponível para \"" + p.getNome() + "\".");
            return;
        }

        for (ItemCarrinho i : carrinhoAtual) {
            if (i.produto.getId().equals(p.getId())) {
                i.qtd++;
                atualizarTela();
                return;
            }
        }
        carrinhoAtual.add(new ItemCarrinho(p, 1));
        atualizarTela();
    }

    private void atualizarTela() {
        montarProdutos();
        atualizarCarrinho();
    }

    private void atualizarCarrinho() {
        conteudoCarrinho.getChildren().clear();

        boolean semMesa = modoMesa && mesaAtual == null;
        boolean temLancados = !lancados.isEmpty();

        if (temLancados) {
            conteudoCarrinho.getChildren().add(subtitulo("Já lançados"));
            for (ComandaItemModel item : lancados) {
                conteudoCarrinho.getChildren().add(criarLinhaLancado(item));
            }
        }

        if (!carrinhoAtual.isEmpty()) {
            if (temLancados) conteudoCarrinho.getChildren().add(subtitulo("Novos (não lançados)"));
            for (ItemCarrinho item : carrinhoAtual) {
                conteudoCarrinho.getChildren().add(criarLinhaCarrinho(item));
            }
        }

        if (carrinhoAtual.isEmpty() && !temLancados) {
            Label vazio = new Label(semMesa ? "Digite o número da mesa e clique em Abrir" : "Carrinho vazio");
            vazio.setStyle("-fx-text-fill: #999999;");
            conteudoCarrinho.getChildren().add(vazio);
        }

        lblTotal.setText(moeda.format(total()));

        boolean mostrarLancar = modoMesa && mesaAtual != null;
        btnLancar.setVisible(mostrarLancar);
        btnLancar.setManaged(mostrarLancar);
        btnLancar.setDisable(carrinhoAtual.isEmpty());

        btnFinalizar.setDisable((carrinhoAtual.isEmpty() && !temLancados) || semMesa);
        btnLimpar.setDisable(semMesa || (!modoMesa && carrinhoAtual.isEmpty()));

        if (modoMesa) {
            btnFinalizar.setText(fechandoConta ? "Confirmar fechamento" : "Fechar conta");
        }
        atualizarPagamento();
    }

    private Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #888888;");
        l.setPadding(new Insets(4, 0, 0, 0));
        return l;
    }

    private HBox criarLinhaLancado(ComandaItemModel item) {
        Label nome = new Label(item.getQuantidade() + "x  " + item.getNome());
        nome.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777;");
        HBox.setHgrow(nome, Priority.ALWAYS);
        nome.setMaxWidth(Double.MAX_VALUE);

        Label sub = new Label(moeda.format(item.getSubtotal()));
        sub.setMinWidth(75);
        sub.setAlignment(Pos.CENTER_RIGHT);
        sub.setStyle("-fx-font-weight: bold; -fx-text-fill: #777777;");

        HBox linha = new HBox(6, nome, sub);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(6, 8, 6, 8));
        linha.setStyle("-fx-background-color: #eeeeee; -fx-background-radius: 6;");
        return linha;
    }

    private HBox criarLinhaCarrinho(ItemCarrinho item) {
        Label nome = new Label(item.produto.getNome());
        nome.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        HBox.setHgrow(nome, Priority.ALWAYS);
        nome.setMaxWidth(Double.MAX_VALUE);

        Button menos = botaoPequeno("−", "#6c757d");
        menos.setOnAction(e -> {
            item.qtd--;
            if (item.qtd <= 0) carrinhoAtual.remove(item);
            atualizarTela();
        });

        Label qtd = new Label(String.valueOf(item.qtd));
        qtd.setMinWidth(24);
        qtd.setAlignment(Pos.CENTER);
        qtd.setStyle("-fx-font-weight: bold;");

        Button mais = botaoPequeno("+", "#28a745");
        mais.setDisable(disponivel(item.produto) <= 0);
        mais.setOnAction(e -> adicionar(item.produto));

        Label sub = new Label(moeda.format(item.subtotal()));
        sub.setMinWidth(75);
        sub.setAlignment(Pos.CENTER_RIGHT);
        sub.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        Button remover = botaoPequeno("✕", "#dc3545");
        remover.setOnAction(e -> {
            carrinhoAtual.remove(item);
            atualizarTela();
        });

        HBox linha = new HBox(6, nome, menos, qtd, mais, sub, remover);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(6, 8, 6, 8));
        linha.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        return linha;
    }

    private Button botaoPequeno(String texto, String cor) {
        Button b = new Button(texto);
        b.setMinWidth(28);
        b.setStyle("-fx-background-color: " + cor + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        return b;
    }

    /** Total da conta: itens já lançados (mesa) + itens novos do carrinho. */
    private double total() {
        double t = 0;
        for (ComandaItemModel i : lancados) t += i.getSubtotal();
        for (ItemCarrinho i : carrinhoAtual) t += i.subtotal();
        return t;
    }

    // =========================================================
    // PAGAMENTO
    // =========================================================
    private void atualizarPagamento() {
        boolean mostrar = !modoMesa || fechandoConta;
        cbPagamento.setVisible(mostrar);
        cbPagamento.setManaged(mostrar);

        boolean dinheiro = mostrar && "Dinheiro".equals(cbPagamento.getValue());
        boxRecebido.setVisible(dinheiro);
        boxRecebido.setManaged(dinheiro);
        if (!dinheiro) txtRecebido.clear();
        atualizarTroco();
    }

    private Double recebido() {
        String txt = txtRecebido.getText();
        if (txt == null || txt.isBlank()) return null;
        try {
            return Double.parseDouble(txt.trim().replace("R$", "").replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void atualizarTroco() {
        Double rec = recebido();
        double troco = (rec == null) ? 0 : Math.max(0, rec - total());
        lblTroco.setText("Troco: " + moeda.format(troco));
    }

    // =========================================================
    // LANÇAR / FINALIZAR / LIMPAR
    // =========================================================
    private List<ItemVendaModel> itensDoCarrinho(List<ItemCarrinho> origem) {
        List<ItemVendaModel> itens = new ArrayList<>();
        for (ItemCarrinho i : origem) {
            itens.add(new ItemVendaModel(
                    i.produto.getId(), i.qtd,
                    i.produto.getPrecoCompra(), i.produto.getPrecoVenda()));
        }
        return itens;
    }

    /** Grava os itens novos na comanda da mesa (sem fechar a conta). */
    @FXML
    private void lancarItens() {
        if (!modoMesa || mesaComandaId == null || carrinhoAtual.isEmpty()) return;

        final int comandaId = mesaComandaId;
        final List<ItemVendaModel> itens = itensDoCarrinho(carrinhoAtual);

        btnLancar.setDisable(true);
        btnFinalizar.setDisable(true);
        btnLimpar.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                vendaService.lancarItens(comandaId, itens);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            fechandoConta = false;
            carrinhoAtual.clear();
            carregarLancados(comandaId); // recarrega do banco e atualiza a tela
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            atualizarCarrinho(); // reabilita os botões
            erro("Não foi possível lançar os itens: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void finalizar() {
        if (carrinhoAtual.isEmpty() && lancados.isEmpty()) return;

        if (modoMesa && mesaAtual == null) {
            erro("Abra uma mesa antes de fechar a conta.");
            return;
        }

        // Mesa: o primeiro clique só abre a escolha do pagamento
        if (modoMesa && !fechandoConta) {
            if (!carrinhoAtual.isEmpty()) {
                erro("Lance os itens novos antes de fechar a conta.");
                return;
            }
            fechandoConta = true;
            atualizarCarrinho();
            return;
        }

        String pagamento = cbPagamento.getValue();
        if (pagamento == null) {
            erro("Escolha a forma de pagamento.");
            return;
        }

        double total = total();
        if ("Dinheiro".equals(pagamento)) {
            Double rec = recebido();
            if (rec != null && rec < total) {
                erro("O valor recebido é menor que o total.");
                return;
            }
        }

        String titulo = modoMesa ? "Fechar conta da mesa " + mesaAtual : "Finalizar venda";
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                "Total: " + moeda.format(total) + "\nPagamento: " + pagamento,
                ButtonType.YES, ButtonType.NO);
        confirmacao.setTitle(titulo);
        confirmacao.setHeaderText(titulo + "?");
        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        if (!modoMesa) {
            registrarVendaCaixa(pagamento, total);
        } else {
            fecharMesa(pagamento);
        }
    }

    /** Grava a venda do caixa no banco (transação no DAO) fora da thread da tela. */
    private void registrarVendaCaixa(String pagamento, double total) {
        final List<ItemVendaModel> itens = itensDoCarrinho(carrinhoCaixa);
        final String codigoPagamento = codigoPagamento(pagamento);

        btnFinalizar.setDisable(true);
        btnLimpar.setDisable(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return vendaService.registrarVendaCaixa(itens, total, codigoPagamento);
            }
        };

        task.setOnSucceeded(e -> {
            carrinhoCaixa.clear();
            cbPagamento.setValue(null);
            txtRecebido.clear();
            atualizarCarrinho();
            carregarProdutos(); // estoque atualizado vindo do banco

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Venda #" + task.getValue() + " registrada.");
            ok.setTitle("Sucesso");
            ok.setHeaderText(null);
            ok.showAndWait();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            atualizarCarrinho(); // reabilita os botões
            carregarProdutos();  // pode ter sido falta de estoque, então recarrega
            erro("Não foi possível registrar a venda: " + ex.getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Fecha a comanda da mesa: grava pendentes, baixa estoque e fecha (transação no DAO). */
    private void fecharMesa(String pagamento) {
        final List<ItemVendaModel> pendentes = itensDoCarrinho(carrinhoAtual);
        final int comandaId = mesaComandaId;
        final int numeroMesa = mesaAtual;
        final String codigoPagamento = codigoPagamento(pagamento);

        btnLancar.setDisable(true);
        btnFinalizar.setDisable(true);
        btnLimpar.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                vendaService.fecharComandaMesa(comandaId, pendentes, codigoPagamento);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            mesasAbertas.remove(numeroMesa);
            mesaComandaId = null;
            selecionarMesa(null);
            cbPagamento.setValue(null);
            txtRecebido.clear();
            carregarProdutos();

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Conta da mesa " + numeroMesa + " fechada.");
            ok.setTitle("Sucesso");
            ok.setHeaderText(null);
            ok.showAndWait();
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            atualizarCarrinho(); // reabilita os botões
            carregarProdutos();
            erro("Não foi possível fechar a conta: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String codigoPagamento(String pagamento) {
        if ("Dinheiro".equals(pagamento)) return "D";
        if ("Pix".equals(pagamento)) return "P";
        return "C"; // Cartão
    }

    @FXML
    private void limpar() {
        if (modoMesa) {
            Integer mesa = mesaAtual;
            if (mesa == null) return;
            Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                    "Cancelar a mesa " + mesa + "? Todos os itens (lançados e novos) serão descartados.",
                    ButtonType.YES, ButtonType.NO);
            c.setTitle("Cancelar mesa");
            c.setHeaderText(null);
            if (c.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

            final int comandaId = mesaComandaId;
            btnLancar.setDisable(true);
            btnFinalizar.setDisable(true);
            btnLimpar.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    vendaService.cancelarComandaMesa(comandaId);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                mesasAbertas.remove(mesa);
                mesaComandaId = null;
                selecionarMesa(null);
            });

            task.setOnFailed(e -> {
                task.getException().printStackTrace();
                atualizarCarrinho();
                erro("Não foi possível cancelar a mesa: " + task.getException().getMessage());
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        } else {
            carrinhoCaixa.clear();
            atualizarTela();
        }
    }

    private void erro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Atenção");
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
}