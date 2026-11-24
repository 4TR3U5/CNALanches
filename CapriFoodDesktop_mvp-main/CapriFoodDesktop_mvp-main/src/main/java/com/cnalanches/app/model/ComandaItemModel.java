package com.cnalanches.app.model;

public class ComandaItemModel {
    private final Integer produtoId;
    private final String nome;
    private final Integer quantidade;
    private final Float precoVenda;

    public ComandaItemModel(Integer produtoId, String nome, Integer quantidade, Float precoVenda) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.quantidade = quantidade;
        this.precoVenda = precoVenda;
    }

    public Integer getProdutoId() { return produtoId; }
    public String getNome() { return nome; }
    public Integer getQuantidade() { return quantidade; }
    public Float getPrecoVenda() { return precoVenda; }
    public double getSubtotal() { return (precoVenda == null ? 0 : precoVenda) * quantidade; }
}