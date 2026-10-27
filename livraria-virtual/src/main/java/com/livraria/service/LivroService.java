package com.livraria.service;

import com.livraria.dao.CategoriaDAO;
import com.livraria.dao.LivroDAO;
import com.livraria.dto.LivroRequest;
import com.livraria.dto.LivroResponse;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Categoria;
import com.livraria.model.Livro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Camada de Serviço para Regras de Negócio do Catálogo de Livros e Curadoria Administrativa.
 */
public class LivroService {

    private static final Logger logger = LoggerFactory.getLogger(LivroService.class);

    private final LivroDAO livroDAO;
    private final CategoriaDAO categoriaDAO;

    public LivroService() {
        this.livroDAO = new LivroDAO();
        this.categoriaDAO = new CategoriaDAO();
    }

    public LivroService(LivroDAO livroDAO, CategoriaDAO categoriaDAO) {
        this.livroDAO = livroDAO;
        this.categoriaDAO = categoriaDAO;
    }

    public List<LivroResponse> listarTodos(String termo, String categoria) {
        try {
            List<Livro> livros;
            if ((termo != null && !termo.isBlank()) || (categoria != null && !categoria.isBlank())) {
                livros = livroDAO.pesquisar(termo, categoria);
            } else {
                livros = livroDAO.listarTodos();
            }
            return livros.stream().map(LivroResponse::fromEntity).toList();
        } catch (SQLException e) {
            logger.error("Erro ao listar livros: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao consultar catálogo: " + e.getMessage());
        }
    }

    public LivroResponse buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidationException("ID de livro inválido.");
        }
        try {
            Livro livro = livroDAO.buscarPorId(id);
            if (livro == null) {
                throw new ResourceNotFoundException("Livro com ID " + id + " não encontrado.");
            }
            return LivroResponse.fromEntity(livro);
        } catch (SQLException e) {
            logger.error("Erro ao buscar livro id={}: {}", id, e.getMessage(), e);
            throw new BusinessException("Falha interna ao obter livro: " + e.getMessage());
        }
    }

    public LivroResponse salvar(LivroRequest request) {
        validarLivroRequest(request);

        Livro livro = new Livro();
        preencherDadosLivro(livro, request);

        try {
            Livro salvo = livroDAO.cadastrar(livro);
            logger.info("Novo livro cadastrado com sucesso: id={}, título='{}'", salvo.getId(), salvo.getTitulo());
            return LivroResponse.fromEntity(salvo);
        } catch (SQLException e) {
            logger.error("Erro ao cadastrar livro: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao salvar livro no catálogo: " + e.getMessage());
        }
    }

    public LivroResponse atualizar(int id, LivroRequest request) {
        if (id <= 0) {
            throw new ValidationException("ID de livro inválido para atualização.");
        }
        validarLivroRequest(request);

        try {
            Livro existente = livroDAO.buscarPorId(id);
            if (existente == null) {
                throw new ResourceNotFoundException("Livro com ID " + id + " não encontrado para edição.");
            }

            existente.setId(id);
            preencherDadosLivro(existente, request);

            livroDAO.atualizar(existente);
            Livro atualizado = livroDAO.buscarPorId(id);
            logger.info("Livro atualizado com sucesso: id={}, título='{}'", id, atualizado != null ? atualizado.getTitulo() : "");
            return LivroResponse.fromEntity(atualizado);
        } catch (SQLException e) {
            logger.error("Erro ao atualizar livro id={}: {}", id, e.getMessage(), e);
            throw new BusinessException("Falha interna ao atualizar livro: " + e.getMessage());
        }
    }

    public void excluir(int id) {
        if (id <= 0) {
            throw new ValidationException("ID de livro inválido para exclusão.");
        }
        try {
            Livro existente = livroDAO.buscarPorId(id);
            if (existente == null) {
                throw new ResourceNotFoundException("Livro não encontrado para exclusão.");
            }
            livroDAO.excluir(id);
            logger.info("Livro excluído com sucesso do catálogo: id={}", id);
        } catch (SQLException e) {
            logger.error("Erro ao excluir livro id={}: {}", id, e.getMessage(), e);
            throw new BusinessException("Falha interna ao excluir livro: " + e.getMessage());
        }
    }

    public List<String> listarCategoriasExistentes() {
        try {
            return livroDAO.listarCategoriasExistentes();
        } catch (SQLException e) {
            logger.error("Erro ao listar categorias: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao carregar categorias: " + e.getMessage());
        }
    }

    private void validarLivroRequest(LivroRequest req) {
        if (req == null) {
            throw new ValidationException("Dados do livro não fornecidos.");
        }
        if (req.titulo() == null || req.titulo().isBlank()) {
            throw new ValidationException("O título do livro é obrigatório.");
        }
        if (req.autor() == null || req.autor().isBlank()) {
            throw new ValidationException("O autor do livro é obrigatório.");
        }
        if (req.totalPaginas() <= 0) {
            throw new ValidationException("O total de páginas deve ser maior que zero.");
        }
    }

    private void preencherDadosLivro(Livro livro, LivroRequest req) {
        livro.setTitulo(req.titulo().trim());
        livro.setAutor(req.autor().trim());
        livro.setTotalPaginas(req.totalPaginas());
        livro.setSinopse(req.sinopse() != null ? req.sinopse().trim() : "");
        livro.setCapaUrl(req.capaUrl() != null ? req.capaUrl().trim() : "");
        livro.setConteudo(req.conteudo() != null ? req.conteudo() : "");
        livro.setIsbn(req.isbn() != null ? req.isbn().trim() : "");
        livro.setDataPublicacao(req.dataPublicacao() != null ? req.dataPublicacao().trim() : "");

        // Resolução de Categoria
        if (req.categoriaId() > 0) {
            livro.setCategoriaId(req.categoriaId());
            try {
                Categoria c = categoriaDAO.buscarPorId(req.categoriaId());
                if (c != null) {
                    livro.setCategoria(c.getNome());
                }
            } catch (SQLException ignored) {
            }
        } else if (req.categoria() != null && !req.categoria().isBlank()) {
            livro.setCategoria(req.categoria().trim());
            try {
                Categoria c = categoriaDAO.buscarPorSlug(req.categoria().trim().toLowerCase());
                if (c != null) {
                    livro.setCategoriaId(c.getId());
                }
            } catch (SQLException ignored) {
            }
        } else {
            livro.setCategoria("Geral");
        }
    }
}
