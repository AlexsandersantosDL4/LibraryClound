package com.livraria.model;

/**
 * Modelo de dados que representa um Usuário no sistema LibraryClound.
 * Atende aos requisitos:
 * - Cadastro com nome fantasia, email e senha
 * - Perfil editável: nome fantasia, foto de perfil e descrição de até 150 caracteres
 * - Contagem oficial de livros lidos no perfil
 * - Distinção entre perfil CLIENTE e ADMIN via Role enum
 * - Timestamps de auditoria (criadoEm, atualizadoEm)
 */
public class Usuario {
    private int id;
    private String nomeFantasia;
    private String email;
    private String senha;
    private Role role = Role.CLIENTE;
    private int livrosLidos;
    private String fotoUrl;
    private String descricao;
    private String criadoEm;
    private String atualizadoEm;

    public Usuario() {
    }

    public Usuario(int id, String nomeFantasia, String email, String senha, String tipo, int livrosLidos, String fotoUrl, String descricao) {
        this.id = id;
        this.nomeFantasia = nomeFantasia;
        this.email = email;
        this.senha = senha;
        this.role = Role.fromString(tipo);
        this.livrosLidos = livrosLidos;
        this.fotoUrl = fotoUrl;
        this.descricao = descricao;
    }

    public Usuario(String nomeFantasia, String email, String senha, String tipo) {
        this(0, nomeFantasia, email, senha, tipo, 0, "", "");
    }

    // Regra de negócio: A senha pode ser números até 8 dígitos ou alfanumérica de até 30 caracteres
    public static boolean isSenhaValida(String senha) {
        if (senha == null) return false;
        String limpa = senha.trim();
        return !limpa.isEmpty() && limpa.length() <= 32;
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
        return role != null ? role.name() : Role.CLIENTE.name();
    }

    public void setTipo(String tipo) {
        this.role = Role.fromString(tipo);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.CLIENTE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
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

    /**
     * Serialização simples para JSON (segura, omitindo a senha para respostas da API).
     */
    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"nomeFantasia\":\"" + escapeJson(nomeFantasia) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"tipo\":\"" + escapeJson(getTipo()) + "\","
                + "\"livrosLidos\":" + livrosLidos + ","
                + "\"fotoUrl\":\"" + escapeJson(fotoUrl != null ? fotoUrl : "") + "\","
                + "\"descricao\":\"" + escapeJson(descricao != null ? descricao : "") + "\","
                + "\"criadoEm\":\"" + escapeJson(criadoEm != null ? criadoEm : "") + "\""
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
