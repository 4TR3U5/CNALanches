package com.cnalanches.app.model;

import java.time.LocalDateTime;

public class ItemRelatorioModel {
    private final Integer comandaId;
    private final LocalDateTime fechamento;
    private final String tipo;      // 'C' caixa / 'M' mesa
    private final Integer mesa;
    private final String pagamento; // 'D' / 'P' / 'C'
    private final String produto;
    private final String categoria;
    private final int quantidade;
    private final double precoCusto;
    private final double precoVenda;

    public ItemRelatorioModel(Integer comandaId, LocalDateTime fechamento, String tipo, Integer mesa,
                              String pagamento, String produto, String categoria, int quantidade,
                              double precoCusto, double precoVenda) {
        this.comandaId = comandaId;
        this.fechamento = fechamento;
        this.tipo = tipo;
        this.mesa = mesa;
        this.pagamento = pagamento;
        this.produto = produto;
        this.categoria = categoria;
        this.quantidade = quantidade;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
    }

    public Integer getComandaId() { return comandaId; }
    public LocalDateTime getFechamento() { return fechamento; }
    public String getTipo() { return tipo; }
    public Integer getMesa() { return mesa; }
    public String getPagamento() { return pagamento; }
    public String getProduto() { return produto; }
    public String getCategoria() { return categoria; }
    public int getQuantidade() { return quantidade; }
    public double getPrecoCusto() { return precoCusto; }
    public double getPrecoVenda() { return precoVenda; }

    public double getReceita() { return precoVenda * quantidade; }
    public double getCusto() { return precoCusto * quantidade; }
    public double getLucro() { return getReceita() - getCusto(); }
}