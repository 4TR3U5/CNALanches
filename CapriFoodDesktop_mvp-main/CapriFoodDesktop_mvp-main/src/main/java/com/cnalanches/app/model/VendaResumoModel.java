package com.cnalanches.app.model;

import java.time.LocalDateTime;

public class VendaResumoModel {
    private final Integer comandaId;
    private final LocalDateTime fechamento;
    private final String tipo;       // 'C' caixa / 'M' mesa
    private final Integer mesa;      // só na mesa
    private final String pagamento;  // 'D' / 'P' / 'C'
    private final double total;

    public VendaResumoModel(Integer comandaId, LocalDateTime fechamento, String tipo,
                            Integer mesa, String pagamento, double total) {
        this.comandaId = comandaId;
        this.fechamento = fechamento;
        this.tipo = tipo;
        this.mesa = mesa;
        this.pagamento = pagamento;
        this.total = total;
    }

    public Integer getComandaId() { return comandaId; }
    public LocalDateTime getFechamento() { return fechamento; }
    public String getTipo() { return tipo; }
    public Integer getMesa() { return mesa; }
    public String getPagamento() { return pagamento; }
    public double getTotal() { return total; }
}