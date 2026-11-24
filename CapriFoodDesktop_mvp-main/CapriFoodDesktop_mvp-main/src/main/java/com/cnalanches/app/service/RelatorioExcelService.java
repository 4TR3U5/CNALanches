package com.cnalanches.app.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.cnalanches.app.model.ItemRelatorioModel;

public class RelatorioExcelService {

    /** Acumulador de quantidade, receita e custo. */
    private static class Agg {
        int qtd;
        double receita;
        double custo;
        String categoria = "";

        void add(ItemRelatorioModel i) {
            qtd += i.getQuantidade();
            receita += i.getReceita();
            custo += i.getCusto();
        }

        double lucro() { return receita - custo; }
        double margem() { return receita == 0 ? 0 : lucro() / receita; }
    }

    private CellStyle titulo, cabecalho, dinheiro, dinheiroNegrito, data, percentual, negrito, inteiro;

    public void exportar(File arquivo, LocalDate inicio, LocalDate fim, List<ItemRelatorioModel> itens)
            throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = new FileOutputStream(arquivo)) {
            criarEstilos(wb);
            abaResumo(wb.createSheet("Resumo"), inicio, fim, itens);
            abaCategorias(wb.createSheet("Lucro por categoria"), itens);
            abaItens(wb.createSheet("Itens vendidos"), itens);
            abaSaidas(wb.createSheet("Saídas"), itens);
            wb.write(out);
        }
    }

    // =========================================================
    // ABAS
    // =========================================================
    private void abaResumo(Sheet s, LocalDate inicio, LocalDate fim, List<ItemRelatorioModel> itens) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int r = 0;

        texto(s.createRow(r++), 0, "Relatório de vendas", titulo);
        texto(s.createRow(r++), 0, "Período: " + inicio.format(f) + " a " + fim.format(f), negrito);
        r++;

        Agg total = new Agg();
        Set<Integer> vendas = new HashSet<>();
        Map<String, Agg> porPagamento = new LinkedHashMap<>();
        porPagamento.put("Dinheiro", new Agg());
        porPagamento.put("Pix", new Agg());
        porPagamento.put("Cartão", new Agg());
        porPagamento.put("Não informado", new Agg());
        Map<String, Agg> porTipo = new LinkedHashMap<>();
        porTipo.put("Caixa", new Agg());
        porTipo.put("Mesa", new Agg());

        for (ItemRelatorioModel i : itens) {
            total.add(i);
            vendas.add(i.getComandaId());
            porPagamento.get(nomePagamento(i.getPagamento())).add(i);
            porTipo.get("M".equals(i.getTipo()) ? "Mesa" : "Caixa").add(i);
        }

        int nVendas = vendas.size();

        cabecalho(s.createRow(r++), "Indicador", "Valor");
        linha(s, r++, "Receita total", total.receita, dinheiro);
        linha(s, r++, "Custo total (mercadoria vendida)", total.custo, dinheiro);
        linha(s, r++, "LUCRO TOTAL", total.lucro(), dinheiroNegrito);
        linha(s, r++, "Margem de lucro", total.margem(), percentual);
        linha(s, r++, "Nº de vendas", nVendas, inteiro);
        linha(s, r++, "Ticket médio", nVendas == 0 ? 0 : total.receita / nVendas, dinheiro);
        linha(s, r++, "Itens vendidos (unidades)", total.qtd, inteiro);
        r++;

        r = blocoAgrupado(s, r, "Por forma de pagamento", porPagamento);
        r++;
        blocoAgrupado(s, r, "Por tipo de venda", porTipo);

        s.setColumnWidth(0, 12000);
        s.setColumnWidth(1, 5000);
        s.setColumnWidth(2, 5000);
        s.setColumnWidth(3, 5000);
        s.setColumnWidth(4, 4000);
    }

    private int blocoAgrupado(Sheet s, int r, String nome, Map<String, Agg> grupos) {
        texto(s.createRow(r++), 0, nome, negrito);
        cabecalho(s.createRow(r++), "", "Receita", "Custo", "Lucro", "Margem");
        for (Map.Entry<String, Agg> g : grupos.entrySet()) {
            Agg a = g.getValue();
            if (a.qtd == 0 && g.getKey().equals("Não informado")) continue;
            Row row = s.createRow(r++);
            texto(row, 0, g.getKey(), null);
            numero(row, 1, a.receita, dinheiro);
            numero(row, 2, a.custo, dinheiro);
            numero(row, 3, a.lucro(), dinheiro);
            numero(row, 4, a.margem(), percentual);
        }
        return r;
    }

    private void abaCategorias(Sheet s, List<ItemRelatorioModel> itens) {
        Map<String, Agg> porCategoria = new TreeMap<>();
        for (ItemRelatorioModel i : itens) {
            porCategoria.computeIfAbsent(i.getCategoria(), k -> new Agg()).add(i);
        }

        int r = 0;
        cabecalho(s.createRow(r++), "Categoria", "Qtd vendida", "Receita", "Custo", "Lucro", "Margem");

        Agg total = new Agg();
        for (Map.Entry<String, Agg> e : porCategoria.entrySet()) {
            Agg a = e.getValue();
            total.qtd += a.qtd;
            total.receita += a.receita;
            total.custo += a.custo;

            Row row = s.createRow(r++);
            texto(row, 0, e.getKey(), null);
            numero(row, 1, a.qtd, inteiro);
            numero(row, 2, a.receita, dinheiro);
            numero(row, 3, a.custo, dinheiro);
            numero(row, 4, a.lucro(), dinheiro);
            numero(row, 5, a.margem(), percentual);
        }

        Row row = s.createRow(r);
        texto(row, 0, "TOTAL", negrito);
        numero(row, 1, total.qtd, inteiro);
        numero(row, 2, total.receita, dinheiroNegrito);
        numero(row, 3, total.custo, dinheiroNegrito);
        numero(row, 4, total.lucro(), dinheiroNegrito);
        numero(row, 5, total.margem(), percentual);

        s.setColumnWidth(0, 9000);
        for (int c = 1; c <= 5; c++) s.setColumnWidth(c, 4500);
        s.createFreezePane(0, 1);
    }

    private void abaItens(Sheet s, List<ItemRelatorioModel> itens) {
        int r = 0;
        cabecalho(s.createRow(r++), "Data/hora", "Venda", "Tipo", "Mesa", "Pagamento", "Produto",
                "Categoria", "Qtd", "Custo unit.", "Preço unit.", "Receita", "Custo", "Lucro");

        for (ItemRelatorioModel i : itens) {
            Row row = s.createRow(r++);
            Cell c0 = row.createCell(0);
            c0.setCellValue(i.getFechamento());
            c0.setCellStyle(data);
            numero(row, 1, i.getComandaId(), inteiro);
            texto(row, 2, "M".equals(i.getTipo()) ? "Mesa" : "Caixa", null);
            if (i.getMesa() != null) numero(row, 3, i.getMesa(), inteiro);
            texto(row, 4, nomePagamento(i.getPagamento()), null);
            texto(row, 5, i.getProduto(), null);
            texto(row, 6, i.getCategoria(), null);
            numero(row, 7, i.getQuantidade(), inteiro);
            numero(row, 8, i.getPrecoCusto(), dinheiro);
            numero(row, 9, i.getPrecoVenda(), dinheiro);
            numero(row, 10, i.getReceita(), dinheiro);
            numero(row, 11, i.getCusto(), dinheiro);
            numero(row, 12, i.getLucro(), dinheiro);
        }

        int[] larguras = {4800, 2500, 2800, 2200, 3600, 8000, 5500, 2200, 3600, 3600, 3800, 3800, 3800};
        for (int c = 0; c < larguras.length; c++) s.setColumnWidth(c, larguras[c]);
        s.createFreezePane(0, 1);
        s.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, r - 1), 0, 12));
    }

    /** Saída de estoque por produto no período. */
    private void abaSaidas(Sheet s, List<ItemRelatorioModel> itens) {
        Map<String, Agg> porProduto = new LinkedHashMap<>();
        for (ItemRelatorioModel i : itens) {
            String chave = i.getProduto() + "||" + i.getCategoria();
            Agg a = porProduto.computeIfAbsent(chave, k -> new Agg());
            a.categoria = i.getCategoria();
            a.add(i);
        }

        List<Map.Entry<String, Agg>> ordenado = new java.util.ArrayList<>(porProduto.entrySet());
        ordenado.sort((x, y) -> Integer.compare(y.getValue().qtd, x.getValue().qtd));

        int r = 0;
        cabecalho(s.createRow(r++), "Produto", "Categoria", "Qtd que saiu do estoque",
                "Custo da saída", "Receita", "Lucro");

        int totalQtd = 0;
        double totalCusto = 0;
        for (Map.Entry<String, Agg> e : ordenado) {
            Agg a = e.getValue();
            totalQtd += a.qtd;
            totalCusto += a.custo;

            Row row = s.createRow(r++);
            texto(row, 0, e.getKey().split("\\|\\|")[0], null);
            texto(row, 1, a.categoria, null);
            numero(row, 2, a.qtd, inteiro);
            numero(row, 3, a.custo, dinheiro);
            numero(row, 4, a.receita, dinheiro);
            numero(row, 5, a.lucro(), dinheiro);
        }

        Row row = s.createRow(r);
        texto(row, 0, "TOTAL", negrito);
        numero(row, 2, totalQtd, inteiro);
        numero(row, 3, totalCusto, dinheiroNegrito);

        s.setColumnWidth(0, 8000);
        s.setColumnWidth(1, 5500);
        s.setColumnWidth(2, 6500);
        for (int c = 3; c <= 5; c++) s.setColumnWidth(c, 4200);
        s.createFreezePane(0, 1);
    }

    // =========================================================
    // AUXILIARES
    // =========================================================
    private String nomePagamento(String codigo) {
        if ("D".equals(codigo)) return "Dinheiro";
        if ("P".equals(codigo)) return "Pix";
        if ("C".equals(codigo)) return "Cartão";
        return "Não informado";
    }

    private void linha(Sheet s, int r, String rotulo, double valor, CellStyle estilo) {
        Row row = s.createRow(r);
        texto(row, 0, rotulo, null);
        numero(row, 1, valor, estilo);
    }

    private void cabecalho(Row row, String... titulos) {
        for (int c = 0; c < titulos.length; c++) {
            texto(row, c, titulos[c], cabecalho);
        }
    }

    private void texto(Row row, int col, String valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor == null ? "" : valor);
        if (estilo != null) c.setCellStyle(estilo);
    }

    private void numero(Row row, int col, double valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private void criarEstilos(XSSFWorkbook wb) {
        DataFormat fmt = wb.createDataFormat();

        Font fNegrito = wb.createFont();
        fNegrito.setBold(true);

        Font fTitulo = wb.createFont();
        fTitulo.setBold(true);
        fTitulo.setFontHeightInPoints((short) 16);

        Font fCab = wb.createFont();
        fCab.setBold(true);
        fCab.setColor(IndexedColors.WHITE.getIndex());

        titulo = wb.createCellStyle();
        titulo.setFont(fTitulo);

        negrito = wb.createCellStyle();
        negrito.setFont(fNegrito);

        cabecalho = wb.createCellStyle();
        cabecalho.setFont(fCab);
        cabecalho.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cabecalho.setBorderBottom(BorderStyle.THIN);

        dinheiro = wb.createCellStyle();
        dinheiro.setDataFormat(fmt.getFormat("\"R$\" #,##0.00"));

        dinheiroNegrito = wb.createCellStyle();
        dinheiroNegrito.setDataFormat(fmt.getFormat("\"R$\" #,##0.00"));
        dinheiroNegrito.setFont(fNegrito);

        percentual = wb.createCellStyle();
        percentual.setDataFormat(fmt.getFormat("0.00%"));

        inteiro = wb.createCellStyle();
        inteiro.setDataFormat(fmt.getFormat("0"));

        data = wb.createCellStyle();
        data.setDataFormat(fmt.getFormat("dd/mm/yyyy hh:mm"));
    }
}