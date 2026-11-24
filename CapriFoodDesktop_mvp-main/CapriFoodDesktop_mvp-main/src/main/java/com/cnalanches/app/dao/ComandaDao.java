package com.cnalanches.app.dao;

import java.sql.ResultSet;
import com.cnalanches.app.model.ItemVendaModel;
import com.cnalanches.app.model.MesaAbertaModel;
import com.cnalanches.app.model.ComandaItemModel;
import com.cnalanches.app.model.ItemRelatorioModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import com.cnalanches.app.model.VendaResumoModel;


public class ComandaDao {
    

    public Connection connection;

    public ComandaDao(){
        try{
            this.connection = ConexaoBanco.getConnection();
        }catch(SQLException e){
            System.err.println("Erro ao inicializar conexão no ComandaDao: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public boolean verificaMesa(Integer numeroMesa) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tab_comanda WHERE nr_mesa = ? AND ie_tipo = 'M' AND ie_status = 'A'";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, numeroMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }



    public int registrarVendaCaixa(List<ItemVendaModel> itens, double total, String pagamento) throws SQLException {
        String sqlBaixa = "UPDATE tab_produto SET ie_qtd = ie_qtd - ? "
                        + "WHERE nr_sequencia = ? AND ie_situacao = 'A' AND ie_qtd >= ?";
        String sqlComanda = "INSERT INTO tab_comanda (ie_status, dt_fechamento, ds_total, ie_tipo, ie_pagamento) "
                        + "VALUES ('F', NOW(), ?, 'C', ?)";
        String sqlItem = "INSERT INTO tab_comanda_item "
                    + "(nr_comanda, nr_produto, nr_quantidade, ds_preco_mercado, ds_preco_venda) "
                    + "VALUES (?, ?, ?, ?, ?)";

        boolean autoCommitOriginal = this.connection.getAutoCommit();
        try {
            this.connection.setAutoCommit(false);

            // 1. Baixa o estoque (só baixa se ainda houver quantidade suficiente)
            try (PreparedStatement stmt = this.connection.prepareStatement(sqlBaixa)) {
                for (ItemVendaModel item : itens) {
                    stmt.setInt(1, item.getQuantidade());
                    stmt.setInt(2, item.getProdutoId());
                    stmt.setInt(3, item.getQuantidade());
                    if (stmt.executeUpdate() == 0) {
                        throw new SQLException("Estoque insuficiente ou produto inativo (ID " + item.getProdutoId() + ").");
                    }
                }
            }

            // 2. Cria a comanda já fechada
            int comandaId;
            try (PreparedStatement stmt = this.connection.prepareStatement(sqlComanda, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDouble(1, total);
                stmt.setString(2, pagamento);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Comanda criada, mas o id não foi retornado.");
                    comandaId = rs.getInt(1);
                }
            }

            // 3. Grava os itens com o preço do momento da venda
            try (PreparedStatement stmt = this.connection.prepareStatement(sqlItem)) {
                for (ItemVendaModel item : itens) {
                    stmt.setInt(1, comandaId);
                    stmt.setInt(2, item.getProdutoId());
                    stmt.setInt(3, item.getQuantidade());
                    stmt.setFloat(4, item.getPrecoCompra() == null ? 0f : item.getPrecoCompra());
                    stmt.setFloat(5, item.getPrecoVenda() == null ? 0f : item.getPrecoVenda());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            this.connection.commit();
            return comandaId;

        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            this.connection.setAutoCommit(autoCommitOriginal);
        }
    }


    public int abrirComandaMesa(Integer numeroMesa) throws SQLException {
        String sql = "INSERT INTO tab_comanda (ie_tipo, nr_mesa) VALUES ('M', ?)";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, numeroMesa);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Comanda criada, mas o id não foi retornado.");
            }
        }
    }

    public void fecharComandaMesa(int comandaId, List<ItemVendaModel> itens, double total, String pagamento)
            throws SQLException {
        String sqlBaixa = "UPDATE tab_produto SET ie_qtd = ie_qtd - ? "
                        + "WHERE nr_sequencia = ? AND ie_situacao = 'A' AND ie_qtd >= ?";
        String sqlItem = "INSERT INTO tab_comanda_item "
                    + "(nr_comanda, nr_produto, nr_quantidade, ds_preco_mercado, ds_preco_venda) "
                    + "VALUES (?, ?, ?, ?, ?)";
        String sqlFecha = "UPDATE tab_comanda SET ie_status = 'F', dt_fechamento = NOW(), ds_total = ?, ie_pagamento = ? "
                        + "WHERE nr_sequencia = ? AND ie_status = 'A'";

        boolean autoCommitOriginal = this.connection.getAutoCommit();
        try {
            this.connection.setAutoCommit(false);

            try (PreparedStatement stmt = this.connection.prepareStatement(sqlBaixa)) {
                for (ItemVendaModel item : itens) {
                    stmt.setInt(1, item.getQuantidade());
                    stmt.setInt(2, item.getProdutoId());
                    stmt.setInt(3, item.getQuantidade());
                    if (stmt.executeUpdate() == 0) {
                        throw new SQLException("Estoque insuficiente ou produto inativo (ID " + item.getProdutoId() + ").");
                    }
                }
            }

            try (PreparedStatement stmt = this.connection.prepareStatement(sqlItem)) {
                for (ItemVendaModel item : itens) {
                    stmt.setInt(1, comandaId);
                    stmt.setInt(2, item.getProdutoId());
                    stmt.setInt(3, item.getQuantidade());
                    stmt.setFloat(4, item.getPrecoCompra() == null ? 0f : item.getPrecoCompra());
                    stmt.setFloat(5, item.getPrecoVenda() == null ? 0f : item.getPrecoVenda());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            try (PreparedStatement stmt = this.connection.prepareStatement(sqlFecha)) {
                stmt.setDouble(1, total);
                stmt.setString(2, pagamento);
                stmt.setInt(3, comandaId);
                if (stmt.executeUpdate() == 0) {
                    throw new SQLException("Comanda " + comandaId + " não está aberta.");
                }
            }

            this.connection.commit();

        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            this.connection.setAutoCommit(autoCommitOriginal);
        }
    }

    public void cancelarComandaMesa(int comandaId) throws SQLException {
        String sql = "UPDATE tab_comanda SET ie_status = 'C', dt_fechamento = NOW() "
                + "WHERE nr_sequencia = ? AND ie_status = 'A'";

        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, comandaId);
            stmt.executeUpdate();
        }
    }



    public List<MesaAbertaModel> listarMesasAbertas() throws SQLException {
        String sql = "SELECT c.nr_sequencia, c.nr_mesa, c.dt_abertura, "
                + "COALESCE(SUM(i.nr_quantidade * i.ds_preco_venda), 0) AS total, "
                + "COALESCE(SUM(i.nr_quantidade), 0) AS qtd "
                + "FROM tab_comanda c "
                + "LEFT JOIN tab_comanda_item i ON i.nr_comanda = c.nr_sequencia "
                + "WHERE c.ie_tipo = 'M' AND c.ie_status = 'A' "
                + "GROUP BY c.nr_sequencia, c.nr_mesa, c.dt_abertura "
                + "ORDER BY c.nr_mesa";

        List<MesaAbertaModel> mesas = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mesas.add(new MesaAbertaModel(
                        rs.getInt("nr_sequencia"),
                        rs.getInt("nr_mesa"),
                        rs.getTimestamp("dt_abertura").toLocalDateTime(),
                        rs.getDouble("total"),
                        rs.getInt("qtd")));
            }
        }
        return mesas;
    }

    public List<ComandaItemModel> listarItensComanda(int comandaId) throws SQLException {
        String sql = "SELECT i.nr_produto, p.ds_nome, i.nr_quantidade, i.ds_preco_venda "
                + "FROM tab_comanda_item i "
                + "INNER JOIN tab_produto p ON p.nr_sequencia = i.nr_produto "
                + "WHERE i.nr_comanda = ? ORDER BY i.nr_sequencia";

        List<ComandaItemModel> itens = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, comandaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(new ComandaItemModel(
                            rs.getInt("nr_produto"),
                            rs.getString("ds_nome"),
                            rs.getInt("nr_quantidade"),
                            rs.getFloat("ds_preco_venda")));
                }
            }
        }
        return itens;
    }

    private void inserirItens(int comandaId, List<ItemVendaModel> itens) throws SQLException {
        String sqlItem = "INSERT INTO tab_comanda_item "
                    + "(nr_comanda, nr_produto, nr_quantidade, ds_preco_mercado, ds_preco_venda) "
                    + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = this.connection.prepareStatement(sqlItem)) {
            for (ItemVendaModel item : itens) {
                stmt.setInt(1, comandaId);
                stmt.setInt(2, item.getProdutoId());
                stmt.setInt(3, item.getQuantidade());
                stmt.setFloat(4, item.getPrecoCompra() == null ? 0f : item.getPrecoCompra());
                stmt.setFloat(5, item.getPrecoVenda() == null ? 0f : item.getPrecoVenda());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void exigirComandaAberta(int comandaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tab_comanda WHERE nr_sequencia = ? AND ie_status = 'A'";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, comandaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    throw new SQLException("A comanda " + comandaId + " não está mais aberta.");
                }
            }
        }
    }

    public void lancarItens(int comandaId, List<ItemVendaModel> itens) throws SQLException {
        boolean autoCommitOriginal = this.connection.getAutoCommit();
        try {
            this.connection.setAutoCommit(false);
            exigirComandaAberta(comandaId);
            inserirItens(comandaId, itens);
            this.connection.commit();
        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            this.connection.setAutoCommit(autoCommitOriginal);
        }
    }

    /** Grava os itens pendentes, baixa o estoque de toda a comanda e fecha. Tudo numa transação. */
    public void fecharComandaMesa(int comandaId, List<ItemVendaModel> pendentes, String pagamento)
            throws SQLException {
        String sqlDistintos = "SELECT COUNT(DISTINCT nr_produto) FROM tab_comanda_item WHERE nr_comanda = ?";
        String sqlBaixa = "UPDATE tab_produto p "
                        + "JOIN (SELECT nr_produto, SUM(nr_quantidade) AS qtd FROM tab_comanda_item "
                        + "      WHERE nr_comanda = ? GROUP BY nr_produto) i ON i.nr_produto = p.nr_sequencia "
                        + "SET p.ie_qtd = p.ie_qtd - i.qtd "
                        + "WHERE p.ie_situacao = 'A' AND p.ie_qtd >= i.qtd";
        String sqlFecha = "UPDATE tab_comanda SET ie_status = 'F', dt_fechamento = NOW(), ie_pagamento = ?, "
                        + "ds_total = (SELECT COALESCE(SUM(nr_quantidade * ds_preco_venda), 0) "
                        + "            FROM tab_comanda_item WHERE nr_comanda = ?) "
                        + "WHERE nr_sequencia = ? AND ie_status = 'A'";

        boolean autoCommitOriginal = this.connection.getAutoCommit();
        try {
            this.connection.setAutoCommit(false);
            exigirComandaAberta(comandaId);

            if (pendentes != null && !pendentes.isEmpty()) {
                inserirItens(comandaId, pendentes);
            }

            int distintos;
            try (PreparedStatement stmt = this.connection.prepareStatement(sqlDistintos)) {
                stmt.setInt(1, comandaId);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    distintos = rs.getInt(1);
                }
            }
            if (distintos == 0) {
                throw new SQLException("Comanda sem itens.");
            }

            try (PreparedStatement stmt = this.connection.prepareStatement(sqlBaixa)) {
                stmt.setInt(1, comandaId);
                if (stmt.executeUpdate() != distintos) {
                    throw new SQLException("Estoque insuficiente ou produto inativo em algum item da comanda.");
                }
            }

            try (PreparedStatement stmt = this.connection.prepareStatement(sqlFecha)) {
                stmt.setString(1, pagamento);
                stmt.setInt(2, comandaId);
                stmt.setInt(3, comandaId);
                if (stmt.executeUpdate() == 0) {
                    throw new SQLException("Comanda " + comandaId + " não está aberta.");
                }
            }

            this.connection.commit();

        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            this.connection.setAutoCommit(autoCommitOriginal);
        }
    }




    public List<VendaResumoModel> listarVendas(LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT nr_sequencia, dt_fechamento, ie_tipo, nr_mesa, ie_pagamento, ds_total "
                + "FROM tab_comanda "
                + "WHERE ie_status = 'F' AND dt_fechamento >= ? AND dt_fechamento < ? "
                + "ORDER BY dt_fechamento DESC";

        List<VendaResumoModel> vendas = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vendas.add(new VendaResumoModel(
                            rs.getInt("nr_sequencia"),
                            rs.getTimestamp("dt_fechamento").toLocalDateTime(),
                            rs.getString("ie_tipo"),
                            (Integer) rs.getObject("nr_mesa"),
                            rs.getString("ie_pagamento"),
                            rs.getDouble("ds_total")));
                }
            }
        }
        return vendas;
    }

    /** Itens vendidos em comandas fechadas no período (datas inclusivas). */
    public List<ItemRelatorioModel> listarItensVendidos(LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT c.nr_sequencia, c.dt_fechamento, c.ie_tipo, c.nr_mesa, c.ie_pagamento, "
                + "p.ds_nome AS produto, COALESCE(cat.ds_nome, 'Sem categoria') AS categoria, "
                + "i.nr_quantidade, i.ds_preco_mercado, i.ds_preco_venda "
                + "FROM tab_comanda_item i "
                + "JOIN tab_comanda c ON c.nr_sequencia = i.nr_comanda "
                + "JOIN tab_produto p ON p.nr_sequencia = i.nr_produto "
                + "LEFT JOIN tab_categoria cat ON cat.nr_sequencia = p.nr_categoria "
                + "WHERE c.ie_status = 'F' AND c.dt_fechamento >= ? AND c.dt_fechamento < ? "
                + "ORDER BY c.dt_fechamento, c.nr_sequencia, i.nr_sequencia";

        List<ItemRelatorioModel> itens = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(new ItemRelatorioModel(
                            rs.getInt("nr_sequencia"),
                            rs.getTimestamp("dt_fechamento").toLocalDateTime(),
                            rs.getString("ie_tipo"),
                            (Integer) rs.getObject("nr_mesa"),
                            rs.getString("ie_pagamento"),
                            rs.getString("produto"),
                            rs.getString("categoria"),
                            rs.getInt("nr_quantidade"),
                            rs.getDouble("ds_preco_mercado"),
                            rs.getDouble("ds_preco_venda")));
                }
            }
        }
        return itens;
    }

}
