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
        data.merge!('version' => version)
      }

      number_pattern = /^[0-9.]+$/

      j = 0
      # Add a marker for things that don't have a version, because liquid filters are not as flexible as we'd like
      # This has to happen before we do any conversion of 'main'
      collection.docs.filter { |doc| ! doc.data["version"].match?(number_pattern) }.each { |doc|
        data = doc.data
        j +=1
        data.merge!('unversion_counter' => j)
      }

      # Also add a version scalar that we can use for sorting
      i = 0
      collection.docs.filter { |doc| doc.data["version"].match?(number_pattern) }.sort_by { |doc| Gem::Version.new(doc.data["version"]) }.reverse.each { |doc|
        data = doc.data
        i +=1
        data.merge!('version_counter' => i)


        # Mark the latest release as main
        if i == 1
          data.merge!('proposed_version' => data["version"])
          data.merge!('version' => "main")
        end
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