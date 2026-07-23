package io.quarkus.tools.migration.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

public class ConfigTablePostprocessor extends Postprocessor {

    @Override
    public String process(Document document, String output) {
        Object maxIdObj = document.getAttribute("quarkus-config-max-row-id");
        if (maxIdObj == null) {
            return output;
        }
        int maxId = Integer.parseInt(maxIdObj.toString());

        StringBuilder sb = new StringBuilder(output);

        for (int id = 1; id <= maxId; id++) {
            String cssClass = "";
            if (id > 30) {
                cssClass = " row-hidden";
            }
            if (id % 2 == 1) {
                cssClass += " odd";
            }

            String collapsibleMarker = "id=\"conf-collapsible-desc-" + id + "\"";
            int descIdx = sb.indexOf(collapsibleMarker);
            if (descIdx >= 0) {
                int rowIdx = sb.lastIndexOf("<tr>", descIdx);
                if (rowIdx >= 0) {
                    sb.replace(rowIdx, rowIdx + 4,
                            "<tr class=\"row-collapsible row-collapsed row-with-desc" + cssClass + "\">");
                }
            } else {
                String nonCollapsibleMarker = "id=\"conf-non-collapsible-desc-" + id + "\"";
                descIdx = sb.indexOf(nonCollapsibleMarker);
                if (descIdx >= 0 && !cssClass.isBlank()) {
                    int rowIdx = sb.lastIndexOf("<tr>", descIdx);
                    if (rowIdx >= 0) {
                        sb.replace(rowIdx, rowIdx + 4,
                                "<tr class=\"" + cssClass.trim() + "\">");
                    }
                }
            }
        }

        return sb.toString();
    }
}
