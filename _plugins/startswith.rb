module Jekyll
   module StringFilter
    def startswith(text, query)
      return text.start_with? query
    end
  end
end
  
Liquid::Template.register_filter(Jekyll::StringFilter)
