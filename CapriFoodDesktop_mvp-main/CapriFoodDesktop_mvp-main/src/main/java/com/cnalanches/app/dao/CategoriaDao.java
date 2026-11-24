package com.cnalanches.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cnalanches.app.model.CategoriaModel;

public class CategoriaDao {

    private Connection connection;

    public CategoriaDao() {
        try {
            this.connection = ConexaoBanco.getConnection();
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar conexão no CategoriaDao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<CategoriaModel> obterCategorias() {
        List<CategoriaModel> categorias = new ArrayList<>();
        String sql = "SELECT nr_sequencia, ds_nome FROM TAB_CATEGORIA";  

        try (PreparedStatement stmt = this.connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("nr_sequencia");  
                String nome = rs.getString("ds_nome");
 
                CategoriaModel categoria = new CategoriaModel(id, nome);
                categorias.add(categoria);
            }

        } catch (SQLException e) {
            System.out.println("Erro ao buscar categorias no banco:");
            e.printStackTrace();
        }

        return categorias;
    }

    public  boolean novaCategoria(String nomeCategoria){
        String sql = "INSERT INTO TAB_CATEGORIA(DS_NOME) VALUES (?)";

        try(PreparedStatement stmt = this.connection.prepareStatement(sql)){
            stmt.setString(1, nomeCategoria);

            int linhasAfetadas = stmt.executeUpdate();

            if(linhasAfetadas > 0){
                return true;
            }else{
                return false;
            }
        }catch(SQLException e){
            System.out.println("ERRO AO EXECUTAR SQL NO CATEGORIA DAO");
            System.out.println(e);
           
        }

        return false;
    }
}