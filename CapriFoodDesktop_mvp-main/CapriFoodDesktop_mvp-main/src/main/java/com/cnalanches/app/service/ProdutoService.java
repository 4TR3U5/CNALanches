package com.cnalanches.app.service;

import java.util.List;

import com.cnalanches.app.dao.CategoriaDao;
import com.cnalanches.app.model.CategoriaModel;
import com.cnalanches.app.dao.ProdutoDao;
import com.cnalanches.app.helper.AcaoProduto;
import com.cnalanches.app.model.ProdutoModel;

public class ProdutoService {
    
    private final ProdutoDao produtoDao;
    private final CategoriaDao categoriaDao;

    public ProdutoService() {
        this.produtoDao = new ProdutoDao();
        this.categoriaDao = new CategoriaDao();
    }   

    public boolean novo(String nomeProduto, Integer categoria, Integer quantidadeProduto, Float precoCompra, Float precoVenda) {
        return this.produtoDao.novo(nomeProduto, categoria, quantidadeProduto, precoCompra, precoVenda);
    }

    public List<CategoriaModel> obterCategorias() {
        return this.categoriaDao.obterCategorias();
    }

    public  boolean novaCategoria(String nomeCategoria){
        return this.categoriaDao.novaCategoria(nomeCategoria);
    }

    public List<ProdutoModel> buscaEstoque(){
        return this.produtoDao.buscaEstoque();
    }

    public List<ProdutoModel> buscaItensInativos(){
        return this.produtoDao.buscaItensInativos();
    }

    public boolean atualizarProduto(AcaoProduto acao, Integer id, Object valor) {
        if (acao == AcaoProduto.ALTERAR_PRECO) {
            return this.produtoDao.alterarPreco(id, (Float) valor);
        }
        if (acao == AcaoProduto.ALTERAR_QUANTIDADE) {
            return this.produtoDao.alterarQuantidade(id, (Integer) valor);
        }
        if (acao == AcaoProduto.ALTERAR_CATEGORIA) {
            return this.produtoDao.alterarCategoria(id, (Integer) valor);
        }
        if (acao == AcaoProduto.INATIVAR) {
            return this.produtoDao.inativar(id);
        }
        if (acao == AcaoProduto.ATIVAR) {
            return this.produtoDao.ativar(id);
        }
        return false;
    }
}