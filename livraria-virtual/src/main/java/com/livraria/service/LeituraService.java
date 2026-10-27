package com.livraria.service;

import com.livraria.dao.LivroDAO;
import com.livraria.dao.MarcaPaginaDAO;
import com.livraria.dto.MarcaPaginaRequest;
import com.livraria.dto.MarcaPaginaResponse;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Livro;
import com.livraria.model.MarcaPagina;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Camada de Serviço para a Experiência de Leitura, Marca-Páginas e Conclusão de Livros.
 */
public class LeituraService {

    private static final Logger logger = LoggerFactory.getLogger(LeituraService.class);

    private final MarcaPaginaDAO marcaPaginaDAO;
    private final LivroDAO livroDAO;

    public LeituraService() {
        this.marcaPaginaDAO = new MarcaPaginaDAO();
        this.livroDAO = new LivroDAO();
    }

    public LeituraService(MarcaPaginaDAO marcaPaginaDAO, LivroDAO livroDAO) {
        this.marcaPaginaDAO = marcaPaginaDAO;
        this.livroDAO = livroDAO;
    }

    public MarcaPaginaResponse salvarMarcaPagina(int usuarioId, MarcaPaginaRequest request) {
        if (usuarioId <= 0) {
            throw new ValidationException("Identificador de usuário inválido.");
        }
        if (request == null || request.livroId() <= 0) {
            throw new ValidationException("Identificador do livro é obrigatório.");
        }

        try {
            Livro livro = livroDAO.buscarPorId(request.livroId());
            if (livro == null) {
                throw new ResourceNotFoundException("Livro com ID " + request.livroId() + " não encontrado.");
            }

            int totalPaginas = Math.max(1, livro.getTotalPaginas());
            int paginaDesejada = Math.max(1, Math.min(request.paginaAtual(), totalPaginas));

            MarcaPagina salvo = marcaPaginaDAO.salvarMarcaPagina(usuarioId, request.livroId(), paginaDesejada);
            logger.debug("Marca-páginas atualizado: usuário={}, livro={}, página={}/{}",
                    usuarioId, request.livroId(), paginaDesejada, totalPaginas);

            return MarcaPaginaResponse.fromEntity(salvo);
        } catch (SQLException e) {
            logger.error("Erro ao salvar marca-páginas: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao registrar marca-páginas: " + e.getMessage());
        }
    }

    public MarcaPaginaResponse consultarStatus(int usuarioId, int livroId) {
        if (usuarioId <= 0 || livroId <= 0) {
            throw new ValidationException("Identificadores de usuário e livro são obrigatórios.");
        }

        try {
            MarcaPagina mp = marcaPaginaDAO.buscarPorUsuarioELivro(usuarioId, livroId);
            if (mp != null) {
                return MarcaPaginaResponse.fromEntity(mp);
            }

            // Retorno padrão para início de leitura
            Livro livro = livroDAO.buscarPorId(livroId);
            int total = (livro != null) ? livro.getTotalPaginas() : 1;
            String titulo = (livro != null) ? livro.getTitulo() : "";

            return new MarcaPaginaResponse(0, usuarioId, livroId, titulo, 1, total, 0, false, "");
        } catch (SQLException e) {
            logger.error("Erro ao consultar status de leitura: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao consultar leitura: " + e.getMessage());
        }
    }

    public List<MarcaPaginaResponse> listarPorUsuario(int usuarioId) {
        if (usuarioId <= 0) {
            throw new ValidationException("Identificador de usuário inválido.");
        }
        try {
            return marcaPaginaDAO.listarPorUsuario(usuarioId).stream()
                    .map(MarcaPaginaResponse::fromEntity)
                    .toList();
        } catch (SQLException e) {
            logger.error("Erro ao listar marca-páginas do usuário: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao carregar leituras: " + e.getMessage());
        }
    }

    public int confirmarFinalizacao(int usuarioId, int livroId) {
        if (usuarioId <= 0 || livroId <= 0) {
            throw new ValidationException("Identificadores de usuário e livro são obrigatórios.");
        }

        try {
            Livro livro = livroDAO.buscarPorId(livroId);
            if (livro == null) {
                throw new ResourceNotFoundException("Livro não encontrado para finalização.");
            }

            int livrosLidosAtualizados = marcaPaginaDAO.confirmarFinalizacao(usuarioId, livroId);
            logger.info("Livro finalizado com sucesso! usuário={}, livroId={}, totalLidos={}",
                    usuarioId, livroId, livrosLidosAtualizados);

            return livrosLidosAtualizados;
        } catch (SQLException e) {
            logger.error("Erro ao finalizar livro: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao confirmar leitura concluída: " + e.getMessage());
        }
    }
}
