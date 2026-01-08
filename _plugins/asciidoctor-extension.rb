require 'asciidoctor/extensions'

include Asciidoctor

# Tooltip inline macro
# Constant name allows any character allowed for Java Enum constants.
# Description must be enclosed by Inline Passthrough Triple plus in order to handle any characters.
# Behavior: if no description is passed => same look as before
#           if description is passed => there are dots below constant and on hover tooltip is shown.
# Examples:
#
# - single line description:
# tooltip:SECOND_CONSTANT[+++!@$#^%^%$&8}{":l][]"+++]
#
# - multiple line description:
# tooltip:THIRD_CONSTANT[+++Test sentence test sentence
#  second line of a test sentence etc.+++]
#
# - no description (hint: without description, there is actually no point of using this macro):
# tooltip:FOURTH_CONSTANT[]
#
# - inside table cell, it's vital to prefix cell delimiter | with 'a' and place macros to next lines:
# a|
# tooltip:FIRST_CONSTANT[Description of my enum is xyz],
# tooltip:SECOND_CONSTANT[+++!@$#^%^%$&8}{":l][]"+++],
# ...
#
Extensions.register do
  inline_macro do
    named :tooltip
    resolve_attributes false
    process do |parent, target, attrs|
      tooltip = %(<code>#{target}</code>)
      if !attrs['text'].empty?
        tooltip = %(<span class="asciidoc-tooltip-wrapper"><code>#{target}</code><span class="asciidoc-tooltip">#{attrs['text']}</span></span>)
      end
      create_inline_pass parent, %(#{tooltip})
    end
  end
end

env_var_id=0
Extensions.register do
  inline_macro do
    named :env_var_with_copy_button
    resolve_attributes false
    process do |parent, target, attrs|
      copy_btn = %(<code id="env-var-#{env_var_id}">#{target}</code><button class="btn-copy fa fa-clipboard inline-btn-copy" data-clipboard-action="copy" data-clipboard-target="#env-var-#{env_var_id}" title="Copy to clipboard" do-not-collapse="true"></button>)
      env_var_id += 1
      create_inline_pass parent, %(#{copy_btn})
    end
  end
end

Extensions.register do
  inline_macro do
    named :config_property_copy_button
    resolve_attributes false
    process do |parent, target, attrs|
      copy_btn = %(<button class="btn-copy fa fa-clipboard inline-btn-copy" data-clipboard-action="copy" data-clipboard-text='#{target}' title="Copy to clipboard" do-not-collapse="true"></button>)
      create_inline_pass parent, %(#{copy_btn})
    end
  end
end

### ALL CONFIG page processors

# add show more 'button'
doc_max_id = Array.new
Extensions.register do
  tree_processor do
    process do |doc|
      # skip 2.7 branch as following code is not backwards compatible
      if !(doc.base_dir.include? '2.7')
        row_id = 0
        search_field_id = 0
        doc.find_by context: :table do |table|
          if (table.role && (table.role.include? 'configuration-reference'))
            table.add_role 'configuration-reference-all-rows'
            table.rows.body.each do |row|
              if (row && row.size > 0 && row[0].inner_document)
                row[0].inner_document.find_by role: 'description' do |desc|
                  row_id += 1
                  if (desc.blocks && desc.blocks.size && (desc.blocks.size > 1 || (desc.blocks.size == 1 && desc.blocks[0].content && !(desc.blocks[0].content.start_with? 'Environment variable: '))))
                    # now we know description consist of more than one 'Environment variable' line
                    desc.id = %(conf-collapsible-desc-#{row_id})
                    desc.add_role 'description-collapsed'
                    btnBlock = Block.new(desc.parent, :paragraph, source: %(<i class="fa fa-chevron-down"></i><span>Show more</span>))
                    btnBlock.add_role 'description-decoration'
                    # add show more 'button'
                    row[0].inner_document.blocks.push(btnBlock)
                  else
                    desc.id = %(conf-non-collapsible-desc-#{row_id})
                  end
                end
              end
            end
            if (table.role.include? 'searchable')
              caption = (children = table.parent.blocks)[(children.index table)-1]
              # create new caption with disabled search input
              new_caption = Block.new(caption.parent, :paragraph, source: %(#{caption.content} <input type="search" id="config-search-#{search_field_id}" placeholder="FILTER CONFIGURATION" disabled>))
              search_field_id += 1
              # replace original caption with enhanced one
              (children = caption.parent.blocks)[children.index caption] = new_caption
            end
          end
        end
        if row_id != 0
          # collect row ids per document
          doc_max_id.push(row_id)
          doc.id = %(page-with-config-#{doc_max_id.size - 1})
        end
      end
    end
  end
end

# set CSS classes to rows with description
Extensions.register do
  postprocessor do
    process do |doc, output|
      if doc.id && (doc.id.start_with? 'page-with-config-')
        doc_max_id_idx = doc.id.gsub('page-with-config-', '').to_i
        for id in 1..doc_max_id[doc_max_id_idx] do
          css_class = ''
          if (id > 30)
            css_class = ' row-hidden'
          end
          if (id.odd?)
            css_class += ' odd'
          end
          desc_idx = output.index(%(id="conf-collapsible-desc-#{id}"), 0)
          if (desc_idx)
            # collapsible description
            # set CSS classes for collapsible row
            row_idx = output.rindex('<tr>', desc_idx)
            if row_idx
              output[row_idx..(row_idx + 3)] = %(<tr class="row-collapsible row-collapsed row-with-desc#{css_class}">)
            else
              # illegal state - we don't expect this to happen unless there are breaking changes in Asciidoctor
              LoggerManager.logger.error(%(Failed to detect '<tr>' element on page #{doc.title}, show more button and config search won't work as expected))
            end
          else
            # odd non-collapsible row with description
            # add 'odd' CSS class
            desc_idx = output.index(%(id="conf-non-collapsible-desc-#{id}"), 0)
            row_idx = output.rindex('<tr>', desc_idx)
            if row_idx
              output[row_idx..(row_idx + 3)] = %(<tr class="#{css_class}">)
            else
              # illegal state - we don't expect this to happen unless there are breaking changes in Asciidoctor
              LoggerManager.logger.error(%(Failed to detect '<tr>' element on page #{doc.title}, row background color won't be correct))
            end
          end
        end
      end
      output
    end
  end
end

Extensions.register do
  tree_processor do
    process do |doc|
      status = doc.attr('extension-status')

      if status && !status.empty?
        tooltip = case status
          when 'experimental'
            'This extension requests early feedback to mature the idea'
          when 'preview'
            'This extension\'s backward compatibility and presence in the ecosystem is not guaranteed'
          when 'stable'
            'This extension\'s backward compatibility and presence in the ecosystem are taken very seriously'
          when 'deprecated'
            'This extension is likely to be replaced or removed in a future version'
          else
            ""
          end
        label_html = %(<a class="status-label status-#{status}" title="#{tooltip}" href="#extension-status-note">#{status}</a>)

        label_block = create_pass_block doc, label_html, {}
        doc.blocks.insert(0, label_block)
      end
      doc
    end
  end
end
