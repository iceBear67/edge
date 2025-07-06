package io.ib67.edge;

import io.ib67.edge.enhance.AnnotationEnhancer;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AnnotationRuleParser {
    private final Map<String, String> aliases = new HashMap<>();
    private String currentAnnotationDescriptor = null;
    private final List<AnnotationEnhancer.EnhanceRule> rules = new ArrayList<>();

    private record SimpleEnhanceRule(
            AnnotationEnhancer.EnhanceType type,
            Predicate<String> name,
            String descriptor) implements AnnotationEnhancer.EnhanceRule {

        @Override
        public boolean shouldEnhance(AnnotationEnhancer.EnhanceType type, String descriptor) {
            // descriptor here is the descriptor of the target element
            return this.type == type && this.name.test(descriptor);
        }
    }

    public List<AnnotationEnhancer.EnhanceRule> parse(String input) {
        rules.clear();
        aliases.clear();
        currentAnnotationDescriptor = null;

        for (String line : input.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("import")) {
                parseImport(line);
            } else if (line.startsWith("@")) {
                // Store the annotation descriptor
                String annotationName = line.substring(1);
                currentAnnotationDescriptor = "L" + resolveAlias(annotationName).replace('.', '/') + ";";
            } else {
                parseRule(line);
            }
        }

        return rules;
    }

    private void parseImport(String line) {
        // Format: import package/name as Alias
        String[] parts = line.split("\\s+");
        if (parts.length == 4 && parts[2].equals("as")) {
            aliases.put(parts[3], parts[1]);
        }
    }


    private void parseRule(String line) {
        if (currentAnnotationDescriptor == null) {
            throw new IllegalStateException("No annotation specified before rule");
        }

        String[] parts = line.split("\\s+", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rule format: " + line);
        }

        var type = parseType(parts[0]);
        String targetSpec = parts[1];

        String expectedDescriptor = buildFullDescriptor(targetSpec);
        Predicate<String> matcher = descriptor -> descriptor.equals(expectedDescriptor);

        rules.add(new SimpleEnhanceRule(type, matcher, currentAnnotationDescriptor));
    }

    private String buildFullDescriptor(String targetSpec) {
        int dotIndex = targetSpec.indexOf('.');
        if (dotIndex == -1) {
            // Type only
            return resolveAlias(targetSpec).replace('.', '/');
        }

        String className = resolveAlias(targetSpec.substring(0, dotIndex));
        String memberSpec = targetSpec.substring(dotIndex + 1);

        int paramStart = memberSpec.indexOf('(');
        if (paramStart == -1) {
            // Field
            return className.replace('.', '/') + "." + memberSpec;
        }

        // Method
        String methodName = memberSpec.substring(0, paramStart);
        String methodSig = memberSpec.substring(paramStart); // includes () and return type

        StringBuilder fullDescriptor = new StringBuilder();
        fullDescriptor.append(className.replace('.', '/')).append('.');
        fullDescriptor.append(methodName);

        // Parse method signature
        int returnTypeStart = methodSig.indexOf(')') + 1;
        String paramsStr = methodSig.substring(1, methodSig.indexOf(')'));
        String returnType = methodSig.substring(returnTypeStart);

        fullDescriptor.append('(');

        if (!paramsStr.isEmpty()) {
            String[] params = paramsStr.split(",");
            for (String param : params) {
                param = param.trim();
                // If it's already a descriptor (starts with L, [, etc), use as is
                if (param.startsWith("L") || param.startsWith("[") || param.length() == 1) {
                    fullDescriptor.append(param);
                } else {
                    // Convert class name to descriptor
                    fullDescriptor.append('L')
                            .append(resolveAlias(param).replace('.', '/'))
                            .append(';');
                }
            }
        }

        fullDescriptor.append(')');

        // Append return type as is if it's a descriptor, otherwise convert
        if (returnType.startsWith("L") || returnType.startsWith("[") || returnType.length() == 1) {
            fullDescriptor.append(returnType);
        } else if (!returnType.isEmpty()) {
            fullDescriptor.append('L')
                    .append(resolveAlias(returnType).replace('.', '/'))
                    .append(';');
        }

        return fullDescriptor.toString();
    }


    private String resolveAlias(String name) {
        return aliases.getOrDefault(name, name);
    }

    private AnnotationEnhancer.EnhanceType parseType(String type) {
        return switch (type.toUpperCase()) {
            case "TYPE" -> AnnotationEnhancer.EnhanceType.CLASS;
            case "METHOD" -> AnnotationEnhancer.EnhanceType.METHOD;
            case "FIELD" -> AnnotationEnhancer.EnhanceType.FIELD;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}