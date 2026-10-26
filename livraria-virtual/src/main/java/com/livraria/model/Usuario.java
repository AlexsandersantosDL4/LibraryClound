package com.livraria.model;

/**
 * Modelo de dados que representa um Usuário no sistema LibraryClound.
 * Atende aos requisitos:
 * - Cadastro com nome fantasia, email e senha (máx 8 números)
 * - Perfil editável: nome fantasia, foto de perfil e descrição de até 150 caracteres
 * - Contagem oficial de livros lidos no perfil
 * - Distinção entre perfil CLIENTE e ADMIN
 */
public class Usuario {
    private int id;
    private String nomeFantasia;
    private String email;
    private String senha;
    private String tipo; // "ADMIN" ou "CLIENTE"
    private int livrosLidos;
    private String fotoUrl;
    private String descricao; // Até 150 caracteres

    public Usuario() {
    }

    public Usuario(int id, String nomeFantasia, String email, String senha, String tipo, int livrosLidos, String fotoUrl, String descricao) {
        this.id = id;
        this.nomeFantasia = nomeFantasia;
        this.email = email;
        this.senha = senha;
        this.tipo = tipo;
        this.livrosLidos = livrosLidos;
        this.fotoUrl = fotoUrl;
        this.descricao = descricao;
    }

    public Usuario(String nomeFantasia, String email, String senha, String tipo) {
        this(0, nomeFantasia, email, senha, tipo, 0, "", "");
    }

    // Regra de negócio: A senha deve conter apenas números e no máximo 8 dígitos
    public static boolean isSenhaValida(String senha) {
        if (senha == null) return false;
        String limpa = senha.trim();
        return limpa.matches("^[0-9]{1,8}$");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.tipo);
    }

    public int getLivrosLidos() {
        return livrosLidos;
    }

    public void setLivrosLidos(int livrosLidos) {
        this.livrosLidos = livrosLidos;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        if (descricao != null && descricao.length() > 150) {
            this.descricao = descricao.substring(0, 150);
        } else {
            this.descricao = descricao;
        }
    }

    /**
     * Serialização simples para JSON (segura, omitindo a senha para respostas da API).
     */
    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"nomeFantasia\":\"" + escapeJson(nomeFantasia) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"tipo\":\"" + escapeJson(tipo) + "\","
                + "\"livrosLidos\":" + livrosLidos + ","
                + "\"fotoUrl\":\"" + escapeJson(fotoUrl != null ? fotoUrl : "") + "\","
                + "\"descricao\":\"" + escapeJson(descricao != null ? descricao : "") + "\""
                + "}";
    }

    private String escapeJson(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
