package com.livraria.dao;

import com.livraria.model.Favorito;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para gerenciamento de Livros Favoritos no SQLite.
 * Atende ao requisito:
 * "uma aba para favoritos(criando assim uma funçao para favoritar os livros e essa
 *  funcao deve aparecer no perfil dos usuarios mostrando, nome, categoria e a capa do livro)"
 */
public class FavoritoDAO {

    private String getTimestampAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Alterna o estado de favorito: se já for favorito, remove; se não for, adiciona.
     * @return true se foi favoritado, false se foi removido dos favoritos.
     */
    public boolean toggleFavorito(int usuarioId, int livroId) throws SQLException {
        if (isFavorito(usuarioId, livroId)) {
            removerFavorito(usuarioId, livroId);
            return false;
        } else {
            adicionarFavorito(usuarioId, livroId);
            return true;
        }
    }

    public boolean isFavorito(int usuarioId, int livroId) throws SQLException {
        String sql = "SELECT 1 FROM favoritos WHERE usuario_id = ? AND livro_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void adicionarFavorito(int usuarioId, int livroId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO favoritos (usuario_id, livro_id, data_adicionado) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.setString(3, getTimestampAtual());
            pstmt.executeUpdate();
        }
    }

    public void removerFavorito(int usuarioId, int livroId) throws SQLException {
        String sql = "DELETE FROM favoritos WHERE usuario_id = ? AND livro_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, livroId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Lista todos os livros favoritados pelo usuário.
     * Retorna nome do livro, categoria e a capa do livro para exibição no perfil.
     */
    public List<Favorito> listarPorUsuario(int usuarioId) throws SQLException {
        List<Favorito> lista = new ArrayList<>();
        String sql = "SELECT f.id, f.usuario_id, f.livro_id, l.titulo AS livro_titulo, "
                + "l.categoria AS livro_categoria, l.capa_url AS livro_capa_url, f.data_adicionado "
                + "FROM favoritos f "
                + "JOIN livros l ON l.id = f.livro_id "
                + "WHERE f.usuario_id = ? "
                + "ORDER BY f.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Favorito(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getInt("livro_id"),
                            rs.getString("livro_titulo"),
                            rs.getString("livro_categoria"),
                            rs.getString("livro_capa_url"),
                            rs.getString("data_adicionado")
                    ));
                }
            }
        }
        return lista;
    }
}
