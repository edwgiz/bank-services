package com.github.edwgiz.sample.bank.build.tools.jooq;

import org.jooq.codegen.JavaGenerator;
import org.jooq.codegen.JavaWriter;
import org.jooq.meta.ColumnDefinition;

import static org.codehaus.plexus.util.StringUtils.isNotBlank;

/**
 * Example of Jooq code generation customizing.
 *
 * Overrides {@link #printColumnJPAAnnotation(JavaWriter, ColumnDefinition)} method to print an swagger annotation by
 * a column comment.
 */
public class SwaggerAwareJavaGenerator extends JavaGenerator {

    protected void printColumnJPAAnnotation(final JavaWriter out, final ColumnDefinition column) {
        super.printColumnJPAAnnotation(out, column);
        printColumnSwaggerAnnotation(out, column);
    }

    private void printColumnSwaggerAnnotation(final JavaWriter out, final ColumnDefinition column) {
        final String comment = column.getComment();
        if (isNotBlank(comment)) {
            out.tab(1).println("@%s(description = \"%s\")",
                    out.ref("io.swagger.v3.oas.annotations.media.Schema"), comment);
        }
    }
}
