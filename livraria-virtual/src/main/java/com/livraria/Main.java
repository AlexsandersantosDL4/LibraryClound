package com.livraria;

import com.livraria.controller.AdminController;
import com.livraria.controller.AuthController;
import com.livraria.controller.AvaliacaoController;
import com.livraria.controller.FavoritoController;
import com.livraria.controller.LeituraController;
import com.livraria.controller.LivroController;
import com.livraria.controller.StaticFileHandler;
import com.livraria.dao.DatabaseConnection;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Ponto de entrada (Main) do sistema LibraryClound.
 * Inicializa o banco de dados SQLite, registra os Controllers da API REST
 * (incluindo Favoritos, Avaliações por Estrelas e Edição de Perfil)
 * e disponibiliza o frontend web na porta 8080.
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("            ☁️ 📚 SISTEMA LIBRARYCLOUND (BACK-END)             ");
        System.out.println("       Tecnologias: Java 17+, SQLite (JDBC) e API REST         ");
        System.out.println("===============================================================");

        // 1. Inicializa o banco SQLite
        try {
            DatabaseConnection.inicializarBanco();
        } catch (Exception e) {
            System.err.println("❌ Falha crítica ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Inicializa o servidor HTTP da API
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Mapeamento dos Controllers REST
            server.createContext("/api/auth", new AuthController());
            server.createContext("/api/livros", new LivroController());
            server.createContext("/api/leitura", new LeituraController());
            server.createContext("/api/admin", new AdminController());
            server.createContext("/api/favoritos", new FavoritoController());
            server.createContext("/api/avaliacoes", new AvaliacaoController());

            // Servir arquivos estáticos do Frontend (HTML/CSS/JS)
            String publicDir = new File("src/main/resources/public").getAbsolutePath();
            server.createContext("/", new StaticFileHandler(publicDir));

            // Pool de threads para requisições concorrentes
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("---------------------------------------------------------------");
            System.out.println("🚀 Servidor LibraryClound iniciado com sucesso!");
            System.out.println("🌐 Acesse no seu navegador: http://localhost:" + PORT);
            System.out.println("---------------------------------------------------------------");
            System.out.println("🔑 Credenciais para demonstração:");
            System.out.println("   👑 Admin/Dono:  admin@livraria.com | Senha: 12345678");
            System.out.println("   📖 Leitor:      leitor@livraria.com | Senha: 1234");
            System.out.println("---------------------------------------------------------------");
            System.out.println("📋 Principais recursos:");
            System.out.println("   • Edição de Perfil: Nome, Foto e Descrição (até 150 caracteres)");
            System.out.println("   • Favoritos: Salva e exibe no perfil (título, categoria e capa)");
            System.out.println("   • Leitor com Conteúdo: Texto completo inserido pelo admin");
            System.out.println("   • Avaliações: Sistema de estrelas (de 0.5 até 5.0 estrelas)");
            System.out.println("   • Marca-páginas e Botão Finalizar Livro (+1 no perfil)");
            System.out.println("===============================================================");

        } catch (IOException e) {
            System.err.println("❌ Erro ao iniciar o servidor HTTP na porta " + PORT + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
