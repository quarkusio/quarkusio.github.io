module VersionPermalink
  class VersionPermalink < Jekyll::Generator
    safe true
    priority :low

    def generate(site)
      # fill in version information from the filename
      collection = site.collections["migrations"]
      collection.docs.each { |doc|
        data = doc.data
        reg_str = data["version_regex"]
        repl_str = ""
        re = Regexp.new reg_str

        # This will be returned
        version = data["slug"].gsub re, repl_str
        doc.data.merge!('version' => version)

      }
    end

    Jekyll::Hooks.register :migrations, :pre_render do |doc|
      begin
        # check if jekyll can resolve the url template
        doc.url
      rescue NoMethodError => error
        begin
          def doc.url_template
            custom_permalink_placeholders = ["version"]
            @custom_url_template ||= custom_permalink_placeholders.inject(collection.url_template) { |o, m| o.sub ":" + m, data["version"].to_s }

          end
        end
      end
    end
  end
end