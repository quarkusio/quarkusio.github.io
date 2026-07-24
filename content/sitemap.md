---
layout: sitemap
title: Sitemap
subtitle: Complete navigation guide to all Quarkus content
---

**Note:** This sitemap was generated using AI and reviewed by humans. If you notice any missing or incorrect links, please [open an issue](https://github.com/quarkusio/quarkusio.github.io/issues) on GitHub.

This sitemap provides a comprehensive overview of all content available on the Quarkus website, organized by category for easy navigation by both humans and search engines.

{% for section in site.data.sitemap-links.sections %}
## {{ section.title }}

{% for link in section.links %}
- [{{ link.title }}]({{ link.url }}) - {{ link.description }}
{% endfor %}

{% if section.subsections %}
{% for subsection in section.subsections %}
### {{ subsection.title }}
{% for link in subsection.links %}
- [{{ link.title }}]({{ link.url }}) - {{ link.description }}
{% endfor %}
{% endfor %}
{% endif %}

{% endfor %}

---

## For Search Engines & LLMs

This sitemap is designed to be easily crawlable and indexable. All links are relative to the root domain (https://quarkus.io) and follow a consistent structure. The content is organized hierarchically to reflect the information architecture of the Quarkus website.

**Last Updated:** May 2026

**Content Categories:**
- Documentation (Guides, Tutorials, References)
- Community (Blog, Podcast, Events, User Stories)
- Learning (Books, Training, Videos)
- Development (Tools, Extensions, Contributing)
- Foundation (Governance, Security, Usage)

For questions or suggestions about this sitemap, please [open an issue](https://github.com/quarkusio/quarkusio.github.io/issues) on GitHub.
