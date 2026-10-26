package com.livraria.dao;

import com.livraria.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de Usuários no SQLite.
 * Suporta autenticação, edição de perfil (nome, foto, descrição até 150 chars)
 * e contagem de livros lidos.
 */
public class UsuarioDAO {

    public Usuario cadastrar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuarios (nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, usuario.getNomeFantasia());
            pstmt.setString(2, usuario.getEmail().trim().toLowerCase());
            pstmt.setString(3, usuario.getSenha().trim());
            pstmt.setString(4, (usuario.getTipo() != null && !usuario.getTipo().isBlank()) ? usuario.getTipo() : "CLIENTE");
            pstmt.setString(5, usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "");
            pstmt.setString(6, usuario.getDescricao() != null ? usuario.getDescricao() : "");

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                    usuario.setLivrosLidos(0);
                }
            }
            return usuario;
        }
    }

    public Usuario autenticar(String email, String senha) throws SQLException {
        String sql = "SELECT id, nome_fantasia, email, tipo, livros_lidos, foto_url, descricao "
                + "FROM usuarios WHERE LOWER(email) = LOWER(?) AND senha = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim());
            pstmt.setString(2, senha.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nome_fantasia, email, tipo, livros_lidos, foto_url, descricao FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Atualização do próprio perfil pelo usuário.
     * Requisito: "podendo editar seu nome, por uma foto, criar uma descriçao sobre si de ate 150 caracteres"
     */
    public Usuario atualizarPerfil(int id, String nomeFantasia, String fotoUrl, String descricao) throws SQLException {
        // Garantir limite de 150 caracteres para descrição
        String descTratada = descricao;
        if (descTratada != null && descTratada.length() > 150) {
            descTratada = descTratada.substring(0, 150);
        }

        String sql = "UPDATE usuarios SET nome_fantasia = ?, foto_url = ?, descricao = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeFantasia.trim());
            pstmt.setString(2, fotoUrl != null ? fotoUrl.trim() : "");
            pstmt.setString(3, descTratada != null ? descTratada.trim() : "");
            pstmt.setInt(4, id);

            pstmt.executeUpdate();
        }

        return buscarPorId(id);
    }

    public boolean existeEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int incrementarLivrosLidos(int usuarioId) throws SQLException {
        String sqlUpdate = "UPDATE usuarios SET livros_lidos = livros_lidos + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {

            pstmt.setInt(1, usuarioId);
            pstmt.executeUpdate();
        }

        Usuario u = buscarPorId(usuarioId);
        return (u != null) ? u.getLivrosLidos() : 0;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nome_fantasia, email, tipo, livros_lidos, foto_url, descricao FROM usuarios ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        }
        return lista;
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNomeFantasia(rs.getString("nome_fantasia"));
        u.setEmail(rs.getString("email"));
        u.setTipo(rs.getString("tipo"));
        u.setLivrosLidos(rs.getInt("livros_lidos"));
        u.setFotoUrl(rs.getString("foto_url"));
        u.setDescricao(rs.getString("descricao"));
        return u;
    }
}
