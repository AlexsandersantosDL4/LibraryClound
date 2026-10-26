package com.livraria.model;

/**
 * Modelo de dados que representa um Livro no catálogo da LibraryClound.
 * Atende aos requisitos:
 * - Informações bibliográficas (título, autor, categoria, páginas, sinopse, capa)
 * - Conteúdo completo do livro inserido pelo Administrador (independente de quantas páginas)
 * - Média de avaliação em estrelas e quantidade de avaliações
 */
public class Livro {
    private int id;
    private String titulo;
    private String autor;
    private String categoria;
    private int totalPaginas;
    private String sinopse;
    private String capaUrl;
    private String conteudo; // Conteúdo completo do livro inserido pelo admin
    private double mediaAvaliacao; // Média de 0.5 a 5.0 estrelas
    private int totalAvaliacoes;

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

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"titulo\":\"" + escapeJson(titulo) + "\","
                + "\"autor\":\"" + escapeJson(autor) + "\","
                + "\"categoria\":\"" + escapeJson(categoria) + "\","
                + "\"totalPaginas\":" + totalPaginas + ","
                + "\"sinopse\":\"" + escapeJson(sinopse) + "\","
                + "\"capaUrl\":\"" + escapeJson(capaUrl) + "\","
                + "\"conteudo\":\"" + escapeJson(conteudo != null ? conteudo : "") + "\","
                + "\"mediaAvaliacao\":" + String.format(java.util.Locale.US, "%.1f", mediaAvaliacao) + ","
                + "\"totalAvaliacoes\":" + totalAvaliacoes
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
