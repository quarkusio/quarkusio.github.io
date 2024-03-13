require 'open-uri'

module Jekyll
  class CopySearchScript < Jekyll::Plugin
    Jekyll::Hooks.register :site, :post_write do |site|
      script_copy = site.config['search']['script-copy']
      script_copy_url = URI(script_copy)
      script_src = site.config['search']['script']
      unless !script_src.nil? && !script_src.empty? && script_src.start_with?('http')
        if script_copy.nil? || script_copy.empty?
          raise "Error: The search wc script-copy URL is not configured."
        end
        assets_path = File.join(site.config['destination'], script_src)
        if script_copy_url.host == 'localhost' && File.exist?('/.dockerenv')
          script_copy_url.host = 'host.docker.internal'
        end
        begin
          URI.open(script_copy_url) do |response|
            content = response.read
            if response.status[0] != '200'
              raise "Error: The search wc '#{script_copy_url}' is not accessible. Status: #{response.status[0]} - #{response.status[1]}"
            elsif content.nil? || content.empty?
              raise "Error: The search wc '#{script_copy_url}' is empty."
            else
              content_without_sourcemaps = content.gsub(/\/\/# sourceMappingURL=.*\.map/, '')
              File.delete(assets_path) if File.exist?(assets_path)
              File.open(assets_path, 'w') { |file| file.write(content_without_sourcemaps) }
              puts "The search wc '#{script_copy_url}' has been copied to '#{assets_path}'."
            end
          end
        rescue OpenURI::HTTPError => e
          raise "Error: The search wc '#{script_copy_url}' is not accessible. Status: #{e.io.status[0]} - #{e.io.status[1]}"
        end
      end
    end
  end
end