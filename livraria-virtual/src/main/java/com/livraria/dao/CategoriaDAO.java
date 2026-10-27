package com.livraria.dao;

import com.livraria.model.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para gerenciamento da tabela de categorias de livros.
 */
public class CategoriaDAO {

    public List<Categoria> listarTodas() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT c.id, c.nome, c.slug, c.descricao, COUNT(l.id) AS total_livros "
                + "FROM categorias c "
                + "LEFT JOIN livros l ON (l.categoria_id = c.id OR LOWER(l.categoria) = LOWER(c.nome)) "
                + "GROUP BY c.id, c.nome, c.slug, c.descricao "
                + "ORDER BY c.nome ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Categoria c = new Categoria();
                c.setId(rs.getInt("id"));
                c.setNome(rs.getString("nome"));
                c.setSlug(rs.getString("slug"));
                c.setDescricao(rs.getString("descricao"));
                c.setTotalLivros(rs.getInt("total_livros"));
                lista.add(c);
            }
        }
        return lista;
    }

    public Categoria buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nome, slug, descricao FROM categorias WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Categoria c = new Categoria();
                    c.setId(rs.getInt("id"));
                    c.setNome(rs.getString("nome"));
                    c.setSlug(rs.getString("slug"));
                    c.setDescricao(rs.getString("descricao"));
                    return c;
                }
            }
        }
        return null;
    }

    public Categoria buscarPorSlug(String slug) throws SQLException {
        String sql = "SELECT id, nome, slug, descricao FROM categorias WHERE LOWER(slug) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, slug.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Categoria c = new Categoria();
                    c.setId(rs.getInt("id"));
                    c.setNome(rs.getString("nome"));
                    c.setSlug(rs.getString("slug"));
                    c.setDescricao(rs.getString("descricao"));
                    return c;
                }
            }
        }
        return null;
    }

    public boolean existePorNomeOuSlug(String nome, String slug) throws SQLException {
        String sql = "SELECT 1 FROM categorias WHERE LOWER(nome) = LOWER(?) OR LOWER(slug) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nome.trim());
            pstmt.setString(2, slug.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Categoria cadastrar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categorias (nome, slug, descricao) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, categoria.getNome().trim());
            pstmt.setString(2, categoria.getSlug().trim().toLowerCase());
            pstmt.setString(3, categoria.getDescricao() != null ? categoria.getDescricao().trim() : "");
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    categoria.setId(rs.getInt(1));
                }
            }
            return categoria;
        }
    }
}
