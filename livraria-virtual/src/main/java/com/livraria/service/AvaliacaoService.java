package com.livraria.service;

import com.livraria.dao.AvaliacaoDAO;
import com.livraria.dao.LivroDAO;
import com.livraria.dto.AvaliacaoRequest;
import com.livraria.dto.AvaliacaoResponse;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Avaliacao;
import com.livraria.model.Livro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Camada de Serviço para Avaliações de Livros com Estrelas (0.5 a 5.0) e Comentários.
 */
public class AvaliacaoService {

    private static final Logger logger = LoggerFactory.getLogger(AvaliacaoService.class);

    private final AvaliacaoDAO avaliacaoDAO;
    private final LivroDAO livroDAO;

    public AvaliacaoService() {
        this.avaliacaoDAO = new AvaliacaoDAO();
        this.livroDAO = new LivroDAO();
    }

    public AvaliacaoService(AvaliacaoDAO avaliacaoDAO, LivroDAO livroDAO) {
        this.avaliacaoDAO = avaliacaoDAO;
        this.livroDAO = livroDAO;
    }

    public AvaliacaoResponse avaliar(int usuarioId, AvaliacaoRequest request) {
        if (usuarioId <= 0) {
            throw new ValidationException("Identificador de usuário inválido.");
        }
        if (request == null || request.livroId() <= 0) {
            throw new ValidationException("Identificador de livro é obrigatório.");
        }
        if (request.nota() < 0.5 || request.nota() > 5.0) {
            throw new ValidationException("A nota deve estar entre 0.5 e 5.0 estrelas.");
        }

        try {
            Livro livro = livroDAO.buscarPorId(request.livroId());
            if (livro == null) {
                throw new ResourceNotFoundException("Livro com ID " + request.livroId() + " não encontrado.");
            }

            Avaliacao salva = avaliacaoDAO.salvarOuAtualizar(
                    usuarioId,
                    request.livroId(),
                    request.nota(),
                    request.comentario()
            );

            logger.info("Avaliação registrada com sucesso: usuário={}, livro={}, nota={}",
                    usuarioId, request.livroId(), salva.getNota());

            return AvaliacaoResponse.fromEntity(salva);
        } catch (SQLException e) {
            logger.error("Erro ao salvar avaliação: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao salvar avaliação: " + e.getMessage());
        }
    }

    public AvaliacaoResponse obterAvaliacaoDoUsuario(int usuarioId, int livroId) {
        if (usuarioId <= 0 || livroId <= 0) {
            return null;
        }
        try {
            Avaliacao a = avaliacaoDAO.buscarPorUsuarioELivro(usuarioId, livroId);
            return AvaliacaoResponse.fromEntity(a);
        } catch (SQLException e) {
            logger.error("Erro ao buscar avaliação do usuário: {}", e.getMessage(), e);
            return null;
        }
    }

    public double[] obterEstatisticas(int livroId) {
        if (livroId <= 0) {
            return new double[]{0.0, 0.0};
        }
        try {
            return avaliacaoDAO.obterEstatisticas(livroId);
        } catch (SQLException e) {
            logger.error("Erro ao obter estatísticas de avaliação para livro id={}: {}", livroId, e.getMessage(), e);
            return new double[]{0.0, 0.0};
        }
    }

    public List<AvaliacaoResponse> listarPorLivro(int livroId) {
        if (livroId <= 0) {
            throw new ValidationException("Identificador de livro inválido.");
        }
        try {
            return avaliacaoDAO.listarPorLivro(livroId).stream()
                    .map(AvaliacaoResponse::fromEntity)
                    .toList();
        } catch (SQLException e) {
            logger.error("Erro ao listar avaliações do livro: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao carregar avaliações: " + e.getMessage());
        }
    }
}
