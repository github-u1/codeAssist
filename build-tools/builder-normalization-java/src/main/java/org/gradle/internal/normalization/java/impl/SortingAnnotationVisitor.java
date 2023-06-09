package org.gradle.internal.normalization.java.impl;

import org.objectweb.asm.AnnotationVisitor;

import com.google.common.collect.Lists;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class SortingAnnotationVisitor extends AnnotationVisitor {

    private final List<AnnotationValue<?>> annotationValues = Lists.newLinkedList();
    private final AnnotationMember annotation;

    private SortingAnnotationVisitor parentVisitor;
    private String annotationValueName;
    private String arrayValueName;

    public SortingAnnotationVisitor(AnnotationMember parentAnnotation, AnnotationVisitor av) {
        super(Opcodes.ASM7, av);
        this.annotation = parentAnnotation;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        AnnotationMember annotation = new AnnotationMember(desc, true);
        SortingAnnotationVisitor visitor =
                new SortingAnnotationVisitor(annotation, super.visitAnnotation(name, desc));
        visitor.parentVisitor = this;
        visitor.annotationValueName = nameOrValue(name);
        return visitor;
    }

    @Override
    public void visit(String name, Object value) {
        annotationValues.add(new SimpleAnnotationValue(nameOrValue(name), value));
        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        SortingAnnotationVisitor visitor = new SortingAnnotationVisitor(annotation, super.visitArray(name));
        visitor.arrayValueName = name;
        return visitor;
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        annotationValues.add(new EnumAnnotationValue(nameOrValue(name), value, desc));
        super.visitEnum(name, desc, value);
    }

    @Override
    public void visitEnd() {
        if (annotationValueName != null) {
            AnnotationAnnotationValue value = new AnnotationAnnotationValue(annotationValueName, annotation);
            parentVisitor.annotationValues.add(value);
            annotationValueName = null;
        } else if (arrayValueName != null) {
            ArrayAnnotationValue value = new ArrayAnnotationValue(
                    arrayValueName, annotationValues.toArray(new AnnotationValue<?>[0]));
            annotation.addValue(value);
            arrayValueName = null;
        }
        annotation.addValues(annotationValues);
        annotationValues.clear();
        super.visitEnd();
    }

    private String nameOrValue(String name) {
        return name == null ? "value" : name;
    }
}