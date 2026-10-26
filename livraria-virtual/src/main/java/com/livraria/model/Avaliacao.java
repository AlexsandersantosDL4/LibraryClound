package com.livraria.model;

/**
 * Modelo de dados que representa a Avaliação de um Livro em estrelas (de 0.5 até 5.0).
 * Atende ao requisito:
 * "os usuarios tbm pode ter a opçao de avaliar os livros com a interaçao com estrelas de meia estrela ate 5 estrelas"
 */
public class Avaliacao {
    private int id;
    private int usuarioId;
    private int livroId;
    private double nota; // De 0.5 a 5.0 (em incrementos de 0.5)
    private String dataAvaliacao;

    public Avaliacao() {
    }

    public Avaliacao(int id, int usuarioId, int livroId, double nota, String dataAvaliacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.livroId = livroId;
        this.nota = validarNota(nota);
        this.dataAvaliacao = dataAvaliacao;
    }

    public Avaliacao(int usuarioId, int livroId, double nota) {
        this(0, usuarioId, livroId, nota, "");
    }

    public static double validarNota(double nota) {
        if (nota < 0.5) return 0.5;
        if (nota > 5.0) return 5.0;
        // Arredondar para o múltiplo de 0.5 mais próximo
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

    public String getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(String dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"usuarioId\":" + usuarioId + ","
                + "\"livroId\":" + livroId + ","
                + "\"nota\":" + String.format(java.util.Locale.US, "%.1f", nota) + ","
                + "\"dataAvaliacao\":\"" + (dataAvaliacao != null ? dataAvaliacao : "") + "\""
                + "}";
    }
}
