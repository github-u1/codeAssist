package org.gradle.internal.typeconversion;


import org.gradle.internal.exceptions.DiagnosticsVisitor;

class CharSequenceNotationParser implements NotationConverter<String, String> {
    @Override
    public void convert(String notation, NotationConvertResult<? super String> result) throws TypeConversionException {
        result.converted(notation);
    }

    @Override
    public void describe(DiagnosticsVisitor visitor) {
        visitor.candidate("String or CharSequence instances.");
    }
}