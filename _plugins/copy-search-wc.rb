require 'open-uri'

module Jekyll
  class CopySearchScript < Jekyll::Plugin
    Jekyll::Hooks.register :site, :post_write do |site|
      script_mode = site.config['search']['script-mode']
      script_path = site.config['search']['script-path']
      search_host = site.config['search']['host']
      script_copy_from_url = URI(search_host + script_path)

      if script_mode == 'cached'
        if search_host.nil? || search_host.empty?
          raise "Error: The search host URL is not configured."
        end
        cached_script_file = site.config['search']['cached-script-file']
        assets_path = File.join(site.config['destination'], cached_script_file)
        if script_copy_from_url.host == 'localhost' && File.exist?('/.dockerenv')
          script_copy_from_url.host = 'host.docker.internal'
        end
        begin
          URI.open(script_copy_from_url) do |response|
            content = response.read
            if response.status[0] != '200'
              raise "Error: The search wc '#{script_copy_from_url}' is not accessible. Status: #{response.status[0]} - #{response.status[1]}"
            elsif content.nil? || content.empty?
              raise "Error: The search wc '#{script_copy_from_url}' is empty."
            else
              content_without_sourcemaps = content.gsub(/\/\/# sourceMappingURL=.*\.map/, '')
              File.delete(assets_path) if File.exist?(assets_path)
              File.open(assets_path, 'w') { |file| file.write(content_without_sourcemaps) }
              puts "The search wc '#{script_copy_from_url}' has been copied to '#{assets_path}'."
            end
          end
        rescue OpenURI::HTTPError => e
          raise "Error: The search wc '#{script_copy_from_url}' is not accessible. Status: #{e.io.status[0]} - #{e.io.status[1]}"
        end
      end
    end
  end
end