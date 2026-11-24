package com.cnalanches.app.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import com.cnalanches.app.dao.ComandaDao;
import com.cnalanches.app.model.ItemVendaModel;
import com.cnalanches.app.model.MesaAbertaModel;
import com.cnalanches.app.model.VendaResumoModel;
import com.cnalanches.app.model.ComandaItemModel;
import com.cnalanches.app.model.ItemRelatorioModel;

public class VendaService {
    

    private final ComandaDao comandaDao;

    public VendaService(){
        this.comandaDao = new ComandaDao();
    }
    
    public boolean verificaMesa(Integer numeroMesa) throws SQLException {
        return this.comandaDao.verificaMesa(numeroMesa);
    }

    public int registrarVendaCaixa(List<ItemVendaModel> itens, double total, String pagamento) throws SQLException {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Carrinho vazio.");
        }
        return this.comandaDao.registrarVendaCaixa(itens, total, pagamento);
    }

    public int abrirComandaMesa(Integer numeroMesa) throws SQLException {
        return this.comandaDao.abrirComandaMesa(numeroMesa);
    }

    public void fecharComandaMesa(int comandaId, List<ItemVendaModel> pendentes, String pagamento)
            throws SQLException {
        this.comandaDao.fecharComandaMesa(comandaId, pendentes, pagamento);
    }

    public void cancelarComandaMesa(int comandaId) throws SQLException {
        this.comandaDao.cancelarComandaMesa(comandaId);
    }


    public List<MesaAbertaModel> listarMesasAbertas() throws SQLException {
        return this.comandaDao.listarMesasAbertas();
    }

    public List<ComandaItemModel> listarItensComanda(int comandaId) throws SQLException {
        return this.comandaDao.listarItensComanda(comandaId);
    }

    public void lancarItens(int comandaId, List<ItemVendaModel> itens) throws SQLException {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Nenhum item para lançar.");
        }
        this.comandaDao.lancarItens(comandaId, itens);
    }

    public List<VendaResumoModel> listarVendas(LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido.");
        }
        return this.comandaDao.listarVendas(inicio, fim);
    }

    public List<ItemRelatorioModel> listarItensVendidos(LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido.");
        }
        return this.comandaDao.listarItensVendidos(inicio, fim);
    }

}
