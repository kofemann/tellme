package dev.kofemann.tellme;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * This is the main class of the Tellme application.
 * It uses the {@link PgVectorEmbeddingStore} to store embeddings of Java methods.
 * The embeddings are generated using the {@link AllMiniLmL6V2EmbeddingModel}.
 */
public class PGVector {

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

        // parse and fill the embedding store with java code
        JavaLangParser javaLangParser = new JavaLangParser("../nfs4j/core/src/main/java");
        javaLangParser.embed((fqmn, body) -> {
            TextSegment location = TextSegment.from("// " + fqmn +"\n {" + body + "}");
            TextSegment method = TextSegment.from(body);
            Embedding embedding = embeddingModel.embed(method).content();

            embeddingStore.add(embedding, location);
        });
    }

}
