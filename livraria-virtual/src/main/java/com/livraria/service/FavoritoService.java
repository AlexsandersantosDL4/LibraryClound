package com.livraria.service;

import com.livraria.dao.FavoritoDAO;
import com.livraria.dao.LivroDAO;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Favorito;
import com.livraria.model.Livro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Camada de Serviço para a Aba de Favoritos de Usuários.
 */
public class FavoritoService {

    private static final Logger logger = LoggerFactory.getLogger(FavoritoService.class);

    private final FavoritoDAO favoritoDAO;
    private final LivroDAO livroDAO;

    public FavoritoService() {
        this.favoritoDAO = new FavoritoDAO();
        this.livroDAO = new LivroDAO();
    }

    public FavoritoService(FavoritoDAO favoritoDAO, LivroDAO livroDAO) {
        this.favoritoDAO = favoritoDAO;
        this.livroDAO = livroDAO;
    }

    public boolean toggleFavorito(int usuarioId, int livroId) {
        if (usuarioId <= 0 || livroId <= 0) {
            throw new ValidationException("Identificadores de usuário e livro são obrigatórios.");
        }

        try {
            Livro livro = livroDAO.buscarPorId(livroId);
            if (livro == null) {
                throw new ResourceNotFoundException("Livro não encontrado para favoritar.");
            }

            boolean favoritado = favoritoDAO.toggleFavorito(usuarioId, livroId);
            logger.info("Estado de favorito alterado: usuário={}, livro={}, favoritado={}",
                    usuarioId, livroId, favoritado);
            return favoritado;
        } catch (SQLException e) {
            logger.error("Erro ao alternar favorito: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao atualizar favoritos: " + e.getMessage());
        }
    }

    public boolean isFavorito(int usuarioId, int livroId) {
        if (usuarioId <= 0 || livroId <= 0) {
            return false;
        }
        try {
            return favoritoDAO.isFavorito(usuarioId, livroId);
        } catch (SQLException e) {
            logger.error("Erro ao consultar favorito: {}", e.getMessage(), e);
            return false;
        }
    }

    public List<Favorito> listarPorUsuario(int usuarioId) {
        if (usuarioId <= 0) {
            throw new ValidationException("Identificador de usuário inválido.");
        }
        try {
            return favoritoDAO.listarPorUsuario(usuarioId);
        } catch (SQLException e) {
            logger.error("Erro ao listar favoritos do usuário: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao carregar favoritos: " + e.getMessage());
        }
    }
}
