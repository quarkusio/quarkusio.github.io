# loads CNAME into cname config variable
module Jekyll
	class LimitedEnvironmentVariables < Generator
		def generate(site)
			if(File.exist?('CNAME')) 
				site.config['cname'] = File.read("CNAME").strip
			else
				site.config['cname'] = ""
			end
		end
	end
end