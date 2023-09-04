package org.manaslu.cache.core;

import com.google.auto.service.AutoService;
import org.manaslu.cache.core.annotations.Enhance;
import org.manaslu.cache.core.annotations.SubEnhanceEntity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("org.manaslu.cache.core.annotations.SubEnhanceEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class SubEnhancedEntityProcessor extends AbstractProcessor {
    private Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        var set = new HashSet<>(super.getSupportedAnnotationTypes());
        set.add(SubEnhanceEntity.class.getCanonicalName());
        return set;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(SubEnhanceEntity.class);
        for (Element element : elements) {
            if (element.getKind() == ElementKind.CLASS) {
                parseElement(element);
            }
        }
        return true;
    }

    void parseElement(Element element) {
        var name = element.toString();
        var packageName = name.substring(0, name.lastIndexOf('.'));
        var enclosedElements = element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE) && !e.getModifiers().contains(Modifier.STATIC)) // 去除私有
                .toList();
        StringBuilder sb = new StringBuilder()
                .append("package ").append(packageName).append(";\n\n")
                .append("""
                        import org.manaslu.cache.core.*;
                        import org.manaslu.cache.core.annotations.*;
                        import java.util.*;
                        """)
                .append("\n")
                .append(buildAnnotation(element, ""))
                .append("public class ").append(element.getSimpleName().toString()).append("$Proxy extends ").append(name)
                .append(" {\n\n")
                .append(buildConstruct(name))
                .append(buildOverrideMethod(enclosedElements))
                .append("}\n");
        try {
            JavaFileObject source = filer.createSourceFile(name + "$Proxy");
            Writer writer = source.openWriter();
            writer.write(sb.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {

        }
    }

    /**
     * 覆盖注解
     */
    private String buildAnnotation(Element element, String prefix) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors()
                .stream()
                .filter(e -> !e.getAnnotationType().toString().equals(Enhance.class.getCanonicalName()))
                .toList();
        var sb = new StringBuilder();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            var elementValues = annotationMirror.getElementValues();
            if (annotationMirror.getAnnotationType().toString().equals(SubEnhanceEntity.class.getCanonicalName())) {
                sb.append(prefix).append("@SubEntity").append("(");
            } else {
                sb.append(prefix).append("@").append(annotationMirror.getAnnotationType().toString()).append("(");
            }
            var list = elementValues.keySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).getSimpleName()).append(" = ").append(elementValues.get(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            elementValues.forEach((k, v) -> {

            });
            sb.append(")\n");
        }
        return sb.toString();
    }

    private String buildFieldAnnotation(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors()
                .stream()
                .filter(e -> !e.getAnnotationType().toString().equals(SubEnhanceEntity.class.getCanonicalName()))
                .filter(e -> !e.getAnnotationType().toString().equals(Enhance.class.getCanonicalName()))
                .toList();
        var sb = new StringBuilder();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            var elementValues = annotationMirror.getElementValues();
            sb.append("@").append(annotationMirror.getAnnotationType().toString()).append("(");
            var list = elementValues.keySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).getSimpleName()).append(" = ").append(elementValues.get(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            elementValues.forEach((k, v) -> {

            });
            sb.append(") ");
        }
        return sb.toString();
    }

    private boolean methodHasEnhance(ExecutableElement element) {
        var annotation = element.getAnnotationMirrors()
                .stream()
                .filter(e -> e.getAnnotationType().toString().equals(Enhance.class.getCanonicalName()))
                .findFirst()
                .orElse(null);
        return annotation != null;
    }

    /**
     * 覆盖方法
     */
    private String buildOverrideMethod(List<ExecutableElement> elements) {
        var sb = new StringBuilder();
        for (ExecutableElement element : elements) {
            var simpleName = element.getSimpleName();
            var modifiers = element.getModifiers();
            var parameters = element.getParameters();
            var returnType = element.getReturnType();
            sb.append(buildAnnotation(element, "\t"));
            sb.append("\t");
            if (modifiers.contains(Modifier.PUBLIC)) {
                sb.append("public ");
            } else if (modifiers.contains(Modifier.PROTECTED)) {
                sb.append("protected ");
            }
            sb.append(returnType.toString()).append(" ").append(simpleName).append("(");
            StringBuilder params = new StringBuilder();
            for (int i = 0; i < parameters.size(); i++) {
                var variableElement = parameters.get(i);
                sb.append(buildFieldAnnotation(variableElement))
                        .append(variableElement.asType().toString()).append(" ")
                        .append(variableElement.getSimpleName());
                params.append(variableElement.getSimpleName());
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                    params.append(", ");
                }
            }

            sb.append(") {\n");
            if (returnType.getKind() == TypeKind.VOID) {
                sb.append("\t\t").append("_raw.").append(simpleName).append("(")
                        .append(params)
                        .append(");\n");
                if (methodHasEnhance(element)) {
                    sb.append("\t\tthis._parent.dumpStrategy().update(new UpdateInfo(this._parent, _dumpFields));\n");
                }

            } else {
                sb.append("\t\t").append("var r = _raw.").append(simpleName).append("(")
                        .append(params)
                        .append(");\n");
                if (methodHasEnhance(element)) {
                    sb.append("\t\tthis._parent.dumpStrategy().update(new UpdateInfo(this._parent, _dumpFields));\n");
                }
                sb.append("\t\treturn r;\n");
            }
            sb.append("\t}\n\n");
        }
        return sb.toString();
    }

    /**
     * 新增构造方法
     */
    private String buildConstruct(String name) {
        var simpleName = name.substring(name.lastIndexOf('.') + 1);
        return String.format("""
                    private final %s _raw;
                    private final Set<String> _dumpFields;
                    private final AbstractEntity<?> _parent;
                    
                    public %s$Proxy(%s _raw, AbstractEntity<?> _parent, String updateName) {
                        this._raw = _raw;
                        this._parent = _parent;
                        this._dumpFields = Set.of(updateName);
                    }
                """, name, simpleName, name);
    }
}
