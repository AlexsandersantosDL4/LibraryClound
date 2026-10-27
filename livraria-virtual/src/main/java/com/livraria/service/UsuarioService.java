package com.livraria.service;

import com.livraria.dao.UsuarioDAO;
import com.livraria.dto.AuthResponse;
import com.livraria.dto.LoginRequest;
import com.livraria.dto.RegisterRequest;
import com.livraria.dto.UpdateProfileRequest;
import com.livraria.dto.UsuarioResponse;
import com.livraria.exception.AuthenticationException;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Role;
import com.livraria.model.Usuario;
import com.livraria.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Camada de Serviço para Regras de Negócio de Usuários e Autenticação.
 */
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioDAO usuarioDAO;
    private final JwtService jwtService;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAO();
        this.jwtService = new JwtService();
    }

    public UsuarioService(UsuarioDAO usuarioDAO, JwtService jwtService) {
        this.usuarioDAO = usuarioDAO;
        this.jwtService = jwtService;
    }

    public AuthResponse autenticar(LoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.senha() == null || request.senha().isBlank()) {
            throw new ValidationException("Informe o email e a senha para efetuar o login.");
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(request.email().trim(), request.senha().trim());
            if (usuario == null) {
                logger.warn("Tentativa de login inválida para o email: {}", request.email());
                throw new AuthenticationException("Email ou senha incorretos. Verifique suas credenciais.");
            }

            String token = jwtService.gerarToken(usuario);
            UsuarioResponse usuarioResp = UsuarioResponse.fromEntity(usuario);

            logger.info("Usuário autenticado com sucesso: id={}, email={}", usuario.getId(), usuario.getEmail());
            return new AuthResponse("Login realizado com sucesso!", token, usuarioResp);
        } catch (SQLException e) {
            logger.error("Erro no SQLite durante autenticação: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao autenticar usuário: " + e.getMessage());
        }
    }

    public AuthResponse cadastrar(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Dados de cadastro não informados.");
        }

        String nome = request.nomeFantasia() != null ? request.nomeFantasia().trim() : "";
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        String senha = request.senha() != null ? request.senha().trim() : "";
        String tipo = (request.tipo() != null && !request.tipo().isBlank()) ? request.tipo().trim() : Role.CLIENTE.name();

        if (nome.length() < 3 || nome.length() > 80) {
            throw new ValidationException("O Nome Fantasia deve conter entre 3 e 80 caracteres.");
        }

        if (email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Informe um endereço de e-mail válido.");
        }

        if (!Usuario.isSenhaValida(senha)) {
            throw new ValidationException("A senha informada é inválida. Informe até 32 caracteres.");
        }

        try {
            if (usuarioDAO.existeEmail(email)) {
                throw new ValidationException("Este e-mail já está cadastrado na LibraryClound.");
            }

            Usuario novo = new Usuario(nome, email, senha, tipo);
            novo.setFotoUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            novo.setDescricao("Novo leitor da LibraryClound!");

            Usuario salvo = usuarioDAO.cadastrar(novo);
            String token = jwtService.gerarToken(salvo);
            UsuarioResponse usuarioResp = UsuarioResponse.fromEntity(salvo);

            logger.info("Novo usuário cadastrado com sucesso: id={}, email={}", salvo.getId(), salvo.getEmail());
            return new AuthResponse("Cadastro realizado com sucesso na LibraryClound!", token, usuarioResp);
        } catch (SQLException e) {
            logger.error("Erro no SQLite durante cadastro: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao cadastrar usuário: " + e.getMessage());
        }
    }

    public UsuarioResponse buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidationException("ID de usuário inválido.");
        }

        try {
            Usuario u = usuarioDAO.buscarPorId(id);
            if (u == null) {
                throw new ResourceNotFoundException("Usuário não encontrado para o ID: " + id);
            }
            return UsuarioResponse.fromEntity(u);
        } catch (SQLException e) {
            logger.error("Erro ao buscar usuário por id: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao consultar perfil: " + e.getMessage());
        }
    }

    public UsuarioResponse atualizarPerfil(int id, UpdateProfileRequest request) {
        if (id <= 0) {
            throw new ValidationException("ID de usuário inválido.");
        }
        if (request == null || request.nomeFantasia() == null || request.nomeFantasia().isBlank()) {
            throw new ValidationException("O Nome Fantasia é obrigatório.");
        }

        String nome = request.nomeFantasia().trim();
        if (nome.length() < 3 || nome.length() > 80) {
            throw new ValidationException("O Nome Fantasia deve conter entre 3 e 80 caracteres.");
        }

        String desc = request.descricao() != null ? request.descricao().trim() : "";
        if (desc.length() > 150) {
            throw new ValidationException("A descrição de perfil não pode ultrapassar 150 caracteres (atual: " + desc.length() + ").");
        }

        String foto = request.fotoUrl() != null ? request.fotoUrl().trim() : "";

        try {
            Usuario atualizado = usuarioDAO.atualizarPerfil(id, nome, foto, desc);
            if (atualizado == null) {
                throw new ResourceNotFoundException("Usuário não encontrado para atualização.");
            }
            logger.info("Perfil atualizado com sucesso para o usuário id={}", id);
            return UsuarioResponse.fromEntity(atualizado);
        } catch (SQLException e) {
            logger.error("Erro ao atualizar perfil: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao atualizar perfil: " + e.getMessage());
        }
    }

    public List<UsuarioResponse> listarTodos() {
        try {
            return usuarioDAO.listarTodos().stream()
                    .map(UsuarioResponse::fromEntity)
                    .toList();
        } catch (SQLException e) {
            logger.error("Erro ao listar todos os usuários: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao listar usuários: " + e.getMessage());
        }
    }
}
