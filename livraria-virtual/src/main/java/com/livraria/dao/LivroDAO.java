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
 * Integrado ao pool HikariCP, com suporte a categorias normalizadas e dados bibliográficos (ISBN).
 */
public class LivroDAO {

    private static final String BASE_SELECT =
            "SELECT l.id, l.titulo, l.autor, COALESCE(l.categoria_id, c.id, 0) AS categoria_id, "
            + "COALESCE(c.nome, l.categoria, 'Geral') AS categoria, l.total_paginas, l.sinopse, "
            + "l.capa_url, l.conteudo, l.isbn, l.data_publicacao, l.criado_em, l.atualizado_em, "
            + "COALESCE(AVG(a.nota), 0.0) AS media_nota, COUNT(a.id) AS total_avaliacoes "
            + "FROM livros l "
            + "LEFT JOIN categorias c ON (c.id = l.categoria_id OR LOWER(c.nome) = LOWER(l.categoria)) "
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
            sql.append(" AND (LOWER(l.categoria) = LOWER(?) OR LOWER(c.nome) = LOWER(?))");
            parametros.add(categoria.trim());
            parametros.add(categoria.trim());
        }

        if (termo != null && !termo.trim().isEmpty()) {
            sql.append(" AND (LOWER(l.titulo) LIKE ? OR LOWER(l.autor) LIKE ? OR LOWER(l.sinopse) LIKE ?)");
            String likePattern = "%" + termo.trim().toLowerCase() + "%";
            parametros.add(likePattern);
            parametros.add(likePattern);
            parametros.add(likePattern);
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
        return listarCategoriasExistentes();
    }

    public List<String> listarCategoriasExistentes() throws SQLException {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT DISTINCT nome FROM categorias UNION SELECT DISTINCT categoria FROM livros WHERE categoria IS NOT NULL ORDER BY 1 ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String cat = rs.getString(1);
                if (cat != null && !cat.isBlank()) {
                    categorias.add(cat.trim());
                }
            }
        }
        return categorias;
    }

    public Livro inserir(Livro livro) throws SQLException {
        return cadastrar(livro);
    }

    public Livro cadastrar(Livro livro) throws SQLException {
        String sql = "INSERT INTO livros (titulo, autor, categoria_id, categoria, total_paginas, sinopse, capa_url, conteudo, isbn, data_publicacao, criado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, livro.getTitulo().trim());
            pstmt.setString(2, livro.getAutor().trim());
            if (livro.getCategoriaId() > 0) {
                pstmt.setInt(3, livro.getCategoriaId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.setString(4, livro.getCategoria() != null ? livro.getCategoria().trim() : "Geral");
            pstmt.setInt(5, Math.max(1, livro.getTotalPaginas()));
            pstmt.setString(6, livro.getSinopse() != null ? livro.getSinopse().trim() : "");
            pstmt.setString(7, livro.getCapaUrl() != null ? livro.getCapaUrl().trim() : "");
            pstmt.setString(8, livro.getConteudo() != null ? livro.getConteudo() : "");
            pstmt.setString(9, livro.getIsbn() != null ? livro.getIsbn().trim() : "");
            pstmt.setString(10, livro.getDataPublicacao() != null ? livro.getDataPublicacao().trim() : "");

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
        String sql = "UPDATE livros SET titulo = ?, autor = ?, categoria_id = ?, categoria = ?, total_paginas = ?, "
                + "sinopse = ?, capa_url = ?, conteudo = ?, isbn = ?, data_publicacao = ?, atualizado_em = CURRENT_TIMESTAMP "
                + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, livro.getTitulo().trim());
            pstmt.setString(2, livro.getAutor().trim());
            if (livro.getCategoriaId() > 0) {
                pstmt.setInt(3, livro.getCategoriaId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.setString(4, livro.getCategoria() != null ? livro.getCategoria().trim() : "Geral");
            pstmt.setInt(5, Math.max(1, livro.getTotalPaginas()));
            pstmt.setString(6, livro.getSinopse() != null ? livro.getSinopse().trim() : "");
            pstmt.setString(7, livro.getCapaUrl() != null ? livro.getCapaUrl().trim() : "");
            pstmt.setString(8, livro.getConteudo() != null ? livro.getConteudo() : "");
            pstmt.setString(9, livro.getIsbn() != null ? livro.getIsbn().trim() : "");
            pstmt.setString(10, livro.getDataPublicacao() != null ? livro.getDataPublicacao().trim() : "");
            pstmt.setInt(11, livro.getId());

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
        Livro l = new Livro();
        l.setId(rs.getInt("id"));
        l.setTitulo(rs.getString("titulo"));
        l.setAutor(rs.getString("autor"));
        l.setCategoriaId(rs.getInt("categoria_id"));
        l.setCategoria(rs.getString("categoria"));
        l.setTotalPaginas(rs.getInt("total_paginas"));
        l.setSinopse(rs.getString("sinopse"));
        l.setCapaUrl(rs.getString("capa_url"));
        l.setConteudo(rs.getString("conteudo"));
        l.setMediaAvaliacao(rs.getDouble("media_nota"));
        l.setTotalAvaliacoes(rs.getInt("total_avaliacoes"));
        try {
            l.setIsbn(rs.getString("isbn"));
            l.setDataPublicacao(rs.getString("data_publicacao"));
            l.setCriadoEm(rs.getString("criado_em"));
            l.setAtualizadoEm(rs.getString("atualizado_em"));
        } catch (SQLException ignored) {
        }
        return l;
    }
}
