package com.livraria.dao;

import com.livraria.model.Avaliacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Access Object (DAO) para operações de Avaliação de Livros com estrelas (0.5 a 5.0).
 */
public class AvaliacaoDAO {

    private String getTimestampAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Salva ou atualiza a nota de um usuário para um livro (upsert).
     */
    public Avaliacao salvarOuAtualizar(int usuarioId, int livroId, double nota) throws SQLException {
        double notaTratada = Avaliacao.validarNota(nota);
        String timestamp = getTimestampAtual();

        String sql = "INSERT INTO avaliacoes (usuario_id, livro_id, nota, data_avaliacao) "
                + "VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(usuario_id, livro_id) DO UPDATE SET "
                + "nota = excluded.nota, "
                + "data_avaliacao = excluded.data_avaliacao";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.setDouble(3, notaTratada);
            pstmt.setString(4, timestamp);

            pstmt.executeUpdate();
        }

        return new Avaliacao(0, usuarioId, livroId, notaTratada, timestamp);
    }

    public double obterNotaDoUsuario(int usuarioId, int livroId) throws SQLException {
        String sql = "SELECT nota FROM avaliacoes WHERE usuario_id = ? AND livro_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("nota");
                }
            }
        }
        return 0.0;
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
}
