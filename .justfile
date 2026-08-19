default:
   just --list

# Preview blog posts with auto-detection and browser open
blog-preview:
    bash blog-preview.sh

# Serve the full site locally (includes guides)
serve:
    bash serve.sh

# Serve the site locally without guides (fast)
serve-noguides:
    bash serve-noguides.sh

# Serve the site locally with only the latest guides
serve-only-latest-guides:
    bash serve-only-latest-guides.sh
