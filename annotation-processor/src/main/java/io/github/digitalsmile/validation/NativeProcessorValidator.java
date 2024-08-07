package io.github.digitalsmile.validation;

import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.library.NativeMemoryLibrary;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;
import io.github.digitalsmile.headers.model.NativeMemoryNode;

import javax.annotation.processing.Messager;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validation class used to process annotation validations.
 */
public class NativeProcessorValidator {
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;

    // https://stackoverflow.com/a/69168419
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)*[a-z0-9_]*$");

    /**
     * Creates validation instance
     *
     * @param messager     messager
     * @param elementUtils elements utility
     * @param typeUtils    types utility
     */
    public NativeProcessorValidator(Messager messager, Elements elementUtils, Types typeUtils) {
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    /**
     * Validates file system path.
     *
     * @param path path to validate
     * @return path if validation passed
     * @throws ValidationException if validation fails
     */
    public String validatePath(String path) throws ValidationException {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            throw new ValidationException("Malformed path: " + path);
        }
        return path;
    }

    /**
     * Validates java name to be used.
     *
     * @param javaName java name to validate
     * @return java name if validation passed
     * @throws ValidationException if validation fails
     */
    public String validateJavaName(String javaName) throws ValidationException {
        if (!SourceVersion.isIdentifier(javaName)) {
            throw new ValidationException("Malformed java name: " + javaName);
        } else if (SourceVersion.isKeyword(javaName)) {
            throw new ValidationException("Malformed java name (is a keyword): " + javaName);
        }
        return javaName;
    }

    /**
     * Validates package name to be used.
     *
     * @param packageName package name to validate
     * @return package name if validation passed
     * @throws ValidationException if validation fails
     */
    public String validatePackageName(String packageName) throws ValidationException {
        if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
            throw new ValidationException("Malformed package name: " + packageName);
        }
        return packageName;
    }

    /**
     * Supported java classes in functions generation
     */
    private static final List<String> SUPPORTED_JAVA_CLASSES = List.of(String.class.getName());

    /**
     * Validate a manual function.
     *
     * @param element element annotated with {@link io.github.digitalsmile.annotation.function.NativeManualFunction}
     * @throws ValidationException if validation fails
     */
    public void validateManualFunction(Element element) throws ValidationException {
        if (!(element instanceof ExecutableElement manualFunction)) {
            return;
        }
        var throwType = elementUtils.getTypeElement(NativeMemoryException.class.getName()).asType();
        if (!manualFunction.getThrownTypes().contains(throwType)) {
            throw new ValidationException("Method must throw NativeMemoryException", manualFunction);
        }

        var returnsCount = manualFunction.getParameters().stream().filter(p -> p.getAnnotation(Returns.class) != null).count();
        if (returnsCount > 1) {
            throw new ValidationException("Only one @Returns annotation is allowed in method declaration", manualFunction);
        }
        var returnType = manualFunction.getReturnType();
        for (VariableElement parameter : manualFunction.getParameters()) {
            var returns = parameter.getAnnotation(Returns.class);
            var parameterType = parameter.asType();
            if (returns != null && !typeUtils.isSameType(parameterType, returnType)) {
                throw new ValidationException("Parameter '" + parameter.getSimpleName() + "' annotated with @Returns must be the same type as return method type", parameter);
            }

            if (!checkType(parameterType)) {
                throw new ValidationException("Parameter object type must implement NativeMemoryLayout class or be in supported java classes(" + String.join(", ", SUPPORTED_JAVA_CLASSES) + ")", parameter);
            }
        }

        var returns = manualFunction.getParameters().stream().filter(p -> p.getAnnotation(Returns.class) != null).findFirst().orElse(null);
        for (TypeParameterElement typeParameterElement : manualFunction.getTypeParameters()) {
            for (TypeMirror type : typeParameterElement.getBounds()) {
                if (returns != null && typeUtils.isSameType(returns.asType(), typeParameterElement.asType()) && !type.toString().equals(NativeMemoryLayout.class.getName())) {
                    throw new ValidationException("Type parameter used in return type must extend NativeMemoryLayout interface", typeParameterElement);
                }
            }
        }
    }

    /**
     * Checks the type for support by annotation processing.
     *
     * @param type type to be checked
     * @return true if type is supported, false otherwise
     */
    private boolean checkType(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY -> {
                var arrayType = (ArrayType) type;
                return checkType(arrayType.getComponentType());
            }
            case DECLARED, TYPEVAR -> {
                if (type instanceof DeclaredType declaredType && !declaredType.getTypeArguments().isEmpty()) {
                    type = typeUtils.erasure(declaredType);
                }

                if (!hasNativeMemoryLayout(type) && !SUPPORTED_JAVA_CLASSES.contains(type.toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Recursive function to checks if type is supertype of {@link NativeMemoryLayout}.
     *
     * @param type type to be checked
     * @return true if type is supertype of {@link NativeMemoryLayout}
     */
    private boolean hasNativeMemoryLayout(TypeMirror type) {
        for (TypeMirror superType : typeUtils.directSupertypes(type)) {
            var superTypeName = superType.toString();
            if (superTypeName.equals(Object.class.getName())) {
                continue;
            }
            if (superTypeName.equals(NativeMemoryLayout.class.getName()) || hasNativeMemoryLayout(superType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates automatic functions to be parsed.
     *
     * @param parsed                    parsed model of header file
     * @param automaticFunctionElements options to be used with automated parsed functions
     */
    public void validateAutomaticFunctions(List<NativeMemoryNode> parsed, NativeMemoryLibrary automaticFunctionElements) {
        //TBD
    }
}
