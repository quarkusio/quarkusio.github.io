require 'chunky_png'
require 'cairo'
require 'rsvg2'

module Jekyll
  # Generates social images for blog posts and guides
  module SocialImages
    def social_image(text, page_path)
      # If text is not empty, return it
      if text.nil? || text.empty?
        if File.exist?("./assets/images/social/#{File.basename(page_path, '.adoc')}.png")
          return "/assets/images/social/#{File.basename(page_path, '.adoc')}.png"
        else
          return "/assets/images/quarkus_card.png"
        end
      else
        text
      end
    end
  end

  class GenerateSocialImagesGenerator < Generator
    def generate(site)
      # Check if skip_social_images is set to true
      # If so, skip generating social images
      # This is useful when running the site locally
      if site.config['skip_social_images']
        Jekyll.logger.info('Skipping social image generation')
        return
      end
      generate_images(Dir.glob(File.join(site.source, '_posts', '*.adoc')), site)
      generate_images(Dir.glob(File.join(site.source, '_guides', '*.adoc')), site)
    end

    def split_text_into_lines(text)
      lines = []
      words = text.split(' ')
      current_line = ''

      words.each do |word|
        if current_line.length + word.length <= 32
          current_line += (current_line == '' ? '' : ' ') + word
        else
          lines.push(current_line)
          current_line = word
        end
      end

      lines.push(current_line) unless current_line.empty?

      lines
    end

    private

    def generate_images(files, site)
      output_dir = 'assets/images/social'
      FileUtils.mkdir_p(File.join(site.source, output_dir))

      files.each do |guide_file|
        basename = File.basename(guide_file, '.adoc')
        if basename.start_with?('_')
          next
        end
        title = extract_title(guide_file)
        # Skip if title is empty
        if (title.nil? || title.empty?)
          next
        end
        output_file = File.join(site.source, output_dir, "#{basename}.png")
        # Skip if the file already exists
        if File.exist?(output_file)
          next
        end

        Jekyll.logger.info("Generating social image for '#{title}' in #{output_file}")

        # Generate the SVG image
        svg_image_str = generate_svg_string(title)

        # Create a Cairo surface and context for the PNG image (must be smaller than 600x330)
        surface = Cairo::ImageSurface.new(Cairo::FORMAT_ARGB32, 600, 250)
        context = Cairo::Context.new(surface)

        # Load and render the SVG onto the Cairo context
        svg = RSVG::Handle.new_from_data(svg_image_str)
        context.render_rsvg_handle(svg)

        # Save the Cairo surface to a PNG file
        b = StringIO.new
        surface.write_to_png(b)

        # Compose the generated image with the template image
        png_image = ChunkyPNG::Image.from_file('_plugins/assets/quarkus_card_blank.png')
        # Change the last parameters to change the position of the generated image
        png_image.compose!(ChunkyPNG::Image.from_blob(b.string), 0, 80)

        # Save the composed image to the output file
        png_image.save(output_file)
      end
    end

    def generate_svg_string(title)
      idx = 90
      font_size = 30
      tspan_elements = ''
      # Sanitize title
      title = title.gsub(/&/, '&amp;')
      title = title.gsub(/</, '&lt;')
      title = title.gsub(/>/, '&gt;')

      split_text_into_lines(title).each_with_index do |line, index|
        tspan_elements += "<tspan x='50%' y='#{idx}'>#{line}</tspan>"
        idx += font_size + 10
      end
      "
        <svg width=\"600\" height=\"330\">
          <style>
            .title { fill: white; font-size: #{font_size}px; font-weight: bold; font-family:'Open Sans'}
          </style>
          <text x=\"50%\" y=\"50%\" text-anchor=\"middle\" class=\"title\" >
              #{tspan_elements}
          </text>
        </svg>
      "
    end

    def extract_title(adoc_file)
      line_nr = 0
      File.readlines(adoc_file).each do |line|
        if line_nr == 0
          # If line does not start with --- break
          unless line.strip.start_with?('---')
            break
          end
        end
        if line_nr > 0 && line.strip.start_with?('---')
          break;
        end
        if line.strip.start_with?('title:')
          title = line.strip.sub('title:', '').strip
          # Remove quotes
          title = title.gsub(/\A[\"']|[\"']\z/, '')
          return title
        end
        line_nr += 1
      end
      doc = Asciidoctor.load_file(adoc_file, header_only: true, logger: NullLogger.new)
      doc.doctitle
    end
  end
end

Liquid::Template.register_filter(Jekyll::SocialImages)
