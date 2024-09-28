package dev.kofemann.tellme;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class JavaLangParser {

    static private final Logger LOGGER = LoggerFactory.getLogger(JavaLangParser.class);

    private final Path root;

    public JavaLangParser(String path) {
        this.root = Path.of(path);
    }

    public void embed(BiConsumer<String, String> processor) throws IOException {

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    JavaType<?> javaClass = Roaster.parse(path.toFile());
                    if (javaClass instanceof JavaClassSource javaClassSource) {
                        javaClassSource.getMethods().forEach(m -> {

                            int start = m.getStartPosition();
                            int end = m.getEndPosition();

                            String fqmn = javaClass.getQualifiedName() + "#" + m.getName() + ":" + start + "-" + end;

                            String body = m.getBody();
                            if (body != null && !body.isEmpty()) {
                                processor.accept(fqmn, m.getBody());
                            }
                        });
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to parse file: {} : {}", path, e.getMessage());
                }
            });
        }
    }
}
