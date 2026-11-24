package com.cnalanches.app.model;

import java.time.LocalDateTime;

public class MesaAbertaModel {
    private final Integer comandaId;
    private final Integer mesa;
    private final LocalDateTime abertura;
    private final double total;
    private final int qtdItens;

    public MesaAbertaModel(Integer comandaId, Integer mesa, LocalDateTime abertura, double total, int qtdItens) {
        this.comandaId = comandaId;
        this.mesa = mesa;
        this.abertura = abertura;
        this.total = total;
        this.qtdItens = qtdItens;
    }

    public Integer getComandaId() { return comandaId; }
    public Integer getMesa() { return mesa; }
    public LocalDateTime getAbertura() { return abertura; }
    public double getTotal() { return total; }
    public int getQtdItens() { return qtdItens; }
}