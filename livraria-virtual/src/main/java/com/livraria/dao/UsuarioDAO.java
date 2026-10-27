package com.livraria.dao;

import com.livraria.model.Usuario;
import com.livraria.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para operações de Usuários no SQLite com pool HikariCP.
 * Suporta BCrypt, migração transparente de senhas legadas e timestamps de auditoria.
 */
public class UsuarioDAO {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioDAO.class);

    public Usuario cadastrar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuarios (nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao, criado_em) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, CURRENT_TIMESTAMP)";

        String senhaSegura = PasswordEncoder.isBCryptHash(usuario.getSenha())
                ? usuario.getSenha()
                : PasswordEncoder.encode(usuario.getSenha());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, usuario.getNomeFantasia().trim());
            pstmt.setString(2, usuario.getEmail().trim().toLowerCase());
            pstmt.setString(3, senhaSegura);
            pstmt.setString(4, (usuario.getTipo() != null && !usuario.getTipo().isBlank()) ? usuario.getTipo() : "CLIENTE");
            pstmt.setString(5, usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "");
            pstmt.setString(6, usuario.getDescricao() != null ? usuario.getDescricao() : "");

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                    usuario.setSenha(senhaSegura);
                    usuario.setLivrosLidos(0);
                }
            }
            return usuario;
        }
    }

    public Usuario autenticar(String email, String senhaPlana) throws SQLException {
        String sql = "SELECT id, nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao, criado_em, atualizado_em "
                + "FROM usuarios WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashArmazenado = rs.getString("senha");
                    if (PasswordEncoder.matches(senhaPlana, hashArmazenado)) {
                        Usuario usuario = mapearUsuario(rs);

                        // Se a senha armazenada for texto plano antigo, migra para BCrypt automaticamente
                        if (!PasswordEncoder.isBCryptHash(hashArmazenado)) {
                            logger.info("Migrando senha legada para hash BCrypt para usuário id={}", usuario.getId());
                            atualizarSenha(usuario.getId(), PasswordEncoder.encode(senhaPlana));
                        }

                        return usuario;
                    }
                }
            }
        }
        return null;
    }

    public void atualizarSenha(int id, String novoHash) throws SQLException {
        String sql = "UPDATE usuarios SET senha = ?, atualizado_em = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, novoHash);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao, criado_em, atualizado_em "
                + "FROM usuarios WHERE id = ?";
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

    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT id, nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao, criado_em, atualizado_em "
                + "FROM usuarios WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Usuario atualizarPerfil(int id, String nomeFantasia, String fotoUrl, String descricao) throws SQLException {
        String descTratada = descricao;
        if (descTratada != null && descTratada.length() > 150) {
            descTratada = descTratada.substring(0, 150);
        }

        String sql = "UPDATE usuarios SET nome_fantasia = ?, foto_url = ?, descricao = ?, atualizado_em = CURRENT_TIMESTAMP WHERE id = ?";
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
        String sqlUpdate = "UPDATE usuarios SET livros_lidos = livros_lidos + 1, atualizado_em = CURRENT_TIMESTAMP WHERE id = ?";
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
        String sql = "SELECT id, nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao, criado_em, atualizado_em FROM usuarios ORDER BY id ASC";
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
        u.setSenha(rs.getString("senha"));
        u.setTipo(rs.getString("tipo"));
        u.setLivrosLidos(rs.getInt("livros_lidos"));
        u.setFotoUrl(rs.getString("foto_url"));
        u.setDescricao(rs.getString("descricao"));
        try {
            u.setCriadoEm(rs.getString("criado_em"));
            u.setAtualizadoEm(rs.getString("atualizado_em"));
        } catch (SQLException ignored) {
        }
        return u;
    }
}
