package com.livraria.dao;

import com.livraria.model.MarcaPagina;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de Marca-Páginas e Conclusão de Livros.
 */
public class MarcaPaginaDAO {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private String getTimestampAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public MarcaPagina salvarMarcaPagina(int usuarioId, int livroId, int paginaAtual) throws SQLException {
        String timestamp = getTimestampAtual();

        String sql = "INSERT INTO marca_paginas (usuario_id, livro_id, pagina_atual, finalizado, data_atualizacao) "
                + "VALUES (?, ?, ?, 0, ?) "
                + "ON CONFLICT(usuario_id, livro_id) DO UPDATE SET "
                + "pagina_atual = excluded.pagina_atual, "
                + "data_atualizacao = excluded.data_atualizacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.setInt(3, Math.max(1, paginaAtual));
            pstmt.setString(4, timestamp);

            pstmt.executeUpdate();
        }

        return buscarPorUsuarioELivro(usuarioId, livroId);
    }

    public MarcaPagina buscarPorUsuarioELivro(int usuarioId, int livroId) throws SQLException {
        String sql = "SELECT m.id, m.usuario_id, m.livro_id, l.titulo AS livro_titulo, "
                + "m.pagina_atual, l.total_paginas, m.finalizado, m.data_atualizacao "
                + "FROM marca_paginas m "
                + "JOIN livros l ON l.id = m.livro_id "
                + "WHERE m.usuario_id = ? AND m.livro_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MarcaPagina(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getInt("livro_id"),
                            rs.getString("livro_titulo"),
                            rs.getInt("pagina_atual"),
                            rs.getInt("total_paginas"),
                            rs.getInt("finalizado") == 1,
                            rs.getString("data_atualizacao")
                    );
                }
            }
        }
        return null;
    }

    public List<MarcaPagina> listarPorUsuario(int usuarioId) throws SQLException {
        List<MarcaPagina> lista = new ArrayList<>();
        String sql = "SELECT m.id, m.usuario_id, m.livro_id, l.titulo AS livro_titulo, "
                + "m.pagina_atual, l.total_paginas, m.finalizado, m.data_atualizacao "
                + "FROM marca_paginas m "
                + "JOIN livros l ON l.id = m.livro_id "
                + "WHERE m.usuario_id = ? "
                + "ORDER BY m.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MarcaPagina(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getInt("livro_id"),
                            rs.getString("livro_titulo"),
                            rs.getInt("pagina_atual"),
                            rs.getInt("total_paginas"),
                            rs.getInt("finalizado") == 1,
                            rs.getString("data_atualizacao")
                    ));
                }
            }
        }
        return lista;
    }

    public int confirmarFinalizacao(int usuarioId, int livroId) throws SQLException {
        String timestamp = getTimestampAtual();

        MarcaPagina existente = buscarPorUsuarioELivro(usuarioId, livroId);
        boolean jaFinalizado = (existente != null && existente.isFinalizado());

        String sql = "INSERT INTO marca_paginas (usuario_id, livro_id, pagina_atual, finalizado, data_atualizacao) "
                + "VALUES (?, ?, 999999, 1, ?) "
                + "ON CONFLICT(usuario_id, livro_id) DO UPDATE SET "
                + "finalizado = 1, "
                + "data_atualizacao = excluded.data_atualizacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.setString(3, timestamp);

            pstmt.executeUpdate();
        }

        if (!jaFinalizado) {
            return usuarioDAO.incrementarLivrosLidos(usuarioId);
        } else {
            var u = usuarioDAO.buscarPorId(usuarioId);
            return (u != null) ? u.getLivrosLidos() : 0;
        }
    }
}
