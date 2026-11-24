package com.cnalanches.app.model;

import com.cnalanches.app.dao.UsuarioDao;

public class UsuarioModel {
    
    private final UsuarioDao usuarioDao;

    public UsuarioModel() {
        this.usuarioDao = new UsuarioDao();
    }

    public boolean validar(String usuario, String senha) {
        return this.usuarioDao.buscarPorUsuarioESenha(usuario, senha);
    }
}