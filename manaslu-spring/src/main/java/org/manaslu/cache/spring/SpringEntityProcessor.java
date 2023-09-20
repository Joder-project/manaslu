package org.manaslu.cache.spring;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.manaslu.cache.spring.GenerateRepository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class SpringEntityProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        var set = new HashSet<>(super.getSupportedAnnotationTypes());
        set.add(GenerateRepository.class.getCanonicalName());
        set.add(EnableManaslu.class.getCanonicalName());
        return set;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var mainClass = roundEnv.getElementsAnnotatedWith(EnableManaslu.class).stream().findFirst()
                .filter(e -> e.getKind() == ElementKind.CLASS)
                .map(e -> {
                    var name = e.toString();
                    return name.substring(0, name.lastIndexOf("."));
                })
                .orElse(null);
        var elements = roundEnv.getElementsAnnotatedWith(GenerateRepository.class).stream()
                .filter(e -> e.getKind() == ElementKind.CLASS)
                .collect(Collectors.toSet());
        createConfiguration(elements, mainClass);
        return true;
    }

    void createConfiguration(Set<? extends Element> elements, String basePackage) {
        if (basePackage == null) {
            basePackage = elements.stream().findFirst()
                    .map(e -> {
                        var name = e.toString();
                        return name.substring(0, name.lastIndexOf("."));
                    })
                    .orElse("org.manaslu.cache.spring");
        }
        StringBuilder sb = new StringBuilder()
                .append(String.format("""
                        package %s;
                                          
                        import java.util.*;
                        import org.manaslu.cache.core.*;
                        import org.springframework.context.annotation.*;
                        import org.springframework.boot.autoconfigure.condition.*;
                                                
                        @Configuration
                        class SpringEntityGeneratorConfiguration {
                            
                        """, basePackage));
        List<String> classes = new ArrayList<>();
        for (Element element : elements) {
            sb.append(buildBeanMethod(((TypeElement) element), classes));
        }
        sb.append(String.format("\tprivate final List<Class<? extends AbstractEntity<?>>> registerClasses = List.of(%s);\n", String.join(", ", classes)));
        sb.append("""
                    
                    SpringEntityGeneratorConfiguration(EntityTypeManager manager) {
                        manager.registerTypes(registerClasses);
                    }
                    
                """);
        sb.append("}\n");
        try {
            JavaFileObject source = filer.createSourceFile(basePackage + ".SpringEntityGeneratorConfiguration");
            Writer writer = source.openWriter();
            writer.write(sb.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {

        }
    }

    String buildBeanMethod(TypeElement element, List<String> classes) {
        String id = element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> ((ExecutableElement) e))
                .filter(e -> "id".equals(e.getSimpleName().toString()))
                .map(e -> e.getReturnType().toString())
                .findFirst()
                .orElse("");
        String type = element.getQualifiedName().toString();
        String simpleName = element.getSimpleName().toString();
        classes.add(type + ".class");
        return String.format("""
                                        
                    @Bean
                    Repository<%s, %s> %sRepository(RepositoryFactory factory){
                        return factory.getRepository(%s.class);
                    }
                """, id, type, simpleName, type);
    }
}
