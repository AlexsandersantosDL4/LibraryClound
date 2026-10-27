package com.livraria.model;

/**
 * Modelo de dados que representa um Livro no catálogo da LibraryClound.
 * Atende aos requisitos:
 * - Informações bibliográficas (título, autor, categoria com id normalizado, páginas, sinopse, capa)
 * - ISBN e data de publicação
 * - Conteúdo completo do livro inserido pelo Administrador
 * - Média de avaliação em estrelas e quantidade de avaliações
 * - Auditoria (criadoEm, atualizadoEm)
 */
public class Livro {
    private int id;
    private String titulo;
    private String autor;
    private int categoriaId;
    private String categoria; // Nome da categoria para compatibilidade
    private int totalPaginas;
    private String sinopse;
    private String capaUrl;
    private String conteudo; // Conteúdo completo do livro inserido pelo admin
    private double mediaAvaliacao; // Média de 0.5 a 5.0 estrelas
    private int totalAvaliacoes;
    private String isbn;
    private String dataPublicacao;
    private String criadoEm;
    private String atualizadoEm;

    public Livro() {
    }

    public Livro(int id, String titulo, String autor, String categoria, int totalPaginas, String sinopse, String capaUrl, String conteudo, double mediaAvaliacao, int totalAvaliacoes) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.totalPaginas = totalPaginas;
        this.sinopse = sinopse;
        this.capaUrl = capaUrl;
        this.conteudo = conteudo;
        this.mediaAvaliacao = mediaAvaliacao;
        this.totalAvaliacoes = totalAvaliacoes;
    }

    public Livro(String titulo, String autor, String categoria, int totalPaginas, String sinopse, String capaUrl, String conteudo) {
        this(0, titulo, autor, categoria, totalPaginas, sinopse, capaUrl, conteudo, 0.0, 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public int getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public String getCapaUrl() {
        return capaUrl;
    }

    public void setCapaUrl(String capaUrl) {
        this.capaUrl = capaUrl;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public double getMediaAvaliacao() {
        return mediaAvaliacao;
    }

    public void setMediaAvaliacao(double mediaAvaliacao) {
        this.mediaAvaliacao = mediaAvaliacao;
    }

    public int getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public void setTotalAvaliacoes(int totalAvaliacoes) {
        this.totalAvaliacoes = totalAvaliacoes;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(String dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public String getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(String criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(String atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"titulo\":\"" + escapeJson(titulo) + "\","
                + "\"autor\":\"" + escapeJson(autor) + "\","
                + "\"categoriaId\":" + categoriaId + ","
                + "\"categoria\":\"" + escapeJson(categoria != null ? categoria : "") + "\","
                + "\"totalPaginas\":" + totalPaginas + ","
                + "\"sinopse\":\"" + escapeJson(sinopse) + "\","
                + "\"capaUrl\":\"" + escapeJson(capaUrl) + "\","
                + "\"conteudo\":\"" + escapeJson(conteudo != null ? conteudo : "") + "\","
                + "\"mediaAvaliacao\":" + String.format(java.util.Locale.US, "%.1f", mediaAvaliacao) + ","
                + "\"totalAvaliacoes\":" + totalAvaliacoes + ","
                + "\"isbn\":\"" + escapeJson(isbn != null ? isbn : "") + "\","
                + "\"dataPublicacao\":\"" + escapeJson(dataPublicacao != null ? dataPublicacao : "") + "\","
                + "\"criadoEm\":\"" + escapeJson(criadoEm != null ? criadoEm : "") + "\""
                + "}";
    }

    private String escapeJson(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
