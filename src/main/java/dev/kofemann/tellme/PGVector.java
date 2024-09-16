package dev.kofemann.tellme;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;

public class PGVector {

    public static void main(String[] args) throws IOException {

        DockerImageName dockerImageName = DockerImageName.parse("pgvector/pgvector:pg16");
        try (PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(dockerImageName)) {
            postgreSQLContainer.start();

            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(postgreSQLContainer.getHost())
                    .port(postgreSQLContainer.getFirstMappedPort())
                    .database(postgreSQLContainer.getDatabaseName())
                    .user(postgreSQLContainer.getUsername())
                    .password(postgreSQLContainer.getPassword())
                    .table("code_embeddings")
                    .dimension(384)
                    .build();

            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();


            // parse and fill the embedding store with java code
            JavaLangParser javaLangParser = new JavaLangParser("../nfs4j/core/src/main/java");
            javaLangParser.embed(embeddingStore, embeddingModel);

            // query for code in natural language
            String question = "create nfsv41 session";
            Embedding queryEmbedding = embeddingModel.embed(question).content();
            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 4);

            System.out.println("Question: " + question);
            System.out.println("Answer: ");

            if (relevant.isEmpty()) {
                System.out.println("No relevant embeddings found");
            } else {
                relevant.forEach(embeddingMatch -> {
                    /*
                        0.8759738507208494 : org.dcache.nfs.v4.NFS4Client#getSession
                        0.8669842541785753 : org.dcache.nfs.v4.NFS4Client#removeSession
                        0.8229844945934887 : org.dcache.nfs.v4.OperationDESTROY_SESSION#process
                        0.812421753983918 : org.dcache.nfs.v4.CompoundBuilder#withDestroysession
                     */
                    System.out.println("  " + embeddingMatch.score() + " : " + embeddingMatch.embedded().text());
                });
            }
            postgreSQLContainer.stop();
        }
    }

}
