package com.livraria.model;

/**
 * Entidade de Categoria de Livros na LibraryClound.
 */
public class Categoria {
    private int id;
    private String nome;
    private String slug;
    private String descricao;
    private int totalLivros;

    public Categoria() {
    }

    public Categoria(int id, String nome, String slug, String descricao) {
        this.id = id;
        this.nome = nome;
        this.slug = slug;
        this.descricao = descricao;
    }

    public Categoria(String nome, String slug, String descricao) {
        this(0, nome, slug, descricao);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getTotalLivros() {
        return totalLivros;
    }

    public void setTotalLivros(int totalLivros) {
        this.totalLivros = totalLivros;
    }
}
