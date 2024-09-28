package dev.kofemann.tellme;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.jboss.forge.roaster.Roaster;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import javax.sql.RowSetReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Tellme {

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileReader("tellme.properties"));

        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host(properties.getProperty("db.host", "localhost"))
                .port(Integer.parseInt(properties.getProperty("db.port", "5432")))
                .database(properties.getProperty("db.name"))
                .user(properties.getProperty("db.user"))
                .password(properties.getProperty("db.password"))
                .table(properties.getProperty("db.table"))
                .dimension(Integer.parseInt(properties.getProperty("db.dimension", "384")))
                .build();

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();


        Terminal terminal = TerminalBuilder.builder().type("ANSI").build();
        LineReader reader = LineReaderBuilder.builder()
                .appName("tellme")
                .terminal(terminal)
                .history(new DefaultHistory())
                .build();

        String question;

        double threshold = 0.7;

        String prompt = new AttributedString("tellme> ", AttributedStyle.DEFAULT.bold()).toAnsi();
        while ((question = reader.readLine(prompt)) != null) {
            question = question.trim();
            if (question.isEmpty()) {
                continue;
            }

            Embedding queryEmbedding = embeddingModel.embed(question).content();
            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 4);


            if (relevant.isEmpty() || relevant.get(0).score() < threshold) {
                AttributedString as = new AttributedString("No relevant embeddings found",
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
                terminal.writer().println(as.toAnsi());
            } else {
                relevant.stream().filter(e -> e.score() >= threshold).forEach(embeddingMatch -> {
                    AttributedString as = new AttributedString("Score: " + embeddingMatch.score() + "\n",
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());
                    terminal.writer().println(as.toAnsi());
                    terminal.writer().println(" : " + Roaster.format(embeddingMatch.embedded().text()));
                });
            }

            terminal.flush();
        }

    }
}
