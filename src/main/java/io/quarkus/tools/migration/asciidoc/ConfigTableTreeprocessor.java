package io.quarkus.tools.migration.asciidoc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.extension.Treeprocessor;

public class ConfigTableTreeprocessor extends Treeprocessor {

    @Override
    public Document process(Document document) {
        int rowId = 0;
        int searchFieldId = 0;

        List<StructuralNode> tables = document.findBy(Map.of("context", ":table"));
        for (StructuralNode node : tables) {
            if (!(node instanceof Table table)) {
                continue;
            }
            if (!table.hasRole("configuration-reference")) {
                continue;
            }

            table.addRole("configuration-reference-all-rows");

            for (Row row : table.getBody()) {
                if (row.getCells().isEmpty()) {
                    continue;
                }
                Cell firstCell = row.getCells().get(0);
                Document innerDoc = firstCell.getInnerDocument();
                if (innerDoc == null) {
                    continue;
                }

                for (StructuralNode block : findByRole(innerDoc, "description")) {
                    rowId++;
                    if (isCollapsible(block)) {
                        block.setId("conf-collapsible-desc-" + rowId);
                        block.addRole("description-collapsed");
                        Block btnBlock = createBlock(innerDoc, "paragraph",
                                "<i class=\"fa fa-chevron-down\"></i><span>Show more</span>",
                                new HashMap<>());
                        btnBlock.addRole("description-decoration");
                        innerDoc.getBlocks().add(btnBlock);
                    } else {
                        block.setId("conf-non-collapsible-desc-" + rowId);
                    }
                }
            }

            if (table.hasRole("searchable") && table.getParent() instanceof StructuralNode parent) {
                List<StructuralNode> siblings = parent.getBlocks();
				int tableIndex = logicalIndexOf( siblings, table );
                if (tableIndex > 0) {
                    StructuralNode caption = siblings.get(tableIndex - 1);
                    String captionContent = caption.getContent() != null
                            ? caption.getContent().toString()
                            : "";
                    searchFieldId++;
                    Block newCaption = createBlock(parent, "paragraph",
                            captionContent + " <input type=\"search\" id=\"config-search-%d\" placeholder=\"FILTER CONFIGURATION\" disabled>".formatted(searchFieldId),
                            new HashMap<>());
                    siblings.set(tableIndex - 1, newCaption);
                }
            }
        }

        if (rowId > 0) {
            document.setAttribute("quarkus-config-max-row-id", rowId, true);
        }

        return document;
    }

	/**
	 * Returns the <em>logical</em> (0-based) index of {@code node} within {@code blocks}, or -1 if absent.
	 *
	 * <p>This works around an AsciidoctorJ/JRuby bridge behaviour. When a document has a header, the parser
	 * shifts the header off the body block list, leaving the underlying {@code RubyArray} with a non-zero
	 * internal {@code begin} offset. JRuby's {@code RubyArray.indexOf} returns a {@code begin}-relative
	 * index, whereas {@code get}/{@code set}/{@code remove}/{@code add} all use (0-based) indices.
	 *
	 * <p>The JRuby method causing this (returns the {@code begin}-relative {@code i}) is at:
	 * <a href="https://github.com/jruby/jruby/blob/9.4.14.0/core/src/main/java/org/jruby/RubyArray.java#L5839-L5852">
	 */
	private int logicalIndexOf(List<StructuralNode> blocks, StructuralNode node) {
		if ( blocks.isEmpty() ) {
			return -1;
		}
		int physical = blocks.indexOf( node );
		if ( physical < 0 ) {
			return -1;
		}
		int begin = blocks.indexOf( blocks.get( 0 ) );
		return physical - begin;
	}

    private boolean isCollapsible(StructuralNode desc) {
        List<StructuralNode> blocks = desc.getBlocks();
        if (blocks.size() > 1) {
            return true;
        }
        if (blocks.size() == 1) {
            Object content = blocks.get(0).getContent();
            if (content != null && !content.toString().startsWith("Environment variable: ")) {
                return true;
            }
        }
        return false;
    }

    private List<StructuralNode> findByRole(StructuralNode parent, String role) {
        List<StructuralNode> result = new java.util.ArrayList<>();
        for (StructuralNode block : parent.getBlocks()) {
            if (block.hasRole(role)) {
                result.add(block);
            }
            result.addAll(findByRole(block, role));
        }
        return result;
    }
}
