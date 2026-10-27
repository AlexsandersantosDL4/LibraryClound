package com.livraria.model;

/**
 * Modelo de dados que representa a Avaliação de um Livro em estrelas (de 0.5 até 5.0)
 * com suporte a comentário textual opcional.
 */
public class Avaliacao {
    private int id;
    private int usuarioId;
    private String usuarioNome;
    private int livroId;
    private double nota; // De 0.5 a 5.0 (em incrementos de 0.5)
    private String comentario;
    private String dataAvaliacao;
    private String criadoEm;

    public Avaliacao() {
    }

    public Avaliacao(int id, int usuarioId, int livroId, double nota, String comentario, String dataAvaliacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.livroId = livroId;
        this.nota = validarNota(nota);
        this.comentario = comentario;
        this.dataAvaliacao = dataAvaliacao;
    }

    public Avaliacao(int id, int usuarioId, int livroId, double nota, String dataAvaliacao) {
        this(id, usuarioId, livroId, nota, "", dataAvaliacao);
    }

    public Avaliacao(int usuarioId, int livroId, double nota) {
        this(0, usuarioId, livroId, nota, "", "");
    }

    public static double validarNota(double nota) {
        if (nota < 0.5) return 0.5;
        if (nota > 5.0) return 5.0;
        return Math.round(nota * 2.0) / 2.0;
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

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public int getLivroId() {
        return livroId;
    }

    public void setLivroId(int livroId) {
        this.livroId = livroId;
    }

    public double getNota() {
        return nota;
    }

    public void setNota(double nota) {
        this.nota = validarNota(nota);
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(String dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public String getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(String criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"usuarioId\":" + usuarioId + ","
                + "\"usuarioNome\":\"" + escapeJson(usuarioNome != null ? usuarioNome : "") + "\","
                + "\"livroId\":" + livroId + ","
                + "\"nota\":" + String.format(java.util.Locale.US, "%.1f", nota) + ","
                + "\"comentario\":\"" + escapeJson(comentario != null ? comentario : "") + "\","
                + "\"dataAvaliacao\":\"" + (dataAvaliacao != null ? dataAvaliacao : "") + "\""
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
