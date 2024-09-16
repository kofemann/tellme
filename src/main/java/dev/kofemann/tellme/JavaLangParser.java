package dev.kofemann.tellme;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.utils.SourceRoot;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.io.IOException;
import java.nio.file.Path;

public class JavaLangParser {


    private final SourceRoot sourceRoot;

    public JavaLangParser(String path) {
        this.sourceRoot = new SourceRoot(Path.of(path));
    }


    public void embed(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) throws IOException {

        sourceRoot.tryToParse().forEach(cu -> {
            cu.getResult().get().getPrimaryType()
                    .get().getMembers().stream()
                    .filter(BodyDeclaration::isMethodDeclaration)
                    .map(BodyDeclaration::asMethodDeclaration).forEach(m -> {
                String methodName = m.getName().getIdentifier();
                String className = cu.getResult().get().getPrimaryType().get().getFullyQualifiedName().get();

                // skip empty methods
                if (m.getBody().isEmpty()) {
                    return;
                }

                String methodBody = m.getBody().get().toString();
                TextSegment location = TextSegment.from(className + "#" + methodName);
                TextSegment method = TextSegment.from(methodBody);

                // map vector to location
                Embedding embedding = embeddingModel.embed(method).content();

                embeddingStore.add(embedding, location);
            });

        });
    }
}
