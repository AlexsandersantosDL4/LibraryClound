package com.livraria;

import com.livraria.config.DatabaseConfig;
import com.livraria.controller.AdminController;
import com.livraria.controller.AuthController;
import com.livraria.controller.AvaliacaoController;
import com.livraria.controller.CategoriaController;
import com.livraria.controller.FavoritoController;
import com.livraria.controller.LeituraController;
import com.livraria.controller.LivroController;
import com.livraria.controller.StaticFileHandler;
import com.livraria.dao.DatabaseConnection;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Ponto de entrada (Main) do sistema LibraryClound v2.0.
 * Utiliza Java 21 Virtual Threads para alta concorrência,
 * banco de dados SQLite otimizado com pool HikariCP,
 * autenticação JWT com criptografia BCrypt e API REST completa.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 8080;

    public static void main(String[] args) {
        logger.info("===============================================================");
        logger.info("            ☁️ 📚 SISTEMA LIBRARYCLOUND (BACK-END v2.0)         ");
        logger.info("       Tecnologias: Java 21 LTS (Virtual Threads), SQLite HikariCP");
        logger.info("===============================================================");

        // 1. Inicializa o banco SQLite e migrações
        try {
            DatabaseConnection.inicializarBanco();
        } catch (Exception e) {
            logger.error("❌ Falha crítica ao inicializar o banco de dados: {}", e.getMessage(), e);
            return;
        }

        // 2. Registra shutdown hook para fechar o pool de conexões de forma segura
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Encerrando aplicação LibraryClound...");
            DatabaseConfig.fecharPool();
        }));

        // 3. Inicializa o servidor HTTP com Virtual Threads do Java 21
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Mapeamento dos Controllers REST
            server.createContext("/api/auth", new AuthController());
            server.createContext("/api/livros", new LivroController());
            server.createContext("/api/categorias", new CategoriaController());
            server.createContext("/api/leitura", new LeituraController());
            server.createContext("/api/admin", new AdminController());
            server.createContext("/api/favoritos", new FavoritoController());
            server.createContext("/api/avaliacoes", new AvaliacaoController());

            // Resolução do diretório do frontend: verifica primeiro ../frontend ou pasta dedicada
            File frontendDir = new File("frontend");
            if (!frontendDir.exists()) {
                frontendDir = new File("../frontend");
            }
            if (!frontendDir.exists()) {
                frontendDir = new File("src/main/resources/public");
            }

            server.createContext("/", new StaticFileHandler(frontendDir.getAbsolutePath()));

            // Executor com Virtual Threads do Java 21 para máxima concorrência
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            logger.info("---------------------------------------------------------------");
            logger.info("🚀 Servidor LibraryClound iniciado com sucesso!");
            logger.info("🌐 Acesse no seu navegador: http://localhost:{}", PORT);
            logger.info("⚡ Executor ativo: Java 21 Virtual Threads");
            logger.info("📁 Frontend servido a partir de: {}", frontendDir.getAbsolutePath());
            logger.info("---------------------------------------------------------------");
            logger.info("🔑 Credenciais para demonstração:");
            logger.info("   👑 Admin/Dono:  admin@livraria.com | Senha: 12345678");
            logger.info("   📖 Leitor:      leitor@livraria.com | Senha: 1234");
            logger.info("===============================================================");

        } catch (IOException e) {
            logger.error("❌ Erro ao iniciar o servidor HTTP na porta {}: {}", PORT, e.getMessage(), e);
        }
    }
}
