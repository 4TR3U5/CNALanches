package com.cnalanches.app.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.cnalanches.app.model.ComandaItemModel;
import com.cnalanches.app.model.VendaResumoModel;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import com.cnalanches.app.model.ItemRelatorioModel;
import com.cnalanches.app.service.RelatorioExcelService;
import javafx.stage.FileChooser;

public class RelatorioController {

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private Button btnFiltrar;
    @FXML private Label lblStatus;
    @FXML private Label lblTotal;
    @FXML private Label lblQtd;
    @FXML private Label lblTicket;
    @FXML private Label lblDinheiro;
    @FXML private Label lblPix;
    @FXML private Label lblCartao;
    @FXML private Label lblCaixa;
    @FXML private Label lblMesa;
    @FXML private VBox listaVendas;
    @FXML private Button btnExcel;

    private final VendaService vendaService = new VendaService();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final RelatorioExcelService excelService = new RelatorioExcelService();


    @FXML
    public void initialize() {
        periodoHoje();
    }

    // ---------- Atalhos de período ----------
    @FXML
    private void periodoHoje() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje);
        dpFim.setValue(hoje);
        filtrar();
    }

    @FXML
    private void periodo7Dias() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje.minusDays(6));
        dpFim.setValue(hoje);
        filtrar();
    }

    @FXML
    private void periodoMes() {
        LocalDate hoje = LocalDate.now();
        dpInicio.setValue(hoje.withDayOfMonth(1));
        dpFim.setValue(hoje);
        filtrar();
    }

    // ---------- Consulta ----------
    @FXML
    private void filtrar() {
        final LocalDate inicio = dpInicio.getValue();
        final LocalDate fim = dpFim.getValue();

        if (inicio == null || fim == null) {
            erro("Escolha a data inicial e a final.");
            return;
        }
        if (fim.isBefore(inicio)) {
            erro("A data final não pode ser anterior à inicial.");
            return;
        }

        btnFiltrar.setDisable(true);
        lblStatus.setText("Carregando...");

        Task<List<VendaResumoModel>> task = new Task<>() {
            @Override
            protected List<VendaResumoModel> call() throws Exception {
                return vendaService.listarVendas(inicio, fim);
            }
        };

        task.setOnSucceeded(e -> {
            mostrar(task.getValue());
            btnFiltrar.setDisable(false);
            lblStatus.setText("");
        });

        task.setOnFailed(e -> {
            btnFiltrar.setDisable(false);
            lblStatus.setText("Erro ao carregar");
            task.getException().printStackTrace();
            erro("Não foi possível carregar o relatório: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ---------- Exibição ----------
    private void mostrar(List<VendaResumoModel> vendas) {
        double total = soma(vendas, v -> true);
        int qtd = vendas.size();

        lblTotal.setText(moeda.format(total));
        lblQtd.setText(String.valueOf(qtd));
        lblTicket.setText(moeda.format(qtd == 0 ? 0 : total / qtd));

        lblDinheiro.setText(moeda.format(soma(vendas, v -> "D".equals(v.getPagamento()))));
        lblPix.setText(moeda.format(soma(vendas, v -> "P".equals(v.getPagamento()))));
        lblCartao.setText(moeda.format(soma(vendas, v -> "C".equals(v.getPagamento()))));

        lblCaixa.setText(moeda.format(soma(vendas, v -> "C".equals(v.getTipo()))));
        lblMesa.setText(moeda.format(soma(vendas, v -> "M".equals(v.getTipo()))));

        listaVendas.getChildren().clear();
        if (vendas.isEmpty()) {
            Label vazio = new Label("Nenhuma venda no período.");
            vazio.setStyle("-fx-text-fill: #999999;");
            listaVendas.getChildren().add(vazio);
            return;
        }
        for (VendaResumoModel v : vendas) {
            listaVendas.getChildren().add(criarLinha(v));
        }
    }

    private double soma(List<VendaResumoModel> vendas, Predicate<VendaResumoModel> filtro) {
        double t = 0;
        for (VendaResumoModel v : vendas) {
            if (filtro.test(v)) t += v.getTotal();
        }
        return t;
    }

    private HBox criarLinha(VendaResumoModel v) {
        Label id = new Label("#" + v.getComandaId());
        id.setMinWidth(60);
        id.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2d3d;");

        Label data = new Label(v.getFechamento().format(formatoData));
        data.setMinWidth(130);
        data.setStyle("-fx-text-fill: #555555;");

        String tipoTxt = "M".equals(v.getTipo())
                ? "Mesa " + (v.getMesa() == null ? "" : v.getMesa())
                : "Caixa";
        Label tipo = new Label(tipoTxt);
        tipo.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 10 2 10;");
        tipo.setMinWidth(80);
        tipo.setAlignment(Pos.CENTER);

        Label pagamento = new Label(nomePagamento(v.getPagamento()));
        pagamento.setMinWidth(80);
        pagamento.setStyle("-fx-text-fill: #555555;");
        HBox.setHgrow(pagamento, Priority.ALWAYS);
        pagamento.setMaxWidth(Double.MAX_VALUE);

        Label valor = new Label(moeda.format(v.getTotal()));
        valor.setMinWidth(110);
        valor.setAlignment(Pos.CENTER_RIGHT);
        valor.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");

        HBox linha = new HBox(14, id, data, tipo, pagamento, valor);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPadding(new Insets(10, 16, 10, 16));
        String base = "-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #e6e6e6; -fx-border-radius: 8; -fx-cursor: hand;";
        linha.setStyle(base);
        linha.setOnMouseEntered(e -> linha.setStyle(base.replace("white", "#f3f8ff")));
        linha.setOnMouseExited(e -> linha.setStyle(base));
        linha.setOnMouseClicked(e -> abrirItens(v));
        return linha;
    }

    private String nomePagamento(String codigo) {
        if ("D".equals(codigo)) return "Dinheiro";
        if ("P".equals(codigo)) return "Pix";
        if ("C".equals(codigo)) return "Cartão";
        return "-";
    }

    // ---------- Itens da venda ----------
    private void abrirItens(VendaResumoModel venda) {
        Task<List<ComandaItemModel>> task = new Task<>() {
            @Override
            protected List<ComandaItemModel> call() throws Exception {
                return vendaService.listarItensComanda(venda.getComandaId());
            }
        };

        task.setOnSucceeded(e -> {
            StringBuilder sb = new StringBuilder();
            for (ComandaItemModel i : task.getValue()) {
                sb.append(i.getQuantidade()).append("x  ").append(i.getNome())
                  .append("  —  ").append(moeda.format(i.getSubtotal())).append("\n");
            }
            sb.append("\nTotal: ").append(moeda.format(venda.getTotal()));

            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            a.setTitle("Venda #" + venda.getComandaId());
            a.setHeaderText(venda.getFechamento().format(formatoData) + "  •  " + nomePagamento(venda.getPagamento()));
            a.showAndWait();
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            erro("Não foi possível carregar os itens da venda.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void erro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }



    @FXML
    private void exportarExcel() {
        final LocalDate inicio = dpInicio.getValue();
        final LocalDate fim = dpFim.getValue();

        if (inicio == null || fim == null) {
            erro("Escolha a data inicial e a final.");
            return;
        }
        if (fim.isBefore(inicio)) {
            erro("A data final não pode ser anterior à inicial.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório");
        chooser.setInitialFileName("relatorio-vendas_" + inicio + "_a_" + fim + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File arquivo = chooser.showSaveDialog(dpInicio.getScene().getWindow());
        if (arquivo == null) return;

        btnExcel.setDisable(true);
        lblStatus.setText("Gerando Excel...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<ItemRelatorioModel> itens = vendaService.listarItensVendidos(inicio, fim);
                excelService.exportar(arquivo, inicio, fim, itens);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            btnExcel.setDisable(false);
            lblStatus.setText("");
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Relatório salvo em:\n" + arquivo.getAbsolutePath());
            ok.setTitle("Sucesso");
            ok.setHeaderText(null);
            ok.showAndWait();
        });

        task.setOnFailed(e -> {
            btnExcel.setDisable(false);
            lblStatus.setText("Erro ao gerar");
            task.getException().printStackTrace();
            erro("Não foi possível gerar o Excel: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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