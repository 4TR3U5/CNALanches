package com.cnalanches.app.model;

public class ProdutoModel {
    private Integer id;
    private String nome;
    private Integer categoria;
    private Integer quantidade;
    private Float precoCompra;
    private Float precoVenda;
    private String nomeCategoria;

    public ProdutoModel() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getCategoria() { return categoria; }
    public void setCategoria(Integer categoria) { this.categoria = categoria; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Float getPrecoCompra() { return precoCompra; }
    public void setPrecoCompra(Float precoCompra) { this.precoCompra = precoCompra; }

    public Float getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(Float precoVenda) { this.precoVenda = precoVenda; }

    public String getNomeCategoria() {return nomeCategoria; }
    public void setNomeCategoria(String nomeCategoria){this.nomeCategoria = nomeCategoria;}
    
}