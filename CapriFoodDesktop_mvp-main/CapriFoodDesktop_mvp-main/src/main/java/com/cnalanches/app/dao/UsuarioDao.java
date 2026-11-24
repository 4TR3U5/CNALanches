package com.cnalanches.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;  

public class UsuarioDao {

    private Connection connection;

    public UsuarioDao() {
        try {
            this.connection = ConexaoBanco.getConnection();
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar conexão no UsuarioDao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean buscarPorUsuarioESenha(String usuario, String senhaEmTextoPuro) {
        
        String sql = "SELECT ds_senha FROM usuarios WHERE ds_nome = ?";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {

            stmt.setString(1, usuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    
                    String hashSalvo = rs.getString("ds_senha");

                    System.out.println("--- DIAGNÓSTICO BCRYPT ---");
                    System.out.println("Senha Digitada: [" + senhaEmTextoPuro + "]");
                    System.out.println("Hash no Banco:   [" + hashSalvo + "]");
                    System.out.println("Tamanho do Hash: " + (hashSalvo != null ? hashSalvo.length() : 0));

                    boolean confere = BCrypt.checkpw(senhaEmTextoPuro, hashSalvo);
                    System.out.println("Resultado Check: " + confere);

                    return confere;
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao consultar usuário no banco: " + e.getMessage());
            e.printStackTrace();
        }

        
        return false;
    }
}