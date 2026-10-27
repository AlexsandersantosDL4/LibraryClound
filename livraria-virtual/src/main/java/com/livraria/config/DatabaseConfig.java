package com.livraria.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gerenciador centralizado de conexões com pool HikariCP e otimizações SQLite:
 * - WAL mode (Write-Ahead Logging) para permitir leituras concorrentes com escritas
 * - Foreign Keys habilitadas (PRAGMA foreign_keys = ON)
 * - Busy Timeout de 5000ms para evitar erros SQLITE_BUSY em concorrência
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DB_FILE = "livraria.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static HikariDataSource dataSource;

    static {
        inicializarPool();
    }

    private static synchronized void inicializarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        try {
            logger.info("Inicializando Pool de Conexões HikariCP para SQLite: {}", new File(DB_FILE).getAbsolutePath());

            HikariConfig config = new HikariConfig();
            config.setPoolName("LibraryClound-HikariPool");
            config.setJdbcUrl(JDBC_URL);
            config.setDriverClassName("org.sqlite.JDBC");

            // Configurações do Pool adequadas ao SQLite
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(60000);
            config.setConnectionTimeout(10000);

            // PRAGMAs vitais do SQLite para performance e integridade referencial
            config.setConnectionInitSql(
                    "PRAGMA journal_mode = WAL; " +
                    "PRAGMA foreign_keys = ON; " +
                    "PRAGMA busy_timeout = 5000; " +
                    "PRAGMA synchronous = NORMAL;"
            );

            dataSource = new HikariDataSource(config);
            logger.info("Pool HikariCP inicializado com sucesso para o banco SQLite!");
        } catch (Exception e) {
            logger.error("Falha crítica ao configurar o pool HikariCP: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao inicializar pool de conexões SQLite", e);
        }
    }

    /**
     * Obtém uma conexão ativa gerenciada pelo pool HikariCP.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            inicializarPool();
        }
        return dataSource.getConnection();
    }

    /**
     * Encerra o pool de conexões ao finalizar o ciclo de vida da aplicação.
     */
    public static synchronized void fecharPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Encerrando pool HikariCP...");
            dataSource.close();
            dataSource = null;
        }
    }
}
