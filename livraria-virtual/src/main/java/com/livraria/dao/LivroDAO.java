package com.livraria.dao;

import com.livraria.model.Livro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações no catálogo de Livros da LibraryClound.
 * Suporta busca por título/categoria, conteúdo completo do livro (inserido pelo admin)
 * e cálculo em tempo real da média de avaliação em estrelas (0.5 a 5.0).
 */
public class LivroDAO {

    private static final String BASE_SELECT =
            "SELECT l.id, l.titulo, l.autor, l.categoria, l.total_paginas, l.sinopse, l.capa_url, l.conteudo, "
            + "COALESCE(AVG(a.nota), 0.0) as media_nota, COUNT(a.id) as total_avaliacoes "
            + "FROM livros l "
            + "LEFT JOIN avaliacoes a ON a.livro_id = l.id ";

    public List<Livro> listarTodos() throws SQLException {
        List<Livro> lista = new ArrayList<>();
        String sql = BASE_SELECT + "GROUP BY l.id ORDER BY l.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearLivro(rs));
            }
        }
        return lista;
    }

    public Livro buscarPorId(int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.id = ? GROUP BY l.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearLivro(rs);
                }
            }
        }
        return null;
    }

    public List<Livro> pesquisar(String termo, String categoria) throws SQLException {
        List<Livro> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT + "WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (categoria != null && !categoria.trim().isEmpty() && !categoria.equalsIgnoreCase("Todas")) {
            sql.append(" AND LOWER(l.categoria) = LOWER(?)");
            parametros.add(categoria.trim());
        }

        if (termo != null && !termo.trim().isEmpty()) {
            sql.append(" AND (LOWER(l.titulo) LIKE LOWER(?) OR LOWER(l.autor) LIKE LOWER(?) OR LOWER(l.categoria) LIKE LOWER(?))");
            String like = "%" + termo.trim() + "%";
            parametros.add(like);
            parametros.add(like);
            parametros.add(like);
        }

        sql.append(" GROUP BY l.id ORDER BY l.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                pstmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLivro(rs));
                }
            }
        }
        return lista;
    }

    public List<String> listarCategorias() throws SQLException {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT DISTINCT categoria FROM livros WHERE categoria IS NOT NULL AND TRIM(categoria) != '' ORDER BY categoria ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                categorias.add(rs.getString("categoria"));
            }
        }
        return categorias;
    }

    /**
     * Inserção de novo livro com conteúdo livre gerenciado pelo admin.
     * Requisito: "o admin pode por o conteudo do livro, independente de quantas paginas"
     */
    public Livro inserir(Livro livro) throws SQLException {
        String sql = "INSERT INTO livros (titulo, autor, categoria, total_paginas, sinopse, capa_url, conteudo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, livro.getTitulo());
            pstmt.setString(2, livro.getAutor());
            pstmt.setString(3, livro.getCategoria());
            pstmt.setInt(4, livro.getTotalPaginas());
            pstmt.setString(5, livro.getSinopse());
            pstmt.setString(6, livro.getCapaUrl());
            pstmt.setString(7, livro.getConteudo() != null ? livro.getConteudo() : "");

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    livro.setId(rs.getInt(1));
                }
            }
            return livro;
        }
    }

    public boolean atualizar(Livro livro) throws SQLException {
        String sql = "UPDATE livros SET titulo = ?, autor = ?, categoria = ?, total_paginas = ?, sinopse = ?, capa_url = ?, conteudo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, livro.getTitulo());
            pstmt.setString(2, livro.getAutor());
            pstmt.setString(3, livro.getCategoria());
            pstmt.setInt(4, livro.getTotalPaginas());
            pstmt.setString(5, livro.getSinopse());
            pstmt.setString(6, livro.getCapaUrl());
            pstmt.setString(7, livro.getConteudo() != null ? livro.getConteudo() : "");
            pstmt.setInt(8, livro.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean excluir(int id) throws SQLException {
        String sql = "DELETE FROM livros WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Livro mapearLivro(ResultSet rs) throws SQLException {
        return new Livro(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("autor"),
                rs.getString("categoria"),
                rs.getInt("total_paginas"),
                rs.getString("sinopse"),
                rs.getString("capa_url"),
                rs.getString("conteudo"),
                rs.getDouble("media_nota"),
                rs.getInt("total_avaliacoes")
        );
    }
}
