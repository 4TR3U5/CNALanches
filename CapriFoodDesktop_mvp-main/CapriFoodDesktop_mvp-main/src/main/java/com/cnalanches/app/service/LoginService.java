package com.cnalanches.app.service;
import com.cnalanches.app.model.UsuarioModel;

public class LoginService{

    private final UsuarioModel usuarioModel;

    public LoginService(){
        this.usuarioModel = new UsuarioModel();
    }
    

    public boolean validarLogin(String usuario, String senha){
        
        System.out.println(usuario);
        System.out.println(senha);


        return this.usuarioModel.validar(usuario, senha);
        
    }
}