import { createApp } from './vue.esm-browser.prod.js'

// https://blog.logrocket.com/debounce-throttle-vue/
function debounce(wait, fn) {
  let timer;
  return function(...args) {
    if (timer) {
      clearTimeout(timer) // clear any pre-existing timer
    }
    const context = this // get the current context
    timer = setTimeout(() => {
      fn.apply(context, args) // call the function if time expires
    }, wait)
  }
}

function concat(fn1, fn2) {
  return function(...args) {
    fn1.apply(this, args)
    fn2.apply(this, args)
  }
}

const appSelector = '#guides-app'
const appElement = document.querySelector(appSelector);

const app = createApp({
  props: {
    searchApiServer: String,
    quarkusVersion: String,
    language: String,
    initialTimeout: Number,
    moreTimeout: Number,
    minChars: Number
  },
  data() {
    return {
      loading: false,
      search: {
        input: {
        },
        page: null,
        result: {
          hits: [],
          hasMoreHits: false
        }
      },
      guidesPathToCardHtmlElement: {
      }
    }
  },
  watch: {
    'search.input.q': {
      handler: concat(
        // Without debouncing, we want to cancel the previous search and mark the view as "loading",
        // so that we don't mistakenly display "no results" while debouncing the initial search.
        // See https://github.com/quarkusio/search.quarkus.io/issues/200
        function(newValue, oldValue) {
          this.resetAndMarkLoading()
        },
        // "debounce" makes sure we only run search ~300ms after the user is done typing the text
        // WARNING: we really do want to debounce here, NOT in setters,
        // because debouncing in setters leads to data in input forms being refreshed after the timeout,
        // causing problems when typing text relatively fast.
        debounce(300, function(newValue, oldValue) {
          this.resetAndSearch()
        })
      )
    },
    'search.input.categories': {
      handler(newValue, oldValue) {
        this.resetAndSearch()
      }
    }
  },
  computed: {
    text: {
      get() {
        return this.search.input.q
      },
      set(val) {
        this.search.input.q = val
      }
    },
    category: {
      get() {
        // Turn "no entry" into "", because that's how the select box refers to "all categories"
        return this.search.input.categories || ""
      },
      set(val) {
        if (val) {
          this.search.input.categories = val
        }
        // Turn null/"" into "no entry", because not specifying the category
        // is how we get the search service to return all categories
        else {
          delete this.search.input.categories
        }
      }
    },
    hasInput() {
      return this.search.input.q && this.search.input.q.length >= this.minChars || this.search.input.categories
    },
    hasInputWithTooFewChars() {
      return this.search.input.q && this.search.input.q.length < this.minChars
    },
    hasHits() {
      return this.search.result.hits.length > 0
    },
    searchHits() {
      return this.search.result.hits
    }
  },
  mounted() {
    const appElement = this.$el.parentElement

    // Retrieve cards from the static HTML, so that we can use them to display search results.
    let cardSelector = '.docs-card'
    let cards = appElement.querySelectorAll(cardSelector)
    if (cards.length == 0) {
      // Fallback for older versions of the docs
      cardSelector = '.card'
      cards = appElement.querySelectorAll(cardSelector)
    }
    this.guidesPathToCardHtmlElement = new Map(Array.from(cards)
        .map(element => {
          const link = element.querySelector('h4 a')
          if (link) {
            // new versions:
            const url = link.getAttribute('href');
            return [
              new URL(link.href).pathname,
              {
                url: url,
                title: link.innerHTML,
                type: [...element.classList]
                    .filter(clazz => clazz.endsWith("bkg"))
                    .map(clazz => clazz.substring(0, clazz.length - "bkg".length))
                    .at(0),
                summary: element.querySelector('div .description').innerHTML,
                keywords: element.querySelector('div .keywords').innerHTML,
                categories: element.querySelector('div .categories').innerHTML,
                origin: element.querySelector('div .origin')?.innerHTML
              }];
          } else {
            // older Quarkus versions:
            const url = element.querySelector('a').getAttribute('href')
            return [
              url,
              {
                url: url,
                title: element.querySelector('p.title').innerHTML,
                summary: element.querySelector('div.description').innerHTML,
                keywords: element.querySelector('div.keywords').innerHTML,
                origin: element.querySelector('div.origin')?.innerHTML
              }];
          }
        }))

    // Load more results on scroll
    document.addEventListener('scroll', e => {
      if (!this.search.result.hasMoreHits) {
        // No more hits to fetch.
        return
      }
      const resultCards = appElement.querySelectorAll('.results ' + cardSelector)
      const lastResultCard = resultCards.length == 0 ? null : resultCards[resultCards.length - 1]
      if (!lastResultCard) {
        // No result card is being displayed at the moment.
        return
      }
      const scrollElement = document.documentElement // Scroll bar is on the <html> element
      const bottomOfViewport = scrollElement.scrollTop + scrollElement.clientHeight
      const topOfLastResultCard = lastResultCard.offsetTop
      if (bottomOfViewport >= topOfLastResultCard) {
        this.searchMore()
      }
    })
  },
  methods: {
    async resetAndMarkLoading() {
      if (this.loading) {
        this.loading.abort()
      }
      this.loading = new AbortController()
      this._resetResults()
    },
    async resetAndSearch() {
      this.resetAndMarkLoading()
      await this._searchBatch(this.loading, this.initialTimeout)
    },
    async searchMore() {
      if (this.loading) {
        return // Already searching
      }
      this.loading = new AbortController();
      this.search.page = this.search.page + 1
      await this._searchBatch(this.loading, this.moreTimeout)
    },
    _resetResults() {
      this.search.page = 0
      this.search.result.hits = []
      this.search.result.hasMoreHits = false
    },
    async _searchBatch(controller, timeout) {
      try {
        if (!this.hasInput) {
          // No input => no search
          return
        }
        if (this.hasInputWithTooFewChars) {
          throw 'Too few characters'
        }
        const queryParams = {
          page: this.search.page,
          version: this.quarkusVersion,
          language: this.language,
          contentSnippets: 2,
          contentSnippetsLength: 120,
          highlightCssClass: 'highlighted'
        }
        Object.assign(queryParams, this.search.input)
        const result = await this._jsonFetch(controller, 'GET', queryParams, timeout)
        this.search.result.hits = this.search.result.hits.concat(this._processHits(result.hits))
        this.search.result.hasMoreHits = result.hits.length > 0
      }
      catch(error) {
        console.error('Could not run search: ' + error)
        if (this.loading != controller) {
          // A concurrent search erased ours; most likely input changed while waiting for results.
          // Ignore this search and let the concurrent one reset the data as it sees fit.
          return
        }
        this._resetResults()
        // Fall back to Javascript in-page search
        const hits = this._searchUsingJavascript()
        this.search.result.hits = hits
      }
      finally {
        if (this.loading == controller) {
          this.loading = null
        }
      }
    },
    _processHits(serverHits) {
      return serverHits.map(hit => {
        hit.content = hit?.content.map(paragraph => `...${paragraph}...`)
        return hit;
      })
    },
    async _jsonFetch(controller, method, queryParams, timeout) {
      const timeoutId = setTimeout(() => controller.abort(), timeout)
      const response = await fetch(`${this.searchApiServer}api/guides/search?${new URLSearchParams( queryParams )}`, {
        method: method,
        signal: controller.signal,
        body: null
      })
      clearTimeout(timeoutId)
      if (response.ok) {
        return await response.json()
      }
      else {
        throw 'Response status is ' + response.status + '; response: ' + await response.text()
      }
    },
    _searchUsingJavascript() {
      const terms = this.search.input.q ? this.search.input.q.split(' ').map(token => token.trim()) : null
      const categories = this.search.input.categories

      return Array.from(this.guidesPathToCardHtmlElement)
          .filter(([path, card]) => this._javascriptFilter(card, terms, categories))
          .map(([_, card]) => card)
    },
    _javascriptFilter(card, terms, categories) {
      let match = true
      if (match && categories) {
        match = this._containsAllCaseInsensitive(card.categories, categories)
      }
      if (match && terms) {
        match = this._containsAllCaseInsensitive(`${card.keywords}${card.summary}${card.title}${card.categories}`, terms)
      }
      return match
    },
    _containsAllCaseInsensitive(elem, terms) {
      const text = (elem ? elem : '').toLowerCase();
      for (let i in terms) {
        if (text.indexOf(terms[i].toLowerCase()) < 0) {
          return false
        }
      }
      return true
    }
  }
},
// Pass data-* elements as props: https://stackoverflow.com/a/64010905/6692043
{ ...appElement.dataset }
)
app.mount(appSelector)
app.config.errorHandler = (err, instance, info) => {
    console.error(err)
}
