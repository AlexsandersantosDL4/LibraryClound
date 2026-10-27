package com.livraria.dao;

import com.livraria.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerenciador de conexão e inicialização do banco de dados SQLite para a LibraryClound.
 * Integrado ao pool HikariCP, com suporte a migrações idempotentes e normalização de categorias.
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String DB_FILE = "livraria.db";

    public static Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public static void inicializarBanco() {
        logger.info("Inicializando banco de dados SQLite em: {}", new File(DB_FILE).getAbsolutePath());

        String sqlCategorias = "CREATE TABLE IF NOT EXISTS categorias ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT NOT NULL UNIQUE, "
                + "slug TEXT NOT NULL UNIQUE, "
                + "descricao TEXT"
                + ");";

        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome_fantasia TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE, "
                + "senha TEXT NOT NULL, "
                + "tipo TEXT NOT NULL DEFAULT 'CLIENTE', "
                + "livros_lidos INTEGER NOT NULL DEFAULT 0, "
                + "foto_url TEXT, "
                + "descricao TEXT, "
                + "criado_em TEXT DEFAULT CURRENT_TIMESTAMP, "
                + "atualizado_em TEXT"
                + ");";

        String sqlLivros = "CREATE TABLE IF NOT EXISTS livros ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "titulo TEXT NOT NULL, "
                + "autor TEXT NOT NULL, "
                + "categoria_id INTEGER, "
                + "categoria TEXT NOT NULL, "
                + "total_paginas INTEGER NOT NULL, "
                + "sinopse TEXT, "
                + "capa_url TEXT, "
                + "conteudo TEXT, "
                + "isbn TEXT, "
                + "data_publicacao TEXT, "
                + "criado_em TEXT DEFAULT CURRENT_TIMESTAMP, "
                + "atualizado_em TEXT, "
                + "FOREIGN KEY(categoria_id) REFERENCES categorias(id)"
                + ");";

        String sqlMarcaPaginas = "CREATE TABLE IF NOT EXISTS marca_paginas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_id INTEGER NOT NULL, "
                + "livro_id INTEGER NOT NULL, "
                + "pagina_atual INTEGER NOT NULL DEFAULT 1, "
                + "finalizado INTEGER NOT NULL DEFAULT 0, "
                + "data_atualizacao TEXT NOT NULL, "
                + "UNIQUE(usuario_id, livro_id), "
                + "FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, "
                + "FOREIGN KEY(livro_id) REFERENCES livros(id) ON DELETE CASCADE"
                + ");";

        String sqlFavoritos = "CREATE TABLE IF NOT EXISTS favoritos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_id INTEGER NOT NULL, "
                + "livro_id INTEGER NOT NULL, "
                + "data_adicionado TEXT NOT NULL, "
                + "UNIQUE(usuario_id, livro_id), "
                + "FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, "
                + "FOREIGN KEY(livro_id) REFERENCES livros(id) ON DELETE CASCADE"
                + ");";

        String sqlAvaliacoes = "CREATE TABLE IF NOT EXISTS avaliacoes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_id INTEGER NOT NULL, "
                + "livro_id INTEGER NOT NULL, "
                + "nota REAL NOT NULL, "
                + "comentario TEXT, "
                + "data_avaliacao TEXT NOT NULL, "
                + "criado_em TEXT DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE(usuario_id, livro_id), "
                + "FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, "
                + "FOREIGN KEY(livro_id) REFERENCES livros(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlCategorias);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlLivros);
            stmt.execute(sqlMarcaPaginas);
            stmt.execute(sqlFavoritos);
            stmt.execute(sqlAvaliacoes);

            // Migrações seguras caso colunas novas ainda não existam no banco existente
            executarMigracao(stmt, "ALTER TABLE usuarios ADD COLUMN foto_url TEXT");
            executarMigracao(stmt, "ALTER TABLE usuarios ADD COLUMN descricao TEXT");
            executarMigracao(stmt, "ALTER TABLE usuarios ADD COLUMN criado_em TEXT DEFAULT CURRENT_TIMESTAMP");
            executarMigracao(stmt, "ALTER TABLE usuarios ADD COLUMN atualizado_em TEXT");

            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN conteudo TEXT");
            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN categoria_id INTEGER REFERENCES categorias(id)");
            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN isbn TEXT");
            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN data_publicacao TEXT");
            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN criado_em TEXT DEFAULT CURRENT_TIMESTAMP");
            executarMigracao(stmt, "ALTER TABLE livros ADD COLUMN atualizado_em TEXT");

            executarMigracao(stmt, "ALTER TABLE avaliacoes ADD COLUMN comentario TEXT");
            executarMigracao(stmt, "ALTER TABLE avaliacoes ADD COLUMN criado_em TEXT DEFAULT CURRENT_TIMESTAMP");

            // Seed inicial de categorias, administradores e livros de exemplo
            inserirCategoriasIniciais(conn);
            inserirAdminPadrao(conn);
            inserirLivrosIniciais(conn);

            logger.info("Banco SQLite configurado e atualizado com sucesso!");
        } catch (SQLException e) {
            logger.error("Erro ao inicializar o banco SQLite: {}", e.getMessage(), e);
        }
    }

    private static void executarMigracao(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
            // Coluna já existe ou migração já executada
        }
    }

    private static void inserirCategoriasIniciais(Connection conn) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM categorias";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String sql = "INSERT INTO categorias (nome, slug, descricao) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    adicionarCategoria(pstmt, "Tecnologia", "tecnologia", "Livros e manuais de computação, engenharia de software e inteligência artificial.");
                    adicionarCategoria(pstmt, "Ficção", "ficcao", "Narrativas literárias, romances e grandes histórias contemporâneas.");
                    adicionarCategoria(pstmt, "Fantasia", "fantasia", "Aventuras épicas, universos fantásticos e ficção especulativa.");
                    adicionarCategoria(pstmt, "Clássicos", "classicos", "Obras atemporais da literatura brasileira e mundial.");
                    adicionarCategoria(pstmt, "Romance", "romance", "Dramas românticos, relações humanas e histórias de época.");
                }
                logger.info("5 categorias oficiais inseridas no banco de dados.");
            }
        }

        // Atualizar categoria_id nos livros com base no nome da categoria
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE livros SET categoria_id = ("
                    + "  SELECT c.id FROM categorias c WHERE LOWER(c.nome) = LOWER(livros.categoria)"
                    + ") WHERE categoria_id IS NULL OR categoria_id = 0"
            );
        } catch (SQLException ignored) {
        }
    }

    private static void adicionarCategoria(PreparedStatement pstmt, String nome, String slug, String descricao) throws SQLException {
        pstmt.setString(1, nome);
        pstmt.setString(2, slug);
        pstmt.setString(3, descricao);
        pstmt.executeUpdate();
    }

    private static void inserirAdminPadrao(Connection conn) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM usuarios WHERE tipo = 'ADMIN'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String sqlInsert = "INSERT INTO usuarios (nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao) "
                        + "VALUES ('LibraryClound Admin', 'admin@livraria.com', '12345678', 'ADMIN', 0, "
                        + "'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', "
                        + "'Administrador e Curador Chefe da LibraryClound.')";
                stmt.executeUpdate(sqlInsert);

                String sqlLeitor = "INSERT INTO usuarios (nome_fantasia, email, senha, tipo, livros_lidos, foto_url, descricao) "
                        + "VALUES ('Alex Leitor', 'leitor@livraria.com', '1234', 'CLIENTE', 0, "
                        + "'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', "
                        + "'Apaixonado por ficção científica, tecnologia e literatura clássica.')";
                stmt.executeUpdate(sqlLeitor);

                logger.info("Usuários iniciais criados (Admin e Leitor).");
            }
        }
    }

    private static void inserirLivrosIniciais(Connection conn) throws SQLException {
        String sqlCheck = "SELECT COUNT(*) FROM livros";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String sql = "INSERT INTO livros (titulo, autor, categoria, categoria_id, total_paginas, sinopse, capa_url, conteudo, isbn) "
                        + "VALUES (?, ?, ?, (SELECT id FROM categorias WHERE LOWER(nome) = LOWER(?)), ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    adicionarLivro(pstmt, "Código Limpo (Clean Code)", "Robert C. Martin", "Tecnologia", 464,
                            "Mesmo um código ruim pode funcionar. Mas se o código não for limpo, ele pode acabar com uma empresa de desenvolvimento.",
                            "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=400",
                            "CAPÍTULO 1: Código Limpo\n\nHaverá código. Alguém poderia pensar que o código em breve deixará de ser um problema. Alguns pensam que estamos próximos do fim do código. Que em breve ele será gerado automaticamente em vez de escrito.\n\nEssa ideia é totalmente infundada. Nunca nos livraremos do código, pois o código representa os detalhes dos requisitos. Em algum nível, esses detalhes devem ser especificados com precisão matemática.\n\nEscrever código limpo é como pintar um quadro. A maioria de nós sabe quando um quadro está bem pintado ou mal pintado. Mas saber reconhecer boa arte não significa que sabemos pintar.",
                            "978-8576082675");

                    adicionarLivro(pstmt, "Entendendo Algoritmos", "Aditya Y. Bhargava", "Tecnologia", 264,
                            "Um guia ilustrado e prático para programadores e curiosos entenderem estruturas e algoritmos fundamentais.",
                            "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?w=400",
                            "CAPÍTULO 1: Introdução a Algoritmos\n\nUm algoritmo é um conjunto de instruções que realizam uma tarefa específica. Cada trecho de código que você escreve pode ser considerado um algoritmo.\n\nA pesquisa binária é um algoritmo que recebe uma lista ordenada de elementos e procura por um item. Se o item estiver na lista, a pesquisa binária retorna a sua localização. Caso contrário, retorna null.\n\nCom a pesquisa binária, você chuta um número intermediário e elimina a metade dos números restantes a cada tentativa!",
                            "978-8575225639");

                    adicionarLivro(pstmt, "O Pequeno Príncipe", "Antoine de Saint-Exupéry", "Ficção", 96,
                            "Uma fábula poética e filosófica sobre a essência humana, os laços que criamos e a beleza das coisas simples.",
                            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400",
                            "CAPÍTULO 1: A Jiboia e o Elefante\n\nCerta vez, quando eu tinha seis anos, vi num livro sobre a Floresta Virgem uma linda gravura. Representava uma serpente jiboia engolindo uma fera.\n\nMostrei minha obra-prima às pessoas grandes e perguntei se o meu desenho lhes dava medo.\n\nElas me responderam: 'Por que um chapéu daria medo?' Meu desenho não representava um chapéu. Representava uma serpente jiboia digerindo um elefante.",
                            "978-8522031474");

                    adicionarLivro(pstmt, "O Hobbit", "J.R.R. Tolkien", "Fantasia", 310,
                            "Bilbo Bolseiro vive uma vida tranquila no Condado até que Gandalf o convoca para uma grande aventura na Montanha Solitária.",
                            "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400",
                            "CAPÍTULO 1: Uma Festa Inesperada\n\nNum buraco no chão vivia um hobbit. Não um buraco nojento, sujo e úmido, cheio de restos de minhocas e com cheiro de lodo; nem um buraco seco, arenoso e vazio, sem nada em que sentar ou o que comer: era a toca de um hobbit, e isso significa conforto.\n\nTinha uma porta perfeitamente redonda como uma escotilha, pintada de verde, com uma maçaneta de latão amarelo bem no centro.",
                            "978-8595084742");

                    adicionarLivro(pstmt, "Dom Casmurro", "Machado de Assis", "Clássicos", 256,
                            "Bentinho relembra sua juventude, sua paixão por Capitu e as dúvidas eternas sobre fidelidade e ciúmes.",
                            "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=400",
                            "CAPÍTULO 1: Do Título\n\nUma noite destas, vindo da cidade para o Engenho Novo, encontrei no trem da Central um rapaz aqui do bairro, que eu conheço de vista e de chapéu.\n\nCumprimentou-me, sentou-se ao pé de mim, falou da lua e dos ministros, e acabou recitando-me versos.\n\nNo dia seguinte entraram a chamar-me de Dom Casmurro. Casmurro não está aqui no sentido de teimoso, mas de homem calado e metido consigo.",
                            "978-8535914849");

                    adicionarLivro(pstmt, "Orgulho e Preconceito", "Jane Austen", "Romance", 424,
                            "A relação tempestuosa entre a decidida Elizabeth Bennet e o orgulhoso aristocrata Sr. Darcy na Inglaterra do século XIX.",
                            "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400",
                            "CAPÍTULO 1: Uma Verdade Universal\n\nÉ uma verdade universalmente reconhecida que um homem solteiro, possuidor de uma grande fortuna, deve estar necessitado de uma esposa.\n\nPor menos conhecidos que sejam os sentimentos ou as opiniões de tal homem ao fixar-se numa vizinhança, esta verdade está de tal modo enraizada nos corações das famílias circundantes que ele é considerado a propriedade legítima de uma ou de outra das suas filhas.",
                            "978-8544001820");

                    logger.info("6 livros de acervo inicial inseridos.");
                }

                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.executeUpdate("INSERT INTO avaliacoes (usuario_id, livro_id, nota, comentario, data_avaliacao) VALUES (2, 1, 5.0, 'Obra fundamental para qualquer desenvolvedor!', '24/10/2026')");
                    stmt2.executeUpdate("INSERT INTO avaliacoes (usuario_id, livro_id, nota, comentario, data_avaliacao) VALUES (2, 3, 4.5, 'Poético, tocante e inesquecível.', '24/10/2026')");
                    stmt2.executeUpdate("INSERT INTO favoritos (usuario_id, livro_id, data_adicionado) VALUES (2, 1, '24/10/2026')");
                    stmt2.executeUpdate("INSERT INTO favoritos (usuario_id, livro_id, data_adicionado) VALUES (2, 4, '24/10/2026')");
                }
            }
        }
    }

    private static void adicionarLivro(PreparedStatement pstmt, String titulo, String autor, String categoria,
                                       int paginas, String sinopse, String capa, String conteudo, String isbn) throws SQLException {
        pstmt.setString(1, titulo);
        pstmt.setString(2, autor);
        pstmt.setString(3, categoria);
        pstmt.setString(4, categoria);
        pstmt.setInt(5, paginas);
        pstmt.setString(6, sinopse);
        pstmt.setString(7, capa);
        pstmt.setString(8, conteudo);
        pstmt.setString(9, isbn);
        pstmt.executeUpdate();
    }
}
