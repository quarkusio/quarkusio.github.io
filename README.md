# Quarkus.io Website Based on Jekyll

## Getting Started

These instructions will get you a copy of the Quarkus.io website up and running on your local machine for development and testing purposes.

### Installation
[Jekyll static site generator docs](https://jekyllrb.com/docs/).

1. Install a full [Ruby development environment](https://jekyllrb.com/docs/installation/)
2. Install Jekyll and [bundler](https://jekyllrb.com/docs/ruby-101/#bundler)  [gems](https://jekyllrb.com/docs/ruby-101/#gems) 
  
        gem install jekyll bundler

3. Fork the [project repository](https://github.com/quarkusio/quarkusio.github.io), then clone your fork.
  
        git clone git@github.com:YOUR_USER_NAME/quarkusio.github.io.git

4. Change into the projecr directory
  
        cd quarkusio.github.io

5. Use bundler to fetch all required gems in their respective versions

        bundle install

6. Build the site and make it available on a local server
  
        bundle exec jekyll serve
        
7. Now browse to http://localhost:4000

> If you encounter any unexpected errors during the above, please refer to the [troubleshooting](https://jekyllrb.com/docs/troubleshooting/#configuration-problems) page or the [requirements](https://jekyllrb.com/docs/installation/#requirements) page, as you might be missing development headers or other prerequisites.


**For more regarding the use of Jekyll, please refer to the [Jekyll Step by Step Tutorial](https://jekyllrb.com/docs/step-by-step/01-setup/).**

## Contributing

Please read [CONTRIBUTING.md](https://github.com/quarkusio/quarkusio.github.io/blob/master/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## License

This website is licensed under the [Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/).
