package com.livraria.model;

/**
 * Modelo de dados que representa o Marca-Páginas e o Progresso de Leitura do usuário em um livro.
 * Controla em qual página o usuário parou e se o livro já foi finalizado.
 */
public class MarcaPagina {
    private int id;
    private int usuarioId;
    private int livroId;
    private String livroTitulo;
    private int paginaAtual;
    private int totalPaginas;
    private boolean finalizado;
    private String dataAtualizacao;

    public MarcaPagina() {
    }

    public MarcaPagina(int id, int usuarioId, int livroId, String livroTitulo, int paginaAtual, int totalPaginas, boolean finalizado, String dataAtualizacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.livroId = livroId;
        this.livroTitulo = livroTitulo;
        this.paginaAtual = paginaAtual;
        this.totalPaginas = totalPaginas;
        this.finalizado = finalizado;
        this.dataAtualizacao = dataAtualizacao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getLivroId() {
        return livroId;
    }

    public void setLivroId(int livroId) {
        this.livroId = livroId;
    }

    public String getLivroTitulo() {
        return livroTitulo;
    }

    public void setLivroTitulo(String livroTitulo) {
        this.livroTitulo = livroTitulo;
    }

    public int getPaginaAtual() {
        return paginaAtual;
    }

    public void setPaginaAtual(int paginaAtual) {
        this.paginaAtual = paginaAtual;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public void setFinalizado(boolean finalizado) {
        this.finalizado = finalizado;
    }

    public String getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(String dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public int getProgressoPercentual() {
        if (totalPaginas <= 0) return 0;
        return Math.min(100, Math.round(((float) paginaAtual / totalPaginas) * 100));
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"usuarioId\":" + usuarioId + ","
                + "\"livroId\":" + livroId + ","
                + "\"livroTitulo\":\"" + (livroTitulo != null ? escapeJson(livroTitulo) : "") + "\","
                + "\"paginaAtual\":" + paginaAtual + ","
                + "\"totalPaginas\":" + totalPaginas + ","
                + "\"progressoPercentual\":" + getProgressoPercentual() + ","
                + "\"finalizado\":" + finalizado + ","
                + "\"dataAtualizacao\":\"" + (dataAtualizacao != null ? escapeJson(dataAtualizacao) : "") + "\""
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
