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