package com.livraria.service;

import com.livraria.dao.CategoriaDAO;
import com.livraria.dto.CategoriaRequest;
import com.livraria.dto.CategoriaResponse;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.model.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;

/**
 * Camada de Serviço para Categorias de Livros.
 */
public class CategoriaService {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaService.class);
    private final CategoriaDAO categoriaDAO;

    public CategoriaService() {
        this.categoriaDAO = new CategoriaDAO();
    }

    public CategoriaService(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = categoriaDAO;
    }

    public List<CategoriaResponse> listarTodas() {
        try {
            return categoriaDAO.listarTodas().stream()
                    .map(CategoriaResponse::fromEntity)
                    .toList();
        } catch (SQLException e) {
            logger.error("Erro ao listar categorias: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao consultar categorias: " + e.getMessage());
        }
    }

    public CategoriaResponse buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidationException("ID de categoria inválido.");
        }
        try {
            Categoria c = categoriaDAO.buscarPorId(id);
            if (c == null) {
                throw new ResourceNotFoundException("Categoria ID " + id + " não encontrada.");
            }
            return CategoriaResponse.fromEntity(c);
        } catch (SQLException e) {
            logger.error("Erro ao buscar categoria id={}: {}", id, e.getMessage(), e);
            throw new BusinessException("Falha interna ao consultar categoria: " + e.getMessage());
        }
    }

    public CategoriaResponse salvar(CategoriaRequest request) {
        if (request == null || request.nome() == null || request.nome().isBlank()) {
            throw new ValidationException("O nome da categoria é obrigatório.");
        }

        String nome = request.nome().trim();
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? gerarSlug(request.slug())
                : gerarSlug(nome);
        String desc = request.descricao() != null ? request.descricao().trim() : "";

        try {
            if (categoriaDAO.existePorNomeOuSlug(nome, slug)) {
                throw new ValidationException("Já existe uma categoria cadastrada com este nome ou slug.");
            }

            Categoria c = new Categoria(nome, slug, desc);
            Categoria salva = categoriaDAO.cadastrar(c);
            logger.info("Nova categoria cadastrada: id={}, nome='{}'", salva.getId(), salva.getNome());
            return CategoriaResponse.fromEntity(salva);
        } catch (SQLException e) {
            logger.error("Erro ao cadastrar categoria: {}", e.getMessage(), e);
            throw new BusinessException("Falha interna ao salvar categoria: " + e.getMessage());
        }
    }

    private String gerarSlug(String input) {
        if (input == null) return "";
        String semAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
