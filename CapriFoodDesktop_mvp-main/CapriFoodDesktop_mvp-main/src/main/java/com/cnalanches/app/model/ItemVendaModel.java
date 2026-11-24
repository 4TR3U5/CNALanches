package com.cnalanches.app.model;

public class ItemVendaModel {
    private final Integer produtoId;
    private final Integer quantidade;
    private final Float precoCompra;
    private final Float precoVenda;

    public ItemVendaModel(Integer produtoId, Integer quantidade, Float precoCompra, Float precoVenda) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoCompra = precoCompra;
        this.precoVenda = precoVenda;
    }

    public Integer getProdutoId() { return produtoId; }
    public Integer getQuantidade() { return quantidade; }
    public Float getPrecoCompra() { return precoCompra; }
    public Float getPrecoVenda() { return precoVenda; }
}