package io.github.digitalsmile.validation;

import javax.lang.model.element.Element;

/**
 * Annotation processing validation exception.
 */
public class ValidationException extends Exception {
    // Stores element exception occurs
    private final Element element;

    /**
     * Creates validation exception.
     *
     * @param message message of exception
     * @param element element to mark in IDE
     */
    public ValidationException(String message, Element element) {
        super(message);
        this.element = element;
    }

    /**
     * Creates validation exception.
     *
     * @param message message of exception
     */
    public ValidationException(String message) {
        this(message, null);
    }

    /**
     * Gets the element to mark exception in IDE.
     *
     * @return element to mark exception
     */
    public Element getElement() {
        return element;
    }
}
