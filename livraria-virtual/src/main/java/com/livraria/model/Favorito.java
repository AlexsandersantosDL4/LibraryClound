package com.livraria.model;

/**
 * Modelo de dados que representa um Livro Favoritado pelo usuário.
 * Atende ao requisito:
 * "uma aba para favoritos(criando assim uma função para favoritar os livros e essa
 *  função deve aparecer no perfil dos usuários mostrando, nome, categoria e a capa do livro)"
 */
public class Favorito {
    private int id;
    private int usuarioId;
    private int livroId;
    private String livroTitulo;
    private String livroCategoria;
    private String livroCapaUrl;
    private String dataAdicionado;

    public Favorito() {
    }

    public Favorito(int id, int usuarioId, int livroId, String livroTitulo, String livroCategoria, String livroCapaUrl, String dataAdicionado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.livroId = livroId;
        this.livroTitulo = livroTitulo;
        this.livroCategoria = livroCategoria;
        this.livroCapaUrl = livroCapaUrl;
        this.dataAdicionado = dataAdicionado;
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

    public String getLivroCategoria() {
        return livroCategoria;
    }

    public void setLivroCategoria(String livroCategoria) {
        this.livroCategoria = livroCategoria;
    }

    public String getLivroCapaUrl() {
        return livroCapaUrl;
    }

    public void setLivroCapaUrl(String livroCapaUrl) {
        this.livroCapaUrl = livroCapaUrl;
    }

    public String getDataAdicionado() {
        return dataAdicionado;
    }

    public void setDataAdicionado(String dataAdicionado) {
        this.dataAdicionado = dataAdicionado;
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"usuarioId\":" + usuarioId + ","
                + "\"livroId\":" + livroId + ","
                + "\"livroTitulo\":\"" + escapeJson(livroTitulo) + "\","
                + "\"livroCategoria\":\"" + escapeJson(livroCategoria) + "\","
                + "\"livroCapaUrl\":\"" + escapeJson(livroCapaUrl) + "\","
                + "\"dataAdicionado\":\"" + escapeJson(dataAdicionado) + "\""
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
