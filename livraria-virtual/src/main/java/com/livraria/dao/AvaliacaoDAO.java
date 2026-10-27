package com.livraria.dao;

import com.livraria.model.Avaliacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de Avaliação de Livros com estrelas (0.5 a 5.0)
 * e comentários textuais.
 */
public class AvaliacaoDAO {

    private String getTimestampAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Salva ou atualiza a nota e comentário de um usuário para um livro (upsert).
     */
    public Avaliacao salvarOuAtualizar(int usuarioId, int livroId, double nota, String comentario) throws SQLException {
        double notaTratada = Avaliacao.validarNota(nota);
        String timestamp = getTimestampAtual();
        String comentarioTratado = (comentario != null) ? comentario.trim() : "";

        String sql = "INSERT INTO avaliacoes (usuario_id, livro_id, nota, comentario, data_avaliacao, criado_em) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT(usuario_id, livro_id) DO UPDATE SET "
                + "nota = excluded.nota, "
                + "comentario = excluded.comentario, "
                + "data_avaliacao = excluded.data_avaliacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.setDouble(3, notaTratada);
            pstmt.setString(4, comentarioTratado);
            pstmt.setString(5, timestamp);

            pstmt.executeUpdate();
        }

        return new Avaliacao(0, usuarioId, livroId, notaTratada, comentarioTratado, timestamp);
    }

    public Avaliacao salvarOuAtualizar(int usuarioId, int livroId, double nota) throws SQLException {
        return salvarOuAtualizar(usuarioId, livroId, nota, "");
    }

    public Avaliacao buscarPorUsuarioELivro(int usuarioId, int livroId) throws SQLException {
        String sql = "SELECT a.id, a.usuario_id, u.nome_fantasia, a.livro_id, a.nota, a.comentario, a.data_avaliacao "
                + "FROM avaliacoes a "
                + "JOIN usuarios u ON u.id = a.usuario_id "
                + "WHERE a.usuario_id = ? AND a.livro_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Avaliacao a = new Avaliacao(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getInt("livro_id"),
                            rs.getDouble("nota"),
                            rs.getString("comentario"),
                            rs.getString("data_avaliacao")
                    );
                    a.setUsuarioNome(rs.getString("nome_fantasia"));
                    return a;
                }
            }
        }
        return null;
    }

    public double obterNotaDoUsuario(int usuarioId, int livroId) throws SQLException {
        Avaliacao a = buscarPorUsuarioELivro(usuarioId, livroId);
        return a != null ? a.getNota() : 0.0;
    }

    public double[] obterEstatisticas(int livroId) throws SQLException {
        String sql = "SELECT COALESCE(AVG(nota), 0.0) as media, COUNT(*) as total FROM avaliacoes WHERE livro_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new double[]{ rs.getDouble("media"), rs.getInt("total") };
                }
            }
        }
        return new double[]{ 0.0, 0 };
    }

    public List<Avaliacao> listarPorLivro(int livroId) throws SQLException {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = "SELECT a.id, a.usuario_id, u.nome_fantasia, a.livro_id, a.nota, a.comentario, a.data_avaliacao "
                + "FROM avaliacoes a "
                + "JOIN usuarios u ON u.id = a.usuario_id "
                + "WHERE a.livro_id = ? "
                + "ORDER BY a.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Avaliacao a = new Avaliacao(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getInt("livro_id"),
                            rs.getDouble("nota"),
                            rs.getString("comentario"),
                            rs.getString("data_avaliacao")
                    );
                    a.setUsuarioNome(rs.getString("nome_fantasia"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }
}
