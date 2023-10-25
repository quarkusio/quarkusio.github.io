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

const appSelector = '#guides-app'
const appElement = document.querySelector(appSelector);

const app = createApp({
  props: {
    url: String,
    initialTimeout: Number,
    moreTimeout: Number,
    minChars: Number,
    quarkusVersion: String
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
      // "debounce" makes sure we only run search ~300ms after the user is done typing the text
      // WARNING: we really do want to debounce here, NOT in setters,
      // because debouncing in setters leads to data in input forms being refreshed after the timeout,
      // causing problems when typing text relatively fast.
      handler: debounce(300, function(newValue, oldValue) {
        this.resetAndSearch()
      })
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
        // Turn null to "", because that's how the select box refers to "all categories"
        return this.search.input.categories || ""
      },
      set(val) {
        // Turn "" to null, because that's how the search service refers to "all categories"
        this.search.input.categories = val || null
      }
    },
    hasInput() {
      return this.search.input.q || this.search.input.categories
    },
    hasInputWithTooFewChars() {
      return this.search.input.q && this.search.input.q.length < this.minChars
    },
    hasHits() {
      return this.search.result.hits.length > 0
    },
    cardHits() {
      return this.search.result.hits.map(hit => this.guidesPathToCardHtmlElement.get(hit.id)).filter(e => e)
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
      .map(element => [new URL(element.querySelector('a').href).pathname, element]))

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
    async resetAndSearch() {
      if (this.loading) {
        this.loading.abort()
      }
      this.loading = new AbortController();
      this._resetResults()
      await this._searchBatch(this.initialTimeout)
    },
    async searchMore() {
      if (this.loading) {
        return // Already searching
      }
      this.loading = new AbortController();
      this.search.page = this.search.page + 1
      await this._searchBatch(this.moreTimeout)
    },
    _resetResults() {
      this.search.page = 0
      this.search.result.hits = []
      this.search.result.hasMoreHits = false
    },
    async _searchBatch(timeout) {
      if (!this.hasInput) {
        // No input => no search
        return
      }
      const controller = this.loading
      try {
        if (this.hasInputWithTooFewChars) {
          throw 'Too few characters'
        }
        const queryParams = {
          page: this.search.page,
          version: this.quarkusVersion
        }
        Object.assign(queryParams, this.search.input)
        const result = await this._jsonFetch(controller, 'GET', queryParams, timeout)
        this.search.result.hits = this.search.result.hits.concat(result.hits)
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
    async _jsonFetch(controller, method, queryParams, timeout) {
      const timeoutId = setTimeout(() => controller.abort(), timeout)
      const response = await fetch(this.url + '?' + ( new URLSearchParams( queryParams ) ).toString(), {
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
          .map(([path, _]) => { return { id: path } })
    },
    _javascriptFilter(card, terms, categories) {
      let match = true
      if (match && categories) {
        const categoriesElem = card.getElementsByClassName('categories').item(0)
        match = categoriesElem && this._containsAllCaseInsensitive(categoriesElem, categories)
      }
      if (match && terms) {
        match = this._containsAllCaseInsensitive(card, terms)
      }
      return match
    },
    _containsAllCaseInsensitive(elem, terms) {
      const text = (elem.textContent || elem.innerText || '').toLowerCase()
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
