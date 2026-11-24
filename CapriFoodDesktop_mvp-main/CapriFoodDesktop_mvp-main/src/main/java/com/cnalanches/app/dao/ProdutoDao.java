package com.cnalanches.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
 import com.cnalanches.app.model.ProdutoModel;

public class ProdutoDao {
    
    private Connection connection;

    public ProdutoDao(){
        try{
            this.connection = ConexaoBanco.getConnection();
        }catch(SQLException e){
            System.err.println("Erro ao inicializar conexão no UsuarioDao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean novo(String nomeProduto, Integer categoria, Integer quantidadeProduto, Float precoCompra, float precoVenda) {


        String sql = "INSERT INTO TAB_PRODUTO(nr_categoria, ds_nome, ds_preco_mercado, ds_preco_venda, ie_qtd) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
                
            stmt.setInt(1, categoria);
            stmt.setString(2, nomeProduto);
            stmt.setFloat(3, precoCompra);  
            stmt.setFloat(4, precoVenda);
            stmt.setInt(5, quantidadeProduto);

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao executar a instrução SQL no banco de dados:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro ao processar a instrução:");
            e.printStackTrace();
        }

        return false;
    }
 
    
    public List<ProdutoModel> buscaEstoque() {
        List<ProdutoModel> produtos = new ArrayList<>();
        String sql = "SELECT p.nr_sequencia, p.ds_nome, p.nr_categoria, c.ds_nome AS nm_categoria, "
                + "p.ie_qtd, p.ds_preco_mercado, p.ds_preco_venda "
                + "FROM TAB_PRODUTO p "
                + "INNER JOIN TAB_CATEGORIA c ON c.nr_sequencia = p.nr_categoria"
                + " WHERE p.ie_situacao = 'A' ORDER BY c.ds_nome, p.ds_nome";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ProdutoModel produto = new ProdutoModel();
                produto.setId(rs.getInt("nr_sequencia"));
                produto.setNome(rs.getString("ds_nome"));
                produto.setCategoria(rs.getInt("nr_categoria"));
                produto.setNomeCategoria(rs.getString("nm_categoria"));
                produto.setQuantidade(rs.getInt("ie_qtd"));
                produto.setPrecoCompra(rs.getFloat("ds_preco_mercado"));
                produto.setPrecoVenda(rs.getFloat("ds_preco_venda"));
                produtos.add(produto);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao executar a instrução SQL no banco de dados:");
            e.printStackTrace();
        }

        return produtos;
    }


    private boolean executar(String sql, Object... params) {
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao executar a instrução SQL no banco de dados:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean alterarPreco(Integer id, Float preco) {
        return executar("UPDATE TAB_PRODUTO SET ds_preco_venda = ? WHERE nr_sequencia = ?", preco, id);
    }

    public boolean alterarQuantidade(Integer id, Integer qtd) {
        return executar("UPDATE TAB_PRODUTO SET ie_qtd = ? WHERE nr_sequencia = ?", qtd, id);
    }

    public boolean alterarCategoria(Integer id, Integer categoriaId) {
        return executar("UPDATE TAB_PRODUTO SET nr_categoria = ? WHERE nr_sequencia = ?", categoriaId, id);
    }

    public boolean inativar(Integer id) {
        return executar("UPDATE TAB_PRODUTO SET ie_situacao = 'I' WHERE nr_sequencia = ?", id);
    }

    public boolean ativar(Integer id) {
        return executar("UPDATE TAB_PRODUTO SET ie_situacao = 'A' WHERE nr_sequencia = ?", id);
    }

    public List<ProdutoModel> buscaItensInativos() {
        List<ProdutoModel> produtos = new ArrayList<>();
        String sql = "SELECT p.nr_sequencia, p.ds_nome, p.nr_categoria, c.ds_nome AS nm_categoria, "
                + "p.ie_qtd, p.ds_preco_mercado, p.ds_preco_venda "
                + "FROM TAB_PRODUTO p "
                + "INNER JOIN TAB_CATEGORIA c ON c.nr_sequencia = p.nr_categoria"
                + " WHERE p.ie_situacao = 'I' ORDER BY c.ds_nome, p.ds_nome";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ProdutoModel produto = new ProdutoModel();
                produto.setId(rs.getInt("nr_sequencia"));
                produto.setNome(rs.getString("ds_nome"));
                produto.setCategoria(rs.getInt("nr_categoria"));
                produto.setNomeCategoria(rs.getString("nm_categoria"));
                produto.setQuantidade(rs.getInt("ie_qtd"));
                produto.setPrecoCompra(rs.getFloat("ds_preco_mercado"));
                produto.setPrecoVenda(rs.getFloat("ds_preco_venda"));
                produtos.add(produto);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao executar a instrução SQL no banco de dados:");
            e.printStackTrace();
        }

        return produtos;
    }



    

}
